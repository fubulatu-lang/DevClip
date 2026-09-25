package com.devclip.app

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * The ring that says "there is a selection, tap to save it".
 *
 * Two strokes, not one. The bubble floats over other people's apps and
 * DevClip has no say in what is behind it: a white ring on a white app is no
 * ring at all, and a black one on a dark app is no better. A dark stroke
 * sitting just outside a white one survives any background, because whichever
 * half disappears the other is at full contrast against it.
 *
 * It breathes rather than expands. A halo growing outward would be the
 * clearer signal, and it is not available: the bubble's window is exactly the
 * size of the bubble, so anything drawn past that edge is clipped away by the
 * window rather than by this drawable. What is left is the ring's own weight
 * and opacity, and varying both together reads as a pulse without needing a
 * single pixel outside the circle.
 *
 * The animation is skipped when the user has turned animations off. The ring
 * is still drawn — it carries the meaning, the movement only draws the eye.
 */
class SelectionRingDrawable(
    private val strokePx: Float,
    private val outlinePx: Float,
    animated: Boolean
) : Drawable() {

    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = DevClipTheme.Overlay.LIGHT
    }

    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = DevClipTheme.Overlay.DARK
    }

    /** 0 is the quiet end of the breath, 1 the full one. */
    private var phase = 1f

    private val animator: ValueAnimator? = if (!animated) null else
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidateSelf()
            }
        }

    companion object {
        /** One half-breath. Two of these is a full cycle, so ~1.6s in and out. */
        private const val PULSE_MS = 800L

        /** How thin the ring gets at the quiet end, as a share of full weight. */
        private const val MIN_WEIGHT = 0.55f

        /** How faint it gets there, out of 255. */
        private const val MIN_ALPHA = 110
    }

    fun start() {
        animator?.let { if (!it.isStarted) it.start() }
    }

    /**
     * Stops the animation and releases it.
     *
     * A ValueAnimator with an infinite repeat keeps running after the view it
     * was drawing into is gone, holding this drawable and the view with it.
     * The ring is removed every time a selection ends, which on a phone being
     * typed on is often.
     */
    fun stop() {
        animator?.cancel()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return

        val weight = strokePx * (MIN_WEIGHT + (1f - MIN_WEIGHT) * phase)
        val alpha = (MIN_ALPHA + (255 - MIN_ALPHA) * phase).toInt()

        // Centred on the stroke, so the ring sits inside the bubble's edge
        // rather than half outside a window that would cut it in half.
        val radius = minOf(b.width(), b.height()) / 2f - strokePx / 2f
        if (radius <= 0f) return

        val cx = b.exactCenterX()
        val cy = b.exactCenterY()

        dark.strokeWidth = weight + outlinePx * 2f
        dark.alpha = (alpha * 0.55f).toInt()
        canvas.drawCircle(cx, cy, radius, dark)

        white.strokeWidth = weight
        white.alpha = alpha
        canvas.drawCircle(cx, cy, radius, white)
    }

    override fun setAlpha(alpha: Int) { /* Opacity is this drawable's own business. */ }

    override fun setColorFilter(colorFilter: ColorFilter?) { /* Deliberately fixed. */ }

    @Deprecated("Required by Drawable; the framework still calls it.")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
