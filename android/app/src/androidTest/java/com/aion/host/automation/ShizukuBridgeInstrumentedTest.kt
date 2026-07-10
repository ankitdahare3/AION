package com.aion.host.automation

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** T-043 AC — real on-device proof of the graceful-degrade path: no Shizuku app installed here. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShizukuBridgeInstrumentedTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var bridge: ShizukuBridge

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun degradesCleanlyWhenShizukuIsNotInstalled() {
        assertFalse(bridge.isAvailable())
        assertFalse(bridge.hasPermission())
    }

    @Test
    fun execReturnsUnavailableInsteadOfThrowing() =
        runBlocking {
            val result = bridge.exec("echo hi")
            assertFalse(result is ShizukuResult.Success)
        }
}
