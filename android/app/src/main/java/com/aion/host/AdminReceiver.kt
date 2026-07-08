package com.aion.host

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * DOC-016 §3 — Device Owner entry point. Provisioned via
 * `dpm set-device-owner com.aion.host/.AdminReceiver` on a factory-reset dedicated phone
 * (scripts/provision-device-owner.sh). Device Owner status is what later lets AION silently
 * grant its own runtime permissions and stay alive as a persistent app (DOC-016 §3) — it is
 * NEVER used to hide activity from the user (DOC-016 §3, SR-01).
 */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(
        context: Context,
        intent: Intent,
    ) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisabled(
        context: Context,
        intent: Intent,
    ) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device admin disabled")
    }

    companion object {
        private const val TAG = "AionAdminReceiver"
    }
}
