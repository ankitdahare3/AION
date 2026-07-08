package com.aion.host.security

import java.security.MessageDigest

/**
 * DOC-017 §4 — hash-chain math, deliberately dependency-free (no Room/Android) so it is
 * unit-testable on the plain JVM without an emulator or Robolectric.
 */
object AuditChain {
    val GENESIS_HASH = "0".repeat(64)

    fun computeHash(
        prevHash: String,
        actor: String,
        action: String,
        payloadJson: String,
        ts: Long,
    ): String {
        val input = "$prevHash|$actor|$action|$payloadJson|$ts"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * [entries] must be in ascending seq order. Returns false if any entry's hash doesn't match
     * its recomputed value, or if the prevHash/hash links between entries are broken.
     */
    fun verify(entries: List<AuditLogEntry>): Boolean {
        var expectedPrev = GENESIS_HASH
        for (entry in entries) {
            if (entry.prevHash != expectedPrev) return false
            val recomputed = computeHash(entry.prevHash, entry.actor, entry.action, entry.payloadJson, entry.ts)
            if (recomputed != entry.hash) return false
            expectedPrev = entry.hash
        }
        return true
    }
}
