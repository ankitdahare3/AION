package com.aion.host.communications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import com.aion.host.ui.theme.MockupScaffold
import java.text.DateFormat
import java.util.Date

@Composable
fun CommunicationsScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val callReader = remember { CallLogReader(context) }
    val smsReader = remember { SmsReader(context) }
    var calls by remember { mutableStateOf(callReader.recentCalls()) }
    var messages by remember { mutableStateOf(smsReader.recentMessages()) }
    var selectedTab by remember { mutableStateOf("All") }

    LaunchedEffect(resumeSignal) {
        calls = callReader.recentCalls()
        messages = smsReader.recentMessages()
    }

    MockupScaffold(
        title = "Communications",
        onBack = onBack,
        trailingIcon = Icons.Filled.Person,
        onTrailingClick = { /* Profile */ },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            // Tabs Segmented Control
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AionColors.SurfaceContainerHigh, RoundedCornerShape(12.dp))
                        .padding(4.dp),
            ) {
                val tabs = listOf("All", "Calls", "Messages", "Emails")
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AionColors.PrimaryContainer else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) AionColors.OnPrimaryContainer else AionColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Priority Section
            Text(
                "PRIORITY",
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.Primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PriorityCard(Icons.Filled.CorporateFare, "HR Department", "Managed", Color(0xFF34D399)) // Emerald
                    PriorityCard(Icons.Filled.Person, "Ananya Singh", "UX Architect", Color(0xFF34D399)) // Emerald
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PriorityCard(Icons.Filled.Person, "Rahul Sharma", "Lead Developer", Color(0xFF60A5FA)) // Blue
                    PriorityCard(Icons.Filled.Groups, "Team Project", "12 Members", Color(0xFFFB923C)) // Orange
                }
            }

            Spacer(Modifier.height(32.dp))

            // Recent Activity Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.Primary,
                    modifier = Modifier.padding(start = 4.dp),
                    letterSpacing = 1.sp,
                )
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.PrimaryContainer,
                    modifier = Modifier.clickable { },
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == "All" || selectedTab == "Calls") {
                if (calls.isNotEmpty()) {
                    calls.take(5).forEach { call ->
                        CallCard(call)
                        Spacer(Modifier.height(12.dp))
                    }
                } else if (selectedTab == "Calls") {
                    EmptyHint("No calls found, or permission not granted.")
                }
            }
            if (selectedTab == "All" || selectedTab == "Messages") {
                if (messages.isNotEmpty()) {
                    messages.take(10).forEach { msg ->
                        SmsCard(msg)
                        Spacer(Modifier.height(12.dp))
                    }
                } else if (selectedTab == "Messages") {
                    EmptyHint("No messages found, or permission not granted.")
                }
            }
            if (selectedTab == "Emails") {
                EmptyHint("Emails feature is not connected yet.")
            }

            // Mock items if real items are empty and we are in "All" view to show design
            if (selectedTab == "All" && calls.isEmpty() && messages.isEmpty()) {
                MockActivityCard(
                    Icons.Filled.CallMissed,
                    AionColors.Error,
                    "+1 234 567 890",
                    "California, USA",
                    "2h ago",
                )
                Spacer(Modifier.height(12.dp))
                MockActivityCard(
                    Icons.Filled.CallReceived,
                    AionColors.PrimaryContainer,
                    "+44 20 7946 0958",
                    "London, UK",
                    "Yesterday",
                )
                Spacer(Modifier.height(12.dp))
                MockActivityCard(
                    Icons.Filled.CallMade,
                    AionColors.OnSurfaceVariant,
                    "+81 3 1234 5678",
                    "Tokyo, JP",
                    "Yesterday",
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PriorityCard(
    icon: ImageVector,
    name: String,
    subtitle: String,
    statusColor: Color,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(64.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .background(AionColors.SurfaceContainerHighest, CircleShape)
                            .border(1.dp, AionColors.OutlineVariant.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = AionColors.Primary, modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 4.dp)
                            .size(12.dp)
                            .background(statusColor, CircleShape)
                            .border(2.dp, AionColors.Surface, CircleShape),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                subtitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.OnSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnSurfaceVariant)
    }
}

@Composable
private fun MockActivityCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    time: String,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnSurface)
                Text(
                    subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.OnSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            }
            Text(
                time,
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun CallCard(call: CallLogItem) {
    val isMissed = call.direction == CallDirection.MISSED
    val iconColor = if (isMissed) AionColors.Error else AionColors.PrimaryContainer
    val icon =
        when (call.direction) {
            CallDirection.MISSED -> Icons.Filled.CallMissed
            CallDirection.INCOMING -> Icons.Filled.CallReceived
            CallDirection.OUTGOING -> Icons.Filled.CallMade
            else -> Icons.Filled.Call
        }

    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(call.displayName, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnSurface)
                Text(
                    call.direction.name.lowercase().replaceFirstChar {
                        it.uppercase()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        AionColors.OnSurfaceVariant.copy(
                            alpha = 0.6f,
                        ),
                    fontSize = 10.sp,
                )
            }
            Text(
                dateTimeFormat.format(Date(call.timestampMs)),
                style = MaterialTheme.typography.labelSmall,
                color = AionColors.OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SmsCard(sms: SmsItem) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(AionColors.Secondary.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, AionColors.Secondary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = AionColors.Secondary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sms.address, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnSurface)
                    Text(
                        dateTimeFormat.format(Date(sms.timestampMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = AionColors.OnSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    sms.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = AionColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val dateTimeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
