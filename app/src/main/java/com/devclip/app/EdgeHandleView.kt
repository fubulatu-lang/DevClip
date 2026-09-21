package com.devclip.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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

    /**
     * The dark edge under the white bar.
     *
     * The same problem the bubble's ring has, for the same reason: this floats
     * over other people's apps and DevClip has no say in what is behind it. A
     * white bar pulsing on a white page is a bar pulsing out of existence. A
     * dark edge under it means whichever half disappears, the other is at full
     * contrast.
     */
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    private val outlineRect = RectF()

    private val rect = RectF()

    private var highlighted = false

    /** 0 is the quiet end of the breath, 1 the full one. */
    private var phase = 1f
    private var pulse: ValueAnimator? = null

    companion object {
        /** Drawn thickness of the bar at rest. */
        const val BAR_WIDTH_DP = 5

        /** Drawn thickness while there is a selection waiting to be saved. */
        const val HIGHLIGHT_BAR_WIDTH_DP = 16

        /** The dark edge drawn around the highlighted bar. */
        private const val OUTLINE_DP = 1.5f

        /**
         * Width of the window behind it, which is the actual touch target.
         *
         * Comfortably wider than the highlighted bar and its outline, so
         * thickening never reaches the edge of its own window and gets
         * squared off.
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
        // White while it matters, and white regardless of theme: this is
        // drawn over another app, so the thing it has to stand out from is
        // that app, not DevClip's own palette.
        barPaint.color = if (on) Color.WHITE else palette.inkFaint
        if (on) startPulse() else stopPulse()
        invalidate()
    }

    /** Re-reads the theme. The service outlives every screen that can change it. */
    fun applyTheme() {
        palette = DevClipTheme.colors(context)
        barPaint.color = if (highlighted) Color.WHITE else palette.inkFaint
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

        val alpha = if (highlighted) (MIN_ALPHA + (255 - MIN_ALPHA) * phase).toInt() else 255
        barPaint.alpha = alpha

        // Room for the outline at top and bottom. Without it the bar fills
        // the window's full height, the outline is drawn past the edge of
        // that window, and the rounded caps come back squared off.
        val inset = if (highlighted) OUTLINE_DP * density else 0f

        // Overhang the outer edge by the corner radius so that rounding falls
        // off-screen: the bar reads as growing out of the edge rather than
        // floating beside it.
        if (onLeftEdge) {
            rect.set(-radius, inset, bar, height - inset)
        } else {
            rect.set(width - bar, inset, width + radius, height - inset)
        }

        if (highlighted) {
            // Under the bar and slightly proud of it on the three sides that
            // are on screen. The outer side needs none: it is off the edge of
            // the display.
            val out = OUTLINE_DP * density
            outlineRect.set(
                rect.left - if (onLeftEdge) 0f else out,
                rect.top - out,
                rect.right + if (onLeftEdge) out else 0f,
                rect.bottom + out
            )
            outlinePaint.alpha = (alpha * 0.5f).toInt()
            canvas.drawRoundRect(outlineRect, radius + out, radius + out, outlinePaint)
        }

        canvas.drawRoundRect(rect, radius, radius, barPaint)
    }
}
