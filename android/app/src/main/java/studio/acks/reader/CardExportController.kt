package studio.acks.reader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.min

data class CardExportOptions(
    val themeId: String = "aireport",
    val density: String = "balanced",
    val withCover: Boolean = true,
    val fontSource: String = "local",
    val fontSizePx: Float = 18f,   // card base font size (14–22 px)
    val padPx: Int = 28            // card padding on all sides
)

data class CardExportResult(
    val files: List<File>,
    val count: Int
)

object CardExportController {

    private const val CARD_CSS_W = 540   // CSS px（= 物理 px，density 已由 setInitialScale 抵消）
    private const val CARD_CSS_H = 720
    private const val CARD_PAD   = 28    // 与 JS 端 CARD_PAD 保持一致
    private const val OUT_W      = 1080  // 输出 JPEG 物理宽
    private const val OUT_H      = 1440  // 输出 JPEG 物理高
    private const val MAX_H      = 20_000 // 测量 WebView 最大高度（物理 px）

    /**
     * 逐页渲染方案：
     * 1. 在 sourceWebView 里提取块列表 + 生成测量文档 HTML
     * 2. 软件 WebView 加载测量文档，查询每块真实 CSS 高度
     * 3. Kotlin 贪心装页（不跨块切断）
     * 4. 每页独立生成 HTML → 独立软件 WebView 截图 → 缩放保存
     */
    suspend fun exportCards(
        ctx: Context,
        sourceWebView: WebView,
        markdown: String,
        opts: CardExportOptions,
        previewMode: Boolean = false,   // true = only cache files, no gallery save
        onProgress: (current: Int, total: Int) -> Unit
    ): CardExportResult = withContext(Dispatchers.Main) {

        onProgress(0, -1)

        val optsJs = buildOptsJson(opts)

        // ── Step 1: 存 markdown 到 sourceWebView 上下文 ─────────────────────────
        suspendCancellableCoroutine<Unit> { cont ->
            sourceWebView.evaluateJavascript(
                "window.__acksCardMd = ${jsStr(markdown)}; null"
            ) { cont.resume(Unit) }
        }

        // ── Step 2: 提取块列表 ───────────────────────────────────────────────────
        val blocksJson = suspendCancellableCoroutine<String> { cont ->
            sourceWebView.evaluateJavascript(
                "(function(){ try{ return window.extractCardBlocks(window.__acksCardMd, $optsJs); }catch(e){ return '[]'; } })()"
            ) { raw -> cont.resume(decodeJsString(raw ?: "\"[]\"")) }
        }

        val blockHtmls = parseBlockHtmls(blocksJson)
        if (blockHtmls.isEmpty() && !opts.withCover) {
            return@withContext CardExportResult(emptyList(), 0)
        }

        // ── Step 3: 生成测量文档 HTML ────────────────────────────────────────────
        val measureHtml = suspendCancellableCoroutine<String> { cont ->
            sourceWebView.evaluateJavascript(
                "(function(){ try{ return window.buildMeasurementHtml(${jsStr(blocksJson)}, $optsJs); }catch(e){ return ''; } })()"
            ) { raw -> cont.resume(decodeJsString(raw ?: "\"\"")) }
        }

        // ── Step 4: 软件 WebView 测量各块高度 ────────────────────────────────────
        val pages: List<List<Int>>
        if (measureHtml.isBlank() || blockHtmls.isEmpty()) {
            pages = emptyList()
        } else {
            val measWv = createSoftwareWebView(ctx)
            measWv.measure(
                View.MeasureSpec.makeMeasureSpec(CARD_CSS_W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(MAX_H, View.MeasureSpec.EXACTLY)
            )
            measWv.layout(0, 0, CARD_CSS_W, MAX_H)

            val measLoaded = suspendCancellableCoroutine<Boolean> { cont ->
                var done = false
                measWv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (!done) { done = true; cont.resume(true) }
                    }
                    override fun onReceivedError(
                        view: WebView, errorCode: Int, description: String?, url: String?
                    ) { if (!done) { done = true; cont.resume(false) } }
                }
                measWv.loadDataWithBaseURL(
                    "file:///android_asset/web/", measureHtml, "text/html", "UTF-8", null
                )
            }

            val positionsJson = if (measLoaded) {
                delay(500)  // 给字体加载足够时间，否则 fallback 字体高度偏小导致装页错误
                suspendCancellableCoroutine<String> { cont ->
                    // 内联查询逻辑：getMeasuredPositions 只在 host WebView 里定义，
                    // 无法在测量 WebView 里直接调用，必须内联
                    measWv.evaluateJavascript("""
                        (function(){
                          var blocks=Array.from(document.querySelectorAll('[data-bidx]'));
                          var firstTop=blocks.length>0
                            ?Math.round(blocks[0].getBoundingClientRect().top+window.scrollY)
                            :28;
                          return JSON.stringify({firstTop:firstTop,positions:blocks.map(function(b){
                            var r=b.getBoundingClientRect();
                            return{top:Math.round(r.top+window.scrollY),bottom:Math.round(r.bottom+window.scrollY)};
                          })});
                        })()
                    """.trimIndent()) { r -> cont.resume(decodeJsString(r ?: "\"{}\"")) }
                }
            } else ""
            measWv.destroy()

            val (firstTop, positions) = parsePositions(positionsJson)
            // usableH = 卡片高度 - 上下边距 - 额外安全裕量（补偿字体测量误差）
            val safetyMargin = opts.padPx
            pages = packPages(positions, CARD_CSS_H - opts.padPx * 2 - safetyMargin, firstTop)
        }

