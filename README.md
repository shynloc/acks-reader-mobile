<div align="center">

<img src="uploads/touming N.png" width="96" height="96" alt="ACKS Reader Logo" />

# ACKS Reader

**让 Markdown 和 HTML 文档，在手机上以最美的姿态呈现。**

[![Version](https://img.shields.io/badge/version-1.0.0-F26419?style=flat-square&logo=android&logoColor=white)](https://github.com/shynloc/acks-reader-mobile/releases/latest)
[![Platform](https://img.shields.io/badge/platform-Android%208.0+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-MIT-22C55E?style=flat-square)](LICENSE)
[![Release](https://img.shields.io/github/v/release/shynloc/acks-reader-mobile?style=flat-square&color=F26419)](https://github.com/shynloc/acks-reader-mobile/releases/latest)

[**下载 APK**](https://github.com/shynloc/acks-reader-mobile/releases/latest) · [反馈 Bug](https://github.com/shynloc/acks-reader-mobile/issues) · [查看 Roadmap](uploads/ACKS-Reader-Android-Roadmap.md)

</div>

---

## 是什么

ACKS Reader 是一款专为 **Markdown / HTML 文档**设计的 Android 阅读器，主打**精美排版**与**一键导出**。

从微信、飞书、Telegram 收到 `.md` 文件？点「打开方式」选 ACKS Reader，秒变精美排版，PDF 或长图一键导出分享。

---

## 功能全览

### 文件来源

| 来源 | 支持情况 |
|------|---------|
| 微信文件消息 | ✅ 「打开方式」直接打开 |
| 飞书下载文件 | ✅ |
| Telegram 文件 | ✅ |
| 系统文件管理器 | ✅ SAF 选择器 |
| Gmail 附件 | ✅ |
| 支持格式 | `.md` `.markdown` `.html` `.htm` |

### 渲染引擎

| 功能 | 说明 |
|------|------|
| Markdown → HTML | 自研轻量解析器，CommonMark + GFM |
| 表格 / 任务列表 / 删除线 | ✅ |
| 代码高亮 | ✅ 支持多语言语法着色 |
| **Mermaid 流程图** | ✅ 内置渲染器，支持 `flowchart TD/LR`，纯 JS 无外部依赖 |
| **数学公式（TeX）** | ✅ 内联 `$…$` 与块级 `$$…$$`，支持分数、根号、求和、积分等 |
| HTML 安全预览 | ✅ 屏蔽所有 `<script>`，防止恶意代码 |
| HTML 交互模式 | ✅ 完整 JS / Canvas / SVG / 动画，适合 AI 生成页面 |

### 主题系统（13 款）

| 主题 | 风格 | 深色 |
|------|------|------|
| AI 报告 | 商务紫调 | ✅ |
| 简洁阅读 | 暖白纸质感 | ✅ |
| 商业报告 | 企业蓝正式 | ✅ |
| 技术文档 | GitHub 风绿 | ✅ |
| 深色代码 | 纯深色代码 | — |
| 社交长图 | 高对比橙 | ✅ |
| 学术论文 | 乳白衬线 | — |
| 微信图文 | 微信绿清爽 | — |
| 杂志美文 | 暖棕人文 | ✅ |
| 欧式极简 | 金黑极简 | ✅ |
| 国风古典 | 宣纸风 | — |
| 竖排古风 | 传统竖排 | — |
| 海报封面 | 大字封面 | ✅ |

### 预览与排版

| 功能 | 说明 |
|------|------|
| 5 档视口宽度 | 手机 / 桌面 1024px / A4 794px / 社交 480px / 自定义 |
| 深色 / 浅色模式 | 每个主题独立控制 |
| **全文搜索** | 实时高亮 · 上/下跳转 · 结果计数（如 3/12） |
| **文档目录 TOC** | 自动解析标题 · 点击跳转 · 实时追踪当前位置 |
| 字体缩放 | 80% – 150% 自由调节，实时生效 |

### 导出与分享

| 功能 | 说明 |
|------|------|
| **PDF 导出** | 直接写文件，非系统打印弹窗，精准还原排版 |
| **长图导出** | 按视口宽度截图，自动保存相册 |
| 一键分享 | 系统分享面板，直发微信 / 朋友圈 |
| WYSIWYG | 导出结果与预览 100% 一致 |

### 设置与个性化

| 功能 | 说明 |
|------|------|
| **App 界面主题** | 深色 / 浅色 / 跟随系统，首页一键快速切换 |
| 默认文档主题 | DataStore 持久化，新文件自动应用 |
| 默认视口 | 手机 / 桌面 / A4 / 社交 |
| 默认 HTML 模式 | 安全预览 / 交互模式 |
| 字体缩放（全局）| 80% – 150% |
| Mermaid 开关 | 可单独禁用流程图渲染 |
| 数学公式开关 | 可单独禁用 TeX 渲染 |
| 管理历史文件 | 首页每张卡单独删除 |
| 清除所有文件 | 一键清空历史记录与沙盒缓存 |

### 健壮性

| 场景 | 处理方式 |
|------|---------|
| 空文件 | 友好提示「文件为空」 |
| 超大文件 >5MB | 截取前 5MB 渲染 + 顶部警告条 |
| 乱码 / 损坏文件 | 降级解码，彻底无法识别时显示「文件可能已损坏」 |
| 长图 OOM | 提示「请改用 PDF 导出」 |
| WebView 崩溃 | 自动重载，不崩 App |
| 横竖屏切换 | ViewModel 保持全部状态 |

---

## 快速上手

### 安装

[**直接下载 APK →**](https://github.com/shynloc/acks-reader-mobile/releases/latest)

或自行编译：

```bash
git clone git@github.com:shynloc/acks-reader-mobile.git
cd acks-reader-mobile/android

# （可选）下载本地字体，用于无网络环境
bash scripts/download-fonts.sh

./gradlew installDebug
```

**环境要求**：Android Studio Ladybug+，JDK 17，Android 8.0+（minSdk 26）

### 使用流程

```
微信 / 飞书 / Telegram 收到 .md 文件
         ↓
    点击「打开方式」
         ↓
    选择 ACKS Reader
         ↓
  自动渲染 · 选择主题 · 调整视口
         ↓
    导出 PDF 或长图
         ↓
      直接分享 ✨
```

---

## 项目结构

```
ACKS Reader
├── android/                    # Android 原生工程（本仓库主体）
│   └── app/src/main/
│       ├── assets/web/         # 渲染引擎（WebView 加载）
│       │   ├── host.html       # WebView 宿主页
│       │   ├── markdown.js     # Markdown → HTML 解析器
│       │   ├── themes.js       # 13 款主题定义 + CSS 生成
│       │   └── extensions.js   # Mermaid + TeX 渲染器
│       └── java/studio/acks/reader/
│           ├── MainActivity.kt         # 入口 + Intent 接收
│           ├── ReaderViewModel.kt      # 全局状态（StateFlow）
│           ├── DocState.kt             # 文档状态模型
│           ├── ImportController.kt     # 文件导入 + 异常处理
│           ├── ExportController.kt     # PDF / 长图导出
│           ├── PreviewWebView.kt       # WebView 封装 + JS Bridge
│           ├── FontManager.kt          # 字体路由（Google/镜像/本地）
│           ├── AppSettings.kt          # DataStore 偏好设置
│           ├── DocDatabase.kt          # Room 数据库 + Migration
│           └── ui/
│               ├── RecentScreen.kt     # 首页（最近文件）
│               ├── PreviewScreen.kt    # 预览页（搜索/工具栏）
│               ├── SettingsScreen.kt   # 设置页
│               ├── AboutScreen.kt      # 关于页
│               └── sheets/             # 主题/视口/导出/TOC/引导等面板
├── render-core/                # Web 渲染引擎源码
├── app/                        # Web 版 App 源码（React）
├── spec/                       # 设计规格文档
└── uploads/                    # 开发文档 + Roadmap
```

**技术栈**

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 状态管理 | ViewModel + StateFlow + Coroutines |
| 持久化 | Room 2.7 + DataStore Preferences |
| 渲染 | Android System WebView + JS Bridge |
| 字体 | Space Grotesk / DM Sans / JetBrains Mono（三路路由） |
| 安全 | SAF 文件访问，无 READ_EXTERNAL_STORAGE 权限 |

---

## 开发进度

```
✅ Phase 1  MVP：文件打开 · 主题切换 · PDF/长图导出
✅ Phase 2  完整功能：TOC · 搜索 · 设置 · 首次引导
✅ Phase 3  质量：异常处理 · ProGuard · WebView 崩溃恢复
✅ Phase 4  发布准备：图标 · 签名 · Release APK
🚀 v1.0.0  正式发布
```

---

## 贡献 & 反馈

处于早期版本，欢迎提 [Issue](https://github.com/shynloc/acks-reader-mobile/issues) 反馈 Bug 或功能建议。

---

<div align="center">

Made with ❤️ by **Thom** · Powered by **ACKS Studio**

[![GitHub](https://img.shields.io/badge/GitHub-shynloc-181717?style=flat-square&logo=github)](https://github.com/shynloc)

</div>
