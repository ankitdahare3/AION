package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Manual "Explore Device" feature — AC: a batch of app scans becomes real PROFILE memories, ready to insert via MemoryStore. */
class DeviceExplorerTest {
    @Test
    fun `a readable scan becomes a PROFILE memory naming the app and its screen text`() {
        val scans = listOf(AppScanResult("com.android.settings", "Wi-Fi, Bluetooth, Display"))

        val memories = DeviceExplorer.buildProfileMemories(scans, nowMs = 1000L)

        assertEquals(1, memories.size)
        val m = memories.single()
        assertEquals(MemoryKind.PROFILE, m.kind)
        assertEquals("App com.android.settings: Wi-Fi, Bluetooth, Display", m.text)
        assertEquals(DeviceExplorer.PROVENANCE, m.provenance)
        assertEquals(1000L, m.created)
        assertEquals(1000L, m.accessed)
        assertEquals(1.0, m.decayScore, 0.0001)
    }

    @Test
    fun `a null screenText (launch failed) produces no memory`() {
        val scans = listOf(AppScanResult("com.example.broken", null))

        val memories = DeviceExplorer.buildProfileMemories(scans, nowMs = 1000L)

        assertTrue(memories.isEmpty())
    }

    @Test
    fun `a blank screenText produces no memory`() {
        val scans = listOf(AppScanResult("com.example.blank", "   "))

        val memories = DeviceExplorer.buildProfileMemories(scans, nowMs = 1000L)

        assertTrue(memories.isEmpty())
    }

    @Test
    fun `a mixed batch keeps only the readable scans`() {
        val scans =
            listOf(
                AppScanResult("com.android.settings", "Wi-Fi, Bluetooth"),
                AppScanResult("com.example.broken", null),
                AppScanResult("com.whatsapp", "Chats, Status, Calls"),
            )

        val memories = DeviceExplorer.buildProfileMemories(scans, nowMs = 2000L)

        assertEquals(2, memories.size)
        assertEquals(setOf("com.android.settings", "com.whatsapp"), memories.map { it.text.substringAfter("App ").substringBefore(":") }.toSet())
    }

    @Test
    fun `an empty batch produces no memories`() {
        assertTrue(DeviceExplorer.buildProfileMemories(emptyList(), nowMs = 0L).isEmpty())
    }
}
