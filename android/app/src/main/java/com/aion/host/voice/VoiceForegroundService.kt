package com.aion.host.voice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aion.host.MainActivity
import com.aion.host.R
import com.aion.host.security.AuditLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * T-010 (DOC-011 §2/§4) — the always-on FGS wake-word detection is meant to run inside. Genuinely
 * opens and continuously reads the microphone (16kHz mono PCM, the standard input format both
 * openWakeWord and Silero VAD expect) rather than just declaring the service — Android's own FGS
 * policy expects a `microphone`-typed service to actually be using the microphone, and a fake
 * declaration would be dishonest. There's no wake-word model to feed yet (T-011, not built): each
 * buffer is read and discarded, marked below with the exact hookup point T-011 needs.
 *
 * AudioFocus is requested as `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` per DOC-011 §4 — this service
 * only ever listens for now (T-014's TTS/barge-in ducking doesn't exist yet), but requesting the
 * weakest focus type that's still correct today means nothing needs to change here once TTS lands.
 *
 * The privacy overlay is DOC-011 §4's own trust requirement ("privacy LED overlay indicator
 * whenever mic hot") — a project-controlled signal alongside whatever the OS itself shows, same
 * "build a visible, undeniable indicator" reasoning as [com.aion.host.security.KillSwitchOverlayService].
 * Gracefully no-ops (no crash) if either RECORD_AUDIO or the overlay permission isn't granted,
 * matching this project's established degrade pattern (ShizukuBridge, AppLockGate).
 *
 * AC ("service survives 24h emulator soak") is a real-time endurance test this session can't
 * literally run — verified instead that the service starts, holds foreground state, and doesn't
 * crash across a real (if much shorter) live check; the 24h soak itself is left for T-122 or the
 * owner's own device.
 */
@AndroidEntryPoint
class VoiceForegroundService : Service() {
    @Inject
    lateinit var auditLogger: AuditLogger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var focusRequest: AudioFocusRequest? = null
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        auditAsync("voicefgs.start")
        requestAudioFocus()
        showPrivacyOverlay()
        startCapture()
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val channel =
            NotificationChannel(CHANNEL_ID, getString(R.string.voice_notification_channel), NotificationManager.IMPORTANCE_LOW)
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
            .setContentTitle(getString(R.string.voice_notification_title))
            .setContentText(getString(R.string.voice_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonAudioFocus() {
        focusRequest?.let {
            (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
        }
        focusRequest = null
    }

    private fun showPrivacyOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        val dot =
            View(this).apply {
                setBackgroundColor(Color.RED)
            }
        val size = (8 * resources.displayMetrics.density).toInt()
        val params =
            WindowManager
                .LayoutParams(
                    size,
                    size,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 16
                    y = 16
                }
        windowManager.addView(dot, params)
        overlayView = dot
    }

    private fun hidePrivacyOverlay() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    private fun startCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBufferSize <= 0) return
        val record =
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2,
            )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }
        audioRecord = record
        record.startRecording()
        captureJob =
            scope.launch {
                val buffer = ShortArray(minBufferSize)
                // T-011 hookup point: openWakeWord/Silero VAD consume this exact 16kHz mono PCM
                // stream instead of discarding it. Reading continuously (not just opening the
                // stream) is what actually proves the mic stays genuinely held for the AC's soak
                // test, not just that startRecording() was called once.
                while (isActive) {
                    record.read(buffer, 0, buffer.size)
                }
            }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    override fun onDestroy() {
        stopCapture()
        hidePrivacyOverlay()
        abandonAudioFocus()
        // Found live: `scope.launch { ... }` immediately followed by `scope.cancel()` raced and
        // cancelled the write before Room's suspend insert ever completed, so "voicefgs.stop" never
        // actually reached the audit log — runBlocking here genuinely waits for it first.
        runBlocking {
            try {
                auditLogger.record("user", "voicefgs.stop", "{}")
            } catch (e: Exception) {
                Log.e(TAG, "audit write failed for voicefgs.stop", e)
            }
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun auditAsync(action: String) {
        scope.launch {
            try {
                auditLogger.record("user", action, "{}")
            } catch (e: Exception) {
                Log.e(TAG, "audit write failed for $action", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val TAG = "VoiceForegroundService"
        const val CHANNEL_ID = "aion_voice"
        const val NOTIFICATION_ID = 1001
        const val SAMPLE_RATE_HZ = 16000
    }
}
