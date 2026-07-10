package com.aion.host.brain

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-110 AC — the real WorkManager Constraints Dream Mode schedules under: DOC-008 §5's "charging + idle".
 *
 * Only `requiresCharging` is asserted here. `requiresDeviceIdle` genuinely can't be: WorkManager's
 * own `Constraints.Builder.build()` (decompiled and confirmed, not assumed) gates it behind
 * `Build.VERSION.SDK_INT >= 23`, which is 0 on a plain JVM test with no Robolectric in this project
 * — so the built `Constraints` would report `requiresDeviceIdle() == false` no matter what
 * `dreamModeConstraints()` actually passed to the builder, an artifact of the test environment, not
 * of this code. `setRequiresDeviceIdle(true)` is real and correct (verified by reading the library's
 * own bytecode — the *setter* has no such gate, only `build()` does), and always true on a real
 * device (minSdk 31 ≫ 23) — this specific claim is only observable on-device, same category of gap
 * as every other Android-framework behavior this session couldn't unit-test on plain JVM.
 */
class DreamModeConstraintsTest {
    @Test
    fun `requires charging`() {
        assertTrue(dreamModeConstraints().requiresCharging())
    }
}
