package studio.acks.reader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.DocState
import studio.acks.reader.ExportState
import studio.acks.reader.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    doc: DocState,
    exportState: ExportState,
    onExportPdf: () -> Unit,
    onExportImage: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = exportState is ExportState.Running

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("导出", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp))

            // Current preset info
            val vpLabel = when (doc.viewport) {
                "phone"   -> "手机宽度"
                "desktop" -> "桌面宽度"
                "a4"      -> "A4"
                "social"  -> "长图宽度"
                "custom"  -> "自定义 ${doc.customWidth}px"
                else      -> doc.viewport
            }
            Text("当前视口: $vpLabel", color = AcksFg3, fontSize = 11.sp)

            HorizontalDivider(color = AcksBorder)

            ExportOption(
                icon    = Icons.Default.PictureAsPdf,
                title   = "导出为 PDF",
                sub     = "A4 格式 · 保存到文件并分享",
                color   = Color(0xFF3B82F6),
                enabled = !busy,
                onClick = onExportPdf
            )

            ExportOption(
                icon    = Icons.Default.Image,
                title   = "导出为长图",
                sub     = "PNG 格式 · 可直接分享到微信/朋友圈",
                color   = Color(0xFF22C55E),
                enabled = !busy,
                onClick = onExportImage
            )

            HorizontalDivider(color = AcksBorder)

            // Note
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AcksSurface2)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                Text(
                    "导出结果与当前预览主题和设备宽度完全一致。PDF 保存在文档文件夹，完成后可直接分享。",
                    color    = AcksFg3,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ExportOption(
    icon: ImageVector,
    title: String,
    sub: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (enabled) AcksFg else AcksFg3, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = AcksFg3, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (enabled) AcksFg3 else AcksBorder2, modifier = Modifier.size(18.dp))
    }
}
