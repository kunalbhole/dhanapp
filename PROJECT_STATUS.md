# Dhan — project status

_A consolidated record of everything done on this app so far, organized around the
active React Native environment (`dhan-rn/`). For quick operating notes (what to do,
what not to touch), see `CLAUDE.md`; this file is the fuller history/context behind
those notes._

## Where things stand right now

- **Active stack: `dhan-rn/`** — React Native 0.87 + TypeScript, native Kotlin capture
  engine underneath. This is what to build on going forward.
- **`main` on `github.com/kunalbhole/dhanapp` is now the React Native project.**
  GitHub push access is resolved and has been working reliably for several sessions now.
  Branch layout as of this session:
  - **`main`** — the active RN codebase (this file lives here too, alongside `CLAUDE.md`).
  - **`1st-update`** — archived snapshot of the original native Kotlin/Compose project,
    preserved as a labeled milestone rather than deleted. Not touched going forward.
  - **`2nd-update`** — same commit as `main` as of this session (`main` was fast-forwarded
    to it). Kept around for now as a safety net until `main` and the EAS APK build off it
    are confirmed stable; slated for cleanup once that's done, not before.
- **Real verification obtained**: `npm install`, `npx tsc --noEmit` (zero errors across
  the whole app), `npx eslint` (zero errors). This is genuinely more verification than
  the earlier native version ever got.
- **Not verified**: the actual Android/Gradle compile. The sandbox this was built in
  has no route to `dl.google.com` / `maven.google.com` (confirmed by direct testing,
  not assumed), which is where the Android Gradle Plugin and any AndroidX/Compose
  artifacts live — Maven Central, which the sandbox _can_ reach, doesn't mirror those.
  This is a network boundary, not a code problem, and it applies to any Android
  framework choice, not just the one dropped. First real compile happens via EAS Build
  or the user's own Android Studio.
- **CI**: the old raw-Gradle GitHub Actions workflow (`.github/workflows/android-build.yml`)
  has been removed from `main` — it predated the EAS Build decision and would have
  started firing on every push once `main` held the RN project, conflicting with the
  actual build path. The real build path is EAS Build (`eas.json`, `preview` profile,
  `buildType: apk`) — see "Immediate next step" below. A CI step that auto-triggers an
  `eas build` on push (via an `EXPO_TOKEN` secret) is possible later but is a separate
  decision requiring Expo account setup, not bolted on silently here.

## How we got here

1. **Design handoff.** A Claude Design bundle (`project/`, `chats/`) — an HTML/CSS/JS
   prototype, ~26 screens, navy `#141C41` / gold `#C9A84C` design system — was handed
   off for implementation. The chat history shows two prompt-injection attempts
   (font-weight tampering) that were correctly refused during the design phase;
   resolved noise, not live instructions.
2. **First build: native Kotlin + Jetpack Compose** (`android/`). Full pixel-accurate
   port of all ~26-27 screens, a Room database, and a real SMS/notification capture
   engine (`TransactionParser` — regex-based, tuned for Indian bank/UPI SMS and
   notification text). Extensive manual review caught and fixed several real bugs
   (a wrong package import repeated across 14 files, a missing Compose opt-in that
   would have cascaded into dozens of errors, a Kotlin-2.0/Compose-compiler config
   mismatch). None of it was ever compiled for real — same Google Maven network wall
   as above.
3. **Attempted GitHub Actions build.** Wrote `.github/workflows/android-build.yml`
   to do the real compile in CI. Blocked immediately by the same GitHub repo-access
   issue described above — never got as far as a first CI run.
4. **Pivot to React Native**, at the user's explicit request, after being told plainly
   that switching frameworks would *not* remove the Google-Maven network wall (any
   Android build needs it) and *would* mean hand-writing the same native
   SMS/notification-listener code again, just behind a JS bridge. The user chose to
   proceed anyway.
5. **`dhan-rn/` built.** Real `npm install`/`tsc`/`eslint` verification (a first for
   this project). Scope deliberately narrowed to the core money-tracking loop rather
   than re-porting all 26 screens a second time — see "What's in `dhan-rn/`" below.
6. **GitHub Actions workflow rewritten** for the RN project layout (`npm ci` → JDK 17 →
   `cd android && ./gradlew assembleDebug` → uploads `dhan-app-debug-apk`). Push still
   blocked by the same repo-access issue, so delivered as a zip instead.

## What's in `dhan-rn/`

