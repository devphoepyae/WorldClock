package com.worldclock.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worldclock.app.WorldClockApp
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.data.repository.ClockRepository
import com.worldclock.app.data.repository.PrefsError
import com.worldclock.app.ui.mapping.buildWorldClockUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Drives every clock screen.
 *
 * The state is the combination of the persisted preferences and the once-a-
 * second ticker. `SharingStarted.WhileSubscribed` means the ticker only runs
 * while a screen is actually observing it, so nothing ticks in the background.
 */
class WorldClockViewModel(
    private val repository: ClockRepository,
    ticker: Flow<Instant>,
    private val deviceZoneId: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    val uiState: StateFlow<WorldClockUiState> = combine(
        repository.preferences,
        ticker,
    ) { preferences, now ->
        buildWorldClockUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            use24Hour = preferences.use24Hour,
            now = now,
            deviceZoneId = deviceZoneId(),
            theme = preferences.theme,
            cityLabels = preferences.cityLabels,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WorldClockUiState(),
    )

    /** Storage failures for the UI to surface as a non-blocking message. */
    val errors: SharedFlow<PrefsError> = repository.errors

    /**
     * The device's own timezone: the reference every difference is measured
     * against, and the zone the meeting planner's slot is read in.
     */
    val deviceZone: ZoneId get() = deviceZoneId()

    fun addCity(cityId: String) {
        viewModelScope.launch { repository.addCity(cityId) }
    }

    fun removeCity(cityId: String) {
        viewModelScope.launch { repository.removeCity(cityId) }
    }

    fun selectCity(cityId: String) {
        viewModelScope.launch { repository.selectCity(cityId) }
    }

    fun setUse24Hour(use24Hour: Boolean) {
        viewModelScope.launch { repository.setUse24Hour(use24Hour) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    /** Stores the user's own label for a city, or clears it when [label] is null. */
    fun setCityLabel(cityId: String, label: String?) {
        viewModelScope.launch { repository.setCityLabel(cityId, label) }
    }

    companion object {
        /** Keeps the ticker alive across configuration changes. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as WorldClockApp
                WorldClockViewModel(
                    repository = app.container.clockRepository,
                    ticker = app.container.secondTicker.ticker(),
                )
            }
        }
    }
}
