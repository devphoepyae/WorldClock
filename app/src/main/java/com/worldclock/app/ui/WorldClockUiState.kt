package com.worldclock.app.ui

import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.ui.components.CityCardModel
import java.time.Instant

/** The device's own timezone and current time, shown on the detail screen. */
data class DeviceLocalTime(
    /** IANA id of the device timezone, e.g. "Asia/Yangon". */
    val zoneId: String,
    /** e.g. "UTC+6:30" */
    val offsetLabel: String,
    /** e.g. "16:40" */
    val timeText: String,
)

/**
 * Everything the clock screens render at one point in time.
 *
 * @param now the instant all displayed times were computed from; refreshed once
 *   a second by the ticker
 * @param theme the persisted theme; the root theme reads this so it can be
 *   applied before any screen is composed
 */
data class WorldClockUiState(
    val isLoading: Boolean = true,
    val now: Instant = Instant.EPOCH,
    val use24Hour: Boolean = false,
    val theme: AppTheme = AppTheme.Default,
    val activeCityId: String? = null,
    val cities: List<CityCardModel> = emptyList(),
    val deviceLocal: DeviceLocalTime? = null,
)