        val totalCards = pages.size + if (opts.withCover) 1 else 0
        val cacheDir  = File(ctx.cacheDir, "card_export").apply { mkdirs() }
        val files     = mutableListOf<File>()
        var cardIdx   = 0

        onProgress(0, totalCards)

        // ── Step 6: 创建复用渲染 WebView（原生 2x：直接渲染到 1080×1440，无需放大）───
        val renderWv = createRenderWebView(ctx)
        renderWv.measure(
            View.MeasureSpec.makeMeasureSpec(OUT_W, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(OUT_H, View.MeasureSpec.EXACTLY)
        )
        renderWv.layout(0, 0, OUT_W, OUT_H)

        // ── Step 7: 封面卡 ────────────────────────────────────────────────────────
        if (opts.withCover) {
            val coverHtml = suspendCancellableCoroutine<String> { cont ->
                sourceWebView.evaluateJavascript(
                    "(function(){ try{ return window.buildCoverCardHtml(window.__acksCardMd, $optsJs); }catch(e){ return ''; } })()"
                ) { raw -> cont.resume(decodeJsString(raw ?: "\"\"")) }
            }
            ++cardIdx
            renderAndSave(ctx, renderWv, coverHtml, cacheDir, previewMode)
                ?.let { files.add(it) }
            onProgress(cardIdx, totalCards)
        }

        // ── Step 8: 内容卡逐页渲染 ───────────────────────────────────────────────
        for (pageBlockIndices in pages) {
            val pageBlocksJson = buildPageBlocksJson(blockHtmls, pageBlockIndices)
            ++cardIdx
            val pageHtml = suspendCancellableCoroutine<String> { cont ->
                sourceWebView.evaluateJavascript(
                    "(function(){ try{ return window.buildContentCardHtml(${jsStr(pageBlocksJson)}, $cardIdx, $totalCards, $optsJs); }catch(e){ return ''; } })()"
                ) { raw -> cont.resume(decodeJsString(raw ?: "\"\"")) }
            }
            renderAndSave(ctx, renderWv, pageHtml, cacheDir, previewMode)
                ?.let { files.add(it) }
            onProgress(cardIdx, totalCards)
        }

        renderWv.destroy()
        CardExportResult(files, files.size)
    }

