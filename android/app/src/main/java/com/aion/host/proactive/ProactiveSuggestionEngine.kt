package com.aion.host.proactive

enum class SuggestionKind { BATTERY_LOW, UPCOMING_EVENT, TAKE_A_BREAK }

data class ProactiveSuggestion(
    val kind: SuggestionKind,
    val message: String,
)

/**
 * T-155 (EPIC 17, mockup #11) — pure evaluator (no Android dependency, unit-testable without a
 * device), so the real screen just wires already-real readers' numbers in as plain primitives.
 * Deliberately only 3 signals: `DeviceStatusReader` (battery), `CalendarReader` (events),
 * `UsageStatsReader` (screen time) all already exist and are genuinely real. The mockup's other
 * suggestion types ("Linux Mint download completed", "HR verification mail is pending") are
 * NOT built here — a third-party app's downloads aren't visible to us (`DownloadManager.query()`
 * without `ACCESS_DOWNLOAD_MANAGER`, a system-signature permission we can't hold, only returns
 * downloads WE initiated) and "is this mail asking for a reply" needs real content
 * classification, not a threshold check. Filed in BACKLOG.md rather than faked with a canned
 * example.
 *
 * ponytail: thresholds (20% battery, 15min lookahead, 2h screen time) are simple fixed constants,
 * not learned/configurable — upgrade path is exposing them as owner-tunable settings if the
 * defaults turn out wrong for someone's actual routine, not preemptively now.
 */
object ProactiveSuggestionEngine {
    private const val LOW_BATTERY_PERCENT = 20
    private const val UPCOMING_EVENT_WINDOW_MS = 15 * 60 * 1000L
    private const val BREAK_REMINDER_THRESHOLD_MS = 2 * 60 * 60 * 1000L

    fun evaluate(
        batteryPercent: Int,
        charging: Boolean,
        nowMs: Long,
        nextEventStartMs: Long?,
        nextEventTitle: String?,
        screenTimeTodayMs: Long,
    ): List<ProactiveSuggestion> {
        val suggestions = mutableListOf<ProactiveSuggestion>()

        if (batteryPercent in 0 until LOW_BATTERY_PERCENT && !charging) {
            suggestions.add(
                ProactiveSuggestion(
                    SuggestionKind.BATTERY_LOW,
                    "Battery is at $batteryPercent% — turn on Power Saver?",
                ),
            )
        }

        if (nextEventStartMs != null && nextEventTitle != null) {
            val minutesAway = (nextEventStartMs - nowMs) / 60_000
            if (minutesAway in 0..(UPCOMING_EVENT_WINDOW_MS / 60_000)) {
                suggestions.add(
                    ProactiveSuggestion(
                        SuggestionKind.UPCOMING_EVENT,
                        "\"$nextEventTitle\" starts in ${minutesAway}min — get ready?",
                    ),
                )
            }
        }

        if (screenTimeTodayMs >= BREAK_REMINDER_THRESHOLD_MS) {
            val hours = screenTimeTodayMs / 3_600_000
            suggestions.add(
                ProactiveSuggestion(
                    SuggestionKind.TAKE_A_BREAK,
                    "You've been on your phone for ${hours}h+ today — take a short break?",
                ),
            )
        }

        return suggestions
    }
}
