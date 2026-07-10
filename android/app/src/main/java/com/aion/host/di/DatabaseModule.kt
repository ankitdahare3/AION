package com.aion.host.di

import android.content.Context
import androidx.room.Room
import com.aion.host.brain.BudgetDao
import com.aion.host.brain.GraphCheckpointDao
import com.aion.host.brain.ProviderStatsDao
import com.aion.host.memory.ElementMapDao
import com.aion.host.memory.EpisodeDao
import com.aion.host.security.AionDatabase
import com.aion.host.security.AuditDao
import com.aion.host.security.DbPassphraseStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAionDatabase(
        @ApplicationContext context: Context,
        passphraseStore: DbPassphraseStore,
    ): AionDatabase {
        SQLiteDatabase.loadLibs(context)
        val factory = SupportFactory(SQLiteDatabase.getBytes(passphraseStore.getOrCreatePassphrase()))
        return Room
            .databaseBuilder(context, AionDatabase::class.java, "aion.db")
            .openHelperFactory(factory)
            .addMigrations(AionDatabase.MIGRATION_1_2, AionDatabase.MIGRATION_2_3, AionDatabase.MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideAuditDao(db: AionDatabase): AuditDao = db.auditDao()

    @Provides
    fun provideProviderStatsDao(db: AionDatabase): ProviderStatsDao = db.providerStatsDao()

    @Provides
    fun provideBudgetDao(db: AionDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideGraphCheckpointDao(db: AionDatabase): GraphCheckpointDao = db.graphCheckpointDao()

    @Provides
    fun provideEpisodeDao(db: AionDatabase): EpisodeDao = db.episodeDao()

    @Provides
    fun provideElementMapDao(db: AionDatabase): ElementMapDao = db.elementMapDao()
}
