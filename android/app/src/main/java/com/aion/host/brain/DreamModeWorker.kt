package com.aion.host.brain

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aion.brain.DreamModeGuard
import com.aion.brain.MemoryConsolidator
import com.aion.brain.MemoryStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * DOC-008 §5 — Dream Mode's WorkManager job. T-112 (scorecards)/T-113 (routine proposals) are real
 * nightly steps that don't exist yet — this only runs T-111's memory consolidation so far; each
 * future step slots in the same way, preceded by its own `checkContinue()` call.
 *
 * Uses WorkManager's default no-arg-plus-params reflective construction rather than Hilt's
 * `@HiltWorker` (a new Gradle dependency + KSP processor this project doesn't have yet) — pulls
 * [MemoryStore] via a plain [EntryPoint] instead, the standard way to reach the Hilt graph from a
 * class Hilt doesn't construct.
 */
class DreamModeWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val guard = DreamModeGuard()
        guard.start(nowMs = System.currentTimeMillis(), batteryPct = batteryPct())

        consolidateMemory()

        // T-112/T-113's real steps run here, each preceded by their own checkContinue() call.
        val abort =
            guard.checkContinue(
                nowMs = System.currentTimeMillis(),
                batteryPct = batteryPct(),
                isCharging = isCharging(),
                isThermalThrottled = isThermalThrottled(),
            )
        if (abort != null) {
            Log.i(TAG, "dream mode aborted: $abort")
        }
        return Result.success() // aborting early isn't a job failure, just stopping the cycle
    }

    private suspend fun consolidateMemory() {
        val store = entryPoint().memoryStore()
        val result = MemoryConsolidator.consolidate(store.getAllActive(), nowMs = System.currentTimeMillis())
        result.updates.forEach { store.update(it) }
        result.softDeletes.forEach { store.softDelete(it) }
        Log.i(TAG, result.report.summary)
    }

    private fun entryPoint(): DreamModeWorkerEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, DreamModeWorkerEntryPoint::class.java)

    private companion object {
        const val TAG = "DreamModeWorker"
    }

    private fun batteryPct(): Int {
        val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isThermalThrottled(): Boolean {
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DreamModeWorkerEntryPoint {
    fun memoryStore(): MemoryStore
}

/** The real WorkManager [Constraints] Dream Mode runs under — DOC-008 §5: charging + idle. */
fun dreamModeConstraints(): Constraints =
    Constraints
        .Builder()
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

object DreamModeScheduler {
    private const val UNIQUE_WORK_NAME = "dream_mode"

    fun schedule(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<DreamModeWorker>(24, TimeUnit.HOURS)
                .setConstraints(dreamModeConstraints())
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
