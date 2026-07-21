package com.aion.host.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import kotlinx.coroutines.launch

/** DOC-017 §4 — audit log viewer with a live tamper-check against the hash chain (mockup: "tamper-proof audit log"). */
@Composable
fun AuditLogScreen(
    auditLogger: AuditLogger,
    modifier: Modifier = Modifier,
) {
    val entries by auditLogger.observeEntries().collectAsState(initial = emptyList())
    var chainValid by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Audit Log", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Button(onClick = { scope.launch { chainValid = auditLogger.verifyChain() } }) {
                Text("Verify chain")
            }
            Spacer(modifier = Modifier.width(12.dp))
            when (chainValid) {
                true -> Text("Intact", color = AionColors.SecurityGreen)
                false -> Text("TAMPERED", color = AionColors.Error)
                null -> {}
            }
        }
        if (entries.isEmpty()) {
            Text("No entries yet.", style = MaterialTheme.typography.bodyMedium, color = AionColors.OnSurfaceVariant)
        } else {
            GlassPanel {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(entries) { entry ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(
                                "${entry.actor} → ${entry.action}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AionColors.OnBackground,
                            )
                            Text(
                                entry.payloadJson,
                                style = MaterialTheme.typography.bodySmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                            Text(
                                "seq=${entry.seq}  hash=${entry.hash.take(12)}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = AionColors.OutlineVariant)
                    }
                }
            }
        }
    }
}
