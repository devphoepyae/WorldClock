package com.worldclock.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.worldclock.app.data.prefs.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks down all five palettes and the Material 3 colour scheme built from them
 * (spec section 8).
 *
 * The contrast checks are deliberately written as a ratio rather than as a set
 * of hard-coded hex values: they assert the *relationships* the design relies
 * on, so a future token tweak that breaks readability fails here instead of on
 * a screenshot. Every check runs over every theme, so the three themes added
 * after the first two cannot quietly be held to a lower bar.
 */
class WorldClockPaletteTest {

    /** Every shipped theme, in the order the design lists them. */
    private val palettes = AppTheme.entries.map(::paletteFor)

    // --- The agreed values ---------------------------------------------------

    @Test
    fun theLightMonochromePaletteKeepsTheOriginalSpecValues() {
        assertEquals(Color(0xFFF3F3F1), LightWorldClockPalette.background)
        assertEquals(Color(0xFFFAFAF8), LightWorldClockPalette.card)
        assertEquals(Color(0xFF111111), LightWorldClockPalette.foreground)
        assertEquals(Color(0xFF777774), LightWorldClockPalette.mutedForeground)
        assertEquals(false, LightWorldClockPalette.isDark)
    }

    @Test
    fun theTrueDarkPaletteUsesTheAgreedDarkTokens() {
        assertEquals(Color(0xFF111111), DarkWorldClockPalette.background)
        assertEquals(Color(0xFF1A1A1A), DarkWorldClockPalette.card)
        assertEquals(Color(0xFFE1E1DE), DarkWorldClockPalette.foreground)
        assertEquals(Color(0xFF777774), DarkWorldClockPalette.mutedForeground)
        assertEquals(true, DarkWorldClockPalette.isDark)
    }

    @Test
    fun theWarmSepiaPaletteUsesTheAgreedPaperAndInk() {
        assertEquals(Color(0xFFF7F4EF), WarmSepiaWorldClockPalette.background)
        assertEquals(Color(0xFF2C2521), WarmSepiaWorldClockPalette.foreground)
        assertEquals(false, WarmSepiaWorldClockPalette.isDark)
        // The card is a lighter shade of the paper, as it is in the light
        // monochrome theme.
        assertTrue(
            luminance(WarmSepiaWorldClockPalette.card) > luminance(WarmSepiaWorldClockPalette.background),
        )
    }

    @Test
    fun theCoolSlatePaletteUsesTheAgreedSurfaceAndText() {
        assertEquals(Color(0xFFEEF2F5), CoolSlateWorldClockPalette.background)
        assertEquals(Color(0xFF1E293B), CoolSlateWorldClockPalette.foreground)
        assertEquals(false, CoolSlateWorldClockPalette.isDark)
        assertTrue(
            luminance(CoolSlateWorldClockPalette.card) > luminance(CoolSlateWorldClockPalette.background),
        )
    }

    @Test
    fun theHighContrastPaletteIsPureBlackAndWhite() {
        assertEquals(Color(0xFF000000), HighContrastWorldClockPalette.background)
        assertEquals(Color(0xFF000000), HighContrastWorldClockPalette.card)
        assertEquals(Color(0xFFFFFFFF), HighContrastWorldClockPalette.foreground)
        assertEquals(Color(0xFFFFFFFF), HighContrastWorldClockPalette.primary)
        assertEquals(Color(0xFF000000), HighContrastWorldClockPalette.primaryForeground)
        assertEquals(true, HighContrastWorldClockPalette.isDark)
    }

    // --- The relationships every theme keeps ---------------------------------

    @Test
    fun everyThemeSelectsWithTheInverseOfItsSurface() {
        // The selected city card is always the mirror of the base surface: a
        // black card on the light themes, a white card on the dark ones.
        palettes.forEach { palette ->
            assertEquals(palette.foreground, palette.primary)
        }
    }

    @Test
    fun everyDarkThemeIsSeparatedFromItsBackgroundBySomething() {
        // The dark themes may not lift the card the way the light ones do —
        // High Contrast keeps it pure black on purpose — but then the outline
        // has to carry the separation, so no dark theme may have both a
        // background-coloured card and a border that vanishes into it.
        palettes.filter { it.isDark }.forEach { palette ->
            val cardReads = cellIsLighterThanTheBackground(palette)
            val outlinesRead = luminance(palette.border) > luminance(palette.background)
            assertTrue(
                "a dark theme's cards are neither lifted nor outlined",
                cardReads || outlinesRead,
            )
        }

        // The dark monochrome theme lifts the card rather than outlining it.
        assertTrue(cellIsLighterThanTheBackground(DarkWorldClockPalette))
        assertNotEquals(DarkWorldClockPalette.card, DarkWorldClockPalette.background)
    }

    @Test
    fun theAccentIsSharedSoTheBrandColourDoesNotShift() {
        // One warm accent for all five themes: it is the app's only
        // non-monochrome element, and it moves with the palette otherwise.
        val accents = palettes.map { it.mapAccent }.toSet()
        assertEquals(setOf(Color(0xFFBF6B6B)), accents)
    }

