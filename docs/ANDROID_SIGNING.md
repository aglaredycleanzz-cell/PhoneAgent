# Android signing and safe update policy

## Decision

- The debug build variant uses application ID io.phoneagent.mobile.ci and is for CI validation only. Its runner-generated debug certificate is not stable and its APK must not be published as an installable update.
- The release build variant uses application ID io.phoneagent.mobile and must be signed by one permanent keystore.
- A release APK is published as a workflow artifact only after the workflow verifies that its signer certificate SHA-256 equals the certificate exported from the configured keystore.
- Never commit the keystore, the Base64 representation, store password, key password, or access tokens to Git.

## Why the previous builds conflict

Android accepts an update only when the installed app and incoming APK have the same application ID and a compatible signing certificate. A changed application ID installs a different app; a changed signing certificate for the same ID is rejected as an update. See the [Android signing guide](https://developer.android.com/studio/publish/app-signing) and [Android app update rules](https://developer.android.com/guide/app-bundle/faq).

Earlier CI debug APKs used different signing certificates (recorded fingerprints):
- Earlier .debug APK: 157f7a932e60c4b806c1a78feee978b23fb9885ccbcf264f49c024edb69dedfb
- Later .ci APK: 831295e04a6bbc6e7987d021334b476c13ab47acb662b3aa65a77c96114c8143

The old CI workflow did not preserve its generated debug keystore. A certificate fingerprint identifies the public certificate; it cannot recreate the private signing key. Unless the exact old keystore was saved elsewhere, those old package variants cannot be updated in place with a newly generated key. Do not uninstall old variants to troubleshoot this; leave them installed so their local data remains intact.

The stable io.phoneagent.mobile release identity is a new, separate installation beside those legacy variants. Android does not automatically transfer another app's private SharedPreferences into it. A future explicit export/import path would be required to migrate old local data.

## 1. Generate the permanent release keystore on Android

Use Termux installed from a trusted source. The keystore is generated on your device; do not send it to a chat, commit it, or attach it to an issue.

~~~sh
pkg update
pkg install openjdk-17
mkdir -p "$HOME/phoneagent-signing"

keytool -genkeypair -v \
  -keystore "$HOME/phoneagent-signing/phoneagent-release.jks" \
  -storetype JKS \
  -alias phoneagent-release \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=PhoneAgent Release, OU=Android, O=PhoneAgent, L=Muscat, ST=Muscat, C=OM"
~~~

keytool will prompt you to create and confirm passwords. Keep them in a trusted password manager. Losing this keystore or either required password can prevent future in-place updates of the release app. Make a secure offline backup of the .jks file.

Confirm the alias exists without exposing the private key:

~~~sh
keytool -list -v \
  -keystore "$HOME/phoneagent-signing/phoneagent-release.jks" \
  -storetype JKS -alias phoneagent-release
~~~

## 2. Encode the keystore for GitHub Actions

Base64 is only an encoding, not encryption. Keep the output private and enter it only into GitHub's Secrets UI.

~~~sh
base64 "$HOME/phoneagent-signing/phoneagent-release.jks" | tr -d '\n' \
  > "$HOME/phoneagent-signing/phoneagent-release.base64"
cat "$HOME/phoneagent-signing/phoneagent-release.base64"
~~~

The last command prints a long line. Copy it directly into GitHub as the value of the secret named below. Do not paste that line into this chat or into repository files.

## 3. Add repository secrets

Open the repository on GitHub, then go to **Settings → Secrets and variables → Actions → Secrets → New repository secret**. Create these four secrets:

| Secret name | Value |
|---|---|
| PHONEAGENT_KEYSTORE_BASE64 | The complete Base64 line generated above |
| PHONEAGENT_KEYSTORE_PASSWORD | Keystore password from keytool |
| PHONEAGENT_KEY_ALIAS | phoneagent-release |
| PHONEAGENT_KEY_PASSWORD | Password for that key alias |

GitHub documents this process in [Using secrets in GitHub Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets).

The workflow intentionally does not access signing secrets from pull-request builds. It only attempts a signed release on a push to main or a manual workflow run selected on main.

## 4. Verify the first stable release

After this pull request is merged and all four secrets exist:

1. Open **Actions → Android CI → Run workflow** and select main, or push a later approved change to main.
2. Confirm the job passes **Build stable signed release APK** and **Verify APK signature matches the configured keystore**.
3. Download only the **PhoneAgent-stable-release-apk** artifact.
4. For any subsequent release, compare the logged SHA-256 signer fingerprint with the first stable release. It must stay identical.

If secrets are not configured, unit tests and CI debug compilation may still pass, but the workflow deliberately publishes no installable release APK.

## Important limits

This fixes signing consistency for future release builds. It does not recover the lost private keys of prior CI debug builds, does not make old .debug / .ci installations update-compatible with the new release ID, and does not move their private app data. Do not uninstall those older apps until you decide whether their data must be retained.
