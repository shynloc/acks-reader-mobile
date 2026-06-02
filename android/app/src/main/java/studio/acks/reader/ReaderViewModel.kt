package studio.acks.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen { RECENT, PREVIEW, SETTINGS, ABOUT, CARD_PREVIEW }

enum class ActiveSheet { THEME, VIEWPORT, EXPORT, DOC_INFO, HTML_SAFETY, TOC, CARD_EXPORT }

sealed class ExportState {
    object Idle : ExportState()
    data class Running(val format: String, val progress: Float = 0f) : ExportState()
    data class Done(val files: List<File>, val format: String) : ExportState()
    data class Failed(val error: String) : ExportState()
}

data class SearchState(
    val isOpen: Boolean = false,
    val query: String = "",
    val current: Int = 0,
    val total: Int = 0
)

data class SettingsState(
    val defaultTheme: String = "aireport",
    val defaultViewport: String = "phone",
    val defaultHtmlMode: String = "safe",
    val fontScale: Float = 1.0f,
    val appTheme: String = "system",
    val enableMermaid: Boolean = true,
    val enableMath: Boolean = true,
    // 卡片导出参数（持久化）
    val cardFontSizePx: Float = 18f,
    val cardPadPx: Int = 28,
    val cardWithCover: Boolean = true,
    val cardThemeId: String = "aireport",
    // 字体来源
    val fontSourceOverride: String = "auto",   // "auto" | "local" | "cn_mirror"
    val resolvedFontSource: String = "auto"    // 当前实际生效的来源（用于状态显示）
)

data class CardExportState(
    val isRunning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val doneFiles: List<java.io.File> = emptyList(),
    val error: String? = null
)

data class CardPreviewState(
    val isLoading: Boolean = false,
    val previewFiles: List<java.io.File> = emptyList(),   // rendered JPEG files (preview)
    val currentPage: Int = 0,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportDone: Boolean = false,
    val exportError: String? = null
)

