package studio.acks.reader.ui

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import studio.acks.reader.*
import studio.acks.reader.ui.sheets.*

@Composable
fun PreviewScreen(state: AppUiState, vm: ReaderViewModel) {
    val doc   = state.docState ?: return
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewHost by remember { mutableStateOf<PreviewWebView?>(null) }

    // Helper to send WebView commands
    fun cmd(name: String, arg: String = "{}") = previewHost?.command(name, arg)

    BackHandler {
        when {
            state.search.isOpen -> { cmd("clearfind"); vm.closeSearch() }
            state.activeSheet != null -> vm.closeSheet()
            else -> vm.goBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AcksBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            PreviewTopBar(doc = doc, onBack = { vm.goBack() }, onInfo = { vm.openSheet(ActiveSheet.DOC_INFO) })

            // ── Search bar (slides in below top bar) ──────────────────────
            AnimatedVisibility(
                visible = state.search.isOpen,
                enter   = slideInVertically { -it } + fadeIn(),
                exit    = slideOutVertically { -it } + fadeOut()
            ) {
                SearchBar(
                    search    = state.search,
                    onQuery   = { q ->
                        vm.setSearchQuery(q)
                        cmd("search", """{"q":${jsonStr(q)}}""")
                    },
                    onNav     = { dir -> cmd("findnav", """{"dir":$dir}""") },
                    onClose   = { cmd("clearfind"); vm.closeSearch() }
                )
            }

            // ── Main content ──────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when (doc.lifecycle) {
                    Lifecycle.UNSUPPORTED -> UnsupportedContent(onBack = { vm.goBack() })
                    Lifecycle.ERROR       -> ErrorContent()
                    Lifecycle.CORRUPTED   -> CorruptedContent(onBack = { vm.goBack() })
                    else -> DocWebView(
                        state         = doc,
                        onHostCreated = { previewHost = it },
                        onRenderDone  = {
                            vm.onRenderComplete()
                            previewHost?.command("trackHeadings")
                        },
                        onMessage     = { vm.handleAcksMessage(it) }
                    )
                }

                if (doc.lifecycle == Lifecycle.LOADING || doc.lifecycle == Lifecycle.RENDERING) {
                    LoadingSkeleton()
                }

                if (doc.lifecycle == Lifecycle.LARGE && doc.lifecycle != Lifecycle.RENDERING) {
                    LargeFileBanner(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    )
                }

                if (doc.format == Format.HTML && doc.htmlMode == "safe" && doc.lifecycle == Lifecycle.RENDERED) {
                    SafeModeBanner(
                        onEnable    = { vm.setHtmlMode("interactive") },
                        onLearnMore = { vm.openSheet(ActiveSheet.HTML_SAFETY) },
                        modifier    = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    )
                }

                (state.exportState as? ExportState.Running)?.let { ExportProgressOverlay(it) }
            }

            // ── Bottom toolbar ────────────────────────────────────────────
            if (doc.lifecycle != Lifecycle.UNSUPPORTED) {
                BottomToolbar(
                    doc        = doc,
                    isSearchOn = state.search.isOpen,
                    onTheme    = { vm.openSheet(ActiveSheet.THEME) },
                    onViewport = { vm.openSheet(ActiveSheet.VIEWPORT) },
                    onExport   = { vm.openSheet(ActiveSheet.EXPORT) },
                    onSafety   = { if (doc.format == Format.HTML) vm.openSheet(ActiveSheet.HTML_SAFETY) },
                    onToc      = { vm.openSheet(ActiveSheet.TOC) },
                    onSearch   = {
                        if (state.search.isOpen) { cmd("clearfind"); vm.closeSearch() }
                        else vm.openSearch()
                    }
                )
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        (state.exportState as? ExportState.Done)?.let { done ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = {
                        if (done.format == "pdf")
                            ExportController.shareFile(ctx, done.files.first(), "application/pdf")
                        else
                            ExportController.shareFiles(ctx, done.files, "image/png")
                        vm.setExportState(ExportState.Idle)
                    }) { Text("分享", color = AcksAccent) }
                },
                dismissAction = {
                    TextButton(onClick = { vm.setExportState(ExportState.Idle) }) {
                        Text("关闭", color = AcksFg2)
                    }
                },
                containerColor = AcksSurface2, contentColor = AcksFg
            ) {
                Text(when (done.format) {
                    "pdf"   -> "PDF 已生成"
                    "image" -> if (done.files.size == 1) "长图已保存到相册"
                               else "长图已分 ${done.files.size} 张保存到相册"
                    else    -> "导出完成"
                })
            }
        }

        (state.exportState as? ExportState.Failed)?.let { failed ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                dismissAction = {
                    TextButton(onClick = { vm.setExportState(ExportState.Idle) }) {
                        Text("关闭", color = AcksFg2)
                    }
                },
                containerColor = AcksSurface2, contentColor = AcksFg
            ) { Text("导出失败: ${failed.error}") }
        }
    }

    // ── Bottom sheets ─────────────────────────────────────────────────────
    when (state.activeSheet) {
        ActiveSheet.THEME -> ThemeSheet(
            currentThemeId = doc.themeId, currentMode = doc.mode, format = doc.format,
            onTheme = { vm.setTheme(it) }, onMode = { vm.setMode(it) },
            onDismiss = { vm.closeSheet() }
        )
        ActiveSheet.VIEWPORT -> ViewportSheet(
            current = doc.viewport, customWidth = doc.customWidth,
            onViewport = { vm.setViewport(it) }, onCustomWidth = { vm.setCustomWidth(it) },
            onDismiss = { vm.closeSheet() }
        )
        ActiveSheet.EXPORT -> ExportSheet(
            doc = doc, exportState = state.exportState,
            onExportPdf = {
                val wv = previewHost?.webView ?: run { vm.setExportState(ExportState.Failed("WebView not ready")); return@ExportSheet }
                vm.closeSheet()
                scope.launch {
                    vm.setExportState(ExportState.Running("pdf"))
                    try {
                        val file = ExportController.exportToPdf(ctx, wv, doc.title, doc.viewportWidthPx) { p ->
                            vm.setExportState(ExportState.Running("pdf", p))
                        }
                        vm.setExportState(ExportState.Done(listOf(file), "pdf"))
                    } catch (e: Exception) {
                        vm.setExportState(ExportState.Failed(e.message ?: "PDF 生成失败"))
                    }
                }
            },
            onExportImage = {
                val wv = previewHost?.webView ?: run { vm.setExportState(ExportState.Failed("WebView not ready")); return@ExportSheet }
                vm.closeSheet()
                scope.launch {
                    vm.setExportState(ExportState.Running("image"))
                    val files = ExportController.captureToImage(ctx, wv, doc.viewportWidthPx) { p ->
                        vm.setExportState(ExportState.Running("image", p))
                    }
                    vm.setExportState(
                        if (files.isNotEmpty()) ExportState.Done(files, "image")
                        else ExportState.Failed("内存不足，请尝试更短的文档")
                    )
                }
            },
            onExportCards = {
                val wv = previewHost?.webView ?: run { vm.closeSheet(); return@ExportSheet }
                vm.closeSheet()
                vm.navToCardPreview(wv)
            },
            onDismiss = { vm.closeSheet() }
        )
        ActiveSheet.CARD_EXPORT -> {}   // handled by CardPreviewScreen
        ActiveSheet.DOC_INFO -> DocInfoSheet(doc = doc, onDismiss = { vm.closeSheet() })
        ActiveSheet.HTML_SAFETY -> HtmlSafetySheet(
            htmlMode = doc.htmlMode, onSetMode = { vm.setHtmlMode(it) },
            onDismiss = { vm.closeSheet() }
        )
        ActiveSheet.TOC -> TocSheet(
            markdownSource = doc.markdownSource,
            activeHead     = state.tocActiveHead,
            onGoto         = { idx -> previewHost?.command("gotoHeading", """{"i":$idx}""") },
            onDismiss      = { vm.closeSheet() }
        )
        null -> {}
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewTopBar(doc: DocState, onBack: () -> Unit, onInfo: () -> Unit) {
    val fmtColor = if (doc.format == Format.HTML) Color(0xFF3B82F6) else AcksAccent
    TopAppBar(
        title = {
            Column {
                Text(doc.title, color = AcksFg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val fmt = if (doc.format == Format.HTML) "HTML" else "MD"
                    Text(fmt, color = fmtColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(fmtColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp))
                    if (doc.format == Format.MARKDOWN) {
                        AcksThemes.find(doc.themeId)?.let { theme ->
                            Text("·", color = AcksFg3, fontSize = 9.sp)
                            Text(theme.name, color = AcksFg3, fontSize = 9.sp)
                        }
                    }
                    if (doc.format == Format.HTML && doc.htmlMode == "interactive") {
                        Text("·", color = AcksFg3, fontSize = 9.sp)
                        Text("交互", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = AcksFg)
            }
        },
        actions = {
            IconButton(onClick = onInfo) {
                Icon(Icons.Default.Info, contentDescription = "文档信息", tint = AcksFg2)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AcksSurface)
    )
    HorizontalDivider(color = AcksBorder, thickness = 0.5.dp)
}

// ── Search Bar ────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    search: SearchState,
    onQuery: (String) -> Unit,
    onNav: (Int) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AcksSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(18.dp))
        OutlinedTextField(
            value       = search.query,
            onValueChange = onQuery,
            modifier    = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine  = true,
            placeholder = { Text("在文档中搜索…", color = AcksFg3, fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onNav(1) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AcksAccent,
                unfocusedBorderColor = AcksBorder,
                focusedTextColor     = AcksFg,
                unfocusedTextColor   = AcksFg,
                cursorColor          = AcksAccent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )
        // Result count
        if (search.total > 0) {
            Text("${search.current}/${search.total}", color = AcksFg2, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 2.dp))
        } else if (search.query.isNotEmpty()) {
            Text("无结果", color = AcksFg3, fontSize = 11.sp)
        }
        // Nav buttons
        IconButton(onClick = { onNav(-1) }, enabled = search.total > 0, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上一个",
                tint = if (search.total > 0) AcksFg2 else AcksFg3, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { onNav(1) }, enabled = search.total > 0, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下一个",
                tint = if (search.total > 0) AcksFg2 else AcksFg3, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "关闭搜索", tint = AcksFg2, modifier = Modifier.size(18.dp))
        }
    }
    HorizontalDivider(color = AcksBorder, thickness = 0.5.dp)
}

// ── WebView ───────────────────────────────────────────────────────────────────

@Composable
private fun DocWebView(
    state: DocState,
    onHostCreated: (PreviewWebView) -> Unit,
    onRenderDone: () -> Unit,
    onMessage: (AcksMessage) -> Unit
) {
    var host by remember { mutableStateOf<PreviewWebView?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory  = { ctx ->
            PreviewWebView(ctx, onReady = {}, onRenderDone = onRenderDone, onMessage = onMessage)
                .also { pv -> host = pv; onHostCreated(pv); pv.render(state) }
                .webView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
        }
    )

    LaunchedEffect(state.id, state.themeId, state.mode, state.htmlMode,
                   state.viewport, state.customWidth, state.overrides, state.fontSource) {
        host?.render(state)
    }
}

// ── Bottom Toolbar ────────────────────────────────────────────────────────────

@Composable
private fun BottomToolbar(
    doc: DocState,
    isSearchOn: Boolean,
    onTheme: () -> Unit,
    onViewport: () -> Unit,
    onExport: () -> Unit,
    onSafety: () -> Unit,
    onToc: () -> Unit,
    onSearch: () -> Unit
) {
    val hasHeadings = remember(doc.markdownSource) {
        doc.format == Format.MARKDOWN &&
        doc.markdownSource.lines().any { it.matches(Regex("^#{1,3}\\s+.+")) }
    }

    HorizontalDivider(color = AcksBorder, thickness = 0.5.dp)
    Row(
        modifier = Modifier.fillMaxWidth().background(AcksSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (doc.format == Format.MARKDOWN) {
            ToolbarBtn("主题", Icons.Default.Palette, onTheme, true)
        }
        ToolbarBtn("设备", Icons.Default.PhoneAndroid, onViewport, true)
        ToolbarBtn("导出", Icons.Default.FileDownload, onExport, true)
        if (doc.format == Format.MARKDOWN) {
            ToolbarBtn("目录", Icons.AutoMirrored.Filled.FormatListBulleted, onToc, hasHeadings,
                tint = if (hasHeadings) AcksFg2 else AcksFg3)
        }
        if (doc.format == Format.HTML) {
            val safetyTint = if (doc.htmlMode == "interactive") Color(0xFF22C55E) else AcksFg2
            ToolbarBtn(
                if (doc.htmlMode == "interactive") "交互" else "安全",
                if (doc.htmlMode == "interactive") Icons.Default.PlayArrow else Icons.Default.Security,
                onSafety, true, safetyTint
            )
        }
        ToolbarBtn("搜索", Icons.Default.Search, onSearch, true,
            tint = if (isSearchOn) AcksAccent else AcksFg2)
    }
}

@Composable
private fun ToolbarBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = AcksFg2
) {
    val effectiveTint = if (enabled) tint else AcksFg3
    Column(horizontalAlignment = Alignment.CenterHorizontally,
           modifier = Modifier.padding(horizontal = 2.dp)) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp)) {
            Icon(icon, contentDescription = label, tint = effectiveTint, modifier = Modifier.size(21.dp))
        }
        Text(label, color = effectiveTint, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
    }
}

