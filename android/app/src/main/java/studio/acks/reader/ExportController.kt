package studio.acks.reader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.WebView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.min

object ExportController {

    /**
     * Exports WebView content as a PDF file (A4 width, full document height).
     * [viewportCssPx]: the CSS pixel width of the document content (null = device width).
     */
    suspend fun exportToPdf(
        ctx: Context,
        webView: WebView,
        title: String,
        viewportCssPx: Int? = null,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.Main) {
        onProgress(0.1f)

        val density  = ctx.resources.displayMetrics.density
        val contentW = viewportCssPx?.let { (it * density).toInt() }
            ?: webView.measuredWidth.takeIf { it > 0 }
            ?: (390 * density).toInt()

        val scrollCss = suspendCancellableCoroutine<Int> { cont ->
            webView.evaluateJavascript(
                "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)"
            ) { r -> cont.resume(r?.trim()?.toIntOrNull() ?: webView.contentHeight) }
        }
        val contentH = (scrollCss * density).toInt().coerceIn(100, 30_000)

        onProgress(0.3f)

        val a4W   = 595
        val scale = a4W.toFloat() / contentW
        val pdfH  = (contentH * scale).toInt().coerceAtLeast(1)

        val savedW         = webView.measuredWidth
        val savedH         = webView.measuredHeight
        val savedLayerType = webView.layerType

        // 切换软件渲染：硬件渲染只维护可视区域 tile，draw() 无法捕获屏幕外内容
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(contentW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(contentH, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, contentW, contentH)
        delay(400) // 等 renderer 以新尺寸完成 layout
        onProgress(0.55f)

        val pdf      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(a4W, pdfH, 1).create()
        val page     = pdf.startPage(pageInfo)
        page.canvas.scale(scale, scale)
        webView.draw(page.canvas)
        pdf.finishPage(page)

        // 恢复原始尺寸和渲染模式
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(savedW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(savedH, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, savedW, savedH)
        webView.setLayerType(savedLayerType, null)
        onProgress(0.85f)

        val outDir  = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: ctx.filesDir
        outDir.mkdirs()
        val safe    = title.substringBeforeLast('.').replace(Regex("[^\\w\\s\\-]"), "").trim().take(40).ifBlank { "document" }
        val outFile = File(outDir, "${safe}_${System.currentTimeMillis()}.pdf")
        outFile.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        onProgress(1f)
        outFile
    }

    /**
     * Captures WebView as PNG long image(s), splitting into segments to avoid OOM.
     * Returns a list of files (one per segment). For short docs, the list has one element.
     * Each segment is also saved to the system gallery.
     */
    suspend fun captureToImage(
        ctx: Context,
        webView: WebView,
        viewportCssPx: Int? = null,
        onProgress: (Float) -> Unit
    ): List<File> = withContext(Dispatchers.Main) {
        onProgress(0.1f)

        val density  = ctx.resources.displayMetrics.density
        val widthPx  = viewportCssPx?.let { (it * density).toInt() }
            ?: webView.measuredWidth.takeIf { it > 0 }
            ?: (390 * density).toInt()

        val scrollCss = suspendCancellableCoroutine<Int> { cont ->
            webView.evaluateJavascript(
                "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)"
            ) { r -> cont.resume(r?.trim()?.toIntOrNull() ?: webView.contentHeight) }
        }
        // 全高（物理 px），最多 80 000 px（约 26 屏 @ density 3）
        val totalPhysPx = (scrollCss * density).toInt().coerceIn(100, 80_000)
        onProgress(0.2f)

        val savedW         = webView.measuredWidth
        val savedH         = webView.measuredHeight
        val savedLayerType = webView.layerType

        // 切换软件渲染：draw() 同步绘制完整 DOM，分段截取时靠 canvas.translate 偏移
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(totalPhysPx, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, widthPx, totalPhysPx)
        delay(400)

        // ── 查询块底部位置，用于内容感知截断 ──────────────────────────────────
        val blockEndsJson = suspendCancellableCoroutine<String> { cont ->
            webView.evaluateJavascript("""
                (function(){
                  try{
                    var els=Array.from(document.querySelectorAll('.md-content > *, .acks-content > *'));
                    if(!els.length) els=Array.from(document.querySelectorAll('body > *'));
                    var bots=els.map(function(e){
                      return Math.round(e.getBoundingClientRect().bottom+window.scrollY);
                    }).filter(function(b){return b>0;});
                    bots.sort(function(a,b){return a-b;});
                    var r=[];bots.forEach(function(b){if(!r.length||r[r.length-1]!==b)r.push(b);});
                    return JSON.stringify(r);
                  }catch(e){return '[]';}
                })()
            """.trimIndent()) { r -> cont.resume(r?.trim()?.removeSurrounding("\"")
                ?.replace("\\\"","\"") ?: "[]") }
        }
        val safeBlockEndsCss: List<Int> = try {
            val arr = org.json.JSONArray(blockEndsJson)
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (_: Exception) { emptyList() }
        // 转换为物理 px
        val safeEnds = safeBlockEndsCss.map { (it * density).toInt() }.sorted()

        // 最大段 5 000 物理 px，容差 1 000 px 内找块边界；找不到再硬截
        val maxSegPx  = 5_000
        val tolerance = 1_000
        val breakPts  = buildBreakPoints(totalPhysPx, maxSegPx, tolerance, safeEnds)
        val numSegs   = breakPts.size
        val cacheDir  = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val files     = mutableListOf<File>()

        for (i in breakPts.indices) {
            val top  = if (i == 0) 0 else breakPts[i - 1]
            val segH = breakPts[i] - top
            if (segH <= 0) continue

            val bitmap = try {
                Bitmap.createBitmap(widthPx, segH, Bitmap.Config.ARGB_8888)
            } catch (_: OutOfMemoryError) {
                onProgress(0.25f + 0.7f * (i + 1).toFloat() / numSegs)
                continue
            }

            // 通过 translate 让 draw() 只画这一段内容
            val canvas = Canvas(bitmap)
            canvas.translate(0f, -top.toFloat())
            webView.draw(canvas)

            val suffix    = if (numSegs == 1) "" else "_%02d".format(i + 1)
            val cacheFile = File(cacheDir, "acks${suffix}_${System.currentTimeMillis()}.png")
            cacheFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 92, it) }
            saveToGallery(ctx, bitmap, if (numSegs == 1) null else i + 1)
            bitmap.recycle()
            files.add(cacheFile)

            onProgress(0.25f + 0.7f * (i + 1).toFloat() / numSegs)
        }

        // 恢复原始尺寸和渲染模式
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(savedW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(savedH, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, savedW, savedH)
        webView.setLayerType(savedLayerType, null)
        onProgress(1f)
        files
    }

    /** Saves a bitmap to the system gallery (MediaStore). index=null → no suffix. */
    private fun saveToGallery(ctx: Context, bitmap: Bitmap, index: Int? = null) {
        try {
            val filename = if (index == null) "ACKS_${System.currentTimeMillis()}.png"
                           else "ACKS_%02d_%d.png".format(index, System.currentTimeMillis())
            val stream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ACKS Reader")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                stream  = uri?.let { ctx.contentResolver.openOutputStream(it) }
                stream?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 92, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                uri?.let { ctx.contentResolver.update(it, values, null, null) }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ACKS Reader")
                dir.mkdirs()
                val file = File(dir, filename)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 92, it) }
                ctx.sendBroadcast(Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                    android.net.Uri.fromFile(file)))
            }
        } catch (_: Exception) {}
    }

    /** Share one file via system share sheet using FileProvider. */
    fun shareFile(ctx: Context, file: File, mimeType: String = "image/png") {
        val uri = FileProvider.getUriForFile(ctx, "studio.acks.reader.fileprovider", file)
        ctx.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }, null
        ))
    }

    /**
     * 计算段落感知的截断点列表。
     * 在每个 maxSegPx 间隔内，优先找 [idealEnd-tolerance, idealEnd] 范围内的块底边；
     * 找不到就用硬截点，保证段高 ≤ maxSegPx + tolerance。
     */
    private fun buildBreakPoints(
        totalPx: Int,
        maxSegPx: Int,
        tolerance: Int,
        safeBlockEnds: List<Int>
    ): List<Int> {
        val breaks = mutableListOf<Int>()
        var cursor = 0
        while (cursor < totalPx) {
            val idealEnd = (cursor + maxSegPx).coerceAtMost(totalPx)
            if (idealEnd >= totalPx) { breaks.add(totalPx); break }

            // 优先：在 [idealEnd - tolerance, idealEnd] 内最后一个块底边
            val safe = safeBlockEnds.lastOrNull { it in (idealEnd - tolerance)..idealEnd }
            // 次选：idealEnd 之后最近的块底边（稍微延长一点而不是截文字）
                ?: safeBlockEnds.firstOrNull { it > idealEnd && it <= idealEnd + tolerance / 2 }
                ?: idealEnd  // 兜底：硬截

            breaks.add(safe)
            cursor = safe
        }
        return breaks
    }

    /** Share multiple files (e.g., long-image segments) via system share sheet. */
    fun shareFiles(ctx: Context, files: List<File>, mimeType: String = "image/png") {
        if (files.isEmpty()) return
        if (files.size == 1) { shareFile(ctx, files[0], mimeType); return }
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(ctx, "studio.acks.reader.fileprovider", it)
        })
        ctx.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }, null
        ))
    }
}
