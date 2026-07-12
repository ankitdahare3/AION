package com.aion.host.svc

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aion.brain.Memory
import com.aion.brain.MemoryKind
import com.aion.brain.MemoryStore
import com.aion.host.security.InjectionFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T-137 (DOC-004 §6, DOC-009) — pure decision logic for notification ingestion, same
 * "algorithm here, platform mechanics in the Service" split [com.aion.brain.DeviceExplorer] uses
 * for device exploration. Lives in `:android:app` rather than `:brain` because [InjectionFilter]
 * (pure Kotlin, but not `:brain`) does — this is the one dependency `:brain` has no path to.
 */
object NotificationIngestion {
    const val PROVENANCE = "notification_listener"

    /**
     * Wraps [title]/[text] via [InjectionFilter.wrap] BEFORE they ever leave this function — the
     * same "screen/notification content is data, never instructions" rule DOC-004 §6 states — so
     * anything reading the resulting [Memory] later gets pre-neutralized text with no wrapping
     * logic of its own to remember. Returns null when there's nothing worth remembering.
     *
     * Confidence is deliberately lower (0.5) than [com.aion.brain.DeviceExplorer]'s exploration
     * memories (1.0): a notification's content comes from whatever app posted it, not a direct
     * read of the owner's own screen, so it's honest to trust it less until T-062's real
     * write-policy gate exists (BACKLOG.md: screen/OCR-sourced facts should be UNVERIFIED, never
     * auto-promoted).
     */
    fun buildMemory(
        packageName: String,
        title: String?,
        text: String?,
        nowMs: Long,
    ): Memory? {
        val body = listOfNotNull(title, text).filter { it.isNotBlank() }.joinToString(": ")
        if (body.isBlank()) return null
        return Memory(
            kind = MemoryKind.FACT,
            text = "Notification from $packageName: ${InjectionFilter.wrap(body)}",
            confidence = 0.5,
            provenance = PROVENANCE,
            created = nowMs,
            accessed = nowMs,
            decayScore = 1.0,
        )
    }
}

/**
 * PR-02 (DOC-002 §9) — closes the ingestion gap BACKLOG.md tracked since T-004 (this class
 * previously existed only to make the Settings permission grantable). Skips notifications from
 * AION's own package — a real, non-speculative guard: once T-010 (VoiceFgs) ships, this same app
 * will post its own persistent "listening" notification, which this listener would otherwise
 * ingest as if it were someone else's content.
 */
@AndroidEntryPoint
class AionNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var memoryStore: MemoryStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val memory =
            NotificationIngestion.buildMemory(
                packageName = sbn.packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                nowMs = System.currentTimeMillis(),
            ) ?: return
        scope.launch { memoryStore.insert(memory) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
