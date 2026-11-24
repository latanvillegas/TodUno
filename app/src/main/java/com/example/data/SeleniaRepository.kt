package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CalculationRecord
import com.example.domain.SimpleDate
import com.example.ui.CalendarNote
import com.example.ui.StopwatchLap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.seleniaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "selenia_user_preferences"
)

@Immutable
data class BackupImportSummary(
    val notesImported: Int,
    val calculationsImported: Int,
    val worldCitiesCount: Int
)

@Immutable
data class SeleniaPersistedSettings(
    val isNorthernHemisphere: Boolean = true,
    val is24HourFormat: Boolean = true,
    val isRadiansMode: Boolean = false,
    val lunarAlertsEnabled: Boolean = false,
    val selectedWorldCityIds: List<String> = DEFAULT_WORLD_CITY_IDS,
    val watchDialPresetName: String = DEFAULT_WATCH_DIAL_PRESET,
    val stopwatchRunning: Boolean = false,
    val stopwatchAccumulatedMs: Long = 0L,
    val stopwatchStartWallEpochMs: Long = 0L,
    val stopwatchLaps: List<StopwatchLap> = emptyList(),
    val timerTotalSeconds: Int = 300,
    val timerRunning: Boolean = false,
    val timerRemainingPausedMs: Long = 300_000L,
    val timerEndWallEpochMs: Long = 0L
) {
    companion object {
        val DEFAULT_WORLD_CITY_IDS: List<String> = emptyList()
        const val DEFAULT_WATCH_DIAL_PRESET: String = "SAPPHIRE_ASTRAL"
    }
}

