package com.worldclock.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One complete set of app colours (spec section 8).
 *
 * The app ships five palettes — Light Monochrome, True Dark, Warm Sepia, Cool
 * Slate and High Contrast — and every screen paints with whichever one is
 * currently provided through [LocalWorldClockPalette]. Nothing falls back to a
 * Material default, so the Material 3 purple/blue accent can never appear.
 */
@Immutable
data class WorldClockPalette(
    /** App main surface. */
    val background: Color,
    /** Main text and primary actions. */
    val foreground: Color,
    /** City cards and elevated surfaces. */
    val card: Color,
    /** Selected card and active controls. */
    val primary: Color,
    /** Text and icons on [primary]. */
    val primaryForeground: Color,
    /**
     * Supporting text drawn on top of the selected card. Derived from
     * [primaryForeground] rather than being an independent colour.
     */
    val onPrimaryMuted: Color,
    /** Secondary controls and map grid. */
    val secondary: Color,
    /** Supporting text, offsets and labels. */
    val mutedForeground: Color,
    /** Soft selected surfaces. */
    val accent: Color,
    /** Outlines, separators and the map graticule. */
    val border: Color,
    /** Map meridian, active pin, and the remove (destructive) action. */
    val mapAccent: Color,
    /**
     * The landmasses on the schematic map.
     *
     * Separate from [accent] — which is also the fill of the app's soft pills —
     * because the two pull in opposite directions in a black-and-white theme:
     * the pills need a near-black fill so white text stays readable on them,
     * while the landmasses need to read *against* an ocean that is itself
     * [card]. High Contrast is the only theme where they differ.
     */
    val mapLandmass: Color,
    /**
     * The wash drawn over the half of the globe that is in shadow.
     *
     * The alpha lives in the value because darkness has to be painted as
     * darkness in every theme — a light black wash over a light map, a heavier
     * black wash over a dark one. Deriving it from [foreground] would wash a
     * dark map *lighter*, which reads as daylight.
     *
     * A black wash cannot show over a pure-black ocean, which is why High
     * Contrast paints its landmasses light: the shadow stays visible on the
     * continents (see `WorldClockPaletteTest`).
     */
    val mapNight: Color,
    /** True for the dark palettes. */
    val isDark: Boolean,
)

/**
 * Theme 1 — light monochrome. These are the original spec values and are
 * unchanged, so light-mode rendering is byte-for-byte what it was.
 */
val LightWorldClockPalette = WorldClockPalette(
    background = Color(0xFFF3F3F1),
    foreground = Color(0xFF111111),
    card = Color(0xFFFAFAF8),
    primary = Color(0xFF111111),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0x9EFFFFFF),
    secondary = Color(0xFFE8E8E6),
    mutedForeground = Color(0xFF777774),
    accent = Color(0xFFE3E3E0),
    border = Color(0xFFD9D9D6),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFE3E3E0),
    mapNight = Color(0xFF111111).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * Theme 2 — true dark.
 *
 * The first four values are the agreed dark tokens. The rest are derived so the
 * dark theme keeps the same relationships the light theme uses:
 *
 *  - [WorldClockPalette.primary] stays the *inverse* of the base surface, so the
 *    selected city card is a light card with dark text — the mirror of the
 *    light theme's black card with light text.
 *  - [WorldClockPalette.secondary], [WorldClockPalette.accent] and
 *    [WorldClockPalette.border] step up from [WorldClockPalette.card] by the
 *    same kind of increments the light theme uses.
 *  - [WorldClockPalette.mapAccent] is deliberately unchanged: the single warm
 *    accent has enough contrast against every background and is the app's one
 *    non-monochrome element.
 */
val DarkWorldClockPalette = WorldClockPalette(
    background = Color(0xFF111111),
    foreground = Color(0xFFE1E1DE),
    card = Color(0xFF1A1A1A),
    primary = Color(0xFFE1E1DE),
    primaryForeground = Color(0xFF111111),
    onPrimaryMuted = Color(0x9E111111),
    secondary = Color(0xFF232323),
    mutedForeground = Color(0xFF777774),
    accent = Color(0xFF262626),
    border = Color(0xFF2E2E2E),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFF262626),
    mapNight = Color(0xFF000000).copy(alpha = 0.45f),
    isDark = true,
)

/**
 * Theme 3 — warm sepia.
 *
 * Built on the light theme's structure with the surface turned to paper and the
 * text to warm ink: the card is a shade lighter than the background, and
 * [WorldClockPalette.secondary], [WorldClockPalette.accent] and
 * [WorldClockPalette.border] step down from the background in the same
 * increments. The warm accent is shared with every other theme so the brand
 * colour does not shift; the night wash is tinted with the ink rather than pure
 * black, so the shadow over the map stays warm.
 */
