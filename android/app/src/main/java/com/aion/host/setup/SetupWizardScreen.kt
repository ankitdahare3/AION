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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * DOC-016 §5, T-004 — walks the owner through every PR-02 permission. [resumeSignal] is bumped
 * by MainActivity.onResume() so statuses re-check after returning from a Settings screen.
 */
@Composable
fun SetupWizardScreen(resumeSignal: Int) {
    val context = LocalContext.current
    var statuses by remember {
        mutableStateOf(SetupPermission.entries.associateWith { it.isGranted(context) })
    }

    LaunchedEffect(resumeSignal) {
        statuses = SetupPermission.entries.associateWith { it.isGranted(context) }
    }

    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            statuses = statuses + (SetupPermission.MICROPHONE to granted)
        }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                        if (permission == SetupPermission.MICROPHONE) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
