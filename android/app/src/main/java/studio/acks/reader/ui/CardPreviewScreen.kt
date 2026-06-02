package studio.acks.reader.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.AcksThemes
import studio.acks.reader.AppUiState
import studio.acks.reader.ReaderViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPreviewScreen(state: AppUiState, vm: ReaderViewModel) {
    val cs  = state.cardPreviewState
    val st  = state.settings
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage    = cs.currentPage,
        pageCount      = { cs.previewFiles.size.coerceAtLeast(1) }
    )

    // Sync page change back to VM
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != cs.currentPage)
            vm.setCardPreviewPage(pagerState.currentPage)
    }

    Scaffold(
        containerColor = AcksBg,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.clearCardPreview() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = AcksFg)
                    }
                },
                title = {
                    val total = cs.previewFiles.size
                    Text(
                        if (total > 0) "卡片预览  ${cs.currentPage + 1} / $total"
                        else "卡片预览",
                        color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (!cs.isLoading && cs.previewFiles.isNotEmpty()) {
                        TextButton(
                            onClick = { vm.exportCardsFromPreview() },
                            enabled = !cs.isExporting
                        ) {
                            Text(
                                if (cs.isExporting) "导出中…" else "导出全部",
                                color = if (cs.isExporting) AcksFg3 else AcksAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AcksSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Card pager ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (cs.isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AcksAccent)
                        Text("正在生成预览…", color = AcksFg3, fontSize = 13.sp)
                    }
                } else if (cs.previewFiles.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null,
                            tint = AcksFg3, modifier = Modifier.size(40.dp))
                        Text("暂无预览", color = AcksFg3, fontSize = 13.sp)
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp
                    ) { page ->
                        CardPreviewPage(file = cs.previewFiles.getOrNull(page))
                    }
                }

                // Export progress overlay
                if (cs.isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AcksBg.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(AcksSurface)
                                .padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { cs.exportProgress },
                                color = AcksAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "正在导出 ${(cs.exportProgress * 100).toInt()}%",
                                color = AcksFg, fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // ── Page indicator dots ───────────────────────────────────────────
            if (!cs.isLoading && cs.previewFiles.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cs.previewFiles.indices.forEach { i ->
                        val selected = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (selected) AcksAccent else AcksBorder2)
                        )
                    }
                }
            }

            // ── Controls panel ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AcksSurface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Theme + Cover row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Theme selector (scrollable row of mini swatches)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(AcksThemes.all) { t ->
                            val selected = t.id == st.cardThemeId
                            val mode = t.defaultMode
                            val bg  = Color(AcksThemes.bgFor(t, mode))
                            val acc = Color(AcksThemes.accentFor(t, mode))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(bg)
                                    .then(
                                        if (selected) Modifier.border(2.dp, AcksAccent, RoundedCornerShape(7.dp))
                                        else Modifier.border(0.5.dp, AcksBorder, RoundedCornerShape(7.dp))
                                    )
                                    .clickable { vm.setCardPreviewTheme(t.id) }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(acc))
                            }
                        }
                    }

                    // Cover toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("封面", color = AcksFg2, fontSize = 12.sp)
                        Switch(
                            checked = st.cardWithCover,
                            onCheckedChange = { vm.setCardPreviewWithCover(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor   = AcksAccent,
                                checkedThumbColor   = Color.White,
                                uncheckedTrackColor = AcksBorder2,
                                uncheckedThumbColor = AcksFg3
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                // Font size slider  14–36 px, 每 1 px 一档
                SliderRow(
                    label = "字号",
                    value = st.cardFontSizePx,
                    valueRange = 14f..36f,
                    steps = 21,            // 23 档：14,15,...,36
                    displayText = "${st.cardFontSizePx.toInt()}px",
                    onValueChange = { vm.setCardFontSize(it) }
                )

                // Padding slider  20–50 px（四边），每 2 px 一档
                SliderRow(
                    label = "四边距",
                    value = st.cardPadPx.toFloat(),
                    valueRange = 20f..50f,
                    steps = 14,            // 16 档：20,22,...,50
                    displayText = "${st.cardPadPx}px",
                    onValueChange = { vm.setCardPad(it.toInt()) }
                )
            }
        }
    }

    // Export done snackbar
    if (cs.exportDone) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2500)
            vm.clearCardPreview()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp),
                containerColor = AcksSurface2,
                contentColor = AcksFg
            ) {
                Text("${state.cardPreviewState.previewFiles.size} 张卡片已保存到相册")
            }
        }
    }
}

@Composable
private fun CardPreviewPage(file: File?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(AcksSurface2),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            val bitmap = remember(file.path) {
                runCatching {
                    BitmapFactory.Options().let { opts ->
                        opts.inSampleSize = 2  // load at half size for preview (540×720)
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = AcksAccent, modifier = Modifier.size(24.dp))
            }
        } else {
            CircularProgressIndicator(color = AcksAccent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayText: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = AcksFg2, fontSize = 12.sp,
            modifier = Modifier.width(28.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AcksAccent,
                activeTrackColor = AcksAccent,
                inactiveTrackColor = AcksBorder2
            )
        )
        Text(displayText, color = AcksFg3, fontSize = 11.sp,
            modifier = Modifier.width(36.dp))
    }
}
