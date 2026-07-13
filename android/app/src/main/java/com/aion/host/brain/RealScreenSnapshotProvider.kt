package com.aion.host.brain

import com.aion.brain.ScreenSnapshotProvider
import com.aion.host.automation.AionAccessibilityService
import com.aion.host.security.InjectionFilter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-139 (BACKLOG.md, E1_WRONG_ELEMENT) — bridges the frozen `:brain` [ScreenSnapshotProvider]
 * contract to the real, already-connected [AionAccessibilityService]. Returns null when no
 * accessibility session is live (service not connected, or no active window) — [PlannerAgent]
 * treats that the same as never having a provider at all. Wraps via [InjectionFilter.wrap] before
 * the text ever crosses into `:brain`, same as [com.aion.host.svc.NotificationIngestion] (T-137)
 * does for notification content.
 */
@Singleton
class RealScreenSnapshotProvider
    @Inject
    constructor() : ScreenSnapshotProvider {
        override suspend fun currentScreenText(): String? {
            val raw = AionAccessibilityService.instance?.currentScreenText() ?: return null
            return InjectionFilter.wrap(raw)
        }
    }
