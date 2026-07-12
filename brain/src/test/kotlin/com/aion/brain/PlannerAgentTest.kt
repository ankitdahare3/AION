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
}
