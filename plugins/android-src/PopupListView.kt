package com.devclip.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The floating list, drawn in ordinary Android views.
 *
 * This used to be a React Native surface, and that is why it did not work. A
 * surface needs a live React instance behind it, and nothing starts one but an
 * Activity — so launched from the bubble after a reboot, or after DevClip was
 * swiped out of Recents, the window was added with nothing inside it and the
 * user saw an empty outline. Opening the launcher app started the instance and
 * the list appeared, which made the bug look intermittent when it was not.
 *
 * Views need none of that. The list reads the database directly and draws
 * immediately, whether or not any React instance has ever existed in this
 * process.
 *
 * One shape, tethered to the bubble, paste-only, at a smaller type scale — the
 * same surface it always was. No search and no editing: those live in the full
 * app, where there is room for a keyboard and a result list.
 */
@SuppressLint("ViewConstructor")
class PopupListView(context: Context) : LinearLayout(context) {

    /** Paste this clip into whatever field is focused underneath. */
    var onPaste: ((String) -> Unit)? = null

    /** Open the launcher app, which also closes this window. */
    var onOpenFullApp: (() -> Unit)? = null

    /** Close this window. The bubble stays. */
    var onClose: (() -> Unit)? = null

    private val palette = DevClipTheme.colors(context)
    private val rows = LinearLayout(context)
    private val scroller = ScrollView(context)
    private val handler = Handler(Looper.getMainLooper())

    /**
     * The clip one tap away from pasting, or null.
     *
     * One armed row at a time, held here rather than per row, so that arming
     * one clip and then tapping another arms the second rather than pasting
     * it. A mis-tap on the wrong row must never paste the wrong text into
     * somebody's message.
     */
    private var armedId: Long? = null
    private val disarm = Runnable { armedId = null; redraw() }

    private var clips: List<DevClipDatabaseHelper.Clip> = emptyList()
    private var confirmBeforePaste = true

    companion object {
        /**
         * How many clips the floating list draws.
         *
         * It is a quick-paste surface, not the history. Drawing the user's
         * whole limit — up to 500 rows — into a scrolling column would build
         * every one of those views on the main thread while a finger is
         * waiting on the bubble. The full list is one tap away in the app.
         */
        const val MAX_ROWS = 50

        /** How long an armed row stays armed. Matches src/store/pasteArmStore.ts. */
        private const val ARM_TIMEOUT_MS = 2000L
    }

    init {
        orientation = VERTICAL
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(DevClipTheme.RADIUS_CONTAINER).toFloat()
            setColor(palette.bg)
            setStroke(dp(1), palette.border)
        }
        // A rounded background alone does not clip children on a plain
        // LinearLayout; without this the first row's corners square off the
        // container they sit in.
        clipToOutline = true

        addView(buildHeader(), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        rows.orientation = VERTICAL
        rows.setPadding(
            dp(DevClipTheme.SPACE_LG), dp(DevClipTheme.SPACE_SM),
            dp(DevClipTheme.SPACE_LG), dp(DevClipTheme.SPACE_SM)
        )
        scroller.isFillViewport = true
        // No explicit params: a ScrollView is a FrameLayout underneath and
        // rejects the LinearLayout params this class's `LayoutParams` resolves
        // to, at layout time rather than at compile time.
        scroller.addView(rows)
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun sp(view: TextView, value: Float) =
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)

    // ---- Header ----

    private fun buildHeader(): View {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(palette.surface)
            setPadding(
                dp(DevClipTheme.SPACE_LG), dp(DevClipTheme.SPACE_SM),
                dp(DevClipTheme.SPACE_SM), dp(DevClipTheme.SPACE_SM)
            )
        }

