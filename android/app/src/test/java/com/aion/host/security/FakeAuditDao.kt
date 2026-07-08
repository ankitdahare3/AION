package com.aion.host.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory AuditDao for plain-JVM unit tests — no Room/Android runtime needed. */
class FakeAuditDao : AuditDao {
    private val entries = mutableListOf<AuditLogEntry>()
    private val flow = MutableStateFlow<List<AuditLogEntry>>(emptyList())

    override suspend fun insert(entry: AuditLogEntry): Long {
        val seq = entries.size + 1L
        entries.add(entry.copy(seq = seq))
        flow.value = entries.toList()
        return seq
    }

    override suspend fun getAllOrdered(): List<AuditLogEntry> = entries.toList()

    override suspend fun getLast(): AuditLogEntry? = entries.lastOrNull()

    override fun observeAll(): Flow<List<AuditLogEntry>> = flow
}
