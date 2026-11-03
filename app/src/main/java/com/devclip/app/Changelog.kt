package com.devclip.app

/**
 * Every release entry ever written, newest first. What "What's new" shows.
 *
 * Entries are never edited after they ship and never deleted: this is the
 * app's release history, and someone who skipped four versions should be
 * able to read all four. If a shipped entry turns out to be wrong, the next
 * entry says so.
 *
 * Written for the person using the app, not for whoever maintains it. Titles
 * are sentences about what is now true; screens and buttons are named the way
 * they are labelled on screen; anything touching stored clips says what
 * happened to them. Technical detail belongs in the README's section for the
 * same version. Two to five items.
 *
 * The newest entry's version must equal `versionName` in app/build.gradle.kts.
 * The build checks it and fails, naming this file, if they disagree — so
 * bumping the version without writing the entry does not ship.
 *
 * An object, so nothing here is built until the dialog first asks for it.
 * Showing the version number in Settings goes through [AppVersion] and never
 * touches this.
 */
object Changelog {

    data class Entry(
        val version: String,
        val title: String,
        val items: List<String>
    )

    val entries: List<Entry> = listOf(
        Entry(
            version = "1.4.0",
            title = "Clips open when you tap them, and notes start with a +",
            items = listOf(
                "Tapping a clip in the app now opens it to read, copy or change, instead of pasting it. Pasting still happens from the floating list, which is where there is somewhere to paste into.",
                "An opened clip has Cancel beside Save, so you can leave without changing anything. Delete is still there, and still offers Undo.",
                "The + at the top of the app, and the + in the floating list, start a new note you type yourself. It is saved alongside your captured clips.",
                "The bubble button at the top of the app brings the bubble back on screen, wherever it was hidden.",
                "This list. It appears once after each update, and any time from the version number at the bottom of Settings.",
            )
        ),
        Entry(
            version = "1.3.1",
            title = "Tidying that you should not notice",
            items = listOf(
                "Code left over from an older version of the app was removed. Nothing about how DevClip behaves has changed, and no clip was touched.",
            )
        ),
        Entry(
            version = "1.3.0",
            title = "Every action within reach, and nothing deleted by accident",
            items = listOf(
                "Deleting a clip offers Undo, and Delete all clips in Settings asks first. It used to happen on a single tap.",
                "TalkBack can now tap the bubble, edit a clip and hear what happened after each action.",
                "On a tablet or an unfolded phone the clip list uses two columns and the margins grow with the screen.",
            )
        ),
    )
}
