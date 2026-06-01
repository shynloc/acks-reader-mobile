package studio.acks.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onShowOnboarding: () -> Unit
) {
    var showClearConfirm    by remember { mutableStateOf(false) }
    var showThemePicker     by remember { mutableStateOf(false) }
    var showViewportPicker  by remember { mutableStateOf(false) }
    var showHtmlModePicker  by remember { mutableStateOf(false) }
    var showAppThemePicker  by remember { mutableStateOf(false) }

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
                modifier = Modifier.heightIn(max = 440.dp)
            ) {
                items(themes.size) { idx ->
                    val t = themes[idx]
                    val isSelected = t.id == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AcksAccentSoft else Color.Transparent)
                            .clickable { onSelect(t.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color swatch — use light swatch for light-only themes
                        val swatchColor = if (t.modes == listOf("light")) Color(t.swatchLight) else Color(t.swatchDark)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(swatchColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, color = if (isSelected) AcksAccent else AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(t.tag, color = AcksFg3, fontSize = 11.sp)
                        }
                        if (isSelected) {
                            Text(
                                "当前",
                                color = AcksAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(AcksAccentSoft, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
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
