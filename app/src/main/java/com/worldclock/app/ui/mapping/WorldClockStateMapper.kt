package com.worldclock.app.ui.mapping

import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.domain.time.CityTimeFormatter
import com.worldclock.app.domain.time.TimeDifference
import com.worldclock.app.ui.DeviceLocalTime
import com.worldclock.app.ui.WorldClockUiState
import com.worldclock.app.ui.components.CityCardModel
import java.time.Instant
import java.time.ZoneId

/**
 * Builds the renderable screen state from saved ids and the current instant.
 *
 * Pure and side-effect free so it can be exercised directly by tests and used
 * to produce deterministic preview/screenshot data.
 *
 * @param cityLabels the user's own labels keyed by city id; a missing key means
 *   "no label"
 */
fun buildWorldClockUiState(
    savedCityIds: List<String>,
    activeCityId: String?,
    use24Hour: Boolean,
    now: Instant,
    deviceZoneId: ZoneId,
    theme: AppTheme = AppTheme.Default,
    cityLabels: Map<String, String> = emptyMap(),
): WorldClockUiState {
    val cities = CityDirectory.citiesForIds(savedCityIds).map { city ->
        val zoneId = ZoneId.of(city.timeZone)
        val time = CityTimeFormatter.snapshot(
            zoneId = zoneId,
            instant = now,
            use24Hour = use24Hour,
        )
        CityCardModel(
            id = city.id,
            offsetLabel = time.offsetLabel,
            weekdayLabel = time.weekdayShort,
            name = city.name,
            region = city.region,
            timeText = time.timeText,
            isDaytime = time.isDaytime,
            isActive = city.id == activeCityId,
            label = cityLabels[city.id]?.takeIf { it.isNotBlank() },
            // Every card is compared with the device's own time, which is the
            // reference the detail screen also spells out. Comparing against the
            // active city instead would always read "Same time" on the detail
            // screen, because opening a city makes it active (spec section 4.5).
            difference = TimeDifference.between(zoneId, deviceZoneId, now),
            latitude = city.latitude,
            longitude = city.longitude,
        )
    }

    val deviceTime = CityTimeFormatter.snapshot(
        zoneId = deviceZoneId,
        instant = now,
        use24Hour = use24Hour,
    )

    return WorldClockUiState(
        isLoading = false,
        now = now,
        use24Hour = use24Hour,
        theme = theme,
        activeCityId = activeCityId,
        cities = cities,
        deviceLocal = DeviceLocalTime(
            zoneId = deviceZoneId.id,
            offsetLabel = deviceTime.offsetLabel,
            timeText = deviceTime.timeText,
        ),
    )
}
