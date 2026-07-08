package com.aion.host.security

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SR-03, DOC-016 §5 — floating always-on-top STOP button. Requires the OVERLAY permission
 * (T-004's SetupWizardScreen); silently does nothing if it isn't granted rather than crashing.
 */
@AndroidEntryPoint
class KillSwitchOverlayService : Service() {
    @Inject
    lateinit var killSwitch: KillSwitch

    private var overlayView: Button? = null
    private lateinit var windowManager: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (overlayView == null && Settings.canDrawOverlays(this)) {
            addOverlay()
        }
        return START_STICKY
    }

    private fun addOverlay() {
        val button =
            Button(this).apply {
                text = "STOP"
                setBackgroundColor(Color.RED)
                setTextColor(Color.WHITE)
                setOnClickListener { scope.launch { killSwitch.trigger("overlay") } }
            }
        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    x = 16
                    y = 100
                }
        windowManager.addView(button, params)
        overlayView = button
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
