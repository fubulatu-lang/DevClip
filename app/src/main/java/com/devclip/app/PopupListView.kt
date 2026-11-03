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
import android.widget.EditText
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
 * One shape, tethered to the bubble, at a smaller type scale — the same
 * surface it always was. It pastes, and the + opens a note at the top of it
 * that saves itself when the list closes. No search and no editing of
 * existing clips: those live in the full
 * app, where there is room for a keyboard and a result list.
 */
@SuppressLint("ViewConstructor")
class PopupListView(context: Context) : LinearLayout(context) {

    /**
     * Paste this clip into whatever field is focused underneath.
     *
     * Handed the row, not its text: the row may hold only a preview, and
     * what gets pasted has to be the whole clip.
     */
    var onPaste: ((DevClipDatabaseHelper.Clip) -> Unit)? = null

    /** The + was tapped: the service starts a note in this window. */
    var onAddNote: (() -> Unit)? = null

    /**
     * A note has started. The service makes this window able to take the
     * keyboard, which it otherwise deliberately cannot.
     */
    var onComposeStart: (() -> Unit)? = null

    /** The note's text changed. The service keeps it as a draft. */
    var onDraftChanged: ((String) -> Unit)? = null

    /**
     * A clip was tapped while a note was open. The service saves the note and
     * returns the list to pasting; the tap itself pastes nothing.
     */
    var onCommitRequested: (() -> Unit)? = null

    /** Open the launcher app, which also closes this window. */
    var onOpenFullApp: (() -> Unit)? = null

    /** Close this window. The bubble stays. */
    var onClose: (() -> Unit)? = null

    private var palette = DevClipTheme.colors(context)
    private val rows = LinearLayout(context)
    private var header: View? = null
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
    private val disarm = Runnable { setArmed(null) }

    /**
     * Whether a note is open at the top of the list.
     *
     * While it is, this window can take focus and the keyboard, and so a
     * paste from it would land in the note rather than in the app beneath.
     * That is why a row tap during a note saves the note instead of pasting.
     */
    var isComposing = false
        private set
    private var composer: View? = null
    private var noteField: EditText? = null
    private var noteError: View? = null
    private var noteFieldBg: GradientDrawable? = null

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

