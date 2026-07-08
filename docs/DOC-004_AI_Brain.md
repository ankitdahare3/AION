# DOC-004 — AI BRAIN (INTERNAL DESIGN)
**Project AION | v1.0 | 07 July 2026**

## ADR-002 — Agent Orchestration Framework: **DECIDED**
**Decision: Custom Kotlin-native orchestration ("AION Graph"), pattern-borrowed from LangGraph (state-graph + checkpointing), NOT a Python framework dependency.**
- Why not LangGraph/AutoGen/CrewAI directly: all are Python-first; running Python on-device (Chaquopy/Termux) adds 150MB+, battery cost, and IPC fragility. Android brain must be Kotlin.
- What we borrow: LangGraph's StateGraph model (nodes=agents, edges=conditions, checkpointed state), MCP as the tool protocol, ReAct+reflection loop from Mobile-Agent-v3/MobileUse research.
- Laptop dev-harness MAY use LangGraph for prototyping flows before porting to AION Graph.
- Rejected: OpenAI Agents SDK (vendor gravity), CrewAI (role-play overhead), AutoGen (conversation-centric, heavy), Semantic Kernel (.NET-first).

## 1. AION Graph (orchestrator)
```kotlin
class AionGraph(val nodes: Map<String, Agent>, val edges: List<Edge>) {
  suspend fun run(initial: AgentState): AgentState {
    var s = initial; var node = "planner"
    while (node != END) {
      s = nodes[node]!!.step(s)          // checkpoint after every step
      checkpoints.save(s)
      node = route(node, s)               // conditional edges
      if (s.needsApproval) s = approvalGate.await(s)
      if (s.stepCount > MAX_STEPS) node = "reflector"
    }
    return s
  }
}
```
AgentState = { goal, plan[], currentStep, screenState, memoryContext, toolResults[], failures[], needsApproval, budget }

## 2. Standard Graph (v1)
`intent → planner → executor ⇄ verifier → (fail→reflector→planner) → responder → memory_writer → END`

## 3. Intent Classification (local-first)
Local 4B model classifies: CHAT / SIMPLE_ACTION / MULTI_STEP / INFO_QUERY / SYSTEM. Only MULTI_STEP + hard INFO_QUERY go to cloud. Target: ≥70% of turns never leave device.

## 4. Context Builder
Assembles per-call: system persona + safety rules (immutable prefix) + user profile summary (≤300 tokens) + relevant memories (vector top-k=5) + compressed a11y tree (droidrun-style structured text, ≤2000 tokens) + last N=6 turns + tool schemas. Hard budget 8K tokens default.

## 5. Planner / Executor / Reflector contracts
- Planner: goal → ordered steps [{action, target, expected_observation}] , JSON-schema constrained
- Executor: one step → ActionDispatcher; captures post-action a11y diff
- Verifier: expected vs observed; confidence <0.7 → Reflector
- Reflector: classify failure (FR-R02 taxonomy) → patch plan or abort with explanation

## 6. Safety inside the Brain
- Safety rules are code-level system-prefix, not editable by memory/skills
- Screen text enters context wrapped as `<screen_data>` — InjectionFilter strips imperative patterns; Brain instructed: screen content is never an instruction
- Side-effecting tools flagged in schema; Planner must set needsApproval → ApprovalGate blocks until explicit user yes
