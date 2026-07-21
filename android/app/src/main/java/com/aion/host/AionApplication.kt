package com.aion.host

import android.app.Application
import com.aion.host.brain.RoomBudgetGuard
import com.aion.host.brain.RoomScoreStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** DOC-020 S1 app skeleton — Hilt DI entry point for the :core process. */
@HiltAndroidApp
class AionApplication : Application() {
    @Inject
    lateinit var scoreStore: RoomScoreStore

    @Inject
    lateinit var budgetGuard: RoomBudgetGuard

    override fun onCreate() {
        super.onCreate()
        // T-030's own doc comment on RoomScoreStore: "Call load() once at startup to warm the
        // cache from disk before the first ScoreStore call" — never had a real caller until now.
        // RoomBudgetGuard.load() had the exact same doc comment and the exact same gap: today's
        // spend was never read back from Room, so every process start began at $0 regardless of
        // what was actually spent earlier today (found 2026-07-13 alongside Gemini's missing PAID
        // tier — with FREE, canSpend() was never even consulted, so this alone wasn't yet visible).
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch { scoreStore.load() }
        scope.launch { budgetGuard.load() }
    }
}
