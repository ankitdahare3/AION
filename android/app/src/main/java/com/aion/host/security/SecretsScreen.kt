package com.aion.host.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** T-024 — API keys settings screen. Values only ever touch [SecretVault]; never logged (SR-08). */
@Composable
fun SecretsScreen(
    secretVault: SecretVault,
    auditLogger: AuditLogger,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // T-121 prep — the row list has no fixed length (ProviderKey.entries grows over time, T-102
    // already added 2 more rows to what was originally a 3-row screen); without scroll, rows past
    // the viewport are genuinely unreachable, found while entering real keys for the first time.
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("API Keys", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Stored only in Android Keystore-backed encrypted storage on this device.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        ProviderKey.entries.forEach { providerKey ->
            ProviderKeyRow(providerKey, secretVault) {
                scope.launch {
                    auditLogger.record("user", "secretvault.save", """{"provider":"${providerKey.name}"}""")
                }
            }
        }
    }
}

@Composable
private fun ProviderKeyRow(
    providerKey: ProviderKey,
    secretVault: SecretVault,
    onSaved: () -> Unit,
) {
    var value by remember { mutableStateOf(secretVault.get(providerKey) ?: "") }
    var saved by remember { mutableStateOf(secretVault.has(providerKey)) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                saved = false
            },
            label = { Text(providerKey.label) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {
                secretVault.put(providerKey, value)
                saved = true
                onSaved()
            }) { Text("Save") }
            if (saved) {
                Text("Saved", modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
