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
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.min

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

    private const val CARD_CSS_W = 540   // 软件 WebView CSS 像素宽（= 物理像素，无 density 缩放）
    private const val CARD_CSS_H = 720   // 每张卡片高度（CSS px = 物理 px）
    private const val OUT_W = 1080       // 输出 JPEG 物理宽
    private const val OUT_H = 1440       // 输出 JPEG 物理高

    /**
     * 长图切片方案：
     * 1. 调用 sourceWebView JS 生成包含封面 + 内容的单张连续 HTML
     * 2. 软件 WebView 渲染该 HTML，截取全长 Bitmap
     * 3. 按 CARD_CSS_H 切片，缩放至 OUT_W×OUT_H，保存相册
     */
    suspend fun exportCards(
        ctx: Context,
        sourceWebView: WebView,
        markdown: String,
        opts: CardExportOptions,
        onProgress: (current: Int, total: Int) -> Unit
    ): CardExportResult = withContext(Dispatchers.Main) {

        // ── Step 1: 存 markdown 到 sourceWebView JS 上下文 ───────────────────
        suspendCancellableCoroutine<Unit> { cont ->
            sourceWebView.evaluateJavascript(
                "window.__acksCardMd = ${jsStr(markdown)}; null"
            ) { cont.resume(Unit) }
        }

        // ── Step 2: 生成长图 HTML ─────────────────────────────────────────────
        val optsJs = """{"themeId":${jsStr(opts.themeId)},"density":${jsStr(opts.density)},"withCover":${opts.withCover},"fontSource":${jsStr(opts.fontSource)}}"""
        val longHtml = suspendCancellableCoroutine<String> { cont ->
            sourceWebView.evaluateJavascript(
                "(function(){ try{ return window.buildLongDocForSlice(window.__acksCardMd,$optsJs); }catch(e){ return ''; } })()"
            ) { raw -> cont.resume(decodeJsString(raw ?: "\"\"")) }
        }
        if (longHtml.isBlank()) return@withContext CardExportResult(emptyList(), 0)

        // ── Step 3: 获取主题色（末页填充 + 页码颜色）────────────────────────
        val colorsJson = suspendCancellableCoroutine<String> { cont ->
            sourceWebView.evaluateJavascript(
                "window.getThemeSwatchColors(${jsStr(opts.themeId)})"
            ) { raw -> cont.resume(decodeJsString(raw ?: "\"{}\"")) }
        }
        val bgColor = parseHexColor(colorsJson, "bg", 0xFF0D0F1A.toInt())
        val fgColor = parseHexColor(colorsJson, "fg", 0xFFFFFFFF.toInt())

        // ── Step 4: 软件 WebView 渲染长图 HTML ───────────────────────────────
        // 关键：初始高度必须足够大，让内容一次全量渲染；
        // 之后只截取 actualH 部分，不能在截图前再 re-measure（会导致重排但 draw 来不及等渲染器刷新）。
        val MAX_H = 20_000
        val renderWv = createSoftwareWebView(ctx)
        renderWv.measure(
            View.MeasureSpec.makeMeasureSpec(CARD_CSS_W, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(MAX_H, View.MeasureSpec.EXACTLY)
        )
        renderWv.layout(0, 0, CARD_CSS_W, MAX_H)

        val loaded = suspendCancellableCoroutine<Boolean> { cont ->
            var done = false
            renderWv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (!done) { done = true; cont.resume(true) }
                }
                override fun onReceivedError(
                    view: WebView, errorCode: Int, description: String?, url: String?
                ) {
                    if (!done) { done = true; cont.resume(false) }
                }
            }
            renderWv.loadDataWithBaseURL(
                "file:///android_asset/web/",
                longHtml, "text/html", "UTF-8", null
            )
        }
        if (!loaded) { renderWv.destroy(); return@withContext CardExportResult(emptyList(), 0) }

        // 等字体 / CSS 应用完成（本地字体 450ms 足够，如遇网络字体可适当加长）
        delay(450)
        onProgress(0, -1)

        // ── Step 5: 查询实际内容高度，截取对应部分 ───────────────────────────
        // WebView 已按 MAX_H 全量渲染；draw() 从顶部开始，canvas 高度 = actualH 自动截断末尾空白
        val scrollCss = suspendCancellableCoroutine<Int> { cont ->
            renderWv.evaluateJavascript(
                "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)"
            ) { r -> cont.resume(r?.trim()?.toIntOrNull() ?: renderWv.contentHeight) }
        }
        val totalH = scrollCss.coerceIn(CARD_CSS_H, MAX_H)

        val longBitmap = try {
            Bitmap.createBitmap(CARD_CSS_W, totalH, Bitmap.Config.ARGB_8888)
                .also { renderWv.draw(Canvas(it)) }
        } catch (_: OutOfMemoryError) {
            renderWv.destroy()
            return@withContext CardExportResult(emptyList(), 0)
        }
        renderWv.destroy()

        // ── Step 6: 按 CARD_CSS_H 切片、缩放、保存 ──────────────────────────
        val totalCards = ceil(totalH.toFloat() / CARD_CSS_H).toInt()
        val cacheDir   = File(ctx.cacheDir, "card_export").apply { mkdirs() }
        val files      = mutableListOf<File>()

        for (i in 0 until totalCards) {
            val top    = i * CARD_CSS_H
            val sliceH = min(CARD_CSS_H, totalH - top)

            val slice = try {
                Bitmap.createBitmap(longBitmap, 0, top, CARD_CSS_W, sliceH)
            } catch (_: OutOfMemoryError) { onProgress(i + 1, totalCards); continue }

            // 末页不足一张时用主题背景色填充
            val card = if (sliceH < CARD_CSS_H) {
                try {
                    Bitmap.createBitmap(CARD_CSS_W, CARD_CSS_H, Bitmap.Config.ARGB_8888).also {
                        Canvas(it).apply { drawColor(bgColor); drawBitmap(slice, 0f, 0f, null) }
                    }
                } catch (_: OutOfMemoryError) { slice }
                    .also { if (it !== slice) slice.recycle() }
            } else slice

            // 2× 双线性缩放至 1080×1440
            val scaled = try {
                Bitmap.createScaledBitmap(card, OUT_W, OUT_H, true)
            } catch (_: OutOfMemoryError) { card }
            if (scaled !== card) card.recycle()

            // 在缩放后的 bitmap 上叠加页码
            drawPageNumber(scaled, i + 1, totalCards, fgColor)

            val file = File(cacheDir, "card_%03d.jpg".format(i + 1))
            file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            files.add(file)
            saveToGallery(ctx, scaled, i + 1)
            scaled.recycle()

            onProgress(i + 1, totalCards)
        }

        longBitmap.recycle()
        CardExportResult(files, files.size)
    }

    // ── 页码绘制（叠加在 OUT_W×OUT_H bitmap 右下角）──────────────────────────────
    private fun drawPageNumber(bitmap: Bitmap, index: Int, total: Int, fgColor: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            alpha = (0.38f * 255).toInt()
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }
        Canvas(bitmap).drawText("$index / $total", (OUT_W - 36).toFloat(), (OUT_H - 28).toFloat(), paint)
    }

    // ── 解析 JSON 颜色字段（如 {"bg":"#0D0F1A","fg":"#FFFFFF"}）────────────────────
    private fun parseHexColor(json: String, key: String, default: Int): Int {
        val m = Regex(""""$key"\s*:\s*"(#[0-9A-Fa-f]{6,8})"""").find(json)
        return m?.groupValues?.getOrNull(1)?.let {
            try { android.graphics.Color.parseColor(it) } catch (_: Exception) { null }
        } ?: default
    }

    // ── 软件渲染 WebView（不依赖 Window / GPU 也能 draw）────────────────────────────
    @Suppress("SetJavaScriptEnabled")
    private fun createSoftwareWebView(ctx: Context) = WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        settings.textZoom = 100
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    // ── 保存到系统相册 ─────────────────────────────────────────────────────────────
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
