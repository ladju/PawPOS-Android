# PawPOS — Design System (`DESIGN.md`)

```yaml
name: PawPOS (PawPlace Pet Shop POS)
version: 2.0.0
platform: Android (Mobile Portrait & Tablet Landscape)
aesthetic: Samsung One UI 8 — Focus Blocks & Reachability
default_theme: dark
locale: id-ID (Indonesian Rupiah / IDR)
```

---

## 1. Visual Philosophy & Layout Principles

PawPOS is a high-speed, offline-first **Point of Sale & Inventory Control** app tailored for Indonesian pet shops and grooming salons. Its interface follows **Samsung One UI 7 / 8** ergonomics:

1. **Viewing Area vs. Interaction Area**:
   - The top header (`Cashier`, `Inventory`, `Scanner`, `Recap`, `Settings`) displays bold 26sp titles, live low-stock warning pills, a live cart total pill, and a 1-tap Light/Dark theme toggle.
   - Primary actions (floating Cart Bar, Category Pills, Quick `+ / -` steppers, Checkout sheet, and Floating Pill Bottom Navigation) sit in the **lower thumb-reachable zone**.
2. **26dp Squircle Focus Blocks**:
   - Content is grouped into generous, high-contrast rounded cards (`26dp` for primary containers/sheets, `22dp` for product cards, `50%` full pill for search bars, category filters, and bottom navigation).
3. **Zero-Friction Solo Counter Mode**:
   - **2-Tender Checkout**: Only `💵 Cash` and `📲 QRIS Instant` (no clutter from unused card/transfer tabs).
   - **High-Contrast Customer QRIS Flip**: Always renders QRIS codes on a pure `#FFFFFF` surface with `#111827` text—even in OLED Dark Mode—so e-wallet cameras scan instantaneously across the counter.

---

## 2. Color Tokens

### Brand & Semantic Accents
| Token | Hex | Usage |
|---|---|---|
| `OneUiBlue` | `#0381FE` | Primary CTAs, active category/nav pills, cart total highlight, QRIS badge |
| `OneUiBlueDark` | `#026BCE` | Pressed states, primary button gradients |
| `OneUiBlueTint` | `#D6EAFF` | Subtle blue badge/chip background in Light Mode |
| `OneUiCalcGreen` | `#169B7C` | Payment confirmation (`=`), BCA Direct Paid badge, Cash Drawer Audit, Net Settlement |
| `OneUiAmber` | `#FF9F0A` | Low-stock warning badges (`⚠️ Low`), stock alert cards |
| `OneUiCoral` | `#FF453A` | Clear (`C`), out-of-stock / critical alerts, destructive actions |
| `WhatsAppGreen` | `#075E54` | WhatsApp Digital Receipt QR & Shift Z-Report share CTA |

### OLED Dark Mode (Default)
| Token | Hex | Usage |
|---|---|---|
| `OneUiOledBlack` | `#010101` | Root app canvas (saves battery on AMOLED counter phones) |
| `OneUiDarkSurface` | `#1C1C1E` | Primary `26dp` focus block cards, floating bottom nav pill |
| `OneUiDarkSurface2` | `#2C2C2E` | Search bar pill, secondary chips, inset 1px hairlines |
| `OnSurfaceDark` | `#FAFAFA` | Primary text |
| `OnSurfaceMutedDark` | `#8E8E93` | Subtitles, SKU/barcode labels, secondary metadata |

### Soft Light Mode
| Token | Hex | Usage |
|---|---|---|
| `OneUiLightCanvas` | `#F2F2F7` | Root app canvas |
| `OneUiLightSurface` | `#FFFFFF` | Primary `26dp` focus block cards, floating bottom nav pill |
| `OneUiLightSurface2` | `#E5E5EA` | Search bar pill, secondary chips, inset 1px hairlines |
| `OnSurfaceLight` | `#010101` | Primary text |
| `OnSurfaceMutedLight` | `#64748B` | Subtitles, SKU/barcode labels, secondary metadata |

---

## 3. Typography & Number Formatting

- **Primary UI Font**: System Sans-Serif (`Roboto` / `One UI Sans` / `-apple-system`)
- **Monospace Font**: System Monospace (`Roboto Mono`) for thermal receipts, 12-digit Bank RRN, NMID, and countdown timers (`⏳ 03:00`).
- **Hierarchy**:
  - **Screen Title**: `26sp`, `ExtraBold (800)`, tight tracking.
  - **Card / Section Header**: `16sp–18sp`, `Bold (700)` or `ExtraBold (800)`.
  - **Product Name**: `14sp`, `Bold (700)`, max 2 lines with ellipsis.
  - **IDR Price Readout**: `15sp–16sp`, `ExtraBold (800)` in `OneUiBlue` (`#0381FE`), formatted with dot thousand separators (`Rp 145.000`).
  - **Checkout Grand Total**: `28sp–32sp`, `ExtraBold (800)` tabular numerals.

