package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T-110 AC — Dream Mode's constraint/budget guard: ≤30 min, ≤15% battery, abort on unplug/thermal. */
class DreamModeGuardTest {
    private fun guard() = DreamModeGuard(maxDurationMs = 1_000, maxBatteryDropPct = 10)

    @Test
    fun `continues under normal charging, cool, within-budget conditions`() {
        val g = guard()
        g.start(nowMs = 0, batteryPct = 80)

        assertNull(g.checkContinue(nowMs = 500, batteryPct = 82, isCharging = true, isThermalThrottled = false))
    }

    @Test
    fun `aborts once elapsed time exceeds the max duration`() {
        val g = guard()
        g.start(nowMs = 0, batteryPct = 80)

        assertEquals(
            DreamModeAbortReason.TIME_LIMIT,
            g.checkContinue(nowMs = 1_001, batteryPct = 80, isCharging = true, isThermalThrottled = false),
        )
    }

    @Test
    fun `aborts once battery has dropped past the max allowed drop`() {
        val g = guard()
        g.start(nowMs = 0, batteryPct = 80)

        assertEquals(
            DreamModeAbortReason.BATTERY_LIMIT,
            g.checkContinue(nowMs = 100, batteryPct = 69, isCharging = true, isThermalThrottled = false),
        )
    }

    @Test
    fun `aborts immediately when unplugged, regardless of time or battery`() {
        val g = guard()
        g.start(nowMs = 0, batteryPct = 80)

        assertEquals(
            DreamModeAbortReason.UNPLUGGED,
            g.checkContinue(nowMs = 100, batteryPct = 80, isCharging = false, isThermalThrottled = false),
        )
    }

    @Test
    fun `aborts immediately when thermal throttled, regardless of time or battery`() {
        val g = guard()
        g.start(nowMs = 0, batteryPct = 80)

        assertEquals(
            DreamModeAbortReason.THERMAL_THROTTLED,
            g.checkContinue(nowMs = 100, batteryPct = 80, isCharging = true, isThermalThrottled = true),
        )
    }

    @Test
    fun `unplugged takes priority over other simultaneous violations`() {
        val g = guard()
        g.start(nowMs = 0, batteryPct = 80)

        assertEquals(
            DreamModeAbortReason.UNPLUGGED,
            g.checkContinue(nowMs = 5_000, batteryPct = 50, isCharging = false, isThermalThrottled = true),
        )
    }

    @Test
    fun `checking before start never aborts`() {
        val g = guard()

        assertNull(g.checkContinue(nowMs = 999_999, batteryPct = 1, isCharging = false, isThermalThrottled = true))
    }
}
