package com.example.domain

import androidx.compose.runtime.Immutable
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class MoonPhaseType(
    val spanishName: String,
    val shortLabel: String,
    val emoji: String,
    val isMajorPhase: Boolean
) {
    NEW_MOON(
        spanishName = "Luna Nueva",
        shortLabel = "Nueva",
        emoji = "🌑",
        isMajorPhase = true
    ),
    WAXING_CRESCENT(
        spanishName = "Luna Creciente",
        shortLabel = "Creciente",
        emoji = "🌒",
        isMajorPhase = false
    ),
    FIRST_QUARTER(
        spanishName = "Cuarto Creciente",
        shortLabel = "C. Creciente",
        emoji = "🌓",
        isMajorPhase = true
    ),
    WAXING_GIBBOUS(
        spanishName = "Gibosa Creciente",
        shortLabel = "Gibosa Crec.",
        emoji = "🌔",
        isMajorPhase = false
    ),
    FULL_MOON(
        spanishName = "Luna Llena",
        shortLabel = "Llena",
        emoji = "🌕",
        isMajorPhase = true
    ),
    WANING_GIBBOUS(
        spanishName = "Gibosa Menguante",
        shortLabel = "Gibosa Meng.",
        emoji = "🌖",
        isMajorPhase = false
    ),
    LAST_QUARTER(
        spanishName = "Cuarto Menguante",
        shortLabel = "C. Menguante",
        emoji = "🌗",
        isMajorPhase = true
    ),
    WANING_CRESCENT(
        spanishName = "Luna Menguante",
        shortLabel = "Menguante",
        emoji = "🌘",
        isMajorPhase = false
    )
}

enum class LunarEclipseKind {
    NONE,
    PENUMBRAL,
    PARTIAL,
    TOTAL
}

enum class SolarEclipseKind {
    NONE,
    PARTIAL,
    ANNULAR,
    HYBRID,
    TOTAL
}

@Immutable
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

    val storageKey: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        formatIsoDateFast(year, month, day)
    }

    fun toPackedKey(): Int = (year shl 9) or (month shl 5) or day

    fun toStorageKey(): String = storageKey

    fun formatSpanishLong(): String {
        val dayOfWeek = LunarAstronomer.getDayOfWeekSpanish(year, month, day)
        return "$dayOfWeek, $day de ${MONTH_NAMES_LOWER[(month - 1).coerceIn(0, 11)]} de $year"
    }

    fun formatSpanishShort(): String {
        return "$day ${MONTH_NAMES_SHORT[(month - 1).coerceIn(0, 11)]} $year"
    }

    companion object {
        private val MONTH_NAMES_LOWER = arrayOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        private val MONTH_NAMES_SHORT = arrayOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        )

        private fun formatIsoDateFast(year: Int, month: Int, day: Int): String {
            val y = year.coerceIn(0, 9999)
            val m = month.coerceIn(1, 12)
            val d = day.coerceIn(1, 31)
            val chars = CharArray(10)
            chars[0] = ('0'.code + (y / 1000)).toChar()
            chars[1] = ('0'.code + ((y / 100) % 10)).toChar()
            chars[2] = ('0'.code + ((y / 10) % 10)).toChar()
            chars[3] = ('0'.code + (y % 10)).toChar()
            chars[4] = '-'
            chars[5] = ('0'.code + (m / 10)).toChar()
            chars[6] = ('0'.code + (m % 10)).toChar()
            chars[7] = '-'
            chars[8] = ('0'.code + (d / 10)).toChar()
            chars[9] = ('0'.code + (d % 10)).toChar()
            return String(chars)
        }
    }
}

@Immutable
data class LunarDayInfo(
    val date: SimpleDate,
    val phaseFraction: Double,          // 0.0 (New) -> 0.5 (Full) -> 1.0 (New)
    val illuminationPercent: Double,    // 0.0 .. 100.0 (true selenocentric phase angle)
    val lunarAgeDays: Double,           // Days elapsed since exact preceding New Moon
    val distanceKm: Int,                // ~356,400 .. 406,700 km (Meeus Ch. 47)
    val angularDiameterArcmin: Double,  // ~29.4' .. 33.5' apparent lunar diameter
    val eclipticLongitudeDeg: Double,   // 0.0° .. 360.0° geocentric ecliptic longitude
    val eclipticLatitudeDeg: Double,    // -5.3° .. +5.3° geocentric ecliptic latitude
    val zodiacSign: String,             // Real ecliptic sector from true longitude
    val zodiacDegreeInSign: Double,     // 0.0° .. 30.0° within sector
    val phaseType: MoonPhaseType,
    val isExactMajorPhaseDay: Boolean,
    val isSupermoon: Boolean = false,
    val lunarEclipseKind: LunarEclipseKind = LunarEclipseKind.NONE,
    val solarEclipseKind: SolarEclipseKind = SolarEclipseKind.NONE
)

@Immutable
data class CalendarDayCell(
    val date: SimpleDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val lunarInfo: LunarDayInfo
)

@Immutable
data class MajorPhaseMilestone(
    val date: SimpleDate,
    val phaseType: MoonPhaseType,
    val illuminationPercent: Int
)

enum class CelestialEventKind {
    SUPERMOON,
    LUNAR_ECLIPSE,
    SOLAR_ECLIPSE
}

