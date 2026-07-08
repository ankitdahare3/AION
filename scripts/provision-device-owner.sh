#!/usr/bin/env bash
# DOC-016 §3 — Run after factory reset (skip Google account). USB debugging on.
set -e
adb install -r aion-host.apk
adb shell dpm set-device-owner com.aion.host/.AdminReceiver
adb shell appops set com.aion.host SYSTEM_ALERT_WINDOW allow
adb shell cmd notification allow_listener com.aion.host/.svc.AionNotificationListener
echo "Next: enable Accessibility for AION in Settings; install Shizuku and start via adb."
