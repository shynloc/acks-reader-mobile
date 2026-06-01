package studio.acks.reader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.AcksThemes
import studio.acks.reader.DocState
import studio.acks.reader.Format
import studio.acks.reader.ui.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocInfoSheet(doc: DocState, onDismiss: () -> Unit) {
    val file     = if (doc.sandboxPath.isNotEmpty()) File(doc.sandboxPath) else null
    val sizeStr  = file?.length()?.let { formatBytes(it) } ?: "—"
    val charCount = doc.markdownSource.length
    val lineCount = doc.markdownSource.lines().size
    val theme     = AcksThemes.find(doc.themeId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text("文档信息", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow("文件名", doc.title)
            InfoRow("格式", if (doc.format == Format.HTML) "HTML" else if (doc.format == Format.MARKDOWN) "Markdown" else "未知")
            InfoRow("文件大小", sizeStr)
            InfoRow("字符数", "%,d 个字符".format(charCount))
            InfoRow("行数", "%,d 行".format(lineCount))
            if (doc.format == Format.MARKDOWN && theme != null) {
                InfoRow("当前主题", "${theme.name}（${if (doc.mode == "dark") "深色" else "浅色"}）")
            }
            val vpStr = when (doc.viewport) {
                "phone"   -> "手机（设备宽度）"
                "desktop" -> "桌面（1024 px）"
                "a4"      -> "A4（794 px）"
                "social"  -> "社交长图（480 px）"
                "custom"  -> "自定义（${doc.customWidth} px）"
                else      -> doc.viewport
            }
            InfoRow("预览视口", vpStr)
            if (doc.format == Format.HTML) {
                InfoRow("HTML 模式", if (doc.htmlMode == "interactive") "交互模式" else "安全预览")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = AcksFg2, fontSize = 13.sp)
        Text(value, color = AcksFg, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp))
    }
    HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}
