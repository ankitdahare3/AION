package com.aion.host.security

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * DOC-019 §1 — v1 covers only audit_log (T-020's scope); T-060 expands this to the full schema
 * (conversations, turns, memories, episodes, skills, element_maps, providers_stats, plugins) and
 * adds SQLCipher at-rest encryption.
 */
@Database(entities = [AuditLogEntry::class], version = 1, exportSchema = true)
abstract class AionDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
}
