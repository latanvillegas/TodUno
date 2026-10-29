package com.example.domain

import androidx.compose.runtime.Immutable
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

enum class LunarAlmanacCategory(
    val spanishTitle: String,
    val shortChipLabel: String,
    val emoji: String
) {
    ALL("Todas las Guías", "Todas", "✨"),
    BEAUTY_SKIN("Belleza y Cuidado de la Piel", "Belleza y Piel", "💆‍♀️"),
    HAIR_CARE("Cabello, Corte y Depilación", "Cabello y Corte", "💇‍♀️"),
    WELLNESS("Bienestar, Cuerpo y Energía", "Bienestar", "🧘‍♀️"),
    GARDENING("Agricultura y Jardín", "Agricultura", "🌱"),
    ASTRONOMY("Observación Astronómica", "Astronomía", "🔭"),
    TIDES("Dinámica de Mareas", "Mareas", "🌊"),
    FISHING("Pesca y Ciclo Solunar", "Pesca", "🎣"),
    HOME_CRAFTS("Hogar, Madera y Conservas", "Hogar y Madera", "🏡")
}

@Immutable
data class LunarAlmanacGuideItem(
    val category: LunarAlmanacCategory,
    val badgeTitle: String,
    val actionHighlight: String,
    val description: String,
    val scoreOutOfFive: Int
)

enum class LunarTraditionalGoal(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val idealPhases: Set<MoonPhaseType>
) {
    FAST_HAIR_GROWTH(
        title = "Corte para Crecimiento Rápido",
        emoji = "✂️",
        subtitle = "Estimula el folículo para que el cabello crezca con mayor rapidez.",
        idealPhases = setOf(MoonPhaseType.WAXING_CRESCENT, MoonPhaseType.FIRST_QUARTER)
    ),
    STRONG_THICK_HAIR(
        title = "Corte para Volumen y Raíz Fuerte",
        emoji = "🦁",
        subtitle = "Ideal para cabellos finos o quebradizos; aporta densidad y brillo.",
        idealPhases = setOf(MoonPhaseType.WAXING_GIBBOUS, MoonPhaseType.FULL_MOON)
    ),
    LONG_LASTING_DEPILATION(
        title = "Depilación Duradera y Lenta",
        emoji = "🪒",
        subtitle = "El vello tarda más en reaparecer y se debilita progresivamente.",
        idealPhases = setOf(MoonPhaseType.LAST_QUARTER, MoonPhaseType.WANING_CRESCENT)
    ),
    SKIN_NUTRITION_GLOW(
        title = "Mascarillas Nutritivas y Colágeno",
        emoji = "💆‍♀️",
        subtitle = "Máxima absorción de sérums, vitaminas, aceites e hidratación profunda.",
        idealPhases = setOf(MoonPhaseType.FIRST_QUARTER, MoonPhaseType.WAXING_GIBBOUS)
    ),
    SKIN_DETOX_EXFOLIATION(
        title = "Limpieza Facial, Peeling y Detox",
        emoji = "🧖‍♀️",
        subtitle = "Eliminación de impurezas, puntos negros, toxinas y retención de líquidos.",
        idealPhases = setOf(MoonPhaseType.WANING_GIBBOUS, MoonPhaseType.LAST_QUARTER, MoonPhaseType.NEW_MOON)
    ),
    PLANTING_FRUITS_FLOWERS(
        title = "Siembra de Flores, Hojas y Frutos",
        emoji = "🌱",
        subtitle = "Impulso ascendente de la savia para germinación y desarrollo aéreo.",
        idealPhases = setOf(MoonPhaseType.WAXING_CRESCENT, MoonPhaseType.FIRST_QUARTER)
    ),
    PRUNING_AND_TIMBER(
        title = "Poda, Tubérculos y Corte de Madera",
        emoji = "🪵",
        subtitle = "Savia en las raíces; cicatrización limpia y maderas resistentes a insectos.",
        idealPhases = setOf(MoonPhaseType.LAST_QUARTER, MoonPhaseType.WANING_CRESCENT)
    )
}

