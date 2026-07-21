package com.aion.host.communications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat

enum class CallDirection { INCOMING, OUTGOING, MISSED, OTHER }

data class CallLogItem(
    val displayName: String,
    val number: String,
    val direction: CallDirection,
    val timestampMs: Long,
)

/** T-152 (EPIC 17) — reads the device's own real `CallLog.Calls`, most-recent-first. Gracefully
 * returns an empty list without `READ_CALL_LOG`, same degrade pattern as `CalendarReader`. Also
 * gracefully returns empty if the query itself throws (e.g. a transiently-unavailable content
 * provider) rather than crashing the screen (T-160). */
class CallLogReader(
    private val context: Context,
) {
    fun recentCalls(limit: Int = 10): List<CallLogItem> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val projection =
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE)
        val calls = mutableListOf<CallLogItem>()
        try {
            context.contentResolver
                .query(CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC")
                ?.use { cursor ->
                    while (cursor.moveToNext() && calls.size < limit) {
                        val number = cursor.getString(1) ?: ""
                        calls.add(
                            CallLogItem(
                                displayName = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: number,
                                number = number,
                                direction = directionOf(cursor.getInt(2)),
                                timestampMs = cursor.getLong(3),
                            ),
                        )
                    }
                }
        } catch (e: Exception) {
            return emptyList()
        }
        return calls
    }

    private fun directionOf(type: Int): CallDirection =
        when (type) {
            CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
            else -> CallDirection.OTHER
        }
}
