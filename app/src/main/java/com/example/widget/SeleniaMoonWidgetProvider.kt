package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.SeleniaRepository
import com.example.domain.LunarAstronomer
import com.example.domain.LunarEclipseKind
import com.example.domain.SolarEclipseKind
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

class SeleniaMoonWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            refreshAllWidgets(context.applicationContext)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val appCtx = context.applicationContext
        val isNorthern = SeleniaRepository.readWidgetNorthernHemisphere(appCtx)
        updateWidgetsBatch(appCtx, appWidgetManager, appWidgetIds, isNorthern)
    }

    companion object {
        private const val WIDGET_BITMAP_SIZE_PX = 160
        private const val WIDGET_STEPS = 32
        private val UNIT_T = FloatArray(WIDGET_STEPS + 1) { i ->
            -1f + 2f * (i.toFloat() / WIDGET_STEPS)
        }
        private val UNIT_HALF_CHORD = FloatArray(WIDGET_STEPS + 1) { i ->
            val t = UNIT_T[i]
            sqrt((1f - t * t).coerceAtLeast(0f))
        }

        fun refreshAllWidgets(
            context: Context,
            isNorthernHemisphereOverride: Boolean? = null
        ) {
            runCatching {
                val appCtx = context.applicationContext
                val isNorthern = isNorthernHemisphereOverride
                    ?: SeleniaRepository.readWidgetNorthernHemisphere(appCtx)
                val manager = AppWidgetManager.getInstance(appCtx)
                val component = ComponentName(appCtx, SeleniaMoonWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    updateWidgetsBatch(appCtx, manager, ids, isNorthern)
                }
            }
        }

        /**
         * Renders the lunar bitmap and ephemeris strings ONCE per batch and shares across all active
         * widget instances, respecting Northern/Southern Hemisphere limb orientation and real eclipses.
         */
        private fun updateWidgetsBatch(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            isNorthernHemisphere: Boolean
        ) {
            val today = LunarAstronomer.today()
            val info = LunarAstronomer.getLunarInfo(today)

            val hemisphereTag = if (isNorthernHemisphere) "HN" else "HS"
            val dateHeader = "${today.formatSpanishLong().replaceFirstChar { it.uppercase() }} • ${info.zodiacSign} ($hemisphereTag)"
            val phaseHeadline = when {
                info.lunarEclipseKind == LunarEclipseKind.TOTAL -> "Eclipse Lunar Total"
                info.lunarEclipseKind == LunarEclipseKind.PARTIAL -> "Eclipse Lunar Parcial"
                info.lunarEclipseKind == LunarEclipseKind.PENUMBRAL -> "Eclipse Lunar Penumbral"
                info.solarEclipseKind == SolarEclipseKind.TOTAL -> "Eclipse Solar Total"
                info.solarEclipseKind == SolarEclipseKind.ANNULAR -> "Eclipse Solar Anular"
                info.solarEclipseKind == SolarEclipseKind.HYBRID -> "Eclipse Solar Híbrido"
                info.solarEclipseKind == SolarEclipseKind.PARTIAL -> "Eclipse Solar Parcial"
                else -> info.phaseType.spanishName
            }
            val illuminationLine = "%.1f%% luz • Edad %.1f d • %d km".format(
                Locale.US,
                info.illuminationPercent,
                info.lunarAgeDays,
                info.distanceKm
            )
            val ephemerisLine = "Ø %.1f' • λ %.1f° • β %+.1f°".format(
                Locale.US,
                info.angularDiameterArcmin,
                info.eclipticLongitudeDeg,
                info.eclipticLatitudeDeg
            )

            val isUmbralLunarEclipse = info.lunarEclipseKind == LunarEclipseKind.TOTAL ||
                info.lunarEclipseKind == LunarEclipseKind.PARTIAL

            val moonBitmap = renderMoonBitmap(
                sizePx = WIDGET_BITMAP_SIZE_PX,
                phaseFraction = info.phaseFraction,
                isNorthernHemisphere = isNorthernHemisphere,
                isUmbralLunarEclipse = isUmbralLunarEclipse
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_selenia_moon)
                views.setTextViewText(R.id.widget_date_text, dateHeader)
                views.setTextViewText(R.id.widget_phase_name, phaseHeadline)
                views.setTextViewText(R.id.widget_illumination_text, illuminationLine)
                views.setTextViewText(R.id.widget_rise_set_text, ephemerisLine)
                views.setImageViewBitmap(R.id.widget_moon_bitmap, moonBitmap)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        internal fun renderMoonBitmap(
            sizePx: Int,
            phaseFraction: Double,
            isNorthernHemisphere: Boolean,
            isUmbralLunarEclipse: Boolean = false
        ): Bitmap {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val cx = sizePx / 2f
            val cy = sizePx / 2f
            val radius = sizePx * 0.40f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val glowInner = if (isUmbralLunarEclipse) {
                Color.argb(95, 216, 67, 21)
            } else {
                Color.argb(80, 243, 230, 200)
            }
            val glowMid = if (isUmbralLunarEclipse) {
                Color.argb(32, 183, 28, 28)
            } else {
                Color.argb(24, 100, 210, 255)
            }

            // Ambient corona glow
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx,
                cy,
                radius * 1.24f,
                intArrayOf(glowInner, glowMid, Color.TRANSPARENT),
                floatArrayOf(0.55f, 0.85f, 1.0f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * 1.24f, paint)

            // Shadowed lunar sphere
            paint.shader = null
            paint.color = Color.rgb(18, 26, 51)
            canvas.drawCircle(cx, cy, radius, paint)

            // Illuminated terminator curve respecting Northern vs Southern Hemisphere orientation
            val isWaxing = phaseFraction <= 0.5
            val litOnRight = if (isNorthernHemisphere) isWaxing else !isWaxing
            val illumination = ((1.0 - cos(phaseFraction * 2.0 * PI)) / 2.0).toFloat()
            if (illumination > 0.02f) {
                val gradientColors = if (isUmbralLunarEclipse) {
                    intArrayOf(
                        Color.rgb(255, 138, 101),
                        Color.rgb(216, 67, 21),
                        Color.rgb(142, 28, 12)
                    )
                } else {
                    intArrayOf(
                        Color.rgb(252, 247, 234),
                        Color.rgb(243, 230, 200),
                        Color.rgb(204, 188, 156)
                    )
                }
                paint.shader = LinearGradient(
                    cx - radius,
                    cy - radius,
                    cx + radius,
                    cy + radius,
                    gradientColors,
                    null,
                    Shader.TileMode.CLAMP
                )
                if (illumination >= 0.98f) {
                    canvas.drawCircle(cx, cy, radius, paint)
                } else {
                    val path = Path()
                    val k = 2f * illumination - 1f
                    for (i in 0..WIDGET_STEPS) {
                        val y = cy + UNIT_T[i] * radius
                        val halfChord = UNIT_HALF_CHORD[i] * radius
                        val xLimb = if (litOnRight) cx + halfChord else cx - halfChord
                        if (i == 0) path.moveTo(xLimb, y) else path.lineTo(xLimb, y)
                    }
                    for (i in WIDGET_STEPS downTo 0) {
                        val y = cy + UNIT_T[i] * radius
                        val halfChord = UNIT_HALF_CHORD[i] * radius
                        val xTerm = if (litOnRight) cx - k * halfChord else cx + k * halfChord
                        path.lineTo(xTerm, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }

            // Subtle maria / crater details (vertically mirrored in Southern Hemisphere)
            val ySign = if (isNorthernHemisphere) 1f else -1f
            val xSign = if (isNorthernHemisphere) 1f else -1f
            paint.shader = null
            paint.color = Color.argb(46, 16, 24, 48)
            canvas.drawCircle(cx - xSign * radius * 0.25f, cy - ySign * radius * 0.18f, radius * 0.20f, paint)
            canvas.drawCircle(cx + xSign * radius * 0.20f, cy + ySign * radius * 0.22f, radius * 0.15f, paint)
            canvas.drawCircle(cx - xSign * radius * 0.08f, cy + ySign * radius * 0.32f, radius * 0.11f, paint)

            // Crisp selenite / umbral rim
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = sizePx * 0.018f
            paint.color = if (isUmbralLunarEclipse) {
                Color.argb(165, 255, 112, 67)
            } else {
                Color.argb(150, 243, 230, 200)
            }
            canvas.drawCircle(cx, cy, radius, paint)

            return bmp
        }
    }
}
