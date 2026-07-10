package com.aion.brain

/** DOC-008 §5 — why a running Dream Mode cycle stopped early. */
enum class DreamModeAbortReason { UNPLUGGED, THERMAL_THROTTLED, TIME_LIMIT, BATTERY_LIMIT }

/**
 * DOC-008 §5 — Dream Mode's own budget: ≤30 min/night, ≤15% battery, abort on unplug/thermal.
 * Pure arithmetic on values the caller reads from real Android APIs (battery %, charging state,
 * thermal status) — this class never touches Android itself, so it's plain-JVM testable.
 */
class DreamModeGuard(
    private val maxDurationMs: Long = DEFAULT_MAX_DURATION_MS,
    private val maxBatteryDropPct: Int = DEFAULT_MAX_BATTERY_DROP_PCT,
) {
    private var startedAtMs: Long? = null
    private var startBatteryPct: Int? = null

    fun start(
        nowMs: Long,
        batteryPct: Int,
    ) {
        startedAtMs = nowMs
        startBatteryPct = batteryPct
    }

    /** Null means "keep going"; a real caller checks this repeatedly between Dream Mode's steps. */
    fun checkContinue(
        nowMs: Long,
        batteryPct: Int,
        isCharging: Boolean,
        isThermalThrottled: Boolean,
    ): DreamModeAbortReason? {
        val startedAt = startedAtMs ?: return null
        val startBattery = startBatteryPct ?: batteryPct
        return when {
            !isCharging -> DreamModeAbortReason.UNPLUGGED
            isThermalThrottled -> DreamModeAbortReason.THERMAL_THROTTLED
            nowMs - startedAt > maxDurationMs -> DreamModeAbortReason.TIME_LIMIT
            startBattery - batteryPct > maxBatteryDropPct -> DreamModeAbortReason.BATTERY_LIMIT
            else -> null
        }
    }

    companion object {
        const val DEFAULT_MAX_DURATION_MS = 30 * 60 * 1000L
        const val DEFAULT_MAX_BATTERY_DROP_PCT = 15
    }
}
