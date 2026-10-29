package com.example.domain

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MoonPhaseType(
    val spanishName: String,
    val shortLabel: String,
    val emoji: String,
    val isMajorPhase: Boolean,
    val gardeningTip: String,
    val stargazingTip: String,
    val tideEffect: String
) {
    NEW_MOON(
        spanishName = "Luna Nueva",
        shortLabel = "Nueva",
        emoji = "🌑",
        isMajorPhase = true,
        gardeningTip = "Ideal para reposo del suelo, poda de limpieza y control de malezas.",
        stargazingTip = "Cielo oscuro óptimo para observar galaxias, nebulosas y vía láctea.",
        tideEffect = "Mareas vivas (sicigia): amplitud máxima de pleamar y bajamar."
    ),
    WAXING_CRESCENT(
        spanishName = "Luna Creciente",
        shortLabel = "Creciente",
        emoji = "🌒",
        isMajorPhase = false,
        gardeningTip = "La savia comienza a subir: excelente para sembrar hortalizas de hoja y flores.",
        stargazingTip = "Luz cenicienta visible sobre el limbo oscuro al atardecer occidental.",
        tideEffect = "Mareas en transición descendente tras la sicigia."
    ),
    FIRST_QUARTER(
        spanishName = "Cuarto Creciente",
        shortLabel = "C. Creciente",
        emoji = "🌓",
        isMajorPhase = true,
        gardeningTip = "Favorece plantas de fruto, cereales y trasplantes de crecimiento vigoroso.",
        stargazingTip = "El terminador muestra cráteres y cordilleras lunares con máximo relieve.",
        tideEffect = "Mareas muertas (cuadratura): menor diferencia entre pleamar y bajamar."
    ),
    WAXING_GIBBOUS(
        spanishName = "Gibosa Creciente",
        shortLabel = "Gibosa Crec.",
        emoji = "🌔",
        isMajorPhase = false,
        gardeningTip = "Máxima absorción de nutrientes y agua; ideal para injertos y abonos foliares.",
        stargazingTip = "Excelente visibilidad de los grandes mares lunares y el cráter Copérnico.",
        tideEffect = "Mareas en aumento progresivo hacia el plenilunio."
    ),
    FULL_MOON(
        spanishName = "Luna Llena",
        shortLabel = "Llena",
        emoji = "🌕",
        isMajorPhase = true,
        gardeningTip = "Savia concentrada en la copa: momento óptimo para cosechar frutos y hierbas.",
        stargazingTip = "Disco 100% iluminado; destacan los sistemas de rayos brillantes como Tycho.",
        tideEffect = "Mareas vivas (sicigia): fuerte atracción gravitatoria combinada Sol-Luna."
    ),
    WANING_GIBBOUS(
        spanishName = "Gibosa Menguante",
        shortLabel = "Gibosa Meng.",
        emoji = "🌖",
        isMajorPhase = false,
        gardeningTip = "La savia empieza a descender hacia las raíces; propicio para sembrar tubérculos.",
        stargazingTip = "Salida de la luna tras el anochecer con excelente contraste en el limbo oriental.",
        tideEffect = "Mareas moderadas-altas disminuyendo gradualmente."
    ),
    LAST_QUARTER(
        spanishName = "Cuarto Menguante",
        shortLabel = "C. Menguante",
        emoji = "🌗",
        isMajorPhase = true,
        gardeningTip = "Ideal para podas de formación, trasplante de raíces, bulbos y compostaje.",
        stargazingTip = "Visible en la madrugada y mañana; relieve inverso en los Montes Apeninos.",
        tideEffect = "Mareas muertas (cuadratura): corrientes marinas más suaves y estables."
    ),
    WANING_CRESCENT(
        spanishName = "Luna Menguante",
        shortLabel = "Menguante",
        emoji = "🌘",
        isMajorPhase = false,
        gardeningTip = "Fase balsámica de descanso vegetal, deshierbe y preparación de sustratos.",
        stargazingTip = "Fino creciente matutino antes del alba; cielos nocturnos despejados de brillo lunar.",
        tideEffect = "Mareas aumentando hacia el nuevo ciclo sinódico."
    )
}

data class SimpleDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<SimpleDate> {
    override fun compareTo(other: SimpleDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toStorageKey(): String = "%04d-%02d-%02d".format(year, month, day)

    fun formatSpanishLong(): String {
        val monthNames = arrayOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        val dayOfWeek = LunarAstronomer.getDayOfWeekSpanish(year, month, day)
        return "$dayOfWeek, $day de ${monthNames[(month - 1).coerceIn(0, 11)]} de $year"
    }

    fun formatSpanishShort(): String {
        val monthShort = arrayOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        )
        return "$day ${monthShort[(month - 1).coerceIn(0, 11)]} $year"
    }
}

