package com.aion.brain

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-033 AC — 100-utterance labeled set (bilingual Hindi/English/Hinglish per PRD US1, 20 per
 * class), created here since no such set existed. Target: ≥90% accuracy.
 */
class IntentClassifierTest {
    private val labeled: List<Pair<String, Intent>> =
        listOf(
            // CHAT (20)
            "kaise ho" to Intent.CHAT,
            "hello" to Intent.CHAT,
            "hi" to Intent.CHAT,
            "good morning" to Intent.CHAT,
            "thank you" to Intent.CHAT,
            "shukriya" to Intent.CHAT,
            "tum bahut acche ho" to Intent.CHAT,
            "mujhe bore ho raha hai" to Intent.CHAT,
            "I am feeling happy today" to Intent.CHAT,
            "aaj mera mood acha nahi hai" to Intent.CHAT,
            "good night" to Intent.CHAT,
            "chalo baat karte hai" to Intent.CHAT,
            "tum kamaal ho" to Intent.CHAT,
            "ha theek hai" to Intent.CHAT,
            "nahi yaar" to Intent.CHAT,
            "waah bahut badhiya" to Intent.CHAT,
            "I like this" to Intent.CHAT,
            "bahut maza aaya" to Intent.CHAT,
            "you are funny" to Intent.CHAT,
            "chalo koi baat nahi" to Intent.CHAT,
            // INFO_QUERY (20)
            "aaj mausam kaisa hai" to Intent.INFO_QUERY,
            "kal barish hogi kya" to Intent.INFO_QUERY,
            "capital of France kya hai" to Intent.INFO_QUERY,
            "1 kg mein kitne gram hote hai" to Intent.INFO_QUERY,
            "what time is it" to Intent.INFO_QUERY,
            "abhi kitne baje hai" to Intent.INFO_QUERY,
            "who is the prime minister of India" to Intent.INFO_QUERY,
            "Everest ki height kitni hai" to Intent.INFO_QUERY,
            "how many days in a year" to Intent.INFO_QUERY,
            "cricket match ka score kya hai" to Intent.INFO_QUERY,
            "kal Sunday hai kya" to Intent.INFO_QUERY,
            "why is the sky blue" to Intent.INFO_QUERY,
            "what is the meaning of life" to Intent.INFO_QUERY,
            "Delhi se Mumbai ki distance kitni hai" to Intent.INFO_QUERY,
            "how much does an iPhone cost" to Intent.INFO_QUERY,
            "which is the tallest mountain" to Intent.INFO_QUERY,
            "kal ka schedule kya hai" to Intent.INFO_QUERY,
            "battery kitni bachi hai" to Intent.INFO_QUERY,
            "what is my next meeting" to Intent.INFO_QUERY,
            "aaj ki date kya hai" to Intent.INFO_QUERY,
            // SIMPLE_ACTION (20)
            "youtube kholo" to Intent.SIMPLE_ACTION,
            "gaana bajao" to Intent.SIMPLE_ACTION,
            "camera khol do" to Intent.SIMPLE_ACTION,
            "call karo mummy ko" to Intent.SIMPLE_ACTION,
            "message bhejo Rahul ko" to Intent.SIMPLE_ACTION,
            "alarm laga do 7 baje ka" to Intent.SIMPLE_ACTION,
            "volume badha do" to Intent.SIMPLE_ACTION,
            "flashlight on karo" to Intent.SIMPLE_ACTION,
            "WhatsApp khol do" to Intent.SIMPLE_ACTION,
            "photo khincho" to Intent.SIMPLE_ACTION,
            "timer laga do" to Intent.SIMPLE_ACTION,
            "play some music" to Intent.SIMPLE_ACTION,
            "open camera" to Intent.SIMPLE_ACTION,
            "send a message to papa" to Intent.SIMPLE_ACTION,
            "call the office" to Intent.SIMPLE_ACTION,
            "set an alarm for 6am" to Intent.SIMPLE_ACTION,
            "turn on wifi" to Intent.SIMPLE_ACTION,
            "screenshot le lo" to Intent.SIMPLE_ACTION,
            "note likho" to Intent.SIMPLE_ACTION,
            "mute the phone" to Intent.SIMPLE_ACTION,
            // MULTI_STEP (20)
            "youtube kholo aur Arijit Singh bajao" to Intent.MULTI_STEP,
            "kal ki meeting ka time nikal ke HR ko mail karo" to Intent.MULTI_STEP,
            "email padho aur reply karo" to Intent.MULTI_STEP,
            "calendar khol ke naya event add karo" to Intent.MULTI_STEP,
            "camera khol ke photo khincho aur WhatsApp pe bhejo" to Intent.MULTI_STEP,
            "gaana bajao phir volume badha do" to Intent.MULTI_STEP,
            "alarm laga do aur reminder bhi laga do" to Intent.MULTI_STEP,
            "message likho aur send kar do" to Intent.MULTI_STEP,
            "flight book karo aur calendar mein add karo" to Intent.MULTI_STEP,
            "call karo mummy ko aur phir message bhejo papa ko" to Intent.MULTI_STEP,
            "search karo restaurant aur table book karo" to Intent.MULTI_STEP,
            "photo khincho aur Instagram pe post karo" to Intent.MULTI_STEP,
            "email likho aur attachment add karo" to Intent.MULTI_STEP,
            "settings kholo aur wifi on karo" to Intent.MULTI_STEP,
            "note likho aur reminder laga do" to Intent.MULTI_STEP,
            "contact save karo aur message bhejo" to Intent.MULTI_STEP,
            "playlist banao aur gaane add karo" to Intent.MULTI_STEP,
            "location share karo aur call bhi karo" to Intent.MULTI_STEP,
            "form bharo aur submit kar do" to Intent.MULTI_STEP,
            "photo khincho aur save kar do" to Intent.MULTI_STEP,
            // SYSTEM (20)
            "aion stop" to Intent.SYSTEM,
            "AION band ho jao" to Intent.SYSTEM,
            "audit log dikhao" to Intent.SYSTEM,
            "kill switch dabao" to Intent.SYSTEM,
            "AION ki settings kholo" to Intent.SYSTEM,
            "AION mein api key set karo" to Intent.SYSTEM,
            "AION listening band karo" to Intent.SYSTEM,
            "wake word change karo" to Intent.SYSTEM,
            "AION ko mute karo" to Intent.SYSTEM,
            "AION ke permission dikhao" to Intent.SYSTEM,
            "AION ka version kya hai" to Intent.SYSTEM,
            "AION kaunsa model use kar raha hai" to Intent.SYSTEM,
            "aion apni setting badlo" to Intent.SYSTEM,
            "AION band kar do abhi" to Intent.SYSTEM,
            "AION stop listening now" to Intent.SYSTEM,
            "AION show me the audit log" to Intent.SYSTEM,
            "AION change your wake word" to Intent.SYSTEM,
            "AION mute yourself" to Intent.SYSTEM,
            "AION update your api key" to Intent.SYSTEM,
            "AION stop" to Intent.SYSTEM,
        )

