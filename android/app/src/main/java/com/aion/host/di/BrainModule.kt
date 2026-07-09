package com.aion.host.di

import com.aion.brain.BudgetGuard
import com.aion.brain.ScoreStore
import com.aion.host.brain.RoomBudgetGuard
import com.aion.host.brain.RoomScoreStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the frozen :brain contracts (DOC-013) to their Room-backed implementations (T-030). */
@Module
@InstallIn(SingletonComponent::class)
abstract class BrainModule {
    @Binds
    abstract fun bindScoreStore(impl: RoomScoreStore): ScoreStore

    @Binds
    abstract fun bindBudgetGuard(impl: RoomBudgetGuard): BudgetGuard
}
