package com.devclip.app

import android.content.Context

/**
 * Decides whether "What's new" opens by itself: once per version, after an
 * update.
 *
 * Keyed by install, not by person, and that is a decision rather than a
 * shortcut. DevClip has no accounts — the clips themselves are per install —
 * so the person who has not seen the release notes is the person holding
 * this phone.
 *
 * Compared by equality, not ordering: the stored value is the version that
 * was seen, and anything else means show it. No version parsing, and a
 * rollback to an older build still behaves.
 *
 * Storage failures are swallowed both ways. Failing to read means the dialog
 * shows; failing to write means it shows again next launch. Neither is worth
 * taking the app down over.
 *
 * This governs only the automatic popup. The version link in Settings always
 * opens the dialog.
 */
object ChangelogGate {

    private fun prefs(context: Context) =
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    fun shouldShow(context: Context): Boolean {
        val current = AppVersion.name(context) ?: return false
        val seen = try {
            prefs(context).getString(Prefs.KEY_CHANGELOG_SEEN, null)
        } catch (e: Exception) {
            null
        }
        return seen != current
    }

    /**
     * Records the running version as seen.
     *
     * Also called when setup finishes. Someone who has just installed DevClip
     * did not update from anything; showing them a list of changes to an app
     * they have never used would be noise on their first minute with it.
     */
    fun markSeen(context: Context) {
        val current = AppVersion.name(context) ?: return
        try {
            prefs(context).edit().putString(Prefs.KEY_CHANGELOG_SEEN, current).apply()
        } catch (e: Exception) {
            // Best effort: it shows once more next launch.
        }
    }
}
