package com.aion.host.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** DOC-017 §4 — audit log viewer with a live tamper-check against the hash chain (mockup: "tamper-proof audit log"). */
@Composable
fun AuditLogScreen(
    auditLogger: AuditLogger,
    modifier: Modifier = Modifier,
) {
    val entries by auditLogger.observeEntries().collectAsState(initial = emptyList())
    var chainValid by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "Tamper-Proof Audit Log",
                trailingIcon = Icons.Filled.Security,
                onTrailingClick = { scope.launch { chainValid = auditLogger.verifyChain() } },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp),
            ) {
                // Banner
                GlassPanel(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, AionColors.Primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                ) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(AionColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Terminal, contentDescription = null, tint = AionColors.Primary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Hash-chained execution log.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AionColors.OnSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "All AION actions are recorded here with immutable cryptographic verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                            if (chainValid != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (chainValid ==
                                        true
                                    ) {
                                        "CHAIN VERIFIED - INTACT"
                                    } else {
                                        "CHAIN VERIFICATION FAILED - TAMPERED"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (chainValid == true) Color(0xFF34D399) else AionColors.Error,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No entries yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AionColors.OnSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(entries.reversed()) { entry ->
                            AuditTimelineItem(entry)
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditTimelineItem(entry: AuditLogEntry) {
    val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault())
    val timestampStr = formatter.format(Date()) // Mock timestamp since AuditEntry lacks it

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline line + icon
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(AionColors.SurfaceContainer, CircleShape)
                        .border(1.dp, AionColors.Primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = AionColors.Primary)
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(
                            Brush.verticalGradient(listOf(AionColors.Primary.copy(alpha = 0.3f), Color.Transparent)),
                        ),
            )
        }

        Spacer(Modifier.width(24.dp))

        // Content
        GlassPanel(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(bottom = 32.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SUCCESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                            fontSize =
                                androidx.compose.ui.unit
                                    .TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp),
                        )
                    }
                    Text(
                        timestampStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = AionColors.OnSurfaceVariant,
                        fontSize =
                            androidx.compose.ui.unit
                                .TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Action", style = MaterialTheme.typography.labelSmall, color = AionColors.OnSurfaceVariant)
                    Text(
                        entry.action,
                        style = MaterialTheme.typography.bodySmall,
                        color = AionColors.OnSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Actor", style = MaterialTheme.typography.labelSmall, color = AionColors.OnSurfaceVariant)
                    Text(entry.actor, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurface)
                }
                androidx.compose.material3.Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                Spacer(Modifier.height(8.dp))

                Text(
                    "Hash Signature (seq=${entry.seq})",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                AionColors.Primary.copy(alpha = 0.05f),
                                RoundedCornerShape(4.dp),
                            ).padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        entry.hash,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = AionColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
