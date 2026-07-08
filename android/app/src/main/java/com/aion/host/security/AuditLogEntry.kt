package com.aion.host.security

import androidx.room.Entity
import androidx.room.PrimaryKey

/** DOC-019 §1 audit_log — hash-chained: hash = H(prevHash, actor, action, payloadJson, ts). */
@Entity(tableName = "audit_log")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val prevHash: String,
    val hash: String,
    val actor: String,
    val action: String,
    val payloadJson: String,
    val ts: Long,
)
