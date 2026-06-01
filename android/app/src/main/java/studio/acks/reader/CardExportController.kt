package studio.acks.reader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
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
import java.io.File
import kotlin.coroutines.resume

data class CardExportOptions(
    val themeId: String = "aireport",
    val density: String = "balanced",
    val withCover: Boolean = true,
    val fontSource: String = "local"
)

data class CardExportResult(
    val files: List<File>,
    val count: Int
)

object CardExportController {

    private const val CARD_CSS_W = 540   // CSS px = render WebView 物理宽度
    private const val CARD_CSS_H = 720   // CSS px = render WebView 物理高度
    private const val OUT_W = 1080       // 输出 JPEG 物理宽度（2x upscale）
    private const val OUT_H = 1440       // 输出 JPEG 物理高度

    suspend fun exportCards(
        ctx: Context,
        sourceWebView: WebView,
        markdown: String,
        opts: CardExportOptions,
        onProgress: (current: Int, total: Int) -> Unit
    ): CardExportResult = withContext(Dispatchers.Main) {

        // ── Step 1: 把 markdown 存入 JS 变量（避免单次传大字符串）────────────────
        suspendCancellableCoroutine<Unit> { cont ->
            sourceWebView.evaluateJavascript(
                "window.__acksCardMd = ${jsStr(markdown)}; null"
            ) { cont.resume(Unit) }
        }

        // ── Step 2: 调用 buildCardPages，返回卡片数量整数 ────────────────────────
        val optsJs = """{"themeId":${jsStr(opts.themeId)},"density":${jsStr(opts.density)},"withCover":${opts.withCover},"fontSource":${jsStr(opts.fontSource)}}"""
        val countStr = suspendCancellableCoroutine<String> { cont ->
            sourceWebView.evaluateJavascript(
                "(function(){ try{ return String(window.buildCardPages(window.__acksCardMd,$optsJs)); }catch(e){ return '0'; } })()"
            ) { result ->
                cont.resume(result?.trim()?.removeSurrounding("\"") ?: "0")
            }
        }
        val total = countStr.toIntOrNull() ?: 0
        if (total == 0) return@withContext CardExportResult(emptyList(), 0)

        onProgress(0, total)

        // ── Step 3: 创建渲染用 WebView（软件渲染，CSS 像素尺寸，无需挂载 Window）──
        // 关键：用 CSS 像素（540×720）而非物理像素（1080×1440）
        // 这样 viewport width=540,initial-scale=1 能精确匹配，不受设备 density 干扰
        val renderWv = createSoftwareWebView(ctx)
        renderWv.measure(
            View.MeasureSpec.makeMeasureSpec(CARD_CSS_W, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(CARD_CSS_H, View.MeasureSpec.EXACTLY)
        )
        renderWv.layout(0, 0, CARD_CSS_W, CARD_CSS_H)

        val cacheDir = File(ctx.cacheDir, "card_export").apply { mkdirs() }
        val files = mutableListOf<File>()

        // ── Step 4: 逐张拉 HTML → 渲染 → 截图 ──────────────────────────────────
        for (i in 0 until total) {
            // 从 JS 缓存逐张拉取 HTML（每次回传一张，避免大 JSON 问题）
            val html = suspendCancellableCoroutine<String> { cont ->
                sourceWebView.evaluateJavascript(
                    "window.getCardPageHtml($i) || ''"
                ) { raw ->
                    // evaluateJavascript 把字符串结果包在 JSON 字符串引号里，需要解码
                    val decoded = decodeJsString(raw ?: "\"\"")
                    cont.resume(decoded)
                }
            }
            if (html.isBlank()) { onProgress(i + 1, total); continue }

            val bitmap = renderCard(renderWv, html)
            if (bitmap != null) {
                val file = File(cacheDir, "card_%03d.jpg".format(i + 1))
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                files.add(file)
                saveToGallery(ctx, bitmap, i + 1)
                bitmap.recycle()
            }
            onProgress(i + 1, total)
        }

        renderWv.destroy()
        CardExportResult(files, files.size)
    }

    // ── 软件渲染 WebView（不需要 Window 也能 draw）────────────────────────────────
    @Suppress("SetJavaScriptEnabled")
    private fun createSoftwareWebView(ctx: Context) = WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        settings.textZoom = 100
        // 软件渲染：不需要 GPU/Window，draw() 直接可用
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    // ── 渲染单张卡片，返回 1080×1440 bitmap ──────────────────────────────────────
    private suspend fun renderCard(wv: WebView, html: String): Bitmap? =
        withContext(Dispatchers.Main) {
            val loaded = suspendCancellableCoroutine<Boolean> { cont ->
                var done = false
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (!done) { done = true; cont.resume(true) }
                    }
                    override fun onReceivedError(
                        view: WebView, errorCode: Int, description: String?, url: String?
                    ) {
                        if (!done) { done = true; cont.resume(false) }
                    }
                }
                wv.loadDataWithBaseURL(
                    "file:///android_asset/web/",
                    html, "text/html", "UTF-8", null
                )
            }
            if (!loaded) return@withContext null

            // 等 CSS / 字体应用完成
            delay(350)

            // re-measure 确保字体加载后布局正确（仍用 CSS 像素）
            wv.measure(
                View.MeasureSpec.makeMeasureSpec(CARD_CSS_W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(CARD_CSS_H, View.MeasureSpec.EXACTLY)
            )
            wv.layout(0, 0, CARD_CSS_W, CARD_CSS_H)

            // 在 CSS 尺寸（540×720）上截图，然后 scale 到 1080×1440
            val raw = try {
                Bitmap.createBitmap(CARD_CSS_W, CARD_CSS_H, Bitmap.Config.ARGB_8888)
                    .also { wv.draw(Canvas(it)) }
            } catch (_: OutOfMemoryError) { return@withContext null }

            // 高质量双线性 upscale → 1080×1440
            val scaled = try {
                Bitmap.createScaledBitmap(raw, OUT_W, OUT_H, true)
            } catch (_: OutOfMemoryError) { raw }
            if (scaled !== raw) raw.recycle()
            scaled
        }

    // ── 保存到系统相册 ────────────────────────────────────────────────────────────
    private fun saveToGallery(ctx: Context, bitmap: Bitmap, index: Int) {
        try {
            val name = "ACKS_Card_%03d_%d.jpg".format(index, System.currentTimeMillis())
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
            Intent(Intent.ACTION_SEND).apply { type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uris[0]) }
        else
            Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "image/jpeg"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(Intent.createChooser(intent, "分享图文卡片"))
    }

    // ── evaluateJavascript 返回字符串时带 JSON 引号，需要解码 ──────────────────────
    private fun decodeJsString(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("\"")) return trimmed
        return try {
            // 用 JSONArray 解析外层引号（最稳定）
            org.json.JSONArray("[$trimmed]").getString(0)
        } catch (_: Exception) {
            trimmed.removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "")
                .replace("\\t", "\t")
        }
    }

    // ── JS 字符串转义（用于 evaluateJavascript 参数） ─────────────────────────────
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