data class AppUiState(
    val screen: AppScreen = AppScreen.RECENT,
    val docState: DocState? = null,
    val recentDocs: List<DocRecord> = emptyList(),
    val isImporting: Boolean = false,
    val importError: String? = null,
    val activeSheet: ActiveSheet? = null,
    val exportState: ExportState = ExportState.Idle,
    val cardExportState: CardExportState = CardExportState(),
    val cardPreviewState: CardPreviewState = CardPreviewState(),
    val search: SearchState = SearchState(),
    val tocActiveHead: Int = 0,
    val settings: SettingsState = SettingsState(),
    val showOnboarding: Boolean = false
)

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val db       = DocDatabase.getInstance(app)
    private val repo     = DocRepository(db)
    private val settings = AppSettings(app)

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    @Volatile private var resolvedFontSource: String = FontManager.LOCAL

    init {
        loadRecent()
        resolveFontSource()
        observeSettings()
        checkFirstRun()
    }

    private fun observeSettings() {
        viewModelScope.launch { settings.defaultTheme.collect { v -> patchSettings { copy(defaultTheme = v) } } }
        viewModelScope.launch { settings.defaultViewport.collect { v -> patchSettings { copy(defaultViewport = v) } } }
        viewModelScope.launch { settings.defaultHtmlMode.collect { v -> patchSettings { copy(defaultHtmlMode = v) } } }
        viewModelScope.launch { settings.fontScale.collect { v -> patchSettings { copy(fontScale = v) } } }
        viewModelScope.launch { settings.appTheme.collect { v -> patchSettings { copy(appTheme = v) } } }
        viewModelScope.launch { settings.cardFontSizePx.collect { v -> patchSettings { copy(cardFontSizePx = v) } } }
        viewModelScope.launch { settings.cardPadPx.collect { v -> patchSettings { copy(cardPadPx = v.toInt()) } } }
        viewModelScope.launch { settings.cardWithCover.collect { v -> patchSettings { copy(cardWithCover = v) } } }
        viewModelScope.launch { settings.cardThemeId.collect { v -> patchSettings { copy(cardThemeId = v) } } }
        viewModelScope.launch { settings.fontSourceOverride.collect { v ->
            FontManager.setOverride(v)
            patchSettings { copy(fontSourceOverride = v) }
        } }
    }

    private inline fun patchSettings(block: SettingsState.() -> SettingsState) {
        _ui.value = _ui.value.copy(settings = _ui.value.settings.block())
    }

    private fun checkFirstRun() {
        viewModelScope.launch {
            val first = settings.isFirstRun.first()
            if (first) _ui.value = _ui.value.copy(showOnboarding = true)
        }
    }

    fun dismissOnboarding() {
        _ui.value = _ui.value.copy(showOnboarding = false)
        viewModelScope.launch { settings.markFirstRunDone() }
    }

    fun showOnboarding() {
        _ui.value = _ui.value.copy(showOnboarding = true)
    }

    private fun resolveFontSource() {
        viewModelScope.launch {
            resolvedFontSource = FontManager.resolve(getApplication())
            patchSettings { copy(resolvedFontSource = resolvedFontSource) }
            _ui.value.docState?.let { doc ->
                if (doc.fontSource != resolvedFontSource) {
                    _ui.value = _ui.value.copy(
                        docState = doc.copy(fontSource = resolvedFontSource, lifecycle = Lifecycle.RENDERING)
                    )
                }
            }
        }
    }

    private fun loadRecent() {
        viewModelScope.launch { _ui.value = _ui.value.copy(recentDocs = repo.getAll()) }
    }

    // ── File import ──────────────────────────────────────────────────────────

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isImporting = true, importError = null)
            try {
                val defaultTheme    = settings.defaultTheme.first()
                val defaultViewport = settings.defaultViewport.first()
                val defaultHtmlMode = settings.defaultHtmlMode.first()
                val fontScale       = settings.fontScale.first()
                val result = ImportController.import(getApplication(), uri)
                val docState = result.docState.copy(
                    themeId    = defaultTheme,
                    viewport   = defaultViewport,
                    htmlMode   = defaultHtmlMode,
                    fontSource = resolvedFontSource,
                    fontScale  = fontScale
                )
                repo.saveDoc(docState, result.sizeBytes)
                _ui.value = _ui.value.copy(
                    isImporting = false,
                    docState    = docState,
                    screen      = AppScreen.PREVIEW,
                    exportState = ExportState.Idle
                )
                loadRecent()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    isImporting = false,
                    importError = e.message ?: "Import failed"
                )
            }
        }
    }

    fun openRecent(record: DocRecord) {
        viewModelScope.launch {
            val file = File(record.sandboxPath)
            if (!file.exists()) {
                repo.delete(record)
                loadRecent()
                _ui.value = _ui.value.copy(importError = "文件已不存在")
                return@launch
            }
            try {
                val text      = file.readText()
                val fontScale = record.fontScale  // use persisted scale, not global default
                val fmt       = when (record.format) {
                    "html"     -> Format.HTML
                    "markdown" -> Format.MARKDOWN
                    else       -> Format.UNSUPPORTED
                }
                val doc = DocState(
                    id             = record.id,
                    title          = record.title,
                    format         = fmt,
                    sandboxPath    = record.sandboxPath,
                    markdownSource = text,
                    themeId        = record.lastThemeId,
                    mode           = record.lastMode,
                    viewport       = record.lastViewport,
                    customWidth    = record.lastCustomWidth,
                    htmlMode       = record.htmlMode,
                    fontSource     = resolvedFontSource,
                    fontScale      = fontScale,
                    lifecycle      = if (fmt == Format.UNSUPPORTED) Lifecycle.UNSUPPORTED else Lifecycle.RENDERING
                )
                _ui.value = _ui.value.copy(
                    docState    = doc,
                    screen      = AppScreen.PREVIEW,
                    activeSheet = null,
                    exportState = ExportState.Idle
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(importError = "无法打开文件: ${e.message}")
            }
        }
    }

    // ── DocState mutations ───────────────────────────────────────────────────

    private fun updateDoc(patch: DocState) {
        _ui.value = _ui.value.copy(docState = patch)
        viewModelScope.launch { repo.updateSettings(patch) }
    }

    fun setTheme(themeId: String) {
        val doc   = _ui.value.docState ?: return
        val meta  = AcksThemes.find(themeId) ?: return
        val modes = meta.modes
        val newMode = when {
            modes.size == 1 -> modes[0]
            "dark" in modes && "light" !in modes -> "dark"
            else -> doc.mode
        }
        updateDoc(doc.copy(themeId = themeId, mode = newMode))
    }

    fun setMode(mode: String) {
        _ui.value.docState?.let { updateDoc(it.copy(mode = mode)) }
    }

    fun setViewport(vp: String) {
        // No RENDERING state — WebView re-renders silently via LaunchedEffect
        _ui.value.docState?.let { updateDoc(it.copy(viewport = vp)) }
    }

    fun setCustomWidth(w: Int) {
        _ui.value.docState?.let { updateDoc(it.copy(customWidth = w)) }
    }

    fun setHtmlMode(mode: String) {
        _ui.value.docState?.let { updateDoc(it.copy(htmlMode = mode, lifecycle = Lifecycle.RENDERING)) }
    }

    // ── UI state ─────────────────────────────────────────────────────────────

    fun openSheet(s: ActiveSheet) { _ui.value = _ui.value.copy(activeSheet = s) }
    fun closeSheet() { _ui.value = _ui.value.copy(activeSheet = null) }

    fun goBack() {
        _ui.value = _ui.value.copy(screen = AppScreen.RECENT, activeSheet = null)
    }

    fun navToSettings() { _ui.value = _ui.value.copy(screen = AppScreen.SETTINGS) }

    fun navToAbout() { _ui.value = _ui.value.copy(screen = AppScreen.ABOUT) }

    fun navBack() {
        _ui.value = _ui.value.copy(screen = AppScreen.RECENT)
    }

    fun navBackFromAbout() {
        _ui.value = _ui.value.copy(screen = AppScreen.SETTINGS)
    }

    fun onRenderComplete() {
        val doc = _ui.value.docState ?: return
        if (doc.lifecycle == Lifecycle.RENDERING) {
            _ui.value = _ui.value.copy(docState = doc.copy(lifecycle = Lifecycle.RENDERED))
        }
    }

    fun deleteRecent(record: DocRecord) {
        viewModelScope.launch { repo.delete(record); loadRecent() }
    }

    fun clearAllDocs() {
        viewModelScope.launch {
            val all = repo.getAll()
            all.forEach { repo.delete(it) }
            loadRecent()
        }
    }

    fun setDefaultTheme(themeId: String) {
        viewModelScope.launch { settings.setDefaultTheme(themeId) }
    }

    fun setDefaultViewport(viewport: String) {
        viewModelScope.launch { settings.setDefaultViewport(viewport) }
    }

    fun setDefaultHtmlMode(mode: String) {
        viewModelScope.launch { settings.setDefaultHtmlMode(mode) }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch { settings.setAppTheme(theme) }
    }

    fun setEnableMermaid(enabled: Boolean) {
        _ui.value = _ui.value.copy(settings = _ui.value.settings.copy(enableMermaid = enabled))
    }

    fun setEnableMath(enabled: Boolean) {
        _ui.value = _ui.value.copy(settings = _ui.value.settings.copy(enableMath = enabled))
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settings.setFontScale(scale) }
        // Also apply to currently open doc immediately
        _ui.value.docState?.let { doc ->
            _ui.value = _ui.value.copy(docState = doc.copy(fontScale = scale))
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(importError = null) }

    fun setExportState(s: ExportState) { _ui.value = _ui.value.copy(exportState = s) }

    fun startCardExport(
        webView: android.webkit.WebView,
        opts: CardExportOptions
    ) {
        val markdown = _ui.value.docState?.markdownSource ?: return
        val ctx = getApplication<android.app.Application>()
        _ui.value = _ui.value.copy(
            cardExportState = CardExportState(isRunning = true),
            activeSheet = ActiveSheet.CARD_EXPORT
        )
        viewModelScope.launch {
            try {
                val result = CardExportController.exportCards(
                    ctx, webView, markdown,
                    opts.copy(fontSource = resolvedFontSource)
                ) { cur, total ->
                    _ui.value = _ui.value.copy(
                        cardExportState = _ui.value.cardExportState.copy(current = cur, total = total)
                    )
                }
                _ui.value = _ui.value.copy(
                    cardExportState = CardExportState(
                        isRunning = false,
                        doneFiles = result.files,
                        total = result.count
                    )
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    cardExportState = CardExportState(isRunning = false, error = e.message)
                )
            }
        }
    }

    fun clearCardExportState() {
        _ui.value = _ui.value.copy(cardExportState = CardExportState(), activeSheet = null)
    }

    // ── Card Preview ─────────────────────────────────────────────────────────

    @Volatile private var previewSourceWebView: java.lang.ref.WeakReference<android.webkit.WebView>? = null

    fun navToCardPreview(webView: android.webkit.WebView) {
        previewSourceWebView = java.lang.ref.WeakReference(webView)
        _ui.value = _ui.value.copy(
            screen = AppScreen.CARD_PREVIEW,
            activeSheet = null,
            cardPreviewState = CardPreviewState(isLoading = true)
        )
        generateCardPreviews()
    }

    fun setCardPreviewPage(page: Int) {
        _ui.value = _ui.value.copy(cardPreviewState = _ui.value.cardPreviewState.copy(currentPage = page))
    }

    fun setCardFontSize(px: Float) {
        viewModelScope.launch { settings.setCardFontSizePx(px) }
        regeneratePreview()
    }

    fun setCardPad(px: Int) {
        viewModelScope.launch { settings.setCardPadPx(px.toFloat()) }
        regeneratePreview()
    }

    fun setCardPreviewTheme(themeId: String) {
        viewModelScope.launch { settings.setCardThemeId(themeId) }
        regeneratePreview()
    }

    fun setCardPreviewWithCover(v: Boolean) {
        viewModelScope.launch { settings.setCardWithCover(v) }
        regeneratePreview()
    }

    private var previewJob: kotlinx.coroutines.Job? = null

    private fun regeneratePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)  // debounce
            generateCardPreviews()
        }
    }

    private fun generateCardPreviews() {
        val wv  = previewSourceWebView?.get() ?: return
        val md  = _ui.value.docState?.markdownSource ?: return
        val s   = _ui.value.settings
        val ctx = getApplication<android.app.Application>()
        val opts = CardExportOptions(
            themeId    = s.cardThemeId,
            withCover  = s.cardWithCover,
            fontSource = resolvedFontSource,
            fontSizePx = s.cardFontSizePx,
            padPx      = s.cardPadPx
        )
        _ui.value = _ui.value.copy(cardPreviewState = _ui.value.cardPreviewState.copy(isLoading = true))
        viewModelScope.launch {
            try {
                val result = CardExportController.exportCards(ctx, wv, md, opts, previewMode = true) { _, _ -> }
                _ui.value = _ui.value.copy(
                    cardPreviewState = _ui.value.cardPreviewState.copy(
                        isLoading = false,
                        previewFiles = result.files,
                        currentPage = 0
                    )
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    cardPreviewState = _ui.value.cardPreviewState.copy(isLoading = false)
                )
            }
        }
    }

    fun exportCardsFromPreview() {
        val wv  = previewSourceWebView?.get() ?: return
        val md  = _ui.value.docState?.markdownSource ?: return
        val s   = _ui.value.settings
        val ctx = getApplication<android.app.Application>()
        val opts = CardExportOptions(
            themeId    = s.cardThemeId,
            withCover  = s.cardWithCover,
            fontSource = resolvedFontSource,
            fontSizePx = s.cardFontSizePx,
            padPx      = s.cardPadPx
        )
        _ui.value = _ui.value.copy(
            cardPreviewState = _ui.value.cardPreviewState.copy(isExporting = true, exportProgress = 0f)
        )
        viewModelScope.launch {
            try {
                val result = CardExportController.exportCards(ctx, wv, md, opts, previewMode = false) { cur, total ->
                    val p = if (total > 0) cur.toFloat() / total else 0f
                    _ui.value = _ui.value.copy(
                        cardPreviewState = _ui.value.cardPreviewState.copy(exportProgress = p)
                    )
                }
                _ui.value = _ui.value.copy(
                    cardPreviewState = _ui.value.cardPreviewState.copy(
                        isExporting = false, exportDone = true
                    )
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    cardPreviewState = _ui.value.cardPreviewState.copy(
                        isExporting = false, exportError = e.message
                    )
                )
            }
        }
    }

    fun clearCardPreview() {
        previewSourceWebView = null
        _ui.value = _ui.value.copy(
            screen = AppScreen.PREVIEW,
            cardPreviewState = CardPreviewState()
        )
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun setFontSourceOverride(v: String) {
        viewModelScope.launch {
            settings.setFontSourceOverride(v)
            patchSettings { copy(fontSourceOverride = v) }
            // 重新解析字体源并应用到当前文档
            val newSource = FontManager.resolve(getApplication())
            resolvedFontSource = newSource
            patchSettings { copy(resolvedFontSource = newSource) }
            _ui.value.docState?.let { doc ->
                _ui.value = _ui.value.copy(docState = doc.copy(fontSource = newSource, lifecycle = Lifecycle.RENDERING))
            }
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    fun openSearch() { _ui.value = _ui.value.copy(search = SearchState(isOpen = true)) }

    fun closeSearch() { _ui.value = _ui.value.copy(search = SearchState(isOpen = false)) }

    fun setSearchQuery(q: String) {
        _ui.value = _ui.value.copy(search = _ui.value.search.copy(query = q))
    }

    // ── WebView message handler ──────────────────────────────────────────────

    fun handleAcksMessage(msg: AcksMessage) {
        when (msg.kind) {
            "searchres" -> {
                val total = Regex(""""count"\s*:\s*(\d+)""").find(msg.raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val cur   = Regex(""""cur"\s*:\s*(\d+)""").find(msg.raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                _ui.value = _ui.value.copy(search = _ui.value.search.copy(current = cur, total = total))
            }
            "activehead" -> {
                val i = Regex(""""i"\s*:\s*(\d+)""").find(msg.raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                _ui.value = _ui.value.copy(tocActiveHead = i)
            }
        }
    }
}
