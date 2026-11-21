# Vex — AI-powered Android Code Editor

A lightweight, local-first Android coding editor starter with:
- Editable code area
- Open and save files using Android's document picker
- Configurable OpenAI-compatible Chat Completions endpoint
- AI coding assistant panel
- Dark Compose interface

## Requirements
- Android Studio or Gradle 8.11.1+
- JDK 17
- Android SDK Platform 35
- Android device with Android 8.0+ (API 26+)

## Automated build
GitHub Actions builds a debug APK on pushes to `main`/`master`, pull requests, and manual workflow dispatch. Download the `Vex-debug-apk` artifact from the Actions run.

## Build
From this project directory:

```bash
gradle assembleDebug
```

The APK is generated at:

`app/build/outputs/apk/debug/app-debug.apk`

Install with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Termux notes
On Termux, install Java and Gradle if compatible packages are available:

```bash
pkg update
pkg install openjdk-17 gradle
java -version
gradle --version
```

Then unzip the project and run `gradle assembleDebug`. Android SDK setup on Termux can be more involved; if Gradle cannot locate the SDK, configure `ANDROID_HOME` to a valid Android SDK installation. Building in GitHub Actions is a practical alternative when the phone cannot host the Android SDK.

## Configure AI
Open Settings in the app and enter:
- Chat Completions URL (OpenAI-compatible)
- Model name supported by your provider
- API key

Example URL: `https://api.openai.com/v1/chat/completions`

**Important:** API calls may incur provider charges. Never commit API keys to GitHub or embed your personal key in a distributable APK. This MVP stores the key in local app preferences; production should migrate secrets to Android Keystore and use a secure backend/proxy for commercial distribution.

## Current MVP limitations
This is a first buildable foundation, not yet a full IDE:
- Basic text editor (no advanced syntax highlighting, tabs, LSP, debugger, or terminal yet)
- AI response is displayed as text; it does not automatically modify files
- Provider must support the OpenAI Chat Completions request/response format
- No billing, accounts, plugin marketplace, or cloud sync yet

## Product roadmap
1. Add a proper editor engine, syntax highlighting, tabs, search and replace.
2. Add diff preview and explicit Apply Patch workflow for AI edits.
3. Add project tree, multi-file context, Git and terminal integration.
4. Add provider adapters, usage limits, freemium entitlements and secure backend.