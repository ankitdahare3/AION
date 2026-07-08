package com.aion.host.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** T-022 AC — real on-device test: trigger() must halt within <1s, and the "aion stop" hook works. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class KillSwitchInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var killSwitch: KillSwitch

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun triggerHaltsWithinOneSecond() =
        runBlocking {
            assertFalse(killSwitch.halted.value)
            val start = System.currentTimeMillis()

            killSwitch.trigger("test")
            val halted = withTimeout(1_000) { killSwitch.halted.first { it } }
            val elapsedMs = System.currentTimeMillis() - start

            assertTrue(halted)
            assertTrue("halt took ${elapsedMs}ms, must be <1000ms", elapsedMs < 1_000)
        }

    @Test
    fun killPhraseTriggersHalt() =
        runBlocking {
            val matched = killSwitch.checkPhrase("please AION stop right now")
            assertTrue(matched)
            assertTrue(killSwitch.halted.value)
        }

    @Test
    fun nonMatchingPhraseDoesNotTrigger() =
        runBlocking {
            val matched = killSwitch.checkPhrase("hello world, play some music")
            assertFalse(matched)
            assertFalse(killSwitch.halted.value)
        }
}
