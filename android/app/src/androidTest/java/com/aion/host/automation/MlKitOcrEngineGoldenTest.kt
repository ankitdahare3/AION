package com.aion.host.automation

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-100 AC — real ML Kit OCR against 10 golden screenshots captured from the aion_test emulator
 * (androidTest/assets/ocr-golden/). Each fixture was visually confirmed to contain real, non-blank
 * content before being locked in; the asserted strings are text actually present on that screen.
 */
@RunWith(AndroidJUnit4::class)
class MlKitOcrEngineGoldenTest {
    private val engine = MlKitOcrEngine()

    private fun recognize(assetName: String): String {
        // The fixtures live in this test APK's own assets/, not the target app's — must read via
        // the instrumentation context, not targetContext (which resolves to app-debug.apk).
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap =
            context.assets.open("ocr-golden/$assetName").use {
                BitmapFactory.decodeStream(it)
            }
        return runBlocking { engine.recognize(bitmap) }.screenSummary
    }

    private fun assertContainsAll(
        summary: String,
        vararg expected: String,
    ) {
        for (e in expected) {
            assertTrue("expected OCR summary to contain \"$e\" but got:\n$summary", summary.contains(e))
        }
    }

    @Test
    fun screen1Home() = assertContainsAll(recognize("screen1_home.png"), "Jul")

    @Test
    fun screen2Settings() = assertContainsAll(recognize("screen2_settings.png"), "Settings", "Network & internet", "Battery")

    @Test
    fun screen3Wifi() = assertContainsAll(recognize("screen3_wifi.png"), "Internet", "Wi-Fi", "AndroidWifi")

    @Test
    fun screen4Display() = assertContainsAll(recognize("screen4_display.png"), "Display", "Brightness", "Dark theme")

    @Test
    fun screen5Battery() = assertContainsAll(recognize("screen5_battery.png"), "Battery Saver", "Turn off when charged")

    @Test
    fun screen6Accessibility() = assertContainsAll(recognize("screen6_accessibility.png"), "Accessibility", "Magnification")

    @Test
    fun screen7About() = assertContainsAll(recognize("screen7_about.png"), "About emulated", "Device name", "Model")

    @Test
    fun screen8Aion() = assertContainsAll(recognize("screen8_aion.png"), "AION", "API Keys", "Audit Log")

    @Test
    fun screen9Contacts() = assertContainsAll(recognize("screen9_contacts.png"), "Contacts", "ADD ACCOUNT")

    @Test
    fun screen10Storage() = assertContainsAll(recognize("screen10_storage.png"), "Storage")
}
