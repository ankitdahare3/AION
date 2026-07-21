package com.aion.host.translate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import kotlinx.coroutines.launch

private sealed interface TranslateUiState {
    data object Idle : TranslateUiState

    data object Translating : TranslateUiState

    data class Done(
        val result: String,
    ) : TranslateUiState

    data class Failed(
        val message: String,
    ) : TranslateUiState
}

/** T-161 (EPIC 17) — on-device translation (ML Kit, free, no API key/signup). */
@Composable
fun TranslateScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val reader = remember { TranslatorReader() }
    var sourceText by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(SupportedLanguage.ENGLISH) }
    var to by remember { mutableStateOf(SupportedLanguage.HINDI) }
    var state by remember { mutableStateOf<TranslateUiState>(TranslateUiState.Idle) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Translate", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)

        Column(modifier = Modifier.padding(top = 16.dp)) {
            LanguagePicker("From", from, onSelect = { from = it })
            TextButton(onClick = {
                val swap = from
                from = to
                to = swap
            }) { Text("⇄ Swap") }
            LanguagePicker("To", to, onSelect = { to = it })
        }

        OutlinedTextField(
            value = sourceText,
            onValueChange = { sourceText = it },
            label = { Text("Text to translate") },
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        TextButton(
            enabled = sourceText.isNotBlank() && state != TranslateUiState.Translating,
            onClick = {
                state = TranslateUiState.Translating
                scope.launch {
                    state =
                        try {
                            TranslateUiState.Done(reader.translate(sourceText, from, to))
                        } catch (e: Exception) {
                            TranslateUiState.Failed(e.message ?: e.javaClass.simpleName)
                        }
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Translate") }

        when (val s = state) {
            TranslateUiState.Idle -> {}
            TranslateUiState.Translating ->
                Text(
                    "Translating… (first use for this language pair downloads a small offline model)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            is TranslateUiState.Done ->
                GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(
                        s.result,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AionColors.OnBackground,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            is TranslateUiState.Failed ->
                Text(
                    "Couldn't translate right now: ${s.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.Error,
                    modifier = Modifier.padding(top = 16.dp),
                )
        }
    }
}

@Composable
private fun LanguagePicker(
    label: String,
    selected: SupportedLanguage,
    onSelect: (SupportedLanguage) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AionColors.OnSurfaceVariant)
        Row {
            SupportedLanguage.entries.forEach { lang ->
                TextButton(onClick = { onSelect(lang) }) {
                    Text(
                        lang.label,
                        color = if (lang == selected) AionColors.Primary else AionColors.OnSurfaceVariant,
                    )
                }
            }
        }
    }
}
