package com.aion.host

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aion.host.setup.SetupWizardScreen
import dagger.hilt.android.AndroidEntryPoint

/** DOC-020 S1 app skeleton / T-004 — hosts the PR-02 permission setup wizard as the launcher screen. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var resumeTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AionApp(resumeTrigger) }
    }

    override fun onResume() {
        super.onResume()
        resumeTrigger++
    }
}

@Composable
private fun AionApp(resumeSignal: Int) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SetupWizardScreen(resumeSignal)
        }
    }
}