    @Test
    fun theLandmassesReadAgainstTheOceanTheyFloatIn() {
        palettes.forEach { palette ->
            assertNotEquals(
                "the landmasses vanish into the ocean",
                palette.card,
                palette.mapLandmass,
            )
        }
        // High Contrast is the theme that has to draw them light: a black wash
        // cannot shade a black ocean, so the shadow is carried by the
        // continents instead (see the terminator check below).
        assertEquals(Color(0xFFFFFFFF), HighContrastWorldClockPalette.mapLandmass)
    }

    @Test
    fun nightIsAlwaysPaintedDarkerThanTheMapBeneathIt() {
        // Every night wash is a dark-grey/black shade, never a light one: a
        // light wash on a dark map would read as daylight.
        palettes.forEach { palette ->
            assertTrue(
                "night must be painted with a dark shade",
                luminance(palette.mapNight.copy(alpha = 1f)) < 0.1,
            )
        }
        // The dark themes shade harder than the light ones, because their ocean
        // is already nearly black.
        assertTrue(DarkWorldClockPalette.mapNight.alpha > LightWorldClockPalette.mapNight.alpha)
        assertTrue(HighContrastWorldClockPalette.mapNight.alpha > LightWorldClockPalette.mapNight.alpha)
        assertEquals(Color(0xFF111111), LightWorldClockPalette.mapNight.copy(alpha = 1f))
        assertEquals(Color(0xFF000000), DarkWorldClockPalette.mapNight.copy(alpha = 1f))
    }

    @Test
    fun theTerminatorIsVisibleInEveryTheme() {
        // The absence of these checks is how a theme ships with no visible
        // day/night shading at all: an invisible wash still passes "night is
        // dark". The wash is drawn over the landmasses, so that is where it has
        // to change what the eye sees.
        palettes.forEach { palette ->
            val shaded = compositeOver(palette.mapNight, palette.mapLandmass)
            assertTrue(
                "the night wash does not change the landmasses",
                luminance(shaded) < luminance(palette.mapLandmass),
            )
        }
    }

    // --- Legibility ----------------------------------------------------------

    @Test
    fun mainTextPassesWcagAaInEveryTheme() {
        palettes.forEach { palette ->
            assertContrastAtLeast(4.5, palette.foreground, palette.background, "foreground on background")
            assertContrastAtLeast(4.5, palette.foreground, palette.card, "foreground on card")
            assertContrastAtLeast(4.5, palette.primaryForeground, palette.primary, "text on selected card")
        }
    }

    @Test
    fun supportingTextStaysReadableInEveryTheme() {
        palettes.forEach { palette ->
            // The grey behind supporting labels is a spec value and sits a
            // little under AA for body text; it is only ever used for short
            // supporting labels, so the large-text / non-text threshold is the
            // right bar.
            assertContrastAtLeast(3.0, palette.mutedForeground, palette.background, "muted on background")
            assertContrastAtLeast(3.0, palette.mutedForeground, palette.card, "muted on card")
            assertContrastAtLeast(3.0, palette.mutedForeground, palette.accent, "muted on accent")
        }
    }

    @Test
    fun supportingTextOnTheSelectedCardStaysReadable() {
        palettes.forEach { palette ->
            // `onPrimaryMuted` is translucent, so it has to be composited over
            // the selected card before it can be measured.
            val composited = compositeOver(palette.onPrimaryMuted, palette.primary)
            assertContrastAtLeast(3.0, composited, palette.primary, "muted on selected card")
        }
    }

    // --- Wiring --------------------------------------------------------------

    @Test
    fun theColourSchemeIsBuiltOnlyFromThePalette() {
        palettes.forEach { palette ->
            val allowed = with(palette) {
                setOf(
                    background, foreground, card, primary, primaryForeground, onPrimaryMuted,
                    secondary, mutedForeground, accent, border, mapAccent, mapLandmass, mapNight,
                )
            } + Color(0xFF000000) // the scrim

            palette.toColorScheme().roles().forEach { (role, color) ->
                assertTrue(
                    "$role fell back to $color, which is not in the ${if (palette.isDark) "dark" else "light"} palette",
                    color in allowed,
                )
            }
        }
    }

    @Test
    fun noRoleFallsBackToAMaterialAccent() {
        // The default Material 3 light accents. None of them may appear.
        val materialAccents = setOf(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260))
        palettes.forEach { palette ->
            palette.toColorScheme().roles().forEach { (role, color) ->
                assertTrue("$role is a default Material accent", color !in materialAccents)
            }
        }
    }

    // --- helpers -------------------------------------------------------------

    private fun cellIsLighterThanTheBackground(palette: WorldClockPalette): Boolean =
        luminance(palette.card) > luminance(palette.background)

    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
    )

    private fun assertContrastAtLeast(minimum: Double, foreground: Color, background: Color, what: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$what is only ${"%.2f".format(ratio)}:1", ratio >= minimum)
    }

    /** Paints [overlay] over [base], the way the canvas actually draws it. */
    private fun compositeOver(overlay: Color, base: Color): Color {
        val alpha = overlay.alpha
        return Color(
            red = overlay.red * alpha + base.red * (1f - alpha),
            green = overlay.green * alpha + base.green * (1f - alpha),
            blue = overlay.blue * alpha + base.blue * (1f - alpha),
        )
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }
}
