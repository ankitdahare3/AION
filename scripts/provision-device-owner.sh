#!/usr/bin/env bash
# DOC-016 §3 — Run after factory reset (skip Google account). USB debugging on.
# T-004: pre-grants what adb can grant silently; the rest (Accessibility, Usage access,
# Microphone, battery optimization) has no silent adb path and is walked through by the
# in-app setup wizard (SetupWizardScreen) on first launch instead.
set -e
adb install -r aion-host.apk
adb shell dpm set-device-owner com.aion.host/.AdminReceiver
adb shell appops set com.aion.host SYSTEM_ALERT_WINDOW allow
adb shell cmd notification allow_listener com.aion.host/.svc.AionNotificationListener
echo "Next: launch AION and complete the remaining permissions in its setup wizard; install Shizuku and start via adb."
