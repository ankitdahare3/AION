package com.aion.host.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId

/** T-151 (EPIC 17) — a real calendar event, read from Android's own `CalendarProvider`. No AION
 * schema of its own: this is exactly what's on the device, nothing invented. */
data class CalendarEvent(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val allDay: Boolean,
)

/** Reads today's real events via `CalendarContract.Instances` (resolves recurring events within
 * a range — the raw `Events` table wouldn't). Gracefully returns an empty list without the
 * `READ_CALENDAR` permission, same degrade pattern as `ShizukuBridge`/`AppLockGate` — this project
 * never crashes on a missing capability, it reports "not available." */
class CalendarReader(private val context: Context) {
    fun todayEvents(nowMs: Long = System.currentTimeMillis()): List<CalendarEvent> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val (startOfDay, endOfDay) = dayRangeMs(nowMs)
        val uri =
            CalendarContract.Instances.CONTENT_URI
                .buildUpon()
                .also {
                    ContentUris.appendId(it, startOfDay)
                    ContentUris.appendId(it, endOfDay)
                }.build()
        val projection =
            arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
            )
        val events = mutableListOf<CalendarEvent>()
        context.contentResolver
            .query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            title = cursor.getString(0) ?: "(no title)",
                            startMs = cursor.getLong(1),
                            endMs = cursor.getLong(2),
                            allDay = cursor.getInt(3) != 0,
                        ),
                    )
                }
            }
        return events
    }
}

/** Pure, Android-independent so it's unit-testable without Robolectric/instrumentation. */
internal fun dayRangeMs(nowMs: Long): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return startOfDay to endOfDay
}