val WarmSepiaWorldClockPalette = WorldClockPalette(
    background = Color(0xFFF7F4EF),
    foreground = Color(0xFF2C2521),
    card = Color(0xFFFCFAF6),
    primary = Color(0xFF2C2521),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0x9EFFFFFF),
    secondary = Color(0xFFEFEAE2),
    mutedForeground = Color(0xFF7A7065),
    accent = Color(0xFFEDE7DD),
    border = Color(0xFFE2DACE),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFEDE7DD),
    mapNight = Color(0xFF2C2521).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * Theme 4 — cool slate.
 *
 * The same structure again, shifted to a blue-grey: a near-white card on a
 * muted surface, with the supporting greys taken from the slate family so the
 * whole screen stays cool. Supporting text is a little darker than the light
 * theme's muted grey, because a blue-grey background reflects more light.
 */
val CoolSlateWorldClockPalette = WorldClockPalette(
    background = Color(0xFFEEF2F5),
    foreground = Color(0xFF1E293B),
    card = Color(0xFFF8FAFC),
    primary = Color(0xFF1E293B),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0x9EFFFFFF),
    secondary = Color(0xFFE4EAEF),
    mutedForeground = Color(0xFF64748B),
    accent = Color(0xFFDDE5EB),
    border = Color(0xFFCFD9E1),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFDDE5EB),
    mapNight = Color(0xFF1E293B).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * Theme 5 — high contrast.
 *
 * Pure black and pure white for the surfaces, the text and the outlines, with
 * the supporting greys chosen only where pure black or white would make two
 * things indistinguishable. Two consequences are deliberate:
 *
 *  - [WorldClockPalette.card] is the same black as the background, so cards are
 *    separated by their [WorldClockPalette.border] outlines rather than by a
 *    fill. That is the point of the theme, and it is why this is the one dark
 *    palette a "cards are lighter than the background" check does not apply to.
 *  - [WorldClockPalette.mapLandmass] is white, not [WorldClockPalette.accent]:
 *    a black night wash over a black ocean is invisible, so the continents are
 *    drawn light and carry the terminator.
 */
val HighContrastWorldClockPalette = WorldClockPalette(
    background = Color(0xFF000000),
    foreground = Color(0xFFFFFFFF),
    card = Color(0xFF000000),
    primary = Color(0xFFFFFFFF),
    primaryForeground = Color(0xFF000000),
    onPrimaryMuted = Color(0x9E000000),
    secondary = Color(0xFF1A1A1A),
    mutedForeground = Color(0xFFC4C4C4),
    accent = Color(0xFF1F1F1F),
    border = Color(0xFFFFFFFF),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFFFFFFF),
    mapNight = Color(0xFF000000).copy(alpha = 0.40f),
    isDark = true,
)

/**
 * The palette in effect for the current theme.
 *
 * Provided by [WorldClockTheme]; use [WorldClockColors] to read it.
 */
val LocalWorldClockPalette = staticCompositionLocalOf { LightWorldClockPalette }

/**
 * The active palette, read through Material-style composable getters.
 *
 * This mirrors `MaterialTheme.colorScheme`: the properties read the composition
 * local rather than holding constants, so a screen written once paints
 * correctly in every theme. Because they read a composition local they
 * are only accessible from a composable scope — non-composable drawing code
 * receives a [WorldClockPalette] as a parameter instead (see `WorldMapCanvas`).
 */
object WorldClockColors {
    /** App main surface. */
    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.background

    /** Main text and primary actions. */
    val Foreground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.foreground

    /** City cards and elevated surfaces. */
    val Card: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.card

    /** Selected card and active controls. */
    val Primary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.primary

    /** Text and icons on [Primary]. */
    val PrimaryForeground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.primaryForeground

    /** Supporting text on the selected card. */
    val OnPrimaryMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.onPrimaryMuted

    /** Secondary controls and map grid. */
    val Secondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.secondary

    /** Supporting text, offsets and labels. */
    val MutedForeground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.mutedForeground

    /** Soft selected surfaces. */
    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.accent

    /** Outlines, separators and the map graticule. */
    val Border: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.border

    /** Map meridian, active pin and the destructive action. */
    val MapAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.mapAccent

    /** The continents on the schematic map. */
    val MapLandmass: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.mapLandmass

    /** The wash drawn over the half of the globe in shadow. */
    val MapNight: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalWorldClockPalette.current.mapNight
}