@Immutable
data class CelestialEvent(
    val date: SimpleDate,
    val kind: CelestialEventKind,
    val title: String,
    val subtitle: String,
    val visibilityRegion: String,
    val phaseType: MoonPhaseType
)

/**
 * Pure astronomical ephemeris engine implementing Jean Meeus ("Astronomical Algorithms",
 * 2nd Ed., Chapters 25, 47, 48, 49, and 54).
 * Zero hardcoded lookup tables, zero simulated local horizon guesses, zero location permissions.
 */
object LunarAstronomer {
    const val SYNODIC_MONTH_DAYS = 29.530588861
    private const val J2000_JD = 2451545.0
    private const val MEEUS_EPOCH_NEW_MOON_JDE = 2451550.09766
    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI
    private const val MOON_MEAN_RADIUS_KM = 1737.4
    private const val AU_IN_KM = 149597870.7

    private val ZODIAC_SECTORS = listOf(
        "Aries ♈", "Tauro ♉", "Géminis ♊", "Cáncer ♋",
        "Leo ♌", "Virgo ♍", "Libra ♎", "Escorpio ♏",
        "Sagitario ♐", "Capricornio ♑", "Acuario ♒", "Piscis ♓"
    )

    val SPANISH_MONTHS = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val SPANISH_WEEKDAYS_SHORT = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    private val WEEKDAY_NAMES_FROM_MONDAY = arrayOf(
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    )