        val dot = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.accent)
            }
        }
        header.addView(dot, LayoutParams(dp(8), dp(8)).apply {
            rightMargin = dp(DevClipTheme.SPACE_SM)
        })

        val title = TextView(context).apply {
            text = context.getString(R.string.devclip_popup_title)
            setTextColor(palette.ink)
            sp(this, DevClipTheme.TEXT_BODY_SP)
            maxLines = 1
        }
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        header.addView(
            iconButton(StrokeIcon.EXPAND, R.string.devclip_popup_open_full) {
                onOpenFullApp?.invoke()
            }
        )
        header.addView(
            iconButton(StrokeIcon.CLOSE, R.string.devclip_popup_close) {
                onClose?.invoke()
            }
        )

        val divider = View(context).apply { setBackgroundColor(palette.divider) }
        val wrap = LinearLayout(context).apply { orientation = VERTICAL }
        wrap.addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        wrap.addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))
        return wrap
    }

    /**
     * A 40dp round tap target.
     *
     * The icon inside is 18dp; the target around it is not, because a control
     * in a floating window over somebody else's app is the last place to make
     * a touch target the size of its artwork.
     */
    private fun iconButton(icon: StrokeIcon, labelRes: Int, action: () -> Unit): View {
        val view = StrokeIconView(context, icon, palette.inkSoft).apply {
            contentDescription = context.getString(labelRes)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
            }
        }
        view.layoutParams = LayoutParams(dp(40), dp(40))
        return view
    }

    // ---- Content ----

    /** Replaces what the list shows. Cheap enough to call on every open. */
    fun render(clips: List<DevClipDatabaseHelper.Clip>, confirmBeforePaste: Boolean) {
        this.clips = clips
        this.confirmBeforePaste = confirmBeforePaste
        handler.removeCallbacks(disarm)
        armedId = null
        redraw()
    }

    private fun redraw() {
        rows.removeAllViews()
        if (clips.isEmpty()) {
            rows.addView(buildEmptyState())
            return
        }
        clips.forEachIndexed { index, clip ->
            rows.addView(buildRow(clip, index + 1))
        }
    }

    private fun buildEmptyState(): View {
        val text = TextView(context).apply {
            text = context.getString(R.string.devclip_popup_empty)
            setTextColor(palette.inkFaint)
            sp(this, DevClipTheme.TEXT_CAPTION_SP)
            gravity = Gravity.CENTER
            setPadding(dp(DevClipTheme.SPACE_LG), dp(40), dp(DevClipTheme.SPACE_LG), dp(40))
        }
        return text
    }

    private fun buildRow(clip: DevClipDatabaseHelper.Clip, position: Int): View {
        val armed = armedId == clip.id
        // A ring rather than a colour swap: the row has to stay readable while
        // it says the next tap will paste it.
        val pad = dp(DevClipTheme.SPACE_MD)
        val card = LinearLayout(context).apply {
            orientation = HORIZONTAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(DevClipTheme.RADIUS_MD).toFloat()
                setColor(palette.surface)
                if (armed) setStroke(dp(2), palette.accent)
            }
            setPadding(pad, pad, pad, pad)
            isClickable = true
            isFocusable = true
        }

        val badge = TextView(context).apply {
            text = position.toString()
            setTextColor(palette.inkSoft)
            sp(this, DevClipTheme.TEXT_CAPTION_SP)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(DevClipTheme.RADIUS_SM).toFloat()
                setColor(palette.surfaceSunken)
            }
        }
        card.addView(badge, LayoutParams(dp(24), dp(24)).apply {
            rightMargin = dp(DevClipTheme.SPACE_MD)
            topMargin = dp(2)
        })

        val column = LinearLayout(context).apply { orientation = VERTICAL }

        if (!clip.title.isNullOrBlank()) {
            column.addView(TextView(context).apply {
                text = clip.title
                setTextColor(palette.ink)
                sp(this, DevClipTheme.TEXT_BODY_SP)
                maxLines = 1
            })
        }

        column.addView(TextView(context).apply {
            text = clip.content
            setTextColor(palette.inkSoft)
            sp(this, DevClipTheme.TEXT_SECONDARY_SP)
            maxLines = 2
        })

        column.addView(TextView(context).apply {
            text = context.getString(
                if (armed) R.string.devclip_popup_armed else R.string.devclip_popup_tap_to_paste
            )
            setTextColor(if (armed) palette.accent else palette.inkFaint)
            sp(this, DevClipTheme.TEXT_CAPTION_SP)
        }.also { it.setPadding(0, dp(DevClipTheme.SPACE_SM), 0, 0) })

        card.addView(column, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        card.contentDescription = buildString {
            append(position).append(". ")
            if (!clip.title.isNullOrBlank()) append(clip.title).append(": ")
            append(clip.content)
        }
        card.setOnClickListener { onRowTapped(clip) }

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.bottomMargin = dp(DevClipTheme.SPACE_SM)
        card.layoutParams = params
        return card
    }

    /**
     * Tap once to arm, tap again to paste.
     *
     * Not a dialog. An Android dialog needs a foreground Activity to attach
     * to, and this window has none by design, so a confirmation dialog here
     * would not merely be cramped — it would never appear, and the tap would
     * do nothing at all.
     */
    private fun onRowTapped(clip: DevClipDatabaseHelper.Clip) {
        if (!confirmBeforePaste) {
            onPaste?.invoke(clip.content)
            return
        }
        if (armedId == clip.id) {
            handler.removeCallbacks(disarm)
            armedId = null
            redraw()
            onPaste?.invoke(clip.content)
            return
        }
        armedId = clip.id
        redraw()
        handler.removeCallbacks(disarm)
        handler.postDelayed(disarm, ARM_TIMEOUT_MS)
    }

    /** Releases the arm timer so a torn-down window leaves nothing pending. */
    fun release() {
        handler.removeCallbacks(disarm)
    }
}

/** The two glyphs the floating list needs, drawn rather than shipped. */
enum class StrokeIcon { EXPAND, CLOSE }

/**
 * Draws an icon with strokes instead of loading a drawable.
 *
 * Two shapes at one weight is not worth a vector asset each, and drawing them
 * means the stroke width and colour follow the same tokens as everything else
 * on this surface rather than being baked into an XML file.
 */
@SuppressLint("ViewConstructor")
class StrokeIconView(context: Context, private val icon: StrokeIcon, tint: Int) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 1.8f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val glyph = 18f * density
        val left = (width - glyph) / 2f
        val top = (height - glyph) / 2f
        val right = left + glyph
        val bottom = top + glyph
        val inset = glyph * 0.18f

        when (icon) {
            StrokeIcon.CLOSE -> {
                canvas.drawLine(left + inset, top + inset, right - inset, bottom - inset, paint)
                canvas.drawLine(right - inset, top + inset, left + inset, bottom - inset, paint)
            }
            // Two opposing corner brackets: "make this bigger", without
            // needing arrowheads that turn to mush at 18dp.
            StrokeIcon.EXPAND -> {
                val arm = glyph * 0.3f
                canvas.drawLine(left + inset, top + inset + arm, left + inset, top + inset, paint)
                canvas.drawLine(left + inset, top + inset, left + inset + arm, top + inset, paint)
                canvas.drawLine(right - inset, bottom - inset - arm, right - inset, bottom - inset, paint)
                canvas.drawLine(right - inset, bottom - inset, right - inset - arm, bottom - inset, paint)
            }
        }
    }
}
