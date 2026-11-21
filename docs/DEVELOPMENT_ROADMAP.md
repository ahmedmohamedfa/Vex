# Vex Development Roadmap

Vex is an Android-first coding studio. The project is being developed in verifiable milestones.

## Milestone 0 — Buildable foundation
- Kotlin and Jetpack Compose application shell
- Android Storage Access Framework for opening and saving documents
- Configurable OpenAI-compatible chat-completions endpoint
- GitHub Actions debug APK build

## Milestone 1 — Editor reliability
- Add editor state and document lifecycle handling
- Integrate Sora Editor with native line numbers, TextMate syntax grammars, selection actions, search, undo/redo and auto-indent
- Add safe handling for large files and unsaved changes
- Add focused unit and Android device tests for editor lifecycle, keyboard input, save/restore and large documents

## Milestone 2 — Project workspace
- Improve SAF project tree operations and recent-workspace recovery
- Create, rename, move and delete files with confirmation
- Persist workspace permissions and recent projects
- Tabs and split-pane mobile navigation

## Milestone 3 — AI Agent
- Project-aware context selection
- Structured edit proposals and unified diff preview
- Explicit user approval before writing files
- Cancellation, streaming, timeouts and clear error states
- Never silently overwrite user files

## Milestone 4 — Developer tools
- Git status, diff and commit workflows
- Sandboxed command execution strategy suitable for Android
- Build/run integrations where supported
- Logs and diagnostics

## Release gates
A milestone is not complete until its code is committed, CI passes, and the relevant behavior is tested on an Android device or emulator. A successful upload alone is not a successful build.

## Security baseline
- Do not embed personal API keys in the APK or repository.
- Prefer encrypted local storage for provider credentials.
- Use HTTPS endpoints only.
- Require confirmation before destructive file operations or AI-applied changes.
