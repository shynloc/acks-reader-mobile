package studio.acks.reader.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.BuildConfig
import studio.acks.reader.ReaderViewModel

@Composable
fun AboutScreen(vm: ReaderViewModel) {
    val ctx = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(AcksBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Column(modifier = Modifier.fillMaxWidth().background(AcksSurface)) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.navBackFromAbout() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = AcksFg2)
                    }
                    Text("关于 ACKS Reader", color = AcksFg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = AcksBorder, thickness = 0.5.dp)
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Hero ─────────────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(AcksAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AN", color = Color.White, fontSize = 28.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("ACKS Reader", color = AcksFg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "版本 ${BuildConfig.VERSION_NAME}",
                            color = AcksFg3, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "让 Markdown 和 HTML 文档\n在手机上以最美的姿态呈现",
                            color = AcksFg2, fontSize = 14.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── 功能亮点 ──────────────────────────────────────────────────
                item {
                    SectionCard {
                        FeatureRow(Icons.Default.Palette,       "12 款精美主题",     "AI报告、商业简报、国风古典等多种风格")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        FeatureRow(Icons.Default.Search,         "全文搜索",          "实时高亮，上/下导航，结果计数")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        FeatureRow(Icons.Default.AccountTree,    "Mermaid 流程图",    "支持 flowchart TD/LR，自动布局渲染")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        FeatureRow(Icons.Default.Functions,      "数学公式",          "支持 TeX 语法内联 \$…\$ 和块 \$\$…\$\$")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        FeatureRow(Icons.Default.PictureAsPdf,   "PDF / 长图导出",    "WYSIWYG，导出结果与预览完全一致")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        FeatureRow(Icons.Default.Security,       "HTML 安全预览",     "屏蔽脚本保护隐私，或切换交互模式")
                    }
                }

                // ── 链接 ──────────────────────────────────────────────────────
                item {
                    SectionCard {
                        LinkRow(
                            icon  = Icons.Default.Code,
                            label = "GitHub 开源仓库",
                            url   = "https://github.com/shynloc/acks-reader-mobile",
                            ctx   = ctx
                        )
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
                        LinkRow(
                            icon  = Icons.Default.BugReport,
                            label = "反馈 Bug / 功能建议",
                            url   = "https://github.com/shynloc/acks-reader-mobile/issues",
                            ctx   = ctx
                        )
                    }
                }

                // ── 技术栈 ────────────────────────────────────────────────────
                item {
                    SectionCard {
                        TechRow("渲染引擎",   "Android System WebView")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                        TechRow("UI 框架",   "Jetpack Compose + Material 3")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                        TechRow("数据库",    "Room 2.7 + DataStore")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                        TechRow("Markdown", "自研轻量解析器（CommonMark + GFM）")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                        TechRow("图表",     "内置 Mermaid 渲染器（纯 JS，无依赖）")
                        HorizontalDivider(color = AcksBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                        TechRow("数学",     "内置 TeX 子集渲染器")
                    }
                }

                // ── 致谢 ──────────────────────────────────────────────────────
                item {
                    Text(
                        "Made with ❤ by Thom · Powered by ACKS Studio",
                        color = AcksFg3, fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AcksSurface),
        content = content
    )
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AcksAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = AcksFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = AcksFg3, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    url: String,
    ctx: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AcksFg2, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = AcksFg, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AcksFg3, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AcksFg2, fontSize = 13.sp, modifier = Modifier.weight(0.35f))
        Text(value, color = AcksFg, fontSize = 13.sp, modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End)
    }
}
