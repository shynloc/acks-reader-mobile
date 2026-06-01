package studio.acks.reader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlSafetySheet(
    htmlMode: String,
    onSetMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isSafe = htmlMode == "safe"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("HTML 安全设置", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp))

            // Safe Preview option
            ModeCard(
                selected = isSafe,
                icon     = Icons.Default.Security,
                iconTint = Color(0xFFF59E0B),
                title    = "安全预览",
                desc     = "屏蔽所有 <script> 标签，防止恶意代码执行。适用于未知来源的 HTML 文件。",
                onClick  = { onSetMode("safe") }
            )

            // Interactive Mode option
            ModeCard(
                selected = !isSafe,
                icon     = Icons.Default.PlayArrow,
                iconTint = Color(0xFF22C55E),
                title    = "交互模式",
                desc     = "完整运行 JavaScript、CSS 动画、Canvas 和 SVG。适用于 AI 生成的交互页面。",
                onClick  = { onSetMode("interactive") }
            )

            // Warning
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C1A0A))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null,
                    tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp).padding(top = 1.dp))
                Text(
                    "交互模式允许脚本执行。仅对可信来源的文件启用，切勿对陌生人发送的文件启用。",
                    color = Color(0xFFD4A96A),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ModeCard(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    val bg          = if (selected) iconTint.copy(alpha = 0.08f) else AcksSurface2
    val borderColor = if (selected) iconTint.copy(alpha = 0.4f) else AcksBorder

    OutlinedCard(
        onClick = onClick,
        shape   = RoundedCornerShape(12.dp),
        border  = androidx.compose.foundation.BorderStroke(
            if (selected) 1.5.dp else 0.5.dp,
            borderColor
        ),
        colors  = CardDefaults.outlinedCardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, color = AcksFg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (selected) {
                        Text("当前", color = iconTint, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = AcksFg2, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}
