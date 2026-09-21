package com.devclip.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devclip.app.DevClipTheme

/**
 * The design system, for Compose.
 *
 * Deliberately thin. Every value comes from [DevClipTheme], which the
 * floating windows also draw with — this converts them into Compose types and
 * publishes them, rather than restating them. Two palettes that agree today
 * are two palettes that disagree eventually.
 *
 * Material3 is here for its components, not its colour. Its scheme is filled
 * in from the same monochrome roles so that anything reaching for
 * `MaterialTheme.colorScheme` lands on DevClip's colours rather than
 * Material's purple.
 */

/** The colour roles, as Compose sees them. */
data class DevClipColors(
    val bg: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val border: Color,
    val divider: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val inkDisabled: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
    val isDark: Boolean
)

/**
 * One UI spacing. Named rather than inlined: a 16.dp written at a call site
 * is a number, and a number is the thing that drifts off the scale.
 */
object Space {
    val xs = DevClipTheme.Spacing.XS.dp
    val sm = DevClipTheme.Spacing.SM.dp
    val md = DevClipTheme.Spacing.MD.dp
    val lg = DevClipTheme.Spacing.LG.dp
    val xl = DevClipTheme.Spacing.XL.dp

    /** 24dp from each screen edge. The most important rule in One UI. */
    val keyline = DevClipTheme.Spacing.KEYLINE.dp
}

object Radius {
    val xs = DevClipTheme.Radius.XS.dp
    val sm = DevClipTheme.Radius.SM.dp
    val md = DevClipTheme.Radius.MD.dp
    val lg = DevClipTheme.Radius.LG.dp

    /** One UI buttons are pills. A 4dp button reads as Material. */
    val pill = DevClipTheme.Radius.PILL.dp
    val container = DevClipTheme.Radius.CONTAINER.dp
}

/** The minimum touch target. A control may be drawn smaller; its target may not. */
val MinTouchTarget = DevClipTheme.MIN_TOUCH_TARGET.dp

private val LocalDevClipColors = staticCompositionLocalOf<DevClipColors> {
    error("DevClipComposeTheme is missing from the tree")
}

/** The tokens, from anywhere under [DevClipComposeTheme]. */
object Tokens {
    val colors: DevClipColors
        @Composable @ReadOnlyComposable get() = LocalDevClipColors.current
}

/**
 * The One UI type scale.
 *
 * Mapped onto Material's slots so Material components inherit it, with the
 * roles DevClip actually uses named by what they are for. Sizes are in sp
 * throughout, so everything follows the user's font size setting; a dp here
 * would make this the one app on the phone that ignores it.
 */
private val DevClipTypography = Typography(
    // The expanded app bar title, which collapses into the bar on scroll.
    displaySmall = TextStyle(
        fontSize = DevClipTheme.Text.DISPLAY.sp,
        fontWeight = FontWeight.Normal
    ),
    // Section and card titles.
    titleMedium = TextStyle(
        fontSize = DevClipTheme.Text.TITLE.sp,
        fontWeight = FontWeight.Medium
    ),
    // Body, list primary, button label. The workhorse.
    bodyLarge = TextStyle(
        fontSize = DevClipTheme.Text.BODY.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = DevClipTheme.Text.BUTTON.sp,
        fontWeight = FontWeight.Medium
    ),
    // Secondary and supporting.
    bodyMedium = TextStyle(
        fontSize = DevClipTheme.Text.SECONDARY.sp,
        fontWeight = FontWeight.Normal
    ),
    // Caption and metadata.
    bodySmall = TextStyle(
        fontSize = DevClipTheme.Text.CAPTION.sp,
        fontWeight = FontWeight.Normal
    ),
    labelSmall = TextStyle(
        fontSize = DevClipTheme.Text.MICRO.sp,
        fontWeight = FontWeight.Normal
    )
)

@Composable
fun DevClipComposeTheme(content: @Composable () -> Unit) {
    // Read through the same function the floating windows use, so the app and
    // the bubble cannot end up on different sides of a theme change.
    val context = LocalContext.current
    val palette = DevClipTheme.colors(context)
    val isDark = DevClipTheme.isDark(context)

    val colors = DevClipColors(
        bg = Color(palette.bg),
        surface = Color(palette.surface),
        surfaceSunken = Color(palette.surfaceSunken),
        border = Color(palette.border),
        divider = Color(palette.divider),
        ink = Color(palette.ink),
        inkSoft = Color(palette.inkSoft),
        inkFaint = Color(palette.inkFaint),
        inkDisabled = Color(palette.inkDisabled),
        accent = Color(palette.accent),
        onAccent = Color(palette.onAccent),
        accentSoft = Color(palette.accentSoft),
        success = Color(palette.success),
        warning = Color(palette.warning),
        danger = Color(palette.danger),
        scrim = Color(palette.scrim),
        isDark = isDark
    )

    // Material's scheme, filled from the same roles. Anything that reaches
    // for MaterialTheme.colorScheme gets DevClip's monochrome rather than
    // Material's default palette.
    val scheme = if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceSunken,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.border,
            error = colors.danger
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceSunken,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.border,
            error = colors.danger
        )
    }

    CompositionLocalProvider(LocalDevClipColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DevClipTypography,
            content = content
        )
    }
}
