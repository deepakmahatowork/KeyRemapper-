# KeyRemap — Nothing Essential Key Remapper (Material You Edition)

[![Android CI](../../actions/workflows/android.yml/badge.svg)](../../actions/workflows/android.yml)
[![Download Latest APK](https://img.shields.io/badge/Download-KeyRemap.apk-006874?style=for-the-badge&logo=android&logoColor=white)](../../releases/latest/download/KeyRemap.apk)

A lightweight, modern Android button remapper built with **Kotlin**, **Jetpack Compose**, and **Material Design 3 (Material You)** for the **Nothing Phone / CMF Essential Key** (Keycode `0`) and any custom hardware button on Android 7.0 – Android 16 (SDK 24–36).

---

## Download APK

- **[Direct Download — `KeyRemap.apk` (Latest Release)](../../releases/latest/download/KeyRemap.apk)**
- **[All Releases & Changelogs](../../releases)**
- **[GitHub Actions Build Artifacts (`KeyRemap-debug-apk`)](../../actions/workflows/android.yml)**

---

## Key Features

- **1-Click Gemini Voice Overlay (Power-Button Assist Parity):**
  - Invokes Android's native `VoiceInteractionSession` (`Activity.showAssist` with `INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS = 6` + Shizuku Power-assist support) so a single press pops up the **floating Gemini voice overlay ready for microphone input**—just like holding the Power button, without opening the legacy Google Search app.
- **Instant 0 ms Single-Press Response:**
  - Automatically bypasses the multi-tap waiting window when Double (`2×`) and Triple (`3×`) Press rules are unassigned or disabled, delivering instant 1-click response.
- **Multi-Gesture Hardware Engine:**
  - Remap **Single Press (`1×`)**, **Double Press (`2×`)**, **Triple Press (`3×`)**, and **Long Press (`Hold`)** with customizable multi-press delay (`150–600 ms`) and hold threshold (`300–1200 ms`).
- **Built-in Actions:**
  - **Gemini / Digital Assistant** (floating voice overlay)
  - **Toggle Rear Flashlight** (instant `CameraManager` torch toggle)
  - **Take Screenshot** (`AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT`)
  - **Cycle Ringer Mode** (`Normal` → `Vibrate` → `Silent`)
  - **Media Controls** (`Play / Pause`, `Next Track`)
  - **Launch Camera** or **Launch Any Installed App**
  - **Lock Screen** & **Expand Notification Shade**
  - **Simulate Any Android Keycode** (via Shizuku)
- **Per-App Profile Overrides & Contextual Constraints:**
  - Create per-application button rules and restrict gestures by device state (`Always`, `Screen On`, `Screen Off`, `Media Playing`, `Wi-Fi Connected`, `Charging`).
- **Rootless Nothing OS Essential Key Override (Shizuku or ADB):**
  - Disable or restore `com.nothing.ntessentialspace` and `com.nothing.ntessentialrecorder` either directly in-app via **Shizuku** or from your computer using **ADB** (full guide below).

---

## Step-by-Step Guide: Disabling the Default Nothing / CMF Essential Key via ADB

Here is the complete, step-by-step process from setting up your phone to running the commands on your computer.

### Part 1: Prepare Your Nothing / CMF Phone

1. **Unlock Developer Options:**
   - Open **Settings** on your phone.
   - Scroll down and tap **About phone** (or **About device**).
   - Tap on **Software info** (if present) and find **Build number**.
   - Tap **Build number** 7 times rapidly until you see a message saying *"You are now a developer!"* (you may need to enter your PIN/password).
2. **Turn on USB Debugging:**
   - Go back to the main **Settings** menu and tap **System**.
   - Tap **Developer options**.
   - Scroll down to the **Debugging** section and toggle on **USB debugging**.
   - Tap **OK** on the confirmation pop-up.

---

### Part 2: Prepare Your PC (Windows or Mac)

1. **Download ADB Tools:**
   - Download the official [Android SDK Platform Tools ZIP file](https://developer.android.com/tools/releases/platform-tools) from Google's developer site (search for *"Android SDK Platform Tools"*).
   - Extract the downloaded ZIP file to an easy-to-find folder, such as `C:\platform-tools` on Windows or your `Desktop` on Mac.
2. **Open the Terminal inside the Folder:**
   - **On Windows:** Open the extracted `platform-tools` folder. Click on the address bar at the top of the file explorer window, type `cmd`, and press **Enter**. A black Command Prompt window will open.
   - **On Mac:** Open the **Terminal** app, type `cd ` (with a space after it), drag and drop the `platform-tools` folder into the Terminal window, and press **Enter**.

---

### Part 3: Connect and Authorize

1. **Plug in Your Phone:**
   - Connect your phone to your PC using a good USB data cable.
   - Pull down the notification shade on your phone, tap the **USB charging notification**, and set it to **File Transfer / Android Auto**.
2. **Check Connection & Allow Debugging:**
   - In your PC's Command Prompt/Terminal window, type the following command and press **Enter**:
     ```bash
     adb devices
     ```
     *(Note: If you are on a Mac, type `./adb devices` instead).*
   - **Look at your phone screen:** A pop-up will appear asking *"Allow USB debugging?"*. Check the box for **Always allow from this computer** and tap **Allow**.
   - Type `adb devices` again on your PC. You should now see your phone's serial number followed by the word `device`.

---

### Part 4: Run the Disable Codes

Copy and paste these commands one by one into your terminal and press **Enter** after each:

1. **Disable Essential Space:**
   ```bash
   adb shell pm disable-user --user 0 com.nothing.ntessentialspace
   ```
   *(Mac users: `./adb shell pm disable-user --user 0 com.nothing.ntessentialspace`)*

2. **Disable Essential Recorder:**
   ```bash
   adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder
   ```
   *(Mac users: `./adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder`)*

3. **Verify:**
   - After each command, the terminal should reply:
     `Package com.nothing.ntessential... new state: disabled-user`
   - You can now unplug your phone. Pressing the Essential Key will no longer trigger Nothing's default app, allowing **KeyRemap** to handle it cleanly.

#### (Optional) How to Undo Later
If you ever want the default Nothing button behavior to work again, connect to your PC and run these two commands:

```bash
adb shell pm enable com.nothing.ntessentialspace
adb shell pm enable com.nothing.ntessentialrecorder
```

---

## App Setup After Installing `KeyRemap.apk`

1. **Install [`KeyRemap.apk`](../../releases/latest/download/KeyRemap.apk)** on your phone.
2. **Allow Restricted Settings (Android 13+):**
   - Open **Settings → Apps → KeyRemap → ⋮ (top-right menu) → Allow restricted settings**.
3. **Enable Accessibility Engine:**
   - Open **KeyRemap → Setup → Enable Service** and turn on **KeyRemap Engine** in Accessibility Settings.
4. **Set Gemini as Default Assistant:**
   - In **KeyRemap → Setup → Default Assistant**, verify **Gemini** is selected as your device's Default Digital Assistant.

---

## Building from Source

```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
gradle :app:assembleDebug
```
