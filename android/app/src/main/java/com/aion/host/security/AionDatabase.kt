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
import com.aion.host.memory.ConversationDao
import com.aion.host.memory.ConversationEntity
import com.aion.host.memory.ElementMapDao
import com.aion.host.memory.ElementMapEntity
import com.aion.host.memory.EpisodeDao
import com.aion.host.memory.EpisodeEntity
import com.aion.host.memory.MemoryDao
import com.aion.host.memory.MemoryEntity
import com.aion.host.memory.PluginDao
import com.aion.host.memory.PluginEntity
import com.aion.host.memory.SkillDao
import com.aion.host.memory.SkillEntity
import com.aion.host.memory.TurnDao
import com.aion.host.memory.TurnEntity

/**
 * DOC-019 §1 — v2 adds provider_task_stats/provider_cooldown/budget_day (T-030, DOC-013 §2/§4);
 * v3 adds graph_checkpoint (T-053); v4 adds the full remaining schema (T-060: conversations, turns,
 * memories, episodes, skills, element_maps, plugins) + SQLCipher at-rest encryption (see
 * [com.aion.host.di.DatabaseModule]); v1 covered only audit_log (T-020). `vec_index` (sqlite-vec)
 * is T-061's job — needs the embedder, doesn't exist yet. `config` is explicitly DataStore(proto)
 * per DOC-019 §1, not Room, and nothing in the codebase needs it yet.
 */
@Database(
    entities = [
        AuditLogEntry::class,
        ProviderTaskStatEntity::class,
        ProviderCooldownEntity::class,
        BudgetDayEntity::class,
        GraphCheckpointEntity::class,
        ConversationEntity::class,
        TurnEntity::class,
        MemoryEntity::class,
        EpisodeEntity::class,
        SkillEntity::class,
        ElementMapEntity::class,
        PluginEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AionDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao

    abstract fun providerStatsDao(): ProviderStatsDao

    abstract fun budgetDao(): BudgetDao

    abstract fun graphCheckpointDao(): GraphCheckpointDao

    abstract fun conversationDao(): ConversationDao

    abstract fun turnDao(): TurnDao

    abstract fun memoryDao(): MemoryDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun skillDao(): SkillDao

    abstract fun elementMapDao(): ElementMapDao

    abstract fun pluginDao(): PluginDao

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

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `conversations` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `startedAt` INTEGER NOT NULL,
                            `summary` TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `turns` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `convId` INTEGER NOT NULL,
                            `role` TEXT NOT NULL,
                            `text` TEXT NOT NULL,
                            `lang` TEXT NOT NULL,
                            `ts` INTEGER NOT NULL,
                            FOREIGN KEY(`convId`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_turns_convId` ON `turns` (`convId`)")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `memories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `kind` TEXT NOT NULL,
                            `text` TEXT NOT NULL,
                            `confidence` REAL NOT NULL,
                            `provenance` TEXT NOT NULL,
                            `piiTags` TEXT NOT NULL,
                            `created` INTEGER NOT NULL,
                            `accessed` INTEGER NOT NULL,
                            `decayScore` REAL NOT NULL,
                            `deletedSoft` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `episodes` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `goal` TEXT NOT NULL,
                            `planJson` TEXT NOT NULL,
                            `outcome` TEXT NOT NULL,
                            `failureClass` TEXT,
                            `latencyMs` INTEGER NOT NULL,
                            `costUsd` REAL NOT NULL,
                            `appPkg` TEXT,
                            `ts` INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `skills` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `yaml` TEXT NOT NULL,
                            `version` INTEGER NOT NULL,
                            `status` TEXT NOT NULL,
                            `successCount` INTEGER NOT NULL,
                            `failCount` INTEGER NOT NULL,
                            `approvedAt` INTEGER
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `element_maps` (
                            `appPkg` TEXT NOT NULL,
                            `appVersion` TEXT NOT NULL,
                            `screenHash` TEXT NOT NULL,
                            `selectorJson` TEXT NOT NULL,
                            `confidence` REAL NOT NULL,
                            `ts` INTEGER NOT NULL,
                            PRIMARY KEY(`appPkg`, `appVersion`, `screenHash`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `plugins` (
                            `id` TEXT NOT NULL,
                            `version` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `permissionsJson` TEXT NOT NULL,
                            `installedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
