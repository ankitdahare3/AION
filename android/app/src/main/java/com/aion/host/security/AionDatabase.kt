package com.aion.host.security

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aion.host.brain.BudgetDao
import com.aion.host.brain.BudgetDayEntity
import com.aion.host.brain.GraphCheckpointDao
import com.aion.host.brain.GraphCheckpointEntity
import com.aion.host.brain.ProviderCooldownEntity
import com.aion.host.brain.ProviderStatsDao
import com.aion.host.brain.ProviderTaskStatEntity

/**
 * DOC-019 §1 — v2 adds provider_task_stats/provider_cooldown/budget_day (T-030, DOC-013 §2/§4);
 * v3 adds graph_checkpoint (T-053); v1 covered only audit_log (T-020). T-060 expands this to the
 * full remaining schema (conversations, turns, memories, episodes, skills, element_maps, plugins)
 * and adds SQLCipher.
 */
@Database(
    entities = [
        AuditLogEntry::class,
        ProviderTaskStatEntity::class,
        ProviderCooldownEntity::class,
        BudgetDayEntity::class,
        GraphCheckpointEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AionDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao

    abstract fun providerStatsDao(): ProviderStatsDao

    abstract fun budgetDao(): BudgetDao

    abstract fun graphCheckpointDao(): GraphCheckpointDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `provider_task_stats` (
                            `provider` TEXT NOT NULL,
                            `taskType` TEXT NOT NULL,
                            `successEma` REAL NOT NULL,
                            `latencyEmaMs` REAL NOT NULL,
                            `costEma` REAL NOT NULL,
                            `updated` INTEGER NOT NULL,
                            PRIMARY KEY(`provider`, `taskType`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `provider_cooldown` (
                            `provider` TEXT NOT NULL,
                            `cooldownUntilMs` INTEGER NOT NULL,
                            PRIMARY KEY(`provider`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `budget_day` (
                            `dayEpoch` INTEGER NOT NULL,
                            `spentUsd` REAL NOT NULL,
                            PRIMARY KEY(`dayEpoch`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `graph_checkpoint` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `goal` TEXT NOT NULL,
                            `currentStep` INTEGER NOT NULL,
                            `stepCount` INTEGER NOT NULL,
                            `needsApproval` INTEGER NOT NULL,
                            `done` INTEGER NOT NULL,
                            `response` TEXT,
                            `planSummary` TEXT NOT NULL,
                            `toolResultsCount` INTEGER NOT NULL,
                            `failuresSummary` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
