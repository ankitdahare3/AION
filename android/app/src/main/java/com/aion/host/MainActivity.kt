package com.aion.host

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.aion.host.brain.DeviceExplorationScheduler
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.ApprovalSheetHost
import com.aion.host.security.AuditLogScreen
import com.aion.host.security.AuditLogger
import com.aion.host.security.KillSwitchOverlayService
import com.aion.host.security.SecretVault
import com.aion.host.security.SecretsScreen
import com.aion.host.setup.SetupWizardScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private enum class Screen { SETUP, AUDIT_LOG, API_KEYS }

/** DOC-020 S1 app skeleton / T-004 — hosts the PR-02 permission setup wizard as the launcher screen. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var auditLogger: AuditLogger

    @Inject
    lateinit var approvalGateService: ApprovalGateService

    @Inject
    lateinit var secretVault: SecretVault

    private var resumeTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // T-116 finding — no theme in this project sets windowNoTitle, so the platform's default
        // decor ActionBar renders on top of this fully-Compose UI. Compose content starts at the
        // window's own y=0 with no inset awareness, so anything within the ActionBar's height is
        // silently covered by it — invisible AND untappable. Height varies by OEM skin (barely
        // clipped the button row on stock/emulator, fully hid it on this device's Samsung skin),
        // which is exactly why it never showed up until now. The app never used the title bar for
        // anything, so hiding it outright is the fix, not working around it with inset padding.
        actionBar?.hide()
        setContent { AionApp(resumeTrigger, auditLogger, approvalGateService, secretVault) }
    }

    override fun onResume() {
        super.onResume()
        resumeTrigger++
    }
}

@Composable
private fun AionApp(
    resumeSignal: Int,
    auditLogger: AuditLogger,
    approvalGateService: ApprovalGateService,
    secretVault: SecretVault,
) {
    var screen by remember { mutableStateOf(Screen.SETUP) }
    var overlayRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // T-120 (DOC-017 T4) — block screenshots/screen-recording/recents-thumbnail capture while raw
    // API key values are on screen; cleared again once the user navigates away.
    DisposableEffect(screen) {
        val window = (context as? Activity)?.window
        if (screen == Screen.API_KEYS) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // T-116 finding — 4 TextButtons with no width constraint overflow a real
                    // phone's screen width (font-scale/density-dependent; never showed up on the
                    // emulator's default settings). Without horizontalScroll, Compose's Row squeezes
                    // the last child ("Explore Device") into a tall, narrow multi-line wrap instead
                    // of just clipping — same "4th+ item unreachable" shape as SecretsScreen's T-120
                    // vertical-scroll fix, here on the horizontal axis.
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            overlayRunning = !overlayRunning
                            val intent = Intent(context, KillSwitchOverlayService::class.java)
                            if (overlayRunning) context.startService(intent) else context.stopService(intent)
                        }) {
                            Text(if (overlayRunning) "Hide Kill-Switch" else "Show Kill-Switch")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.API_KEYS) Screen.SETUP else Screen.API_KEYS
                        }) {
                            Text(if (screen == Screen.API_KEYS) "Back to Setup" else "API Keys")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.AUDIT_LOG) Screen.SETUP else Screen.AUDIT_LOG
                        }) {
                            Text(if (screen == Screen.AUDIT_LOG) "Back to Setup" else "Audit Log")
                        }
                        TextButton(onClick = {
                            DeviceExplorationScheduler.triggerNow(context)
                            Toast.makeText(context, "Exploring device…", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Explore Device")
                        }
                    }
                    when (screen) {
                        Screen.AUDIT_LOG -> AuditLogScreen(auditLogger, modifier = Modifier.weight(1f))
                        Screen.API_KEYS -> SecretsScreen(secretVault, auditLogger, modifier = Modifier.weight(1f))
                        Screen.SETUP -> SetupWizardScreen(resumeSignal, auditLogger, modifier = Modifier.weight(1f))
                    }
                }
                // SR-01/02 — sits above everything else; shows itself only when a side-effect
                // action is actually pending approval (none exist yet, ExecutorAgent is T-051).
                ApprovalSheetHost(approvalGateService)
            }
        }
    }
}
