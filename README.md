# TR-25 Bill Generator

An offline Android app that fills in **Form T.R.-25A / G.A.R.-14A — Travelling Allowance Bill for Tour**
from a simple phone form, works out Part B, and produces an A4 PDF to download, print or share.

**Enter data once → Generate TR-25 bill → Download / print / share.**

---

## What the app does

| Home button | What happens |
|---|---|
| A. Create New TR-25 Bill | Opens the form, pre-filled with your saved defaults |
| B. View Saved Bills | Search by Sub-Bill No., date, office or name; open, edit, duplicate or delete |
| C. Edit Existing Bill | Pick a bill to correct or complete |
| D. Generate PDF | Pick a final bill → PDF preview with all actions |
| E. Print Bill | Pick a final bill → Android print screen (A4) |
| F. Export / Share Bill | Pick a final bill → share the PDF (WhatsApp, e-mail, Drive…) |
| G. Settings | Default name, pay, HQ, rates; Backup and Restore |

### The form (follows TR-25A item by item)

* **1–4** Name, Designation, Pay, Headquarters (+ Sub-Bill No., office name for searching, date of claim)
* **5** Journeys — any number of rows: departure date/time & place, arrival date/time & place,
  mode/class, fare, distance, halt (days/hours), purpose. "Add return journey" copies the last row reversed.
* **6** Mode of journey — Air / Rail / Road questions
* **7** Dates of absence (R.H./C.L., not in camp)
* **8** Free board and/or lodging dates
* **9** Hotel stay table · **10** Higher-class journeys + sanction · **11** Road journeys between rail-connected places
* **12** T.A. advance drawn · Certificate · Date
* **Part B** — calculated automatically (see below), voucher No./date, head of account

### Part B calculation (exactly the form's lines)

```
(a) Railways/Air/Bus/Steamer fare  = total of item 5 fares   (or type a different amount)
(b) Road mileage                   = Km × rate per Km        (Km = total of item 5 distances, or type it)
(c) Daily allowance (i)(ii)(iii)   = days × rate per day, each line
(d) Actual expenses                = as typed
    Gross amount                   = (a) + (b) + (c) + (d)
(e) Less T.A. advance              = item 12
    Net amount                     = Gross − (e)
```

No allowance rates are built in. You type every rate (defaults can be saved in Settings).
Amounts use exact decimal arithmetic and are rounded to the paisa (half-up).

### Checks before the bill is generated

* Required: Name, Designation, Pay, Headquarters, at least one journey with From, To and departure date/time.
* Numbers: only digits and one decimal point; at most 2 decimal places; halt days/hours whole numbers; hours 0–23.
* Arrival cannot be before departure; hotel "To" date cannot be before "From".
* Sub-Bill No. must be unique among **final** bills (drafts may repeat).
* Warnings (not blocking): advance larger than gross, DA days without a rate, distance without a mileage rate,
  advance without a voucher number.

Drafts can be saved at any time, even with blanks or errors.

### The PDF

* A4, two pages for a normal bill (Part A on page 1, Part B on page 2). More journeys flow onto extra pages
  and table headings repeat. Text wraps inside its box — nothing is cut off.
* File name: `TR25_Bill_<Sub-Bill No.>_<yyyy-mm-dd>.pdf`, e.g. `TR25_Bill_001_2026-10-17.pdf`.
* **DOWNLOAD PDF** opens Android's "Save as" screen so you choose the folder.
* English only. Items with no entries print "Nil".

### Data safety

* No internet permission — the app cannot send data anywhere.
* Bills live in the app's private storage; other apps cannot read them.
* Excluded from Android cloud backup and device transfer. Use **Settings → Backup** (a JSON file you keep) instead.
* Deleting a bill asks for confirmation.

---

## Building the APK in Android Studio

You need **Android Studio Koala (2024.1) or newer** on a Windows, Mac or Linux computer, and an internet
connection for the first build (Android Studio downloads Gradle and the libraries once).

### 1. Open the project
1. Unzip `TR25BillGenerator.zip`.
2. Android Studio → **File → Open…** → select the `TR25BillGenerator` folder (the one containing `settings.gradle.kts`) → **OK**.
3. If asked, choose **Trust Project**.

### 2. Sync Gradle
Android Studio starts a Gradle sync automatically (bottom status bar). The first sync takes 5–15 minutes.
If it doesn't start: **File → Sync Project with Gradle Files**.

### 3. Fixing common build errors
| Message | Fix |
|---|---|
| *SDK location not found* | **File → Project Structure → SDK Location** → pick the Android SDK (Studio offers to download it). |
| *Failed to find target with hash string 'android-34'* | **Tools → SDK Manager** → tick **Android 14.0 (API 34)** → Apply. |
| *Gradle JDK / "requires Java 17"* | **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** → choose **jbr-17** or **jbr-21**. |
| Studio offers to **upgrade AGP** | You can accept the Upgrade Assistant, or keep the current version — both work. |
| *Could not resolve …* / timeouts | Check internet / proxy (**Settings → HTTP Proxy**), then sync again. |
| Any Kotlin compile error | Copy the first red error line from the **Build** window and send it to me. |

### 4. Run on your phone
1. On the phone: **Settings → About phone** → tap **Build number** 7 times → **Developer options** → turn on **USB debugging**.
2. Connect by USB, allow the computer when the phone asks.
3. Select the phone in the device list at the top of Android Studio → press the green **Run ▶** button.

### 5. Build a debug APK
**Build → Build App Bundle(s) / APK(s) → Build APK(s)**. When it finishes, click **locate** in the popup.

### 6. Build a signed release APK (for distributing to colleagues)
1. **Build → Generate Signed App Bundle / APK…** → choose **APK** → Next.
2. **Create new…** key store: choose a file location, a password, key alias (e.g. `tr25`), validity 25 years, your name/office.
   **Keep this .jks file and passwords safe** — you need the same key for every future update.
3. Next → choose **release** → **Create**.

### 7. Where the APK is
* Debug: `app/build/outputs/apk/debug/app-debug.apk`
* Release: `app/release/app-release.apk` (or `app/build/outputs/apk/release/`)

### 8. Install on an Android phone
1. Copy the APK to the phone (USB, Google Drive, e-mail).
2. Open it from **Files**. Android will ask to allow installs from that app → **Settings → Allow from this source** → back → **Install**.
3. Requires Android 8.0 or newer.

### Command line (optional)
```
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
./gradlew testDebugUnitTest    # runs the calculation tests
```

---

## Project layout

```
app/src/main/java/com/postaltools/tr25/
  MainActivity.kt, TrApp.kt
  data/     Model.kt (TR-25A fields), BillJson.kt, Database.kt (Room), BillRepository.kt, SettingsStore.kt
  logic/    Money.kt (₹ parsing, Indian 1,23,456.00 format), Calculator.kt (Part B), Validator.kt
  pdf/      PdfWriter.kt (A4 layout engine), Tr25aPdf.kt (the TR-25A form)
  util/     PdfFiles.kt (save, share, print, open)
  ui/       home, list, edit (5 tabs), preview, settings, components, theme
app/src/test/.../CalculatorTest.kt   unit tests for the calculation and checks
```

Tech: Kotlin 2.0.20 · Jetpack Compose (Material 3) · Room 2.6.1 · Android PdfDocument / PdfRenderer /
PrintManager · minSdk 26, targetSdk 34 · AGP 8.5.2 · Gradle 8.9.
