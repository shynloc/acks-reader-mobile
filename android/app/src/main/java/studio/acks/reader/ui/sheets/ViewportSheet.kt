package studio.acks.reader.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.ui.*

private data class DeviceOption(
    val id: String,
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val widthHint: String
)

private val DEVICE_OPTIONS = listOf(
    DeviceOption("phone",   "手机",   "跟随设备宽度",  Icons.Default.PhoneAndroid, ""),
    DeviceOption("desktop", "桌面",   "1024 px",      Icons.Default.Monitor,      "1024px"),
    DeviceOption("a4",      "A4",     "794 px",       Icons.Default.Article,       "794px"),
    DeviceOption("social",  "长图",   "480 px",       Icons.Default.Image,         "480px"),
    DeviceOption("custom",  "自定义", "手动输入宽度",   Icons.Default.Tune,          ""),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewportSheet(
    current: String,
    customWidth: Int,
    onViewport: (String) -> Unit,
    onCustomWidth: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localCustom by remember { mutableStateOf(customWidth.toString()) }

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
            Text("预览设备", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp))
            Text("调整渲染内容的宽度，模拟不同设备的显示效果", color = AcksFg3, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 10.dp))

            DEVICE_OPTIONS.forEach { opt ->
                val selected = current == opt.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AcksAccentSoft else Color.Transparent)
                        .border(
                            if (selected) 1.dp else 0.dp,
                            if (selected) AcksAccent.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onViewport(opt.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(opt.icon, contentDescription = null,
                        tint = if (selected) AcksAccent else AcksFg2,
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opt.label,
                            color = if (selected) AcksAccent else AcksFg,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium)
                        Text(opt.sub, color = AcksFg3, fontSize = 11.sp)
                    }
                    if (opt.widthHint.isNotEmpty()) {
                        Text(opt.widthHint, color = AcksFg3, fontSize = 10.sp,
                            modifier = Modifier.padding(end = 4.dp))
                    }
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = AcksAccent, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Custom width input — only visible when "custom" is selected
            AnimatedVisibility(visible = current == "custom") {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    HorizontalDivider(color = AcksBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Text("自定义宽度（px）", color = AcksFg2, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = localCustom,
                            onValueChange = { v ->
                                if (v.length <= 5 && v.all { it.isDigit() }) localCustom = v
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AcksAccent,
                                unfocusedBorderColor = AcksBorder,
                                focusedTextColor     = AcksFg,
                                unfocusedTextColor   = AcksFg,
                                cursorColor          = AcksAccent
                            ),
                            suffix = { Text("px", color = AcksFg3, fontSize = 12.sp) }
                        )
                        Button(
                            onClick = {
                                val w = localCustom.toIntOrNull()?.coerceIn(240, 2560) ?: 600
                                localCustom = w.toString()
                                onCustomWidth(w)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AcksAccent)
                        ) { Text("应用") }
                    }
                    Text("推荐范围：320 – 1440 px", color = AcksFg3, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
