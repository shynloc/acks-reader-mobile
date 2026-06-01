package studio.acks.reader.ui.sheets

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import studio.acks.reader.AcksThemes
import studio.acks.reader.Format
import studio.acks.reader.ThemeMeta
import studio.acks.reader.jsonStr
import studio.acks.reader.ui.*

// Sample markdown showcasing all major formatting elements
private val PREVIEW_MD = """# ACKS Reader
专为移动端设计的 Markdown 阅读器

## 核心特性

- **主题丰富**：13 套精美渲染主题
- **导出灵活**：PDF 与长图一键分享
- 支持微信、飞书、Telegram 打开

> 让每一篇文档都赏心悦目。好的排版是无声的设计。

```python
def render(source: str) -> str:
    return markdown_to_html(source)
```

| 格式 | 状态 | 说明 |
|------|------|------|
| Markdown | ✓ | GFM 完整支持 |
| HTML | ✓ | 安全/交互双模式 |"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(
    currentThemeId: String,
    currentMode: String,
    format: Format,
    onTheme: (String) -> Unit,
    onMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("渲染主题", color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        AcksThemes.find(currentThemeId)?.let { "${it.name} · ${it.tag}" } ?: "",
                        color = AcksFg3, fontSize = 11.sp
                    )
                }
                val currentMeta = AcksThemes.find(currentThemeId)
                if (currentMeta != null && currentMeta.modes.size > 1) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AcksSurface2)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ModeToggleBtn("light", "浅色", Icons.Default.LightMode, currentMode, onMode)
                        ModeToggleBtn("dark",  "深色", Icons.Default.DarkMode,  currentMode, onMode)
                    }
                }
            }

            if (format == Format.HTML) {
                Text(
                    "HTML 文件不使用 Markdown 主题，可在「设备」面板调整宽度",
                    color = AcksFg2, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                return@Column
            }

            // ── Live preview WebView ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, AcksBorder, RoundedCornerShape(12.dp))
            ) {
                ThemePreviewWebView(
                    themeId = currentThemeId,
                    mode    = currentMode,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("选择主题", color = AcksFg2, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(6.dp))

            // ── Theme swatch grid ─────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                items(AcksThemes.all, key = { it.id }) { meta ->
                    ThemeSwatch(
                        meta     = meta,
                        mode     = currentMode,
                        selected = meta.id == currentThemeId,
                        onClick  = { onTheme(meta.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ── Live preview WebView ──────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ThemePreviewWebView(themeId: String, mode: String, modifier: Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess   = true
                settings.domStorageEnabled = true
                settings.textZoom = 100
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun onReady() {
                        post { isReady = true }
                    }
                    @android.webkit.JavascriptInterface
                    fun onRenderDone() {}  // not needed for static preview
                    @android.webkit.JavascriptInterface
                    fun onMessage(json: String) {}
                }, "AndroidBridge")
                loadUrl("file:///android_asset/web/host.html")
            }.also { webView = it }
        }
    )

    LaunchedEffect(themeId, mode, isReady) {
        if (!isReady) return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect
        val opts = """{"themeId":${jsonStr(themeId)},"mode":${jsonStr(mode)},"interactive":false}"""
        wv.post {
            wv.evaluateJavascript("ACKS.render(${jsonStr(PREVIEW_MD)}, ${jsonStr(opts)})", null)
        }
    }
}

// ── Swatch chip (small, for the selection grid) ───────────────────────────────

@Composable
private fun ThemeSwatch(meta: ThemeMeta, mode: String, selected: Boolean, onClick: () -> Unit) {
    val effectiveMode = if (meta.modes.contains(mode)) mode else meta.defaultMode
    val bg     = Color(AcksThemes.bgFor(meta, effectiveMode))
    val accent = Color(AcksThemes.accentFor(meta, effectiveMode))
    val isLight = bg.luminance() > 0.4f
    val labelColor = if (isLight) Color(0xFF1A1A1A) else Color(0xFFEEEEEE)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                if (selected) 2.dp else 0.8.dp,
                if (selected) AcksAccent else accent.copy(alpha = 0.30f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Accent color stripe at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent)
        )
        // Theme name
        Text(
            meta.name,
            color      = labelColor,
            fontSize   = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines   = 1,
            modifier   = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 5.dp)
        )
        // Selected check
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AcksAccent),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModeToggleBtn(
    value: String, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    current: String, onToggle: (String) -> Unit
) {
    val active = current == value
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) AcksAccent else Color.Transparent)
            .clickable { onToggle(value) }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label, tint = if (active) Color.White else AcksFg2,
                modifier = Modifier.size(13.dp))
            Text(label, color = if (active) Color.White else AcksFg2, fontSize = 11.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

internal fun Color.luminance(): Float {
    fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f
        else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * lin(red) + 0.7152f * lin(green) + 0.0722f * lin(blue)
}
