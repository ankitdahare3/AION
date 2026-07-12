package com.aion.host.brain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aion.brain.AgentState
import com.aion.brain.ApprovalGate
import com.aion.brain.BENCHMARK_TASKS
import com.aion.brain.BenchmarkCategory
import com.aion.brain.BenchmarkTask
import com.aion.brain.ProviderRouter
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** T-121 — auto-approves every side-effect step. A scripted device run has nobody to tap the real approval sheet; the AC is measuring planning/execution capability, not human patience. */
private class AutoApprovingGate : ApprovalGate {
    override suspend fun await(s: AgentState): AgentState = s
}

@Serializable
data class BenchmarkTaskResult(
    val goal: String,
    val category: String,
    val success: Boolean,
    val response: String?,
    val latencyMs: Long,
    val stepCount: Int,
)

@Serializable
data class BenchmarkReport(
    val totalTasks: Int,
    val successCount: Int,
    val successRate: Double,
    val byCategory: Map<String, Double>,
    val results: List<BenchmarkTaskResult>,
)

/**
 * T-121 (DOC-018 §2, DOC-001 §12) — "≥60% task success on a 50-task internal Hindi+English
 * benchmark ... evaluated honestly." Runs every [BENCHMARK_TASKS] entry through a REAL [AionGraph]
 * (real [ProviderRouter] with whichever provider keys the owner has entered, real
 * `BuiltInPluginRegistry` with whichever plugins are actually enabled) and produces a real JSON
 * report (DOC-018 §2's "scripted device runs, JSON reports").
 *
 * "Evaluated honestly" is the literal AC — not "score ≥60% no matter what." A task counts as a
 * success only if the graph reaches a real `done` state whose response doesn't start with
 * ResponderAgent's own "Something went wrong" failure prefix (the same convention `ResponderAgent`
 * itself uses) — no curve-fitting the success condition to whatever happens to pass.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BenchmarkHarnessTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var router: ProviderRouter

    @Inject
    lateinit var pluginRegistry: BuiltInPluginRegistry

    @Inject
    lateinit var graphFactory: AionGraphFactory

    @Before
    fun setup() {
        hiltRule.inject()
        connectAccessibilityService()
    }

    /**
     * `am instrument` kills the app process the a11y service was bound into — the system marks the
     * service Crashed and never rebinds it into the fresh instrumentation process on its own
     * (confirmed via dumpsys accessibility mid-run). Re-toggling the secure setting from inside
     * the test forces a rebind into THIS process. FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES is
     * load-bearing: plain getUiAutomation() registers itself as an accessibility service and
     * suppresses all others, which would silently undo the very rebind this is performing.
     */
    private fun connectAccessibilityService() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val ui = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val service = "com.aion.host/com.aion.host.automation.AionAccessibilityService"
        shell(ui, "settings put secure enabled_accessibility_services none")
        shell(ui, "settings put secure enabled_accessibility_services $service")
        shell(ui, "settings put secure accessibility_enabled 1")
        val deadline = System.currentTimeMillis() + 20_000
        while (com.aion.host.automation.AionAccessibilityService.instance == null &&
            System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(250)
        }
        android.util.Log.i(
            TAG,
            "a11y service connected: ${com.aion.host.automation.AionAccessibilityService.instance != null}",
        )
    }

    private fun shell(
        ui: android.app.UiAutomation,
        cmd: String,
    ) {
        // The returned fd must be drained/closed for the command to actually complete.
        ui.executeShellCommand(cmd).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).use { it.readBytes() }
        }
    }

    // runBlocking, not runTest — runTest's virtual-time scheduler has a 60s real-time watchdog
    // meant for fast unit tests; 50 sequential real network calls genuinely need real wall-clock
    // minutes, found the hard way when the first attempt hit that watchdog at exactly 60s.
    @Test
    fun runFullBenchmark() =
        runBlocking {
            val results = mutableListOf<BenchmarkTaskResult>()
            for (task in BENCHMARK_TASKS) {
                results += runOne(task)
                android.util.Log.i(TAG, "${task.category} \"${task.goal}\" -> ${results.last().success} (${results.last().response})")
            }

            val successCount = results.count { it.success }
            val byCategory =
                BenchmarkCategory.entries.associate { cat ->
                    val inCat = results.filter { it.category == cat.name }
                    cat.name to if (inCat.isEmpty()) 0.0 else inCat.count { it.success }.toDouble() / inCat.size
                }
            val report =
                BenchmarkReport(
                    totalTasks = results.size,
                    successCount = successCount,
                    successRate = successCount.toDouble() / results.size,
                    byCategory = byCategory,
                    results = results,
                )
            val json = Json { prettyPrint = true }.encodeToString(report)
            android.util.Log.i(TAG, "BENCHMARK_REPORT_JSON_START")
            json.lines().forEach { android.util.Log.i(TAG, it) }
            android.util.Log.i(TAG, "BENCHMARK_REPORT_JSON_END")

            assertTrue("expected at least one task to run", results.isNotEmpty())
        }

    private suspend fun runOne(task: BenchmarkTask): BenchmarkTaskResult {
        val graph = graphFactory.create(router, pluginRegistry.manager, AutoApprovingGate())
        val t0 = System.currentTimeMillis()
        val final =
            try {
                graph.run(AgentState(goal = task.goal))
            } catch (e: Exception) {
                AgentState(
                    goal = task.goal,
                    done = true,
                    response = "Something went wrong: ${e.message}",
                    failures = listOf(e.message ?: "unknown error"),
                )
            }
        val latency = System.currentTimeMillis() - t0
        // Three real bugs found across the first runs: (1) response?.startsWith(...) != true is
        // ALSO true when response is null, counting "planner failed both attempts" as success; (2)
        // any prefix-matching on response strings misses ReflectorAgent's own unrecoverable-abort
        // response ("AION couldn't complete ..."), which run 3 produced 50 times and the check
        // counted as 50 passes; (3) run 4 showed 4/13 "passes" were actually uncaught exceptions
        // (e.g. a11y screenshot-capability errors) — the catch block above built a fresh AgentState
        // with a DEFAULT EMPTY failures list, so `final.failures.isEmpty()` was trivially true for
        // a real crash. Now the catch path records the exception as a failure too. The honest,
        // format-independent check: the run finished, produced a real response, and ended with a
        // clean failures list (T-082's reflector fix guarantees a successfully-recovered run clears
        // its stale failures, so this doesn't punish recovery).
        val success = final.done && final.response != null && final.failures.isEmpty()
        return BenchmarkTaskResult(
            goal = task.goal,
            category = task.category.name,
            success = success,
            response = final.response,
            latencyMs = latency,
            stepCount = final.stepCount,
        )
    }

    private companion object {
        const val TAG = "BenchmarkHarness"
    }
}
