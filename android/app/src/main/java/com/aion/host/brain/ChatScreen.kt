package com.aion.host.brain

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aion.brain.AgentState
import com.aion.brain.ApprovalGate
import com.aion.brain.PluginManager
import com.aion.brain.ProgressPhrasing
import com.aion.brain.ProviderRouter
import com.aion.brain.ResponsePhrasing
import com.aion.host.security.KillSwitch
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

// No wall-clock ceiling existed for a run before this — only AionGraph's own 30-step counter,
// which a slow-LLM-latency run could still take many real minutes to hit.
private const val GRAPH_TIMEOUT_MS = 5 * 60 * 1000L

@Composable
fun ChatScreen(
    graphFactory: AionGraphFactory,
    router: ProviderRouter,
    pluginManager: PluginManager,
    approvalGate: ApprovalGate,
    killSwitch: KillSwitch,
    checkpointer: RoomCheckpointer,
    modifier: Modifier = Modifier,
) {
    var goal by rememberSaveable { mutableStateOf("") }
    var submittedGoal by rememberSaveable { mutableStateOf("") }
    var response by rememberSaveable { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var runJob by remember { mutableStateOf<Job?>(null) }
    val liveState by checkpointer.liveState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

    LaunchedEffect(response, submittedGoal, running) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val submitGoal = {
        if (goal.isNotBlank() && !running) {
            submittedGoal = goal
            goal = ""
            running = true
            response = null
            checkpointer.resetLiveState()
            runJob =
                scope.launch {
                    try {
                        killSwitch.reset()
                        val graph = graphFactory.create(router, pluginManager, approvalGate)
                        val result =
                            withTimeoutOrNull(GRAPH_TIMEOUT_MS) { graph.run(AgentState(goal = submittedGoal)) }
                        response = result?.response ?: "That's taking too long, so I stopped — want to try again?"
                    } catch (e: CancellationException) {
                        // A user-initiated cancel (the Cancel button), not the timeout above —
                        // withTimeoutOrNull already swallows its OWN internal cancellation and
                        // returns null instead of throwing, so reaching this catch means someone
                        // outside cancelled runJob directly. Rethrowing is required: swallowing a
                        // CancellationException silently breaks structured-concurrency cleanup.
                        response = "Cancelled."
                        throw e
                    } catch (e: Exception) {
                        response = "AION couldn't complete that: ${e.message ?: e.javaClass.simpleName}"
                    } finally {
                        running = false
                    }
                }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        // Atmospheric gradient
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(AionColors.PrimaryContainer.copy(alpha = 0.15f), Color.Transparent),
                            radius = 1000f,
                        ),
                    ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AionTopBar(
                title = "Talk to AION",
                trailingIcon = Icons.Filled.Sensors,
                onTrailingClick = { muted = !muted },
            )

            // Chat Area
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (submittedGoal.isNotBlank()) {
                    UserBubble(text = submittedGoal)
                }
                if (running) {
                    val hinglish = ResponsePhrasing.isHinglish(submittedGoal)
                    val progressText = liveState?.let { ProgressPhrasing.describe(it, hinglish) } ?: "Thinking..."
                    AIBubble(text = progressText)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CancelRunChip(onClick = { runJob?.cancel() })
                    }
                } else if (response != null) {
                    // Split response into simple actions vs text (mock parsing for visuals)
                    // We'll just display it as an AI Bubble for now.
                    AIBubble(text = response!!)
                }

                Spacer(Modifier.height(32.dp))
            }

            // Input Area
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassPanel(
                    modifier = Modifier.weight(1f).height(56.dp),
                    cornerRadius = 28.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = AionColors.OnSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = goal,
                            onValueChange = { goal = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AionColors.OnSurface),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitGoal() }),
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            decorationBox = { innerTextField ->
                                if (goal.isEmpty()) {
                                    Text(
                                        "Type a goal...",
                                        color = AionColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                innerTextField()
                            },
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            Icons.Filled.Keyboard,
                            contentDescription = null,
                            tint = if (goal.isNotBlank()) AionColors.Primary else AionColors.OnSurfaceVariant,
                            modifier = Modifier.clickable { submitGoal() },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                MicButton(
                    onClick = {
                        val intent =
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "AION se boliye…")
                            }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            speechLauncher.launch(intent)
                        } else {
                            response = "Voice input isn't available on this device — no speech recognizer app found."
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.8f)
                    .background(
                        color = AionColors.PrimaryContainer.copy(alpha = 0.2f),
                        shape =
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 0.dp,
                            ),
                    ).border(
                        width = 1.dp,
                        color = AionColors.PrimaryContainer.copy(alpha = 0.4f),
                        shape =
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 0.dp,
                            ),
                    ).padding(20.dp),
        ) {
            Text(text = text, color = AionColors.OnBackground, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AIBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .background(
                        color = AionColors.SurfaceVariant.copy(alpha = 0.6f),
                        shape =
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 16.dp,
                            ),
                    ).border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape =
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 16.dp,
                            ),
                    ).padding(20.dp),
        ) {
            Text(text = text, color = AionColors.OnSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Wired to `runJob?.cancel()` — the only way to stop a run before this was force-quitting the app. */
@Composable
private fun CancelRunChip(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(AionColors.SurfaceVariant.copy(alpha = 0.6f))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            tint = AionColors.OnSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Cancel", color = AionColors.OnSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BlinkingLed() {
    val transition = rememberInfiniteTransition(label = "led")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "ledAlpha",
    )
    Box(
        modifier =
            Modifier
                .size(8.dp)
                .shadow(4.dp, CircleShape, ambientColor = Color(0xFF4ADE80), spotColor = Color(0xFF4ADE80))
                .background(Color(0xFF4ADE80).copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun MicButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "micPulse")
    val shadowRadius by transition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "micShadow",
    )

    Box(
        modifier =
            Modifier
                .size(56.dp)
                .shadow(shadowRadius.dp, CircleShape, ambientColor = AionColors.Glow, spotColor = AionColors.Glow)
                .background(AionColors.PrimaryContainer, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = AionColors.OnPrimaryContainer,
            modifier = Modifier.size(28.dp),
        )
    }
}
