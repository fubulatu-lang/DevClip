package com.devclip.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/**
 * DevClip's design system: One UI, in monochrome.
 *
 * Every value here traces to the Samsung One UI guidelines — the 2dp spacing
 * scale, the 24dp keyline, the 17sp body, pill radii, the real easing curves.
 * `.claude/one-ui/reference/TOKENS.md` holds the citations.
 *
 * The colour is a deliberate departure, and the only one. One UI expects an
 * accent taken from the user's wallpaper palette; DevClip has no accent at
 * all. Black on white, white on black, and nothing in between carrying
 * meaning. A clipboard manager floats over other people's apps all day — it
 * has no business competing with them for attention, and the app icon is
 * where the colour lives instead.
 *
 * The three functional colours survive that decision because One UI does not
 * permit otherwise: success, caution and error must be distinguishable, and
 * colour must never be the only thing distinguishing them. They are the sole
 * hues in the system and they are never decorative.
 *
 * Light and dark come from the system's night mode rather than a stored
 * preference, because a Service is started long before any preference has
 * been read — by BootReceiver, before the app has ever been opened.
 */
object DevClipTheme {

    data class Colors(
        /** The screen behind everything. True white, true black. */
        val bg: Int,
        /** Cards and raised rows. Separated from bg by tone, never by shadow. */
        val surface: Int,
        /** Wells and badges — recessed, below the surface. */
        val surfaceSunken: Int,
        val border: Int,
        val divider: Int,
        /** Primary text and icons. One UI: icons are text-coloured by default. */
        val ink: Int,
        val inkSoft: Int,
        val inkFaint: Int,
        /** Disabled and decorative only — below AA by design, as One UI has it. */
        val inkDisabled: Int,
        /**
         * Selected and active states.
         *
         * Monochrome, so it carries no hue of its own: it is simply ink at
         * full strength against a surface that has stepped back. Anything
         * marked with it must also differ in weight, fill or position —
         * colour alone never conveys state.
         */
        val accent: Int,
        val onAccent: Int,
        val accentSoft: Int,
        val success: Int,
        val warning: Int,
        val danger: Int,
        /** One UI scrim: 20% black, both themes. */
        val scrim: Int
    )

    /**
     * Light.
     *
     * The background is white and cards step *down* to a near-white grey.
     * One UI separates surfaces by tone rather than shadow, and with a true
     * white page the only direction left to move is darker.
     */
    private val light = Colors(
        bg = Color.parseColor("#FFFFFF"),
        surface = Color.parseColor("#F7F7F7"),
        surfaceSunken = Color.parseColor("#EDEDED"),
        border = Color.argb(30, 0, 0, 0),
        divider = Color.argb(20, 0, 0, 0),
        ink = Color.parseColor("#252525"),
        inkSoft = Color.parseColor("#3B3B3B"),
        inkFaint = Color.parseColor("#505050"),
        inkDisabled = Color.parseColor("#8C8C8C"),
        accent = Color.parseColor("#252525"),
        onAccent = Color.parseColor("#FFFFFF"),
        accentSoft = Color.parseColor("#EDEDED"),
        success = Color.parseColor("#0F7A4A"),
        warning = Color.parseColor("#A65A00"),
        danger = Color.parseColor("#C62F26"),
        scrim = Color.argb(51, 0, 0, 0)
    )

    /**
     * Dark.
     *
     * True black, not a dark grey and not a tinted navy. It is what One UI
     * does, it is what the OLED panel on the phone this was built for wants,
     * and it is the background a floating window can sit on without
     * announcing itself. Cards step *up* from it.
     */
    private val dark = Colors(
        bg = Color.parseColor("#000000"),
        surface = Color.parseColor("#121212"),
        surfaceSunken = Color.parseColor("#1C1C1C"),
        border = Color.argb(38, 255, 255, 255),
        divider = Color.argb(28, 255, 255, 255),
        ink = Color.parseColor("#FAFAFA"),
        inkSoft = Color.parseColor("#E5E5E5"),
        inkFaint = Color.parseColor("#B0B0B0"),
        inkDisabled = Color.parseColor("#808080"),
        accent = Color.parseColor("#FAFAFA"),
        onAccent = Color.parseColor("#000000"),
        accentSoft = Color.parseColor("#1C1C1C"),
        success = Color.parseColor("#4FD18B"),
        warning = Color.parseColor("#FFB84D"),
        danger = Color.parseColor("#FF8A80"),
        scrim = Color.argb(51, 0, 0, 0)
    )

