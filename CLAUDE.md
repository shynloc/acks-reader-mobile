# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Install

There is no `gradlew` script committed. Use the cached Gradle distribution directly:

```bash
# Build signed release APK
cd android
KEYSTORE_PATH='/Users/taojin/ACKS Reader/android/acks-release.jks' \
KEYSTORE_PASS='acks2026' KEY_ALIAS='acks' KEY_PASS='acks2026' \
~/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle assembleRelease

# Install to connected device (ADB)
adb install -r app/build/outputs/apk/release/app-release.apk

# Uninstall first if signature mismatch
adb uninstall studio.acks.reader
```

No automated tests exist. Verify changes by installing and testing on device.

## Architecture Overview

The app has two distinct layers that communicate via a JS bridge:

### Kotlin / Android layer (`android/app/src/main/java/studio/acks/reader/`)

| File | Role |
|------|------|
| `ReaderViewModel.kt` | Single source of truth. Holds `AppUiState` (a sealed data class tree). All screens read from `ui: StateFlow<AppUiState>`. |
| `DocState.kt` | Immutable model for the open document: format, theme, viewport, markdown source, lifecycle enum. `renderOptsJson()` serialises to JSON for the JS bridge. |
| `PreviewWebView.kt` | Wraps the single persistent `WebView`. Loads `host.html` once; drives it via `ACKS.render()` / `ACKS.renderHtml()` through `evaluateJavascript`. Uses `LAYER_TYPE_HARDWARE`. |
| `ImportController.kt` | Reads the file via SAF URI, copies to internal sandbox (`filesDir/documents/<id>/`), detects format, handles >5 MB truncation and encoding fallbacks. |
| `ExportController.kt` | PDF + long-image export. **Must switch to `LAYER_TYPE_SOFTWARE` before resizing and calling `draw()`** — hardware WebView only keeps GPU tiles for the visible viewport; software mode renders the full DOM synchronously. Adds `delay(400)` after resize for the renderer to re-layout. |
| `CardExportController.kt` | Card-image export. Creates a fresh software WebView (never attached to a window), loads the long-form HTML from `buildLongDocForSlice()` at **MAX_H = 20 000 px** from the start so content renders fully, then slices the captured bitmap into 540×720 → 1080×1440 JPEG cards. |
| `FontManager.kt` | Probes Google Fonts → CN mirror → local WOFF2 assets. Result cached per session. |
| `AcksThemes.kt` | Kotlin mirror of the JS theme list (ids, swatch colours, modes). Used by card-export UI; not used for actual HTML rendering. |
| `DocDatabase.kt` | Room DB, table `documents` (`DocRecord`). Current schema version 2. Migrations live here. |

### Web / JavaScript layer (`android/app/src/main/assets/web/`)

All JS runs inside the single persistent WebView. Files are loaded as local assets.

| File | Exports |
|------|---------|
| `host.html` | Entry point. Loads all JS, exposes `window.ACKS = { render, renderHtml, command }`. Binds `AndroidBridge` JS interface for native ↔ JS messages. |
| `markdown.js` | `window.renderMarkdown(src)` → HTML string. CommonMark + GFM (tables, tasks, strikethrough), code highlighting, Mermaid placeholders. |
| `themes.js` | `window.THEMES`, `window.THEME_MAP`, `window.buildThemeDoc(id, mdHtml, opts)`, `window.getFontsHtml(source)`. 13 themes, each has `css(mode)` generator and `swatch.{light,dark}` colours. |
| `extensions.js` | Mermaid + TeX rendering, in-page find, TOC heading tracking. Driven by `ACKS.command(name, argJson)` calls from Kotlin. |
| `card-export.js` | Card export engine. `window.buildLongDocForSlice(markdown, opts)` → full self-contained HTML (cover div at top + continuous content). `window.getThemeSwatchColors(themeId)` → `{bg, fg, acc}` JSON. Old paginated API (`buildCardPages` / `getCardPageHtml`) still present but no longer used for export. |

### JS ↔ Kotlin bridge

- **Kotlin → JS**: `webView.evaluateJavascript(script, callback)`. Large strings are JSON-encoded in the callback result and must go through `decodeJsString()`.
- **JS → Kotlin**: `AndroidBridge.onMessage(json)` (`@JavascriptInterface`). Messages are `{__acks: "kind", ...}` objects posted via `window.postMessage`.
- `ACKS.command(name, argJson)` in JS posts a message to parent frame which `AndroidBridge` receives.

### Export rendering rules

**Critical constraint**: the main `PreviewWebView` uses `LAYER_TYPE_HARDWARE`. Hardware WebView only rasterises tiles near the visible viewport. Any export that needs off-screen content **must** either:

1. Switch `webView.setLayerType(LAYER_TYPE_SOFTWARE, null)` → resize → `delay(400)` → `draw()` → restore (used by `ExportController`).
2. Use a freshly created software WebView pre-sized to `MAX_H` before loading HTML (used by `CardExportController`).

### Card export specifics

- Card size: 540×720 CSS px rendered → scaled to 1080×1440 JPEG output.
- `buildLongDocForSlice` embeds the cover as an `acks-cover` div with `height: 720px; overflow: hidden` at the top of the HTML body. Slicing at every 720 px boundary makes the first slice the cover card.
- Page numbers are drawn by Kotlin `Canvas.drawText` on the scaled bitmap (not in CSS), so they don't interfere with layout.
- `evaluateJavascript` returns the HTML string (~20–100 KB for typical docs); encoded as a JSON string and decoded by `decodeJsString()`. Very long documents approaching ~500 KB encoded may hit Binder IPC limits.

## Key Invariants

- **One WebView, one host.html session**. `ACKS.render()` uses `document.open/write/close` to replace the full document; this resets all JS state. `card-export.js` must therefore re-render markdown each time it's called.
- **Font source is resolved once at startup** (`FontManager.resolve`). The result (`google` / `cn_mirror` / `local`) is stored in `ReaderViewModel.resolvedFontSource` and passed into every render call.
- **`DocState.markdownSource`** always holds the raw source text (markdown or HTML). It is passed to JS for rendering and also to `CardExportController` for card generation.
- **Room migration**: increment `DocDatabase` `version` and add a `Migration` object whenever the `DocRecord` schema changes.
