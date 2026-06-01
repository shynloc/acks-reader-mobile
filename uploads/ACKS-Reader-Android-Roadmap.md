# ACKS Reader · Android 开发路线图

> 文档版本：2026-06-02（更新）  
> 当前状态：Phase 1 全部完成，真机验证通过，进入 Phase 2  
> 目标平台：Android 8.0+（minSdk 26），Kotlin 2.2 + Jetpack Compose + System WebView

---

## 当前进度总览

| 阶段 | 状态 | 完成时间 |
|------|------|---------|
| Phase 0：首次编译 | ✅ 完成 | 2026-06-01 |
| Phase 1：MVP 核心流程 | ✅ 完成 | 2026-06-02 |
| Phase 2：完整功能集 | 🔄 进行中（2-A/B/C/D 完成）| — |
| Phase 3：质量与稳定性 | ⏳ 待开始 | — |
| Phase 4：Beta 发布 | ⏳ 待开始 | — |

---

## Phase 1 验收结果（已通过）

| # | 验收项 | 结果 |
|---|--------|------|
| 1-A | 从文件管理器打开 .md → 渲染 | ✅ |
| 1-A | 微信"打开方式"能看到 ACKS Reader | ✅ |
| 1-A | App 内「打开本地文件」按钮（SAF picker） | ✅ |
| 1-B | 主题切换（含实时预览 WebView）| ✅ |
| 1-B | 设备/视口切换（5档 + 自定义）| ✅ |
| 1-B | 深/浅色模式切换 | ✅ |
| 1-C | PDF 导出（直接写文件，非打印对话框）| ✅ |
| 1-C | 长图导出（按 viewport 宽度，保存相册）| ✅ |
| 1-C | 分享到微信 | ✅ |

**已解决的关键 Bug**：
- KSP 2.x + Room 2.6.x 不兼容 → 升级 Room 2.7.1
- `AnimatedVisibility` 在 Kotlin 2.2 的 receiver 重载歧义 → 改用 `if`
- PDF 导出用 `createPrintDocumentAdapter` 构造器权限问题 → 改用 `PdfDocument` 直接写
- 导出宽度只截可视区域 → 改为按 `viewportCssPx × density` 调整 WebView 尺寸
- 长图不保存相册 → 加 `saveToGallery()` + `WRITE_EXTERNAL_STORAGE`（API≤28）

---

## Phase 2：完整功能集（当前阶段）

### 2-A  目录面板 TOC（预计 0.5 天）

**目标**：长文档可以快速跳转到标题。

| # | 任务 | 要点 |
|---|------|------|
| 2-A1 | 新建 `TocSheet.kt` | 从 `markdownSource` 解析 `#/##/###`，显示缩进列表 |
| 2-A2 | 点击跳转 | `webView.evaluateJavascript("ACKS.command('gotoHeading', '{\"i\":N}')", null)` |
| 2-A3 | BottomToolbar 加「目录」按钮 | 仅 Markdown 文件显示；TOC 为空时置灰 |

---

### 2-B  文档内搜索（预计 1 天）

**目标**：在渲染文档中实时高亮搜索词，支持上/下导航。

| # | 任务 | 要点 |
|---|------|------|
| 2-B1 | TopBar 下方展开式搜索条 | 点放大镜图标展开；ESC/返回键关闭 |
| 2-B2 | JS 搜索 | `ACKS.command('search', '{"q":"keyword"}')` → host.html 已内置 `doSearch` |
| 2-B3 | 上/下导航 | `ACKS.command('findnav', '{"dir":1}')` / `'{"dir":-1}'` |
| 2-B4 | 结果计数 | `AndroidBridge.onMessage` 中解析 `__acks: "searchres"` 事件，显示「3/12」 |
| 2-B5 | 关闭清除 | `ACKS.command('clearfind', '{}')` |

---

### 2-C  设置页面 ✅

| # | 任务 | 状态 |
|---|------|------|
| 2-C1 | `SettingsScreen.kt` | ✅ |
| 2-C2 | 默认主题（DataStore）| ✅ |
| 2-C3 | 字体缩放 Slider（80%-150%）| ✅ |
| 2-C4 | 清除所有文件 | ✅ |
| 2-C5 | 关于 / 版本号 | ✅ |
| 2-C6 | RecentScreen 齿轮入口 | ✅ |

