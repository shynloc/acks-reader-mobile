package studio.acks.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
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
    versionName: String,
    vm: ReaderViewModel,
    onPickFile: () -> Unit
) {
    var showClearConfirm    by remember { mutableStateOf(false) }
    var showThemePicker     by remember { mutableStateOf(false) }
    var showViewportPicker  by remember { mutableStateOf(false) }
    var showHtmlModePicker  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AcksBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(onBack = { vm.navBack() })

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ── Section: 阅读偏好 ──────────────────────────────────────
                item {
                    SectionHeader("阅读偏好")
                }

                // Default theme
                item {
                    val themeName = AcksThemes.find(defaultTheme)?.name ?: defaultTheme
                    SettingsRow(
                        icon  = Icons.Default.ColorLens,
                        label = "默认主题",
                        value = themeName,
                        onClick = { showThemePicker = true }
                    )
                }

                // Default viewport
                item {
                    val vpName = when (defaultViewport) {
                        "phone"   -> "手机宽度（默认）"
                        "desktop" -> "桌面宽度（1024px）"
                        "a4"      -> "A4（794px）"
                        "social"  -> "社交长图（480px）"
                        else      -> defaultViewport
                    }
                    SettingsRow(
                        icon    = Icons.Default.PhoneAndroid,
                        label   = "默认视口",
                        value   = vpName,
                        onClick = { showViewportPicker = true }
                    )
                }

                // Default HTML safety mode
                item {
                    SettingsRow(
                        icon    = Icons.Default.Security,
                        label   = "默认 HTML 模式",
                        value   = if (defaultHtmlMode == "safe") "安全预览（屏蔽脚本）" else "交互模式（允许脚本）",
                        onClick = { showHtmlModePicker = true }
                    )
                }

                // Font scale
                item {
                    FontScaleRow(
                        scale = fontScale,
                        onScaleChange = { vm.setFontScale(it) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── Section: 存储 ─────────────────────────────────────────
                item {
                    SectionHeader("存储")
                }

                item {
                    SettingsRow(
                        icon  = Icons.Default.DeleteSweep,
                        label = "清除所有文件",
                        value = "删除全部历史记录和沙盒文件",
                        tint  = Color(0xFFEF4444),
                        onClick = { showClearConfirm = true }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ── Section: 关于 ─────────────────────────────────────────
                item {
                    SectionHeader("关于")
                }

                item {
                    SettingsRow(
                        icon  = Icons.Default.Info,
                        label = "版本",
                        value = versionName,
                        onClick = null
                    )
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
