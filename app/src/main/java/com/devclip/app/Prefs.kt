package com.devclip.app

/**
 * Settings shared between the app and the components that run without it —
 * OverlayService, BootReceiver, the accessibility service.
 *
 * SharedPreferences rather than state held in the app, because these are
 * needed at moments when the app is not running at all: BootReceiver starts
 * the service before anything has been opened, and capture happens with the
 * app closed, which is the entire point of it.
 */
object Prefs {
    const val NAME = "devclip_prefs"
    const val KEY_BUBBLE_RUNNING = "bubble_running"
    const val KEY_AUTO_START_ON_BOOT = "auto_start_on_boot"

    /**
     * Bubble diameter in dp.
     *
     * A number, not one of three names, because the size is a slider now. The
     * floor is Android's comfortable touch target: below 48dp a bubble gets
     * missed, and it gets missed most over a keyboard, which is exactly where
     * it matters. The ceiling is 1.5x that — past it the bubble stops being a
     * bubble and starts being an obstruction.
     *
     * A new key rather than a reused one: the old value was a String, and
     * reading a String key as an Int throws.
     */
    const val KEY_BUBBLE_SIZE_DP = "bubble_size_dp"
    const val MIN_BUBBLE_SIZE_DP = 48
    const val MAX_BUBBLE_SIZE_DP = 72
    const val DEFAULT_BUBBLE_SIZE_DP = 56

    /**
     * Where the bubble is docked, as an edge and a fraction of the way down.
     *
     * Never pixels. A pixel position is meaningless the moment the window
     * changes shape — rotation, split-screen, a foldable opening — and this
     * has to survive all three plus a reboot.
     *
     * There is deliberately no setting for either. Dragging the bubble is the
     * only way to move it, which is why these are read by the service and
     * never written by it from JS.
     */
    const val KEY_BUBBLE_EDGE = "bubble_edge"
    const val KEY_BUBBLE_Y_FRACTION = "bubble_y_fraction"
    const val EDGE_LEFT = "left"
    const val EDGE_RIGHT = "right"
    const val DEFAULT_Y_FRACTION = 0.28f

    /**
     * The user's clip limit, mirrored here so capture can enforce it.
     *
     * Trimming used to happen only while the app was open. Capture happens
     * with the app closed — that is the point of it — so the limit was not a
     * limit until DevClip was next opened.
     */
    const val KEY_MAX_CLIPS = "max_clips"
    const val DEFAULT_MAX_CLIPS = 500

    /**
     * Tap-to-arm, mirrored for the floating list.
     *
     * The list is native now and opens with no React context behind it, so a
     * setting it obeys has to live where it can read it.
     */
    const val KEY_CONFIRM_BEFORE_PASTE = "confirm_before_paste"

    /**
     * How opaque the bubble and the list are, as percentages.
     *
     * The floor is not zero. A window at zero opacity is invisible but still
     * takes touches, which is indistinguishable from a phone that has started
     * ignoring part of the screen — so the slider stops well before the point
     * where the user could lose the bubble entirely.
     */
    const val KEY_BUBBLE_ALPHA = "bubble_alpha"
    const val KEY_POPUP_ALPHA = "popup_alpha"
    const val MIN_ALPHA = 20
    const val DEFAULT_ALPHA = 100

    /** Whether the chosen transparency applies only while the bubble is idle. */
    const val KEY_BUBBLE_IDLE_FADE = "bubble_idle_fade"

    /**
     * Seconds of stillness before the bubble becomes an edge handle. 0 is off.
     *
     * Off by default: a bubble that disappears on its own is a surprise the
     * first time it happens, and it should be something the user turned on.
     */
    const val KEY_TUCK_DELAY_SEC = "tuck_delay_sec"
    const val DEFAULT_TUCK_DELAY_SEC = 0
    const val MAX_TUCK_DELAY_SEC = 120

    /**
     * The size the user last dragged the floating list to, in dp.
     *
     * Stored in dp, never pixels, for the same reason the bubble's position
     * is: a pixel size stops meaning anything the moment the window changes
     * shape, and this has to survive a rotation and a reboot.
     */
    /**
     * Whether setup has been shown.
     *
     * Shown once after install and then never again on its own, however the
     * permissions stand. A screen the user has already worked through is not
     * worth repeating; a permission that has since been revoked is surfaced
     * in Settings under Status instead.
     */
    const val KEY_HAS_ONBOARDED = "has_onboarded"

    /**
     * Light, dark, or whatever the system is doing.
     *
     * Here rather than in the app, because the floating windows have to agree
     * with it and they are drawn by a service that may be the only thing
     * running. An app-held preference would leave the bubble on one theme and
     * the app on another.
     */
    const val KEY_THEME_MODE = "theme_mode"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    const val KEY_POPUP_WIDTH_DP = "popup_width_dp"
    const val KEY_POPUP_HEIGHT_DP = "popup_height_dp"
}
