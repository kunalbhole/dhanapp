# Dhan (React Native)

Personal finance / expense tracker for Android. React Native 0.87 + TypeScript UI over a
native Kotlin capture engine (SMS + notification listener) backed by plain SQLite.

This is a **rewrite** of an earlier native Kotlin/Compose version of the same app (see the
project history for why: mainly to get real `npm install`/`tsc`/`eslint` verification in a
sandboxed dev environment that couldn't reach Google's Maven repo for AndroidX/Compose).
Verified for real in that environment:
- `npm install` — 865+ packages resolve cleanly from the public registry
- `npx tsc --noEmit` — zero type errors across the whole app
- `npx eslint` — zero errors (only cosmetic inline-style style warnings)

What's **not** verified locally: the actual Android/Gradle compile (`assembleDebug`) needs
Google's Maven repo for the Android Gradle Plugin, same as any Android project — that step
only runs for real in CI (see `.github/workflows/android-build.yml`) or in your own Android
Studio.

## Scope of this rewrite

Deliberately narrower than the original Kotlin version's 27 screens — this pass focuses on
the core loop done well, not a second exhaustive port:

- **Welcome** (consolidates the original Splash/Onboarding/SignUp/Permissions/Login into one
  screen — name entry + SMS permission request)
- **Home** — balance summary, quick add, recent transactions
- **Transactions** — filterable list, grouped by day
- **Budget** — category caps (add/edit inline), spend-vs-cap bars
- **Bills** — due-soon list, mark-as-paid, add bill
- **Settings** — SMS/notification capture toggles (real, wired to actual permission state),
  capture activity log, sign out
- **Transaction detail** — full record incl. capture provenance (raw SMS/notification text)

Not rebuilt in this pass (present in the original Kotlin version, cut for scope): debt/split
tracking, goals, insights, linked-accounts/help/paywall screens, multi-step onboarding.

## What's real vs. mock

Same as the original: SMS/notification capture and the regex transaction parser are real
and functional. Bank-account linking, payments, and Google sign-in remain out of scope /
mocked — no backend exists for any of that here either.

## Architecture

- `src/` — TypeScript UI: screens, navigation (React Navigation), design tokens, shared
  components. No custom font or icon-font library (would need native asset linking); emoji
  glyphs stand in for the original Phosphor icon set.
- `android/app/src/main/java/com/dhan/app/`
  - `db/DhanDb.kt` — plain `android.database.sqlite` (not Room — avoids an extra
    AndroidX/KSP annotation-processor dependency chain)
  - `capture/` — `SmsReceiver`, `TxnNotificationListenerService`, `TransactionParser`
    (ported near-verbatim from the earlier Kotlin app), `CaptureIngest`
  - `bridge/` — `DhanDbModule` / `DhanPermissionsModule`, the React Native native-module
    bridge JS calls through to reach the above

## Running it

```
npm install
npx react-native run-android   # device/emulator connected, or
cd android && ./gradlew assembleDebug   # just build the APK
```

## Permissions note (same as before)

`RECEIVE_SMS`/`READ_SMS` are Play-Store-restricted to default SMS handlers — fine for
sideloading, not for Play submission as-is. Notification-listener access has a clearer Play
path since the app's function genuinely depends on it. Both are user-revocable at any time.
