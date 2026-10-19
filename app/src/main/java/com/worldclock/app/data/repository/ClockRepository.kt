package com.worldclock.app.data.repository

import androidx.datastore.core.DataStore
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.data.prefs.SavedCities
import com.worldclock.app.data.prefs.UserPreferences
import com.worldclock.app.data.prefs.seededWithDefaults
import com.worldclock.app.data.prefs.toSavedAppTheme
import com.worldclock.app.data.prefs.toUserPreferences
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Single source of truth for saved cities, the active city and the 12h/24h
 * preference, backed by Proto DataStore (spec section 7).
 *
 * Failures never crash the app: reads fall back to the first-launch defaults
 * and writes are dropped, while a [PrefsError] is emitted so the UI can show a
 * non-blocking message.
 */
class ClockRepository(
    private val dataStore: DataStore<SavedCities>,
) {

    private val _errors = MutableSharedFlow<PrefsError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: SharedFlow<PrefsError> = _errors.asSharedFlow()

    /**
     * Streams the current preferences. The first collection also seeds the
     * first-launch defaults, so the first value already reflects them.
     */
    val preferences: Flow<UserPreferences> = flow {
        ensureInitialized()
        emitAll(storedPreferences())
    }.distinctUntilChanged()

    private fun storedPreferences(): Flow<UserPreferences> =
        dataStore.data
            .map { it.toUserPreferences() }
            .catch { throwable ->
                if (throwable is IOException) {
                    _errors.tryEmit(PrefsError.ReadFailed)
                    emit(UserPreferences.Default)
                } else {
                    throw throwable
                }
            }

    /** Seeds the first-launch defaults when the store has never been written. */
    suspend fun ensureInitialized() {
        mutate { it }
    }

    /** Adds [cityId] and makes it active. Already-saved cities are ignored. */
    suspend fun addCity(cityId: String) {
        mutate { current ->
            if (current.cityIdsList.contains(cityId)) {
                current
            } else {
                current.toBuilder()
                    .addCityIds(cityId)
                    .setActiveCityId(cityId)
                    .build()
            }
        }
    }

    /**
     * Removes [cityId]. When the active city is removed the first remaining city
     * becomes active, or no city when the list becomes empty.
     */
    suspend fun removeCity(cityId: String) {
        mutate { current ->
            val remaining = current.cityIdsList.filterNot { it == cityId }
            val activeId =
                if (current.activeCityId == cityId) remaining.firstOrNull().orEmpty()
                else current.activeCityId
            current.toBuilder()
                .clearCityIds()
                .addAllCityIds(remaining)
                .setActiveCityId(activeId)
                .build()
        }
    }

    /** Marks [cityId] as the active city. Unknown ids are ignored. */
    suspend fun selectCity(cityId: String) {
        mutate { current ->
            if (current.cityIdsList.contains(cityId)) {
                current.toBuilder().setActiveCityId(cityId).build()
            } else {
                current
            }
        }
    }

    /** Stores the 12h/24h display preference. */
    suspend fun setUse24Hour(use24Hour: Boolean) {
        mutate { current -> current.toBuilder().setUse24Hour(use24Hour).build() }
    }

    /**
     * Stores the chosen theme.
     *
     * The legacy light/dark field is cleared in the same write so a store that
     * predates the five themes converges on the new field instead of carrying
     * two answers that can disagree.
     */
    suspend fun setTheme(theme: AppTheme) {
        mutate { current ->
            current.toBuilder()
                .setTheme(theme.toSavedAppTheme())
                .clearThemeMode()
                .build()
        }
    }

    /**
     * Stores a personal label ("Head Office") for [cityId].
     *
     * A `null` or blank label clears it, so the dialog's "remove" action and an
     * empty text field both end in the same state. Labels are keyed by city id,
     * so they survive reordering and are unaffected by changes to other cities.
     */
    suspend fun setCityLabel(cityId: String, label: String?) {
        val trimmed = label?.trim().orEmpty()
        mutate { current ->
            current.toBuilder()
                .apply {
                    if (trimmed.isEmpty()) {
                        removeCityLabels(cityId)
                    } else {
                        putCityLabels(cityId, trimmed)
                    }
                }
                .build()
        }
    }

    private suspend fun mutate(transform: (SavedCities) -> SavedCities) {
        try {
            dataStore.updateData { current -> transform(current.seededWithDefaults()) }
        } catch (throwable: IOException) {
            _errors.tryEmit(PrefsError.WriteFailed)
        }
    }
}
