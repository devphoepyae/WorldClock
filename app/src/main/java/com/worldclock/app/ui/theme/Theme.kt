package com.worldclock.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.worldclock.app.data.prefs.AppTheme

/**
 * The palette a theme paints with.
 *
 * Pure, so the five-way mapping is unit-testable without a device or a
 * Robolectric configuration, and so there is exactly one place that decides
 * what a theme looks like.
 */
fun paletteFor(theme: AppTheme): WorldClockPalette = when (theme) {
    AppTheme.LIGHT_MONOCHROME -> LightWorldClockPalette
    AppTheme.TRUE_DARK -> DarkWorldClockPalette
    AppTheme.WARM_SEPIA -> WarmSepiaWorldClockPalette
    AppTheme.COOL_SLATE -> CoolSlateWorldClockPalette
    AppTheme.HIGH_CONTRAST -> HighContrastWorldClockPalette
}

/**
 * Material 3 colour scheme with every role pinned to [palette].
 *
 * Every slot is listed explicitly — including the `surfaceContainer*` roles —
 * so no component can fall back to a default Material colour. The defaults of
 * `lightColorScheme()` / `darkColorScheme()` are only a starting point here;
 * nothing is inherited from them.
 */
internal fun WorldClockPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = primaryForeground,
        primaryContainer = accent,
        onPrimaryContainer = foreground,
        inversePrimary = secondary,

        secondary = secondary,
        onSecondary = foreground,
        secondaryContainer = accent,
        onSecondaryContainer = foreground,

        tertiary = mapAccent,
        onTertiary = primaryForeground,
        tertiaryContainer = accent,
        onTertiaryContainer = foreground,

        background = background,
        onBackground = foreground,

        surface = card,
        onSurface = foreground,
        surfaceVariant = secondary,
        onSurfaceVariant = mutedForeground,

        // Keeps elevation overlays neutral instead of tinting towards purple.
        surfaceTint = foreground,
        inverseSurface = foreground,
        inverseOnSurface = card,

        error = mapAccent,
        onError = primaryForeground,
        errorContainer = accent,
        onErrorContainer = foreground,

        outline = border,
        outlineVariant = border,
        scrim = Color(0xFF000000),

        surfaceBright = card,
        surfaceDim = background,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = secondary,
        surfaceContainerLow = card,
        surfaceContainerLowest = primaryForeground,
    )
}

/**
 * Root theme for the app.
 *
 * Provides the palette of [theme] through [LocalWorldClockPalette] and wraps
 * content in [MaterialTheme] so Material components inherit the app's colours,
 * Inter typography and 16 dp corner radius while still providing correct
 * accessibility semantics.
 *
 * The device's light/dark setting is deliberately ignored: the user has chosen
 * an exact palette, and repainting it because the phone switched at sunset
 * would throw that choice away.
 *
 * @param theme the user's choice; defaults to Light Monochrome
 */
@Composable
fun WorldClockTheme(
    theme: AppTheme = AppTheme.Default,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(theme)

    CompositionLocalProvider(LocalWorldClockPalette provides palette) {
        // Applied inside the provider so the icons are resolved from the palette
        // that is actually in effect, not from the device setting.
        SystemBarAppearance()
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = WorldClockTypography,
            shapes = WorldClockShapes,
            content = content,
        )
    }
}