        /** How long an armed row stays armed. */
        private const val ARM_TIMEOUT_MS = 2000L
    }

    init {
        orientation = VERTICAL
        background = surface()
        // A rounded background alone does not clip children on a plain
        // LinearLayout; without this the first row's corners square off the
        // container they sit in.
        clipToOutline = true

        header = buildHeader()
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        rows.orientation = VERTICAL
        // 4dp at the sides. This window is a few hundred dp wide and every dp
        // of inset is a dp the clip text does not get — the keyline that makes
        // a full screen readable just makes a small floating one narrow.
        //
        // The bottom is 16dp and that is not symmetry, it is what buys the
        // 4dp. This view clips to a 26dp corner radius, so within 26dp of the
        // bottom the container's own edge curves inward — about 7dp at 8dp
        // up, which would slice the corner off a row inset only 4. Pushing
        // the last row 16dp clear drops that curve to roughly 2dp.
        rows.setPadding(
            dp(DevClipTheme.Spacing.XS), dp(DevClipTheme.Spacing.SM),
            dp(DevClipTheme.Spacing.XS), dp(DevClipTheme.Spacing.LG)
        )
        scroller.isFillViewport = true
        // No explicit params: a ScrollView is a FrameLayout underneath and
        // rejects the LinearLayout params this class's `LayoutParams` resolves
        // to, at layout time rather than at compile time.
        scroller.addView(rows)
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun surface(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(DevClipTheme.Radius.CONTAINER).toFloat()
        setColor(palette.bg)
        setStroke(dp(1), palette.border)
    }

    /**
     * Re-reads the theme and redraws in it.
     *
     * This window is built once and kept for the life of the service, which
     * outlives every screen that can change the theme. Without this, the
     * palette read when the service started was the palette the list kept:
     * switch DevClip to dark in Settings and the app went dark while the
     * floating list stayed white over the top of it, until the phone was
     * rebooted or the bubble stopped and started.
     *
     * Called on every open rather than only on a change, because a theme set
     * to follow the system can move without anything telling this service,
     * and rebuilding one header and a handful of rows costs nothing next to
     * the window it is already placing.
     */
    fun applyTheme() {
        palette = DevClipTheme.colors(context)
        background = surface()
        header?.let { removeView(it) }
        val rebuilt = buildHeader()
        header = rebuilt
        addView(rebuilt, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        if (isComposing) {
            // Rebuilt in the new palette with the same text and caret, so a
            // theme change mid-note costs nothing that was typed.
            val text = noteText()
            val caret = noteField?.selectionEnd ?: text.length
            composer?.let { removeView(it) }
            addComposer(text, restored = false)
            noteField?.setSelection(caret.coerceIn(0, text.length))
        }
        redraw()
    }

    private fun rebuildHeader() {
        header?.let { removeView(it) }
        val rebuilt = buildHeader()
        header = rebuilt
        addView(rebuilt, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun sp(view: TextView, value: Float) =
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)

    // ---- Header ----

    private fun buildHeader(): View {
        // No title and no dot. A window that appeared out of the bubble the
        // user just tapped does not need to introduce itself, and the row it
        // cost was a row of clips — which is the only thing this surface is
        // for. What is left is three controls and the rule under them.
        //
        // No padding above or below either: the row is exactly one touch
        // target tall. That is as slim as it goes without shrinking the
        // targets themselves, and a control floating over somebody else's app
        // is the last place to do that — a missed Close here taps whatever is
        // underneath, in an app DevClip does not own.
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setBackgroundColor(palette.bg)
        }

        // While a note is open the bar says so, and offers only the way out.
        // Close saves: there is no separate Save, because the user asked for
        // a note that keeps itself, and a second button would mean that
        // closing without pressing it could lose something.
        if (isComposing) {
            val label = TextView(context).apply {
                text = context.getString(R.string.note_new_heading)
                setTextColor(palette.ink)
                sp(this, DevClipTheme.MiniText.BODY)
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                setPadding(dp(DevClipTheme.Spacing.LG), 0, 0, 0)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    isAccessibilityHeading = true
                }
            }
            bar.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            bar.addView(
                iconButton(StrokeIcon.CLOSE, R.string.note_close_and_save) {
                    onClose?.invoke()
                }
            )
            return wrapHeader(bar)
        }

        // New note first, furthest from Close: the two do opposite things,
        // and a slip between neighbours should not turn one into the other.
        bar.addView(
            iconButton(StrokeIcon.ADD, R.string.note_add) {
                onAddNote?.invoke()
            }
        )
        bar.addView(
            iconButton(StrokeIcon.EXPAND, R.string.devclip_popup_open_full) {
                onOpenFullApp?.invoke()
            }
        )
        bar.addView(
            iconButton(StrokeIcon.CLOSE, R.string.devclip_popup_close) {
                onClose?.invoke()
            }
        )

        return wrapHeader(bar)
    }

    private fun wrapHeader(bar: View): View {
        val divider = View(context).apply { setBackgroundColor(palette.divider) }
        val wrap = LinearLayout(context).apply { orientation = VERTICAL }
        wrap.addView(
            bar,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(DevClipTheme.MIN_TOUCH_TARGET))
        )
        wrap.addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))
        return wrap
    }

    /**
     * A 48dp round tap target, which is One UI's floor. The icon inside is
     * 18dp; the target around it is not, because a control in a floating
     * window over somebody else's app is the last place to make a touch
     * target the size of its artwork.
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
        view.layoutParams =
            LayoutParams(dp(DevClipTheme.MIN_TOUCH_TARGET), dp(DevClipTheme.MIN_TOUCH_TARGET))
        return view
    }

    // ---- The note ----

    /**
     * Opens a note at the top of the list, or returns to the one already open.
     *
     * [draft] is what was being typed when a note was last interrupted —
     * the screen locked, a call came in, Android stopped the service — and
     * it comes back exactly as it was left.
     */
    fun startComposing(draft: String?) {
        if (isComposing) {
            focusNote()
            return
        }
        isComposing = true
        handler.removeCallbacks(disarm)
        armedId = null
        rebuildHeader()
        addComposer(draft.orEmpty(), restored = !draft.isNullOrBlank())
        redraw()
        // The clips step back while a note is open: still there to read, but
        // plainly not what a tap is for until the note is closed.
        scroller.alpha = 0.45f
        onComposeStart?.invoke()
        focusNote()
    }

    /** Closes the note, keeping nothing of it in this view. */
    fun stopComposing() {
        if (!isComposing) return
        hideKeyboard()
        isComposing = false
        composer?.let { removeView(it) }
        composer = null
        noteField = null
        noteError = null
        noteFieldBg = null
        scroller.alpha = 1f
        rebuildHeader()
        redraw()
    }

    fun noteText(): String = noteField?.text?.toString().orEmpty()

    /**
     * The save failed. The note stays, the field turns the error colour and
     * a line says what happened and what to do — words and an icon as well
     * as colour, and read out by TalkBack.
     */
    fun showSaveError() {
        noteFieldBg?.setStroke(dp(2), palette.danger)
        noteError?.visibility = View.VISIBLE
        announceForAccessibility(context.getString(R.string.note_save_failed_inline))
    }

    fun focusNote() {
        val field = noteField ?: return
        field.requestFocus()
        field.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(field, 0)
        }
    }

    private fun hideKeyboard() {
        val field = noteField ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(field.windowToken, 0)
    }

    private fun addComposer(text: String, restored: Boolean) {
        val built = buildComposer(text, restored)
        composer = built
        // Directly under the header, above the clips: the field has to stay
        // in the part of the window the keyboard does not reach.
        addView(built, 1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun buildComposer(text: String, restored: Boolean): View {
        val column = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(
                dp(DevClipTheme.Spacing.SM + 2), dp(DevClipTheme.Spacing.SM + 2),
                dp(DevClipTheme.Spacing.SM + 2), dp(DevClipTheme.Spacing.XS)
            )
        }

        val bg = GradientDrawable().apply {
            cornerRadius = dp(DevClipTheme.Radius.MD).toFloat()
            setColor(palette.surface)
            setStroke(dp(2), palette.accent)
        }
        noteFieldBg = bg

        val field = EditText(context).apply {
            setText(text)
            setSelection(text.length)
            // The hint names the field for TalkBack while it is empty; a
            // content description would replace what was typed when it reads.
            hint = context.getString(R.string.note_placeholder)
            setTextColor(palette.ink)
            setHintTextColor(palette.inkFaint)
            highlightColor = palette.accentSoft
            sp(this, DevClipTheme.MiniText.BODY)
            background = bg
            setPadding(dp(DevClipTheme.Spacing.MD), dp(10), dp(DevClipTheme.Spacing.MD), dp(10))
            gravity = Gravity.TOP or Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            // No full-screen extract editor in landscape: it would hide the
            // list and the app underneath, which is the point of typing here.
            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
            // Grows to six lines and then scrolls inside itself. No limit on
            // length: a pasted log saves in full.
            minHeight = dp(96)
            maxLines = 6
            isVerticalScrollBarEnabled = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // The platform caret is the theme's teal. An ink one, the way
                // every other mark on this surface is ink.
                textCursorDrawable = GradientDrawable().apply {
                    setColor(palette.ink)
                    setSize(dp(2), 0)
                }
            }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    // Any edit clears a failed save's warning: the next close
                    // is a fresh attempt.
                    if (noteError?.visibility == View.VISIBLE) {
                        noteError?.visibility = View.GONE
                        noteFieldBg?.setStroke(dp(2), palette.accent)
                    }
                    onDraftChanged?.invoke(s?.toString().orEmpty())
                }
            })
        }
        noteField = field
        column.addView(field, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        column.addView(
            statusLine(
                if (restored) StrokeIcon.HISTORY else StrokeIcon.CHECK,
                context.getString(if (restored) R.string.note_draft_restored else R.string.note_saves_on_close),
                palette.inkFaint
            )
        )
        val error = statusLine(
            StrokeIcon.ALERT,
            context.getString(R.string.note_save_failed_inline),
            palette.danger
        ).apply {
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        noteError = error
        column.addView(error)
        return column
    }

    /** An icon and a caption under the note: how it saves, or why it did not. */
    private fun statusLine(icon: StrokeIcon, text: String, color: Int): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(DevClipTheme.Spacing.XS), dp(6), dp(DevClipTheme.Spacing.XS), 0)
        }
        val glyph = StrokeIconView(context, icon, color, glyphDp = 14).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        row.addView(glyph, LayoutParams(dp(16), dp(16)))
        row.addView(
            TextView(context).apply {
                this.text = text
                setTextColor(color)
                sp(this, DevClipTheme.MiniText.CAPTION)
                setPadding(dp(6), 0, 0, 0)
            },
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        )
        return row
    }

    /**
     * Back closes the note — and so saves it — once the keyboard is down.
     * The first Back is the keyboard's own and only hides it.
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (isComposing && event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (event.action == android.view.KeyEvent.ACTION_UP) onClose?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
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
            sp(this, DevClipTheme.MiniText.CAPTION)
            gravity = Gravity.CENTER
            setPadding(dp(DevClipTheme.Spacing.LG), dp(40), dp(DevClipTheme.Spacing.LG), dp(40))
        }
        return text
    }

    private fun buildRow(clip: DevClipDatabaseHelper.Clip, position: Int): View {
        val armed = armedId == clip.id
        // A ring rather than a colour swap: the row has to stay readable while
        // it says the next tap will paste it.
        // The card's own gaps, kept small. Everything in this window is at the
        // reduced type scale already, so padding sized for a full-screen row
        // reads here as a card mostly made of air. Still tighter at the sides
        // than top and bottom: horizontal padding costs text, vertical
        // padding buys the separation that makes one row read as distinct
        // from the next.
        val padX = dp(6)
        val padY = dp(DevClipTheme.Spacing.SM)
        val card = LinearLayout(context).apply {
            orientation = HORIZONTAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(DevClipTheme.Radius.MD).toFloat()
                setColor(palette.surface)
                if (armed) setStroke(dp(2), palette.accent)
            }
            setPadding(padX, padY, padX, padY)
            isClickable = true
            isFocusable = true
        }

        val badge = TextView(context).apply {
            text = position.toString()
            setTextColor(palette.inkSoft)
            sp(this, DevClipTheme.MiniText.CAPTION)
            gravity = Gravity.CENTER
            // A minimum, not a size. At this window's type scale a two-digit
            // number fits in 20dp; at a large font size it does not, and a
            // fixed box clipped it. The badge grows with its number instead.
            minWidth = dp(DevClipTheme.Badge.MINI_MIN)
            minHeight = dp(DevClipTheme.Badge.MINI_MIN)
            includeFontPadding = false
            setPadding(dp(DevClipTheme.Spacing.XS), 0, dp(DevClipTheme.Spacing.XS), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(DevClipTheme.Radius.SM).toFloat()
                setColor(palette.surfaceSunken)
            }
        }
        card.addView(badge, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            rightMargin = dp(DevClipTheme.Spacing.SM)
            topMargin = dp(2)
        })

        val column = LinearLayout(context).apply { orientation = VERTICAL }

        if (!clip.title.isNullOrBlank()) {
            column.addView(TextView(context).apply {
                text = clip.title
                setTextColor(palette.ink)
                sp(this, DevClipTheme.MiniText.BODY)
                maxLines = 1
            })
        }

        column.addView(TextView(context).apply {
            text = clip.content
            setTextColor(palette.inkSoft)
            sp(this, DevClipTheme.MiniText.SECONDARY)
            maxLines = 2
        })

        column.addView(TextView(context).apply {
            text = context.getString(
                if (armed) R.string.devclip_popup_armed else R.string.devclip_popup_tap_to_paste
            )
            setTextColor(if (armed) palette.accent else palette.inkFaint)
            sp(this, DevClipTheme.MiniText.CAPTION)
        }.also { it.setPadding(0, dp(DevClipTheme.Spacing.XS), 0, 0) })

        card.addView(column, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        // The description replaces the visible "Tap again to paste", so it
        // has to carry the armed state itself or a screen reader never hears
        // that the next double-tap pastes.
        card.contentDescription = buildString {
            append(position).append(". ")
            if (!clip.title.isNullOrBlank()) append(clip.title).append(": ")
            append(clip.content)
            if (armed) append(". ").append(context.getString(R.string.devclip_popup_armed))
        }
        card.setOnClickListener { onRowTapped(clip) }

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.bottomMargin = dp(DevClipTheme.Spacing.SM)
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
        if (isComposing) {
            onCommitRequested?.invoke()
            return
        }
        if (!confirmBeforePaste) {
            onPaste?.invoke(clip)
            return
        }
        if (armedId == clip.id) {
            handler.removeCallbacks(disarm)
            setArmed(null)
            onPaste?.invoke(clip)
            return
        }
        setArmed(clip.id)
        announceForAccessibility(context.getString(R.string.devclip_popup_armed_announcement))
        handler.removeCallbacks(disarm)
        handler.postDelayed(disarm, ARM_TIMEOUT_MS)
    }

    /**
     * Moves the arm, rebuilding only the rows it leaves and lands on.
     *
     * It used to redraw the whole list — fifty rows of views torn down and
     * built again on every tap, to change the outline on at most two of them.
     */
    private fun setArmed(id: Long?) {
        val before = armedId
        armedId = id
        if (clips.isEmpty()) return
        listOfNotNull(before, id).distinct().forEach { changed ->
            val index = clips.indexOfFirst { it.id == changed }
            if (index < 0 || index >= rows.childCount) return@forEach
            val focused = rows.getChildAt(index).createAccessibilityNodeInfo().isAccessibilityFocused
            rows.removeViewAt(index)
            val row = buildRow(clips[index], index + 1)
            rows.addView(row, index)
            // Put TalkBack's focus back where it was, or it drops to the top
            // of the window every time a row is armed.
            if (focused) row.post {
                row.performAccessibilityAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                    null
                )
            }
        }
    }

    /** Releases the arm timer so a torn-down window leaves nothing pending. */
    fun release() {
        handler.removeCallbacks(disarm)
    }
}

