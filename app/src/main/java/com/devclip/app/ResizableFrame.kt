package com.devclip.app

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout

/**
 * A container that lets the window around it be resized by dragging its edges.
 *
 * The floating list is a window, not a view inside an Activity, so there is no
 * window chrome to grab — nothing draws a frame around it and nothing offers a
 * corner to pull. This supplies both: a band along the inside of each edge
 * that takes the drag, and the arithmetic for which edges a given grab moves.
 *
 * Touches land on the child as normal until one starts inside that band, which
 * is why this intercepts rather than consumes. Tapping a clip near the edge of
 * the list still pastes it; pressing there and *dragging* resizes. The band is
 * deliberately narrower than a comfortable touch target — it is an affordance
 * for a deliberate gesture, not a control, and every pixel of it is a pixel
 * where tapping a clip becomes harder.
 */
class ResizableFrame(context: Context) : FrameLayout(context) {

    /** Which edges a drag is moving, and how far it has travelled. */
    fun interface ResizeListener {
        fun onResize(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean, dx: Int, dy: Int)
    }

    var onResizeStart: (() -> Unit)? = null
    var onResizeEnd: (() -> Unit)? = null
    var resizeListener: ResizeListener? = null

    /**
     * A touch that landed outside this window entirely.
     *
     * Only ever delivered when the window was added with
     * FLAG_WATCH_OUTSIDE_TOUCH. The touch still reaches whatever is
     * underneath — this is a copy, not an interception, which is what makes
     * it safe to watch for on a window floating over other people's apps.
     */
    var onOutsideTouch: (() -> Unit)? = null

    /**
     * dispatch rather than onTouchEvent: an ACTION_OUTSIDE event carries
     * coordinates outside this view's bounds, so the ordinary hit-testing a
     * ViewGroup does on the way down has nothing to hand it to.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            onOutsideTouch?.invoke()
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private val edgeSlop = (EDGE_DP * context.resources.displayMetrics.density).toInt()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var grabLeft = false
    private var grabTop = false
    private var grabRight = false
    private var grabBottom = false

    private var downX = 0f
    private var downY = 0f
    private var resizing = false

    companion object {
        /**
         * How far inside each edge a resize drag can start.
         *
         * 16dp: wide enough to find without aiming, narrow enough that the
         * clip rows — which are inset from the container by the same order of
         * spacing — stay comfortably tappable.
         */
        private const val EDGE_DP = 16
    }

    /**
     * Works out which edges a press at (x, y) grabs. A press in a corner grabs
     * two, which is what makes corner-dragging resize both axes at once
     * without a separate corner handle.
     */
    private fun grabEdges(x: Float, y: Float): Boolean {
        grabLeft = x <= edgeSlop
        grabTop = y <= edgeSlop
        grabRight = x >= width - edgeSlop
        grabBottom = y >= height - edgeSlop
        return grabLeft || grabTop || grabRight || grabBottom
    }

    private fun clearGrab() {
        grabLeft = false; grabTop = false; grabRight = false; grabBottom = false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizing = false
                downX = event.x
                downY = event.y
                // Note the edges but do not intercept yet: a press that never
                // becomes a drag has to reach the child, or a clip near the
                // edge of the list could not be tapped at all.
                grabEdges(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!resizing && (grabLeft || grabTop || grabRight || grabBottom)) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                        resizing = true
                        onResizeStart?.invoke()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> clearGrab()
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            // Only reached when the press began outside the child's reach; the
            // interesting path is the one the intercept above hands over.
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return grabEdges(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!resizing) return false
                resizeListener?.onResize(
                    grabLeft, grabTop, grabRight, grabBottom,
                    (event.x - downX).toInt(), (event.y - downY).toInt()
                )
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (resizing) {
                    resizing = false
                    clearGrab()
                    onResizeEnd?.invoke()
                    return true
                }
                clearGrab()
            }
        }
        return false
    }
}
