package com.devclip.app

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Reads the text the user has highlighted, anywhere on the phone.
 *
 * This is the whole design. DevClip never reads the clipboard to capture,
 * because since Android 10 it cannot: `getPrimaryClip()` returns null unless
 * the calling app has input focus or is the default keyboard, and an
 * accessibility service gets no exemption from that. The bubble's own window
 * is deliberately non-focusable, so DevClip is never "in front" and never will
 * be. Reading the selection out of the accessibility node tree sidesteps the
 * clipboard entirely and needs no permission the service does not already
 * hold: `canRetrieveWindowContent` and `flagRetrieveInteractiveWindows` are
 * exactly this.
 *
 * Two paths, both built from the start:
 *
 *  1. [liveRead] walks the node tree on demand. This is the one that should
 *     always work, and it is the one that runs first.
 *  2. [remembered] is a cache fed by selection-changed events. It exists
 *     because the whole feature rests on an assumption that cannot be checked
 *     without a device — that tapping the bubble does not clear the user's
 *     selection. It should not: the bubble window is FLAG_NOT_FOCUSABLE and
 *     never takes input focus, which is the same property that makes pasting
 *     into another app's field work. But "should not" is not "does not", and
 *     discovering it late would mean discovering it with no fallback in place.
 *
 * A tree walk on a tap is fine. It is a gesture, not a hot loop.
 */
object SelectionCapture {

    /** What a read produced, and why, when it produced nothing. */
    sealed class Result {
        data class Text(val value: String) : Result()

        /** A selection was found in a password field and deliberately skipped. */
        object Password : Result()

        /** Nothing is selected — the tap means "open the list" instead. */
        object None : Result()
    }

    private data class Remembered(val text: String, val atMillis: Long)

    /**
     * How long a remembered selection stays usable.
     *
     * Long enough to cover reaching for the bubble, short enough that a
     * selection made a minute ago in another app is not silently captured as
     * if it were the one on screen now.
     */
    private const val REMEMBERED_MAX_AGE_MS = 20_000L

    /** How deep the node walk goes before giving up. */
    private const val MAX_DEPTH = 60

    /** How many nodes the walk will visit before giving up. */
    private const val MAX_NODES = 3_000

    @Volatile
    private var remembered: Remembered? = null

    /**
     * Whether a selection is believed to be live right now.
     *
     * Drives the bubble's appearance, so the user is told that tapping will
     * capture rather than left to discover it. This is the honest signal
     * available without walking the tree continuously: it follows the
     * selection-changed events, and expires with them.
     */
    @Volatile
    var listener: ((Boolean) -> Unit)? = null

    val hasLiveSelection: Boolean
        get() = remembered.let { it != null && !it.isStale() }

    private fun Remembered.isStale(): Boolean =
        System.currentTimeMillis() - atMillis > REMEMBERED_MAX_AGE_MS

    /**
     * Feed from `TYPE_VIEW_TEXT_SELECTION_CHANGED`.
     *
     * Events where `fromIndex == toIndex` are cursor moves, not selections —
     * they fire on every keystroke in every edit field on the phone — so they
     * clear the cache rather than filling it.
     */
    fun onSelectionEvent(event: AccessibilityEvent) {
        val from = event.fromIndex
        val to = event.toIndex
        if (from < 0 || to < 0 || from == to) {
            forget()
            return
        }

        val source = try {
            event.source
        } catch (e: Exception) {
            null
        }
        if (source?.isPassword == true) {
            forget()
            return
        }

        // The event carries the field's whole text in event.text; the indices
        // are into that. Prefer the source node's text, which is current, and
        // fall back to the event's copy when the node is gone by the time this
        // runs.
        val whole = source?.text?.toString() ?: event.text.firstOrNull()?.toString()
        val selected = slice(whole, from, to)
        if (selected.isNullOrEmpty()) {
            forget()
            return
        }

        val wasLive = hasLiveSelection
        remembered = Remembered(selected, System.currentTimeMillis())
        if (!wasLive) listener?.invoke(true)
    }

    fun forget() {
        val wasLive = hasLiveSelection
        remembered = null
        if (wasLive) listener?.invoke(false)
    }

    /**
     * Reads the current selection, live first and from the cache second.
     *
     * [service] is the running accessibility service, or null when the user
     * has not enabled it — in which case there is no capture at all and the
     * bubble tap simply opens the list.
     */
    fun capture(service: ClipboardAccessibilityService?): Result {
        if (service == null) return Result.None

        when (val live = liveRead(service)) {
            is Result.Text -> {
                forget()
                return live
            }
            // A password field with a live selection is a definite answer:
            // do not fall through and capture a stale cache entry instead.
            is Result.Password -> {
                forget()
                return live
            }
            is Result.None -> Unit
        }

        val cached = remembered
        if (cached != null && !cached.isStale()) {
            forget()
            return Result.Text(cached.text)
        }
        forget()
        return Result.None
    }

    /**
     * Walks the active window for a node with a non-empty selection.
     *
     * Breadth-first with hard caps on both depth and node count: this runs on
     * the main thread in response to a tap, and a pathological tree (a long
     * chat, a complex web page) must not be able to make the bubble feel
     * stuck. Missing a selection is recoverable — the cache is the fallback,
     * and the user can select again — a frozen bubble is not.
     *
     * Nothing is recycled. AccessibilityNodeInfo's object pool was removed and
     * recycle() deprecated in API 33; recycling a node the walk is still
     * holding is now a way to crash, not a way to save memory.
     */
    private fun liveRead(service: ClipboardAccessibilityService): Result {
        val root = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return Result.None

        // A plain list with a read cursor rather than a Deque: this is a
        // bounded walk, and the cursor costs nothing next to the tree.
        val queue = ArrayList<AccessibilityNodeInfo>()
        val depths = ArrayList<Int>()
        queue.add(root)
        depths.add(0)
        var cursor = 0
        var sawPassword = false

        while (cursor < queue.size && cursor < MAX_NODES) {
            val node = queue[cursor]
            val depth = depths[cursor]
            cursor++

            try {
                val from = node.textSelectionStart
                val to = node.textSelectionEnd
                if (from >= 0 && to >= 0 && from != to) {
                    if (node.isPassword) {
                        // Saving a row of bullets helps nobody, and saving the
                        // real thing is worse. Keep looking in case another
                        // node holds an ordinary selection; if none does, the
                        // password is the answer and the caller says so.
                        sawPassword = true
                    } else {
                        val selected = slice(node.text?.toString(), from, to)
                        if (!selected.isNullOrEmpty()) return Result.Text(selected)
                    }
                }

                if (depth < MAX_DEPTH) {
                    for (i in 0 until node.childCount) {
                        val child = try {
                            node.getChild(i)
                        } catch (e: Exception) {
                            null
                        }
                        if (child != null) {
                            queue.add(child)
                            depths.add(depth + 1)
                        }
                    }
                }
            } catch (e: Exception) {
                // A node can go away underneath the walk. Skip it.
            }
        }

        return if (sawPassword) Result.Password else Result.None
    }

    /**
     * Cuts [from]..[to] out of [whole], tolerating indices that no longer fit.
     *
     * The indices and the text can come from different moments — the node may
     * have changed between the event and this call — so an out-of-range pair
     * means "no answer", not a crash.
     */
    private fun slice(whole: String?, from: Int, to: Int): String? {
        if (whole == null) return null
        val start = minOf(from, to).coerceIn(0, whole.length)
        val end = maxOf(from, to).coerceIn(0, whole.length)
        if (start >= end) return null
        return whole.substring(start, end)
    }
}
