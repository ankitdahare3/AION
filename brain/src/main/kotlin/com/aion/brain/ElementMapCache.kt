package com.aion.brain

data class CachedSelector(
    val selectorJson: String,
    val confidence: Double,
)

/**
 * DOC-019 §1 `element_maps` / DOC-007 §3 "ElementMap per app-version (selector cache with
 * confidence decay)". T-080 AC: E2 (UI changed) auto-invalidates the cached selector for that
 * screen. Kept Android-independent (Room-backed impl lives in `:android:app`, T-080) — this is
 * pure caching policy, not persistence mechanics.
 */
interface ElementMapCache {
    suspend fun get(
        appPkg: String,
        appVersion: String,
        screenHash: String,
    ): CachedSelector?

    /**
     * Records what happened the last time [selectorJson] was used for this screen.
     * [cause] is `null` for success (rewards confidence via EMA toward 1.0); [FailureCause.E2_UI_CHANGED]
     * invalidates the cache entirely (confidence -> 0) — DOC-007 §2's own mapping for that cause;
     * any other cause leaves the cached selector untouched, since the failure wasn't necessarily
     * about this selector being wrong (e.g. E5 permission block isn't the selector's fault).
     */
    suspend fun recordOutcome(
        appPkg: String,
        appVersion: String,
        screenHash: String,
        selectorJson: String,
        cause: FailureCause?,
    )
}
