package studio.acks.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.AcksThemes
import studio.acks.reader.ReaderViewModel

@Composable
fun SettingsScreen(
    defaultTheme: String,
    defaultViewport: String,
    defaultHtmlMode: String,
    fontScale: Float,
    appTheme: String,
    enableMermaid: Boolean,
    enableMath: Boolean,
    versionName: String,
    vm: ReaderViewModel,
    onPickFile: () -> Unit,
    onShowOnboarding: () -> Unit,
    fontSourceOverride: String = "auto",
    resolvedFontSource: String = "auto"
) {
    var showClearConfirm    by remember { mutableStateOf(false) }
    var showThemePicker     by remember { mutableStateOf(false) }
    var showViewportPicker  by remember { mutableStateOf(false) }
    var showHtmlModePicker  by remember { mutableStateOf(false) }
    var showAppThemePicker  by remember { mutableStateOf(false) }

    BackHandler { vm.navBack() }

    Box(modifier = Modifier.fillMaxSize().background(AcksBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(onBack = { vm.navBack() })

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ── 外观 ───────────────────────────────────────────────────
                item { SectionHeader("外观") }
                item {
                    val appThemeLabel = when (appTheme) {
                        "dark"  -> "深色模式"
                        "light" -> "浅色模式"
                        else    -> "跟随系统"
                    }
                    SettingsRow(Icons.Default.DarkMode, "界面主题", appThemeLabel,
                        onClick = { showAppThemePicker = true })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── 阅读偏好 ───────────────────────────────────────────────
                item { SectionHeader("阅读偏好") }
                item {
                    val themeName = AcksThemes.find(defaultTheme)?.name ?: defaultTheme
                    SettingsRow(Icons.Default.ColorLens, "默认主题", themeName,
                        onClick = { showThemePicker = true })
                }
                item {
                    val vpName = when (defaultViewport) {
                        "phone"   -> "手机宽度（设备宽度）"
                        "desktop" -> "桌面宽度（1024px）"
                        "a4"      -> "A4（794px）"
                        "social"  -> "社交长图（480px）"
                        else      -> defaultViewport
                    }
                    SettingsRow(Icons.Default.PhoneAndroid, "默认视口", vpName,
                        onClick = { showViewportPicker = true })
                }
                item {
                    SettingsRow(Icons.Default.Security, "默认 HTML 模式",
                        if (defaultHtmlMode == "safe") "安全预览（屏蔽脚本）" else "交互模式（允许脚本）",
                        onClick = { showHtmlModePicker = true })
                }
                item {
                    FontScaleRow(scale = fontScale, onScaleChange = { vm.setFontScale(it) })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── 字体来源 ───────────────────────────────────────────────
                item { SectionHeader("字体来源") }
                item {
                    FontSourceRow(
                        override = fontSourceOverride,
                        resolved = resolvedFontSource,
                        onChange = { vm.setFontSourceOverride(it) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── 渲染扩展 ───────────────────────────────────────────────
                item { SectionHeader("渲染扩展") }
                item {
                    ToggleRow(
                        icon    = Icons.Default.AccountTree,
                        label   = "Mermaid 流程图",
                        desc    = "渲染 ```mermaid 代码块为 SVG 流程图",
                        checked = enableMermaid,
                        onToggle = { vm.setEnableMermaid(it) }
                    )
                }
                item {
                    ToggleRow(
                        icon    = Icons.Default.Functions,
                        label   = "数学公式（TeX）",
                        desc    = "渲染 \$…\$ 内联和 \$\$…\$\$ 块级公式",
                        checked = enableMath,
                        onToggle = { vm.setEnableMath(it) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── 存储 ───────────────────────────────────────────────────
                item { SectionHeader("存储") }
                item {
                    SettingsRow(Icons.Default.FolderOpen, "管理历史文件",
                        "在首页可对每个文件单独删除", onClick = { vm.navBack() })
                }
                item {
                    SettingsRow(Icons.Default.DeleteSweep, "清除所有文件",
                        "删除全部历史记录和沙盒文件",
                        tint = Color(0xFFEF4444),
                        onClick = { showClearConfirm = true })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── 帮助 ───────────────────────────────────────────────────
                item { SectionHeader("帮助") }
                item {
                    SettingsRow(Icons.Default.MenuBook, "功能指南",
                        "重新查看 App 使用教程", onClick = onShowOnboarding)
                }
                item {
                    SettingsRow(Icons.Default.Info, "关于 ACKS Reader",
                        "版本 $versionName · 开源地址", onClick = { vm.navToAbout() })
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // ── Default theme picker ──────────────────────────────────────────────────
    if (showThemePicker) {
        DefaultThemePickerSheet(
            current  = defaultTheme,
            onSelect = { vm.setDefaultTheme(it); showThemePicker = false },
            onDismiss = { showThemePicker = false }
        )
    }

    // ── Default viewport picker ───────────────────────────────────────────────
    if (showViewportPicker) {
        DefaultViewportPickerSheet(
            current   = defaultViewport,
            onSelect  = { vm.setDefaultViewport(it); showViewportPicker = false },
            onDismiss = { showViewportPicker = false }
        )
    }

    // ── Default HTML mode picker ──────────────────────────────────────────────
    if (showHtmlModePicker) {
        DefaultHtmlModePickerSheet(
            current   = defaultHtmlMode,
            onSelect  = { vm.setDefaultHtmlMode(it); showHtmlModePicker = false },
            onDismiss = { showHtmlModePicker = false }
        )
    }

    // ── App theme picker ─────────────────────────────────────────────────────
    if (showAppThemePicker) {
        AppThemePickerSheet(
            current   = appTheme,
            onSelect  = { vm.setAppTheme(it); showAppThemePicker = false },
            onDismiss = { showAppThemePicker = false }
        )
    }

    // ── Clear all confirm dialog ──────────────────────────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清除所有文件", color = AcksFg) },
            text  = { Text("将删除全部最近记录和沙盒缓存文件，此操作无法撤销。", color = AcksFg2) },
            confirmButton = {
                TextButton(onClick = { showClearConfirm = false; vm.clearAllDocs() }) {
                    Text("清除", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = AcksFg2)
                }
            },
            containerColor = AcksSurface2
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AcksSurface)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = AcksFg2
                )
            }
            Text(
                "设置",
                color = AcksFg,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = AcksBorder, thickness = 0.5.dp)
    }
}

// ── Reusable row ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = AcksFg3,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color = AcksFg2,
    onClick: (() -> Unit)?
) {
    val mod = if (onClick != null)
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AcksSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    else
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AcksSurface)
            .padding(horizontal = 14.dp, vertical = 14.dp)

    Row(modifier = mod, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (tint == Color(0xFFEF4444)) tint else AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(value, color = AcksFg3, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Font scale row ───────────────────────────────────────────────────────────

@Composable
private fun FontScaleRow(scale: Float, onScaleChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AcksSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TextFields,
                contentDescription = null,
                tint = AcksFg2,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("字体缩放", color = AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${(scale * 100).toInt()}%",
                color = AcksAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(AcksAccentSoft, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            valueRange = 0.8f..1.5f,
            steps = 13,  // 0.05 increments between 0.8 and 1.5 = 14 steps, 12 inner steps
            colors = SliderDefaults.colors(
                thumbColor       = AcksAccent,
                activeTrackColor = AcksAccent,
                inactiveTrackColor = AcksBorder2
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("80%", color = AcksFg3, fontSize = 10.sp)
            Text("100%", color = AcksFg3, fontSize = 10.sp)
            Text("150%", color = AcksFg3, fontSize = 10.sp)
        }
    }
}

// ── Font source row ──────────────────────────────────────────────────────────

@Composable
private fun FontSourceRow(override: String, resolved: String, onChange: (String) -> Unit) {
    val statusLabel = when (resolved) {
        "google"    -> "Google Fonts"
        "cn_mirror" -> "CN 镜像 (fonts.loli.net)"
        "local"     -> "本地字体"
        else        -> "检测中…"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AcksSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("当前生效", color = AcksFg2, fontSize = 12.sp)
            Text(statusLabel, color = AcksAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "auto"      to "自动",
                "local"     to "本地",
                "cn_mirror" to "CN 镜像"
            ).forEach { (id, label) ->
                val selected = override == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) AcksAccentSoft else AcksSurface2)
                        .border(
                            if (selected) 1.dp else 0.5.dp,
                            if (selected) AcksAccent.copy(.5f) else AcksBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onChange(id) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) AcksAccent else AcksFg2,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Text(
            "自动：根据网络检测最快来源。本地：使用内置 WOFF2 字体（无需联网）。CN 镜像：强制使用 fonts.loli.net。",
            color = AcksFg3, fontSize = 11.sp, lineHeight = 16.sp
        )
    }
}

// ── Default theme picker sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultThemePickerSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = AcksThemes.all.filter { it.id != "aireport" || true }  // show all themes

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { studio.acks.reader.ui.sheets.SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "默认主题",
                color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Text(
                "新打开的文件将使用此主题",
                color = AcksFg3, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(max = 480.dp)
            ) {
                items(themes.size) { idx ->
                    val t = themes[idx]
                    val isSelected = t.id == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AcksAccentSoft else Color.Transparent)
                            .clickable { onSelect(t.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 如果支持双模式，并排显示浅色+深色迷你预览
                        if (t.modes.size >= 2) {
                            ThemeMiniCard(t, "light", width = 60.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            ThemeMiniCard(t, "dark",  width = 60.dp)
                        } else {
                            ThemeMiniCard(t, t.defaultMode, width = 68.dp)
                            Spacer(modifier = Modifier.width(56.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name,
                                color = if (isSelected) AcksAccent else AcksFg,
                                fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.tag, color = AcksFg3, fontSize = 11.sp)
                                if (t.modes.size >= 2) {
                                    Text("· 深/浅", color = AcksFg3, fontSize = 11.sp)
                                }
                            }
                        }
                        if (isSelected) {
                            Text("当前", color = AcksAccent, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(AcksAccentSoft, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    if (idx < themes.size - 1) {
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.3f), thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 10.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Theme mini preview card ───────────────────────────────────────────────────

@Composable
private fun ThemeMiniCard(t: studio.acks.reader.ThemeMeta, mode: String, width: androidx.compose.ui.unit.Dp) {
    val bg     = Color(studio.acks.reader.AcksThemes.bgFor(t, mode))
    val accent = Color(studio.acks.reader.AcksThemes.accentFor(t, mode))
    val title  = Color(studio.acks.reader.AcksThemes.titleFor(t, mode))

    Box(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.5.dp, AcksBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        // Accent header bar
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(accent))
        Column(
            modifier = Modifier.padding(start = 5.dp, top = 6.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Title line — full accent color
            Box(modifier = Modifier.width(width * 0.55f).height(4.dp)
                .clip(RoundedCornerShape(2.dp)).background(accent))
            // Body line 1 — title color dimmed
            Box(modifier = Modifier.width(width * 0.85f).height(2.5.dp)
                .clip(RoundedCornerShape(1.dp)).background(title.copy(alpha = 0.45f)))
            // Body line 2
            Box(modifier = Modifier.width(width * 0.7f).height(2.5.dp)
                .clip(RoundedCornerShape(1.dp)).background(title.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(2.dp))
            // Code block hint
            Box(modifier = Modifier.width(width * 0.65f).height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent.copy(alpha = 0.12f))) {
                Box(modifier = Modifier.padding(start = 3.dp).width(width * 0.4f).height(2.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(1.dp))
                    .background(accent.copy(alpha = 0.5f)))
            }
        }
        // Mode badge
        Text(
            if (mode == "dark") "暗" else "亮",
            color = title.copy(alpha = 0.5f),
            fontSize = 7.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
        )
    }
}

// ── Toggle row ───────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AcksSurface)
            .clickable { onToggle(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AcksFg2, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = AcksFg3, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AcksAccent,
                uncheckedThumbColor = AcksFg3,
                uncheckedTrackColor = AcksBorder2
            )
        )
    }
}

// ── App theme picker ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppThemePickerSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "system" to "跟随系统",
        "dark"   to "深色模式",
        "light"  to "浅色模式"
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { studio.acks.reader.ui.sheets.SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text("界面主题", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Text("App 外壳的配色模式（不影响文档主题）", color = AcksFg3, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            options.forEach { (id, label) ->
                val isSelected = id == current
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = if (isSelected) AcksAccent else AcksFg, fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f))
                    RadioButton(selected = isSelected, onClick = { onSelect(id) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AcksAccent, unselectedColor = AcksFg3))
                }
                if (id != "light") HorizontalDivider(color = AcksBorder.copy(alpha = 0.4f),
                    thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Default viewport picker ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultViewportPickerSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "phone"   to "手机宽度（设备宽度）",
        "desktop" to "桌面宽度（1024 px）",
        "a4"      to "A4（794 px）",
        "social"  to "社交长图（480 px）"
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { studio.acks.reader.ui.sheets.SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text("默认视口", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Text("新文件打开时默认使用的预览宽度", color = AcksFg3, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            options.forEach { (id, label) ->
                val isSelected = id == current
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = if (isSelected) AcksAccent else AcksFg, fontSize = 14.sp,
                        modifier = Modifier.weight(1f))
                    if (isSelected) RadioButton(selected = true, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = AcksAccent))
                    else RadioButton(selected = false, onClick = { onSelect(id) },
                        colors = RadioButtonDefaults.colors(unselectedColor = AcksFg3))
                }
                HorizontalDivider(color = AcksBorder.copy(alpha = 0.4f), thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Default HTML mode picker ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultHtmlModePickerSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { studio.acks.reader.ui.sheets.SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text("默认 HTML 模式", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp))
            Text("打开 HTML 文件时的默认安全策略", color = AcksFg3, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            listOf(
                "safe"        to Triple("安全预览（推荐）", "屏蔽所有脚本，防止恶意代码", Color(0xFFF59E0B)),
                "interactive" to Triple("交互模式", "允许脚本和动画，适合 AI 生成页面", Color(0xFF22C55E))
            ).forEach { (id, info) ->
                val (title, desc, color) = info
                val selected = id == current
                OutlinedCard(
                    onClick = { onSelect(id) },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selected) 1.5.dp else 0.5.dp,
                        if (selected) color.copy(0.5f) else AcksBorder
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) color.copy(alpha = 0.07f) else AcksSurface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = if (selected) color else AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(desc, color = AcksFg3, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (selected) RadioButton(selected = true, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = color))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
