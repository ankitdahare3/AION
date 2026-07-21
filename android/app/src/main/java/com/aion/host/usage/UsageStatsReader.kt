package com.aion.host.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

data class AppUsage(
    val packageName: String,
    val label: String,
    val foregroundMs: Long,
)

/** T-154 (EPIC 17) — real today's screen-time breakdown (mockup #36), reusing the
 * `USAGE_ACCESS` special-access permission the setup wizard already gates (`SetupPermission
 * .USAGE_ACCESS`) — no new grant needed. Gracefully returns nothing without it. */
class UsageStatsReader(
    private val context: Context,
) {
    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Today's per-app foreground time so far, most-used first. */
    fun todayUsage(nowMs: Long = System.currentTimeMillis()): List<AppUsage> {
        if (!hasAccess()) return emptyList()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val startOfDay = startOfDayMs(nowMs)
        val pm = context.packageManager
        return manager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, nowMs)
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .map { (pkg, entries) -> pkg to entries.sumOf { it.totalTimeInForeground } }
            .sortedByDescending { it.second }
            .map { (pkg, ms) -> AppUsage(pkg, labelFor(pm, pkg), ms) }
    }

    private fun labelFor(
        pm: PackageManager,
        packageName: String,
    ): String =
        try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
}

/** Pure so it's unit-testable without any Android dependency (mirrors `CalendarReader.dayRangeMs`). */
internal fun startOfDayMs(nowMs: Long): Long {
    val zone = java.time.ZoneId.systemDefault()
    return java.time.Instant
        .ofEpochMilli(nowMs)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}
