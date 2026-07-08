package com.aion.host

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.ApprovalSheetHost
import com.aion.host.security.AuditLogScreen
import com.aion.host.security.AuditLogger
import com.aion.host.setup.SetupWizardScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** DOC-020 S1 app skeleton / T-004 — hosts the PR-02 permission setup wizard as the launcher screen. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var auditLogger: AuditLogger

    @Inject
    lateinit var approvalGateService: ApprovalGateService

    private var resumeTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AionApp(resumeTrigger, auditLogger, approvalGateService) }
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
) {
    var showAuditLog by remember { mutableStateOf(false) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showAuditLog = !showAuditLog }) {
                            Text(if (showAuditLog) "Back to Setup" else "Audit Log")
                        }
                    }
                    if (showAuditLog) {
                        AuditLogScreen(auditLogger, modifier = Modifier.weight(1f))
                    } else {
                        SetupWizardScreen(resumeSignal, auditLogger, modifier = Modifier.weight(1f))
                    }
                }
                // SR-01/02 — sits above everything else; shows itself only when a side-effect
                // action is actually pending approval (none exist yet, ExecutorAgent is T-051).
                ApprovalSheetHost(approvalGateService)
            }
        }
    }
}
