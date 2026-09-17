# 🐾 PetCare Android App

A modern, native Android application designed for pet parents to manage pet profiles, organize daily care routines, use gesture controls (swipe to complete, shake to reset), and delegate tasks to pet sitters via SMS.

---

## 📲 Download the App

You can download and install the latest APK directly using any of the following methods:

### 1. Direct APK Download (GitHub Releases)
Whenever a release or tag is published in this repository, the pre-built `.apk` file is attached directly under **Releases**:
👉 **[Go to GitHub Releases](../../releases/latest)** to download `app-debug.apk`.

### 2. GitHub Actions Automated Builds (Always Latest)
Every commit to `main` automatically builds an updated installable APK:
1. Navigate to the **[Actions tab](../../actions)** in this repository.
2. Click on the latest run of the **"Build & Release Android APK"** workflow.
3. Scroll down to the **Artifacts** section and download **`PetCare-Debug-APK`**.
4. Unzip and install the `.apk` on your Android device!

### 3. Interactive Web Emulator (No Install Required)
You can try and test the app immediately in your browser via the Google AI Studio streaming emulator:
👉 **[Launch Interactive App Preview](https://ais-pre-4vd4mnmlktuizqvkztv5sf-946120296816.asia-east1.run.app)**

---

## 🚀 How to Export / Push to Your Own GitHub

If you are working in **Google AI Studio**:
1. Open the **Settings (gear icon)** or **Export** dropdown in the top-right toolbar.
2. Select **"Push to GitHub"** (or **"Create GitHub Repository"**).
3. Connect your GitHub account and choose your repository name.
4. Once pushed, the GitHub Action (`.github/workflows/build-apk.yml`) will automatically build the APK and make it available under the **Actions** and **Releases** tabs!

---

## 📱 How to Install the APK on Your Android Device

1. Download the `app-debug.apk` file to your Android phone or tablet.
2. Open your device's **Files** app and tap the downloaded `.apk` file.
3. If prompted with *"Install unknown apps"*, toggle **Allow from this source**.
4. Tap **Install**, then **Open** to start using PetCare!

---

## ✨ Features

- **Pet Profiles**: Manage multiple pets with breed, species, age, and weight tracking.
- **Daily Care Routines**: Organize tasks across categories (Feeding, Walking, Medication, Grooming, Vet Check, Playtime).
- **Gesture Controls**:
  - **Swipe to Complete**: Swipe routine items right to mark them completed or left to delete.
  - **Shake to Reset**: Shake your device to reset daily tasks with confirmation.
- **SMS Routine Delegation**: Select any pet with responsive cards or quick picker and instantly generate or send routine instructions directly to friends, family, or pet sitters via SMS.
- **Offline First**: Fast and persistent local Room SQLite database with full state retention.
- **Material 3 Design**: Dynamic theming, edge-to-edge system insets, and smooth transitions.