// ── State screens ─────────────────────────────────────────────────────────────

@Composable private fun LoadingSkeleton() {
    Box(modifier = Modifier.fillMaxSize().background(AcksSurface), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = AcksAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
            Text("正在渲染…", color = AcksFg2, fontSize = 12.sp)
        }
    }
}

@Composable private fun UnsupportedContent(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp),
           horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(AcksSurface2),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Block, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("暂不支持此文件", color = AcksFg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("ACKS Reader 支持 .md .markdown .html .htm 格式", color = AcksFg2, fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AcksAccent)) {
            Text("返回")
        }
    }
}

@Composable private fun ErrorContent() {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp),
           horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("渲染失败", color = AcksFg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("文档结构异常或存在无法解析的内容", color = AcksFg2, fontSize = 13.sp)
    }
}

@Composable private fun CorruptedContent(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp),
           horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2A1010)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("文件可能已损坏", color = AcksFg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("内容无法识别，请确认文件编码为 UTF-8 后重试。", color = AcksFg2, fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AcksAccent)) {
            Text("返回")
        }
    }
}

@Composable private fun LargeFileBanner(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A10))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("文件较大，仅显示前 5 MB 内容", color = Color(0xFFF59E0B), fontSize = 12.sp)
    }
}

@Composable private fun SafeModeBanner(onEnable: () -> Unit, onLearnMore: () -> Unit, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1C1A14))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("安全预览（脚本已屏蔽）", color = AcksFg2, fontSize = 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onEnable, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("启用交互", color = AcksAccent, fontSize = 12.sp)
        }
    }
}

@Composable private fun ExportProgressOverlay(s: ExportState.Running) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC0B0B0C)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = AcksAccent, progress = { s.progress })
            Text("正在导出${if (s.format == "pdf") " PDF" else "长图"}… ${(s.progress * 100).toInt()}%",
                color = AcksFg2, fontSize = 13.sp)
        }
    }
}
