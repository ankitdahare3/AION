# DOC-005 — PLUGIN SDK
**Project AION | v1.0 | 07 July 2026**

## 1. Model
Every capability = plugin. Plugins declare tools (MCP-compatible schemas). Brain sees only tool schemas; PluginManager routes ToolCalls.

## 2. Manifest (aion-plugin.json)
```json
{ "id":"com.aion.plugin.gmail", "name":"Gmail", "version":"1.2.0",
  "apiLevel":1, "permissions":["INTERNET","READ_MAIL_SCOPE"],
  "tools":[{"name":"send_email","sideEffect":true,
            "inputSchema":{...},"description":"..."}],
  "dna":{"learn":true,"reflect":true,"benchmark":"bench/gmail.yaml","update":true} }
```

## 3. Kotlin API
```kotlin
abstract class AionPlugin {
  abstract val manifest: PluginManifest
  open suspend fun onInstall(ctx: PluginContext) {}
  abstract suspend fun execute(call: ToolCall): ToolResult
  open suspend fun benchmark(runner: BenchRunner): BenchReport
  open suspend fun reflect(outcome: TaskOutcome): PluginPatch?
}
```
PluginContext exposes: scoped storage, EventBus, memory (namespaced), automation API (rate-limited), NO direct network unless permission.

## 4. Lifecycle & Sandbox
discover → DNAValidator (all 4 gates pass?) → QAAgent test suite → user approval → enable.
Sandbox: separate classloader; permission-scoped facade; CPU/mem quotas; every ToolCall audited. sideEffect:true tools ALWAYS hit ApprovalGate.

## 5. v1 Built-in Plugins (12)
System, Contacts, Phone/SMS, Calendar, Clock/Alarms, Files, Camera, Gallery, Browser, Gmail(API), Telegram(API), UIAutomation(generic fallback).

## 6. Versioning
SemVer; apiLevel gates SDK compatibility; auto-rollback on crash-loop (3 crashes/24h).
