package com.aion.brain

/** One app's read-only landing-screen scan; [screenText] is null when the app never became readable (launch failed, no window, service not connected). */
data class AppScanResult(
    val packageName: String,
    val screenText: String?,
)

/**
 * Manual "Explore Device" feature (DOC-008-adjacent) — turns a batch of read-only app scans into
 * [Memory] rows the planner can draw on for faster/more accurate future goal resolution. Pure
 * decision logic only, same "algorithm here, platform mechanics in :android:app" split
 * MemoryConsolidator/PatternLearner already use: launching each app and reading its accessibility
 * tree is Android-specific and lives in DeviceExplorationWorker; this just decides which scans are
 * worth remembering and how to phrase them.
 */
object DeviceExplorer {
    const val PROVENANCE = "device_exploration"

    fun buildProfileMemories(
        scans: List<AppScanResult>,
        nowMs: Long,
    ): List<Memory> =
        scans
            .filter { !it.screenText.isNullOrBlank() }
            .map { scan ->
                Memory(
                    kind = MemoryKind.PROFILE,
                    text = "App ${scan.packageName}: ${scan.screenText}",
                    confidence = 1.0,
                    provenance = PROVENANCE,
                    created = nowMs,
                    accessed = nowMs,
                    decayScore = 1.0,
                )
            }
}
