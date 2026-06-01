package studio.acks.reader

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Wraps the render-core WebView. Loads assets/web/host.html once, then drives it
 * via ACKS.render() / ACKS.renderHtml(). Document → native messages arrive through
 * the AndroidBridge @JavascriptInterface.
 */
@SuppressLint("SetJavaScriptEnabled")
class PreviewWebView(
    context: Context,
    private val onReady: () -> Unit,
    private val onRenderDone: () -> Unit,
    private val onMessage: (AcksMessage) -> Unit
) {
    val webView: WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled  = true
            allowFileAccess    = true
            domStorageEnabled  = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort    = true
            loadWithOverviewMode = true
            textZoom           = 100
            cacheMode          = WebSettings.LOAD_NO_CACHE
        }
        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        webViewClient = WebViewClient()
        addJavascriptInterface(Bridge(), "AndroidBridge")
        loadUrl("file:///android_asset/web/host.html")
    }

    private var pending: DocState? = null
    private var ready = false

    fun render(state: DocState) {
        pending = state
        if (ready) doRender(state)
    }

    fun command(name: String, argJson: String = "{}") {
        webView.post {
            webView.evaluateJavascript(
                "ACKS.command(${jsonStr(name)}, ${jsonStr(argJson)})", null
            )
        }
    }

    private fun doRender(state: DocState) {
        val src  = jsonStr(state.markdownSource)
        val opts = jsonStr(state.renderOptsJson())
        webView.post {
            when {
                state.format == Format.HTML && state.htmlMode == "safe" ->
                    webView.evaluateJavascript("ACKS.renderHtml($src, true)", null)
                state.format == Format.HTML ->
                    webView.evaluateJavascript("ACKS.renderHtml($src, false)", null)
                else ->
                    webView.evaluateJavascript("ACKS.render($src, $opts)", null)
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            ready = true
            webView.post {
                this@PreviewWebView.onReady()
                pending?.let { doRender(it) }
            }
        }

        @JavascriptInterface
        fun onRenderDone() {
            webView.post { this@PreviewWebView.onRenderDone() }
        }

        @JavascriptInterface
        fun onMessage(json: String) {
            val kind = Regex(""""__acks"\s*:\s*"([^"]+)"""")
                .find(json)?.groupValues?.getOrNull(1) ?: return
            webView.post { this@PreviewWebView.onMessage(AcksMessage(kind, json)) }
        }
    }
}