    /**
     * Whether this surface should be drawn dark.
     *
     * The stored preference wins over the system, and "system" defers to it.
     * Every surface asks this one function — the launcher app, the bubble,
     * the floating list — so a theme chosen in Settings reaches the windows a
     * service draws, which hold no reference to the app and may outlive it.
     */
    fun isDark(context: Context): Boolean {
        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        return when (prefs.getString(Prefs.KEY_THEME_MODE, Prefs.THEME_SYSTEM)) {
            Prefs.THEME_LIGHT -> false
            Prefs.THEME_DARK -> true
            else -> context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun colors(context: Context): Colors = if (isDark(context)) dark else light

    /**
     * One UI spacing: a 2dp-resolution scale, not a strict 8pt grid.
     *
     * KEYLINE is the load-bearing one. Samsung asks for at least 24dp from
     * each screen edge, to clear curved glass and the touch-blocking zones a
     * hand wrapped around the phone creates.
     */
    object Spacing {
        const val XS = 4
        const val SM = 8
        const val MD = 12
        const val LG = 16
        const val XL = 20
        const val KEYLINE = 24
    }

    /**
     * One UI radii. PILL is 26dp and is the standard button shape — a 4dp or
     * 8dp button is the clearest single signal that a design is Material
     * rather than One UI.
     */
    object Radius {
        const val XS = 4
        const val SM = 8
        const val MD = 12
        const val LG = 22
        const val PILL = 26
        const val CONTAINER = 26
    }

    /**
     * The One UI type scale, in sp so it follows the user's font size.
     *
     * Deliberately larger than Material. 17sp body is the One UI default and
     * shrinking it is the fastest way to stop looking like the system.
     */
    object Text {
        const val DISPLAY = 34f
        const val TITLE = 18f
        const val BODY = 17f
        const val BUTTON = 17f
        const val SECONDARY = 15f
        const val CAPTION = 13f
        const val MICRO = 12f
    }

    /**
     * The floating list's type scale: the same roles, proportionally smaller.
     *
     * A proportion rather than fixed small numbers, so someone running large
     * text gets large text here too — just more compact. MICRO is the floor;
     * nothing in the list goes below it however the scale is applied.
     */
    object MiniText {
        private const val SCALE = 0.85f
        private fun shrink(size: Float) =
            maxOf(Text.MICRO, Math.round(size * SCALE).toFloat())

        val BODY = shrink(Text.BODY)
        val SECONDARY = shrink(Text.SECONDARY)
        val CAPTION = shrink(Text.CAPTION)
    }

    /** One UI's real easing curves, as cubic bezier control points. */
    object Easing {
        val SINE_IN_OUT = floatArrayOf(0.33f, 0f, 0.67f, 1f)
        val STANDARD = floatArrayOf(0.4f, 0f, 0.2f, 1f)
        val EMPHASIZED_DECELERATE = floatArrayOf(0.22f, 0.25f, 0f, 1f)
        val BACK_GESTURE = floatArrayOf(0.1f, 0.1f, 0f, 1f)
        val DRAWER_SETTLE = floatArrayOf(0f, 0f, 0f, 1f)
    }

    /** Nothing routine exceeds 500ms. */
    object Duration {
        const val INSTANT = 100L
        const val SHORT = 120L
        const val STANDARD = 167L
        const val MEDIUM = 200L
        const val EMPHASIZED = 260L
        const val LONG = 400L
        const val EXTENDED = 500L
    }

    /** One UI symbol sizes. 24dp is the default for a standalone symbol. */
    object IconSize {
        const val SM = 18
        const val MD = 24
        const val LG = 48
        const val STROKE_DP = 1.8f
    }

    /**
     * The minimum touch target, in dp.
     *
     * A control may be drawn smaller than this; the target may not be. It
     * matters most over a keyboard, which is exactly where the bubble spends
     * its time.
     */
    const val MIN_TOUCH_TARGET = 48
}
