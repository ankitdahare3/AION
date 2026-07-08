package com.aion.host.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun buildValidChain(n: Int): List<AuditLogEntry> {
    var prevHash = AuditChain.GENESIS_HASH
    return (1..n).map { i ->
        val ts = 1_000L * i
        val action = "action-$i"
        val payload = """{"i":$i}"""
        val hash = AuditChain.computeHash(prevHash, "user", action, payload, ts)
        val entry =
            AuditLogEntry(
                seq = i.toLong(),
                prevHash = prevHash,
                hash = hash,
                actor = "user",
                action = action,
                payloadJson = payload,
                ts = ts,
            )
        prevHash = hash
        entry
    }
}

class AuditChainTest {
    @Test
    fun `empty chain is valid`() {
        assertTrue(AuditChain.verify(emptyList()))
    }

    @Test
    fun `valid chain of several entries verifies`() {
        assertTrue(AuditChain.verify(buildValidChain(5)))
    }

    @Test
    fun `computeHash is deterministic`() {
        val a = AuditChain.computeHash("prev", "user", "act", "{}", 42L)
        val b = AuditChain.computeHash("prev", "user", "act", "{}", 42L)
        assertEquals(a, b)
    }

    @Test
    fun `tampering an entry's payload without updating its hash breaks the chain`() {
        val chain = buildValidChain(4)
        val tampered = chain.toMutableList()
        tampered[2] = tampered[2].copy(payloadJson = """{"i":"HACKED"}""")
        assertFalse(AuditChain.verify(tampered))
    }

    @Test
    fun `tampering an entry's action without updating its hash breaks the chain`() {
        val chain = buildValidChain(4)
        val tampered = chain.toMutableList()
        tampered[0] = tampered[0].copy(action = "different-action")
        assertFalse(AuditChain.verify(tampered))
    }

    @Test
    fun `deleting a middle entry breaks the prevHash link`() {
        val chain = buildValidChain(4)
        val withGap = chain.toMutableList().apply { removeAt(1) }
        assertFalse(AuditChain.verify(withGap))
    }

    @Test
    fun `reordering entries breaks the prevHash link`() {
        val chain = buildValidChain(4)
        val reordered = listOf(chain[1], chain[0], chain[2], chain[3])
        assertFalse(AuditChain.verify(reordered))
    }
}
