package com.devclip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devclip.app.DevClipTheme

/**
 * The One UI app bar: a title that grows when the screen is pulled down, and
 * controls that come down with it.
 *
 * Material's LargeTopAppBar looks similar and is not the same thing. It
 * collapses as a list scrolls and re-expands when the list returns to the
 * top, but it has nothing to say about a list that is already at the top —
 * and that is the whole gesture. On a Samsung phone, pulling down at the top
 * of any screen brings the bar's controls into reach of a thumb, whether or
 * not there is anything left to scroll. It is a reach affordance on a tall
 * phone, not a scroll effect.
 *
 * Hence [onPostScroll] rather than only [onPreScroll]. Drag downwards at the
 * top of a list and the list consumes nothing, so the whole delta arrives
 * here unspent; that leftover is exactly the "there is nowhere left to go"
 * signal the gesture needs. Dragging up collapses the bar first and moves the
 * list afterwards, which is the order One UI uses.
 *
 * Collapsed, the title and the controls share one row. Expanded, the title
 * rises and grows and the controls sit at the bottom edge, just above the
 * content — near the thumb, which is the point.
 */
@Stable
class OneUiHeaderState(
    internal val rangePx: Float,
    val collapsedHeight: Dp,
    val expandedHeight: Dp
) {
    /** 0 is collapsed to a plain bar, 1 is fully open. */
    var fraction by mutableFloatStateOf(0f)
        internal set

    private fun drag(dy: Float): Offset {
        if (rangePx <= 0f) return Offset.Zero
        val before = fraction
        fraction = (fraction + dy / rangePx).coerceIn(0f, 1f)
        return Offset(0f, (fraction - before) * rangePx)
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        // Closing happens before the list moves: an upward drag shuts the bar
        // first and only then scrolls, so the content does not jump away
        // underneath the finger while the title is still shrinking.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y >= 0f || fraction <= 0f) Offset.Zero else drag(available.y)

        // Opening happens after: the bar grows only out of a drag the list
        // could not use, which is what makes the gesture mean "you are at the
        // top" rather than "you are scrolling".
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset =
            if (available.y <= 0f || fraction >= 1f) Offset.Zero else drag(available.y)
    }
}

object OneUiHeaderDefaults {
    /** The height of an ordinary app bar, and of the collapsed one. */
    val CollapsedHeight = 56.dp

    /**
     * How tall the bar opens.
     *
     * A share of the screen rather than a fixed number, because the gesture
     * exists to bring controls within thumb reach and a thumb's reach is a
     * share of the screen.
     *
     * It was 30% and the pull did not feel like it went anywhere. The title
     * has to travel far enough to read as having moved into the middle of the
     * screen, and the controls have to end up somewhere a thumb can actually
     * get to — which on a phone this tall is not the top third. The floor is
     * what keeps that true on a small screen, where 44% of not very much is
     * still not very far.
     *
     * The floor gives way on a short window. A phone on its side is about
     * 380dp tall, and a 300dp floor there opened the bar over nearly all of
     * it; half the window is as far as it goes.
     */
    @Composable
    fun expandedHeight(): Dp {
        val height = LocalConfiguration.current.screenHeightDp.toFloat()
        val floor = minOf(300f, height * 0.5f)
        return (height * 0.44f).coerceIn(floor, 460f).dp
    }
}

@Composable
fun rememberOneUiHeaderState(
    collapsedHeight: Dp = OneUiHeaderDefaults.CollapsedHeight,
    expandedHeight: Dp = OneUiHeaderDefaults.expandedHeight()
): OneUiHeaderState {
    val rangePx = with(LocalDensity.current) { (expandedHeight - collapsedHeight).toPx() }
    return remember(rangePx) { OneUiHeaderState(rangePx, collapsedHeight, expandedHeight) }
}

/**
 * @param actionsWidth how much room to keep clear on the right while the bar
 *   is collapsed, so the title does not run under the controls sharing its
 *   row. Nothing measures it for us: the actions are a slot, and a slot has
 *   no width until it is laid out.
 */
@Composable
fun OneUiHeader(
    title: String,
    state: OneUiHeaderState,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actionsWidth: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = Tokens.colors
    val f = state.fraction
    val bar = state.collapsedHeight

    val height = bar + (state.expandedHeight - bar) * f

    // The title's own band: the whole bar while collapsed, everything above
    // the control row once open. Centred in it either way, so one value
    // places the title in both states without a second layout.
    val titleBand = bar + ((state.expandedHeight - bar) - bar) * f

    val titleSize = DevClipTheme.Text.TITLE +
        (DevClipTheme.Text.HERO - DevClipTheme.Text.TITLE) * f

    // Collapsed, the title shares its row with the navigation icon and the
    // actions and has to clear both. Open, it is alone on its band and sits
    // on the keyline like everything else.
    val navReserve = if (navigationIcon == null) Space.keyline else 56.dp
    val actionReserve = if (actionsWidth > Space.keyline) actionsWidth else Space.keyline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.bg)
    ) {
        if (navigationIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(bar)
                    .padding(start = Space.xs),
                contentAlignment = Alignment.Center
            ) { navigationIcon() }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(titleBand)
                .padding(
                    start = navReserve + (Space.keyline - navReserve) * f,
                    end = actionReserve + (Space.keyline - actionReserve) * f
                ),
            // Travels as the bar opens. Collapsed it is left-aligned, sharing
            // its row with the navigation icon and the actions; open it is
            // alone on a band of its own, and left-aligned there just looks
            // dropped in a corner. A bias rather than two alignments, because
            // the point is the journey: the title has to move *with* the
            // finger, not jump when the drag ends.
            //
            // -1 is start, 0 is centre. The padding is symmetrical by the
            // time f reaches 1, so centring inside it is centring on screen.
            contentAlignment = BiasAlignment(
                horizontalBias = -1f + f,
                verticalBias = 0f
            )
        ) {
            Text(
                text = title,
                color = colors.ink,
                fontSize = titleSize.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(bar)
                .padding(end = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/** Room for [count] icon buttons, for [OneUiHeader]'s `actionsWidth`. */
fun actionsWidthFor(count: Int): Dp = MinTouchTarget * count + 8.dp

/** A header action: an icon at One UI's minimum target size. */
@Composable
fun HeaderIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colors = Tokens.colors
    IconButton(
        onClick = onClick,
        modifier = Modifier.width(MinTouchTarget).height(MinTouchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.ink
        )
    }
}
