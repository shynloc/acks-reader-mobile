# ACKS Reader — Android Skeleton

Runnable-shape scaffold for the Android client described in
**ACKS Reader Handoff (Coding Agent)**. This is a starting skeleton, not a
finished app: it wires the critical path (intent → import → sandbox → WebView →
render core) and stubs the shell so you can build the slices in §9 of the handoff.

## Layout

```
android/
├─ app/src/main/
│  ├─ AndroidManifest.xml          # intent filters (open-with + share)
│  ├─ assets/web/                  # ← copy render-core/* here
│  │   ├─ host.html  markdown.js  themes.js  extensions.js
│  └─ java/studio/acks/reader/
│      ├─ MainActivity.kt          # entry, reads the incoming intent
│      ├─ ImportController.kt      # content:// → sandbox copy + format detect
│      ├─ DocState.kt              # state model + Overrides + AcksMessage
│      ├─ PreviewWebView.kt        # WebView host + AndroidBridge + ACKS.render
│      └─ ui/PreviewScreen.kt      # Compose shell stub (top bar + WebView + toolbar)
└─ build.gradle.kts                # module deps (Compose + WebView)
```

## Bring-up

1. Open `android/` in Android Studio (or create a fresh Compose project and drop
   these files in matching the package `studio.acks.reader`).
2. Copy the four files from `render-core/` into `app/src/main/assets/web/`.
3. Run on a device/emulator. Share a `.md` from any app → "ACKS Reader".
4. Then implement slices 2–6 from the handoff (themes picker, viewport, export,
   gestures, platform polish).

> The render engine is already done — these Kotlin files only wrap it. Match the
> design spec tokens/components when fleshing out `ui/`.