    // ── 在 renderWv 里渲染单页 HTML，直接截图 OUT_W×OUT_H 并保存 ─────────────────
    private suspend fun renderAndSave(
        ctx: Context,
        renderWv: WebView,
        html: String,
        cacheDir: File,
        previewMode: Boolean = false
    ): File? = withContext(Dispatchers.Main) {
        if (html.isBlank()) return@withContext null

        val loaded = suspendCancellableCoroutine<Boolean> { cont ->
            var done = false
            renderWv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (!done) { done = true; cont.resume(true) }
                }
                override fun onReceivedError(
                    view: WebView, errorCode: Int, description: String?, url: String?
                ) { if (!done) { done = true; cont.resume(false) } }
            }
            renderWv.loadDataWithBaseURL(
                "file:///android_asset/web/", html, "text/html", "UTF-8", null
            )
        }
        if (!loaded) return@withContext null
        delay(450)  // 字体 + 主题 CSS 应用时间

        // 直接截图到 OUT_W×OUT_H，CSS 已在 2x DPR 下渲染，无需放大
        val bitmap = try {
            Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888)
                .also { renderWv.draw(Canvas(it)) }
        } catch (_: OutOfMemoryError) { return@withContext null }

        // 页码由 contentHtml 里的 CSS `.pn` 负责，2x 渲染下清晰；
        // 封面无页码（符合封面设计）

        val file = File(cacheDir, "card_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (!previewMode) saveToGalleryJpeg(ctx, bitmap)
        bitmap.recycle()
        file
    }

    // ── 贪心装页（按实际 CSS 高度，不跨块切断） ────────────────────────────────────
    private fun packPages(
        positions: List<Pair<Int, Int>>,
        usableH: Int,
        firstBlockTop: Int
    ): List<List<Int>> {
        if (positions.isEmpty()) return emptyList()

        val pages   = mutableListOf<MutableList<Int>>()
        var curPage = mutableListOf<Int>()
        var pageStart = firstBlockTop

        for ((i, pos) in positions.withIndex()) {
            val (top, bottom) = pos
            val relBottom = bottom - pageStart

            if (curPage.isNotEmpty() && relBottom > usableH) {
                pages.add(curPage)
                curPage = mutableListOf()
                pageStart = top
            }

            curPage.add(i)

            // 单块超过整页高度：单独一页，下一页从该块底部开始
            if (curPage.size == 1 && (bottom - pageStart) > usableH) {
                pages.add(curPage)
                curPage = mutableListOf()
                pageStart = bottom
            }
        }

        if (curPage.isNotEmpty()) pages.add(curPage)
        return pages
    }

    // ── JSON 解析 ─────────────────────────────────────────────────────────────────

    private fun parseBlockHtmls(json: String): List<String> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getJSONObject(it).getString("html") }
    } catch (_: Exception) { emptyList() }

    private fun parsePositions(json: String): Pair<Int, List<Pair<Int, Int>>> = try {
        val obj      = JSONObject(json)
        val firstTop = obj.optInt("firstTop", CARD_PAD)
        val arr      = obj.getJSONArray("positions")
        val list     = (0 until arr.length()).map {
            val p = arr.getJSONObject(it)
            Pair(p.getInt("top"), p.getInt("bottom"))
        }
        Pair(firstTop, list)
    } catch (_: Exception) { Pair(CARD_PAD, emptyList()) }

    private fun buildPageBlocksJson(allBlocks: List<String>, indices: List<Int>): String {
        val sb = StringBuilder("[")
        indices.forEachIndexed { j, idx ->
            if (j > 0) sb.append(",")
            sb.append("{\"idx\":").append(idx).append(",\"html\":")
            sb.append(jsStr(allBlocks.getOrElse(idx) { "" }))
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun buildOptsJson(opts: CardExportOptions) =
        """{"themeId":${jsStr(opts.themeId)},"density":${jsStr(opts.density)},"withCover":${opts.withCover},"fontSource":${jsStr(opts.fontSource)},"fontSizePx":${opts.fontSizePx},"padPx":${opts.padPx}}"""

    // ── 测量用软件 WebView（1x：540 物理 px ≈ 540 CSS px，用于准确测量块高度）────
    @Suppress("SetJavaScriptEnabled")
    private fun createSoftwareWebView(ctx: Context): WebView {
        val density = ctx.resources.displayMetrics.density
        return WebView(ctx).apply {
            settings.javaScriptEnabled    = true
            settings.allowFileAccess      = true
            settings.domStorageEnabled    = true
            settings.useWideViewPort      = true
            settings.loadWithOverviewMode = false
            settings.textZoom             = 100
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            setInitialScale((100f / density).toInt().coerceAtLeast(1))
        }
    }

    // ── 渲染用软件 WebView（2x：1080×1440 物理 px，CSS 仍是 540×720，原生 2x 清晰度）
    @Suppress("SetJavaScriptEnabled")
    private fun createRenderWebView(ctx: Context): WebView {
        val density = ctx.resources.displayMetrics.density
        return WebView(ctx).apply {
            settings.javaScriptEnabled    = true
            settings.allowFileAccess      = true
            settings.domStorageEnabled    = true
            settings.useWideViewPort      = true   // 遵守 viewport meta width=540
            settings.loadWithOverviewMode = false
            settings.textZoom             = 100
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            // 2x DPR：540 CSS px × 2 ≈ 1080 物理 px
            setInitialScale((200f / density).toInt().coerceAtLeast(1))
        }
    }

    // ── 保存到系统相册 ────────────────────────────────────────────────────────────
    private fun saveToGalleryJpeg(ctx: Context, bitmap: Bitmap) {
        try {
            val name = "ACKS_Card_%d.jpg".format(System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ACKS Reader")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                uri?.let { ctx.contentResolver.openOutputStream(it) }
                    ?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0)
                uri?.let { ctx.contentResolver.update(it, cv, null, null) }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "ACKS Reader"
                ).apply { mkdirs() }
                File(dir, name).outputStream()
                    .use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            }
        } catch (_: Exception) {}
    }

    // ── 批量分享 ──────────────────────────────────────────────────────────────────
    fun shareCards(ctx: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(ctx, "studio.acks.reader.fileprovider", it)
        })
        val intent = if (uris.size == 1)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        else
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(Intent.createChooser(intent, "分享图文卡片"))
    }

    // ── evaluateJavascript 返回值 JSON 解码 ───────────────────────────────────────
    private fun decodeJsString(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("\"")) return trimmed
        return try {
            org.json.JSONArray("[$trimmed]").getString(0)
        } catch (_: Exception) {
            trimmed.removeSurrounding("\"")
                .replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("\\n", "\n").replace("\\r", "").replace("\\t", "\t")
        }
    }

    // ── JS 字符串转义 ─────────────────────────────────────────────────────────────
    private fun jsStr(s: String) = buildString {
        append('"')
        for (c in s) when (c) {
            '\\' -> append("\\\\"); '"' -> append("\\\"")
            '\n' -> append("\\n");  '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (c.code < 32) append("\\u%04X".format(c.code)) else append(c)
        }
        append('"')
    }
}
