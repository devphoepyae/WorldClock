package com.worldclock.app.data.prefs

import com.worldclock.app.data.model.CityDirectory

/**
 * The user's saved state, decoupled from the stored proto message.
 *
 * @param savedCityIds ordered list of city ids — order is user-visible
 * @param activeCityId the highlighted city, or `null` when the list is empty
 * @param use24Hour true for 24-hour time, false for 12-hour time
 * @param theme one of the five palettes the app ships
 * @param cityLabels the user's own labels keyed by city id, e.g.
 *   `"tokyo" -> "Head Office"`. A missing key means "no label".
 */
data class UserPreferences(
    val savedCityIds: List<String>,
    val activeCityId: String?,
    val use24Hour: Boolean,
    val theme: AppTheme,
    val cityLabels: Map<String, String> = emptyMap(),
) {
    companion object {
        /** State shown on first launch (spec section 5). */
        val Default = UserPreferences(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            activeCityId = CityDirectory.defaultActiveCityId,
            use24Hour = true,
            theme = AppTheme.Default,
            cityLabels = emptyMap(),
        )

        val Empty = UserPreferences(
            savedCityIds = emptyList(),
            activeCityId = null,
            use24Hour = false,
            theme = AppTheme.Default,
            cityLabels = emptyMap(),
        )
    }
}

/**
 * Converts stored state to the domain model, dropping any city id that is no
 * longer in the directory and repairing the active id (spec section 7).
 */
fun SavedCities.toUserPreferences(): UserPreferences {
    val validIds = cityIdsList.filter { CityDirectory.find(it) != null }
    val activeId = activeCityId
        .takeIf { it.isNotEmpty() && validIds.contains(it) }
        ?: validIds.firstOrNull()
    return UserPreferences(
        savedCityIds = validIds,
        activeCityId = activeId,
        use24Hour = use24Hour,
        theme = storedTheme(),
        // A label for an id that has left the directory can never be shown
        // again, so it is dropped along with the id. A label for a city the
        // user removed on purpose is kept, so re-adding that city brings the
        // label back instead of silently losing it.
        cityLabels = cityLabelsMap
            .filterKeys { CityDirectory.find(it) != null }
            .filterValues { it.isNotBlank() },
    )
}

/**
 * The user's theme, migrating the light/dark choice of an older build.
 *
 * The presence bit is what makes the migration safe: a store that has never
 * carried a theme reads as the proto3 zero value, which is a real theme here
 * (Light Monochrome), so without `hasTheme()` every dark-mode user upgrading
 * from the three-mode switch would silently be handed the light theme.
 */
private fun SavedCities.storedTheme(): AppTheme =
    if (hasTheme()) theme.toAppTheme() else themeMode.toMigratedAppTheme()

/** Seeds the first-launch defaults onto an unseeded store. */
fun SavedCities.seededWithDefaults(): SavedCities =
    if (seeded) {
        this
    } else {
        toBuilder()
            .setSeeded(true)
            .clearCityIds()
            .addAllCityIds(CityDirectory.defaultSavedCityIds)
            .setActiveCityId(CityDirectory.defaultActiveCityId)
            .setUse24Hour(true)
            .build()
    }