data class LunarDayInfo(
    val date: SimpleDate,
    val phaseFraction: Double,      // 0.0 (New) -> 0.5 (Full) -> 1.0 (New)
    val illuminationPercent: Double, // 0.0 .. 100.0
    val lunarAgeDays: Double,        // 0.0 .. 29.5306
    val distanceKm: Int,             // ~356,500 .. 406,700 km
    val zodiacSign: String,
    val phaseType: MoonPhaseType,
    val isExactMajorPhaseDay: Boolean
)

data class CalendarDayCell(
    val date: SimpleDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val lunarInfo: LunarDayInfo
)

data class MajorPhaseMilestone(
    val date: SimpleDate,
    val phaseType: MoonPhaseType,
    val illuminationPercent: Int
)

object LunarAstronomer {
    const val SYNODIC_MONTH_DAYS = 29.530588853
    private const val ANOMALISTIC_MONTH_DAYS = 27.554550
    private const val TROPICAL_MONTH_DAYS = 27.321582
    // Known reference New Moon: Jan 6, 2000 at 18:14 UTC -> JD 2451550.1
    private const val REFERENCE_NEW_MOON_JD = 2451550.1
    private const val REFERENCE_PERIGEE_JD = 2451562.2

    private val ZODIAC_CONSTELLATIONS = listOf(
        "Aries ♈", "Tauro ♉", "Géminis ♊", "Cáncer ♋",
        "Leo ♌", "Virgo ♍", "Libra ♎", "Escorpio ♏",
        "Sagitario ♐", "Capricornio ♑", "Acuario ♒", "Piscis ♓"
    )

    val SPANISH_MONTHS = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val SPANISH_WEEKDAYS_SHORT = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    fun today(): SimpleDate {
        val cal = Calendar.getInstance()
        return SimpleDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Computes astronomical Julian Day at 12:00 UTC for a given Gregorian date.
     */
    fun toJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            day + b - 1524.5 + 0.5 // +0.5 for 12:00 noon UTC
    }

    fun fromJulianDay(jd: Double): SimpleDate {
        val z = floor(jd + 0.5).toInt()
        val a = if (z < 2299161) {
            z
        } else {
            val alpha = floor((z - 1867216.25) / 36524.25).toInt()
            z + 1 + alpha - (alpha / 4)
        }
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt()

        val day = b - d - floor(30.6001 * e).toInt()
        val month = if (e < 14) e - 1 else e - 13
        val year = if (month > 2) c - 4716 else c - 4715
        return SimpleDate(year, month, day)
    }

    fun addDays(date: SimpleDate, deltaDays: Int): SimpleDate {
        val jd = toJulianDay(date.year, date.month, date.day)
        return fromJulianDay(jd + deltaDays)
    }

    fun daysBetween(start: SimpleDate, end: SimpleDate): Int {
        val jd1 = toJulianDay(start.year, start.month, start.day)
        val jd2 = toJulianDay(end.year, end.month, end.day)
        return (jd2 - jd1).roundToInt()
    }

