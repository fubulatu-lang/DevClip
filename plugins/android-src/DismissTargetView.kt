package com.devclip.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View

/**
 * The "drop it here to hide it" target: a circle with an ✕, low and centred.
 *
 * Bottom-*centre*, not a full-width bottom strip, and that is the whole point.
 * The bubble lives docked to an edge, so dragging it straight down its own
 * rail to park it low never crosses this. Only a deliberate diagonal drag into
 * the middle of the bottom reaches it, which means the gesture cannot be
 * performed by accident while repositioning. Widening this into a strip would
 * throw that away.
 *
 * It carries its own scrim. A pale ✕ over a pale app is invisible, and this is
 * the control that makes the bubble disappear — the one thing the user must be
 * certain they are about to hit.
 */
class DismissTargetView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density

    private fun dp(value: Float) = value * density

    /** Radius of the resting circle. */
    val radiusPx = dp(30f)

    /** How far the circle's centre sits above the bottom of the safe area. */
    private val bottomMarginPx = dp(88f)

    /**
     * How close counts as "in".
     *
     * Wider than the circle so the target reaches out and takes the bubble
     * rather than demanding pixel accuracy from a dragging thumb. Crossing
     * this is what magnetises the bubble into the centre, so by the time the
     * user lets go the bubble is visibly *inside* the target, not near it.
     */
    val magnetRadiusPx = radiusPx * 1.5f

    var engaged: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    val circleCenterX: Float get() = width / 2f
    val circleCenterY: Float get() = height - bottomMarginPx

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // A gradient rather than a flat wash: a hard scrim edge across the
        // middle of somebody else's app reads as a rendering fault.
        scrimPaint.shader = LinearGradient(
            0f, h * 0.55f, 0f, h.toFloat(),
            Color.TRANSPARENT, Color.argb(150, 0, 0, 0),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, height * 0.55f, width.toFloat(), height.toFloat(), scrimPaint)

        val radius = if (engaged) radiusPx * 1.15f else radiusPx
        circlePaint.color =
            if (engaged) Color.argb(255, 198, 47, 38) else Color.argb(210, 40, 40, 40)
        canvas.drawCircle(circleCenterX, circleCenterY, radius, circlePaint)

        val arm = radius * 0.32f
        canvas.drawLine(
            circleCenterX - arm, circleCenterY - arm,
            circleCenterX + arm, circleCenterY + arm, crossPaint
        )
        canvas.drawLine(
            circleCenterX + arm, circleCenterY - arm,
            circleCenterX - arm, circleCenterY + arm, crossPaint
        )
    }
}
