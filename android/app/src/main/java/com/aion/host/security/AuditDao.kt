package com.aion.host.security

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(entry: AuditLogEntry): Long

    @Query("SELECT * FROM audit_log ORDER BY seq ASC")
    suspend fun getAllOrdered(): List<AuditLogEntry>

    @Query("SELECT * FROM audit_log ORDER BY seq DESC LIMIT 1")
    suspend fun getLast(): AuditLogEntry?

    @Query("SELECT * FROM audit_log ORDER BY seq DESC")
    fun observeAll(): Flow<List<AuditLogEntry>>
}