enum class MoonPhaseType(
    val spanishName: String,
    val shortLabel: String,
    val emoji: String,
    val isMajorPhase: Boolean,
    val beautySkinTip: String,
    val beautyActionBadge: String,
    val hairCareTip: String,
    val hairActionBadge: String,
    val wellnessTip: String,
    val wellnessActionBadge: String,
    val gardeningTip: String,
    val stargazingTip: String,
    val tideEffect: String,
    val fishingFaunaTip: String,
    val homeTimberTip: String
) {
    NEW_MOON(
        spanishName = "Luna Nueva",
        shortLabel = "Nueva",
        emoji = "🌑",
        isMajorPhase = true,
        beautySkinTip = "Fase de renovación celular y desintoxicación profunda. Ideal para baños de vapor, arcillas purificantes, descanso de maquillaje y regeneración de la barrera cutánea.",
        beautyActionBadge = "Detox y Renovación Cutánea",
        hairCareTip = "Evita cortes radicales o químicos agresivos hoy; el cuero cabelludo está en reposo. Excelente día para masajes capilares relajantes y aceites reparadores en puntas.",
        hairActionBadge = "Reposo Capilar y Reparación",
        wellnessTip = "Ciclo de introspección, ayuno ligero o infusiones depurativas. Prioriza el sueño reparador, la meditación y establecer intenciones para el nuevo ciclo lunar.",
        wellnessActionBadge = "Introspección y Detox",
        gardeningTip = "Ideal para reposo del suelo, poda de limpieza, escarda y control natural de malezas.",
        stargazingTip = "Cielo oscuro óptimo (0% brillo lunar) para observar galaxias, nebulosas, cúmulos y la Vía Láctea.",
        tideEffect = "Mareas vivas (sicigia): alineación Sol-Luna-Tierra con amplitud máxima de pleamar y bajamar.",
        fishingFaunaTip = "Alta actividad solunar: excelente picada durante el tránsito lunar superior e inferior.",
        homeTimberTip = "Óptimo para limpieza profunda del hogar, desechar objetos en desuso y airear habitaciones."
    ),
    WAXING_CRESCENT(
        spanishName = "Luna Creciente",
        shortLabel = "Creciente",
        emoji = "🌒",
        isMajorPhase = false,
        beautySkinTip = "La piel inicia su fase receptiva. Aplica cremas hidratantes, ácido hialurónico, vitamina C y contornos de ojos para revitalizar el rostro con rápida absorción.",
        beautyActionBadge = "Hidratación y Luminosidad",
        hairCareTip = "¡Momento estrella para cortar las puntas si deseas que el cabello crezca rápido y largo! También favorece la aplicación de tónicos estimulantes del folículo.",
        hairActionBadge = "✂️ Corte: Crecimiento Rápido",
        wellnessTip = "Energía física en ascenso: ideal para iniciar nuevas rutinas de ejercicio, aumentar el consumo de proteínas/minerales y retomar hábitos saludables.",
        wellnessActionBadge = "Vitalidad en Ascenso",
        gardeningTip = "La savia comienza a subir: excelente para sembrar hortalizas de hoja, hierbas aromáticas y flores.",
        stargazingTip = "Luz cenicienta (brillo terrestre) visible sobre el limbo oscuro al atardecer occidental.",
        tideEffect = "Mareas en transición descendente tras la sicigia de Luna Nueva.",
        fishingFaunaTip = "Buena actividad de peces en aguas superficiales desde el atardecer hasta la puesta lunar.",
        homeTimberTip = "Propicio para iniciar fermentados suaves, pan de masa madre y organización de despensa."
    ),
    FIRST_QUARTER(
        spanishName = "Cuarto Creciente",
        shortLabel = "C. Creciente",
        emoji = "🌓",
        isMajorPhase = true,
        beautySkinTip = "Alta capacidad de asimilación epidérmica. Momento ideal para mascarillas nutritivas, reafirmantes, colágeno, masajes faciales lifting y cuidado fortalecedor de uñas.",
        beautyActionBadge = "Nutrición y Firmeza Facial",
        hairCareTip = "Excelente para cortar el cabello buscando crecimiento acelerado y vitalidad, así como para aplicar tintes vegetales, baños de color o tratamientos de keratina.",
        hairActionBadge = "✂️ Crecimiento y Coloración",
        wellnessTip = "Pico de motivación y resistencia muscular. Aprovecha para entrenamientos de fuerza, actividades al aire libre y proyectos que requieran concentración activa.",
        wellnessActionBadge = "Fuerza y Acción",
        gardeningTip = "Favorece plantas de fruto (tomate, pimiento, fresa), cereales y trasplantes de crecimiento vigoroso.",
        stargazingTip = "El terminador al 50% muestra cráteres y cordilleras lunares con máximo relieve tridimensional.",
        tideEffect = "Mareas muertas (cuadratura): menor diferencia entre pleamar y bajamar; aguas más estables.",
        fishingFaunaTip = "Actividad moderada; mejores capturas al amanecer y cuando la Luna cruza el meridiano.",
        homeTimberTip = "Buen momento para elaborar conservas dulces, mermeladas y trasvasar líquidos claros."
    ),
    WAXING_GIBBOUS(
        spanishName = "Gibosa Creciente",
        shortLabel = "Gibosa Crec.",
        emoji = "🌔",
        isMajorPhase = false,
        beautySkinTip = "Máxima retención de hidratación y principios activos. Aplica ampollas regeneradoras nocturnas, aceites faciales antioxidantes y tratamientos intensivos para labios y manos.",
        beautyActionBadge = "Regeneración Intensiva",
        hairCareTip = "Ideal para cortar el pelo si buscas ganar cuerpo, densidad y fortalecer la raíz. Los tintes y mechas aplicados hoy fijan el pigmento con gran intensidad y brillo.",
        hairActionBadge = "🦁 Volumen, Brillo y Tinte",
        wellnessTip = "El organismo absorbe todo con mayor eficacia: prioriza alimentos frescos ricos en antioxidantes y evita excesos de sal o azúcares refinados.",
        wellnessActionBadge = "Máxima Absorción Nutricional",
        gardeningTip = "Máxima absorción de nutrientes y agua; ideal para injertos, esquejes y abonos foliares.",
        stargazingTip = "Excelente visibilidad de los grandes mares lunares, Sinus Iridum y el cráter Copérnico.",
        tideEffect = "Mareas en aumento progresivo hacia el plenilunio.",
        fishingFaunaTip = "Incremento notable del movimiento de cardúmenes en las horas previas a la Luna Llena.",
        homeTimberTip = "Evita cortar madera para construcción hoy (mayor contenido de savia y humedad interna)."
    ),
    FULL_MOON(
        spanishName = "Luna Llena",
        shortLabel = "Llena",
        emoji = "🌕",
        isMajorPhase = true,
        beautySkinTip = "Plenitud circulatoria y luminosidad cutánea. Ideal para rituales de spa, mascarillas iluminadoras de efecto inmediato y masajes linfáticos suaves (evita extracciones agresivas).",
        beautyActionBadge = "Glow, Spa y Masaje Linfático",
        hairCareTip = "Tradición clásica para cortar cabellos débiles, finos o sin vida: estimula un crecimiento abundante, grueso y resistente. Ideal para mascarillas de nutrición profunda.",
        hairActionBadge = "🦁 Corte: Abundancia y Fuerza",
        wellnessTip = "Sensibilidad y energía emocional al máximo. Practica respiración consciente, yoga suave o baños tibios antes de dormir, ya que el sueño puede ser más ligero.",
        wellnessActionBadge = "Plenitud y Calma Nocturna",
        gardeningTip = "Savia concentrada en la copa y frutos: momento óptimo para cosechar frutos jugosos y plantas medicinales.",
        stargazingTip = "Disco 100% iluminado; destacan los sistemas de rayos brillantes de Tycho, Kepler y Aristarco.",
        tideEffect = "Mareas vivas (sicigia): fuerte atracción gravitatoria combinada Sol-Luna con oleaje intenso.",
        fishingFaunaTip = "Pico solunar nocturno: intensa actividad de especies depredadoras bajo la luz del plenilunio.",
        homeTimberTip = "Excelente para recolectar hierbas aromáticas para secado y preparar macerados naturales."
    ),
    WANING_GIBBOUS(
        spanishName = "Gibosa Menguante",
        shortLabel = "Gibosa Meng.",
        emoji = "🌖",
        isMajorPhase = false,
        beautySkinTip = "Comienza la fase depurativa de la piel. Excelente para exfoliación suave, limpieza de poros, tratamientos anti-acné, drenaje de bolsas/ojeras y reducción de celulitis.",
        beautyActionBadge = "Exfoliación y Drenaje",
        hairCareTip = "Si tienes el cabello muy corto o con corte estructurado y quieres que mantenga su forma por más tiempo sin perder el diseño, corta hoy. Buen inicio para depilación.",
        hairActionBadge = "Mantener Forma del Corte",
        wellnessTip = "El cuerpo elimina líquidos y toxinas con facilidad. Muy favorable para saunas, masajes descontracturantes, infusiones drenantes y estiramientos profundos.",
        wellnessActionBadge = "Liberación y Drenaje",
        gardeningTip = "La savia empieza a descender hacia las raíces; propicio para sembrar tubérculos (papa, zanahoria, rábano).",
        stargazingTip = "Salida de la Luna tras el anochecer con excelente contraste en el limbo oriental (Mar de las Crisis).",
        tideEffect = "Mareas moderadas-altas disminuyendo gradualmente tras el plenilunio.",
        fishingFaunaTip = "Muy buena pesca en las primeras horas de la mañana mientras la Luna aún es visible.",
        homeTimberTip = "Favorable para lavar textiles pesados, quitar manchas difíciles y ventilar colchones."
    ),
    LAST_QUARTER(
        spanishName = "Cuarto Menguante",
        shortLabel = "C. Menguante",
        emoji = "🌗",
        isMajorPhase = true,
        beautySkinTip = "El mejor momento del mes para limpieza facial profunda, extracción de comedones, peelings renovadores, mascarillas de carbón/arcilla y tratamientos purificantes.",
        beautyActionBadge = "🧖‍♀️ Limpieza Profunda y Peeling",
        hairCareTip = "¡Fase óptima para la depilación! El vello tarda mucho más en crecer y nace más fino. Cortar el cabello hoy fortalece la raíz reduciendo la caída (crecimiento lento).",
        hairActionBadge = "🪒 Depilación Duradera y Anticaída",
        wellnessTip = "Ideal para cerrar ciclos, reducir el estrés acumulado, cuidar articulaciones y realizar rutinas de cardio moderado o caminatas en la naturaleza.",
        wellnessActionBadge = "Equilibrio y Depuración",
        gardeningTip = "Ideal para podas de formación, trasplante de raíces, bulbos, abonado orgánico y compostaje.",
        stargazingTip = "Visible en la madrugada y mañana; relieve inverso espectacular en los Montes Apeninos.",
        tideEffect = "Mareas muertas (cuadratura): corrientes marinas más suaves y menor amplitud de marea.",
        fishingFaunaTip = "Aguas tranquilas con corrientes suaves; ideal para pesca de fondo y especies de roca.",
        homeTimberTip = "🪵 Tradición maderera: mejor fase para talar o cortar madera, guadua y bambú (secado firme sin polilla)."
    ),
    WANING_CRESCENT(
        spanishName = "Luna Menguante",
        shortLabel = "Menguante",
        emoji = "🌘",
        isMajorPhase = false,
        beautySkinTip = "Fase balsámica calmante. Ideal para pieles sensibles o reactivas, aguas termales, aloe vera, caléndula, pedicura terapéutica y eliminación de callosidades.",
        beautyActionBadge = "Calma Cutánea y Pedicura",
        hairCareTip = "Segundo momento clave para depilación de larga duración y diseño de cejas. En cabello, aplica lavados detox de cuero cabelludo para eliminar residuos.",
        hairActionBadge = "🪒 Depilación y Detox Capilar",
        wellnessTip = "Disminuye el ritmo físico intenso; favorece el descanso profundo, baños de sales de Epsom, lectura tranquila y preparación mental para el novilunio.",
        wellnessActionBadge = "Descanso y Serenidad",
        gardeningTip = "Fase balsámica de descanso vegetal, deshierbe duradero, arado y preparación de sustratos.",
        stargazingTip = "Fino creciente matutino antes del alba; cielos nocturnos despejados de brillo lunar.",
        tideEffect = "Mareas aumentando progresivamente hacia el nuevo ciclo sinódico.",
        fishingFaunaTip = "Excelente actividad solunar al amanecer coincidiendo con la salida del fino creciente.",
        homeTimberTip = "Ideal para embotellar vinos/vinagres decantados, curar maderas y ordenar espacios de trabajo."
    );

    private val cachedGuides: List<LunarAlmanacGuideItem> by lazy {
        listOf(
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.BEAUTY_SKIN,
                badgeTitle = "💆‍♀️ Belleza y Cuidado de la Piel",
                actionHighlight = beautyActionBadge,
                description = beautySkinTip,
                scoreOutOfFive = when (this) {
                    FIRST_QUARTER, WAXING_GIBBOUS, FULL_MOON, LAST_QUARTER -> 5
                    else -> 4
                }
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.HAIR_CARE,
                badgeTitle = "💇‍♀️ Cabello, Corte y Depilación",
                actionHighlight = hairActionBadge,
                description = hairCareTip,
                scoreOutOfFive = when (this) {
                    WAXING_CRESCENT, FIRST_QUARTER, FULL_MOON, LAST_QUARTER -> 5
                    WAXING_GIBBOUS, WANING_CRESCENT -> 4
                    else -> 3
                }
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.WELLNESS,
                badgeTitle = "🧘‍♀️ Bienestar, Cuerpo y Energía",
                actionHighlight = wellnessActionBadge,
                description = wellnessTip,
                scoreOutOfFive = 5
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.GARDENING,
                badgeTitle = "🌱 Agricultura y Jardín",
                actionHighlight = if (this in setOf(WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS, FULL_MOON)) {
                    "Savia Ascendente / Copa"
                } else {
                    "Savia Descendente / Raíz"
                },
                description = gardeningTip,
                scoreOutOfFive = 5
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.ASTRONOMY,
                badgeTitle = "🔭 Observación Astronómica",
                actionHighlight = if (this == NEW_MOON) "Cielo Profundo Óptimo" else "Relieve y Mares Lunares",
                description = stargazingTip,
                scoreOutOfFive = if (this in setOf(NEW_MOON, FIRST_QUARTER, LAST_QUARTER)) 5 else 4
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.TIDES,
                badgeTitle = "🌊 Dinámica de Mareas",
                actionHighlight = if (this in setOf(NEW_MOON, FULL_MOON)) "Mareas Vivas (Sicigia)" else "Mareas Moderadas / Muertas",
                description = tideEffect,
                scoreOutOfFive = if (this in setOf(NEW_MOON, FULL_MOON)) 5 else 4
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.FISHING,
                badgeTitle = "🎣 Pesca y Ciclo Solunar",
                actionHighlight = if (this in setOf(NEW_MOON, FULL_MOON)) "Actividad Solunar Máxima" else "Actividad Solunar Media-Alta",
                description = fishingFaunaTip,
                scoreOutOfFive = if (this in setOf(NEW_MOON, FULL_MOON, WAXING_GIBBOUS)) 5 else 4
            ),
            LunarAlmanacGuideItem(
                category = LunarAlmanacCategory.HOME_CRAFTS,
                badgeTitle = "🏡 Hogar, Madera y Conservas",
                actionHighlight = if (this == LAST_QUARTER) "Corte Óptimo de Madera" else "Tradición Doméstica",
                description = homeTimberTip,
                scoreOutOfFive = if (this in setOf(LAST_QUARTER, WANING_CRESCENT, FIRST_QUARTER)) 5 else 4
            )
        )
    }

    fun almanacGuides(): List<LunarAlmanacGuideItem> = cachedGuides
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

    val storageKey: String = "%04d-%02d-%02d".format(year, month, day)

    fun toPackedKey(): Int = (year shl 9) or (month shl 5) or day

    fun toStorageKey(): String = storageKey

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

