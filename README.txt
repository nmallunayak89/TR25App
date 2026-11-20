TR-25 BILL GENERATOR – Android project
======================================
The whole app is app/src/main/assets/index.html shown inside a WebView.
"Print / Save PDF" opens Android's print screen -> choose "Save as PDF".

OPTION 1 – Android Studio (any PC)
  File > Open > this folder. Wait for sync.
  Build > Build Bundle(s)/APK(s) > Build APK(s).
  APK: app/build/outputs/apk/debug/app-debug.apk -> copy to phone and install
  (allow "Install unknown apps" when asked).

OPTION 2 – Free online build with GitHub (no PC software needed)
  1. Create a free GitHub account, make a new repository, upload all files in this folder.
  2. Open the "Actions" tab -> "Build APK" runs automatically (about 5 minutes).
  3. Open the finished run -> download "TR25-Bill-APK" -> unzip -> app-debug.apk.
