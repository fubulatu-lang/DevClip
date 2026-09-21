package com.devclip.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.devclip.app.ui.ClipListScreen
import com.devclip.app.ui.DevClipComposeTheme
import com.devclip.app.ui.SetupScreen
import com.devclip.app.ui.SettingsScreen
import com.devclip.app.ui.Snack
import kotlinx.coroutines.launch

/**
 * The launcher app.
 *
 * A ComponentActivity, not a ReactActivity: there is no React instance to
 * host any more, and nothing needs one. The floating windows this activity's
 * service owns used to depend on one existing, which is exactly why they came
 * up empty until the app had been opened once.
 *
 * singleTask in the manifest, because the floating list's "open the full app"
 * button starts this from a service; without it every tap would stack another
 * copy.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before setContent: the window's insets behaviour has to be settled
        // before anything measures against it.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DevClipComposeTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                // Setup is shown once, after install. Whether the
                // permissions are granted does not decide it: a screen the
                // user has already worked through is not worth repeating,
                // and a permission revoked later is surfaced under Status in
                // Settings instead.
                var screen by remember {
                    mutableStateOf(
                        if (hasOnboarded()) Screen.Clips else Screen.Setup
                    )
                }
                var message by remember { mutableStateOf<String?>(null) }

                // The user picks the file each time rather than DevClip
                // holding a standing claim on a folder for something that
                // happens twice a year.
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        message = try {
                            val count = Backup.export(context, uri)
                            resources.getQuantityString(R.plurals.exported, count, count)
                        } catch (e: Exception) {
                            getString(R.string.backup_export_failed)
                        }
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        message = try {
                            val result = Backup.import(context, uri)
                            getString(R.string.backup_imported, result.added, result.skipped)
                        } catch (e: Exception) {
                            e.message ?: getString(R.string.backup_import_failed)
                        }
                    }
                }

                when (screen) {
                    Screen.Setup -> SetupScreen(
                        onDone = {
                            markOnboarded()
                            screen = Screen.Clips
                        }
                    )
                    Screen.Clips -> ClipListScreen(
                        onOpenSettings = { screen = Screen.Settings }
                    )
                    Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.Clips },
                        onExport = { exportLauncher.launch("devclip-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json")) },
                        onClearAll = {
                            scope.launch {
                                ClipRepository.clearAll(context)
                                message = getString(R.string.backup_cleared)
                            }
                        }
                    )
                }

                message?.let { Snack(text = it, onDismiss = { message = null }) }
            }
        }
    }

    /**
     * Two screens, held in state rather than a navigation library.
     *
     * There are two of them and one edge between them. A navigation
     * dependency would buy a back stack this does not have and route parsing
     * it does not need; each screen answers its own back press.
     */
    private fun prefs() = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    private fun hasOnboarded() = prefs().getBoolean(Prefs.KEY_HAS_ONBOARDED, false)

    private fun markOnboarded() =
        prefs().edit().putBoolean(Prefs.KEY_HAS_ONBOARDED, true).apply()

    private enum class Screen { Setup, Clips, Settings }
}
