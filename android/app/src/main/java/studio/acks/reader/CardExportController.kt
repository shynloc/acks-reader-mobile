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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

data class CardExportOptions(
    val themeId: String = "aireport",
    val density: String = "balanced",   // "loose" | "balanced" | "dense"
    val withCover: Boolean = true,
    val fontSource: String = "local"
)

data class CardExportResult(
    val files: List<File>,
    val count: Int
)

object CardExportController {

    // Physical px: 1080×1440 (3:4 @ 2× density)
    private const val CARD_CSS_W = 540
    private const val CARD_CSS_H = 720
    private const val DENSITY   = 2

    suspend fun exportCards(
        ctx: Context,
        sourceWebView: WebView,
        markdown: String,
        opts: CardExportOptions,
        onProgress: (current: Int, total: Int) -> Unit
    ): CardExportResult = withContext(Dispatchers.Main) {

        // Step 1: call JS to get paginated page HTMLs
        val optsJson = buildOptsJson(opts)
        val pagesJson = suspendCancellableCoroutine<String> { cont ->
            sourceWebView.evaluateJavascript(
                "window.buildCardPages(${jsonStr(markdown)}, $optsJson)"
            ) { result ->
                cont.resume(result?.trim()?.removeSurrounding("\"")
                    ?.replace("\\\"", "\"")
                    ?.replace("\\\\", "\\") ?: "{\"pages\":[],\"count\":0}")
            }
        }

        val pagesObj = try { JSONObject(pagesJson) } catch (e: Exception) {
            return@withContext CardExportResult(emptyList(), 0)
        }
        val pages = pagesObj.optJSONArray("pages") ?: JSONArray()
        val total = pages.length()
        if (total == 0) return@withContext CardExportResult(emptyList(), 0)

        onProgress(0, total)

        // Step 2: for each page HTML, render in a dedicated WebView and capture
        val cacheDir = File(ctx.cacheDir, "card_export").apply { mkdirs() }
        val files = mutableListOf<File>()

        // Create a dedicated hidden WebView for card rendering
        val renderWebView = createRenderWebView(ctx)

        for (i in 0 until total) {
            val page = pages.optJSONObject(i) ?: continue
            val html = page.optString("html", "")
            if (html.isBlank()) continue

            val bitmap = renderCardBitmap(renderWebView, html)
            if (bitmap != null) {
                val file = File(cacheDir, "card_%03d.jpg".format(i + 1))
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                files.add(file)
                saveCardToGallery(ctx, bitmap, i + 1)
                bitmap.recycle()
            }
            onProgress(i + 1, total)
        }

        // Cleanup render WebView
        renderWebView.destroy()

        CardExportResult(files, files.size)
    }

    private fun buildOptsJson(opts: CardExportOptions): String =
        """{"themeId":${jsonStr(opts.themeId)},"density":${jsonStr(opts.density)},"withCover":${opts.withCover},"fontSource":${jsonStr(opts.fontSource)}}"""

    @Suppress("SetJavaScriptEnabled")
    private fun createRenderWebView(ctx: Context): WebView {
        return WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = false
            settings.textZoom = 100
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    private suspend fun renderCardBitmap(webView: WebView, html: String): Bitmap? =
        withContext(Dispatchers.Main) {
            val physW = CARD_CSS_W * DENSITY
            val physH = CARD_CSS_H * DENSITY

            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        // Give JS a moment to finish layout
                        view.postDelayed({
                            val bmp = captureBitmap(view, physW, physH)
                            cont.resume(bmp)
                        }, 350)
                    }
                }
                // Load HTML as data URI to allow local asset access
                webView.loadDataWithBaseURL(
                    "file:///android_asset/web/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }

    private fun captureBitmap(webView: WebView, w: Int, h: Int): Bitmap? {
        return try {
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, w, h)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            webView.draw(Canvas(bmp))
            bmp
        } catch (_: OutOfMemoryError) { null }
    }

    private fun saveCardToGallery(ctx: Context, bitmap: Bitmap, index: Int) {
        try {
            val filename = "ACKS_Card_%03d_%d.jpg".format(index, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ACKS Reader")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { ctx.contentResolver.openOutputStream(it) }?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                uri?.let { ctx.contentResolver.update(it, values, null, null) }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ACKS Reader")
                dir.mkdirs()
                File(dir, filename).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            }
        } catch (_: Exception) {}
    }

    fun shareCards(ctx: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(ctx, "studio.acks.reader.fileprovider", it)
        })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(Intent.createChooser(intent, "分享图文卡片"))
    }

    private fun jsonStr(s: String) = buildString {
        append('"')
        for (c in s) when (c) {
            '\\' -> append("\\\\"); '"' -> append("\\\"")
            '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> append(c)
        }
        append('"')
    }
}
