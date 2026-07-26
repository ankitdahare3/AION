package com.aion.host.bubble

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aion.brain.AgentState
import com.aion.brain.ProviderRouter
import com.aion.host.MainActivity
import com.aion.host.automation.AionAccessibilityService
import com.aion.host.brain.AionGraphFactory
import com.aion.host.brain.BuiltInPluginRegistry
import com.aion.host.brain.RealApprovalGate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.abs

/**
 * Gemini-Live-style floating assistant, owner-requested ("jab AION open kare kisi aur app ko, wo
 * background mein chalta rahe aur ek chhota floating bubble dikhaye"): stays running and visible
 * over other apps instead of AION disappearing, draggable, tap opens a compact voice panel to keep
 * talking without returning to the app.
 *
 * Reuses the exact same real conversation pipeline [ChatScreen][com.aion.host.brain.ChatScreen]
 * uses — same [AionGraphFactory]/[ProviderRouter]/[BuiltInPluginRegistry]/[RealApprovalGate],
 * Hilt-injected the same way [com.aion.host.voice.VoiceForegroundService]/
 * [com.aion.host.security.KillSwitchOverlayService] already are — rather than a second, parallel
 * chat implementation.
 *
 * Plain WindowManager Views, not Compose-in-Service: matches the existing overlay precedent in
 * this codebase instead of adding the Compose-outside-an-Activity lifecycle-owner plumbing a
 * Service doesn't get for free.
 *
 * "Screen sharing" reuses [AionAccessibilityService.currentScreenText] — already built for T-051's
 * device-automation grounding, already permitted — instead of MediaProjection. No second, separate
 * consent flow, and on Android the real content that matters (on-screen text/UI structure) is
 * exactly what that already reads; a true pixel/vision capture is a bigger, separate feature if
 * ever needed later, not built here.
 *
 * Approval-gated actions triggered from here have no overlay approval UI yet (only MainActivity's
 * in-app ApprovalSheet exists) — same [GRAPH_TIMEOUT_MS] ceiling [ChatScreen][com.aion.host.brain.ChatScreen]
 * already relies on means a stuck approval times out rather than hanging forever; a real overlay
 * approval UI is a follow-up, not silently pretended to exist.
 */
@AndroidEntryPoint
class AionBubbleService : Service() {
    @Inject
    lateinit var graphFactory: AionGraphFactory

    @Inject
    lateinit var providerRouter: ProviderRouter

    @Inject
    lateinit var pluginRegistry: BuiltInPluginRegistry

    @Inject
    lateinit var realApprovalGate: RealApprovalGate

    private lateinit var windowManager: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var runJob: Job? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false
    private var expanded = false

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelView: View? = null

