package com.aion.host.brain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aion.brain.AppScanResult
import com.aion.brain.DeviceExplorer
import com.aion.brain.MemoryConsolidator
import com.aion.brain.MemoryStore
import com.aion.host.automation.ActionDispatcher
import com.aion.host.automation.AionAccessibilityService
import com.aion.host.automation.GlobalAction
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

/**
 * Manual "Explore Device" feature — a user-tap-triggered, one-off counterpart to [DreamModeWorker]:
 * same shape (Hilt [EntryPoint] into the graph, WorkManager job, [MemoryConsolidator] afterwards),
 * but launched immediately on request rather than nightly, and with no [com.aion.brain.DreamModeGuard]
 * budget check — that guard exists to protect an UNATTENDED nightly run's battery/thermal budget;
 * here the owner is explicitly choosing to run it right now, so there's nothing to guard against.
 *
 * Read-only by construction: every app gets exactly `launchApp` (a real side effect — opening the
 * app — but not an outward-facing one) followed by one screen read and `globalAction(HOME)`. No
 * taps into the app itself, so nothing the app displays can be sent, deleted, or purchased. Deeper
 * in-app navigation (multi-screen crawling) is deliberately NOT built — a single landing-screen read
 * per app is the minimum useful profile; see BACKLOG.md for the multi-screen upgrade path.
 */
class DeviceExplorationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val service =
            AionAccessibilityService.instance ?: run {
                Log.i(TAG, "skipped: accessibility service not connected")
                return Result.success()
            }
        val entry = entryPoint()
        val targets = explorablePackages(applicationContext.packageManager, applicationContext.packageName)

        val scans =
            targets.map { pkg ->
                entry.actionDispatcher().launchApp(pkg)
                delay(LAUNCH_SETTLE_MS)
                val text = service.currentScreenText()
                entry.actionDispatcher().globalAction(GlobalAction.HOME)
                delay(HOME_SETTLE_MS)
                AppScanResult(pkg, text)
            }

        val store = entry.memoryStore()
        val now = System.currentTimeMillis()
        DeviceExplorer.buildProfileMemories(scans, now).forEach { store.insert(it) }

        // Same consolidation step DreamModeWorker runs nightly — reused immediately here so a
        // repeat "Explore Device" tap dedupes against the previous scan on the spot rather than
        // waiting for the next Dream Mode cycle.
        val result = MemoryConsolidator.consolidate(store.getAllActive(), nowMs = now)
        result.updates.forEach { store.update(it) }
        result.softDeletes.forEach { store.softDelete(it) }
        Log.i(TAG, "explored ${targets.size} apps, ${scans.count { it.screenText != null }} readable — ${result.report.summary}")

        return Result.success()
    }

    private fun entryPoint(): DeviceExplorationWorkerEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, DeviceExplorationWorkerEntryPoint::class.java)

    private companion object {
        const val TAG = "DeviceExplorationWorker"
        const val LAUNCH_SETTLE_MS = 1500L
        const val HOME_SETTLE_MS = 500L
    }
}

/** All installed launchable apps except AION itself — the real target list [DeviceExplorationWorker] scans. */
fun explorablePackages(
    pm: PackageManager,
    ownPackage: String,
): List<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val packages = pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
    return filterExplorable(packages, ownPackage)
}

/** Pure decision half of [explorablePackages] — exclude AION's own package, drop duplicates. Split out so it's unit-testable without mocking PackageManager/ResolveInfo. */
fun filterExplorable(
    allLaunchable: List<String>,
    ownPackage: String,
): List<String> = allLaunchable.filter { it != ownPackage }.distinct()

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeviceExplorationWorkerEntryPoint {
    fun memoryStore(): MemoryStore

    fun actionDispatcher(): ActionDispatcher
}

object DeviceExplorationScheduler {
    private const val UNIQUE_WORK_NAME = "device_exploration_manual"

    /** Enqueues the scan immediately — no charging/idle constraints, unlike [DreamModeScheduler]; this is a foreground, owner-requested action, not an unattended nightly one. */
    fun triggerNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DeviceExplorationWorker>().build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
