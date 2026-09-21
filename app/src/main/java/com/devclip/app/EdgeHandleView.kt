package com.devclip.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * What the bubble becomes when it tucks itself away: a slim bar on the screen
 * edge, in the manner of One UI's own edge-panel handle.
 *
 * Deliberately not a smaller bubble. A shrunken app icon still reads as "an
 * app is floating on top of this", which is the thing the user asked to get
 * out of the way; a plain bar reads as an edge of the screen and stops
 * competing for attention. It is also a shape One UI users already know means
 * "pull this".
 *
 * The bar is drawn flush to the outer edge so its outer rounding falls off the
 * screen and only the inner curve shows — the same trick the system handle
 * uses to look attached to the display rather than placed near it. The window
 * behind it is wider than the bar, because a 5dp touch target is not a touch
 * target.
 *
 * It has a second state. Tucked away, the handle is meant to be ignored — but
 * a selection going live while it is tucked is the one moment it has
 * something to offer, and a 5dp grey line at the edge of somebody else's app
 * is not going to be noticed in time. So it thickens, takes the full ink
 * colour and breathes, which says "there is something here now" without
 * putting the bubble back over an app the user is reading.
 */
@SuppressLint("ViewConstructor")
class EdgeHandleView(context: Context, private val onLeftEdge: Boolean) : View(context) {

    private val density = context.resources.displayMetrics.density
    private var palette = DevClipTheme.colors(context)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.inkFaint
    }

    private val rect = RectF()

    private var highlighted = false

    /** 0 is the quiet end of the breath, 1 the full one. */
    private var phase = 1f
    private var pulse: ValueAnimator? = null

    companion object {
        /** Drawn thickness of the bar at rest. */
        const val BAR_WIDTH_DP = 5

        /** Drawn thickness while there is a selection waiting to be saved. */
        const val HIGHLIGHT_BAR_WIDTH_DP = 11

        /**
         * Width of the window behind it, which is the actual touch target.
         *
         * Comfortably wider than the highlighted bar, so thickening never
         * reaches the edge of its own window and gets squared off.
         */
        const val TOUCH_WIDTH_DP = 28

        /** Height of the bar at rest, and of its window. */
        const val HEIGHT_DP = 68

        private const val PULSE_MS = 800L
        private const val MIN_ALPHA = 120
    }

    /**
     * Whether there is a selection waiting.
     *
     * Idempotent: the selection watcher reports on every text-selection event,
     * which during ordinary typing is a great many, and restarting the
     * animator on each one would leave the bar stuttering rather than
     * breathing.
     */
    fun setHighlighted(on: Boolean) {
        if (highlighted == on) return
        highlighted = on
        barPaint.color = if (on) palette.ink else palette.inkFaint
        if (on) startPulse() else stopPulse()
        invalidate()
    }

    /** Re-reads the theme. The service outlives every screen that can change it. */
    fun applyTheme() {
        palette = DevClipTheme.colors(context)
        barPaint.color = if (highlighted) palette.ink else palette.inkFaint
        invalidate()
    }

    private fun startPulse() {
        if (pulse != null) return
        if (animationsDisabled()) { phase = 1f; return }
        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulse?.cancel()
        pulse = null
        phase = 1f
    }

    private fun animationsDisabled(): Boolean =
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (e: Exception) {
            false
        }

    /**
     * An infinite animator outlives the window it was drawing into unless it
     * is stopped, and this window is removed every time the bubble comes back.
     */
    override fun onDetachedFromWindow() {
        stopPulse()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bar = (if (highlighted) HIGHLIGHT_BAR_WIDTH_DP else BAR_WIDTH_DP) * density
        val radius = bar / 2f

        barPaint.alpha =
            if (highlighted) (MIN_ALPHA + (255 - MIN_ALPHA) * phase).toInt() else 255

        // Overhang the outer edge by the corner radius so that rounding falls
        // off-screen: the bar reads as growing out of the edge rather than
        // floating beside it.
        if (onLeftEdge) {
            rect.set(-radius, 0f, bar, height.toFloat())
        } else {
            rect.set(width - bar, 0f, width + radius, height.toFloat())
        }
        canvas.drawRoundRect(rect, radius, radius, barPaint)
    }
}