    private var statusText: TextView? = null
    private var responseText: TextView? = null
    private var screenSharingSwitch: Switch? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this) { }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        if (bubbleView == null && Settings.canDrawOverlays(this)) addBubble()
        isShowing = true
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val channel = NotificationChannel(CHANNEL_ID, "AION Floating Assistant", NotificationManager.IMPORTANCE_MIN)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("AION is running")
            .setContentText("Tap the floating bubble to talk")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    // ---- Bubble ----

    private fun addBubble() {
        val density = resources.displayMetrics.density
        val size = (BUBBLE_SIZE_DP * density).toInt()
        val bubble =
            ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                background = circleDrawable(ACCENT_COLOR)
                setColorFilter(Color.WHITE)
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
            }
        val params =
            WindowManager
                .LayoutParams(
                    size,
                    size,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = resources.displayMetrics.widthPixels - size - (16 * density).toInt()
                    y = resources.displayMetrics.heightPixels / 3
                }
        attachDragAndTap(bubble, params) { toggleExpanded() }
        windowManager.addView(bubble, params)
        bubbleView = bubble
        bubbleParams = params
    }

    private fun attachDragAndTap(
        view: View,
        params: WindowManager.LayoutParams,
        onTap: () -> Unit,
    ) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > CLICK_SLOP_PX || abs(dy) > CLICK_SLOP_PX) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    runCatching { windowManager.updateViewLayout(v, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun circleDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    // ---- Compact voice panel ----

    private fun toggleExpanded() {
        expanded = !expanded
        if (expanded) {
            bubbleView?.let { runCatching { windowManager.removeView(it) } }
            val panel = buildPanel()
            windowManager.addView(panel, panelParams())
            panelView = panel
        } else {
            speechRecognizer?.destroy()
            speechRecognizer = null
            listening = false
            panelView?.let { runCatching { windowManager.removeView(it) } }
            panelView = null
            val bubble = bubbleView
            val params = bubbleParams
            if (bubble != null && params != null) runCatching { windowManager.addView(bubble, params) }
        }
    }

    private fun panelParams(): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        return WindowManager
            .LayoutParams(
                (PANEL_WIDTH_DP * density).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = (16 * density).toInt()
                y = bubbleParams?.y ?: (resources.displayMetrics.heightPixels / 3)
            }
    }

    private fun buildPanel(): View {
        val density = resources.displayMetrics.density

        fun dp(v: Int) = (v * density).toInt()

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(16), dp(20), dp(20))
                background =
                    GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(Color.parseColor("#E6101826"))
                    }
            }

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
        header.addView(
            TextView(this).apply {
                text = "AION"
                setTextColor(Color.WHITE)
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        header.addView(
            TextView(this).apply {
                text = "✕"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 18f
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { toggleExpanded() }
            },
        )
        root.addView(header)

        val shareRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(4))
            }
        shareRow.addView(
            TextView(this).apply {
                text = "See my screen"
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        val shareSwitch = Switch(this)
        screenSharingSwitch = shareSwitch
        shareRow.addView(shareSwitch)
        root.addView(shareRow)

        val status =
            TextView(this).apply {
                text = "Tap the mic and talk"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 12f
                setPadding(0, dp(4), 0, dp(8))
            }
        statusText = status
        root.addView(status)

        val response =
            TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 14f
                text = ""
            }
        responseText = response
        root.addView(
            ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(160))
                addView(response)
            },
        )

        val mic =
            ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                background = circleDrawable(ACCENT_COLOR)
                setColorFilter(Color.WHITE)
                val pad = dp(16)
                setPadding(pad, pad, pad, pad)
                layoutParams =
                    LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        topMargin = dp(12)
                    }
                setOnClickListener { onMicTapped() }
            }
        root.addView(mic)

        return root
    }

    private fun onMicTapped() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            statusText?.text = "Microphone permission not granted"
            return
        }
        listening = true
        statusText?.text = "Listening…"
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    listening = false
                    val heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    recognizer.destroy()
                    speechRecognizer = null
                    if (heard.isNullOrBlank()) {
                        statusText?.text = "Didn't catch that — tap the mic to try again"
                    } else {
                        runGoal(heard)
                    }
                }

                override fun onError(error: Int) {
                    listening = false
                    statusText?.text = "Tap the mic and talk"
                    recognizer.destroy()
                    speechRecognizer = null
                }

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    statusText?.text = "Thinking…"
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?,
                ) {}
            },
        )
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
        recognizer.startListening(intent)
    }

    private fun runGoal(heard: String) {
        responseText?.text = ""
        statusText?.text = "Thinking…"
        runJob?.cancel()
        runJob =
            scope.launch {
                val screenContext =
                    if (screenSharingSwitch?.isChecked == true) {
                        AionAccessibilityService.instance?.currentScreenText()
                    } else {
                        null
                    }
                val goalText =
                    if (!screenContext.isNullOrBlank()) {
                        "$heard\n\n[What's currently on the owner's screen, for context " +
                            "— not something the owner said]\n$screenContext"
                    } else {
                        heard
                    }
                val reply =
                    try {
                        val graph = graphFactory.create(providerRouter, pluginRegistry.manager, realApprovalGate)
                        val result = withTimeoutOrNull(GRAPH_TIMEOUT_MS) { graph.run(AgentState(goal = goalText)) }
                        result?.response ?: "That's taking too long, so I stopped — want to try again?"
                    } catch (e: Exception) {
                        "AION couldn't complete that: ${e.message ?: e.javaClass.simpleName}"
                    }
                statusText?.text = "Tap the mic and talk"
                responseText?.text = reply
                tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
            }
    }

    override fun onDestroy() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        panelView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        panelView = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        runJob?.cancel()
        scope.cancel()
        isShowing = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isShowing = false
            private set

        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "aion_bubble"
        private const val GRAPH_TIMEOUT_MS = 5 * 60 * 1000L
        private const val CLICK_SLOP_PX = 12
        private const val BUBBLE_SIZE_DP = 56
        private const val PANEL_WIDTH_DP = 300
        private val ACCENT_COLOR = Color.parseColor("#2DD4FF") // AION's brand cyan

        private const val PREFS = "aion_bubble"
        private const val KEY_ENABLED = "enabled"

        /** Owner-facing on/off, read by both the Settings toggle and [showIfEnabled]. Defaults on. */
        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

        fun setEnabled(
            context: Context,
            enabled: Boolean,
        ) {
            context
                .getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply()
            if (!enabled) context.stopService(Intent(context, AionBubbleService::class.java))
        }

        /**
         * Owner-requested trigger: "jab AION khud koi app khole to bubble dikhe" — called from
         * [com.aion.host.automation.ActionDispatcher.launchApp] right after it opens another app.
         * No-ops if the owner turned the bubble off, it's already showing, or the overlay
         * permission isn't granted (same graceful-degrade this app's other overlays already use).
         */
        fun showIfEnabled(context: Context) {
            if (isShowing || !isEnabled(context) || !Settings.canDrawOverlays(context)) return
            ContextCompat.startForegroundService(context, Intent(context, AionBubbleService::class.java))
        }
    }
}
