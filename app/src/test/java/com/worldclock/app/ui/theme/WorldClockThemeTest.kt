package com.worldclock.app.ui.theme

import com.worldclock.app.data.prefs.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The theme-to-palette rule (spec section 8).
 *
 * [paletteFor] is pure, so the whole five-way mapping is checked here without a
 * device, a Robolectric configuration or a render. What each palette *looks*
 * like is [WorldClockPaletteTest]'s business; this file only pins which theme
 * paints which palette.
 */
class WorldClockThemeTest {

    @Test
    fun everyThemePaintsItsOwnPalette() {
        assertEquals(LightWorldClockPalette, paletteFor(AppTheme.LIGHT_MONOCHROME))
        assertEquals(DarkWorldClockPalette, paletteFor(AppTheme.TRUE_DARK))
        assertEquals(WarmSepiaWorldClockPalette, paletteFor(AppTheme.WARM_SEPIA))
        assertEquals(CoolSlateWorldClockPalette, paletteFor(AppTheme.COOL_SLATE))
        assertEquals(HighContrastWorldClockPalette, paletteFor(AppTheme.HIGH_CONTRAST))
    }

    @Test
    fun onlyTrueDarkAndHighContrastPaintDark() {
        // Everything that keys off "is this a dark palette" — the Material
        // colour scheme's defaults, the status bar icons — has to agree with
        // the two themes that are actually dark.
        val darkThemes = AppTheme.entries.filter { paletteFor(it).isDark }

        assertEquals(setOf(AppTheme.TRUE_DARK, AppTheme.HIGH_CONTRAST), darkThemes.toSet())
    }

    @Test
    fun noTwoThemesLookTheSame() {
        AppTheme.entries.forEach { theme ->
            val other = AppTheme.entries.filter { it != theme }
            other.forEach { candidate ->
                assertTrue(
                    "$theme and $candidate share a palette",
                    paletteFor(theme) != paletteFor(candidate),
                )
            }
        }
    }

    @Test
    fun theThemesAreNumberedZeroToFourInDesignOrder() {
        // The ids are what is persisted, so the order of the design listing is
        // part of the stored format.
        assertEquals(listOf(0, 1, 2, 3, 4), AppTheme.entries.map { it.id })
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.fromId(0))
        assertEquals(AppTheme.TRUE_DARK, AppTheme.fromId(1))
        assertEquals(AppTheme.WARM_SEPIA, AppTheme.fromId(2))
        assertEquals(AppTheme.COOL_SLATE, AppTheme.fromId(3))
        assertEquals(AppTheme.HIGH_CONTRAST, AppTheme.fromId(4))
    }

    @Test
    fun theDefaultThemeIsLightMonochrome() {
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.Default)
        assertEquals(0, AppTheme.Default.id)
        // Anything out of range reads as the default rather than throwing: a
        // store written by a newer build must not crash an older one.
        assertEquals(AppTheme.Default, AppTheme.fromId(-1))
        assertEquals(AppTheme.Default, AppTheme.fromId(99))
    }
}
