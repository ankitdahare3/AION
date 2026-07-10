package com.aion.host.brain

import com.aion.brain.CachedSelector
import com.aion.brain.ElementMapCache
import com.aion.brain.FailureCause
import com.aion.brain.ScoringMath
import com.aion.host.memory.ElementMapDao
import com.aion.host.memory.ElementMapEntity
import javax.inject.Inject
import javax.inject.Singleton

/** T-080 — Room-backed [ElementMapCache]. AC: an [FailureCause.E2_UI_CHANGED] outcome invalidates the cached selector for that exact screen. */
@Singleton
class RoomElementMapCache
    @Inject
    constructor(
        private val dao: ElementMapDao,
    ) : ElementMapCache {
        override suspend fun get(
            appPkg: String,
            appVersion: String,
            screenHash: String,
        ): CachedSelector? =
            dao.getOne(appPkg, appVersion, screenHash)?.let { CachedSelector(it.selectorJson, it.confidence) }

        override suspend fun recordOutcome(
            appPkg: String,
            appVersion: String,
            screenHash: String,
            selectorJson: String,
            cause: FailureCause?,
        ) {
            when (cause) {
                null -> {
                    val previous = get(appPkg, appVersion, screenHash)?.confidence ?: ScoringMath.DEFAULT_SUCCESS_SCORE
                    upsert(appPkg, appVersion, screenHash, selectorJson, ScoringMath.ema(previous, 1.0))
                }
                FailureCause.E2_UI_CHANGED -> upsert(appPkg, appVersion, screenHash, selectorJson, confidence = 0.0)
                else -> {
                    // Not this selector's fault (e.g. E5 permission block) — leave the cache as-is.
                }
            }
        }

        private suspend fun upsert(
            appPkg: String,
            appVersion: String,
            screenHash: String,
            selectorJson: String,
            confidence: Double,
        ) {
            dao.upsert(
                ElementMapEntity(
                    appPkg = appPkg,
                    appVersion = appVersion,
                    screenHash = screenHash,
                    selectorJson = selectorJson,
                    confidence = confidence,
                    ts = System.currentTimeMillis(),
                ),
            )
        }
    }
