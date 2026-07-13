package com.aion.host.brain

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.brain.AgentState
import com.aion.brain.ApprovalGate
import com.aion.brain.PluginManager
import com.aion.brain.ProviderRouter
import com.aion.brain.ResponsePhrasing
import com.aion.host.security.KillSwitch
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * T-118 — the first way to actually give AION a goal from the app itself, since voice (EPIC 2)
 * doesn't exist yet and no other UI called [AionGraphFactory.create] before this. Runs the exact
 * same real graph [BenchmarkHarnessTest] already exercises — a real side-effect step still shows
 * the real [ApprovalSheetHost] overlay (T-021) and needs a real tap, same as any other run; nothing
 * here bypasses that.
 *
 * T-135 (ADR-011a) — the mic button uses the platform's own speech-recognizer Activity
 * ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH]) rather than a custom [android.speech.SpeechRecognizer]
 * + microphone-level UI: it's a stepping stone toward the real ADR-011 voice stack (openWakeWord +
 * whisper.cpp), not the final answer, so the platform's own "Listening…" dialog is the right amount
 * of effort here. No language is forced via [RecognizerIntent.EXTRA_LANGUAGE] — the device's own
 * configured language (whatever the owner already set it to, hi-IN or en-IN) drives recognition,
 * matching how every other bilingual surface in this app (ResponsePhrasing) detects rather than
 * dictates language. Gracefully degrades (no crash) if no recognizer app is present, same pattern as
 * ShizukuBridge's "not available" path.
 *
 * T-136 (ADR-011a) — replies are read aloud via the platform's own [TextToSpeech], the same
 * stepping-stone scope as T-135's mic button (no custom audio pipeline). Language is picked via
 * [ResponsePhrasing.isHinglish] against the goal that was actually submitted — the same detection
 * this app already uses to decide which of ResponsePhrasing's own strings to show, so speech and
 * text always agree on language. A mute toggle silences it without touching the graph/response at
 * all, matching this screen's own "build the mechanism, don't gate the underlying feature" pattern.
 */
@Composable
fun ChatScreen(
    graphFactory: AionGraphFactory,
    router: ProviderRouter,
    pluginManager: PluginManager,
    approvalGate: ApprovalGate,
    killSwitch: KillSwitch,
    modifier: Modifier = Modifier,
) {
    // Antigravity-audit finding, 2026-07-13: this was `remember`, so rotating the device or
    // toggling dark mode (an Activity recreation either way) wiped the goal/reply mid-conversation.
    // `rememberSaveable` survives both config changes and process death (Bundle-backed), not just
    // in-memory recomposition — the actual bug, not a smaller stand-in for it.
    var goal by rememberSaveable { mutableStateOf("") }
    var submittedGoal by rememberSaveable { mutableStateOf("") }
    var response by rememberSaveable { mutableStateOf<String?>(null) }
    // NOT rememberSaveable: `running=true` would restore across a recreation even though the
    // in-flight coroutine (tied to the OLD rememberCoroutineScope, cancelled on recreation) is
    // genuinely gone — that would strand the UI on "Running…" forever with no way to retry.
    var running by remember { mutableStateOf(false) }
    var muted by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val speechLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val heard =
                result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
            if (!heard.isNullOrBlank()) goal = heard
        }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.shutdown() }
    }
    LaunchedEffect(response, muted) {
        val text = response
        val engine = tts
        if (!muted && text != null && engine != null) {
            engine.language = if (ResponsePhrasing.isHinglish(submittedGoal)) Locale("hi", "IN") else Locale.US
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Talk to AION", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("What should AION do?") },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Row(modifier = Modifier.padding(top = 12.dp)) {
            Button(
                onClick = {
                    submittedGoal = goal
                    running = true
                    response = null
                    scope.launch {
                        try {
                            // Safety-audit finding, 2026-07-13: a prior "aion stop"/overlay trigger
                            // left KillSwitch.halted permanently true (nothing ever called reset()),
                            // which would silently block every automation step of every future run.
                            // Starting a new run is the owner's own signal that it's safe to resume.
                            killSwitch.reset()
                            val graph = graphFactory.create(router, pluginManager, approvalGate)
                            val result = graph.run(AgentState(goal = goal))
                            response = result.response
                        } catch (e: Exception) {
                            response = "AION couldn't complete that: ${e.message ?: e.javaClass.simpleName}"
                        } finally {
                            running = false
                        }
                    }
                },
                enabled = goal.isNotBlank() && !running,
            ) {
                Text(if (running) "Running…" else "Run")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val intent =
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "AION se boliye…")
                        }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        speechLauncher.launch(intent)
                    } else {
                        response = "Voice input isn't available on this device — no speech recognizer app found."
                    }
                },
                enabled = !running,
            ) {
                Text("🎤")
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { muted = !muted }) {
                Text(if (muted) "🔇 Muted" else "🔊 Speak replies")
            }
        }
        response?.let {
            Text(it, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
