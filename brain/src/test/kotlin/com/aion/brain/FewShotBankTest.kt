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
    fun `T-163 - re-adding the same goal accumulates distinct mistakes instead of overwriting the last one`() {
        // Was a real gap: a Map keyed by goal meant a SECOND mistake silently erased the first,
        // so a model cycling between 2-3 different wrong choices could "forget" an earlier one
        // the instant a different one overwrote it. ReflectorAgent's own retry ceiling (T-163,
        // default 5) is what makes accumulating safe now — a single stuck run can't add more
        // than a handful of entries for its own goal before the ceiling ends the run anyway.
        val bank = FewShotBank()
        bank.add(CounterExample("wifi on karo", "[]", "first mistake"))
        bank.add(CounterExample("wifi on karo", "[]", "second mistake"))

        val result = bank.examplesFor("wifi on karo")

        assertEquals(2, result.size)
        assertEquals(listOf("first mistake", "second mistake"), result.map { it.reason })
    }

    @Test
    fun `T-163 - a single goal's mistakes are capped at maxPerGoal, evicting its own oldest entry`() {
        val bank = FewShotBank(maxPerGoal = 2)
        bank.add(CounterExample("wifi on karo", "[]", "mistake 1"))
        bank.add(CounterExample("wifi on karo", "[]", "mistake 2"))
        bank.add(CounterExample("wifi on karo", "[]", "mistake 3"))

        val result = bank.examplesFor("wifi on karo")

        assertEquals(2, result.size)
        assertEquals(listOf("mistake 2", "mistake 3"), result.map { it.reason })
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
