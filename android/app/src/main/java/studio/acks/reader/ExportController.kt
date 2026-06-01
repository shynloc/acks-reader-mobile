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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.resume

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

        val density   = ctx.resources.displayMetrics.density
        // Use viewport-specified width if set, otherwise device width
        val contentW  = viewportCssPx?.let { (it * density).toInt() }
            ?: webView.measuredWidth.takeIf { it > 0 }
            ?: (390 * density).toInt()

        val scrollCss = suspendCancellableCoroutine<Int> { cont ->
            webView.evaluateJavascript(
                "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)"
            ) { r -> cont.resume(r?.trim()?.toIntOrNull() ?: webView.contentHeight) }
        }
        val contentH = (scrollCss * density).toInt().coerceIn(100, 30_000)

        onProgress(0.3f)

        // A4 = 595 pt wide; scale document to fit
        val a4W   = 595
        val scale = a4W.toFloat() / contentW
        val pdfH  = (contentH * scale).toInt().coerceAtLeast(1)

        val savedW = webView.measuredWidth; val savedH = webView.measuredHeight
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(contentW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(contentH, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, contentW, contentH)
        onProgress(0.55f)

        val pdf      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(a4W, pdfH, 1).create()
        val page     = pdf.startPage(pageInfo)
        page.canvas.scale(scale, scale)
        webView.draw(page.canvas)
        pdf.finishPage(page)

        webView.measure(
            View.MeasureSpec.makeMeasureSpec(savedW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(savedH, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, savedW, savedH)
        onProgress(0.85f)

        val outDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: ctx.filesDir
        outDir.mkdirs()
        val safe    = title.substringBeforeLast('.').replace(Regex("[^\\w\\s\\-]"), "").trim().take(40).ifBlank { "document" }
        val outFile = File(outDir, "${safe}_${System.currentTimeMillis()}.pdf")
        outFile.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        onProgress(1f)
        outFile
    }

    /**
     * Captures WebView as a PNG long image.
     * [viewportCssPx]: the CSS pixel width to render at (null = current WebView width).
     * Also saves to system gallery (Pictures/ACKS Reader) if possible.
     */
    suspend fun captureToImage(
        ctx: Context,
        webView: WebView,
        viewportCssPx: Int? = null,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.Main) {
        onProgress(0.1f)

        val density   = ctx.resources.displayMetrics.density
        val widthPx   = viewportCssPx?.let { (it * density).toInt() }
            ?: webView.measuredWidth.takeIf { it > 0 }
            ?: (390 * density).toInt()

        val scrollCss = suspendCancellableCoroutine<Int> { cont ->
            webView.evaluateJavascript(
                "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)"
            ) { r -> cont.resume(r?.trim()?.toIntOrNull() ?: webView.contentHeight) }
        }
        val heightPx = (scrollCss * density).toInt().coerceIn(100, 15_000)

        onProgress(0.25f)

        val bitmap = try {
            Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) { return@withContext null }

        onProgress(0.45f)

        val savedW = webView.measuredWidth; val savedH = webView.measuredHeight
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, widthPx, heightPx)
        webView.draw(Canvas(bitmap))
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(savedW, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(savedH, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, savedW, savedH)
        onProgress(0.7f)

        // Save to cache for share
        val cacheDir  = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(cacheDir, "acks_${System.currentTimeMillis()}.png")
        cacheFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 92, it) }

        // Also save to system gallery (Pictures/ACKS Reader)
        saveToGallery(ctx, bitmap)

        bitmap.recycle()
        onProgress(1f)
        cacheFile
    }

    /** Saves a bitmap to the system gallery (MediaStore). */
    private fun saveToGallery(ctx: Context, bitmap: Bitmap) {
        try {
            val filename = "ACKS_${System.currentTimeMillis()}.png"
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
                // Notify gallery
                ctx.sendBroadcast(Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                    android.net.Uri.fromFile(file)))
            }
        } catch (_: Exception) {}  // gallery save is best-effort
    }

    /** Share a file via system share sheet using FileProvider. */
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
}
