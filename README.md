# PocketLedger AI — Play Store Ready Production Financial Storyteller

PocketLedger AI is a premium, offline-first personal financial assistant that translates raw ledger transactions and manual cash logs into conversational narratives—your personal **Salary Story**. It is designed with strict adherence to Google Play policies: **zero cloud leakage, zero bank credential scanning, and 100% local persistence.**

---

## 🎨 Design Philosophy (Cosmic Slate Aesthetic)
Designed using the Material Design 3 guidelines:
- **Midnight Dark Theme**: Styled with a cohesive navy-charcoal background (`#0C131A`) and high-contrast glassmorphic panels.
- **Neon Accent Indicators**: Highlighting wins in vivid emerald green (`#00FF87`), information in electric cyan (`#60EFFF`), and warning indicators in soft rose-red (`#FF5252`).
- **Touch-Precision & Spacing**: Generous standard margins and 48dp+ tap targets ensure perfect accessibility.

---

## 🏗️ Technical Architecture
PocketLedger AI implements standard Android Clean Architecture with standard MVVM:

```
com.example
│
├── MainActivity.kt        # Entry-point activity with modern Jetpack Navigation Host
│
├── data
│   ├── Entities.kt        # Room Database Schema (Transactions, Salary, Preferences)
│   ├── Database.kt        # Room Abstract Databases & reactive DAOs returning Flow<T>
│   ├── WalletRepository.kt# Unitary data repository handling statement parsing & persistence
│   ├── Engines.kt         # Rule-based calculations (Leaks, Subscriptions, Health Scores)
│   └── GeminiService.kt   # Retrofit-based integration with Google Gemini 3.5-flash
│
└── ui
    ├── MainViewModel.kt   # ViewModel holding states, currency choices, and trigger-actions
    ├── Components.kt      # Material 3 UI widgets (Health Goggles, bar/pie canvas charts)
    └── screens
        ├── HomeScreen.kt  # Storyboard hub: Salary Story, Daily Safe Spend, Timeline Nodes
        ├── TransactionScreens.kt # Timeline lists, searchable filters, and Cash entry pad
        ├── ToolScreens.kt # Statement imports, Subscriptions hunter, Leaks, and reports
        └── SystemScreens.kt # Splash screen, Onboarding flow, Privacy, and settings panel
```

---

## ⚙️ Core Engines (Fully Offline-First)

### 1. Salary Story Engine
Builds an interactive visual timeline mapping out how your monthly income flows from starting credit to top categories of expense, untraced paper money (Cash Wallet), and safe remaining balances.

### 2. Ghost Subscription Hunter
Analyzes billing intervals, merchant similarity patterns, and matching costs to identify hidden subscriptions (e.g. Netflix, Spotify) over multiple cycles, helping you weed out forgot-to-cancel plans.

### 3. Micro Leak Detector
Detects frequent, low-value repeating purchases (e.g., Starbucks coffees, Uber rides) that appear trivial daily but carry significant annual costs.

### 4. Financial Health Score
Synthesizes a master score from 0-100 based on savings rate, subscription burden, cash ratio, and budget consistency, outputting a clear letter grade (A+ to F) with friendly coaching tips.

### 5. Statement Import Engine
A flexible text and CSV parser that matches transaction details, converts dates, assigns smart Material categories, and blocks duplicates before merging into the local SQLite database.

### 6. AI Financial Coach (Gemini API)
Integrates with the **Gemini 3.5-Flash** model to provide empathetic, highly personalized coaching cards from your offline records. No data ever leaves the sandbox—it processes through direct, secure REST pipelines.

---

## 🛡️ Play Store Compliance & Security
- **No SMS Permissions**: Adheres to modern Play Store restrictions by relying on explicit user import and manual cash entry instead of invasive `READ_SMS` privileges.
- **Privacy Dashboard**: A dedicated panel displaying current offline sandbox states, local storage footprints, and a **DANGER ZONE** to instantly purge all transaction caches.

---

## 🚀 Play Store Release Checklist
Before uploading the final bundle to Google Play Console:
1. **Secrets Configuration**: Set your valid `GEMINI_API_KEY` inside the Google AI Studio Secrets panel. The system automatically compiles it using the Secrets Gradle Plugin.
2. **App Launcher Icon**: Ensure you generate high-quality vector resources matching modern Android Adaptive Icon standards (`ic_launcher_background` and `ic_launcher_foreground`).
3. **Application ID**: Confirm your unique package ID inside `app/build.gradle.kts` (`com.aistudio.pocketledger.vhkswx`).
4. **Keystore Signing**: Configure your release signing variables (`STORE_PASSWORD`, `KEY_PASSWORD`) in your environment files to safely compile a production-ready AAB.
