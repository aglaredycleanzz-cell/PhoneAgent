# Security Policy

## Scope of this prototype

Phone Agent 0.1 is a local task-draft notebook. It is not yet a remote-control agent.

- The Android manifest declares no runtime permissions and no `INTERNET` permission.
- No Accessibility Service, overlay, notification listener, file provider, or background execution service is declared.
- Draft text is stored in the app's private SharedPreferences.
- Drafts are not transmitted to a server by this build.
- There is no task execution API; saving a draft cannot trigger device actions.

## Reporting a vulnerability

Do not publish credentials, personal data, or an unpatched exploit in a public issue. Use GitHub's private vulnerability reporting feature if it is enabled for this repository; otherwise contact the maintainers privately through the account associated with the repository.

## Requirements for future capabilities

Any future capability that can read other apps, send data off-device, modify files, or perform a consequential action must document:

1. the Android permission or system setting required;
2. the exact data read and where it goes;
3. the approval checkpoint before consequential actions;
4. cancellation and error behavior;
5. tests for denied permissions and failed actions.

API keys, tokens, signing keys, and keystores must never be committed to the repository.
