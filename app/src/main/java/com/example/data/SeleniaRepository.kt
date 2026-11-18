package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CalculationRecord
import com.example.domain.LunarAstronomer
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.ui.CalendarNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.seleniaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "selenia_user_preferences"
)

@Immutable
data class SeleniaPersistedSettings(
    val isNorthernHemisphere: Boolean = true,
    val is24HourFormat: Boolean = true,
    val isRadiansMode: Boolean = false,
    val lunarAlertsEnabled: Boolean = false,
    val selectedWorldCityIds: List<String> = DEFAULT_WORLD_CITY_IDS
) {
    companion object {
        val DEFAULT_WORLD_CITY_IDS: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val localId = java.util.TimeZone.getDefault().id
            listOf(
                "Europe/Madrid",
                "America/Mexico_City",
                "America/Bogota",
                "America/Argentina/Buenos_Aires",
                "America/New_York",
                "Asia/Tokyo"
            ).filter { it != localId }
        }
    }
}

class SeleniaRepository(
    context: Context,
    private val dao: SeleniaDao
) {
    private val appContext: Context = context.applicationContext

    private object Keys {
        val INITIAL_SEED_DONE = booleanPreferencesKey("initial_seed_done_v1")
        val IS_NORTHERN_HEMISPHERE = booleanPreferencesKey("is_northern_hemisphere")
        val IS_24_HOUR_FORMAT = booleanPreferencesKey("is_24_hour_format")
        val IS_RADIANS_MODE = booleanPreferencesKey("is_radians_mode")
        val LUNAR_ALERTS_ENABLED = booleanPreferencesKey("lunar_alerts_enabled")
        val SELECTED_WORLD_CITIES = stringPreferencesKey("selected_world_cities")
    }

    val notesByDateFlow: Flow<Map<String, List<CalendarNote>>> = dao.observeAllNotes()
        .map { list ->
            list.map { entity ->
                CalendarNote(
                    id = entity.id,
                    date = SimpleDate(entity.year, entity.month, entity.day),
                    title = entity.title,
                    category = entity.category,
                    reminderEnabled = entity.reminderEnabled
                )
            }.groupBy { it.date.storageKey }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val calculationHistoryFlow: Flow<List<CalculationRecord>> = dao.observeCalculationHistory()
        .map { list ->
            list.map { entity ->
                CalculationRecord(
                    id = entity.id,
                    expression = entity.expression,
                    result = entity.result,
                    badgeLabel = entity.badgeLabel
                )
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val settingsFlow: Flow<SeleniaPersistedSettings> = appContext.seleniaDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val citiesCsv = prefs[Keys.SELECTED_WORLD_CITIES]
            val parsedCities = if (citiesCsv.isNullOrBlank()) {
                SeleniaPersistedSettings.DEFAULT_WORLD_CITY_IDS
            } else {
                citiesCsv.split("|").filter { it.isNotBlank() }.distinct()
            }
            SeleniaPersistedSettings(
                isNorthernHemisphere = prefs[Keys.IS_NORTHERN_HEMISPHERE] ?: true,
                is24HourFormat = prefs[Keys.IS_24_HOUR_FORMAT] ?: true,
                isRadiansMode = prefs[Keys.IS_RADIANS_MODE] ?: false,
                lunarAlertsEnabled = prefs[Keys.LUNAR_ALERTS_ENABLED] ?: false,
                selectedWorldCityIds = parsedCities.ifEmpty { SeleniaPersistedSettings.DEFAULT_WORLD_CITY_IDS }
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    suspend fun ensureSeeded() {
        var alreadySeeded = false
        appContext.seleniaDataStore.edit { prefs ->
            alreadySeeded = prefs[Keys.INITIAL_SEED_DONE] == true
            if (!alreadySeeded) {
                prefs[Keys.INITIAL_SEED_DONE] = true
            }
        }
        if (alreadySeeded) return

        if (dao.countNotes() == 0) {
            val today = LunarAstronomer.today()
            val upcoming = LunarAstronomer.getUpcomingMajorPhases(today, count = 4)
            upcoming.firstOrNull { it.phaseType == MoonPhaseType.FULL_MOON }?.let { fullMoon ->
                dao.insertNote(
                    CalendarNoteEntity(
                        dateKey = fullMoon.date.storageKey,
                        year = fullMoon.date.year,
                        month = fullMoon.date.month,
                        day = fullMoon.date.day,
                        title = "Observación de Plenilunio (100% iluminada)",
                        category = "Astronomía",
                        reminderEnabled = false
                    )
                )
            }
            upcoming.firstOrNull { it.phaseType == MoonPhaseType.NEW_MOON }?.let { newMoon ->
                dao.insertNote(
                    CalendarNoteEntity(
                        dateKey = newMoon.date.storageKey,
                        year = newMoon.date.year,
                        month = newMoon.date.month,
                        day = newMoon.date.day,
                        title = "Noche sin luna: ideal para cielo profundo",
                        category = "Astronomía",
                        reminderEnabled = false
                    )
                )
            }
        }

        if (dao.countHistory() == 0) {
            val now = System.currentTimeMillis()
            dao.insertHistoryRecord(
                CalculationHistoryEntity(
                    expression = "365.2422 − (12 × 29.5306)",
                    result = "10.875",
                    badgeLabel = "Epacta anual (días)",
                    createdAt = now - 1000L
                )
            )
            dao.insertHistoryRecord(
                CalculationHistoryEntity(
                    expression = "12 × 29.5306",
                    result = "354.3672",
                    badgeLabel = "Año Lunar (12 lunas)",
                    createdAt = now
                )
            )
        }
    }

    suspend fun insertNote(
        date: SimpleDate,
        title: String,
        category: String,
        reminderEnabled: Boolean
    ): Long {
        return dao.insertNote(
            CalendarNoteEntity(
                dateKey = date.storageKey,
                year = date.year,
                month = date.month,
                day = date.day,
                title = title.take(240),
                category = category.take(48),
                reminderEnabled = reminderEnabled
            )
        )
    }

    suspend fun deleteNote(noteId: Long) {
        dao.deleteNoteById(noteId)
    }

    suspend fun insertCalculation(expression: String, result: String, badgeLabel: String?) {
        dao.insertAndPruneHistoryRecord(
            CalculationHistoryEntity(
                expression = expression.take(256),
                result = result.take(64),
                badgeLabel = badgeLabel?.take(64)
            )
        )
    }

    suspend fun clearCalculations() {
        dao.clearCalculationHistory()
    }

    suspend fun setNorthernHemisphere(isNorthern: Boolean) {
        appContext.seleniaDataStore.edit { it[Keys.IS_NORTHERN_HEMISPHERE] = isNorthern }
    }

    suspend fun set24HourFormat(is24Hour: Boolean) {
        appContext.seleniaDataStore.edit { it[Keys.IS_24_HOUR_FORMAT] = is24Hour }
    }

    suspend fun setRadiansMode(isRadians: Boolean) {
        appContext.seleniaDataStore.edit { it[Keys.IS_RADIANS_MODE] = isRadians }
    }

    suspend fun setLunarAlertsEnabled(enabled: Boolean) {
        appContext.seleniaDataStore.edit { it[Keys.LUNAR_ALERTS_ENABLED] = enabled }
    }

    suspend fun setSelectedWorldCityIds(cityIds: List<String>) {
        appContext.seleniaDataStore.edit {
            it[Keys.SELECTED_WORLD_CITIES] = cityIds.distinct().joinToString("|")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SeleniaRepository? = null

        fun getInstance(context: Context): SeleniaRepository {
            return INSTANCE ?: synchronized(this) {
                val appCtx = context.applicationContext
                INSTANCE ?: SeleniaRepository(
                    context = appCtx,
                    dao = SeleniaDatabase.getInstance(appCtx).seleniaDao()
                ).also { INSTANCE = it }
            }
        }
    }
}
