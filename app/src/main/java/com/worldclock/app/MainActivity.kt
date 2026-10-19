package com.worldclock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldclock.app.ui.WorldClockRoot
import com.worldclock.app.ui.WorldClockViewModel
import com.worldclock.app.ui.theme.WorldClockTheme

/**
 * Single-activity host. Renders the Compose shell using the app's own
 * monochrome theme.
 *
 * The theme is applied here rather than inside [WorldClockRoot] because it has
 * to wrap the shell: the persisted theme is read from the same view model the
 * shell uses, so the palette is already correct on the first composition of any
 * screen — including the very first frame of a cold start.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: WorldClockViewModel = viewModel(factory = WorldClockViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            WorldClockTheme(theme = state.theme) {
                WorldClockRoot(viewModel = viewModel)
            }
        }
    }
}