---

### 2-D  首次引导 ✅

| # | 任务 | 状态 |
|---|------|------|
| 2-D1 | DataStore `isFirstRun` 标记 | ✅ |
| 2-D2 | 三步卡片 BottomSheet（Pager + 圆点指示器）| ✅ |
| 2-D3 | 「立即试用」触发 SAF picker | ✅ |

---

### 2-E  字体本地化（已完成基础设施）

| 状态 | 说明 |
|------|------|
| ✅ `scripts/download-fonts.sh` | 自动下载 SpaceGrotesk / DMSans / JetBrainsMono |
| ✅ `FontManager.kt` | 网络检测 → google / cn_mirror / local 三路路由 |
| ✅ `themes.js` LOCAL_FONT_FACES | @font-face 指向 `assets/fonts/*.woff2` |
| ⚠️ 待验证 | 需要在无网络环境下测试本地字体是否正常加载 |

---

## Phase 3：质量与稳定性

### 3-A  必须处理的异常场景

| # | 场景 | 预期处理 |
|---|------|---------|
| 3-A1 | 空文件（0 字节）| importError 提示"文件为空" |
| 3-A2 | 超大文件（>5 MB）| 显示进度条，分块渲染 |
| 3-A3 | 损坏文件（乱码）| lifecycle = ERROR，显示错误态 |
| 3-A4 | 相机截屏导致 OOM 长图 | 降级提示"文档过长，请改用 PDF" |
| 3-A5 | WebView 进程崩溃 | `onRenderProcessGone` 处理，自动重载 |
| 3-A6 | 竖/横屏切换 | ViewModel 保持所有状态，不重新导入 |

### 3-B  App 来源兼容性测试矩阵

| 来源 App | 文件类型 | 优先级 |
|---------|---------|--------|
| 系统文件管理器 | .md / .html | P0（已验证）|
| 微信 | .md 文件消息 | P0（已验证打开方式可见）|
| 飞书 | 文档下载 | P0 |
| Telegram | 文件消息 | P1 |
| Gmail 附件 | .md / .html | P1 |

### 3-C  ProGuard 规则（release 构建前必须）

```proguard
# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * { @androidx.room.* <methods>; }

# AndroidBridge（WebView JS 接口，混淆后名字变了会崩溃）
-keepclassmembers class studio.acks.reader.PreviewWebView$Bridge {
    public *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```

---

## Phase 4：Beta 发布准备

### 4-A  应用图标

| 状态 | 说明 |
|------|------|
| ✅ 自适应图标 XML | `mipmap-anydpi-v26/ic_launcher.xml`（橙色背景 + N 标志）|
| ❌ PNG 备选 | 需要生成 mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi 各密度 PNG |

推荐尺寸：1024×1024 源文件 → 用 Android Studio 的 Image Asset Studio 批量生成。

### 4-B  签名 + Release 构建

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "")
        storePassword = System.getenv("KEYSTORE_PASS") ?: ""
        keyAlias = System.getenv("KEY_ALIAS") ?: ""
        keyPassword = System.getenv("KEY_PASS") ?: ""
    }
}
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### 4-C  内测分发

| 渠道 | 适用阶段 |
|------|---------|
| 微信/飞书群直接发 APK | 极早期 5-10 人内测 |
| Firebase App Distribution | 20-100 人内测 |
| Google Play 内测轨道 | 正式 Beta |

---

## 待决策事项（更新）

| # | 问题 | 当前状态 |
|---|------|---------|
| D-1 | 字体离线策略 | ✅ 已实现三路路由，待真机无网验证 |
| D-2 | ZIP 包支持 | 暂不做，Phase 2 结束后评估 |
| D-3 | 长图超高限制（当前 15,000px）| OOM 后提示改 PDF，暂时够用 |
| D-4 | iOS 版 | 等 Android Beta 后评估 |
| D-5 | 商业化模式 | 未定 |

---

_最后更新：2026-06-02_
