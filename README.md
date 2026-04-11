# WalletFlow

An elegant, secure, and automated Android application that scans banking SMS messages to create a seamless transaction passbook. Built with modern Android technologies including Jetpack Compose and Room.

## ✨ Features

✅ **Automatic SMS Tracking**: Real-time detection and parsing of banking SMS messages.
✅ **Manual & File Import**: Add transactions manually or import them from CSV, TXT, XLS, and XLSX files.
✅ **Smart Categorization**: Automatically detects Income/Expense types and assigns categories based on transaction patterns.
✅ **Monthly Passbook**: Organized view of your financial flow, grouped by month and transaction date.
✅ **Secure & Private**: All data is stored locally using Room database. No internet permission required.
✅ **Enhanced UI**: Modern Material 3 cards, colored pill indicators, and a clean, responsive design.
✅ **Bank Support**: Pre-configured for major Indian banks (SBI, HDFC, ICICI, Axis, PNB, etc.).

## 🏗️ Project Structure (Modern Compose Architecture)

```
WalletFlow/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/hussain/walletflow/
│   │       │   ├── data/              # Database, DAO, and Data Models
│   │       │   ├── ui/                
│   │       │   │   ├── screens/       # Jetpack Compose UI Screens
│   │       │   │   │   ├── HomeScreen.kt
│   │       │   │   │   ├── PassbookScreen.kt
│   │       │   │   │   ├── SettingsScreen.kt
│   │       │   │   │   └── ImportPreviewScreen.kt
│   │       │   │   └── theme/         # Material 3 Styling & Color Tokens
│   │       │   ├── viewmodel/         # State Management & Business Logic
│   │       │   ├── receiver/          # SMS Broadcast Listeners
│   │       │   ├── utils/             # SMS Parsing & File Input Utilities
│   │       │   └── MainActivity.kt    # Single Activity entry point
│   │       ├── res/                   # Drawables, Strings, and Layouts
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
└── settings.gradle.kts
```

## 🚀 How It Works

### 1. SMS Scanning
- Uses a `BroadcastReceiver` (`SmsReceiver`) to listen for incoming banking messages.
- A background `SmsScanner` service can be triggered to scan existing inbox messages.
- Uses regex patterns to extract Amount, Bank Name, Account Number, and Transaction Type.

### 2. Transaction Management
- Transactions are stored locally in a Room database.
- The **Passbook** screen shows recent SMS-detected transactions for review.
- The **Home** screen displays finalized monthly transactions with daily totals.

### 3. File Import
- Supports importing financial data from multiple formats:
  - **CSV/TXT**: Comma-separated or tab-separated text files.
  - **Excel (XLS/XLSX)**: Direct parsing of spreadsheet files using Apache POI.
- Features an **Import Preview** screen to review data before it's added to your records.

## 🛠️ Setup Instructions

### Prerequisites
- Android Studio Ladybug or later.
- Java 17+.
- Android device running Android 7.0 (API 24) or higher.

### Installation
1. Clone the repository and open it in Android Studio.
2. Allow Gradle to sync and download dependencies (including Apache POI for Excel support).
3. Connect your Android device and click **Run**.
4. Grant **SMS Permissions** when prompted on the first launch.

## 🔒 Privacy & Security

WalletFlow is built with privacy as a core principle:
- **Offline First**: The app does **not** have internet permissions. Your data never leaves your device.
- **Minimal Permissions**: We only request SMS permissions to read transaction alerts. No phone state or location access is required.
- **No Ads/Tracking**: No analytics or advertising SDKs are included.

## 📝 License

This project is open-source and intended for personal financial management and educational purposes.

---

**Developed for the Indian Financial Ecosystem** 🇮🇳
Designed to work with standard Indian banking SMS formats.
