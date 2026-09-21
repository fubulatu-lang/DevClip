package com.devclip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.devclip.app.ui.ClipListScreen
import com.devclip.app.ui.DevClipComposeTheme
import com.devclip.app.ui.SettingsScreen

/**
 * The launcher app.
 *
 * A ComponentActivity, not a ReactActivity: there is no React instance to
 * host any more, and nothing in the app needs one. The floating windows this
 * activity's service owns used to depend on one existing, which is why they
 * came up empty until the app had been opened once.
 *
 * singleTask in the manifest, because the floating list's "open the full app"
 * button starts this activity from a service. Without it, every tap of that
 * button would stack another copy.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before setContent: the window's insets behaviour has to be settled
        // before anything measures against it.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DevClipComposeTheme {
                var screen by remember { mutableStateOf(Screen.Clips) }

                when (screen) {
                    Screen.Clips -> ClipListScreen(
                        onOpenSettings = { screen = Screen.Settings }
                    )
                    Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.Clips }
                    )
                }
            }
        }
    }

    /**
     * Two screens, held in state rather than a navigation library.
     *
     * There are two of them and one edge between them. A navigation
     * dependency would buy a back stack this does not have and route
     * parsing it does not need; each screen handles its own back press.
     */
    private enum class Screen { Clips, Settings }
}
