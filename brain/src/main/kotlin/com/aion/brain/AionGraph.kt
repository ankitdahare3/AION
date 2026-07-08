package com.aion.brain

/** DOC-004 AION Graph — checkpointed state-graph orchestrator. */
data class PlanStep(
    val action: String,
    val target: String,
    val expected: String,
    val sideEffect: Boolean,
)

data class AgentState(
    val goal: String,
    val plan: List<PlanStep> = emptyList(),
    val currentStep: Int = 0,
    val toolResults: List<String> = emptyList(),
    val failures: List<String> = emptyList(),
    val needsApproval: Boolean = false,
    val stepCount: Int = 0,
    val done: Boolean = false,
    val response: String? = null,
)

fun interface Agent {
    suspend fun step(s: AgentState): AgentState
}

fun interface ApprovalGate {
    suspend fun await(s: AgentState): AgentState
}

fun interface Checkpointer {
    fun save(s: AgentState)
}

class AionGraph(
    private val nodes: Map<String, Agent>,
    private val route: (String, AgentState) -> String,
    private val approval: ApprovalGate,
    private val checkpoints: Checkpointer,
    private val maxSteps: Int = 30,
) {
    init {
        require(nodes.containsKey("planner")) { "AionGraph requires a 'planner' node (run() starts there)" }
        require(nodes.containsKey("reflector")) {
            "AionGraph requires a 'reflector' node (run() routes there when maxSteps is exceeded)"
        }
    }

    suspend fun run(initial: AgentState): AgentState {
        var s = initial
        var node = "planner"
        while (node != END && !s.done) {
            s = nodes.getValue(node).step(s).copy(stepCount = s.stepCount + 1)
            checkpoints.save(s)
            if (s.needsApproval) s = approval.await(s).copy(needsApproval = false)
            node = if (s.stepCount > maxSteps) "reflector" else route(node, s)
        }
        return s
    }

    companion object {
        const val END = "__end__"
    }
}
