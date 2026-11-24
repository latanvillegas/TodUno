package com.example.data

import com.example.domain.LunarAstronomer
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Validates the portable TodUno JSON format before ANY persistent state changes.
 * Limits apply both at the Storage Access Framework boundary and in the repository.
 */
internal object BackupImportValidator {
    const val MAX_BACKUP_BYTES: Int = 8 * 1024 * 1024
    const val MAX_NOTES: Int = 5_000
    const val MAX_HISTORY: Int = 100
    const val MAX_CITIES: Int = 100
    private const val MAX_CITY_LENGTH = 100

    fun readBounded(input: InputStream): String = input.use { stream ->
        val buffer = ByteArray(8192)
        val output = ByteArrayOutputStream()
        var count = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            require(read <= MAX_BACKUP_BYTES - count) {
                "El respaldo excede el tamaño máximo admitido"
            }
            count += read
            output.write(buffer, 0, read)
        }
        require(count > 0) { "El respaldo está vacío" }
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray()))
            .toString()
    }

    fun validate(json: String): JSONObject {
        require(json.length <= MAX_BACKUP_BYTES &&
            json.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "El respaldo excede el tamaño máximo admitido"
        }
        val root = JSONObject(json)
        require(root.opt("app") == "TodUno") { "El respaldo pertenece a otra aplicación" }
        intInRange(root, "backupFormatVersion", 1, 1)
        if (root.has("exportedAtEpochMs")) longNonnegative(root, "exportedAtEpochMs")

        val settings = root.optJSONObject("settings")
            ?: throw IllegalArgumentException("Faltan las preferencias del respaldo")
        for (name in arrayOf(
            "isNorthernHemisphere", "is24HourFormat", "isRadiansMode", "lunarAlertsEnabled"
        )) {
            if (settings.has(name)) {
                require(settings.opt(name) is Boolean) { "Preferencia booleana inválida: $name" }
            }
        }
        if (settings.has("timerTotalSeconds")) {
            intInRange(settings, "timerTotalSeconds", 1, 86_399)
        }
        if (settings.has("watchDialPresetName")) {
            boundedString(settings, "watchDialPresetName", 1, 64)
        }
        settings.optJSONArray("worldCityIds")?.let { cities ->
            require(cities.length() <= MAX_CITIES) { "Demasiadas ciudades" }
            for (i in 0 until cities.length()) {
                val id = cities.opt(i)
                require(id is String && id.length in 1..MAX_CITY_LENGTH &&
                    id.isNotBlank() && '|' !in id && id.none(Char::isISOControl)) {
                    "Identificador de ciudad inválido"
                }
            }
        }
        if (settings.has("worldCityIds")) {
            require(settings.optJSONArray("worldCityIds") != null) { "Ciudades inválidas" }
        }

        val notes = requireArray(root, "notes", MAX_NOTES)
        for (i in 0 until notes.length()) {
            val note = requireObject(notes, i)
            val year = intInRange(note, "year", 1900, 2150)
            val month = intInRange(note, "month", 1, 12)
            intInRange(note, "day", 1, LunarAstronomer.daysInMonth(year, month))
            boundedString(note, "title", 1, 240)
            boundedString(note, "category", 1, 48)
            if (note.has("reminderEnabled")) {
                require(note.opt("reminderEnabled") is Boolean) { "Recordatorio inválido" }
            }
            if (note.has("reminderHour")) intInRange(note, "reminderHour", 0, 23)
            if (note.has("reminderMinute")) intInRange(note, "reminderMinute", 0, 59)
            if (note.has("createdAt")) longNonnegative(note, "createdAt")
        }

        val history = requireArray(root, "calculationHistory", MAX_HISTORY)
        for (i in 0 until history.length()) {
            val row = requireObject(history, i)
            boundedString(row, "expression", 1, 256)
            boundedString(row, "result", 1, 64)
            if (row.has("badgeLabel") && row.opt("badgeLabel") != JSONObject.NULL) {
                boundedString(row, "badgeLabel", 1, 64)
            }
            if (row.has("createdAt")) longNonnegative(row, "createdAt")
        }
        return root
    }

    private fun requireArray(root: JSONObject, name: String, max: Int): JSONArray {
        val array = root.optJSONArray(name)
            ?: throw IllegalArgumentException("Falta la lista $name")
        require(array.length() <= max) { "Demasiados elementos en $name" }
        return array
    }

    private fun requireObject(array: JSONArray, index: Int): JSONObject =
        array.optJSONObject(index) ?: throw IllegalArgumentException("Elemento JSON inválido")

    private fun boundedString(obj: JSONObject, key: String, min: Int, max: Int): String {
        val value = obj.opt(key)
        require(value is String && value.trim().length in min..max &&
            value.none(Char::isISOControl)) { "Campo de texto inválido: $key" }
        return value
    }

    private fun intInRange(obj: JSONObject, key: String, min: Int, max: Int): Int {
        val value = obj.opt(key)
        val parsed = (value as? Number)?.toString()?.toIntOrNull()
        require(parsed != null && parsed in min..max) { "Campo numérico inválido: $key" }
        return parsed
    }

    private fun longNonnegative(obj: JSONObject, key: String): Long {
        val value = obj.opt(key)
        val parsed = (value as? Number)?.toString()?.toLongOrNull()
        require(parsed != null && parsed >= 0) { "Fecha de creación inválida" }
        return parsed
    }
}
