<div align="center">

<img src="uploads/touming N.png" width="96" height="96" alt="ACKS Reader Logo" />

# ACKS Reader

**让 Markdown 和 HTML 文档，在手机上以最美的姿态呈现。**

[![Version](https://img.shields.io/badge/version-0.1.0-F26419?style=flat-square&logo=android&logoColor=white)](https://github.com/shynloc/acks-reader-mobile/releases)
[![Platform](https://img.shields.io/badge/platform-Android%208.0+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-MIT-22C55E?style=flat-square)](LICENSE)
[![Status](https://img.shields.io/badge/status-Beta-F59E0B?style=flat-square)]()

</div>

---

## 简介

ACKS Reader 是一款专为 Markdown 和 HTML 文档设计的 Android 阅读器，专注于**专业排版**与**一键导出**。无论是 AI 生成的报告、技术文档、学术论文，还是微信图文，都能以精美的主题展示，并支持直接导出为 PDF 或长截图分享。

从微信、飞书、Telegram 收到 `.md` 文件？直接「打开方式」选 ACKS Reader，秒变精美排版。

---

## 功能特性

### 核心能力

| 功能 | 说明 |
|------|------|
| 📄 **多格式支持** | `.md` `.markdown` `.html` `.htm` 全支持 |
| 🎨 **12 款精美主题** | 涵盖 AI 报告、商业简报、学术论文、微信图文、国风古典等风格 |
| 🌗 **深/浅色模式** | 每个主题独立的深浅色切换 |
| 📐 **5 档视口宽度** | 手机、桌面（1024px）、A4（794px）、社交（480px）、自定义 |
| 📑 **文档目录 TOC** | 自动解析标题，点击快速跳转 |
| 🔍 **全文搜索** | 实时高亮，上/下导航，显示结果计数 |

### 导出与分享

| 功能 | 说明 |
|------|------|
| 📕 **PDF 导出** | 直接写文件，非系统打印对话框，格式精准 |
| 🖼 **长图导出** | 按视口宽度截图，自动保存相册 |
| 📤 **一键分享** | 支持分享到微信、飞书等 App |

### 个性化设置

| 功能 | 说明 |
|------|------|
| 🎭 **默认主题** | DataStore 持久化，新文件自动应用 |
| 🔤 **字体缩放** | 80% – 150% 自由调节，实时生效 |
| 🗂 **最近文件** | 自动记录，沙盒缓存，无需重复导入 |
| 🧹 **一键清除** | 清除全部历史记录与缓存文件 |

---

## 技术架构

```
ACKS Reader
├── render-core/          # 渲染引擎（Web 端，主题/Markdown 解析）
│   ├── themes.js         # 12 款主题定义
│   ├── renderer.js       # Markdown 渲染逻辑
│   └── host.html         # WebView 宿主页
│
├── android/              # Android App
│   └── app/src/main/java/studio/acks/reader/
│       ├── MainActivity.kt         # 入口，SAF 文件接收
│       ├── ReaderViewModel.kt      # 全局状态管理（StateFlow）
│       ├── DocState.kt             # 文档状态模型
│       ├── ImportController.kt     # 文件导入（沙盒复制）
│       ├── ExportController.kt     # PDF / 长图导出
│       ├── FontManager.kt          # 字体路由（Google / 镜像 / 本地）
│       ├── AppSettings.kt          # DataStore 用户偏好
│       ├── DocDatabase.kt          # Room 数据库
│       └── ui/
│           ├── RecentScreen.kt     # 最近文件列表
│           ├── PreviewScreen.kt    # 文档预览（含搜索栏）
│           ├── SettingsScreen.kt   # 设置页面
│           └── sheets/             # 主题/视口/导出/TOC/引导等面板
│
└── app/                  # Web 版入口（离线单文件 HTML）
```

**依赖技术栈**

- **UI**：Jetpack Compose + Material 3
- **状态**：ViewModel + StateFlow + Kotlin Coroutines  
- **持久化**：Room 2.7 + DataStore Preferences
- **渲染**：System WebView + JavaScript Bridge
- **字体**：Space Grotesk / DM Sans / JetBrains Mono（三路路由）

---

## 快速开始

### 环境要求

- Android Studio Ladybug（2024.2）或更高
- JDK 17
- Android SDK API 35
- 真机或模拟器（Android 8.0+）

### 编译运行

```bash
git clone git@github.com:shynloc/acks-reader-mobile.git
cd acks-reader-mobile/android

# 下载本地字体（可选，无网络环境需要）
bash scripts/download-fonts.sh

# 编译并安装调试包
./gradlew installDebug
```

### 从微信/飞书接收文件

1. 在聊天中收到 `.md` 文件
2. 点击文件 → 「更多」→「用其他应用打开」
3. 选择 **ACKS Reader**
4. 自动渲染，选择喜欢的主题，导出分享 ✨

---

## 主题预览

| 主题 | 风格标签 | 深色支持 |
|------|---------|---------|
| **AI 报告** `aireport` | 商务 | ✅ |
| **简洁阅读** `clean` | 阅读 | ✅ |
| **商业报告** `business` | 商务 | ✅ |
| **技术文档** `technical` | 技术 | ✅ |
| **深色代码** `darkcode` | 代码 | — |
| **社交长图** `social` | 社交 | ✅ |
| **学术论文** `academic` | 学术 | — |
| **微信图文** `wechat` | 社交 | — |
| **杂志美文** `magazine` | 杂志 | ✅ |
| **欧式极简** `euro` | 极简 | ✅ |
| **国风古典** `cnclassic` | 国风 | — |

---

## 开发路线图

```
✅ Phase 0  首次编译上机
✅ Phase 1  MVP：文件打开 · 主题切换 · PDF/长图导出
✅ Phase 2  完整功能：TOC · 搜索 · 设置 · 首次引导
🔄 Phase 3  质量：异常处理 · 来源兼容性 · ProGuard
⏳ Phase 4  Beta：图标 · 签名 · 内测分发
```

---

## 项目结构说明

```
ACKS Reader (离线单文件).html   # Web 版，无依赖，可直接浏览器打开
ACKS Reader.html               # Web 版入口（在线加载资源）
android/                       # Android 原生工程
app/                           # Web App 源码（React）
render-core/                   # 渲染引擎核心
assets/                        # 静态资源
spec/                          # 设计规格文档
uploads/                       # 开发文档
```

---

## 贡献

目前处于早期开发阶段，如有 Bug 或功能建议，欢迎提 [Issue](https://github.com/shynloc/acks-reader-mobile/issues)。

---

<div align="center">

Made with ❤️ by **Thom** · Powered by **ACKS Studio**

[![GitHub](https://img.shields.io/badge/GitHub-shynloc-181717?style=flat-square&logo=github)](https://github.com/shynloc)

</div>
