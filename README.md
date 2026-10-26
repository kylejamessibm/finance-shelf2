# Finance Shelf V7 — Design Preview / QA build

This is a **test source project**, not a Play Store release. V7 improves Home UI (navy-to-teal hero, compact activity rows, consistent cards and bottom navigation), plus keeps guest-mode, multi-office records, local SQLite, CSV export, and renewal lists from V6.

## Data safety and Google status
- Google Sign-In uses Firebase Google Auth, but Android signing SHA must match the installed APK's certificate. A passing GitHub build alone does not verify login.
- **Automatic whole-account snapshot writes have intentionally been disabled**: the V6 implementation could overwrite newer data from a second device. The explicit **Back Up to Cloud** button remains a **beta snapshot backup**.
- **Restore Cloud Data** only adds missing local records; it is not safe real-time two-way sync. Payment receipt uploads aren't implemented.
- Debug signing keys can change between GitHub runners; configure stable signing and Play App Signing fingerprints before production.
- Database migration was not changed (`finance_shelf_v5.db`), but still export any important information before uninstalling/upgrading.

## Build
Upload project contents into a **private GitHub repository** and run `.github/workflows/build-apk.yml`. The workflow builds `:app:assembleDebug`; inspect the result and test on Samsung Galaxy A05.

## Release blockers
1. Stable signing/OAuth verification and Google login device QA.
2. Firestore per-document synchronization (creates/updates/deletes, conflict resolution, offline queues, account merge, idempotency).
3. Payment proofs upload with Firebase Storage (which may require billing), secure access rules.
4. Account deletion, privacy policy, data export/deletion, Firestore validation and security rule tests.
5. AAB release signing, target SDK update, consent-compliant AdMob and Play Console requirements.
6. UI accessibility, device tests and transaction calculations (incl. currencies, long names and dates).

The `google-services.json` file contains **client configuration only**, not a service-account private key. Never commit service-account secrets or signing keystores.
