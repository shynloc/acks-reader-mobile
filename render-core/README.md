# ACKS Reader · Render Core (drop-in for native WebView)

> **Single source of truth.** These `.js` files are the canonical render engine.
> The HTML prototype (`ACKS Reader.html`) and the screen-collection doc load them
> **directly** from this folder — there is no longer a separate `app/*.jsx` copy to
> keep in sync. Edit here; both the reference build and the native app stay aligned.

These four files are the **platform-agnostic rendering engine**.

| File | Exports (on `window`) | Purpose |
|------|----------------------|---------|
| `markdown.js`   | `renderMarkdown(src) -> html` | Markdown → semantic HTML (GFM tables, task lists, code fences, callouts, links, images, inline `$math$`, ` ```mermaid ` blocks) |
| `themes.js`     | `THEMES`, `THEME_MAP`, `buildThemeDoc(themeId, mdHtml, opts) -> full HTML doc`, `ensureGoogleFont(spec)` | 13 rendering themes + document builder + on-demand fonts |
| `extensions.js` | `renderMermaid(src)`, `renderMath(tex, display)` | Mermaid flowcharts + math (TeX subset) |
| `host.html`     | `window.ACKS.render / renderHtml / command` | Thin host the native WebView loads; bridges to `window.AndroidBridge` |

## Android install

1. Copy all four files into `app/src/main/assets/web/`.
2. Load once: `webView.loadUrl("file:///android_asset/web/host.html")`.
3. After `onPageFinished`, render a document:

```kotlin
val opts = """{"themeId":"aireport","mode":"dark","interactive":false,
               "ov":{"baseSize":16}}"""
webView.evaluateJavascript(
  "ACKS.render(${JSONObject.quote(markdownSource)}, ${JSONObject.quote(opts)})", null)
```

4. Switching theme / viewport / type = just call `ACKS.render(...)` again with new opts
   (instant, no reload). Viewport width is handled natively by sizing the WebView /
   using `setInitialScale` — the document itself is fluid.

## Message protocol (document ⇄ native)

The injected gesture script posts `window.postMessage({__acks:'...'} )`. `host.html`
forwards every `__acks` message to `AndroidBridge.onMessage(json)`
(register a `@JavascriptInterface` named `AndroidBridge`). Native → document uses
`ACKS.command(name, argJson)`. Full message table is in the Handoff doc, §9.

## Theme list

`clean, business, technical, darkcode, social, academic, wechat, magazine,
aireport, euro, cnclassic, cnvertical, poster`  — see `THEMES` for metadata
(`id, name, en, modes, defaultMode, swatch`).

## Notes

* `buildThemeDoc` returns a **complete** `<!DOCTYPE html>` document including a
  Google Fonts `<link>` and all theme CSS — load it with
  `loadDataWithBaseURL` or via `document.write` (host.html does the latter).
* `interactive:true` injects the gesture script (long-press select, pinch/double-tap
  zoom, search highlight, TOC tracking). For `.md` files keep it `true`; for untrusted
  `.html` files in Safe Preview keep scripts off.
* No network needed except Google Fonts. Bundle WOFF2 locally if you need full offline.