---

## 4. Shape, Spacing & Elevation

- **Corner Radii**:
  - Modal Bottom Sheets & Checkout Cards: `26dp`
  - Product Grid Cards & Audit Blocks: `22dp`
  - Inner Subcards (Cash Audit / QRIS Settlement): `16dp`
  - Buttons & Inputs: `14dp`
  - Pills (Search, Category Chips, Floating Bottom Nav, Badges): `50%` (`CircleShape`)
- **Spacing Grid**: `4dp` base unit (`8dp` tight gap, `12dp` card grid gap, `16dp` screen padding, `20dp` header horizontal padding).
- **Floating Bottom Navigation Bar**:
  - 5 tabs: **Cashier**, **Inventory**, **Scanner**, **Recap**, **Settings**.
  - Height `64dp`, `26dp` pill container with `12dp` shadow elevation, active indicator pill in `OneUiBlue` (`#0381FE`).

---

## 5. Core Screen Specifications

### A. Cashier (`PosScreen`)
- **Top Search Pill**: Full-pill search input (`Search name, SKU, barcode...`) + inline Camera Barcode Scanner icon button.
- **Category Filter Row**: Horizontally scrollable pill chips (`All`, `🐶 Dogs`, `🐱 Cats`, `🦜 Birds`, `🐠 Aquatics`, `🐹 Small Pets`, `✂️ Services`) — no wrapping or clipped text on narrow 360dp phones.
- **Product Grid (2-column on Phone, 3–4 column on Tablet)**:
  - Category emoji badge + stock status pill (`In Stock` / `Low` / `Out`).
  - Product title, unit (`bag`, `can`, `session`), IDR price, and **direct inline `+ / -` quantity stepper** when in cart (or `+ Add` pill when 0).
- **Sticky Bottom Cart Pill**: Shows item count badge, live IDR total, and `Pay ➔` CTA above the floating nav bar.

### B. Checkout & Dynamic QRIS (`CheckoutDialog` + `CustomerFullscreenQrisDialog`)
- **2-Way Tender Switcher**: `💵 Cash` | `📲 QRIS Instant`.
- **Cash Mode**: Smart IDR quick-tender pills (`Exact`, rounded `Rp 50k`, `Rp 100k`) + Samsung Calculator-inspired numpad (`C` in `#FF453A`, `=` / `Confirm` in `#169B7C`) + live Change Returned readout.
- **QRIS Instant Mode**:
  - Pure `#FFFFFF` high-contrast QRIS card with `#111827` text regardless of Dark/Light mode.
  - Displays Merchant Name (`PawPlace Pet Shop`), **NMID** (`ID1020038192019`), locked **IDR Grand Total**, and live **3-minute (`03:00`) countdown timer**.
  - **`🔲 Customer View` Button**: Launches fullscreen high-contrast QRIS modal for across-the-counter scanning.
  - **`✓ Payment Received` Button**: Plays dual-tone POS chime and generates a **12-digit Bank RRN** stored on the receipt.

### C. Receipt (`ThermalReceiptView`)
- Classic monospace thermal layout on `#FFFFFF` paper card.
- For QRIS transactions: green-tinted settlement verification box (`✓ BCA Direct Paid`, `NMID`, `12-Digit Bank RRN`, `Issuer: BCA QRIS / GPN`).
- Bottom **Scan for WhatsApp Receipt** QR block (`wa.me / INV-...`).

### D. Sales Recap & Shift Z-Report (`AnalyticsScreen`)
- **KPI Row**: Today's Revenue (IDR) & Gross Profit.
- **Payment Tender & Drawer Audit Card**:
  - `SHIFT #SH-102 ACTIVE` badge.
  - **💵 Cash in Drawer**: Opening Float (`Rp 200.000`) + Cash Sales = **Expected Cash in Till**.
  - **📲 QRIS Instant**: Gross QRIS - BI MDR Fee (`0.0% UMI` or `0.3% Regular`) = **Net BCA Bank Settlement**.
  - **📲 Send Shift Z-Report to Owner WhatsApp** button (`#075E54`).

### E. Settings (`SettingsScreen`)
- Active Currency (`IDR ACTIVE`), **Light / OLED Dark Mode** switcher, Store Profile, Optional Tax (`0%`, `5%`, `11% PPN`), and **Bank Indonesia QRIS MDR Rate** (`0.0% UMI Mikro` vs `0.3% Small Business`).
