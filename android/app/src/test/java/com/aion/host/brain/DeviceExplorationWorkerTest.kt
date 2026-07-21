package com.aion.host.brain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Manual "Explore Device" feature — AC: the scan target list excludes AION itself and has no duplicates. Split from [explorablePackages] so this doesn't need a mocked PackageManager/ResolveInfo. */
class DeviceExplorationWorkerTest {
    @Test
    fun `own package is excluded from the scan targets`() {
        val result =
            filterExplorable(
                listOf("com.android.settings", "com.aion.host", "com.whatsapp"),
                ownPackage = "com.aion.host",
            )

        assertEquals(listOf("com.android.settings", "com.whatsapp"), result)
    }

    @Test
    fun `duplicate packages collapse to one entry`() {
        val result =
            filterExplorable(listOf("com.android.settings", "com.android.settings"), ownPackage = "com.aion.host")

        assertEquals(listOf("com.android.settings"), result)
    }

    @Test
    fun `an empty launchable list produces an empty target list`() {
        val result = filterExplorable(emptyList(), ownPackage = "com.aion.host")

        assertEquals(emptyList<String>(), result)
    }
}
