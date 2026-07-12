package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsePhrasingTest {
    @Test
    fun `a goal using common Hindi-Hinglish tokens is detected as Hinglish`() {
        assertTrue(ResponsePhrasing.isHinglish("wifi on karo"))
        assertTrue(ResponsePhrasing.isHinglish("mummy ko call karo"))
        assertTrue(ResponsePhrasing.isHinglish("aaj ke events dikhao"))
        assertTrue(ResponsePhrasing.isHinglish("WhatsApp khol ke naya message bhejo"))
    }

    @Test
    fun `a plain English goal is not detected as Hinglish`() {
        assertFalse(ResponsePhrasing.isHinglish("send a message to Ravi saying running late"))
        assertFalse(ResponsePhrasing.isHinglish("navigate home"))
        assertFalse(ResponsePhrasing.isHinglish("check credit card statement"))
    }

    @Test
    fun `success phrasing differs by language and never echoes the raw goal`() {
        assertEquals("Done! That's taken care of.", ResponsePhrasing.forSuccess(hinglish = false))
        assertEquals("Ho gaya! Kaam ho gaya.", ResponsePhrasing.forSuccess(hinglish = true))
    }

    @Test
    fun `every failure cause has a phrasing in both languages, and none contain a raw cause code`() {
        for (cause in FailureCause.entries) {
            val en = ResponsePhrasing.forFailure(cause, hinglish = false)
            val hi = ResponsePhrasing.forFailure(cause, hinglish = true)

            assertFalse("English phrasing for $cause leaked its own enum name", en.contains(cause.name))
            assertFalse("Hinglish phrasing for $cause leaked its own enum name", hi.contains(cause.name))
            assertTrue("English phrasing for $cause was blank", en.isNotBlank())
            assertTrue("Hinglish phrasing for $cause was blank", hi.isNotBlank())
        }
    }

    @Test
    fun `same cause gets a different phrasing per language`() {
        val en = ResponsePhrasing.forFailure(FailureCause.E5_PERMISSION_BLOCKED, hinglish = false)
        val hi = ResponsePhrasing.forFailure(FailureCause.E5_PERMISSION_BLOCKED, hinglish = true)

        assertNotEquals(en, hi)
    }
}
