package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fakePlugin(
    id: String = "com.aion.plugin.test",
    tools: List<ToolSchema>,
    onExecute: (ToolCall) -> ToolResult = { ToolResult(success = true, resultJson = "{}") },
) = object : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = id,
            name = "Test",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            tools = tools,
            dna = DnaConfig(),
        )

    override suspend fun execute(call: ToolCall): ToolResult = onExecute(call)
}

class PluginManagerTest {
    @Test
    fun `registering a valid plugin passes DNA validation`() {
        val manager = PluginManager(PluginApprovalGate { _, _ -> true })
        val plugin =
            fakePlugin(
                tools =
                    listOf(
                        ToolSchema(
                            "read_screen",
                            sideEffect = false,
                            inputSchema = "{}",
                            description = "reads the screen",
                        ),
                    ),
            )

        val result = manager.register(plugin)

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `an invalid plugin is rejected and never routable`() =
        runTest {
            val manager = PluginManager(PluginApprovalGate { _, _ -> true })
            val plugin = fakePlugin(tools = emptyList()) // gate 1 failure: no tools

            val result = manager.register(plugin)
            manager.enable(plugin.manifest.id)
            val routed = manager.route(plugin.manifest.id, ToolCall("anything", "{}", false))

            assertTrue(result is ValidationResult.Invalid)
            assertFalse(manager.enable(plugin.manifest.id))
            assertTrue(routed is PluginRouteResult.Rejected)
        }

    @Test
    fun `a non-side-effect tool executes without asking for approval`() =
        runTest {
            var approvalCalls = 0
            val manager =
                PluginManager(
                    PluginApprovalGate { _, _ ->
                        approvalCalls++
                        true
                    },
                )
            val plugin =
                fakePlugin(
                    tools =
                        listOf(
                            ToolSchema("read_screen", sideEffect = false, inputSchema = "{}", description = "reads"),
                        ),
                    onExecute = { ToolResult(success = true, resultJson = """{"text":"hi"}""") },
                )
            manager.register(plugin)
            manager.enable(plugin.manifest.id)

            val result = manager.route(plugin.manifest.id, ToolCall("read_screen", "{}", false))

            require(result is PluginRouteResult.Success)
            assertEquals("""{"text":"hi"}""", result.result.resultJson)
            assertEquals(0, approvalCalls)
        }

    @Test
    fun `a side-effect tool requires approval before executing`() =
        runTest {
            var executed = false
            val manager = PluginManager(PluginApprovalGate { _, _ -> true })
            val plugin =
                fakePlugin(
                    tools =
                        listOf(
                            ToolSchema("send_email", sideEffect = true, inputSchema = "{}", description = "sends"),
                        ),
                    onExecute = {
                        executed = true
                        ToolResult(success = true, resultJson = "{}")
                    },
                )
            manager.register(plugin)
            manager.enable(plugin.manifest.id)

            val result = manager.route(plugin.manifest.id, ToolCall("send_email", "{}", true))

            assertTrue(result is PluginRouteResult.Success)
            assertTrue(executed)
        }

    @Test
    fun `denied approval blocks execution of a side-effect tool`() =
        runTest {
            var executed = false
            val manager = PluginManager(PluginApprovalGate { _, _ -> false })
            val plugin =
                fakePlugin(
                    tools =
                        listOf(
                            ToolSchema("send_email", sideEffect = true, inputSchema = "{}", description = "sends"),
                        ),
                    onExecute = {
                        executed = true
                        ToolResult(success = true, resultJson = "{}")
                    },
                )
            manager.register(plugin)
            manager.enable(plugin.manifest.id)

            val result = manager.route(plugin.manifest.id, ToolCall("send_email", "{}", true))

            require(result is PluginRouteResult.Rejected)
            assertTrue(result.reason.contains("denied"))
            assertFalse(executed)
        }

    @Test
    fun `routing to a disabled plugin is rejected`() =
        runTest {
            val manager = PluginManager(PluginApprovalGate { _, _ -> true })
            val plugin =
                fakePlugin(
                    tools =
                        listOf(
                            ToolSchema("read_screen", sideEffect = false, inputSchema = "{}", description = "reads"),
                        ),
                )
            manager.register(plugin) // never enabled

            val result = manager.route(plugin.manifest.id, ToolCall("read_screen", "{}", false))

            assertTrue(result is PluginRouteResult.Rejected)
        }

    @Test
    fun `routing an unknown tool name on a valid plugin is rejected`() =
        runTest {
            val manager = PluginManager(PluginApprovalGate { _, _ -> true })
            val plugin =
                fakePlugin(
                    tools =
                        listOf(
                            ToolSchema("read_screen", sideEffect = false, inputSchema = "{}", description = "reads"),
                        ),
                )
            manager.register(plugin)
            manager.enable(plugin.manifest.id)

            val result = manager.route(plugin.manifest.id, ToolCall("delete_everything", "{}", true))

            require(result is PluginRouteResult.Rejected)
            assertTrue(result.reason.contains("delete_everything"))
        }
}