/** The glyphs the floating list needs, drawn rather than shipped. */
enum class StrokeIcon { ADD, EXPAND, CLOSE, CHECK, HISTORY, ALERT }

/**
 * Draws an icon with strokes instead of loading a drawable.
 *
 * A handful of shapes at one weight is not worth a vector asset each, and drawing them
 * means the stroke width and colour follow the same tokens as everything else
 * on this surface rather than being baked into an XML file.
 */
@SuppressLint("ViewConstructor")
class StrokeIconView(
    context: Context,
    private val icon: StrokeIcon,
    tint: Int,
    private val glyphDp: Int = DevClipTheme.IconSize.SM
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * DevClipTheme.IconSize.STROKE_DP
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val glyph = glyphDp * density
        val left = (width - glyph) / 2f
        val top = (height - glyph) / 2f
        val right = left + glyph
        val bottom = top + glyph
        val inset = glyph * 0.18f

        when (icon) {
            StrokeIcon.ADD -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                canvas.drawLine(cx, top + inset, cx, bottom - inset, paint)
                canvas.drawLine(left + inset, cy, right - inset, cy, paint)
            }
            StrokeIcon.CHECK -> {
                val path = android.graphics.Path().apply {
                    moveTo(left + glyph * 0.2f, top + glyph * 0.52f)
                    lineTo(left + glyph * 0.42f, top + glyph * 0.72f)
                    lineTo(left + glyph * 0.8f, top + glyph * 0.3f)
                }
                canvas.drawPath(path, paint)
            }
            // A circle with the stroke left open at the top left and an
            // arrowhead there: going back to where you were.
            StrokeIcon.HISTORY -> {
                val r = glyph * 0.36f
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 200f, 300f, false, paint)
                canvas.drawLine(cx, cy - r * 0.5f, cx, cy, paint)
                canvas.drawLine(cx, cy, cx + r * 0.45f, cy + r * 0.3f, paint)
            }
            StrokeIcon.ALERT -> {
                val r = glyph * 0.4f
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                canvas.drawCircle(cx, cy, r, paint)
                canvas.drawLine(cx, cy - r * 0.5f, cx, cy + r * 0.1f, paint)
                canvas.drawPoint(cx, cy + r * 0.45f, paint)
            }
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
