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
import com.example.domain.LunarAstronomer
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

class SeleniaMoonWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        updateWidgetsBatch(context.applicationContext, appWidgetManager, appWidgetIds)
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

        fun refreshAllWidgets(context: Context) {
            runCatching {
                val appCtx = context.applicationContext
                val manager = AppWidgetManager.getInstance(appCtx)
                val component = ComponentName(appCtx, SeleniaMoonWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    updateWidgetsBatch(appCtx, manager, ids)
                }
            }
        }

        /**
         * Renders the lunar bitmap and ephemeris strings ONCE per batch and shares across all active
         * widget instances, cutting CPU and Binder IPC bitmap allocations when multiple widgets exist.
         */
        private fun updateWidgetsBatch(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val today = LunarAstronomer.today()
            val info = LunarAstronomer.getLunarInfo(today)

            val dateHeader = "${today.formatSpanishLong().replaceFirstChar { it.uppercase() }} • ${info.zodiacSign}"
            val illuminationLine = "%.1f%% luz • Edad %.1f d • %d km".format(
                Locale.US,
                info.illuminationPercent,
                info.lunarAgeDays,
                info.distanceKm
            )
            val riseSetLine = "Salida ${info.moonriseTime} • Puesta ${info.moonsetTime}"

            val moonBitmap = renderMoonBitmap(
                sizePx = WIDGET_BITMAP_SIZE_PX,
                phaseFraction = info.phaseFraction
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_selenia_moon)
                views.setTextViewText(R.id.widget_date_text, dateHeader)
                views.setTextViewText(R.id.widget_phase_name, info.phaseType.spanishName)
                views.setTextViewText(R.id.widget_illumination_text, illuminationLine)
                views.setTextViewText(R.id.widget_rise_set_text, riseSetLine)
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

        private fun renderMoonBitmap(sizePx: Int, phaseFraction: Double): Bitmap {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val cx = sizePx / 2f
            val cy = sizePx / 2f
            val radius = sizePx * 0.40f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Ambient corona glow
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx,
                cy,
                radius * 1.24f,
                intArrayOf(
                    Color.argb(85, 245, 196, 81),
                    Color.argb(25, 100, 210, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.55f, 0.85f, 1.0f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * 1.24f, paint)

            // Shadowed lunar sphere
            paint.shader = null
            paint.color = Color.rgb(18, 26, 51)
            canvas.drawCircle(cx, cy, radius, paint)

            // Illuminated terminator curve using precomputed chord table
            val isWaxing = phaseFraction <= 0.5
            val illumination = ((1.0 - cos(phaseFraction * 2.0 * PI)) / 2.0).toFloat()
            if (illumination > 0.02f) {
                paint.shader = LinearGradient(
                    cx - radius,
                    cy - radius,
                    cx + radius,
                    cy + radius,
                    intArrayOf(
                        Color.rgb(255, 251, 224),
                        Color.rgb(245, 196, 81),
                        Color.rgb(230, 154, 40)
                    ),
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
                        val xLimb = if (isWaxing) cx + halfChord else cx - halfChord
                        if (i == 0) path.moveTo(xLimb, y) else path.lineTo(xLimb, y)
                    }
                    for (i in WIDGET_STEPS downTo 0) {
                        val y = cy + UNIT_T[i] * radius
                        val halfChord = UNIT_HALF_CHORD[i] * radius
                        val xTerm = if (isWaxing) cx - k * halfChord else cx + k * halfChord
                        path.lineTo(xTerm, y)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }

            // Subtle maria / crater details
            paint.shader = null
            paint.color = Color.argb(46, 16, 24, 48)
            canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.18f, radius * 0.20f, paint)
            canvas.drawCircle(cx + radius * 0.20f, cy + radius * 0.22f, radius * 0.15f, paint)
            canvas.drawCircle(cx - radius * 0.08f, cy + radius * 0.32f, radius * 0.11f, paint)

            // Crisp golden rim
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = sizePx * 0.018f
            paint.color = Color.argb(150, 245, 196, 81)
            canvas.drawCircle(cx, cy, radius, paint)

            return bmp
        }
    }
}
