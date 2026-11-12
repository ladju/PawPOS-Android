# 🐾 PawPOS - Pet Shop Inventory Control & Cashier POS System (Android)

A modern, offline-first Android application designed specifically for pet shops, veterinary clinics, pet boutiques, and pet grooming salons. Built with **100% Jetpack Compose**, **Material 3**, **Room SQLite Database**, **Coroutines / Flow**, and **Clean MVVM Architecture**.

---

## 🌟 Key Features

### 1. 🛒 Fast Cashier POS (Samsung One UI 8 Solo Counter Mode)
- **Instant Product Search & Barcode Lookup**: Scan or search by name, barcode, or SKU.
- **Pet Category Filtering**: Quick horizontal pill tabs for 🐶 Dogs, 🐱 Cats, 🦜 Birds, 🐠 Aquatics, 🐹 Small Pets, and ✂️ Services.
- **Cart Management**: Real-time `+ / -` quantity steppers, subtotal, optional tax calculation (default 0% UMKM, configurable 5% / 11%), and manual discount support.
- **2-Tender Checkout (Cash & Dynamic QRIS)**:
  - **💵 Cash**: Smart IDR denomination suggestions (`Exact`, rounded `Rp 50k`, `Rp 100k`) + custom calculator numpad with automatic change calculation.
  - **📲 QRIS Instant (BCA / All E-Wallets)**:
    - **Dynamic QRIS Focus Block**: High-contrast white scan surface, exact locked IDR bill total, NMID (`ID1020038192019`), and live **3-minute (`03:00`) countdown timer**.
    - **1-Tap Customer View (`🔲 Customer View`)**: Fullscreen high-contrast customer scan modal so you can turn the phone across the counter without exposing cashier controls.
    - **12-Digit Bank RRN & Audio Verification Chime**: Auto-generates a 12-digit Retrieval Reference Number (`RRN`) recorded onto the receipt and plays a dual-tone POS verification chime upon confirmation.
- **Digital WhatsApp QR & Thermal Receipt**:
  - Displays business header, itemized breakdown, payment tender, QRIS RRN/NMID settlement proof, and a scannable **WhatsApp Digital Receipt QR**.
- **Atomic Stock Deduction**: Automatically decreases tangible inventory stocks upon completed sale.

### 2. 📦 Pet Shop Inventory Control
- **Real-Time Stock Health Monitoring**:
  - 🟢 **In Stock**: Healthy supply levels.
  - 🟡 **Low Stock Alert**: Below minimum alert threshold (default 5 units).
  - 🔴 **Out of Stock**: Depleted items highlighted with warning banners.
- **Pet-Specific Attributes**:
  - Expiry date tracking (essential for pet food bags, wet pouches, and medications).
  - Unit tracking (`bag`, `can`, `pouch`, `pack`, `pcs`, `session`).
  - Cost price vs Selling price with live **Profit Margin %** calculation.
- **Quick Stock Adjustments**:
  - One-tap stock in/out dialog with audit reasons: `Restock / Shipment Arrival`, `Customer Sale`, `Damaged / Defect`, `Expired Disposal`, `Inventory Count Audit`.
- **Add & Edit Products**: Full CRUD modal bottom sheet with input validation.

### 3. 📊 Sales Recap & Shift Closing Audit (Z-Report)
- **Today's Financial KPIs**:
  - Real-time Gross Revenue (IDR) & Gross Profit Margin.
  - Total Transactions completed & Items sold.
- **💳 Payment Tender & Cash Drawer Audit**:
  - **💵 Cash in Drawer**: Opening Drawer Float (`Rp 200.000`) + Cash Sales = **Expected Cash in Till**.
  - **📲 QRIS Instant Settlement**: Gross QRIS Sales minus configurable **Bank Indonesia MDR Fee** (`0.0% UMI Mikro` or `0.3% Regular`) = **Net BCA Bank Settlement**.
  - **📲 1-Tap WhatsApp Shift Z-Report**: Send the formatted Shift Z-Report directly to the store owner's WhatsApp.
- **🏆 Top Selling Products & ⚠️ Low Stock Reorder List**.
- **📜 Transaction History**: Full invoice log with one-tap receipt reprint and QRIS RRN inspection.

