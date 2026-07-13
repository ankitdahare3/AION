package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val noopScoreStore =
    object : ScoreStore {
        override fun taskScore(
            id: String,
            t: TaskType,
        ) = 0.5

        override fun latencyNorm(id: String) = 0.5

        override fun notInCooldown(id: String) = true

        override fun recordSuccess(
            id: String,
            t: TaskType,
            latencyMs: Long,
            cost: Double,
        ) {}

        override fun recordFailure(
            id: String,
            t: TaskType,
            e: ProviderFailure,
        ) {}
    }

private val alwaysCanSpend =
    object : BudgetGuard {
        override fun canSpend(req: BrainRequest) = true

        override fun record(cost: Double) {}
    }

/** Returns [responses]\[goal\] verbatim as the model's text output, or [fallback] if the goal is unmapped. */
private fun scriptedProvider(
    responses: Map<String, String>,
    fallback: String = "not-json",
) = object : Provider {
    override val id = "scripted"
    override val tier = Tier.LOCAL
    override val caps = ProviderCaps()

    override suspend fun complete(req: BrainRequest): BrainResult {
        val goal = req.messages.last().content
        val text = responses[goal] ?: fallback
        return BrainResult(text = text, provider = id, latencyMs = 1, costUsd = 0.0)
    }
}

private fun routerFor(provider: Provider) =
    ProviderRouter(registry = listOf(provider), scores = noopScoreStore, budget = alwaysCanSpend)

class PlannerAgentTest {
    // 20 canned goals -> canned valid JSON plans (T-050 AC).
    private val goals =
        listOf(
            "wifi on karo" to
                """[{"action":"tap","target":"Wi-Fi toggle","expected":"Wi-Fi is on","sideEffect":false}]""",
            "open settings" to
                """[{"action":"launchApp","target":"com.android.settings","expected":"Settings home screen","sideEffect":false}]""",
            "mummy ko call karo" to
                """[{"action":"tap","target":"Mom contact","expected":"call in progress","sideEffect":true}]""",
            "screen brightness kam karo" to
                """[{"action":"tap","target":"Display","expected":"Display settings open","sideEffect":false},
                    {"action":"swipe","target":"brightness slider","expected":"brightness reduced","sideEffect":false}]""",
            "battery percentage batao" to
                """[{"action":"tap","target":"Battery","expected":"battery percentage shown","sideEffect":false}]""",
            "bluetooth band karo" to
                """[{"action":"tap","target":"Bluetooth toggle","expected":"Bluetooth is off","sideEffect":false}]""",
            "send a message to Ravi saying running late" to
                """[{"action":"launchApp","target":"Messages","expected":"Messages app open","sideEffect":false},
                    {"action":"tap","target":"Ravi","expected":"chat with Ravi open","sideEffect":false},
                    {"action":"type","target":"message box","expected":"text entered","sideEffect":false},
                    {"action":"tap","target":"Send","expected":"message sent","sideEffect":true}]""",
            "kal ka alarm 7 baje ka lagao" to
                """[{"action":"launchApp","target":"Clock","expected":"Clock app open","sideEffect":false},
                    {"action":"tap","target":"Add alarm","expected":"alarm editor open","sideEffect":false},
                    {"action":"type","target":"time field","expected":"7:00 AM set","sideEffect":true}]""",
            "storage space check karo" to
                """[{"action":"tap","target":"Storage","expected":"storage usage shown","sideEffect":false}]""",
            "wifi ka password dikhao" to
                """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi details open","sideEffect":false},
                    {"action":"tap","target":"Share","expected":"QR/password shown","sideEffect":false}]""",
            "dark mode on karo" to
                """[{"action":"tap","target":"Display","expected":"Display settings open","sideEffect":false},
                    {"action":"tap","target":"Dark theme switch","expected":"dark mode enabled","sideEffect":false}]""",
            "open camera and take a photo" to
                """[{"action":"launchApp","target":"Camera","expected":"Camera app open","sideEffect":false},
                    {"action":"tap","target":"Shutter","expected":"photo captured","sideEffect":true}]""",
            "notifications band kar do" to
                """[{"action":"tap","target":"Notifications","expected":"notification settings open","sideEffect":false}]""",
            "youtube khol ke lofi music dhundo" to
                """[{"action":"launchApp","target":"YouTube","expected":"YouTube app open","sideEffect":false},
                    {"action":"tap","target":"Search","expected":"search box focused","sideEffect":false},
                    {"action":"type","target":"search box","expected":"lofi music typed","sideEffect":false}]""",
            "check kitna data use hua hai is mahine" to
                """[{"action":"tap","target":"Network & internet","expected":"network settings open","sideEffect":false},
                    {"action":"tap","target":"Data usage","expected":"data usage shown","sideEffect":false}]""",
            "airplane mode on karo" to
                """[{"action":"swipe","target":"quick settings","expected":"quick settings open","sideEffect":false},
                    {"action":"tap","target":"Airplane mode","expected":"airplane mode enabled","sideEffect":true}]""",
            "add a new contact named Priya" to
                """[{"action":"launchApp","target":"Contacts","expected":"Contacts app open","sideEffect":false},
                    {"action":"tap","target":"Create new contact","expected":"new contact form open","sideEffect":false},
                    {"action":"type","target":"name field","expected":"Priya entered","sideEffect":true}]""",
            "volume badhao" to
                """[{"action":"tap","target":"volume up key","expected":"volume increased","sideEffect":false}]""",
            "wifi settings mein jao aur network preferences dekho" to
                """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi settings open","sideEffect":false},
                    {"action":"tap","target":"Network preferences","expected":"preferences shown","sideEffect":false}]""",
            "close all recent apps" to
                """[{"action":"globalAction","target":"RECENTS","expected":"recents screen open","sideEffect":false},
                    {"action":"swipe","target":"clear all","expected":"all apps closed","sideEffect":true}]""",
        )

