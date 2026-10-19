package com.worldclock.app.testing

import androidx.datastore.core.DataStore
import com.worldclock.app.data.prefs.SavedCities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [DataStore] used by tests and screenshot renders.
 *
 * It never touches the disk and never suspends, so screen state is fully
 * deterministic and no background I/O can race a render.
 */
class FakeSavedCitiesDataStore(
    initial: SavedCities = SavedCities.getDefaultInstance(),
) : DataStore<SavedCities> {

    private val state = MutableStateFlow(initial)

    override val data: Flow<SavedCities> = state

    override suspend fun updateData(
        transform: suspend (t: SavedCities) -> SavedCities,
    ): SavedCities {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
