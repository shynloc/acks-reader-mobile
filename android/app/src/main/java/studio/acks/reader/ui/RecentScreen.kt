package studio.acks.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentScreen(state: AppUiState, vm: ReaderViewModel, onPickFile: () -> Unit, onSettings: () -> Unit = { vm.navToSettings() }) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AcksBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RecentTopBar(onPickFile = onPickFile, onSettings = onSettings)
            if (state.recentDocs.isEmpty()) {
                EmptyState(onPickFile = onPickFile, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.recentDocs, key = { it.id }) { record ->
                        RecentDocCard(
                            record  = record,
                            onClick  = { vm.openRecent(record) },
                            onDelete = { vm.deleteRecent(record) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // Import error snackbar — show friendly message, not raw system error
        if (state.importError != null) {
            val friendlyMsg = when {
                state.importError.contains("Permission", ignoreCase = true) ->
                    "无法访问此文件。请通过其他 App 的「打开方式」发送文件到 ACKS Reader。"
                state.importError.contains("stream", ignoreCase = true) ||
                state.importError.contains("open", ignoreCase = true) ->
                    "文件打开失败，可能已被移动或删除。"
                else -> "导入失败，请重试。"
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { vm.clearError() }) {
                        Text("关闭", color = AcksAccent)
                    }
                },
                containerColor = AcksSurface2,
                contentColor   = AcksFg
            ) {
                Text(friendlyMsg, color = AcksFg)
            }
        }

        // Importing indicator
        if (state.isImporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = AcksAccent)
                    Text("正在导入文件…", color = AcksFg2, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RecentTopBar(onPickFile: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AcksSurface)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AcksAccent),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ACKS Reader", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                Text("最近文件", color = AcksFg2, fontSize = 11.sp, lineHeight = 13.sp)
            }
            // 打开文件 button in top bar
            TextButton(onClick = onPickFile) {
                Text("打开文件", color = AcksAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = AcksFg3, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = AcksBorder, thickness = 0.5.dp)
    }
}

@Composable
private fun RecentDocCard(
    record: DocRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val fmtBadgeColor = if (record.format == "html") Color(0xFF3B82F6) else AcksAccent
    val dateStr = remember(record.importedAt) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(record.importedAt))
    }
    val theme = AcksThemes.find(record.lastThemeId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AcksSurface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(fmtBadgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint   = fmtBadgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title + meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.title,
                    color    = AcksFg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Format badge
                    Text(
                        record.format.uppercase(),
                        color    = fmtBadgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(fmtBadgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                    // Theme badge
                    if (theme != null && record.format == "markdown") {
                        Text(
                            theme.name,
                            color    = AcksFg3,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(AcksSurface2, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text("·", color = AcksFg3, fontSize = 10.sp)
                    Text(dateStr, color = AcksFg3, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = AcksFg3, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件", color = AcksFg) },
            text  = { Text("将从最近记录和沙盒中删除「${record.title}」", color = AcksFg2) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("删除", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = AcksFg2)
                }
            },
            containerColor = AcksSurface2,
        )
    }
}

@Composable
private fun EmptyState(onPickFile: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AcksSurface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(34.dp))
            }
            Text("暂无最近文件", color = AcksFg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "从微信、飞书、Telegram 等 App\n通过「打开方式」发送文件到这里",
                color     = AcksFg2,
                fontSize  = 13.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Direct file picker — uses SAF, no storage permission required
            Button(
                onClick = onPickFile,
                colors  = ButtonDefaults.buttonColors(containerColor = AcksAccent),
                shape   = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("打开本地文件", fontWeight = FontWeight.SemiBold)
            }
            Text(
                "支持 .md  .markdown  .html  .htm",
                color    = AcksFg3,
                fontSize = 11.sp
            )
        }
    }
}
