package com.aion.host.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reader = remember { CalendarReader(context) }
    var events by remember { mutableStateOf(reader.todayEvents()) }

    LaunchedEffect(resumeSignal) {
        events = reader.todayEvents()
    }

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "AION",
                trailingIcon = Icons.Filled.Add,
                onTrailingClick = { /* No-op for now */ },
            )

            // Content
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 16.dp)) {
                WeekStrip()

                Spacer(Modifier.height(24.dp))

                val nextEvent = events.firstOrNull { it.startMs > System.currentTimeMillis() }
                if (nextEvent != null) {
                    NextEventHighlight(nextEvent)
                    Spacer(Modifier.height(24.dp))
                }

                if (events.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No events found — either your day is free, or Calendar access hasn't been granted yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AionColors.OnSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(events) { index, event ->
                            val isPast = event.endMs < System.currentTimeMillis()
                            val isActive =
                                event.startMs <= System.currentTimeMillis() && event.endMs >= System.currentTimeMillis()
                            val isNext = event == nextEvent
                            EventCard(event, isPast = isPast, isActive = isActive, isNext = isNext, index = index)
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip() {
    val cal = Calendar.getInstance()
    val days =
        (0..6).map { i ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, i)
            c
        }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())

    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(days) { index, c ->
            val isToday = index == 0
            val dayName = dayFormat.format(c.time).uppercase()
            val dateNum = dateFormat.format(c.time)

            Box(
                modifier =
                    Modifier
                        .size(48.dp, 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isToday) AionColors.PrimaryContainer.copy(alpha = 0.1f) else Color.Transparent)
                        .then(
                            if (isToday) {
                                Modifier.border(
                                    1.dp,
                                    AionColors.PrimaryContainer.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isToday) {
                                AionColors.PrimaryContainer
                            } else {
                                AionColors.OnSurfaceVariant.copy(
                                    alpha = 0.7f,
                                )
                            },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dateNum,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isToday) AionColors.PrimaryContainer else AionColors.OnSurfaceVariant,
                    )
                }
                if (isToday) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .size(6.dp)
                                .background(AionColors.PrimaryContainer, CircleShape)
                                .shadow(8.dp, ambientColor = AionColors.PrimaryContainer),
                    )
                }
            }
        }
    }
}

@Composable
private fun NextEventHighlight(event: CalendarEvent) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2F3639).copy(alpha = 0.6f))
                .border(1.dp, AionColors.PrimaryContainer.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier =
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(AionColors.PrimaryContainer.copy(alpha = 0.1f), Color.Transparent)),
                ),
        )

        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "NEXT EVENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = AionColors.PrimaryContainer,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AionColors.OnSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("STARTS IN", style = MaterialTheme.typography.labelSmall, color = AionColors.OnSurfaceVariant)
                    val diffMins = ((event.startMs - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            diffMins.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = AionColors.PrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "m",
                            style = MaterialTheme.typography.bodySmall,
                            color = AionColors.PrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier =
                        Modifier
                            .size(
                                40.dp,
                            ).border(2.dp, AionColors.PrimaryContainer.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = AionColors.PrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    isPast: Boolean,
    isActive: Boolean,
    isNext: Boolean,
    index: Int,
) {
    val colors = listOf(Color(0xFF34D399), Color(0xFF60A5FA), Color(0xFFFB923C), Color(0xFFC084FC))
    val dotColor =
        if (isPast) {
            AionColors.OutlineVariant
        } else if (isActive ||
            isNext
        ) {
            AionColors.PrimaryContainer
        } else {
            colors[index % colors.size]
        }

    val bgAlpha = if (isActive || isNext) 0.05f else 0.7f
    val bgColor = if (isActive || isNext) AionColors.PrimaryContainer else Color(0xFF161D1F)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor.copy(alpha = bgAlpha))
                .border(
                    1.dp,
                    if (isActive ||
                        isNext
                    ) {
                        AionColors.PrimaryContainer.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.05f)
                    },
                    RoundedCornerShape(12.dp),
                ).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    if (event.allDay) "All day" else timeFormat.format(Date(event.startMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isActive ||
                            isNext
                        ) {
                            AionColors.PrimaryContainer
                        } else if (isPast) {
                            AionColors.OnSurfaceVariant
                        } else {
                            AionColors.OnSurface
                        },
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(dotColor, CircleShape)
                        .then(
                            if (!isPast) {
                                Modifier.shadow(
                                    8.dp,
                                    ambientColor = dotColor,
                                    spotColor = dotColor,
                                )
                            } else {
                                Modifier
                            },
                        ),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPast) AionColors.OnSurfaceVariant else AionColors.OnSurface,
                    fontWeight = if (isPast) FontWeight.Normal else FontWeight.SemiBold,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint =
                    if (isActive ||
                        isNext
                    ) {
                        AionColors.PrimaryContainer
                    } else {
                        AionColors.OnSurfaceVariant.copy(alpha = 0.5f)
                    },
            )
        }
    }
}

private val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