    fun getDayOfWeekSpanish(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(year, month - 1, day, 12, 0, 0)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            else -> "Domingo"
        }
    }

    private fun rawPhaseFractionForJd(jd: Double): Double {
        val cycles = (jd - REFERENCE_NEW_MOON_JD) / SYNODIC_MONTH_DAYS
        var frac = cycles - floor(cycles)
        if (frac < 0) frac += 1.0
        return frac
    }

    fun getLunarInfo(date: SimpleDate): LunarDayInfo {
        val jd = toJulianDay(date.year, date.month, date.day)
        val phaseFraction = rawPhaseFractionForJd(jd)
        val lunarAge = phaseFraction * SYNODIC_MONTH_DAYS

        // Spherical illumination percentage
        val phaseAngle = phaseFraction * 2.0 * PI
        val illumination = ((1.0 - cos(phaseAngle)) / 2.0) * 100.0

        // Earth-Moon orbital distance using anomalistic cycle + solar perturbation
        val anomCycles = (jd - REFERENCE_PERIGEE_JD) / ANOMALISTIC_MONTH_DAYS
        val anomAngle = (anomCycles - floor(anomCycles)) * 2.0 * PI
        val distanceKm = (384400.0 - 20905.0 * cos(anomAngle) - 3699.0 * cos(2 * phaseAngle - anomAngle)).roundToInt()

        // Ecliptic zodiac transit
        val tropCycles = (jd - 2451545.0) / TROPICAL_MONTH_DAYS
        var tropFrac = tropCycles - floor(tropCycles)
        if (tropFrac < 0) tropFrac += 1.0
        val zodiacIdx = (floor(tropFrac * 12.0).toInt()) % 12
        val zodiacSign = ZODIAC_CONSTELLATIONS[zodiacIdx]

        // Check if this specific day is the closest day to one of the 4 major phases
        val prevFrac = rawPhaseFractionForJd(jd - 1.0)
        val nextFrac = rawPhaseFractionForJd(jd + 1.0)

        fun distToTarget(f: Double, target: Double): Double {
            val d = abs(f - target)
            return minOf(d, 1.0 - d)
        }

        val isExactNew = distToTarget(phaseFraction, 0.0) <= distToTarget(prevFrac, 0.0) &&
            distToTarget(phaseFraction, 0.0) < distToTarget(nextFrac, 0.0) &&
            distToTarget(phaseFraction, 0.0) < 0.035

        val isExactFirstQuarter = distToTarget(phaseFraction, 0.25) <= distToTarget(prevFrac, 0.25) &&
            distToTarget(phaseFraction, 0.25) < distToTarget(nextFrac, 0.25) &&
            distToTarget(phaseFraction, 0.25) < 0.035

        val isExactFull = distToTarget(phaseFraction, 0.50) <= distToTarget(prevFrac, 0.50) &&
            distToTarget(phaseFraction, 0.50) < distToTarget(nextFrac, 0.50) &&
            distToTarget(phaseFraction, 0.50) < 0.035

        val isExactLastQuarter = distToTarget(phaseFraction, 0.75) <= distToTarget(prevFrac, 0.75) &&
            distToTarget(phaseFraction, 0.75) < distToTarget(nextFrac, 0.75) &&
            distToTarget(phaseFraction, 0.75) < 0.035

        val phaseType = when {
            isExactNew -> MoonPhaseType.NEW_MOON
            isExactFirstQuarter -> MoonPhaseType.FIRST_QUARTER
            isExactFull -> MoonPhaseType.FULL_MOON
            isExactLastQuarter -> MoonPhaseType.LAST_QUARTER
            phaseFraction < 0.03 -> MoonPhaseType.NEW_MOON
            phaseFraction < 0.22 -> MoonPhaseType.WAXING_CRESCENT
            phaseFraction < 0.28 -> MoonPhaseType.FIRST_QUARTER
            phaseFraction < 0.47 -> MoonPhaseType.WAXING_GIBBOUS
            phaseFraction < 0.53 -> MoonPhaseType.FULL_MOON
            phaseFraction < 0.72 -> MoonPhaseType.WANING_GIBBOUS
            phaseFraction < 0.78 -> MoonPhaseType.LAST_QUARTER
            phaseFraction < 0.97 -> MoonPhaseType.WANING_CRESCENT
            else -> MoonPhaseType.NEW_MOON
        }

        val isExactMajor = isExactNew || isExactFirstQuarter || isExactFull || isExactLastQuarter

        return LunarDayInfo(
            date = date,
            phaseFraction = phaseFraction,
            illuminationPercent = illumination,
            lunarAgeDays = lunarAge,
            distanceKm = distanceKm,
            zodiacSign = zodiacSign,
            phaseType = phaseType,
            isExactMajorPhaseDay = isExactMajor
        )
    }

    /**
     * Generates a 42-cell (6 weeks, Monday-first) calendar grid for a given year and month (1..12).
     */
    fun getMonthGrid(year: Int, month: Int, today: SimpleDate = today()): List<CalendarDayCell> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 12, 0, 0)
        // Convert Sunday=1..Saturday=7 to Monday=0..Sunday=6
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingDays = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        val firstCellDate = addDays(SimpleDate(year, month, 1), -leadingDays)

        return (0 until 42).map { offset ->
            val cellDate = addDays(firstCellDate, offset)
            CalendarDayCell(
                date = cellDate,
                isCurrentMonth = cellDate.year == year && cellDate.month == month,
                isToday = cellDate == today,
                lunarInfo = getLunarInfo(cellDate)
            )
        }
    }

    /**
     * Returns the major lunar phase milestones occurring within a specific month.
     */
    fun getMonthMajorMilestones(year: Int, month: Int): List<MajorPhaseMilestone> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 12, 0, 0)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val milestones = mutableListOf<MajorPhaseMilestone>()
        for (day in 1..daysInMonth) {
            val info = getLunarInfo(SimpleDate(year, month, day))
            if (info.isExactMajorPhaseDay) {
                milestones.add(
                    MajorPhaseMilestone(
                        date = info.date,
                        phaseType = info.phaseType,
                        illuminationPercent = info.illuminationPercent.roundToInt()
                    )
                )
            }
        }
        return milestones
    }

    /**
     * Finds the next [count] exact major moon phases starting from [startDate].
     */
    fun getUpcomingMajorPhases(startDate: SimpleDate, count: Int = 4): List<MajorPhaseMilestone> {
        val result = mutableListOf<MajorPhaseMilestone>()
        var offset = 0
        while (result.size < count && offset < 45) {
            val candidate = addDays(startDate, offset)
            val info = getLunarInfo(candidate)
            if (info.isExactMajorPhaseDay) {
                result.add(
                    MajorPhaseMilestone(
                        date = candidate,
                        phaseType = info.phaseType,
                        illuminationPercent = info.illuminationPercent.roundToInt()
                    )
                )
            }
            offset++
        }
        return result
    }
}
