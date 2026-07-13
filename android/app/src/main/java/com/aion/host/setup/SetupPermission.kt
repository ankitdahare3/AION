package com.aion.host.setup

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.aion.host.svc.AionNotificationListener

/** PR-02 (DOC-002 §9) — one entry per required platform permission/status for the setup wizard. */
enum class SetupPermission(
    val label: String,
    val description: String,
) {
    DEVICE_OWNER(
        "Device Owner",
        "Provisioned via adb before first launch (scripts/provision-device-owner.sh) — nothing to do here.",
    ),
    ACCESSIBILITY(
        "Accessibility",
        "Lets AION read screen content to automate apps. The service ships in a later task (T-040); " +
            "this only opens Settings for now.",
    ),
    NOTIFICATION_ACCESS(
        "Notification access",
        "Lets AION see notification content for context.",
    ),
    USAGE_ACCESS(
        "Usage access",
        "Lets AION see which app is in the foreground.",
    ),
    OVERLAY(
        "Display over other apps",
        "Needed for the kill-switch overlay and approval prompts (SR-01, SR-03).",
    ),
    MICROPHONE(
        "Microphone",
        "Needed for the wake word and voice commands.",
    ),
    NOTIFICATIONS(
        "Notifications",
        "Needed so AION's voice-listening status is visible (T-010, Android 13+).",
    ),
    BATTERY_OPTIMIZATION(
        "Ignore battery optimization",
        "Keeps AION's services alive in the background.",
    ),
    ;

    fun isGranted(context: Context): Boolean =
        when (this) {
            DEVICE_OWNER -> {
                val dpm = context.getSystemService(DevicePolicyManager::class.java)
                dpm?.isDeviceOwnerApp(context.packageName) == true
            }
            // No AccessibilityService is declared yet (ships in T-040), so nothing can be enabled.
            ACCESSIBILITY -> false
            NOTIFICATION_ACCESS -> {
                val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
                enabled.contains(ComponentName(context, AionNotificationListener::class.java).flattenToString())
            }
            USAGE_ACCESS -> {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode =
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName,
                    )
                mode == AppOpsManager.MODE_ALLOWED
            }
            OVERLAY -> Settings.canDrawOverlays(context)
            MICROPHONE ->
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            // Below API 33 this permission doesn't gate anything — checkSelfPermission returns
            // GRANTED automatically on those devices, which is the correct backward-compatible answer.
            NOTIFICATIONS ->
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            BATTERY_OPTIMIZATION -> {
                val pm = context.getSystemService(PowerManager::class.java)
                pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
        }

    /** Settings screen to resolve this item; null means it's handled another way (adb, or a runtime-permission launcher). */
    fun settingsIntent(context: Context): Intent? =
        when (this) {
            DEVICE_OWNER -> null
            ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            MICROPHONE -> null
            NOTIFICATIONS -> null
            BATTERY_OPTIMIZATION ->
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}"),
                )
        }
}
