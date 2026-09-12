# Tinda GO

Simple, fast, **offline-only** Android app for small Philippine businesses to track
inventory, sales, customer utang, expenses, and basic business summaries.

Target users: sari-sari stores, mini-marts, carinderias, food stalls,
water-refilling stations, salons, barbershops, and home businesses.

Core philosophy: **Open → Understand → Tap → Done.**

## Features

- **Stock** – products with optional photo (gallery or camera, stored locally
  and resized), one-tap sell (-1) / restock (+1), custom-quantity sale and
  restock sheets, clear In Stock / Low Stock / Out of Stock states with
  per-product low-stock threshold, product notes, product details with history,
  sales history filters (Today / Yesterday / This week / This month)
- **Sales** – custom quantities, percentage or fixed-amount discounts with
  subtotal / discount / total breakdown, optional sale notes
- **Tracker** – customer **utang** (mark as paid, share list via Sharesheet,
  notes) and **expenses** with categories and notes
- **Dashboard** – today's sales & expenses, outstanding utang, inventory value,
  honest profit estimate (shows "unavailable" when cost data is missing)
- **Calculator** – its own bottom tab for quick math on prices and change
- **Notes** – its own bottom tab for quick reminders and lists (searchable,
  included in backup and CSV export)
- **Backup & Restore** – local JSON backup/restore, no account, no cloud
- **CSV export** – products, sales, utang, expenses via the system file picker
- English + Filipino (Tagalog) UI, dark / light / system theme
- Bottom navigation: Stock, Tracker, Calc, Notes, More; replayable introduction
  (More → Help → View introduction)
- 100% offline – no `INTERNET` permission, no analytics, no ads, no tracking

## Tech stack

- Native Android, **Kotlin**, Jetpack Compose (Material 3)
- Room (source of truth), DataStore (settings only), Coroutines + Flow,
  ViewModel + Repository pattern
- Money stored as integer centavos (`Long`) – no float precision issues
- `minSdk 24`, `targetSdk 34`, Java 17

## Project structure

```
app/src/main/java/com/tindahan/tracker/
├── data/local/         # Room: AppDatabase, DAOs, entities
├── data/repository/    # TindahanRepository, SettingsRepository
├── ui/screens/         # Stock, Tracker, More, Dashboard, ProductDetails, ...
├── ui/components/      # Cards, bottom sheets, dialogs
├── ui/navigation/      # Routes
├── ui/theme/           # Material 3 dark-first theme
├── viewmodel/          # Screen state holders
└── util/               # Money, dates, CSV, JSON backup
```

## Build

Requirements: JDK 17, Android SDK (platform 34, build-tools 34.0.0).

```bash
# Debug APK (signed with debug key, installable)
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Unit tests (25 tests, pure JVM)
./gradlew :app:testDebugUnitTest

# Release APK (R8 + resource shrinking; unsigned unless signing is configured)
./gradlew :app:assembleRelease
```

Release signing: copy `keystore.properties.example` to `keystore.properties`
and fill in your keystore, or set `STORE_FILE`, `STORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD` env vars. Without them the APK stays unsigned.
Back up your release keystore somewhere safe (e.g. a password manager) —
losing it means installed apps can never be updated with the same signature.

## Privacy

All business data (products, sales, utang, expenses, backups) stays on the
device. The app shows AdMob App Open ads, which require INTERNET /
ACCESS_NETWORK_STATE and involve network requests to Google's ad servers —
ad providers may collect device/advertising identifiers per their own policies.
No analytics or tracking SDKs are included.
