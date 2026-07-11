package com.aion.host

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        // T-030's own doc comment on RoomScoreStore: "Call load() once at startup to warm the
        // cache from disk before the first ScoreStore call" — never had a real caller until now.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { scoreStore.load() }
    }
}
