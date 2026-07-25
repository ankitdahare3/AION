package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LlmIntentClassificationTest {
    @Test
    fun `an exact clean label parses to the matching Intent`() {
        assertEquals(Intent.CHAT, LlmIntentClassification.parseLabel("CHAT"))
        assertEquals(Intent.SIMPLE_ACTION, LlmIntentClassification.parseLabel("SIMPLE_ACTION"))
        assertEquals(Intent.MULTI_STEP, LlmIntentClassification.parseLabel("MULTI_STEP"))
        assertEquals(Intent.INFO_QUERY, LlmIntentClassification.parseLabel("INFO_QUERY"))
        assertEquals(Intent.SYSTEM, LlmIntentClassification.parseLabel("SYSTEM"))
    }

    @Test
    fun `lowercase, surrounding whitespace, and trailing punctuation are tolerated`() {
        assertEquals(Intent.CHAT, LlmIntentClassification.parseLabel("  chat.\n"))
        assertEquals(Intent.INFO_QUERY, LlmIntentClassification.parseLabel("\"INFO_QUERY\""))
        assertEquals(Intent.SYSTEM, LlmIntentClassification.parseLabel("system!"))
    }

    @Test
    fun `a label prefixing extra words the model wasn't supposed to add still matches`() {
        assertEquals(Intent.SIMPLE_ACTION, LlmIntentClassification.parseLabel("SIMPLE_ACTION because it opens an app"))
    }

    @Test
    fun `unparseable output returns null rather than guessing`() {
        assertNull(LlmIntentClassification.parseLabel("I'm not sure what category this is"))
        assertNull(LlmIntentClassification.parseLabel(""))
        assertNull(LlmIntentClassification.parseLabel("maybe SIMPLE_ACTION or MULTI_STEP"))
    }
}
