# RetroArch Launcher

A single-app Android launcher: it only launches RetroArch.
Home button, reboot, or closing RetroArch all bring it back.
Tries these packages in order: com.retroarch, com.retroarch.aarch64, com.retroarch.ra32

## Get the APK
Option A - GitHub (no Android Studio needed):
  1. Push this folder to a new GitHub repo.
  2. Actions tab -> "Build APK" runs automatically.
  3. Download the "RetroArchLauncher-apk" artifact from the finished run.

Option B - Android Studio: open the folder, Build > Build APK(s).

## Install
1. Install the APK on the phone (allow "install unknown apps").
2. Press Home, choose "RetroArch Launcher", tap "Always"
   (or Settings > Apps > Default apps > Home app).

## Landscape lock
The screen is always kept in landscape, for every app (RetroArch included).
On first start the launcher opens two settings screens; turn each ON for
RetroArch Launcher and press Back: "Display over other apps" (main lock) and
"Modify system settings" (backup: locks system rotation to landscape).
A missed prompt is shown again after the next reboot. A small "Landscape lock is on"
notification stays while the lock is active.