    // T-166 — found via a real T-121 benchmark re-run: these exact phrases (real benchmark goals)
    // were all falling through to CHAT because "check karo"/"dikhao"/"batao"/"band karo"/"badhao"
    // had no actionVerbs entry, so ChatAgent fabricated an answer instead of ever attempting the
    // real automation/INFO_QUERY path. Not CHAT is the whole fix — SIMPLE_ACTION vs INFO_QUERY
    // doesn't matter downstream (IntentRoutingAgent only diverts CHAT to ChatAgent).
    @Test
    fun `T-166 - real benchmark phrases that used to fall through to CHAT no longer do`() {
        val shouldNotBeChat =
            listOf(
                "battery percentage batao",
                "wallet balance dikhao",
                "current location check karo",
                "storage space check karo",
                "bluetooth band karo",
                "volume badhao",
                "unread messages check karo",
                "recent transactions dikhao",
            )
        for (utterance in shouldNotBeChat) {
            assertTrue(
                "\"$utterance\" should not classify as CHAT (would divert to a fabricated ChatAgent answer)",
                IntentClassifier.classify(utterance) != Intent.CHAT,
            )
        }
    }

    @Test
    fun `at least 90 percent of the 100-utterance labeled set classifies correctly`() {
        assertTrue("expected exactly 100 labeled utterances, found ${labeled.size}", labeled.size == 100)

        val misclassified = mutableListOf<String>()
        var correct = 0
        for ((utterance, expected) in labeled) {
            val actual = IntentClassifier.classify(utterance)
            if (actual == expected) {
                correct++
            } else {
                misclassified += "\"$utterance\" expected=$expected actual=$actual"
            }
        }

        val accuracy = correct.toDouble() / labeled.size
        assertTrue(
            "accuracy $accuracy ($correct/${labeled.size}) below 90%% target. Misclassified:\n" +
                misclassified.joinToString("\n"),
            accuracy >= 0.90,
        )
    }
}
