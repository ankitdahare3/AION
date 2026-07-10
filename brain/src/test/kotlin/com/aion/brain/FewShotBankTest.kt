package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FewShotBankTest {
    @Test
    fun `examplesFor returns only entries for the matching goal`() {
        val bank = FewShotBank()
        bank.add(CounterExample("wifi on karo", "[]", "wrong element"))
        bank.add(CounterExample("open settings", "[]", "wrong app"))

        val result = bank.examplesFor("wifi on karo")

        assertEquals(1, result.size)
        assertEquals("wrong element", result.single().reason)
    }

    @Test
    fun `an unknown goal has no counter-examples`() {
        val bank = FewShotBank()

        assertTrue(bank.examplesFor("never seen this goal").isEmpty())
    }

    @Test
    fun `re-adding the same goal replaces its counter-example rather than duplicating it`() {
        val bank = FewShotBank()
        bank.add(CounterExample("wifi on karo", "[]", "first mistake"))
        bank.add(CounterExample("wifi on karo", "[]", "second mistake"))

        val result = bank.examplesFor("wifi on karo")

        assertEquals(1, result.size)
        assertEquals("second mistake", result.single().reason)
    }

    @Test
    fun `the bank never grows past maxSize, evicting the oldest entry`() {
        val bank = FewShotBank(maxSize = 3)
        bank.add(CounterExample("goal1", "[]", "r1"))
        bank.add(CounterExample("goal2", "[]", "r2"))
        bank.add(CounterExample("goal3", "[]", "r3"))
        bank.add(CounterExample("goal4", "[]", "r4"))

        assertEquals(3, bank.size())
        assertTrue("oldest entry (goal1) should have been evicted", bank.examplesFor("goal1").isEmpty())
        assertTrue(bank.examplesFor("goal4").isNotEmpty())
    }
}
