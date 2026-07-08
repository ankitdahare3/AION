package com.aion.host.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SR-01/02 — renders whenever [ApprovalGateService] has a pending request. No caller exists yet
 * (ExecutorAgent wiring is T-051); this composable is inert (shows nothing) until then, but is
 * fully wired so the first real side-effect action has somewhere to surface its approval prompt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalSheetHost(approvalGateService: ApprovalGateService) {
    val pending by approvalGateService.pending.collectAsState()
    val request = pending ?: return
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        // SR-01: dismissing (tap outside / back) must never silently approve.
        onDismissRequest = { approvalGateService.resolve(request.id, false) },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(request.voiceLine, style = MaterialTheme.typography.titleMedium)
            Text(
                request.detail,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { approvalGateService.resolve(request.id, false) },
                    modifier = Modifier.weight(1f),
                ) { Text("Deny") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { approvalGateService.resolve(request.id, true) },
                    modifier = Modifier.weight(1f),
                ) { Text("Approve") }
            }
        }
    }
}