class SeleniaRepository(
    context: Context,
    private val dao: SeleniaDao
) {
    private val appContext: Context = context.applicationContext

    private object Keys {
        val IS_NORTHERN_HEMISPHERE = booleanPreferencesKey("is_northern_hemisphere")
        val IS_24_HOUR_FORMAT = booleanPreferencesKey("is_24_hour_format")
        val IS_RADIANS_MODE = booleanPreferencesKey("is_radians_mode")
        val LUNAR_ALERTS_ENABLED = booleanPreferencesKey("lunar_alerts_enabled")
        val SELECTED_WORLD_CITIES = stringPreferencesKey("user_added_world_cities_v2")
        val WATCH_DIAL_PRESET = stringPreferencesKey("watch_dial_preset_v1")

        val STOPWATCH_RUNNING = booleanPreferencesKey("stopwatch_running_v1")
        val STOPWATCH_ACCUMULATED_MS = longPreferencesKey("stopwatch_accumulated_ms_v1")
        val STOPWATCH_START_WALL_MS = longPreferencesKey("stopwatch_start_wall_ms_v1")
        val STOPWATCH_LAPS = stringPreferencesKey("stopwatch_laps_v1")

        val TIMER_TOTAL_SECONDS = intPreferencesKey("timer_total_seconds_v1")
        val TIMER_RUNNING = booleanPreferencesKey("timer_running_v1")
        val TIMER_REMAINING_PAUSED_MS = longPreferencesKey("timer_remaining_paused_ms_v1")
        val TIMER_END_WALL_MS = longPreferencesKey("timer_end_wall_ms_v1")
    }

    val notesByDateFlow: Flow<Map<String, List<CalendarNote>>> = dao.observeAllNotes()
        .map { list ->
            list.map { entity ->
                CalendarNote(
                    id = entity.id,
                    date = SimpleDate(entity.year, entity.month, entity.day),
                    title = entity.title,
                    category = entity.category,
                    reminderEnabled = entity.reminderEnabled,
                    reminderHour = entity.reminderHour.coerceIn(0, 23),
                    reminderMinute = entity.reminderMinute.coerceIn(0, 59)
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
            val isNorthern = prefs[Keys.IS_NORTHERN_HEMISPHERE] ?: true
            syncWidgetHemispherePref(appContext, isNorthern)

            val citiesCsv = prefs[Keys.SELECTED_WORLD_CITIES]
            val parsedCities = if (citiesCsv.isNullOrBlank()) {
                emptyList()
            } else {
                citiesCsv.split("|").filter { it.isNotBlank() }.distinct()
            }
            val totalTimerSec = (prefs[Keys.TIMER_TOTAL_SECONDS] ?: 300)
                .coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)
            SeleniaPersistedSettings(
                isNorthernHemisphere = isNorthern,
                is24HourFormat = prefs[Keys.IS_24_HOUR_FORMAT] ?: true,
                isRadiansMode = prefs[Keys.IS_RADIANS_MODE] ?: false,
                lunarAlertsEnabled = prefs[Keys.LUNAR_ALERTS_ENABLED] ?: false,
                selectedWorldCityIds = parsedCities,
                watchDialPresetName = prefs[Keys.WATCH_DIAL_PRESET]
                    ?: SeleniaPersistedSettings.DEFAULT_WATCH_DIAL_PRESET,
                stopwatchRunning = prefs[Keys.STOPWATCH_RUNNING] ?: false,
                stopwatchAccumulatedMs = prefs[Keys.STOPWATCH_ACCUMULATED_MS] ?: 0L,
                stopwatchStartWallEpochMs = prefs[Keys.STOPWATCH_START_WALL_MS] ?: 0L,
                stopwatchLaps = decodeStopwatchLaps(prefs[Keys.STOPWATCH_LAPS]),
                timerTotalSeconds = totalTimerSec,
                timerRunning = prefs[Keys.TIMER_RUNNING] ?: false,
                timerRemainingPausedMs = prefs[Keys.TIMER_REMAINING_PAUSED_MS] ?: (totalTimerSec * 1000L),
                timerEndWallEpochMs = prefs[Keys.TIMER_END_WALL_MS] ?: 0L
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    suspend fun getPersistedSettingsSnapshot(): SeleniaPersistedSettings {
        return settingsFlow.first()
    }

    suspend fun getReminderNotes(): List<CalendarNote> {
        return dao.getNotesWithReminders().map { entity ->
            CalendarNote(
                id = entity.id,
                date = SimpleDate(entity.year, entity.month, entity.day),
                title = entity.title,
                category = entity.category,
                reminderEnabled = entity.reminderEnabled,
                reminderHour = entity.reminderHour.coerceIn(0, 23),
                reminderMinute = entity.reminderMinute.coerceIn(0, 59)
            )
        }
    }

    suspend fun insertNote(
        date: SimpleDate,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ): Long {
        return dao.insertNote(
            CalendarNoteEntity(
                dateKey = date.storageKey,
                year = date.year,
                month = date.month,
                day = date.day,
                title = title.take(240),
                category = category.take(48),
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour.coerceIn(0, 23),
                reminderMinute = reminderMinute.coerceIn(0, 59)
            )
        )
    }

    suspend fun updateNote(
        noteId: Long,
        title: String,
        category: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ) {
        dao.updateNoteFields(
            noteId = noteId,
            title = title.take(240),
            category = category.take(48),
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour.coerceIn(0, 23),
            reminderMinute = reminderMinute.coerceIn(0, 59)
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
        syncWidgetHemispherePref(appContext, isNorthern)
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

    suspend fun setWatchDialPresetName(presetName: String) {
        appContext.seleniaDataStore.edit {
            it[Keys.WATCH_DIAL_PRESET] = presetName
        }
    }

    suspend fun saveStopwatchSnapshot(
        running: Boolean,
        accumulatedMs: Long,
        startWallEpochMs: Long,
        laps: List<StopwatchLap>
    ) {
        val encodedLaps = encodeStopwatchLaps(laps)
        appContext.seleniaDataStore.edit { prefs ->
            prefs[Keys.STOPWATCH_RUNNING] = running
            prefs[Keys.STOPWATCH_ACCUMULATED_MS] = accumulatedMs.coerceAtLeast(0L)
            prefs[Keys.STOPWATCH_START_WALL_MS] = startWallEpochMs.coerceAtLeast(0L)
            prefs[Keys.STOPWATCH_LAPS] = encodedLaps
        }
    }

    suspend fun saveTimerSnapshot(
        totalSeconds: Int,
        running: Boolean,
        remainingPausedMs: Long,
        endWallEpochMs: Long
    ) {
        appContext.seleniaDataStore.edit { prefs ->
            prefs[Keys.TIMER_TOTAL_SECONDS] = totalSeconds.coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)
            prefs[Keys.TIMER_RUNNING] = running
            prefs[Keys.TIMER_REMAINING_PAUSED_MS] = remainingPausedMs.coerceAtLeast(0L)
            prefs[Keys.TIMER_END_WALL_MS] = endWallEpochMs.coerceAtLeast(0L)
        }
    }

    /**
     * Exports all user-created notes, calculator history, world cities, and settings
     * into a portable offline JSON string (Storage Access Framework compatible, 0 permissions).
     */
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = getPersistedSettingsSnapshot()
        val notes = dao.getAllNotesSnapshot()
        val history = dao.getAllHistorySnapshot()

        val settingsObj = JSONObject().apply {
            put("isNorthernHemisphere", settings.isNorthernHemisphere)
            put("is24HourFormat", settings.is24HourFormat)
            put("isRadiansMode", settings.isRadiansMode)
            put("lunarAlertsEnabled", settings.lunarAlertsEnabled)
            put("watchDialPresetName", settings.watchDialPresetName)
            put("timerTotalSeconds", settings.timerTotalSeconds)
            put("worldCityIds", JSONArray(settings.selectedWorldCityIds))
        }

        val notesArray = JSONArray()
        for (n in notes) {
            notesArray.put(
                JSONObject().apply {
                    put("year", n.year)
                    put("month", n.month)
                    put("day", n.day)
                    put("title", n.title)
                    put("category", n.category)
                    put("reminderEnabled", n.reminderEnabled)
                    put("reminderHour", n.reminderHour)
                    put("reminderMinute", n.reminderMinute)
                    put("createdAt", n.createdAt)
                }
            )
        }

        val historyArray = JSONArray()
        for (h in history) {
            historyArray.put(
                JSONObject().apply {
                    put("expression", h.expression)
                    put("result", h.result)
                    if (!h.badgeLabel.isNullOrBlank()) {
                        put("badgeLabel", h.badgeLabel)
                    }
                    put("createdAt", h.createdAt)
                }
            )
        }

        JSONObject().apply {
            put("app", "TodUno")
            put("backupFormatVersion", 1)
            put("exportedAtEpochMs", System.currentTimeMillis())
            put("settings", settingsObj)
            put("notes", notesArray)
            put("calculationHistory", historyArray)
        }.toString(2).replace("\\/", "/")
    }

    /**
     * Restores settings, world cities, notes (deduplicated per date/title/category), and
     * calculation history from an offline JSON backup produced by [exportBackupJson].
     */
    suspend fun importBackupJson(jsonText: String): BackupImportSummary = withContext(Dispatchers.IO) {
        // Fully validate structure, calendar dates, field lengths and array sizes before any write.
        val root = BackupImportValidator.validate(jsonText)
        val currentSettings = getPersistedSettingsSnapshot()
        val settingsObj = root.getJSONObject("settings")
        var mergedWorldCities = currentSettings.selectedWorldCityIds

        val isNorthern = settingsObj.optBoolean("isNorthernHemisphere", currentSettings.isNorthernHemisphere)
        val is24h = settingsObj.optBoolean("is24HourFormat", currentSettings.is24HourFormat)
        val isRad = settingsObj.optBoolean("isRadiansMode", currentSettings.isRadiansMode)
        val lunarAlerts = settingsObj.optBoolean("lunarAlertsEnabled", currentSettings.lunarAlertsEnabled)
        val preset = settingsObj.optString("watchDialPresetName", currentSettings.watchDialPresetName)
        val timerSec = settingsObj.optInt("timerTotalSeconds", currentSettings.timerTotalSeconds)
            .coerceIn(MIN_TIMER_SECONDS, MAX_TIMER_SECONDS)

        settingsObj.optJSONArray("worldCityIds")?.let { array ->
            val importedCities = (0 until array.length()).map { array.getString(it).trim() }
            mergedWorldCities = (currentSettings.selectedWorldCityIds + importedCities).distinct()
        }
        require(mergedWorldCities.size <= BackupImportValidator.MAX_CITIES) {
            "Demasiadas ciudades tras combinar los datos"
        }

        val existingNoteSignatures = dao.getAllNotesSnapshot()
            .mapTo(HashSet()) { "${it.dateKey}|${it.title.lowercase()}|${it.category.lowercase()}" }
        val notesToInsert = ArrayList<CalendarNoteEntity>()
        val notesArr = root.getJSONArray("notes")
        for (i in 0 until notesArr.length()) {
            val obj = notesArr.getJSONObject(i)
            val year = obj.getInt("year")
            val month = obj.getInt("month")
            val day = obj.getInt("day")
            val title = obj.getString("title").trim()
            val category = obj.getString("category").trim()
            val date = SimpleDate(year, month, day)
            val signature = "${date.storageKey}|${title.lowercase()}|${category.lowercase()}"
            if (existingNoteSignatures.add(signature)) {
                notesToInsert.add(
                    CalendarNoteEntity(
                        dateKey = date.storageKey,
                        year = year,
                        month = month,
                        day = day,
                        title = title,
                        category = category,
                        reminderEnabled = obj.optBoolean("reminderEnabled", false),
                        reminderHour = obj.optInt("reminderHour", 9),
                        reminderMinute = obj.optInt("reminderMinute", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val existingHistorySignatures = dao.getAllHistorySnapshot()
            .mapTo(HashSet()) { "${it.expression}|${it.result}" }
        val historyToInsert = ArrayList<CalculationHistoryEntity>()
        val historyArr = root.getJSONArray("calculationHistory")
        // Preserve the established oldest-first import semantics.
        for (i in (historyArr.length() - 1) downTo 0) {
            val obj = historyArr.getJSONObject(i)
            val expr = obj.getString("expression").trim()
            val result = obj.getString("result").trim()
            if (existingHistorySignatures.add("${expr}|${result}")) {
                historyToInsert.add(
                    CalculationHistoryEntity(
                        expression = expr,
                        result = result,
                        badgeLabel = obj.optString("badgeLabel", "")
                            .trim().takeIf { it.isNotEmpty() },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // SQLite changes are atomic: a failed insertion cannot leave partial notes or history.
        dao.insertBackupRecords(notesToInsert, historyToInsert)
        // DataStore and Room do not support a single cross-store transaction. Their individual
        // writes are atomic, and this step runs only after full validation and a successful DB commit.
        appContext.seleniaDataStore.edit { prefs ->
            prefs[Keys.IS_NORTHERN_HEMISPHERE] = isNorthern
            prefs[Keys.IS_24_HOUR_FORMAT] = is24h
            prefs[Keys.IS_RADIANS_MODE] = isRad
            prefs[Keys.LUNAR_ALERTS_ENABLED] = lunarAlerts
            prefs[Keys.WATCH_DIAL_PRESET] = preset
            prefs[Keys.SELECTED_WORLD_CITIES] = mergedWorldCities.joinToString("|")
            if (!(prefs[Keys.TIMER_RUNNING] ?: false)) {
                prefs[Keys.TIMER_TOTAL_SECONDS] = timerSec
                prefs[Keys.TIMER_REMAINING_PAUSED_MS] = timerSec * 1000L
            }
        }
        syncWidgetHemispherePref(appContext, isNorthern)

        BackupImportSummary(
            notesImported = notesToInsert.size,
            calculationsImported = historyToInsert.size,
            worldCitiesCount = mergedWorldCities.size
        )
    }

    companion object {
        const val MIN_TIMER_SECONDS = 1
        const val MAX_TIMER_SECONDS = 86_399 // 23h 59m 59s

        private const val WIDGET_SYNC_PREFS = "selenia_widget_sync_prefs"
        private const val KEY_WIDGET_NORTHERN_HEMISPHERE = "is_northern_hemisphere"

        fun readWidgetNorthernHemisphere(context: Context): Boolean {
            return runCatching {
                context.applicationContext
                    .getSharedPreferences(WIDGET_SYNC_PREFS, Context.MODE_PRIVATE)
                    .getBoolean(KEY_WIDGET_NORTHERN_HEMISPHERE, true)
            }.getOrDefault(true)
        }

        fun syncWidgetHemispherePref(context: Context, isNorthern: Boolean) {
            runCatching {
                val sp = context.applicationContext
                    .getSharedPreferences(WIDGET_SYNC_PREFS, Context.MODE_PRIVATE)
                if (!sp.contains(KEY_WIDGET_NORTHERN_HEMISPHERE) ||
                    sp.getBoolean(KEY_WIDGET_NORTHERN_HEMISPHERE, true) != isNorthern
                ) {
                    sp.edit().putBoolean(KEY_WIDGET_NORTHERN_HEMISPHERE, isNorthern).apply()
                }
            }
        }

        internal fun encodeStopwatchLaps(laps: List<StopwatchLap>): String {
            if (laps.isEmpty()) return ""
            return laps.take(200).joinToString(";") { lap ->
                "${lap.lapNumber},${lap.lapDurationMs},${lap.totalElapsedMs}"
            }
        }

        internal fun decodeStopwatchLaps(encoded: String?): List<StopwatchLap> {
            if (encoded.isNullOrBlank()) return emptyList()
            return encoded.split(";").mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size != 3) return@mapNotNull null
                val num = parts[0].toIntOrNull() ?: return@mapNotNull null
                val dur = parts[1].toLongOrNull() ?: return@mapNotNull null
                val tot = parts[2].toLongOrNull() ?: return@mapNotNull null
                StopwatchLap(lapNumber = num, lapDurationMs = dur, totalElapsedMs = tot)
            }
        }

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
