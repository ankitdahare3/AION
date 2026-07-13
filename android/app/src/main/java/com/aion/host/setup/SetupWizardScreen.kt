package com.aion.host.setup

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.security.AuditLogger
import kotlinx.coroutines.launch

/**
 * DOC-016 §5, T-004 — walks the owner through every PR-02 permission. [resumeSignal] is bumped
 * by MainActivity.onResume() so statuses re-check after returning from a Settings screen. Every
 * tap is audited (DOC-017 §4: every action goes through the hash-chained log).
 */
@Composable
fun SetupWizardScreen(
    resumeSignal: Int,
    auditLogger: AuditLogger,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statuses by remember {
        mutableStateOf(SetupPermission.entries.associateWith { it.isGranted(context) })
    }

    LaunchedEffect(resumeSignal) {
        statuses = SetupPermission.entries.associateWith { it.isGranted(context) }
    }

    // T-152 — was one near-identical `rememberLauncherForActivityResult` per runtime permission;
    // with a 5th and 6th about to join MICROPHONE/NOTIFICATIONS/CALENDAR, one shared launcher plus
    // a small "which permission is pending" marker is the same behavior with far less repetition.
    var pendingPermission by remember { mutableStateOf<SetupPermission?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val permission = pendingPermission
            if (permission != null) {
                statuses = statuses + (permission to granted)
                scope.launch {
                    auditLogger.record(
                        "user",
                        "setup.permission.result",
                        """{"permission":"${permission.name}","granted":$granted}""",
                    )
                }
            }
            pendingPermission = null
        }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("AION Setup", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Grant these before AION can act (DOC-002 PR-02). Tap a row to open it.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        LazyColumn {
            items(SetupPermission.entries) { permission ->
                PermissionRow(
                    permission = permission,
                    granted = statuses[permission] == true,
                    onClick = {
                        scope.launch {
                            auditLogger.record(
                                "user",
                                "setup.permission.tap",
                                """{"permission":"${permission.name}"}""",
                            )
                        }
                        val runtimePermission = runtimePermissionFor(permission)
                        if (runtimePermission != null) {
                            pendingPermission = permission
                            permissionLauncher.launch(runtimePermission)
                        } else {
                            permission.settingsIntent(context)?.let { context.startActivity(it) }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

/** Runtime (dialog-based) permissions launch via [rememberLauncherForActivityResult]; everything
 * else (null here) falls back to [SetupPermission.settingsIntent]. `internal` (not `private`) so
 * this mapping is unit-testable without a device, same as `CalendarReader.dayRangeMs`/`DeviceStatusReader`'s
 * pure helpers. */
internal fun runtimePermissionFor(permission: SetupPermission): String? =
    when (permission) {
        SetupPermission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        SetupPermission.NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
        SetupPermission.CALENDAR -> Manifest.permission.READ_CALENDAR
        SetupPermission.CALL_LOG -> Manifest.permission.READ_CALL_LOG
        SetupPermission.SMS -> Manifest.permission.READ_SMS
        SetupPermission.LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
        else -> null
    }

@Composable
private fun PermissionRow(
    permission: SetupPermission,
    granted: Boolean,
    onClick: () -> Unit,
) {
    // DEVICE_OWNER has no in-app action (adb-only, see settingsIntent()); everything else is tappable —
    // including ACCESSIBILITY, which opens the generic Settings screen even though it can't show as
    // granted until T-040 ships the actual service.
    val canAct = permission != SetupPermission.DEVICE_OWNER
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .let { if (canAct) it.clickable(onClick = onClick) else it }
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(permission.label, style = MaterialTheme.typography.bodyLarge)
            Text(permission.description, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            if (granted) "Granted" else "Grant",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(72.dp),
        )
    }
}