@Immutable
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

    // Bounded LRU cache (256 dates) to avoid recomputing orbital math for repeated dates
    private val lunarInfoCache = object : LinkedHashMap<Int, LunarDayInfo>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, LunarDayInfo>?): Boolean {
            return size > 256
        }
    }

    private val WEEKDAY_NAMES_FROM_MONDAY = arrayOf(
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    )

    fun getDayOfWeekSpanish(year: Int, month: Int, day: Int): String {
        // Pure Julian Day arithmetic: avoids allocating a Calendar object per date format call.
        val jdNoon = toJulianDay(year, month, day)
        val idx = ((floor(jdNoon + 0.5).toLong() % 7L) + 7L).toInt() % 7
        return WEEKDAY_NAMES_FROM_MONDAY[idx]
    }

    private fun rawPhaseFractionForJd(jd: Double): Double {
        val cycles = (jd - REFERENCE_NEW_MOON_JD) / SYNODIC_MONTH_DAYS
        var frac = cycles - floor(cycles)
        if (frac < 0) frac += 1.0
        return frac
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

    /**
     * Finds the next [count] upcoming dates (within 30 days from [startDate]) that match the ideal lunar phases
     * for a traditional goal (e.g., fast hair growth, thick hair, long-lasting depilation, skin nutrition, etc.).
     */
    fun getUpcomingIdealDatesForGoal(
        startDate: SimpleDate,
        goal: LunarTraditionalGoal,
        count: Int = 4
    ): List<LunarDayInfo> {
        val matches = mutableListOf<LunarDayInfo>()
        var lastAddedOffset = -10
        for (offset in 0..32) {
            if (matches.size >= count) break
            val candidate = addDays(startDate, offset)
            val info = getLunarInfo(candidate)
            if (info.phaseType in goal.idealPhases) {
                // Space out consecutive days slightly or prefer major/distinct days so the user gets a useful spread
                if (offset == 0 || info.isExactMajorPhaseDay || (offset - lastAddedOffset) >= 2) {
                    matches.add(info)
                    lastAddedOffset = offset
                }
            }
        }
        return matches
    }
}

