package com.devclip.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.devclip.app.DevClipTheme

/**
 * DevClip's symbols, drawn as line work on One UI's 24dp grid.
 *
 * Not Material's filled icons. One UI symbols are outlined strokes of one
 * weight, and the filled set read as a different system sitting on top of
 * this one — the pencil on every row was the loudest thing on the screen.
 * Drawing them here also keeps the stroke on the same token the floating
 * list's glyphs use, so the bubble's window and the app agree.
 *
 * The extended Material icon set would have covered most of these, at the
 * cost of several megabytes of icons the app never draws: shrinking is off
 * (see the release block in app/build.gradle.kts), so nothing would remove
 * them.
 *
 * Paths are tinted at the call site; the colour here is a placeholder that
 * Icon replaces.
 */
object Glyphs {

    private fun glyph(
        name: String,
        autoMirror: Boolean = false,
        vararg strokes: String,
        fills: List<String> = emptyList()
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = DevClipTheme.IconSize.MD.dp,
            defaultHeight = DevClipTheme.IconSize.MD.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror
        )
        strokes.forEach {
            builder.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = DevClipTheme.IconSize.STROKE_DP,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        fills.forEach {
            builder.addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black))
        }
        return builder.build()
    }

    /** New note. */
    val Add: ImageVector by lazy { glyph("Add", false, "M12 5v14M5 12h14") }

    val Close: ImageVector by lazy { glyph("Close", false, "M6 6l12 12M18 6L6 18") }

    /** Mirrors in right-to-left, where "back" points the other way. */
    val Back: ImageVector by lazy {
        glyph("Back", true, "M19 12H5M11 6l-6 6 6 6")
    }

    val Search: ImageVector by lazy {
        glyph("Search", false, "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M20 20l-4-4")
    }

    /** Three dots. Filled, because a 1.8dp stroke around a dot is a smudge. */
    val More: ImageVector by lazy {
        glyph(
            "More", false,
            fills = listOf(
                "M12 3.6a1.6 1.6 0 1 0 0 3.2a1.6 1.6 0 1 0 0-3.2z",
                "M12 10.4a1.6 1.6 0 1 0 0 3.2a1.6 1.6 0 1 0 0-3.2z",
                "M12 17.2a1.6 1.6 0 1 0 0 3.2a1.6 1.6 0 1 0 0-3.2z"
            )
        )
    }

    /**
     * The floating bubble: a ring with a core. The same silhouette as the
     * thing it brings up, which is the only metaphor that needs no learning.
     */
    val Bubble: ImageVector by lazy {
        glyph(
            "Bubble", false,
            "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16z",
            fills = listOf("M12 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z")
        )
    }

    val Copy: ImageVector by lazy {
        glyph(
            "Copy", false,
            "M10 9h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z",
            "M16 9V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"
        )
    }

    val Delete: ImageVector by lazy {
        glyph(
            "Delete", false,
            "M4 7h16",
            "M9 7V4.5h6V7",
            "M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12",
            "M10 11v6M14 11v6"
        )
    }

    /** What's new: a clock turning back. */
    val History: ImageVector by lazy {
        glyph(
            "History", false,
            "M3.5 12a8.5 8.5 0 1 0 2.5-6",
            "M3.5 3.5V8H8",
            "M12 8v4.5l3 2"
        )
    }

    val Settings: ImageVector by lazy {
        glyph(
            "Settings", false,
            "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
            "M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4" +
                "M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7"
        )
    }
}
