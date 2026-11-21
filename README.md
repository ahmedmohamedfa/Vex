# Vex — AI-first Android Code Editor

Vex is a local-first Android code editor built with Kotlin and Jetpack Compose. This repository is an actively developed MVP, not yet a full desktop-class IDE.

## Current features

- Dark editor interface with a dedicated Editor, Files, and AI navigation.
- Open and save a document through Android's Storage Access Framework.
- Open a folder as a project using the Storage Access Framework, browse folders and files, open files, create files, and save edits back to the selected document URI.
- Multi-document tabs with per-tab unsaved-change indicators and close confirmation before discarding edits.
- Remember the last selected project folder across app launches when Android retains the granted permission.
- Editor line-number gutter, lightweight lexical syntax coloring for common languages, and bounded undo/redo history.
- Find and replace all occurrences in the current document.
- Multiple named AI provider profiles.
- API keys encrypted locally with Android Keystore (AES-GCM).
- API key masking in the provider settings screen.
- OpenAI-compatible `/models` discovery, connection check, and model selection.
- Switch providers and models by selecting a saved profile and activating it.
- AI chat using OpenAI-compatible Chat Completions endpoints.
- Extract a fenced code block from an AI response and apply it to the current document only after tapping **Apply code**.
- HTTPS-only provider requests and improved HTTP error reporting.
- GitHub Actions debug APK build.

## AI provider compatibility

Vex currently supports providers that expose an OpenAI-compatible Chat Completions API and a compatible `GET /models` endpoint. Configure the full Chat Completions URL, model ID, and API key. Some providers restrict model-list access or use different authentication/request formats; those require a dedicated adapter and are not automatically compatible.

Provider profiles are stored in app-private preferences. API keys are encrypted with an AES-GCM key held by Android Keystore. Android app backup is disabled to avoid restoring encrypted credentials without the original device keystore key. If you uninstall the app or clear app data, saved profiles may be lost.

**Cost and security:** API usage may incur charges. Use only keys you own, never commit keys to GitHub, and do not distribute an APK containing your personal credentials. A commercial multi-user service should route requests through a properly secured backend with authentication, quotas, and abuse protection.

## Requirements

- JDK 17
- Gradle 8.11.1
- Android SDK Platform 35
- Android 8.0+ (API 26+)

## Build

```bash
gradle --no-daemon assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds a debug APK on pushes to `main`/ `master` and pull requests. Download the `Vex-debug-apk` artifact from the successful workflow run.

## Current limitations

- The Files tab provides a SAF-backed project tree; multi-document tabs preserve in-memory edits until saved or explicitly discarded. Folder and file operations depend on the selected Android document provider.
- Syntax coloring is lightweight lexical highlighting, not parser-aware highlighting. Multi-tab editing, code folding, language-server support (LSP), autocomplete, and debugging are still pending.
- AI code application currently replaces the current document with the first fenced code block. Review the response before applying; structured multi-file patches and diff previews are pending. Undo/redo currently tracks editor typing changes.
- No integrated terminal, Git UI, extension marketplace, cloud sync, account system, or billing yet.
- Only OpenAI-compatible providers are supported at present; Anthropic, Gemini-native, and other distinct APIs need provider-specific adapters.

## Roadmap

See [docs/DEVELOPMENT_ROADMAP.md](docs/DEVELOPMENT_ROADMAP.md) for phased implementation and release gates.
