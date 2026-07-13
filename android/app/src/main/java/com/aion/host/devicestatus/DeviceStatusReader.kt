package com.aion.host.devicestatus

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

data class DeviceStatus(
    val batteryPercent: Int,
    val charging: Boolean,
    val ramFreeBytes: Long,
    val ramTotalBytes: Long,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
)

/** T-153 (EPIC 17) — real device vitals (mockup screens #27 Performance, #35 Battery), no new
 * permission needed: `BatteryManager`/`ActivityManager`/`StatFs` are all normal, ungated APIs. */
class DeviceStatusReader(private val context: Context) {
    fun read(): DeviceStatus {
        // Registering a null receiver for this sticky broadcast returns the last-known battery
        // state synchronously — the standard way to read battery status without a running
        // receiver (no permission needed, unlike BATTERY_STATS).
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val memoryInfo = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)

        val stat = StatFs(Environment.getDataDirectory().path)

        return DeviceStatus(
            batteryPercent = batteryPercent,
            charging = plugged != 0,
            ramFreeBytes = memoryInfo.availMem,
            ramTotalBytes = memoryInfo.totalMem,
            storageFreeBytes = stat.availableBytes,
            storageTotalBytes = stat.totalBytes,
        )
    }
}
