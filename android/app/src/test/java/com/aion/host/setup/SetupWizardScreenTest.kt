package com.aion.host.setup

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetupWizardScreenTest {
    @Test
    fun `dialog-based permissions map to their real runtime permission string`() {
        assertEquals(Manifest.permission.RECORD_AUDIO, runtimePermissionFor(SetupPermission.MICROPHONE))
        assertEquals(Manifest.permission.POST_NOTIFICATIONS, runtimePermissionFor(SetupPermission.NOTIFICATIONS))
        assertEquals(Manifest.permission.READ_CALENDAR, runtimePermissionFor(SetupPermission.CALENDAR))
        assertEquals(Manifest.permission.READ_CALL_LOG, runtimePermissionFor(SetupPermission.CALL_LOG))
        assertEquals(Manifest.permission.READ_SMS, runtimePermissionFor(SetupPermission.SMS))
        assertEquals(Manifest.permission.ACCESS_COARSE_LOCATION, runtimePermissionFor(SetupPermission.LOCATION))
    }

    @Test
    fun `settings-navigation-based permissions have no runtime dialog`() {
        assertNull(runtimePermissionFor(SetupPermission.DEVICE_OWNER))
        assertNull(runtimePermissionFor(SetupPermission.ACCESSIBILITY))
        assertNull(runtimePermissionFor(SetupPermission.NOTIFICATION_ACCESS))
        assertNull(runtimePermissionFor(SetupPermission.USAGE_ACCESS))
        assertNull(runtimePermissionFor(SetupPermission.OVERLAY))
        assertNull(runtimePermissionFor(SetupPermission.BATTERY_OPTIMIZATION))
    }
}