    @Test
    fun `20 canned goals each produce a valid non-empty plan`() =
        runTest {
            val provider = scriptedProvider(goals.toMap())
            val agent = PlannerAgent(routerFor(provider))

            for ((goal, _) in goals) {
                val result = agent.step(AgentState(goal = goal))
                assertTrue("goal \"$goal\" produced no plan: failures=${result.failures}", result.plan.isNotEmpty())
                assertFalse(result.done)
                result.plan.forEach {
                    assertTrue(it.action.isNotBlank())
                    assertTrue(it.target.isNotBlank())
                    assertTrue(it.expected.isNotBlank())
                }
            }
        }

    @Test
    fun `recovers via the repair retry when the first response is malformed`() =
        runTest {
            var callCount = 0
            val provider =
                object : Provider {
                    override val id = "flaky"
                    override val tier = Tier.LOCAL
                    override val caps = ProviderCaps()

                    override suspend fun complete(req: BrainRequest): BrainResult {
                        callCount++
                        val text =
                            if (callCount == 1) {
                                "sure, here's your plan: [broken"
                            } else {
                                """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi on","sideEffect":false}]"""
                            }
                        return BrainResult(text = text, provider = id, latencyMs = 1, costUsd = 0.0)
                    }
                }

            val result = PlannerAgent(routerFor(provider)).step(AgentState(goal = "wifi on karo"))

            assertEquals(2, callCount)
            assertEquals(1, result.plan.size)
            assertFalse(result.done)
        }

    @Test
    fun `gives up honestly after both attempts return invalid JSON`() =
        runTest {
            val provider = scriptedProvider(responses = emptyMap(), fallback = "not json at all")

            val result = PlannerAgent(routerFor(provider)).step(AgentState(goal = "do something impossible"))

            assertTrue(result.done)
            assertTrue(result.plan.isEmpty())
            assertTrue(result.failures.any { it.contains("planner") })
            // A user-facing response must exist even here — AionGraph's frozen run() loop exits the
            // instant done=true is set, so ResponderAgent never gets a turn to fill this in itself.
            assertEquals(
                ResponsePhrasing.forFailure(FailureCause.E4_MODEL_PLAN_ERROR, hinglish = false),
                result.response,
            )
        }

