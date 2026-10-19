# Phone Agent

**A privacy-first Android task agent foundation.** Native Android UI, local task drafts, explicit permission boundaries, and reproducible APK builds through GitHub Actions.

> Current stage: foundation only. Tasks are saved locally as drafts. The app does not control other apps, capture screens, use the network, or request Accessibility permissions yet.

## Product principles

- **Local by default:** task drafts stay in the app's private storage.
- **No implied access:** the app does not claim permissions it has not requested or received.
- **Human approval for sensitive actions:** future execution capabilities must be gated behind a clear review step.
- **Auditable changes:** capability modules will be added one at a time with tests and documented permission requirements.

## Current features

- Native Kotlin + Jetpack Compose interface in Arabic.
- Local task draft creation and persistence.
- Dedicated tasks and security/readiness screens.
- No runtime permissions requested in the initial manifest.
- GitHub Actions workflow for unit tests and a debug APK artifact.

## Build

A local Android SDK and JDK 17 are required.

```bash
gradle :app:assembleDebug :app:testDebugUnitTest
```

The hosted workflow installs Gradle 8.13 and Android SDK API 36 and uploads `app-debug.apk` as a workflow artifact after a successful build.

## Structure

```
app/
  src/main/
    java/io/phoneagent/mobile/
      data/       # local task storage and model
      ui/         # Compose screens and visual theme
      MainActivity.kt
    res/          # app icon resources and theme
.github/workflows/android-ci.yml
docs/ARCHITECTURE.md
```

## Roadmap

1. **Foundation (current):** UI, local drafts, security disclosure, CI build.
2. Accessibility bridge: opt-in service, clear system setting hand-off, capability status.
3. Action engine: allowlisted actions, previews, bounded execution, cancellation and audit log.
4. Optional AI connection: documented endpoint, user-managed credentials, minimal data transfer and explicit external-service disclosure.
5. Device-side validation and release signing.

## Security

See [SECURITY.md](SECURITY.md) and [the architecture notes](docs/ARCHITECTURE.md). Never commit API keys, access tokens, keystores, or personal data.
