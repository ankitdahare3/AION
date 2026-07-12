package com.aion.host.brain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.brain.AgentState
import com.aion.brain.ApprovalGate
import com.aion.brain.PluginManager
import com.aion.brain.ProviderRouter
import kotlinx.coroutines.launch

/**
 * T-118 — the first way to actually give AION a goal from the app itself, since voice (EPIC 2)
 * doesn't exist yet and no other UI called [AionGraphFactory.create] before this. Runs the exact
 * same real graph [BenchmarkHarnessTest] already exercises — a real side-effect step still shows
 * the real [ApprovalSheetHost] overlay (T-021) and needs a real tap, same as any other run; nothing
 * here bypasses that.
 */
@Composable
fun ChatScreen(
    graphFactory: AionGraphFactory,
    router: ProviderRouter,
    pluginManager: PluginManager,
    approvalGate: ApprovalGate,
    modifier: Modifier = Modifier,
) {
    var goal by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Talk to AION", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("What should AION do?") },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Button(
            onClick = {
                running = true
                response = null
                scope.launch {
                    val graph = graphFactory.create(router, pluginManager, approvalGate)
                    val result = graph.run(AgentState(goal = goal))
                    response = result.response
                    running = false
                }
            },
            enabled = goal.isNotBlank() && !running,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(if (running) "Running…" else "Run")
        }
        response?.let {
            Text(it, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
