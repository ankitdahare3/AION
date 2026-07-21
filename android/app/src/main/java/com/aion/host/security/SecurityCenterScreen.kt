package com.aion.host.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.setup.SetupPermission
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.StatCard
import kotlinx.coroutines.launch

/**
 * Mockup "Security & Privacy Center" — combines three already-real sources into one screen rather
 * than building a fourth: [AuditLogger]'s live hash-chain verification (same check
 * [AuditLogScreen] exposes), [KillSwitch]'s real halted state, and granted-permission count
 * ([SetupPermission], same source [com.aion.host.HomeScreen] already reads).
 */
@Composable
fun SecurityCenterScreen(
    auditLogger: AuditLogger,
    killSwitch: KillSwitch,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chainValid by remember { mutableStateOf<Boolean?>(null) }
    val halted by killSwitch.halted.collectAsState()
    val grantedPermissions = SetupPermission.entries.count { it.isGranted(context) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Security Center", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)

        StatCard(
            "Kill switch",
            if (halted) "Halted — automation is stopped" else "Armed — automation runs normally",
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            valueColor = if (halted) AionColors.Error else AionColors.SecurityGreen,
        )
        StatCard(
            "Permissions",
            "$grantedPermissions/${SetupPermission.entries.size} granted",
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        StatCard(
            "Audit log chain",
            when (chainValid) {
                true -> "Intact"
                false -> "TAMPERED"
                null -> "Not checked yet"
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            valueColor =
                when (chainValid) {
                    true -> AionColors.SecurityGreen
                    false -> AionColors.Error
                    null -> AionColors.OnBackground
                },
        )
        Button(
            onClick = { scope.launch { chainValid = auditLogger.verifyChain() } },
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Verify audit chain now")
        }
    }
}
