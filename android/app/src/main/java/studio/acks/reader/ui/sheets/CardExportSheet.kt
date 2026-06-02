package studio.acks.reader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.AcksThemes
import studio.acks.reader.CardExportOptions
import studio.acks.reader.ExportState
import studio.acks.reader.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardExportSheet(
    currentThemeId: String,
    exportState: ExportState,
    onExport: (CardExportOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentThemeId) }
    var density       by remember { mutableStateOf("balanced") }
    var withCover     by remember { mutableStateOf(true) }
    var showThemePick by remember { mutableStateOf(false) }

    val busy = exportState is ExportState.Running
    val progress = (exportState as? ExportState.Running)?.progress ?: 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp)
        ) {
            item {
                Text("图文卡片导出", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp))
                Text("将内容分页为 3:4 竖版卡片，可直接发布到小红书、微博等平台",
                    color = AcksFg3, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp))
            }

            // ── 卡片主题 ──────────────────────────────────────────────────
            item {
                OptionLabel("卡片主题")
                val t = AcksThemes.find(selectedTheme)
                val mode = t?.defaultMode ?: "dark"
                val bg  = t?.let { Color(AcksThemes.bgFor(it, mode)) } ?: AcksSurface2
                val acc = t?.let { Color(AcksThemes.accentFor(it, mode)) } ?: AcksAccent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AcksSurface)
                        .clickable { showThemePick = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .border(0.5.dp, AcksBorder, RoundedCornerShape(8.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(acc))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t?.name ?: selectedTheme, color = AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(t?.tag ?: "", color = AcksFg3, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── 内容密度 ──────────────────────────────────────────────────
            item {
                OptionLabel("内容密度")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "loose"    to "宽松\n每页内容少",
                        "balanced" to "均衡\n推荐",
                        "dense"    to "紧凑\n每页内容多"
                    ).forEach { (id, label) ->
                        val selected = id == density
                        val lines = label.split("\n")
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AcksAccentSoft else AcksSurface)
                                .border(if (selected) 1.5.dp else 0.5.dp,
                                    if (selected) AcksAccent.copy(.5f) else AcksBorder,
                                    RoundedCornerShape(10.dp))
                                .clickable { density = id }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(lines[0], color = if (selected) AcksAccent else AcksFg,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(lines[1], color = AcksFg3, fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── 封面卡片 ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AcksSurface)
                        .clickable { withCover = !withCover }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, tint = AcksFg2, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("生成封面卡", color = AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("第一张卡片用大标题+主题配色作封面", color = AcksFg3, fontSize = 12.sp)
                    }
                    Switch(checked = withCover, onCheckedChange = { withCover = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AcksAccent, checkedThumbColor = Color.White,
                            uncheckedTrackColor = AcksBorder2, uncheckedThumbColor = AcksFg3))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 提示 ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AcksSurface2)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AcksFg3,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Text("卡片以 1080×1440（3:4）分辨率导出为 JPEG，自动保存到相册「ACKS Reader」文件夹，完成后可批量分享。",
                        color = AcksFg3, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 进度 / 导出按钮 ───────────────────────────────────────────
            item {
                if (busy) {
                    val cs = exportState as? ExportState.Running
                    val hasTotal = (cs?.progress ?: 0f) > 0f || progress > 0f
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = AcksAccent,
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                if (!hasTotal) "正在分析文档…"
                                else "正在生成卡片… ${(progress * 100).toInt()}%",
                                color = AcksFg2, fontSize = 13.sp
                            )
                        }
                        if (hasTotal)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = AcksAccent, trackColor = AcksBorder2
                            )
                        else
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = AcksAccent, trackColor = AcksBorder2
                            )
                    }
                } else {
                    Button(
                        onClick = { onExport(CardExportOptions(selectedTheme, density, withCover)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AcksAccent)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("生成图文卡片", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showThemePick) {
        CardThemePickerSheet(
            current  = selectedTheme,
            onSelect = { selectedTheme = it; showThemePick = false },
            onDismiss = { showThemePick = false }
        )
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(text, color = AcksFg2, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardThemePickerSheet(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AcksSurface,
        dragHandle = { SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text("卡片主题", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(max = 480.dp)
            ) {
                items(AcksThemes.all.size) { idx ->
                    val t = AcksThemes.all[idx]
                    val isSelected = t.id == current
                    val mode = t.defaultMode
                    val bg   = Color(AcksThemes.bgFor(t, mode))
                    val acc  = Color(AcksThemes.accentFor(t, mode))
                    val title = Color(AcksThemes.titleFor(t, mode))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AcksAccentSoft else Color.Transparent)
                            .clickable { onSelect(t.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini card preview
                        Box(
                            modifier = Modifier
                                .width(44.dp).height(33.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(bg)
                                .border(0.5.dp, AcksBorder.copy(.4f), RoundedCornerShape(5.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(acc))
                            Column(modifier = Modifier.padding(start = 4.dp, top = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.width(26.dp).height(3.dp)
                                    .clip(RoundedCornerShape(1.dp)).background(acc))
                                Box(modifier = Modifier.width(38.dp).height(2.dp)
                                    .clip(RoundedCornerShape(1.dp)).background(title.copy(.4f)))
                                Box(modifier = Modifier.width(32.dp).height(2.dp)
                                    .clip(RoundedCornerShape(1.dp)).background(title.copy(.28f)))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, color = if (isSelected) AcksAccent else AcksFg,
                                fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(t.tag, color = AcksFg3, fontSize = 11.sp)
                        }
                        if (isSelected) RadioButton(selected = true, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = AcksAccent))
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}
