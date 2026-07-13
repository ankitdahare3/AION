package com.aion.host.communications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class SmsItem(
    val address: String,
    val body: String,
    val timestampMs: Long,
)

/** T-152 (EPIC 17) — reads the device's own real inbox SMS via `Telephony.Sms`, most-recent-first.
 * Gracefully returns an empty list without `READ_SMS`, same degrade pattern as `CalendarReader`/
 * `CallLogReader`. Deliberately just a raw reader — no transaction-parsing logic here; the
 * finance-via-SMS roadmap item (owner-approved, TASKS.md EPIC 17) is its own separate task that
 * will consume this, not something to build speculatively alongside a Communications screen. */
class SmsReader(private val context: Context) {
    fun recentMessages(limit: Int = 20): List<SmsItem> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        val messages = mutableListOf<SmsItem>()
        context.contentResolver
            .query(Telephony.Sms.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC")
            ?.use { cursor ->
                while (cursor.moveToNext() && messages.size < limit) {
                    messages.add(
                        SmsItem(
                            address = cursor.getString(0) ?: "",
                            body = cursor.getString(1) ?: "",
                            timestampMs = cursor.getLong(2),
                        ),
                    )
                }
            }
        return messages
    }
}
