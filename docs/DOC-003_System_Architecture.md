# DOC-003 — SYSTEM ARCHITECTURE
**Project AION | v1.0 | 07 July 2026**

## 1. Layered Architecture
```
┌─────────────────────────────────────────────────┐
│ L6 INTERACTION   Voice UI · Chat UI · Overlay · Approval Prompts │
├─────────────────────────────────────────────────┤
│ L5 AGENTS        Planner · Executor · Reflector · Meta Agents    │
├─────────────────────────────────────────────────┤
│ L4 BRAIN         Orchestrator · Provider Router · Context Builder│
├─────────────────────────────────────────────────┤
│ L3 CAPABILITIES  Plugins · Skills · Tools (MCP) · Automation     │
├─────────────────────────────────────────────────┤
│ L2 CORE SERVICES Memory · Vision · Voice · Security · Telemetry  │
├─────────────────────────────────────────────────┤
│ L1 PLATFORM      Accessibility · Shizuku · DeviceOwner · FGS ·   │
│                  Local LLM Runtime · Room/sqlite-vec · Keystore  │
└─────────────────────────────────────────────────┘
```
Rule: layers call downward only. Cross-cutting: EventBus + Telemetry.

## 2. Component Inventory (top-level, 40 core → decomposes to 200+)
**Brain (8):** Orchestrator, IntentClassifier, ContextBuilder, ProviderRouter, ProviderRegistry, CostTracker, BrainLearning, ResponseParser
**Agents (7):** PlannerAgent, ExecutorAgent, ReflectorAgent, MemoryAgent, VisionAgent, ArchitectAgent, QAAgent
**Voice (6):** WakeWordService, STTEngine, TTSEngine, VoiceSessionManager, BargeInController, LanguageDetector
**Automation (7):** A11yTreeReader, ActionDispatcher, GestureController, ScreenCapture, ElementResolver, StepVerifier, AppFunctionsClient
**Memory (6):** ShortTermStore, LongTermStore, VectorIndex, EpisodicStore, SkillStore, MemoryConsolidator
**Plugins (5):** PluginManager, PluginSandbox, SkillEngine, SkillGenerator, DNAValidator
**Platform (6):** DeviceOwnerManager, ShizukuBridge, PermissionManager, ForegroundServiceHost, NotificationListener, BootReceiver
**Security (5):** ApprovalGate, AuditLogger, InjectionFilter, SecretVault, SensitiveAppGuard

## 3. Primary Data Flow (voice command → action)
```
Mic → WakeWord → STT → IntentClassifier
 ├─ simple → local intent handler → TTS
 └─ complex → ContextBuilder(memory+screen) → ProviderRouter → LLM
      → PlannerAgent(plan) → [ApprovalGate if side-effect]
      → ExecutorAgent → ActionDispatcher → A11y/API
      → StepVerifier → (fail: ReflectorAgent → replan ≤3)
      → ReflectorAgent(post-task) → Memory write → TTS response
```

## 4. Process Model
- Single app, multi-process: `:core` (brain+services), `:automation` (a11y), `:inference` (local LLM, killable under memory pressure)
- All long-running work in typed foreground services
- EventBus: Kotlin SharedFlow; cross-process via AIDL + Messenger

## 5. Key Interfaces (contracts frozen here)
```kotlin
interface Provider { suspend fun complete(req: BrainRequest): BrainResult
                     val caps: ProviderCaps; val id: String }
interface Plugin   { val manifest: PluginManifest
                     suspend fun execute(call: ToolCall): ToolResult }
interface Agent    { suspend fun step(state: AgentState): AgentState }
interface MemoryStore { suspend fun write(m: Memory); 
                        suspend fun search(q: Query): List<Memory> }
```

## 6. Deployment
Monorepo (see DOC-020). Android app (Kotlin+Compose) + laptop dev-harness (Python: benchmarks, skill simulation, log analysis) + optional LAN inference server (Ollama).