    // Antigravity-audit finding, 2026-07-13 — `callAndParse` used to swallow `router.route`'s
    // exception into a bare `null`, indistinguishable from "the model replied with plain prose."
    // A real routing failure (network/auth/quota) must surface its own message in `s.failures`,
    // not the generic parse-failure text.
    @Test
    fun `a real routing exception surfaces its own message, not a generic parse-failure string`() =
        runTest {
            val throwingProvider =
                object : Provider {
                    override val id = "throwing"
                    override val tier = Tier.LOCAL
                    override val caps = ProviderCaps()

                    override suspend fun complete(req: BrainRequest): BrainResult {
                        throw IllegalStateException("provider quota exhausted")
                    }
                }

            val result = PlannerAgent(routerFor(throwingProvider)).step(AgentState(goal = "do anything"))

            assertTrue(result.done)
            assertTrue(result.failures.any { it.contains("provider quota exhausted") })
            assertFalse(result.failures.any { it.contains("failed to produce a valid JSON plan") })
        }

    // T-121 finding — real models routinely ignore "no markdown fences" anyway.
    @Test
    fun `parses a plan wrapped in markdown code fences despite being told not to`() =
        runTest {
            val fenced =
                "```json\n" +
                    """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi on","sideEffect":false}]""" +
                    "\n```"
            val provider = scriptedProvider(mapOf("wifi on karo" to fenced))

            val result = PlannerAgent(routerFor(provider)).step(AgentState(goal = "wifi on karo"))

            assertEquals(1, result.plan.size)
            assertFalse(result.done)
        }

    @Test
    fun `parses a plan with a stray sentence before and after the array`() =
        runTest {
            val chatty =
                "Sure, here's the plan:\n" +
                    """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi on","sideEffect":false}]""" +
                    "\nLet me know if you need anything else!"
            val provider = scriptedProvider(mapOf("wifi on karo" to chatty))

            val result = PlannerAgent(routerFor(provider)).step(AgentState(goal = "wifi on karo"))

            assertEquals(1, result.plan.size)
            assertFalse(result.done)
        }

    private fun memoryStoreOf(vararg memories: Memory) =
        object : MemoryStore {
            override suspend fun insert(memory: Memory) = 0L

            override suspend fun getAllActive() = memories.toList()

            override suspend fun update(memory: Memory) {}

            override suspend fun softDelete(id: Long) {}
        }

    private fun deviceProfile(
        pkg: String,
        screenText: String = "some screen",
    ) = Memory(
        kind = MemoryKind.PROFILE,
        text = "App $pkg: $screenText",
        confidence = 1.0,
        provenance = DeviceExplorer.PROVENANCE,
        created = 0,
        accessed = 0,
        decayScore = 1.0,
    )

    // T-117 — real installed package names fold into the prompt, so the planner can pick one
    // instead of guessing an AOSP name that may not exist on this device (T-116 finding).
    @Test
    fun `known installed packages from device-profile memories are folded into the system prompt`() =
        runTest {
            var capturedSystem: String? = null
            val provider =
                object : Provider {
                    override val id = "capture"
                    override val tier = Tier.LOCAL
                    override val caps = ProviderCaps()

                    override suspend fun complete(req: BrainRequest): BrainResult {
                        capturedSystem = req.system
                        return BrainResult(
                            text = """[{"action":"tap","target":"x","expected":"y","sideEffect":false}]""",
                            provider = id,
                            latencyMs = 1,
                            costUsd = 0.0,
                        )
                    }
                }
            val store = memoryStoreOf(deviceProfile("com.whatsapp"), deviceProfile("com.sec.android.app.camera"))
            val agent = PlannerAgent(routerFor(provider), memoryStore = store)

            agent.step(AgentState(goal = "whatsapp kholo"))

            assertTrue(capturedSystem!!.contains("com.whatsapp"))
            assertTrue(capturedSystem!!.contains("com.sec.android.app.camera"))
        }

