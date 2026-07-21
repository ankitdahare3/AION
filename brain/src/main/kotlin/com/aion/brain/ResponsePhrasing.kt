package com.aion.brain

/**
 * Turns a classified [FailureCause] (or a plain success) into a warm, plain-language reply — never
 * the raw error string a cause was classified FROM. [ReflectorAgent] and [ResponderAgent] are both
 * genuine authors of the final user-facing text (see [ReflectorAgent]'s own doc comment for why:
 * AionGraph's frozen `run()` loop exits the instant `done=true` is set, so most unrecoverable aborts
 * are phrased by ReflectorAgent directly and never reach ResponderAgent at all) — this is the single
 * shared phrasing table both call into, so neither site ever emits a raw cause code, package name,
 * or classifier string directly to the person on the other end.
 *
 * Language style is picked via a small keyword list drawn from this project's own real benchmark
 * goals (`BenchmarkTasks.kt`) — the same lightweight, no-real-NLP-model approach `TextSimilarity`
 * already uses elsewhere in this codebase; genuine language detection isn't built or claimed.
 */
object ResponsePhrasing {
    private val HINGLISH_TOKENS =
        setOf(
            "karo",
            "kar",
            "karein",
            "kijiye",
            "khol",
            "kholo",
            "batao",
            "dikhao",
            "dekho",
            "jao",
            "mein",
            "ka",
            "ke",
            "ki",
            "badhao",
            "dhundo",
            "se",
            "lo",
            "aaj",
            "kal",
            "band",
            "hai",
            "nahi",
            "abhi",
            "bhejo",
            "bhej",
            "wala",
            "kya",
            "pe",
        )

    /** True if [goal] itself was phrased with any Hindi/Hinglish token — the reply matches back in kind. */
    fun isHinglish(goal: String): Boolean = goal.lowercase().split(Regex("[^a-z]+")).any { it in HINGLISH_TOKENS }

    fun forSuccess(hinglish: Boolean): String = if (hinglish) SUCCESS_HI else SUCCESS_EN

    fun forFailure(
        cause: FailureCause,
        hinglish: Boolean,
    ): String = (if (hinglish) FAILURE_HI else FAILURE_EN).getValue(cause)

    /**
     * A denied approval isn't a diagnosable [FailureCause] (E1-E6) — the user made a deliberate
     * choice, not something that went wrong — so it gets its own small phrasing pair rather than
     * an artificial UNKNOWN/E-code. Same "whoever sets done=true must also phrase the response"
     * rule this file's own class doc describes for ReflectorAgent applies to RealApprovalGate.
     */
    fun forDenial(hinglish: Boolean): String = if (hinglish) DENIAL_HI else DENIAL_EN

    private const val SUCCESS_EN = "Done! That's taken care of."
    private const val SUCCESS_HI = "Ho gaya! Kaam ho gaya."

    private const val DENIAL_EN = "Okay, I didn't do that — you said no when I asked to confirm."
    private const val DENIAL_HI = "Theek hai, maine wo nahi kiya — aapne confirm karne se mana kar diya."

    private val FAILURE_EN =
        mapOf(
            FailureCause.E1_WRONG_ELEMENT to
                "Hmm, I couldn't find the right thing to tap on-screen for that. Want to try again?",
            FailureCause.E2_UI_CHANGED to
                "The screen changed while I was working on that, so I couldn't finish it. Let's try once more?",
            FailureCause.E3_VISION_MISREAD to
                "I had trouble reading the screen properly that time. Mind trying again?",
            FailureCause.E4_MODEL_PLAN_ERROR to
                "I got a bit confused figuring out how to do that. Could you say it a little differently?",
            FailureCause.E5_PERMISSION_BLOCKED to
                "I can't do that yet — looks like that app isn't set up or installed on this device.",
            FailureCause.E6_TIMING_RACE to
                "That didn't quite go through in time. Let's give it another go.",
            FailureCause.UNKNOWN to
                "Sorry, that didn't work out and I'm not fully sure why yet. Want to try again?",
        )

    private val FAILURE_HI =
        mapOf(
            FailureCause.E1_WRONG_ELEMENT to
                "Screen par sahi cheez nahi mil paayi tap karne ke liye. Ek baar phir try karein?",
            FailureCause.E2_UI_CHANGED to
                "Kaam karte waqt screen change ho gayi, isliye poora nahi ho paaya. Dobara try karte hain?",
            FailureCause.E3_VISION_MISREAD to
                "Is baar screen sahi se padh nahi paaya. Ek baar phir try kijiye.",
            FailureCause.E4_MODEL_PLAN_ERROR to
                "Ye samajhne mein thodi confusion ho gayi ki kaise karna hai. Thoda alag tarike se bol sakte hain?",
            FailureCause.E5_PERMISSION_BLOCKED to
                "Ye abhi nahi ho sakta — lagta hai ye app is device par set up nahi hai ya install nahi hai.",
            FailureCause.E6_TIMING_RACE to
                "Ye time par complete nahi hua. Ek baar aur try karte hain.",
            FailureCause.UNKNOWN to
                "Maaf kijiye, ye nahi ho paaya aur mujhe abhi poora pata nahi kyun. Dobara try karein?",
        )
}
