package com.aion.host.di

import com.aion.brain.BudgetGuard
import com.aion.brain.FewShotBank
import com.aion.brain.MemoryStore
import com.aion.brain.ScoreStore
import com.aion.brain.ScreenSnapshotProvider
import com.aion.host.brain.RealScreenSnapshotProvider
import com.aion.host.brain.RoomBudgetGuard
import com.aion.host.brain.RoomMemoryStore
import com.aion.host.brain.RoomScoreStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the frozen :brain contracts (DOC-013) to their Room-backed implementations (T-030). */
@Module
@InstallIn(SingletonComponent::class)
abstract class BrainModule {
    @Binds
    abstract fun bindScoreStore(impl: RoomScoreStore): ScoreStore

    @Binds
    abstract fun bindBudgetGuard(impl: RoomBudgetGuard): BudgetGuard

    @Binds
    abstract fun bindMemoryStore(impl: RoomMemoryStore): MemoryStore

    @Binds
    abstract fun bindScreenSnapshotProvider(impl: RealScreenSnapshotProvider): ScreenSnapshotProvider

    companion object {
        // T-081/T-090 built FewShotBank as a plain :brain class with no Android/DI dependency of
        // its own (module-boundary rule) — a @Provides here is what makes it Hilt-constructable,
        // same reason RoomCheckpointer et al. need no such thing (they already have @Inject
        // constructors in :android:app). One process-lifetime instance, shared across every graph
        // AionGraphFactory.create()s, so a counter-example recorded for one goal benefits every
        // later attempt at that same goal, not just the run it failed in (T-158, BACKLOG.md).
        @Provides
        @Singleton
        fun provideFewShotBank(): FewShotBank = FewShotBank()
    }
}