    @Test
    fun `non-PROFILE or differently-sourced memories are never treated as installed packages`() =
        runTest {
            var capturedSystem: String? = null
            val provider =
                object : Provider {
                    override val id = "capture"
                    override val tier = Tier.LOCAL
                    override val caps = ProviderCaps()

                    override suspend fun complete(req: BrainRequest): BrainResult {
                        capturedSystem = req.system
                        return BrainResult(
                            text = """[{"action":"tap","target":"x","expected":"y","sideEffect":false}]""",
                            provider = id,
                            latencyMs = 1,
                            costUsd = 0.0,
                        )
                    }
                }
            val unrelatedFact =
                Memory(
                    kind = MemoryKind.FACT,
                    text = "App com.example.notreal: unrelated",
                    confidence = 1.0,
                    provenance = "some_other_source",
                    created = 0,
                    accessed = 0,
                    decayScore = 1.0,
                )
            val agent = PlannerAgent(routerFor(provider), memoryStore = memoryStoreOf(unrelatedFact))

            agent.step(AgentState(goal = "goal"))

            assertFalse(capturedSystem!!.contains("com.example.notreal"))
        }

    @Test
    fun `no memoryStore at all still plans normally (backwards compatible)`() =
        runTest {
            val provider = scriptedProvider(mapOf("wifi on karo" to goals.first().second))

            val result = PlannerAgent(routerFor(provider)).step(AgentState(goal = "wifi on karo"))

            assertTrue(result.plan.isNotEmpty())
        }

    // T-139 (BACKLOG.md, E1_WRONG_ELEMENT) — real on-screen text grounds tap/longPress targets.
    @Test
    fun `a real screen snapshot is folded into the system prompt`() =
        runTest {
            var capturedSystem: String? = null
            val provider =
                object : Provider {
                    override val id = "capture"
                    override val tier = Tier.LOCAL
                    override val caps = ProviderCaps()

                    override suspend fun complete(req: BrainRequest): BrainResult {
                        capturedSystem = req.system
                        return BrainResult(
                            text = """[{"action":"tap","target":"x","expected":"y","sideEffect":false}]""",
                            provider = id,
                            latencyMs = 1,
                            costUsd = 0.0,
                        )
                    }
                }
            val screenProvider = ScreenSnapshotProvider { "Wi-Fi toggle [off], Airplane mode [off]" }
            val agent = PlannerAgent(routerFor(provider), screenSnapshotProvider = screenProvider)

            agent.step(AgentState(goal = "wifi on karo"))

            assertTrue(capturedSystem!!.contains("Wi-Fi toggle [off], Airplane mode [off]"))
        }

    @Test
    fun `a null or blank screen snapshot leaves the prompt unchanged`() =
        runTest {
            var capturedSystem: String? = null
            val provider =
                object : Provider {
                    override val id = "capture"
                    override val tier = Tier.LOCAL
                    override val caps = ProviderCaps()

                    override suspend fun complete(req: BrainRequest): BrainResult {
                        capturedSystem = req.system
                        return BrainResult(
                            text = """[{"action":"tap","target":"x","expected":"y","sideEffect":false}]""",
                            provider = id,
                            latencyMs = 1,
                            costUsd = 0.0,
                        )
                    }
                }
            val agent = PlannerAgent(routerFor(provider), screenSnapshotProvider = ScreenSnapshotProvider { "   " })

            agent.step(AgentState(goal = "wifi on karo"))

            assertFalse(capturedSystem!!.contains("ACTUALLY visible"))
        }

    @Test
    fun `no screenSnapshotProvider at all still plans normally (backwards compatible)`() =
        runTest {
            val provider = scriptedProvider(mapOf("wifi on karo" to goals.first().second))

            val result = PlannerAgent(routerFor(provider)).step(AgentState(goal = "wifi on karo"))

            assertTrue(result.plan.isNotEmpty())
        }
}
