package com.worldclock.app.data.prefs

/**
 * One of the five themes the app ships (spec section 7).
 *
 * Each entry is a complete palette rather than a light/dark switch, so the
 * choice names an exact look and nothing is resolved against the device
 * setting. [id] is the number that is persisted and quoted in the design, and
 * it is part of the stored format: re-ordering this enum would repaint every
 * existing install, so the ids are written out explicitly.
 *
 * Kept separate from the generated [SavedAppTheme] so the rest of the app never
 * depends on a generated type.
 */
enum class AppTheme(val id: Int) {
    /** Dark text on a soft off-white surface. The default. */
    LIGHT_MONOCHROME(0),

    /** Light text on a near-black surface. */
    TRUE_DARK(1),

    /** Warm brown text on soft paper. */
    WARM_SEPIA(2),

    /** Slate text on a muted blue-grey surface. */
    COOL_SLATE(3),

    /** Pure white on pure black, with outlines instead of fills. */
    HIGH_CONTRAST(4),
    ;

    companion object {
        /** Used when the user has never made a choice. */
        val Default: AppTheme = LIGHT_MONOCHROME

        /** The theme carrying [id], or [Default] when nothing matches. */
        fun fromId(id: Int): AppTheme = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * Maps the stored value onto the domain enum.
 *
 * `UNRECOGNIZED` and the zero value both land on the default, which is what a
 * store that has never carried a theme should read as.
 */
fun SavedAppTheme.toAppTheme(): AppTheme = when (this) {
    SavedAppTheme.SAVED_APP_THEME_TRUE_DARK -> AppTheme.TRUE_DARK
    SavedAppTheme.SAVED_APP_THEME_WARM_SEPIA -> AppTheme.WARM_SEPIA
    SavedAppTheme.SAVED_APP_THEME_COOL_SLATE -> AppTheme.COOL_SLATE
    SavedAppTheme.SAVED_APP_THEME_HIGH_CONTRAST -> AppTheme.HIGH_CONTRAST
    else -> AppTheme.LIGHT_MONOCHROME
}

/** Maps the domain enum onto the value that gets written to the store. */
fun AppTheme.toSavedAppTheme(): SavedAppTheme = when (this) {
    AppTheme.LIGHT_MONOCHROME -> SavedAppTheme.SAVED_APP_THEME_LIGHT_MONOCHROME
    AppTheme.TRUE_DARK -> SavedAppTheme.SAVED_APP_THEME_TRUE_DARK
    AppTheme.WARM_SEPIA -> SavedAppTheme.SAVED_APP_THEME_WARM_SEPIA
    AppTheme.COOL_SLATE -> SavedAppTheme.SAVED_APP_THEME_COOL_SLATE
    AppTheme.HIGH_CONTRAST -> SavedAppTheme.SAVED_APP_THEME_HIGH_CONTRAST
}

/**
 * Migrates the light/dark choice of an older build onto one of the five themes.
 *
 * `DARK` was the true-dark palette and `LIGHT` the light monochrome one, so
 * those users keep looking at what they chose. `SYSTEM` followed the device,
 * and which of the two they were actually seeing is not recoverable from the
 * store, so it lands on the default — the same thing a store with no theme at
 * all reads as.
 */
fun SavedThemeMode.toMigratedAppTheme(): AppTheme = when (this) {
    SavedThemeMode.THEME_MODE_DARK -> AppTheme.TRUE_DARK
    SavedThemeMode.THEME_MODE_LIGHT -> AppTheme.LIGHT_MONOCHROME
    else -> AppTheme.Default
}
