package studio.acks.reader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import studio.acks.reader.ui.*

private data class OnboardStep(
    val icon: ImageVector,
    val iconBg: Color,
    val title: String,
    val body: String
)

private val steps = listOf(
    OnboardStep(
        icon   = Icons.Default.FolderOpen,
        iconBg = Color(0xFF1A3A2A),
        title  = "从任意 App 打开",
        body   = "在微信、飞书、Telegram 等收到 .md 文件后，点击「打开方式」选择 ACKS Reader，或直接从文件管理器打开。"
    ),
    OnboardStep(
        icon   = Icons.Default.Palette,
        iconBg = Color(0xFF1A1A3A),
        title  = "选择你喜欢的主题",
        body   = "内置 10+ 种精美主题，支持深色 / 浅色模式，以及 5 档视口宽度，让 Markdown 以最佳姿态呈现。"
    ),
    OnboardStep(
        icon   = Icons.Default.Share,
        iconBg = Color(0xFF3A1A10),
        title  = "导出 · 分享",
        body   = "一键导出高质量 PDF 或长截图，直接分享到微信、朋友圈，或保存到相册。"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSheet(
    onDismiss: () -> Unit,
    onTryNow: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) { page ->
                OnboardPage(step = steps[page])
            }

            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            ) {
                repeat(steps.size) { i ->
                    val isActive = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 20.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) AcksAccent else AcksBorder2)
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pagerState.currentPage < steps.size - 1) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcksFg2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AcksBorder2)
                    ) {
                        Text("跳过", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AcksAccent)
                    ) {
                        Text("下一步", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcksFg2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AcksBorder2)
                    ) {
                        Text("稍后再说", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { onDismiss(); onTryNow() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AcksAccent)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即试用", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardPage(step: OnboardStep) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(step.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                step.icon,
                contentDescription = null,
                tint = AcksAccent,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            step.title,
            color = AcksFg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            step.body,
            color = AcksFg2,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