    // Bounded LRU cache (512 dates) so multi-month grids and upcoming ephemeris searches never thrash
    private val lunarInfoCache = object : LinkedHashMap<Int, LunarDayInfo>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, LunarDayInfo>?): Boolean {
            return size > 512
        }
    }

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

    private fun getDayOfWeekIndexFromMonday(year: Int, month: Int, day: Int): Int {
        val jdNoon = toJulianDay(year, month, day)
        return (((floor(jdNoon + 0.5).toLong() % 7L) + 7L).toInt() % 7)
    }

    fun getDayOfWeekSpanish(year: Int, month: Int, day: Int): String {
        return WEEKDAY_NAMES_FROM_MONDAY[getDayOfWeekIndexFromMonday(year, month, day)]
    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    private fun norm360(deg: Double): Double {
        var r = deg % 360.0
        if (r < 0.0) r += 360.0
        return r
    }

    private fun norm180(deg: Double): Double {
        var r = norm360(deg)
        if (r > 180.0) r -= 360.0
        return r
    }

    private fun sinDeg(deg: Double): Double = sin(deg * DEG_TO_RAD)
    private fun cosDeg(deg: Double): Double = cos(deg * DEG_TO_RAD)

    /**
     * Approximate Terrestrial Time minus Universal Time (Delta-T = TT - UTC) in days
     * using NASA Espenak & Meeus polynomial approximations.
     */
    private fun estimateDeltaTDays(decimalYear: Double): Double {
        val deltaTSeconds = when {
            decimalYear < 2005.0 -> {
                val t = decimalYear - 2000.0
                63.86 + 0.3345 * t - 0.060374 * t * t
            }
            decimalYear <= 2050.0 -> {
                val t = decimalYear - 2000.0
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
            else -> {
                val u = (decimalYear - 1820.0) / 100.0
                -20.0 + 32.0 * u * u
            }
        }
        return deltaTSeconds / 86400.0
    }

    /**
     * Computes exact UTC Julian Day of a true lunar phase (Jean Meeus Chapter 49):
     * [quarterIndex]: 0 = New Moon, 1 = First Quarter, 2 = Full Moon, 3 = Last Quarter.
     */
    private fun computeExactPhaseJdUtc(lunationK: Int, quarterIndex: Int): Double {
        val kEff = lunationK.toDouble() + quarterIndex * 0.25
        val t = kEff / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        val jdeMean = MEEUS_EPOCH_NEW_MOON_JDE +
            SYNODIC_MONTH_DAYS * kEff +
            0.00015437 * t2 -
            0.000000150 * t3 +
            0.00000000073 * t4

        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val m = norm360(2.5534 + 29.10535670 * kEff - 0.0000014 * t2 - 0.00000011 * t3)
        val mPrime = norm360(
            201.5643 + 385.81693528 * kEff + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4
        )
        val f = norm360(
            160.7108 + 390.67050284 * kEff - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4
        )
        val omega = norm360(124.7746 - 1.56375588 * kEff + 0.0020672 * t2 + 0.00000215 * t3)

        val correction = when (quarterIndex) {
            0 -> { // New Moon
                -0.40720 * sinDeg(mPrime) +
                    0.17241 * e * sinDeg(m) +
                    0.01608 * sinDeg(2.0 * mPrime) +
                    0.01039 * sinDeg(2.0 * f) +
                    0.00739 * e * sinDeg(mPrime - m) -
                    0.00514 * e * sinDeg(mPrime + m) +
                    0.00208 * e * e * sinDeg(2.0 * m) -
                    0.00111 * sinDeg(mPrime - 2.0 * f) -
                    0.00057 * sinDeg(mPrime + 2.0 * f) +
                    0.00056 * e * sinDeg(2.0 * mPrime + m) -
                    0.00042 * sinDeg(3.0 * mPrime) +
                    0.00042 * e * sinDeg(m + 2.0 * f) +
                    0.00038 * e * sinDeg(m - 2.0 * f) -
                    0.00024 * e * sinDeg(2.0 * mPrime - m) -
                    0.00017 * sinDeg(omega)
            }
            2 -> { // Full Moon
                -0.40614 * sinDeg(mPrime) +
                    0.17302 * e * sinDeg(m) +
                    0.01614 * sinDeg(2.0 * mPrime) +
                    0.01043 * sinDeg(2.0 * f) +
                    0.00734 * e * sinDeg(mPrime - m) -
                    0.00515 * e * sinDeg(mPrime + m) +
                    0.00209 * e * e * sinDeg(2.0 * m) -
                    0.00111 * sinDeg(mPrime - 2.0 * f) -
                    0.00057 * sinDeg(mPrime + 2.0 * f) +
                    0.00056 * e * sinDeg(2.0 * mPrime + m) -
                    0.00042 * sinDeg(3.0 * mPrime) +
                    0.00042 * e * sinDeg(m + 2.0 * f) +
                    0.00038 * e * sinDeg(m - 2.0 * f) -
                    0.00024 * e * sinDeg(2.0 * mPrime - m) -
                    0.00017 * sinDeg(omega)
            }
            else -> { // First Quarter (1) or Last Quarter (3)
                val baseQ = -0.62801 * sinDeg(mPrime) +
                    0.17172 * e * sinDeg(m) -
                    0.01183 * e * sinDeg(mPrime + m) +
                    0.00862 * sinDeg(2.0 * mPrime) +
                    0.00804 * sinDeg(2.0 * f) +
                    0.00454 * e * sinDeg(mPrime - m) +
                    0.00204 * e * e * sinDeg(2.0 * m) -
                    0.00180 * sinDeg(mPrime - 2.0 * f) -
                    0.00070 * sinDeg(mPrime + 2.0 * f) -
                    0.00040 * sinDeg(3.0 * mPrime) -
                    0.00034 * e * sinDeg(2.0 * mPrime - m) +
                    0.00032 * e * sinDeg(m + 2.0 * f) +
                    0.00032 * e * sinDeg(m - 2.0 * f) -
                    0.00017 * sinDeg(omega)
                val w = 0.00306 -
                    0.00038 * e * cosDeg(m) +
                    0.00026 * cosDeg(mPrime) -
                    0.00002 * cosDeg(mPrime - m) +
                    0.00002 * cosDeg(mPrime + m) +
                    0.00002 * cosDeg(2.0 * f)
                if (quarterIndex == 1) baseQ + w else baseQ - w
            }
        }

        val jdeTrue = jdeMean + correction
        val approxYear = 2000.0 + (jdeTrue - J2000_JD) / 365.2425
        return jdeTrue - estimateDeltaTDays(approxYear)
    }

    private data class ComputedSolarEclipse(
        val date: SimpleDate,
        val jdMaxUtc: Double,
        val kind: SolarEclipseKind,
        val gamma: Double,
        val u: Double,
        val partialMagnitude: Double
    )

    private data class ComputedLunarEclipse(
        val date: SimpleDate,
        val jdMaxUtc: Double,
        val kind: LunarEclipseKind,
        val gamma: Double,
        val umbralMagnitude: Double,
        val penumbralMagnitude: Double
    )

    /**
     * Dynamically computes Solar Eclipse Besselian elements for New Moon [lunationK]
     * using Jean Meeus Chapter 54 (works for any century without lookup tables).
     */
    private fun evaluateSolarEclipseAtLunation(lunationK: Int): ComputedSolarEclipse? {
        val kEff = lunationK.toDouble()
        val t = kEff / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        val f = norm360(
            160.7108 + 390.67050284 * kEff - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4
        )
        if (abs(sinDeg(f)) > 0.36) return null

        val jdeMean = MEEUS_EPOCH_NEW_MOON_JDE +
            SYNODIC_MONTH_DAYS * kEff +
            0.00015437 * t2 -
            0.000000150 * t3 +
            0.00000000073 * t4

        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val m = norm360(2.5534 + 29.10535670 * kEff - 0.0000014 * t2 - 0.00000011 * t3)
        val mPrime = norm360(
            201.5643 + 385.81693528 * kEff + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4
        )
        val omega = norm360(124.7746 - 1.56375588 * kEff + 0.0020672 * t2 + 0.00000215 * t3)
        val f1 = f - 0.02665 * sinDeg(omega)
        val a1 = norm360(299.77 + 0.107408 * kEff - 0.009173 * t2)

        val jdeMax = jdeMean -
            0.4075 * sinDeg(mPrime) +
            0.1721 * e * sinDeg(m) +
            0.0161 * sinDeg(2.0 * mPrime) -
            0.0097 * sinDeg(2.0 * f1) +
            0.0073 * e * sinDeg(mPrime - m) -
            0.0050 * e * sinDeg(mPrime + m) -
            0.0023 * sinDeg(mPrime - 2.0 * f1) +
            0.0021 * e * sinDeg(2.0 * m) +
            0.0012 * sinDeg(mPrime + 2.0 * f1) +
            0.0006 * e * sinDeg(2.0 * mPrime + m) -
            0.0004 * sinDeg(3.0 * mPrime) -
            0.0003 * e * sinDeg(m + 2.0 * f1) +
            0.0003 * sinDeg(a1) -
            0.0002 * e * sinDeg(m - 2.0 * f1) -
            0.0002 * e * sinDeg(2.0 * mPrime - m) -
            0.0002 * sinDeg(omega)

        val p = 0.2070 * e * sinDeg(m) +
            0.0024 * e * sinDeg(2.0 * m) -
            0.0392 * sinDeg(mPrime) +
            0.0116 * sinDeg(2.0 * mPrime) -
            0.0073 * e * sinDeg(mPrime + m) +
            0.0067 * e * sinDeg(mPrime - m) +
            0.0118 * sinDeg(2.0 * f1)

        val q = 5.2207 -
            0.0048 * e * cosDeg(m) +
            0.0020 * e * cosDeg(2.0 * m) -
            0.3299 * cosDeg(mPrime) -
            0.0060 * e * cosDeg(mPrime + m) +
            0.0041 * e * cosDeg(mPrime - m)

        val w = abs(cosDeg(f1))
        val gamma = (p * cosDeg(f1) + q * sinDeg(f1)) * (1.0 - 0.0048 * w)
        val u = 0.0059 +
            0.0046 * e * cosDeg(m) -
            0.0182 * cosDeg(mPrime) +
            0.0004 * cosDeg(2.0 * mPrime) -
            0.0005 * cosDeg(m + mPrime)

        val absGamma = abs(gamma)
        val outerLimit = 1.5433 + u
        if (absGamma > outerLimit) return null

        val kind = when {
            absGamma <= 0.9972 -> {
                when {
                    u < 0.0 -> SolarEclipseKind.TOTAL
                    u > 0.0047 -> SolarEclipseKind.ANNULAR
                    else -> {
                        val omegaHybrid = 0.00464 * sqrt((1.0 - gamma * gamma).coerceAtLeast(0.0))
                        if (u < omegaHybrid) SolarEclipseKind.HYBRID else SolarEclipseKind.ANNULAR
                    }
                }
            }
            absGamma < 0.9972 + abs(u) -> {
                if (u < 0.0) SolarEclipseKind.TOTAL else SolarEclipseKind.ANNULAR
            }
            else -> SolarEclipseKind.PARTIAL
        }

        val partialMag = ((1.5433 + u - absGamma) / (0.5461 + 2.0 * u)).coerceIn(0.01, 1.0)
        val approxYear = 2000.0 + (jdeMax - J2000_JD) / 365.2425
        val jdMaxUtc = jdeMax - estimateDeltaTDays(approxYear)
        return ComputedSolarEclipse(
            date = fromJulianDay(jdMaxUtc),
            jdMaxUtc = jdMaxUtc,
            kind = kind,
            gamma = gamma,
            u = u,
            partialMagnitude = partialMag
        )
    }

    /**
     * Dynamically computes Lunar Eclipse Besselian elements for Full Moon [lunationK] + 0.5
     * using Jean Meeus Chapter 54 (works for any century without lookup tables).
     */
    private fun evaluateLunarEclipseAtLunation(lunationK: Int): ComputedLunarEclipse? {
        val kEff = lunationK.toDouble() + 0.5
        val t = kEff / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        val f = norm360(
            160.7108 + 390.67050284 * kEff - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4
        )
        if (abs(sinDeg(f)) > 0.36) return null

        val jdeMean = MEEUS_EPOCH_NEW_MOON_JDE +
            SYNODIC_MONTH_DAYS * kEff +
            0.00015437 * t2 -
            0.000000150 * t3 +
            0.00000000073 * t4

        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val m = norm360(2.5534 + 29.10535670 * kEff - 0.0000014 * t2 - 0.00000011 * t3)
        val mPrime = norm360(
            201.5643 + 385.81693528 * kEff + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4
        )
        val omega = norm360(124.7746 - 1.56375588 * kEff + 0.0020672 * t2 + 0.00000215 * t3)
        val f1 = f - 0.02665 * sinDeg(omega)
        val a1 = norm360(299.77 + 0.107408 * kEff - 0.009173 * t2)

        val jdeMax = jdeMean -
            0.4065 * sinDeg(mPrime) +
            0.1727 * e * sinDeg(m) +
            0.0161 * sinDeg(2.0 * mPrime) -
            0.0097 * sinDeg(2.0 * f1) +
            0.0073 * e * sinDeg(mPrime - m) -
            0.0050 * e * sinDeg(mPrime + m) -
            0.0023 * sinDeg(mPrime - 2.0 * f1) +
            0.0021 * e * sinDeg(2.0 * m) +
            0.0012 * sinDeg(mPrime + 2.0 * f1) +
            0.0006 * e * sinDeg(2.0 * mPrime + m) -
            0.0004 * sinDeg(3.0 * mPrime) -
            0.0003 * e * sinDeg(m + 2.0 * f1) +
            0.0003 * sinDeg(a1) -
            0.0002 * e * sinDeg(m - 2.0 * f1) -
            0.0002 * e * sinDeg(2.0 * mPrime - m) -
            0.0002 * sinDeg(omega)

        val p = 0.2070 * e * sinDeg(m) +
            0.0024 * e * sinDeg(2.0 * m) -
            0.0392 * sinDeg(mPrime) +
            0.0116 * sinDeg(2.0 * mPrime) -
            0.0073 * e * sinDeg(mPrime + m) +
            0.0067 * e * sinDeg(mPrime - m) +
            0.0118 * sinDeg(2.0 * f1)

        val q = 5.2207 -
            0.0048 * e * cosDeg(m) +
            0.0020 * e * cosDeg(2.0 * m) -
            0.3299 * cosDeg(mPrime) -
            0.0060 * e * cosDeg(mPrime + m) +
            0.0041 * e * cosDeg(mPrime - m)

        val w = abs(cosDeg(f1))
        val gamma = (p * cosDeg(f1) + q * sinDeg(f1)) * (1.0 - 0.0048 * w)
        val u = 0.0059 +
            0.0046 * e * cosDeg(m) -
            0.0182 * cosDeg(mPrime) +
            0.0004 * cosDeg(2.0 * mPrime) -
            0.0005 * cosDeg(m + mPrime)

        val absGamma = abs(gamma)
        val penumbralMag = (1.5573 + u - absGamma) / 0.5450
        if (penumbralMag <= 0.0) return null

        val umbralMag = (1.0128 - u - absGamma) / 0.5450
        val kind = when {
            umbralMag >= 1.0 -> LunarEclipseKind.TOTAL
            umbralMag > 0.0 -> LunarEclipseKind.PARTIAL
            else -> LunarEclipseKind.PENUMBRAL
        }

        val approxYear = 2000.0 + (jdeMax - J2000_JD) / 365.2425
        val jdMaxUtc = jdeMax - estimateDeltaTDays(approxYear)
        return ComputedLunarEclipse(
            date = fromJulianDay(jdMaxUtc),
            jdMaxUtc = jdMaxUtc,
            kind = kind,
            gamma = gamma,
            umbralMagnitude = umbralMag,
            penumbralMagnitude = penumbralMag
        )
    }

    private fun formatUtcTimeFromJd(jdUtc: Double): String {
        val dayFrac = (jdUtc + 0.5) - floor(jdUtc + 0.5)
        val totalMinutes = ((dayFrac * 1440.0).roundToInt() % 1440 + 1440) % 1440
        val hh = totalMinutes / 60
        val mm = totalMinutes % 60
        return "%02d:%02d UTC".format(Locale.US, hh, mm)
    }

    private fun describeSolarEclipseVisibility(jdMaxUtc: Double, gamma: Double): String {
        val dayFrac = (jdMaxUtc + 0.5) - floor(jdMaxUtc + 0.5)
        val subSolarLon = norm180((0.5 - dayFrac) * 360.0)
        val latBand = when {
            gamma > 0.55 -> "Latitudes boreales"
            gamma < -0.55 -> "Latitudes australes"
            else -> "Franja tropical y templada"
        }
        val lonSector = when {
            subSolarLon in -45.0..45.0 -> "Europa, África y Atlántico"
            subSolarLon in 45.0..135.0 -> "Asia, Índico y Oceanía"
            subSolarLon in -135.0..-45.0 -> "América y Atlántico"
            else -> "Pacífico, Asia Oriental y América"
        }
        return "$latBand • $lonSector (Máx. ${formatUtcTimeFromJd(jdMaxUtc)})"
    }

    private fun describeLunarEclipseVisibility(jdMaxUtc: Double): String {
        val dayFrac = (jdMaxUtc + 0.5) - floor(jdMaxUtc + 0.5)
        val subLunarLon = norm180((0.0 - dayFrac) * 360.0)
        val nightHemisphere = when {
            subLunarLon in -45.0..45.0 -> "Europa, África y Atlántico"
            subLunarLon in 45.0..135.0 -> "Asia, Índico y Oceanía"
            subLunarLon in -135.0..-45.0 -> "América y Atlántico"
            else -> "Pacífico, América y Asia Oriental"
        }
        return "Hemisferio nocturno: $nightHemisphere (Máx. ${formatUtcTimeFromJd(jdMaxUtc)})"
    }

    fun getLunarInfo(date: SimpleDate): LunarDayInfo {
        val key = date.toPackedKey()
        synchronized(lunarInfoCache) {
            lunarInfoCache[key]?.let { return it }
        }
        val computed = computeLunarInfoInternal(date)
        synchronized(lunarInfoCache) {
            lunarInfoCache[key] = computed
        }
        return computed
    }

    private fun computeLunarInfoInternal(date: SimpleDate): LunarDayInfo {
        val jdUtcNoon = toJulianDay(date.year, date.month, date.day)
        val approxYear = date.year + (date.month - 0.5) / 12.0
        val jde = jdUtcNoon + estimateDeltaTDays(approxYear)

        // Julian centuries from J2000.0 (Meeus Ch. 47)
        val t = (jde - J2000_JD) / 36525.0
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        // Fundamental arguments (degrees)
        val lPrime = norm360(
            218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0
        )
        val d = norm360(
            297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0
        )
        val m = norm360(
            357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0
        )
        val mPrime = norm360(
            134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0
        )
        val f = norm360(
            93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0
        )
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val e2 = e * e

        // Meeus Table 47.A: Periodic perturbations for Ecliptic Longitude (degrees)
        val sigmaL = 6.288774 * sinDeg(mPrime) +
            1.274027 * sinDeg(2.0 * d - mPrime) +
            0.658314 * sinDeg(2.0 * d) +
            0.213618 * sinDeg(2.0 * mPrime) -
            0.185116 * e * sinDeg(m) -
            0.114332 * sinDeg(2.0 * f) +
            0.058793 * sinDeg(2.0 * d - 2.0 * mPrime) +
            0.057066 * e * sinDeg(2.0 * d - m - mPrime) +
            0.053322 * sinDeg(2.0 * d + mPrime) +
            0.045758 * e * sinDeg(2.0 * d - m) -
            0.040923 * e * sinDeg(m - mPrime) -
            0.034720 * sinDeg(d) -
            0.030383 * e * sinDeg(m + mPrime) +
            0.015327 * sinDeg(2.0 * d - 2.0 * f) -
            0.012528 * sinDeg(mPrime + 2.0 * f) +
            0.010980 * sinDeg(mPrime - 2.0 * f)

        val lambdaMoon = norm360(lPrime + sigmaL)

        // Meeus Table 47.B: Periodic perturbations for Ecliptic Latitude (degrees)
        val betaMoon = 5.128122 * sinDeg(f) +
            0.280602 * sinDeg(mPrime + f) +
            0.277693 * sinDeg(mPrime - f) +
            0.173237 * sinDeg(2.0 * d - f) +
            0.055413 * sinDeg(2.0 * d - mPrime + f) +
            0.046271 * sinDeg(2.0 * d - mPrime - f) +
            0.032573 * sinDeg(2.0 * d + f) +
            0.017198 * sinDeg(2.0 * mPrime + f)

        // Meeus Table 47.A: Periodic perturbations for Earth-Moon Distance (km)
        val sigmaR = -20905.355 * cosDeg(mPrime) -
            3699.111 * cosDeg(2.0 * d - mPrime) -
            2955.968 * cosDeg(2.0 * d) -
            569.925 * cosDeg(2.0 * mPrime) +
            48.888 * e * cosDeg(m) -
            3.149 * cosDeg(2.0 * f) +
            246.158 * cosDeg(2.0 * d - 2.0 * mPrime) -
            152.138 * e * cosDeg(2.0 * d - m - mPrime) -
            170.733 * cosDeg(2.0 * d + mPrime) -
            204.586 * e * cosDeg(2.0 * d - m) -
            129.620 * e * cosDeg(m - mPrime) +
            108.743 * cosDeg(d) +
            104.755 * e * cosDeg(m + mPrime) +
            79.661 * cosDeg(mPrime - 2.0 * f) -
            34.782 * cosDeg(4.0 * d - mPrime) +
            23.210 * e2 * cosDeg(2.0 * d - 2.0 * m)

        val distanceKmDouble = 385000.56 + sigmaR
        val distanceKm = distanceKmDouble.roundToInt()

        // Apparent angular diameter in arcminutes: 2 * arcsin(R_moon / Delta)
        val angularDiameterDeg = 2.0 * asin((MOON_MEAN_RADIUS_KM / distanceKmDouble).coerceIn(0.0, 1.0)) * RAD_TO_DEG
        val angularDiameterArcmin = angularDiameterDeg * 60.0

        // Solar true ecliptic longitude & distance (Meeus Ch. 25)
        val l0Sun = norm360(280.46646 + 36000.76983 * t + 0.0003032 * t2)
        val cSun = (1.914602 - 0.004817 * t - 0.000014 * t2) * sinDeg(m) +
            (0.019993 - 0.000101 * t) * sinDeg(2.0 * m) +
            0.000289 * sinDeg(3.0 * m)
        val lambdaSun = norm360(l0Sun + cSun)
        val earthEcc = 0.016708634 - 0.000042037 * t - 0.0000001267 * t2
        val sunTrueAnom = m + cSun
        val sunDistAu = (1.000001018 * (1.0 - earthEcc * earthEcc)) / (1.0 + earthEcc * cosDeg(sunTrueAnom))
        val sunDistKm = sunDistAu * AU_IN_KM

        // True geocentric elongation & selenocentric phase angle i (Meeus Ch. 48 eq. 48.1-48.2)
        val deltaLambda = norm360(lambdaMoon - lambdaSun)
        val cosPsi = (cosDeg(betaMoon) * cosDeg(deltaLambda)).coerceIn(-1.0, 1.0)
        val psiRad = acos(cosPsi)
        val phaseAngleRad = atan2(
            sunDistKm * sin(psiRad),
            distanceKmDouble - sunDistKm * cosPsi
        )
        val illumination = ((1.0 + cos(phaseAngleRad)) / 2.0 * 100.0).coerceIn(0.0, 100.0)
        val phaseFraction = (deltaLambda / 360.0).coerceIn(0.0, 1.0)

        // Real ecliptic sector & degree within sector
        val zodiacIdx = (floor(lambdaMoon / 30.0).toInt()).coerceIn(0, 11)
        val zodiacSign = ZODIAC_SECTORS[zodiacIdx]
        val zodiacDegreeInSign = lambdaMoon - (zodiacIdx * 30.0)

        // Exact major phase detection & true lunar age via Meeus Ch. 49
        val kApprox = floor((jdUtcNoon - MEEUS_EPOCH_NEW_MOON_JDE) / SYNODIC_MONTH_DAYS).toInt()
        var exactMajorQuarter: Int? = null
        var matchedLunationK: Int = kApprox
        var latestPrecedingNewMoonJd = jdUtcNoon
        var foundPrecedingNewMoon = false
        var sameDayNewMoonJd: Double? = null

        for (k in (kApprox - 2)..(kApprox + 1)) {
            val nmJd = computeExactPhaseJdUtc(k, 0)
            if (nmJd <= jdUtcNoon) {
                if (!foundPrecedingNewMoon || nmJd > latestPrecedingNewMoonJd) {
                    latestPrecedingNewMoonJd = nmJd
                    foundPrecedingNewMoon = true
                }
            }
            for (q in 0..3) {
                val qJd = if (q == 0) nmJd else computeExactPhaseJdUtc(k, q)
                if (fromJulianDay(qJd) == date) {
                    exactMajorQuarter = q
                    matchedLunationK = k
                    if (q == 0) {
                        sameDayNewMoonJd = qJd
                    }
                }
            }
        }

        val lunarAgeDays = when {
            sameDayNewMoonJd != null -> (jdUtcNoon - sameDayNewMoonJd).coerceAtLeast(0.0)
            foundPrecedingNewMoon -> (jdUtcNoon - latestPrecedingNewMoonJd).coerceIn(0.0, 30.0)
            else -> phaseFraction * SYNODIC_MONTH_DAYS
        }

        val phaseType = when (exactMajorQuarter) {
            0 -> MoonPhaseType.NEW_MOON
            1 -> MoonPhaseType.FIRST_QUARTER
            2 -> MoonPhaseType.FULL_MOON
            3 -> MoonPhaseType.LAST_QUARTER
            else -> when {
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
        }

        val isExactMajor = exactMajorQuarter != null
        val isSupermoon = (exactMajorQuarter == 2 || phaseType == MoonPhaseType.FULL_MOON) && distanceKm <= 363500

        // Check if this exact New/Full Moon day hosts a real Solar or Lunar Eclipse (Meeus Ch. 54)
        var lunarEclipseKind = LunarEclipseKind.NONE
        var solarEclipseKind = SolarEclipseKind.NONE
        if (exactMajorQuarter == 0) {
            val se = evaluateSolarEclipseAtLunation(matchedLunationK)
            if (se != null && se.date == date) {
                solarEclipseKind = se.kind
            }
        } else if (exactMajorQuarter == 2) {
            val le = evaluateLunarEclipseAtLunation(matchedLunationK)
            if (le != null && le.date == date) {
                lunarEclipseKind = le.kind
            }
        }

        return LunarDayInfo(
            date = date,
            phaseFraction = phaseFraction,
            illuminationPercent = illumination,
            lunarAgeDays = lunarAgeDays,
            distanceKm = distanceKm,
            angularDiameterArcmin = angularDiameterArcmin,
            eclipticLongitudeDeg = lambdaMoon,
            eclipticLatitudeDeg = betaMoon,
            zodiacSign = zodiacSign,
            zodiacDegreeInSign = zodiacDegreeInSign,
            phaseType = phaseType,
            isExactMajorPhaseDay = isExactMajor,
            isSupermoon = isSupermoon,
            lunarEclipseKind = lunarEclipseKind,
            solarEclipseKind = solarEclipseKind
        )
    }

    /**
     * Generates a 42-cell (6 weeks, Monday-first) calendar grid for a given year and month (1..12).
     */
    fun getMonthGrid(year: Int, month: Int, today: SimpleDate = today()): List<CalendarDayCell> {
        val leadingDays = getDayOfWeekIndexFromMonday(year, month, 1)
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
        val totalDays = daysInMonth(year, month)
        val milestones = ArrayList<MajorPhaseMilestone>(5)
        for (day in 1..totalDays) {
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
     * Finds the next [count] exact major moon phases starting from [startDate] using Meeus Ch. 49.
     */
    fun getUpcomingMajorPhases(startDate: SimpleDate, count: Int = 4): List<MajorPhaseMilestone> {
        val startJd = toJulianDay(startDate.year, startDate.month, startDate.day)
        val kStart = floor((startJd - MEEUS_EPOCH_NEW_MOON_JDE) / SYNODIC_MONTH_DAYS).toInt() - 1
        val candidates = ArrayList<MajorPhaseMilestone>(count + 4)

        for (k in kStart..(kStart + (count / 4) + 2)) {
            for (q in 0..3) {
                val phaseDate = fromJulianDay(computeExactPhaseJdUtc(k, q))
                if (phaseDate >= startDate) {
                    val info = getLunarInfo(phaseDate)
                    val phaseType = when (q) {
                        0 -> MoonPhaseType.NEW_MOON
                        1 -> MoonPhaseType.FIRST_QUARTER
                        2 -> MoonPhaseType.FULL_MOON
                        else -> MoonPhaseType.LAST_QUARTER
                    }
                    candidates.add(
                        MajorPhaseMilestone(
                            date = phaseDate,
                            phaseType = phaseType,
                            illuminationPercent = info.illuminationPercent.roundToInt()
                        )
                    )
                }
            }
        }

        return candidates
            .sortedBy { it.date }
            .distinctBy { it.date.toPackedKey() }
            .take(count)
    }

    /**
     * Dynamically computes upcoming Supermoons (Full Moon near Perigee <= 363,500 km) and
     * Solar/Lunar Eclipses (Meeus Ch. 54) starting from [startDate] for ANY year or century.
     */
    fun getUpcomingCelestialEvents(startDate: SimpleDate, count: Int = 6): List<CelestialEvent> {
        val startJd = toJulianDay(startDate.year, startDate.month, startDate.day)
        val kStart = floor((startJd - MEEUS_EPOCH_NEW_MOON_JDE) / SYNODIC_MONTH_DAYS).toInt() - 1
        // Scan ~36 lunations (~3 years ahead) to collect upcoming eclipses & supermoons for any epoch
        val eventsByDate = LinkedHashMap<Int, CelestialEvent>()

        for (k in kStart..(kStart + 36)) {
            // 1. Check Solar Eclipse at New Moon (k)
            val solarEclipse = evaluateSolarEclipseAtLunation(k)
            if (solarEclipse != null && solarEclipse.date >= startDate) {
                val (title, subtitle) = when (solarEclipse.kind) {
                    SolarEclipseKind.TOTAL ->
                        "Eclipse Solar Total" to "Totalidad umbral sobre la superficie terrestre"
                    SolarEclipseKind.ANNULAR ->
                        "Eclipse Solar Anular" to "Anillo solar en apogeo relativo lunar"
                    SolarEclipseKind.HYBRID ->
                        "Eclipse Solar Híbrido" to "Transición anular-total a lo largo de la franja central"
                    SolarEclipseKind.PARTIAL -> {
                        val pct = (solarEclipse.partialMagnitude * 100.0).roundToInt().coerceIn(1, 99)
                        "Eclipse Solar Parcial" to "Ocultación solar máxima del $pct% en penumbra"
                    }
                    SolarEclipseKind.NONE -> "" to ""
                }
                if (title.isNotEmpty()) {
                    eventsByDate[solarEclipse.date.toPackedKey()] = CelestialEvent(
                        date = solarEclipse.date,
                        kind = CelestialEventKind.SOLAR_ECLIPSE,
                        title = title,
                        subtitle = subtitle,
                        visibilityRegion = describeSolarEclipseVisibility(solarEclipse.jdMaxUtc, solarEclipse.gamma),
                        phaseType = MoonPhaseType.NEW_MOON
                    )
                }
            }

            // 2. Check Full Moon (k + 0.5): Lunar Eclipse and/or Perigee Supermoon
            val fullMoonDate = fromJulianDay(computeExactPhaseJdUtc(k, 2))
            if (fullMoonDate >= startDate) {
                val fullInfo = getLunarInfo(fullMoonDate)
                val isSupermoon = fullInfo.distanceKm <= 363500
                val lunarEclipse = evaluateLunarEclipseAtLunation(k)

                if (lunarEclipse != null && lunarEclipse.date >= startDate) {
                    val baseTitle = when (lunarEclipse.kind) {
                        LunarEclipseKind.TOTAL -> "Eclipse Total de Luna"
                        LunarEclipseKind.PARTIAL -> "Eclipse Parcial de Luna"
                        LunarEclipseKind.PENUMBRAL -> "Eclipse Lunar Penumbral"
                        LunarEclipseKind.NONE -> ""
                    }
                    val baseSubtitle = when (lunarEclipse.kind) {
                        LunarEclipseKind.TOTAL ->
                            "Inmersión total en la umbra terrestre (mag. ${"%.2f".format(Locale.US, lunarEclipse.umbralMagnitude)})"
                        LunarEclipseKind.PARTIAL -> {
                            val pct = (lunarEclipse.umbralMagnitude * 100.0).roundToInt().coerceIn(1, 99)
                            "$pct% del diámetro lunar cubierto por la umbra"
                        }
                        LunarEclipseKind.PENUMBRAL -> {
                            val pct = (lunarEclipse.penumbralMagnitude * 100.0).roundToInt().coerceIn(1, 100)
                            "Paso por la penumbra terrestre ($pct% mag. penumbral)"
                        }
                        LunarEclipseKind.NONE -> ""
                    }
                    val mergedSubtitle = if (isSupermoon) {
                        "$baseSubtitle • Superluna (${fullInfo.distanceKm} km)"
                    } else {
                        baseSubtitle
                    }
                    eventsByDate[lunarEclipse.date.toPackedKey()] = CelestialEvent(
                        date = lunarEclipse.date,
                        kind = CelestialEventKind.LUNAR_ECLIPSE,
                        title = baseTitle,
                        subtitle = mergedSubtitle,
                        visibilityRegion = describeLunarEclipseVisibility(lunarEclipse.jdMaxUtc),
                        phaseType = MoonPhaseType.FULL_MOON
                    )
                } else if (isSupermoon) {
                    val packed = fullMoonDate.toPackedKey()
                    if (!eventsByDate.containsKey(packed)) {
                        eventsByDate[packed] = CelestialEvent(
                            date = fullMoonDate,
                            kind = CelestialEventKind.SUPERMOON,
                            title = "Superluna de Perigeo",
                            subtitle = "Distancia geocéntrica: ${fullInfo.distanceKm} km • Ø ${"%.1f".format(Locale.US, fullInfo.angularDiameterArcmin)}′",
                            visibilityRegion = "Visible globalmente durante toda la noche",
                            phaseType = MoonPhaseType.FULL_MOON
                        )
                    }
                }
            }
        }

        return eventsByDate.values
            .sortedBy { it.date }
            .take(count)
    }
}