### 4. 📷 Camera & Gallery Barcode Scanner
- Live **CameraX + ZXing** hardware barcode scanner + **Scan from Gallery Picture** support.
- Manual barcode entry and quick test shortcuts for preloaded pet products.

---

## 🏗️ Architecture & Tech Stack

```
PetShopPOS/
├── app/
│   ├── src/main/
│   │   ├── java/com/petshop/pos/
│   │   │   ├── PetShopApp.kt                     # Application class & DI setup
│   │   │   ├── MainActivity.kt                   # Single Activity host
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── PetShopDatabase.kt        # Room Database & Sample Preload
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── ProductDao.kt         # Product CRUD & stock queries
│   │   │   │   │   │   ├── TransactionDao.kt     # Sales & top seller analytics
│   │   │   │   │   │   └── StockMovementDao.kt   # Stock audit history
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── ProductEntity.kt      # Products with pet attributes
│   │   │   │   │       ├── TransactionEntity.kt  # Completed sales orders
│   │   │   │   │       ├── TransactionItemEntity.kt # Line items
│   │   │   │   │       ├── StockMovementEntity.kt# Inventory log
│   │   │   │   │       └── PetCategory.kt        # Enums (Dogs, Cats, Birds, Cash/QRIS)
│   │   │   │   └── repository/
│   │   │   │       └── PetShopRepository.kt      # Atomic checkout & data orchestration
│   │   │   └── ui/
│   │   │       ├── theme/                        # Samsung One UI 8 OLED Dark & Soft Light Palette
│   │   │       ├── components/                   # ReceiptView, Badges, Pills, KPI cards
│   │   │       ├── viewmodel/                    # PosViewModel, InventoryViewModel, AnalyticsViewModel
│   │   │       └── screens/                      # PosScreen, InventoryScreen, AnalyticsScreen, ScannerScreen, SettingsScreen
│   │   └── res/                                  # Drawables, themes, strings, XML rules
│   └── build.gradle.kts                          # Module dependencies
├── build.gradle.kts                              # Root build config
└── settings.gradle.kts                           # Module include
```

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose (BOM 2024.05.00) + Material 3 (Samsung One UI 8 styling)
- **Local Persistence**: Room SQLite 2.6.1 with Coroutines & reactive Flow
- **Scanner**: CameraX 1.3.3 + Google ZXing Core 3.5.3
- **Target SDK**: Android 14 (API 34) • **Min SDK**: Android 8.0 (API 26)

---

## 🚀 How to Run in Android Studio

1. **Open the Project**:
   - Clone `https://github.com/ladju/PawPOS.git` in Android Studio (**File ➔ New ➔ Project from Version Control...**), **OR** extract `PetShopPOS.zip` and choose **File ➔ Open...** on the `PetShopPOS` folder.
2. **Verify Gradle JDK 17**:
   - Go to **Settings / Preferences ➔ Build, Execution, Deployment ➔ Build Tools ➔ Gradle** and ensure **Gradle JDK** is set to **jbr-17 (JetBrains Runtime 17)** or **JDK 17**.
3. **Sync Gradle**:
   - Click **Sync Project with Gradle Files** (🐘 icon in the top toolbar) and wait for dependencies to download.
4. **Select Device & Run**:
   - Choose an Android Emulator (**Pixel 7 / Pixel 8 API 34** recommended) or connect your physical Android phone via **USB Debugging / Wireless Debugging**.
   - Click the green **Run `▶`** button (or press `Shift + F10` on Windows/Linux, `Ctrl + R` on macOS).
5. The app will install and launch with a preloaded Indonesian Rupiah (IDR) catalog of pet food, toys, health supplies, and grooming services!

---

## 🖨️ Thermal Printer & Barcode Hardware Integration
- **Bluetooth ESC/POS Thermal Printers**: Use `ThermalReceiptView` or send formatted bytes to standard 58mm/80mm Bluetooth thermal printers.
- **USB / Bluetooth Barcode Scanners**: Hardware scanners operating as HID keyboards automatically trigger search and cart additions.
