package com.worldclock.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldclock.app.data.model.CityDirectory
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.data.prefs.SavedCities
import com.worldclock.app.data.prefs.SavedThemeMode
import com.worldclock.app.data.prefs.UserPreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Exercises persistence against a real Proto DataStore on a temporary file, so
 * the proto round-trip and the restart path are covered for real.
 */
@RunWith(AndroidJUnit4::class)
class ClockRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var fileCounter = 0

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun newDataStore(file: File): DataStore<SavedCities> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopes += scope
        return DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { SavedCities.getDefaultInstance() },
            scope = scope,
            produceFile = { file },
        )
    }

    private fun newFile(): File = File(tempFolder.root, "prefs-${fileCounter++}.pb")

    // --- First launch --------------------------------------------------------

    @Test
    fun firstLaunchSeedsTheDefaultCities() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        val preferences = repository.preferences.first()

        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
        assertTrue(preferences.use24Hour)
        // Nothing has been chosen yet, so the first of the five themes applies.
        assertEquals(AppTheme.Default, preferences.theme)
    }

    // --- Add / remove / select ----------------------------------------------

    @Test
    fun addingACityAppendsItAndMakesItActive() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.addCity("paris")

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds + "paris", preferences.savedCityIds)
        assertEquals("paris", preferences.activeCityId)
    }

    @Test
    fun duplicateCityIsIgnored() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.addCity("paris")
        repository.addCity("paris")

        val preferences = repository.preferences.first()
        assertEquals(1, preferences.savedCityIds.count { it == "paris" })
        assertEquals(CityDirectory.defaultSavedCityIds.size + 1, preferences.savedCityIds.size)
    }

    @Test
    fun removingTheActiveCityPromotesTheFirstRemainingCity() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.removeCity(CityDirectory.defaultActiveCityId)

        val preferences = repository.preferences.first()
        assertEquals(listOf("tokyo", "sydney", "new-york", "london"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    @Test
    fun removingANonActiveCityKeepsTheActiveCity() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.removeCity("tokyo")

        val preferences = repository.preferences.first()
        assertEquals(listOf("sydney", "los-angeles", "new-york", "london"), preferences.savedCityIds)
        assertEquals("los-angeles", preferences.activeCityId)
    }

    @Test
    fun removingEveryCityLeavesTheEmptyState() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        CityDirectory.defaultSavedCityIds.forEach { repository.removeCity(it) }

        val preferences = repository.preferences.first()
        assertTrue(preferences.savedCityIds.isEmpty())
        assertNull(preferences.activeCityId)
    }

    @Test
    fun selectingACityOnlyWorksForSavedCities() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.selectCity("berlin")
        assertEquals(CityDirectory.defaultActiveCityId, repository.preferences.first().activeCityId)

        repository.selectCity("tokyo")
        assertEquals("tokyo", repository.preferences.first().activeCityId)
    }

    @Test
    fun use24HourIsPersisted() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setUse24Hour(true)
        assertTrue(repository.preferences.first().use24Hour)

        repository.setUse24Hour(false)
        assertFalse(repository.preferences.first().use24Hour)
    }

    @Test
    fun theThemeIsPersisted() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setTheme(AppTheme.TRUE_DARK)
        assertEquals(AppTheme.TRUE_DARK, repository.preferences.first().theme)

        repository.setTheme(AppTheme.WARM_SEPIA)
        assertEquals(AppTheme.WARM_SEPIA, repository.preferences.first().theme)

        repository.setTheme(AppTheme.LIGHT_MONOCHROME)
        assertEquals(AppTheme.LIGHT_MONOCHROME, repository.preferences.first().theme)
    }

    @Test
    fun settingAThemeLeavesTheCitiesAlone() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setTheme(AppTheme.COOL_SLATE)

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
    }

    @Test
    fun choosingAThemeMigratesAStoreThatOnlyHadTheLightDarkField() = runBlocking {
        val store = newDataStore(newFile())
        store.updateData {
            SavedCities.newBuilder()
                .setSeeded(true)
                .addCityIds("tokyo")
                .setThemeMode(SavedThemeMode.THEME_MODE_DARK)
                .build()
        }
        val repository = ClockRepository(store)
        assertEquals(AppTheme.TRUE_DARK, repository.preferences.first().theme)

        repository.setTheme(AppTheme.HIGH_CONTRAST)

        val preferences = repository.preferences.first()
        assertEquals(AppTheme.HIGH_CONTRAST, preferences.theme)
        // The legacy field is reset to its zero value, so the store carries one
        // answer rather than two that can disagree.
        assertEquals(SavedThemeMode.THEME_MODE_SYSTEM, store.data.first().themeMode)
    }

    // --- Restore / prune -----------------------------------------------------

    @Test
    fun unknownStoredCityIdsArePrunedOnRestore() = runBlocking {
        val store = newDataStore(newFile())
        store.updateData {
            SavedCities.newBuilder()
                .setSeeded(true)
                .addCityIds("tokyo")
                .addCityIds("atlantis")
                .addCityIds("paris")
                .setActiveCityId("atlantis")
                .build()
        }

        val preferences = ClockRepository(store).preferences.first()

        assertEquals(listOf("tokyo", "paris"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    @Test
    fun stateSurvivesARestart() = runBlocking {
        val file = newFile()

        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val firstRepository = ClockRepository(
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler {
                    SavedCities.getDefaultInstance()
                },
                scope = firstScope,
                produceFile = { file },
            ),
        )
        firstRepository.addCity("paris")
        firstRepository.setUse24Hour(true)
        firstRepository.selectCity("tokyo")
        firstRepository.setTheme(AppTheme.TRUE_DARK)
        firstRepository.setCityLabel("london", "Head Office")
        val before = firstRepository.preferences.first()

        firstScope.cancel()
        firstScope.coroutineContext[Job]?.join()

        val secondRepository = ClockRepository(newDataStore(file))
        val after = secondRepository.preferences.first()

        assertEquals(before, after)
        assertEquals(true, after.use24Hour)
        assertEquals("tokyo", after.activeCityId)
        assertEquals(AppTheme.TRUE_DARK, after.theme)
        assertEquals("Head Office", after.cityLabels["london"])
        assertEquals(CityDirectory.defaultSavedCityIds + "paris", after.savedCityIds)
    }

    // --- City labels ---------------------------------------------------------

    @Test
    fun aCityLabelIsPersistedTrimmed() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCityLabel("tokyo", "  Head Office  ")

        assertEquals(mapOf("tokyo" to "Head Office"), repository.preferences.first().cityLabels)
    }

    @Test
    fun aBlankOrAbsentLabelClearsIt() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCityLabel("tokyo", "Head Office")
        repository.setCityLabel("tokyo", "   ")
        assertTrue(repository.preferences.first().cityLabels.isEmpty())

        repository.setCityLabel("tokyo", "Head Office")
        repository.setCityLabel("tokyo", null)
        assertTrue(repository.preferences.first().cityLabels.isEmpty())
    }

    @Test
    fun labelsAreIndependentPerCity() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCityLabel("tokyo", "Head Office")
        repository.setCityLabel("new-york", "Client calls")

        val preferences = repository.preferences.first()
        assertEquals("Head Office", preferences.cityLabels["tokyo"])
        assertEquals("Client calls", preferences.cityLabels["new-york"])

        repository.setCityLabel("new-york", null)
        val after = repository.preferences.first()
        assertEquals("Head Office", after.cityLabels["tokyo"])
        assertNull(after.cityLabels["new-york"])
    }

    @Test
    fun settingALabelLeavesTheCitiesAndTheThemeAlone() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setTheme(AppTheme.TRUE_DARK)
        repository.setCityLabel("tokyo", "Head Office")

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
        assertEquals(AppTheme.TRUE_DARK, preferences.theme)
    }
}
