package com.devclip.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/**
 * The colour and metric tokens DevClip's floating windows draw with.
 *
 * A deliberate mirror of `src/theme/theme.ts`. Two copies of one palette is a
 * real cost, and it is paid on purpose: the floating list is native so that it
 * can render with no React instance behind it — which is the whole reason the
 * list used to come up empty until the launcher app had been opened once — and
 * native cannot read a TypeScript module. Change one, change the other. When
 * the launcher app is native too, this becomes the only copy and the cost goes
 * away.
 *
 * Light and dark are chosen from the system's own night mode rather than from
 * the app's theme setting, because a Service is started by the system long
 * before any stored preference has been read.
 */
object DevClipTheme {

    data class Colors(
        val bg: Int,
        val surface: Int,
        val surfaceSunken: Int,
        val border: Int,
        val ink: Int,
        val inkSoft: Int,
        val inkFaint: Int,
        val inkDisabled: Int,
        val accent: Int,
        val divider: Int
    )

    private val light = Colors(
        bg = Color.parseColor("#F4F7F9"),
        surface = Color.parseColor("#FFFFFF"),
        surfaceSunken = Color.parseColor("#E8EDF1"),
        border = Color.argb(26, 27, 42, 53),
        ink = Color.parseColor("#1B2A35"),
        inkSoft = Color.parseColor("#46606F"),
        inkFaint = Color.parseColor("#5A6E7D"),
        inkDisabled = Color.parseColor("#93A4B0"),
        accent = Color.parseColor("#1D6FA9"),
        divider = Color.argb(26, 27, 42, 53)
    )

    private val dark = Colors(
        bg = Color.parseColor("#16242E"),
        surface = Color.parseColor("#243F4F"),
        surfaceSunken = Color.parseColor("#2E4E61"),
        border = Color.argb(31, 255, 255, 255),
        ink = Color.parseColor("#F2F6F8"),
        inkSoft = Color.parseColor("#D4DFE6"),
        inkFaint = Color.parseColor("#9DB3C0"),
        inkDisabled = Color.parseColor("#5E7686"),
        accent = Color.parseColor("#5FB0E8"),
        divider = Color.argb(31, 255, 255, 255)
    )

    fun colors(context: Context): Colors {
        val night = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (night) dark else light
    }

    /**
     * One UI spacing, on its 2dp-resolution scale. The floating list is the
     * "mini" surface, so it uses the tighter end of the scale throughout.
     */
    const val SPACE_XS = 4
    const val SPACE_SM = 8
    const val SPACE_MD = 12
    const val SPACE_LG = 16

    const val RADIUS_SM = 8
    const val RADIUS_MD = 12
    const val RADIUS_CONTAINER = 22

    /**
     * The mini type scale, in sp.
     *
     * Smaller than the launcher app's, but expressed in sp so it still follows
     * whatever font size the user has set system-wide. A fixed pixel size here
     * would make the floating list the one surface in DevClip that ignores an
     * accessibility setting.
     */
    const val TEXT_BODY_SP = 14f
    const val TEXT_SECONDARY_SP = 13f
    const val TEXT_CAPTION_SP = 11f
}
