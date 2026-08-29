# Dhan — Android app

Native Android implementation (Kotlin + Jetpack Compose) of the "Dhan" personal-finance
app designed in `../project/` (a Claude Design handoff — see `../README.md` and
`../chats/` for the design history). Tracks incoming/outgoing money by reading incoming
SMS and notifications from bank/UPI apps, in addition to manual entry.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room (local SQLite) — **all data stays on-device**, no backend, no sync
- Navigation Compose
- minSdk 26, target/compileSdk 34

## Opening the project

Open the `android/` directory (not the repo root) in Android Studio (Koala or newer).
It should sync via Gradle automatically — the project needs network access to Google's
Maven repo (`dl.google.com` / `maven.google.com`) and Maven Central to resolve
AndroidX/Compose/Room dependencies, which **this development sandbox did not have**, so
none of this has been compiled or run yet. Treat the first build in Android Studio as
the real first compile — expect to fix a handful of small issues (an unresolved import,
a missing `when` branch, an icon slug that fell through `DhanIcon.of()`'s fallback) since
every file here was written by hand/by AI agents against a fixed API contract rather than
against a compiler.

## What's real vs. mock

**Real, wired to the on-device Room database:**
- Manual transaction/bill/goal entry, budget caps, category spend tracking
- SMS capture (`capture/sms/SmsReceiver.kt`) and notification capture
  (`capture/notif/TxnNotificationListenerService.kt`) → regex parser
  (`capture/parser/TransactionParser.kt`) → transactions table
- The "Activity" screen shows the real capture-engine audit log (every message scanned,
  matched or not) — this is a rebuild of the source design's static "Notifications"
  screen using real data instead of the prototype's fake alerts
- Settings' SMS/notification-capture toggles reflect and act on real Android permission
  state

**Mock / out of scope (matches the source prototype, which was also a mock in these
areas — no backend exists for any of this):**
- Google sign-in, OTP verification, bank-account linking (LinkBankScreen) — visual only
- Dhan Plus payments — "Upgrade" just flips a local flag, no real billing
- Friend/debt "Settle up" — records a local ledger entry, never moves real money (same
  as the source design's own disclaimer text on that screen)

## The SMS/notification permission model — read before shipping

- **SMS** (`RECEIVE_SMS` / `READ_SMS`): Google Play's SMS/Call Log permissions policy
  restricts these to apps approved as the user's **default SMS handler**, with narrow
  exceptions. This app is *not* a default SMS handler (it's a finance tracker, not a
  messaging app), so **it will not pass Play Store review with these permissions as-is**.
  Options: (a) sideload only / internal distribution, (b) drop SMS capture and rely on
  notification capture only, (c) pursue a Play policy exception if one genuinely
  applies to this use case (unlikely). This tradeoff was surfaced during design, not
  discovered late — decide deliberately before a Play Store submission.
- **Notification access** (`NotificationListenerService`): a Play-restricted permission
  category too, but with a clearer path — Play permits it for apps whose core function
  is legitimately served by reading notifications (this one qualifies) as long as the
  listing and in-app disclosure are honest about it. `TxnNotificationListenerService`
  only reads notifications from an explicit allowlist of known bank/UPI/wallet package
  names (`TRACKED_PACKAGES`) — it deliberately does not read arbitrary app notifications
  even though a granted listener technically could.
- Both permissions are runtime-revocable by the user at any time; the app should (and
  mostly does, via Settings) degrade gracefully to manual entry when they're off.

## Parser tuning

`TransactionParser` (regex-based, offline, no ML) is a best-effort heuristic tuned
against common Indian bank/UPI message phrasing, not a bank-specific parser. Expect to
tune `MERCHANT_CATEGORY_HINTS`, the debit/credit keyword lists, and the exclusion list
(promo/OTP filtering) against real message samples from your own bank/UPI apps — the
`capture_events` table (visible in the Activity screen) is there specifically so you can
see what got skipped and why.

## Known gaps from this pass

- No unit/instrumentation tests yet.
- No app icon beyond the generated adaptive-icon vector (converted programmatically from
  the source SVG logo — worth a design pass).
- `EditBudgetScreen`/`GoalsScreen` etc. use plain local Compose state, no ViewModel
  layer — fine at this scope, would want revisiting if the screens grow more complex.
- Multi-month budget history navigation in `BudgetScreen` works for past months backed
  by real data, but there's no seed/import path for historical transactions predating
  first install.
