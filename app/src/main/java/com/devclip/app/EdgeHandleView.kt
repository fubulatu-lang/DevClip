package com.devclip.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

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
 */
@SuppressLint("ViewConstructor")
class EdgeHandleView(context: Context, private val onLeftEdge: Boolean) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val palette = DevClipTheme.colors(context)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.inkFaint
    }

    private val rect = RectF()

    companion object {
        /** Drawn thickness of the bar. */
        const val BAR_WIDTH_DP = 5

        /** Width of the window behind it, which is the actual touch target. */
        const val TOUCH_WIDTH_DP = 28

        /** Height of the bar, and of its window. */
        const val HEIGHT_DP = 68
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bar = BAR_WIDTH_DP * density
        val radius = bar / 2f

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
