# Local Font Assets — 下载说明

下载 3 个字体的 WOFF2 文件放入本目录（`assets/fonts/`），合计约 150–250 KB。

## 直接运行脚本（推荐）

在 `android/` 目录下执行：

```bash
bash scripts/download-fonts.sh
```

脚本会自动从 Google Fonts CSS API 获取当前真实 URL 再下载，不会因 URL 变更而失效。

## 需要的文件

| 文件名 | 字体 |
|--------|------|
| `SpaceGrotesk.woff2` | Space Grotesk（UI / 标题字体）|
| `DMSans.woff2` | DM Sans（正文字体）|
| `JetBrainsMono.woff2` | JetBrains Mono（代码字体）|

## 说明

- 仅下载 Latin 子集（约 50–80 KB 每个文件）
- 中文字符由 Android 系统字体（Noto Sans SC）自动兜底，无需打包
- 有网络时应用会根据 `FontManager` 检测结果自动切换到 Google Fonts 或大陆镜像
- 本地字体仅在无网络或两个远端都不可达时启用