**Screens (core loop only — see "Cut from this pass" below for what's missing):**
Welcome (consolidates the original Splash/Onboarding/SignUp/Permissions/Login into one
screen), Home, Transactions, Budget (with working inline add/edit caps), Bills, Settings
(real SMS/notification permission toggles + capture activity log), Transaction detail.

**Native capture engine** (`android/app/src/main/java/com/dhan/app/`) — ported
near-verbatim from the Kotlin version, backed by plain SQLite instead of Room to avoid
adding another AndroidX/KSP dependency chain on top of RN's own native build:
- `capture/parser/TransactionParser.kt` — the regex-based bank/UPI SMS parser
- `capture/sms/SmsReceiver.kt` — incoming-SMS `BroadcastReceiver`
- `capture/notif/TxnNotificationListenerService.kt` — reads notifications only from an
  explicit allowlist of known bank/UPI/wallet apps (GPay, PhonePe, Paytm, major banks),
  deliberately not every app's notifications
- `db/DhanDb.kt` — the SQLite schema and query layer
- `bridge/DhanDbModule.kt` / `DhanPermissionsModule.kt` — the JS↔native bridge the RN
  UI calls through

**Design system** (`dhan-rn/src/theme/`, `dhan-rn/src/components/`) — colors/type
tokens matching the source design, shared components (Button, Card, TxnRow, Chip,
Field, StatusPill, CategoryIcon). No icon-font library (avoids native font-asset
linking risk) — emoji glyphs stand in for the original Phosphor icon set. No custom
font bundling either — system font stands in for Poppins.

## Cut from this pass (present in the native version, not in `dhan-rn/`)

Debt/split tracking, savings goals, insights, linked-accounts/help/paywall screens,
the full multi-step onboarding flow. These were scoped out to keep the rewrite focused
on the tracking loop rather than re-doing a 26-screen port a second time. Re-adding any
of them to `dhan-rn/` is a new, separate task, not a resumption of leftover work.

## What's real vs. mock (unchanged since the first build)

**Real:** manual transaction/bill entry, budget caps, SMS capture, notification
capture, the regex transaction parser, the capture activity log.

**Mock — no backend exists for any of this, by design:** Google sign-in, OTP
verification, bank-account linking, Dhan Plus payments, "settle up" between friends
(even in the native version, this only ever recorded a local ledger entry).

## Known risk areas for the first real compile

- A handful of less-common icon names were used with reasonable confidence but never
  compiler-checked (native version only — `dhan-rn` doesn't use an icon library, so
  this risk doesn't apply there).
- The RN native module bridge (`DhanDbModule`) returns JSON strings rather than typed
  `WritableMap`/`WritableArray` trees — simpler and lower-risk to get right by hand,
  but means a malformed JSON string would surface as a runtime parse error rather
  than a compile-time type error. Not currently a known issue, just the traded-off risk.
- Neither project has been through a real Gradle/Android SDK pass yet. Everything
  short of that has been checked as thoroughly as the sandbox allows.

## Session update — onboarding + budget changes, GitHub push, EAS build

- **GitHub push access finally works, and `dhan-rn/` is now pushed.** `dhanapp`'s repo-access
  issue is resolved. `2nd-update` — branched off `main`, with `main`'s old native-Kotlin
  contents replaced by `dhan-rn/`'s — is pushed to `github.com/kunalbhole/dhanapp`. `main`
  itself is untouched and still holds the superseded native project.
- **No backend exists, by design — confirmed again this session.** A NestJS+PostgreSQL
  backend was floated as "our stack" but nothing like that exists anywhere in this repo;
  the app is local-only per an earlier explicit decision. The native Kotlin under
  `dhan-rn/android/` (SMS receiver, notification listener, SQLite bridge) is required
  device-native code for SMS/notification capture, not a backend and not a removable
  "remnant" of one — it stays regardless of any future backend decision.
- **Onboarding now seeds real data instead of staying empty.**
  - `WelcomeScreen` gained a "Monthly income" field. If filled in, it's recorded as a
    real income transaction immediately.
  - Right after requesting SMS permission, if granted, the app now runs a **one-time
    historical scan** of the device's existing SMS inbox (`CaptureIngest.scanHistoricalSms`
    in Kotlin, exposed as `DhanDb.scanHistoricalSms()` on the bridge) through the same
    regex parser the live listener uses, inserting every bank/UPI message it recognizes
    as a transaction. This is in addition to (not instead of) the ongoing real-time
    SmsReceiver for messages that arrive after this point.
  - After the scan, default budget category caps are seeded from whatever spend the scan
    actually found (`seedDefaultBudgets` in `WelcomeScreen.tsx`), so Budget isn't a blank
    screen on first launch either.
  - If SMS permission is denied, onboarding is **not** blocked — it falls through to the
    empty state as before, with a note that permission and scanning can happen later from
    Settings.
  - Settings gained a "Scan now" action (next to SMS capture, once granted) so a user who
    denies at onboarding and grants later can still run the same historical scan on demand.
  - There was never any hardcoded seed/demo data in `dhan-rn` (no mock balance, no sample
    transactions) — a fresh install already started from a genuinely empty state before
    this change; only the *onboarding flow's failure to populate real data* needed fixing.
- **Budget screen gained an editable "Total budget" field**, shown above the existing
  per-category "Add/update a cap" section. It sets an overall monthly cap independently of
  category allocations — editing it never redistributes or changes category caps. Stored
  as a reserved `__total__` row in the existing `budgets` table (no schema/native change
  needed) rather than a derived sum, so it can diverge intentionally from the category
  totals. Saving a total that's lower than the current sum of category caps shows a
  confirmation warning ("allocations exceed this total") rather than silently accepting it.
- Re-verified after all of the above: `npx tsc --noEmit` and `npx eslint` both still exit
  clean. The Kotlin changes (`CaptureIngest.scanHistoricalSms`, the new bridge method)
  are new code the sandbox still can't compile for real — same Google Maven wall as
  always — checked carefully by hand instead.

- **Expo EAS Build is configured, not run.** `eas.json` (a `preview` profile, `buildType:
  apk`, not app-bundle) and a minimal `expo` block in `app.json` (name/slug/version,
  `android.package: com.dhan.app` matching the existing native `applicationId`) are on
  `2nd-update`. `android/` already existed (bare workflow) so **no `expo prebuild` was
  run** — it would have regenerated `android/` from scratch and wiped the hand-written
  Kotlin capture engine. `eas-cli` itself needs no project install; `npx eas-cli@latest`
  resolves and runs fine from this sandbox (confirmed: v23.2.0).
- **Why the build itself wasn't run here**: `eas login` needs an interactive
  browser/terminal flow this remote sandboxed session has no way to drive, and the user
  explicitly said not to attempt authenticating on their behalf. Asked directly, the user
  chose to run the build themselves from their own machine rather than share a token.

## Session update — Google Drive backup/restore

Added alongside the local-first architecture, not instead of it: **on-device SQLite is
always the live source of truth; Google Drive is a backup/restore destination only, never
a live sync target.** Only one device is ever "live" per user in V1, so there is no
conflict-resolution logic anywhere in this feature — by design, not an oversight.

**Where it lives**: `android/app/src/main/java/com/dhan/app/backup/` (7 new files) +
`bridge/DhanBackupModule.kt` + `src/native/DhanBackup.ts` +
`src/screens/BackupSettingsScreen.tsx` (Settings → Data & Privacy → Backup) + a restore
path added to `WelcomeScreen.tsx`.

**Encryption — passphrase, deliberately not device Keystore.** `BackupCrypto.kt` derives
an AES-256-GCM key from a user-set passphrase via PBKDF2 (200k iterations, random
salt+IV stored alongside the ciphertext, since salt/IV aren't secret). This was a genuine
either/or, not a "both" — the request mentioned both a passphrase and the device
Keystore as options. Pure Keystore-derived keys don't migrate to a new device, which
would make "restore on a new device" (an explicit requirement) impossible, so passphrase
had to be primary. The Keystore still gets used, but for a different job:
`PassphraseStore.kt` caches the passphrase **on the device that set it** behind
`androidx.security` `EncryptedSharedPreferences` (Keystore-backed), purely so a scheduled
background backup (no UI available) can run without prompting. A fresh device has no
access to that cache, so restoring there always requires typing the passphrase in by
hand — that's intentional. **If the passphrase is forgotten, the backup is unrecoverable
— same as WhatsApp's model** (nothing is stored server-side to recover it from); the
Settings and onboarding copy both say this.

**Drive integration**: `GoogleAuthManager.kt` wraps `GoogleSignInClient`, requesting only
the `drive.appdata` scope (the hidden per-app folder, never the user's visible Drive).
`DriveBackupClient.kt` is a ~100-line hand-rolled REST client (OkHttp) for the three calls
actually needed (find/upload/download in `appDataFolder`) rather than pulling in the
official `google-api-services-drive` client library, which drags in `google-http-client`
+ Guava on top of an already Google-Maven-only dependency chain. One backup file per
account (`dhan_backup.enc`), overwritten in place every time — no version history, per
spec. New Gradle deps: `play-services-auth`, `androidx.work`, `androidx.security:security-crypto`,
`okhttp`, `kotlinx-coroutines-android` — all Google-Maven-only like every other AndroidX
artifact here, so **unverified by a real compile in this sandbox**, same limitation as
always; checked carefully by hand instead.

**Scheduling**: `BackupWorker`/`BackupScheduler` use `androidx.work.PeriodicWorkRequest`
for Daily/Weekly/Monthly, with a `NetworkType` constraint for the Wi-Fi-only vs.
Wi-Fi-or-mobile-data preference. "Manual only" cancels the scheduled work entirely. "Back
up now" in Settings does **not** go through the scheduler or its network constraint — it
calls the backup directly and immediately, matching "always available regardless of the
frequency setting." A failed scheduled backup is recorded to prefs and surfaced in
Settings as "Last backup failed — tap to retry," never as an intrusive alert, per spec.

**Restore flow — adapted, not literal.** The spec assumes a "login" step exists ("on
fresh install, after login, check Drive..."). `dhan-rn` has no account system at all —
name-only onboarding, no mandatory sign-in. Making Google sign-in a mandatory gate for
every fresh install would have been a bigger architecture change than "add backup," and
would contradict the app's local-first, no-required-account design. Instead, restore is
offered as an **optional secondary path** on the Welcome screen ("Already have a Dhan
backup? Restore instead") — sign in → check for a backup → confirm the dialog showing its
date and size → prompt for the passphrase (Android's `Alert` has no text-input variant,
unlike iOS, so this is a small inline screen, not a native prompt) → decrypt → import.
Declining, or having no backup, falls through to the exact same fresh-onboarding flow as
before (name/income/SMS-permission/historical-scan). If the restore succeeds but the
backup itself had zero transactions in it, onboarding's historical SMS scan still runs
afterward (to avoid leaving the app empty); if it restored any transactions, the scan is
skipped to avoid duplicating data — exactly as specified. Restore-after-onboarding (a user
who set up fresh, then remembers they have a backup) is **out of scope for this pass** —
merge-vs-overwrite semantics for an already-populated device are genuinely ambiguous and
weren't specified; Settings' Backup section only backs up, it doesn't restore.

**Db layer additions** (`DhanDb.kt`): `exportAllJson()`/`importAllJson()` covering
transactions, budgets (all months, not just the current one), bills, goals, friends, and
debt entries — plus the `insertFriend`/`insertDebtEntry`/`getDebtEntriesJson` methods
that didn't exist yet (friends/debt_entries had no write path at all before this).
**Deliberately excluded from the backup payload**: `capture_events` (a re-derivable
activity log, not financial data) and JS-side `AsyncStorage` values like the display name
— a background `WorkManager` job can only reach native SQLite, not the JS bridge, so
including AsyncStorage would have meant a whole separate mirror-to-native sync path for
one string field. Re-entering a display name after a restore is a minor cost; flagging
this rather than silently narrowing scope.

**What still needs the user's own Google account — not done here, can't be**: a Google
Cloud Console project with the Drive API enabled, an OAuth consent screen configured for
the `drive.appdata` scope, and this app's SHA-1 signing fingerprint (the committed
`debug.keystore`'s) registered as an Android OAuth client. `GoogleSignInOptions.DEFAULT_SIGN_IN`
picks that registration up automatically once it exists — no client ID or secret needs to
be pasted into the app. Nothing in this feature will actually sign in or back up until
that one-time setup is done on the user's side; it's an account-console step, not
something a token or credential handoff to this session could substitute for.

**Known incompleteness, flagged rather than silently accepted**: if a Google account ever
signs in without the `drive.appdata` scope already granted (shouldn't happen on a first
sign-in here, since the scope is requested as part of the sign-in flow itself, but could
in principle if Play Services caches an older grant), `GoogleAuthUtil.getToken` throws
`UserRecoverableAuthException`, which isn't specially handled — it would surface as a
generic "Backup failed." Re-requesting consent in that case is a small follow-up, not
done in this pass.

## Flagging section-5 items — gaps vs. what's actually in `dhan-rn/`

Checked each item mentioned as "already decided" against the real code (asked to flag,
not silently fix or overwrite):

- **Budgeting frameworks — not implemented at all.** `BudgetScreen.tsx` is a single flat
  per-category-cap list (plus this session's earlier "total budget" field) with no
  concept of frameworks. There's no 50/30/20 default, no five alternative frameworks, no
  custom framework builder, no accordion-card switching, no "Change framework" action, and
  no Personal-vs-Project budget distinction (there's only one budget, full stop). This is
  a substantial feature gap, not a small one — building it is a separate task.
- **Splits & Dues — not implemented in the UI at all**, and this isn't new: it was
  explicitly cut from `dhan-rn`'s scope in an earlier session (see "Cut from this pass"
  above). The `friends`/`debt_entries` tables exist in `DhanDb.kt` and now have basic
  insert/read methods (added this session only so backup/restore has something to
  serialize), but there's no group model, no net-balance simplification, no split
  methods (equal/custom/percentage), no share-sheet settle-up, no free/Plus group-count
  gate. Nothing here should be read as progress toward that feature.
- **Bills — the due-in-7-days behavior described doesn't match what's actually there,
  and there's a real bug.** `AddBillScreen.tsx:22` sets a bill's status once, at creation
  time, based on a **3-day** threshold (`dueInDays <= 3 ? 'DUE_SOON' : 'UPCOMING'`), not
  the 7 days described. Worse: nothing ever recomputes it afterward — a bill created as
  `UPCOMING` stays `UPCOMING` forever in the stored data, even after it's within 7 days
  of its due date or actually overdue, since `status` is just a stored column with no
  scheduled or on-read recalculation anywhere (`grep` for `DUE_SOON`/`OVERDUE` turns up
  only the one write site and the display code, no recompute logic). Recurrence
  (`repeatMonthly`) is stored but nothing acts on it either — a repeating bill doesn't
  regenerate after being marked paid. Worth fixing before relying on this screen.
- **Dhan Plus — nothing exists.** No subscription model, no trial/bonus-month logic, no
  paywall, no payment integration, and correspondingly nothing gated behind it — SMS/UPI
  detection and everything else currently just works, unconditionally free, for every
  user. This backup feature being "free for everyone" is trivially true right now since
  *nothing* is a paid feature yet, not because it was deliberately carved out of a real
  Plus gate.
- **Design system — Poppins and Phosphor Icons are a known, deliberate divergence, not
  new.** `dhan-rn` uses the system font and emoji glyphs instead (see `CLAUDE.md`,
  original rationale: avoiding native font/icon-asset linking risk). Colors match exactly
  (`#141C41` navy, `#C9A84C` gold, confirmed against `src/theme/colors.ts`). Re-flagging
  this now since section 5 restated it as "already decided," but it hasn't changed since
  the original scope-narrowing decision — revisiting it is a real option, not a blocker,
  if pixel-exact typography/iconography matters enough to take on native asset linking.

None of the above was touched or fixed in this pass — only flagged, as asked.

## Session update — Claude Design redesign brief: sourcing, sequencing, and cluster #1

A large batch of UI/UX/logic changes designed in **Claude Design** (not Figma — a Figma
file key was tried first and turned out to be wrong: Dhan's screens are built in Claude
Design, Figma is only used there for the design-system/component library, so a Figma URL
was never going to have the actual app screens on it) needs to land in `dhan-rn/`. Nine
areas in the original brief: design-system selection-indicator fix, Currency screen
Plus-gating, foreign-transaction SMS parsing + display, a new Currency Converter screen,
Splits & Dues (contacts, settlement, groups), Budget page overhaul (multi-budget
frameworks), a Transactions "Overview" section, Home page search + link restyling, and an
OTP screen fix.

**Agreed process**: implement in dependency order, in numbered clusters, checkpointing
with the user after each one rather than delivering a single giant diff. Visual
references (Claude Design screenshots/share links, one per cluster) are the source of
truth for exact copy/spacing/states — **do not implement a cluster ahead of its visual
reference arriving**, per explicit instruction. Cluster order, as given:
1. Dhan Plus entitlement/subscription state (this session — see below).
2. Budget frameworks + multi-budget data model (biggest; most other Budget work depends
   on it) — **waiting on visual reference before starting.**
3. Groups/Splits data model (Group entity, net-balance simplification, split methods) —
   needed before any Group UI can work.
4. Everything else (search, forex SMS parsing, Currency Converter, Contacts, OTP).

**OTP/phone verification, resolved**: it was leftover from an early prototype pass before
Dhan settled on local-first/no-accounts, not an intentional feature — but it's being kept
because the in-app (Dhan-to-Dhan) settlement nudge in Splits needs *some* way to match
users to each other, and phone number is the simplest key for that. OTP and the in-app
settlement path are linked: if in-app settlement gets cut, OTP goes with it. It stays
part of cluster #3/#4, not built standalone.

**Cluster #1 — Dhan Plus entitlement, built this session** (no visual reference needed;
pure state/logic, not a screen). Confirmed via search first: no subscription,
entitlement, trial, or Razorpay state existed anywhere in `dhan-rn` before this — the
only hits were an unrelated bank-name substring in the Kotlin SMS parser and a static
"free for everyone" label in the backup screen's copy.

- `src/native/Entitlement.ts` — local-only (AsyncStorage, no backend call, matching this
  app's architecture throughout), implementing the product model from `CLAUDE.md`
  exactly: every new user gets a 3-month free trial with no payment required; if they
  subscribe (in or after the trial), they get 3 more bonus months before regular
  ₹199/month billing would start; non-subscribers just revert to free once the trial
  ends. `entitlement.getStatus()` returns `{ isPlusUser, inTrial, trialEndsMillis,
  subscribed, billingStartsMillis }` — `isPlusUser` is the one field every future
  Plus-gated feature (Currency multi-select, Edit categories, unlimited groups, ...)
  should check.
- `entitlement.ensureTrialStarted()` is called from `WelcomeScreen.tsx` at the moment
  onboarding completes — both the normal setup path and the restore-from-backup path — so
  every new user's trial starts exactly once, on first successful onboarding.
- **Razorpay is stubbed, as agreed** — `entitlement.setSubscribedForTesting(bool)` flips
  the subscribed flag locally so Plus-gated UI can be built and tested now. Wired to a
  "Simulate Dhan Plus subscription" switch in Settings (clearly labeled dev-only, not
  real product UI) alongside a status line ("Free trial — N days left" / "Dhan Plus" /
  "Free"). Swap this stub for a real Razorpay webhook-driven flag when payments land —
  nothing else in the entitlement API should need to change shape when that happens.
- **Known limitation, flagged rather than silently accepted**: trial/subscription state
  lives in AsyncStorage, same as the display name — and like the display name, it is
  device-local and **not included in Google Drive backup/restore** (see the backup
  session's documented scope). Restoring onto a new device currently starts a fresh
  3-month trial there rather than carrying over existing entitlement state. Low
  consequence while Razorpay is stubbed; worth a real decision once payments are real.
- `npx tsc --noEmit` and `eslint` both clean after these changes.

**Status**: paused here per the agreed checkpoint process, waiting on the Budget-frameworks
visual reference (cluster #2) before writing any Budget-page code.

## Session update — branch restructuring: `main` is now the RN codebase

Requested and carried out this session, in order:

1. **Found a gap before touching anything**: `2nd-update` on GitHub was one commit behind
   what `PROJECT_STATUS.md` already claimed was done — the Google Drive backup feature and
   the Dhan Plus entitlement state (previous two session updates above) existed only in
   the working source tree, never synced into the git-tracked clone or pushed. Fast-forwarding
   `main` to `2nd-update`'s tip as originally proposed would have promoted a `main` that
   was missing both features. Flagged to the user before pushing anything further; they
   confirmed: sync first, then proceed.
2. Synced the missing files (7 native `backup/` Kotlin files, `DhanBackupModule.kt`,
   `Entitlement.ts`, `DhanBackup.ts`, `BackupSettingsScreen.tsx`, and the modified
   `DhanPackage.kt`/`DhanDb.kt`/`RootNavigator.tsx`/`SettingsScreen.tsx`/
   `WelcomeScreen.tsx`/`format.ts`/`build.gradle`) into the clone, re-verified
   `npx tsc --noEmit` and `eslint` clean there (not just in the original working tree),
   committed, and pushed to `2nd-update`.
3. **Archived `main` as `1st-update`** (`git branch 1st-update main` + push) — the
   original native Kotlin/Compose project is preserved as a labeled branch, not lost.
4. **Fast-forwarded `main` to `2nd-update`'s (now-complete) tip** (`git push origin
   2nd-update:main`) — verified as a true fast-forward first (`git merge-base
   --is-ancestor main 2nd-update`) and confirmed after push, by reading `origin/main`'s
   tree directly, that it now contains `App.tsx`, `android/`, `ios/`, `src/`,
   `package.json`, etc. at the top level. No force-push, no rewritten history.
   (The GitHub UI's Languages bar reflecting TypeScript/Kotlin is a rendering only
   visible on the actual GitHub page — worth a quick look there to confirm visually.)
5. **Removed the stale `.github/workflows/android-build.yml`** from `main` and pushed —
   it predated the EAS Build decision and would have started firing raw `gradlew
   assembleDebug` on every push now that `main` holds the RN project.
6. **This file and `CLAUDE.md` are now committed inside the repo itself** (previously
   they only existed in this Claude Code session's outer working directory, never pushed —
   the reason earlier "Immediate next step" sections said things that were already stale
   by the time anyone read them on GitHub). Keep both updated in place going forward.

`2nd-update` and `1st-update` are both left in place per instruction — not cleanup targets
yet. `2nd-update` gets cleaned up once `main` and its EAS build are confirmed stable;
`1st-update` stays permanently as the archived native-Kotlin reference.

## Session update — first real EAS build caught a genuine build-breaking bug, fixed

The first actual EAS build off `main` (the real Kotlin/Gradle compile this project has
never had access to locally — see "Constraints of the working sandbox" — finally ran for
real, in the user's own environment) failed with two compile errors in
`android/app/src/main/java/com/dhan/app/bridge/DhanBackupModule.kt`, both from the same
root cause, both from code written without ever being compiler-checked:

- `activityEventListener`'s `onActivityResult` override declared its `activity` parameter
  as nullable (`Activity?`). `BaseActivityEventListener`'s actual method takes a
  non-nullable `Activity`, so Kotlin didn't recognize this as a valid override at all
  ("overrides nothing").
- `signIn()` read `currentActivity` as if it were a member of `DhanBackupModule` itself;
  it's actually a member of `reactContext`. Because that reference never resolved, the
  `activity.startActivityForResult(...)` call right after it failed too ("unresolved
  reference") — one root cause, two reported errors.

Fixed exactly as diagnosed from the real compiler output: the override's parameter is now
non-nullable `Activity`, and `signIn()` reads `reactContext.currentActivity` explicitly.
Re-read the file afterward to confirm both changes were applied correctly and checked the
rest of the `android/` tree for the same `currentActivity`-without-`reactContext` pattern
elsewhere (none found — this was the only occurrence). Committed and pushed straight to
`main` (not `2nd-update`, per the standing branch-priority note above) as
`14a6b7d`. This sandbox still has no route to Google's Maven repo, so this fix is
verified by re-reading against the actual reported compiler errors, not by a local
recompile — the next EAS build is the real confirmation.

## Session update — design-system fidelity fixes (fonts, icons, SMS titles)

The app was functionally working but visually diverging from the design system in three
concrete ways. All three are fixed and pushed to `main` (`6019f7d`).

**1. Poppins font, actually linked this time.** The earlier "no custom font" decision
(see the superseded note below) is reversed — Poppins Regular/Medium/SemiBold/Bold are
real `.ttf` files, sourced from `@expo-google-fonts/poppins` (same OFL-licensed Google
Fonts binaries, just repackaged), in `src/assets/fonts/`. `react-native.config.js` points
`assets` at that folder, and **`npx react-native-asset` did the actual native linking** —
this is a pure Node/filesystem tool (no Gradle/Xcode compile involved), so unlike
everything else in this project, it could run for real in this sandbox and be verified
directly rather than written blind:
  - Android: the four `.ttf` files are copied into `android/app/src/main/assets/fonts/` —
    confirmed present. That's the entire Android requirement; nothing else needed.
  - iOS: `Info.plist`'s `UIAppFonts` array and `DhanRN.xcodeproj/project.pbxproj`'s file
    references/build phase were updated by `react-native-asset`'s proper Xcode-project
    parser (not hand-edited text). Structurally consistent on inspection, but — like every
    other native change in this project — **not build-verified**, since this sandbox has
    no Xcode/macOS toolchain at all (a stricter version of the usual "no Google Maven"
    limitation). iOS also isn't the active build target (`eas.json`'s profile is
    Android-only), so this is a bonus, not the priority.
  - `src/components/DhanText.tsx` is a new drop-in replacement for RN's `Text` that reads
    whatever `fontWeight` a style already sets (400/500/600/700) and maps it onto the
    matching Poppins file, dropping `fontWeight` itself (mixing it with a static per-weight
    font file makes Android synthesize an ugly second bold on top of an already-bold font).
    Every screen and component now imports `{ DhanText as Text }` instead of RN's `Text` —
    since the wrapper derives everything from styles that already existed, this needed
    **zero per-style edits** app-wide, just an import swap in 14 files.
  - `src/theme/type.ts`'s `h1` — the role every screen title (Budget/Transactions/Bills/
    More/Welcome) actually uses — is now 20px/Poppins Medium, matching the locked
    "page headings 20px Poppins Medium" spec exactly (it was 24px Bold before). `h2`
    (Home's greeting, the merchant name on transaction detail — a different role) is
    unchanged.
  - **This needs a fresh native build, not just a JS bundle push.** Font files living in
    `android/app/src/main/assets/` and referenced in the iOS Xcode project are packaged
    into the APK/IPA at native-build time — an OTA JS-only update (if this project had one)
    would not pick them up. The next EAS build will include them automatically since
    they're committed to the repo; no extra EAS config needed.

**2. Real Phosphor icons, not emoji.** Same "no icon library" decision reversed, but via
`phosphor-react-native`, which renders each icon as **SVG** (`react-native-svg` peer dep)
rather than a custom icon font — so this genuinely doesn't carry the font-asset-linking
risk the original decision was worried about; autolinking handles `react-native-svg`'s
native module the same way it already handles every other native dependency here, no
manual asset step required. Verified the exact icon names against the installed
package's source before wiring anything (`HouseIcon`, `ForkKnifeIcon`, `BankIcon`, etc. —
the `*Icon`-suffixed names, not the deprecated bare ones). Tab bar icons use `weight="regular"`
normally and `weight="fill"` on the focused tab, per spec. Category icons replace the
emoji circle glyphs; `'other'` specifically gets a Bank icon rather than a generic mark,
since that's where most unclassifiable bank/UPI SMS transactions land — this is also what
fixes the "generic dots/arrow" complaint, since `⋯` and `↓` were exactly the emoji glyphs
for `other`/`income` before.

**3. SMS transaction titles no longer show the raw sender ID.** `TransactionParser.kt`'s
merchant fallback chain was `extractMerchant(...) ?: sourceApp ?: "Transaction"` — when no
payee/merchant could be extracted from the message body, it fell straight through to the
raw SMS sender header (e.g. `"AX-AXISBK-S"`, `"JK-JIOPAY-S"`) with no cleanup at all. Added
`humanizeSender()`: strips the DLT operator-prefix/service-suffix pattern
(`XX-CODE-S` → `CODE`), looks the code up in a small known-bank/UPI-service map (`AXISBK`
→ "Axis Bank", `JIOPAY` → "JioPay", etc.), and falls back to title-casing the stripped
code for anything not in the map — never the raw header. Notification-derived `sourceApp`
values (already human names like "Google Pay") don't match the DLT pattern and pass
through unchanged. **This only affects newly-captured messages** — it's a parser change,
not a data migration, so transactions already sitting in the database with a bad title
from before this fix aren't automatically relabeled. Added `DhanDb.relabelSmsTransactions()`
to close that gap: re-runs the fixed parser against each existing SMS transaction's
already-stored `rawText`, updates `merchant`/`category` in place wherever the result
differs, touches nothing else, and is safe to run more than once. Exposed as a
"Clean up" button next to "Scan now" in Settings → Capture — flagged here since it's a
capability beyond the literal ask, added because fixing the parser alone wouldn't have
actually fixed what the user was looking at on their test device.

All three fixes verified with `npx tsc --noEmit` and `eslint` (clean) both in the working
source and, separately, in the actual git-tracked clone after a fresh `npm install` —
the Kotlin/native side is unverified by a real compile, as always, checked carefully by
hand and cross-referenced against the installed packages' actual source instead.

## Immediate next step — running the EAS build (user's machine)

```
git clone https://github.com/kunalbhole/dhanapp
cd dhanapp
npm install
npx eas-cli@latest login
npx eas-cli@latest build --platform android --profile preview
```
No branch checkout needed anymore — `main` is the default branch and is the RN project.
`eas build` will prompt to link/create an Expo project on first run (since no
`extra.eas.projectId` is set yet) — accept the default. When it finishes, it prints an
APK download link (also visible at expo.dev under the project's Builds tab). Download
that APK to a phone and install it (enable "install from unknown sources" if prompted).
This build is the one that will actually carry the new Poppins/Phosphor assets — a plain
JS reload will not.

## Session update — Budget page rebuilt around frameworks and multiple budgets

The previous Budget page (a single flat "Total budget" field plus generic category chips,
built two sessions ago) didn't match the actual locked spec and has been replaced.
**No visual reference was attached to this request** — per the standing process from the
Claude Design redesign-brief session, implementation normally waits for a screenshot/share
link per cluster. Given how precise this brief was about structure and behavior (accordion
vs. tabs, Edit-flow-only framework switching, inheritance rules) rather than pixel details,
I proceeded from the text spec directly rather than blocking — but exact spacing/copy here
should be treated as provisional until a real Claude Design reference for this screen shows
up, same caveat as the entitlement/backup work before it.

**Data model — new, with a real migration.** `budgets` was a flat (category, monthKey) →
amount table with no concept of more than one budget existing. `DhanDb`'s `DB_VERSION`
goes 1 → 2:
- New `budget_defs` table: `id, name, type (PERSONAL|PROJECT), frameworkKey,
  customFrameworkJson`. A "Personal" row is seeded as id 1 on every fresh install and,
  for existing installs, by the upgrade migration — Personal is always id 1, a convention
  several other places (onboarding's budget-seeding, backup import) rely on.
- `budgets` gains a `budgetDefId` column and its unique constraint becomes
  `(budgetDefId, category, monthKey)` instead of just `(category, monthKey)`, so the same
  category can have independent caps under different budgets.
- The upgrade path (`onUpgrade`, not just `onCreate`) renames the old table, recreates it
  with the new shape, copies every existing row across onto the new Personal budget
  (including the `__total__` sentinel row the total-budget field already used), and drops
  the old table. **This is the first real schema migration this project has needed** —
  worth deliberately testing against a device with real existing budget data before
  trusting it, since this sandbox still can't run a real Android build to exercise it.
- Backup/restore (`exportAllJson`/`importAllJson`) now includes `budgetDefs`, with the
  same "Personal is always id 1, remap everything else" logic used for friends/debts.

**Frameworks (`src/data/frameworks.ts`)** — 50/30/20 is the default. **The "five
alternative frameworks" aren't named anywhere in the design brief** (neither this message
nor the earlier full redesign brief), so this is a flagged assumption, not a confirmed
spec match: 70/20/10, 80/20 (Pay Yourself First), the 60% Solution, Envelope System, and
Zero-Based Budgeting. Each framework is a set of named buckets with a percentage
(summing to 100) and the spending categories that roll into it — a bucket with no
categories (e.g. "Savings") represents money set aside rather than spent, and its amount
is shown as `total × percent` with nothing further to break down. Envelope System and
Zero-Based both use one bucket per spending category as a structural stand-in for
"no fixed split" — this is a simplification (see below).

**BudgetScreen.tsx** — "Your budgets": Personal first, then any Project budgets, as
expandable accordion cards (decided explicitly over tabs or a dropdown). The whole card
is the tap target, no chevron. A 3-dot menu (⋯, via `Alert.alert`'s button list rather
than a new popover component) offers Edit always, Delete only for Project budgets — never
for Personal. Expanding a card shows a read-only framework breakdown: each bucket's
derived amount, and for spending buckets, per-category rows underneath with the same
distinct-per-category bar colors the old page had (not a single flat tone) — nothing here
is editable inline; that's the whole point of moving editing into its own screen.

**EditBudgetScreen.tsx (new)** — the only place "Change framework" exists, per spec.
- Total budget field: changing it **auto-scales every existing category allocation
  proportionally** (new/old ratio applied to each). The brief mentioned this as one of
  two options ("auto-scale... or manual rebalance") — only auto-scale is implemented;
  a manual-rebalance mode is not a separate toggle, flagged as a scope simplification.
- Framework row shows the current framework with a "Change" action; picking a different
  one confirms first ("...will reset your category allocations to the new framework's
  defaults... total stays the same"), then clears category rows and reseeds them from the
  new framework's buckets × the existing total.
- Per-category amounts are editable inline, grouped under their bucket, with a single
  "Save allocations" action.

**CreateBudgetScreen.tsx (new)** — name, framework (all 6 + Custom), and a **required**
total budget amount — required specifically so "pre-filled default categories per
framework, never starts empty" is actually guaranteed rather than aspirational. Calls the
exact same `createBudgetDef`/allocation-seeding path regardless of type, so Project
budgets inherit Personal's framework structure by construction, not by a parallel code
path that could drift from it.

**CustomFrameworkBuilderScreen.tsx (new)** — reachable from both Create and Edit's
framework picker. **Its UX isn't specified anywhere in either brief**, flagged per your
own instruction to flag ambiguity here specifically: this is a functional first pass —
add/rename/remove named buckets, a percent per bucket validated to sum to 100, categories
assigned via toggle chips — not a considered design. Most likely piece to need real
revision once an actual visual reference for it exists.

**Not done in this pass, flagged rather than silently skipped**: month-picker/calendar
modal for switching months (Budget still only ever shows the current month, same as
before); an icon/emoji picker at budget-creation time (mentioned in the original fuller
redesign brief, not repeated in this request, so left out rather than assumed back in).

Verified with `npx tsc --noEmit` and `eslint` — clean — in both the working source and,
separately, the actual git-tracked clone after a fresh `npm install`.

## Session update — data-integrity fix: comma-less amounts were being truncated

**Bug.** `TransactionParser.kt`'s `amountRegex` had two alternatives; the first was
`[0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?` — a `*` (zero-or-more) on the comma-group.
For a comma-less amount like `"5000"`, `[0-9]{1,3}` greedily grabs the first 3 digits
(`"500"`), the comma-group matches zero times (no comma follows), and the alternative
"succeeds" on just `"500"`. Regex alternation doesn't backtrack into the second
alternative once the first has matched, so the full digit run was never reached — **any
comma-less amount of 4+ digits was silently truncated by one digit** (₹5,000 captured as
₹500, ₹12,340 as ₹123, etc.). Comma-formatted amounts (`"5,000"`, `"1,00,000"`) were
never affected, since the comma-group had something to actually match.

**Fix.** Changed that `*` to `+`, so the first alternative only succeeds when at least one
comma group is present — genuinely comma-formatted numbers only. A comma-less number now
fails the first alternative entirely and falls through to the second
(`[0-9]+(?:\.[0-9]{1,2})?`), which matches its full length. Verified against
`"Rs.5000"` → `5000`, `"Rs.5,000"` → `5,000`, `"Rs.500"` → `500`, `"Rs.1,00,000"` →
`1,00,000`.

**This is a data fix, not just a forward fix.** Every device that had already captured a
comma-less 4+ digit transaction (via SMS or notification) has a wrong amount sitting in
`transactions.amount` right now — the parser fix alone doesn't touch existing rows.
Unlike the earlier sender-ID relabel fix (`relabelSmsTransactions()`, a manual "Clean up"
button the user has to know to tap), a silently wrong amount in a finance app isn't
something to leave opt-in. `DhanDb.DB_VERSION` goes 2 → 3, and `onUpgrade`'s new
`oldVersion < 3` branch re-parses every transaction that has its original `rawText`
stored (SMS **and** notification-sourced alike — both go through the same regex) and
overwrites `amount` wherever the corrected parse differs from what's stored; sign
(debit/credit) was never affected, only magnitude, so nothing else about the row
changes. Runs automatically the next time the app opens after this build installs — no
Settings button to remember to press. Idempotent and safe to run more than once (a
correctly-parsed row's re-parse matches what's already stored, so it's skipped).

As with the framework migration before it, this native schema/data migration is
unverified by a real device run — this sandbox still can't do a real Android compile —
checked carefully by hand and cross-checked against the actual regex behavior in Python
(equivalent backtracking semantics to Java/Kotlin's regex engine for this pattern) rather
than assumed correct.
