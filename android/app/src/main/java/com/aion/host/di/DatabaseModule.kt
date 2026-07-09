package com.aion.host.di

import android.content.Context
import androidx.room.Room
import com.aion.host.brain.BudgetDao
import com.aion.host.brain.ProviderStatsDao
import com.aion.host.security.AionDatabase
import com.aion.host.security.AuditDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAionDatabase(
        @ApplicationContext context: Context,
    ): AionDatabase =
        Room
            .databaseBuilder(context, AionDatabase::class.java, "aion.db")
            .addMigrations(AionDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideAuditDao(db: AionDatabase): AuditDao = db.auditDao()

    @Provides
    fun provideProviderStatsDao(db: AionDatabase): ProviderStatsDao = db.providerStatsDao()

    @Provides
    fun provideBudgetDao(db: AionDatabase): BudgetDao = db.budgetDao()
}
