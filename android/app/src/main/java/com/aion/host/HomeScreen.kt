package com.aion.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aion.host.security.AuditLogEntry
import com.aion.host.security.AuditLogger
import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import com.aion.host.setup.SetupPermission
import com.aion.host.ui.theme.AionColors

/**
 * EPIC 16 (2026-07-13, owner-requested) — a real dashboard, not the mockup's illustrative
 * "3 Meetings Today / HR Mail Received" cards (AION has no calendar/email integration to back
 * those with — showing them would be exactly the kind of thing this project's "AI never
 * pretends" rule forbids). Every number on this screen reads from a real source: [SetupPermission]
 * for onboarding completeness, [SecretVault] for configured provider keys, and [AuditLogger]'s
 * actual hash-chained log for recent activity. The "Tap to Speak" circle navigates to the real
 * [com.aion.host.brain.ChatScreen] — the actual place a goal can be given today — since
 * `VoiceSessionManager` (T-015) doesn't exist yet to make voice its own standalone entry point.
 */
@Composable
fun HomeScreen(
    auditLogger: AuditLogger,
    secretVault: SecretVault,
    voiceRunning: Boolean,
    onTapToSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val entries by auditLogger.observeEntries().collectAsState(initial = emptyList())
    val recent = entries.sortedByDescending { it.seq }.take(3)
    val grantedPermissions = SetupPermission.entries.count { it.isGranted(context) }
    val configuredKeys = ProviderKey.entries.count { secretVault.has(it) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("AION", style = MaterialTheme.typography.headlineMedium, color = AionColors.Primary)
        Text(
            "Here's what's actually running right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = AionColors.OnSurfaceMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        StatusCard("Setup", "$grantedPermissions/${SetupPermission.entries.size} permissions granted")
        Spacer(Modifier.height(12.dp))
        StatusCard("Providers", "$configuredKeys/${ProviderKey.entries.size} API keys configured")
        Spacer(Modifier.height(12.dp))
        StatusCard("Voice", if (voiceRunning) "Listening (mic active)" else "Idle (mic off)")

        Spacer(Modifier.height(24.dp))
        Text(
            "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            color = AionColors.OnBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (recent.isEmpty()) {
            Text("Nothing yet — actions you approve will show up here.", color = AionColors.OnSurfaceMuted)
        } else {
            recent.forEach { entry -> ActivityRow(entry) }
        }

        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AionColors.SurfaceVariant)
                        .clickable(onClick = onTapToSpeak),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎙", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text("Tap to talk to AION", color = AionColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AionColors.Surface)
                .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AionColors.OnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
    }
}

@Composable
private fun ActivityRow(entry: AuditLogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(entry.action, color = AionColors.OnBackground, style = MaterialTheme.typography.bodyMedium)
        Text(
            relativeTime(entry.ts),
            color = AionColors.OnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

private fun relativeTime(ts: Long): String {
    val diffMs = System.currentTimeMillis() - ts
    val minutes = diffMs / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}
