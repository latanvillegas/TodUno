package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight 24dp Material vector icons replacing `androidx.compose.material:material-icons-extended`,
 * reducing APK size by ~15–28 MB while maintaining crisp vector rendering across all densities.
 */
object SeleniaVectorIcons {

    private fun buildIcon(
        name: String,
        autoMirror: Boolean = false,
        builder: ImageVector.Builder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror
        ).apply(builder).build()
    }

    private inline fun ImageVector.Builder.solidPath(
        fillType: PathFillType = PathFillType.NonZero,
        pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = fillType,
            pathBuilder = pathBuilder
        )
    }


    val AutoAwesome: ImageVector by lazy {
        buildIcon("AutoAwesome") {
            solidPath {
                moveTo(19f, 9f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23f, 5f)
                lineToRelative(-2.75f, -1.25f)
                lineTo(19f, 1f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15f, 5f)
                lineToRelative(2.75f, 1.25f)
                lineTo(19f, 9f)
                close()
                moveTo(11.5f, 9.5f)
                lineTo(9f, 4f)
                lineTo(6.5f, 9.5f)
                lineTo(1f, 12f)
                lineToRelative(5.5f, 2.5f)
                lineTo(9f, 20f)
                lineToRelative(2.5f, -5.5f)
                lineTo(17f, 12f)
                lineToRelative(-5.5f, -2.5f)
                close()
                moveTo(19f, 15f)
                lineToRelative(-1.25f, 2.75f)
                lineTo(15f, 19f)
                lineToRelative(2.75f, 1.25f)
                lineTo(19f, 23f)
                lineToRelative(1.25f, -2.75f)
                lineTo(23f, 19f)
                lineToRelative(-2.75f, -1.25f)
                lineTo(19f, 15f)
                close()
            }
        }
    }


    val AutoFixHigh: ImageVector by lazy {
        buildIcon("AutoFixHigh") {
            solidPath {
                moveTo(7.5f, 5.6f)
                lineTo(10f, 7f)
                lineTo(8.6f, 4.5f)
                lineTo(10f, 2f)
                lineTo(7.5f, 3.4f)
                lineTo(5f, 2f)
                lineToRelative(1.4f, 2.5f)
                lineTo(5f, 7f)
                close()
                moveTo(17.9f, 8.9f)
                lineToRelative(-2.8f, -2.8f)
                curveToRelative(-0.4f, -0.4f, -1f, -0.4f, -1.4f, 0f)
                lineTo(2.9f, 16.9f)
                curveToRelative(-0.4f, 0.4f, -0.4f, 1f, 0f, 1.4f)
                lineToRelative(2.8f, 2.8f)
                curveToRelative(0.4f, 0.4f, 1f, 0.4f, 1.4f, 0f)
                lineTo(17.9f, 10.3f)
                curveToRelative(0.4f, -0.4f, 0.4f, -1f, 0f, -1.4f)
                close()
            }
        }
    }


    val Calculate: ImageVector by lazy {
        buildIcon("Calculate") {
            solidPath {
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(11f, 16.5f)
                horizontalLineTo(6.5f)
                verticalLineTo(15f)
                horizontalLineTo(11f)
                verticalLineToRelative(1.5f)
                close()
                moveTo(11f, 10.5f)
                horizontalLineTo(9.5f)
                verticalLineTo(12f)
                horizontalLineTo(8f)
                verticalLineToRelative(-1.5f)
                horizontalLineTo(6.5f)
                verticalLineTo(9f)
                horizontalLineTo(8f)
                verticalLineTo(7.5f)
                horizontalLineToRelative(1.5f)
                verticalLineTo(9f)
                horizontalLineTo(11f)
                verticalLineToRelative(1.5f)
                close()
                moveTo(17.5f, 16.5f)
                horizontalLineTo(13f)
                verticalLineTo(15f)
                horizontalLineToRelative(4.5f)
                verticalLineToRelative(1.5f)
                close()
                moveTo(17.5f, 13.5f)
                horizontalLineTo(13f)
                verticalLineTo(12f)
                horizontalLineToRelative(4.5f)
                verticalLineToRelative(1.5f)
                close()
                moveTo(17.5f, 10.5f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineToRelative(4.5f)
                verticalLineToRelative(1.5f)
                close()
            }
        }
    }

    val CalendarMonth: ImageVector by lazy {
        buildIcon("CalendarMonth") {
            solidPath {
                moveTo(19f, 4f)
                horizontalLineToRelative(-1f)
                verticalLineTo(2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                verticalLineTo(2f)
                horizontalLineTo(6f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(3f, 20f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 20f)
                horizontalLineTo(5f)
                verticalLineTo(10f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(10f)
                close()
                moveTo(9f, 14f)
                horizontalLineTo(7f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(13f, 14f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(17f, 14f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }

    val Today: ImageVector by lazy { CalendarMonth }

    val DeleteOutline: ImageVector by lazy {
        buildIcon("DeleteOutline") {
            solidPath {
                moveTo(6f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                verticalLineToRelative(12f)
                close()
                moveTo(8f, 9f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(10f)
                horizontalLineTo(8f)
                verticalLineTo(9f)
                close()
                moveTo(15.5f, 4f)
                lineToRelative(-1f, -1f)
                horizontalLineToRelative(-5f)
                lineToRelative(-1f, 1f)
                horizontalLineTo(5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                verticalLineTo(4f)
                horizontalLineToRelative(-3.5f)
                close()
            }
        }
    }

    val NightlightRound: ImageVector by lazy {
        buildIcon("NightlightRound") {
            solidPath {
                moveTo(12.3f, 4.9f)
                curveToRelative(0.4f, -0.2f, 0.6f, -0.7f, 0.5f, -1.1f)
                reflectiveCurveToRelative(-0.6f, -0.8f, -1.1f, -0.8f)
                curveTo(6.8f, 3.1f, 3f, 7.1f, 3f, 12f)
                curveToRelative(0f, 4.9f, 4f, 9f, 9f, 9f)
                curveToRelative(3.8f, 0f, 7.1f, -2.4f, 8.4f, -5.9f)
                curveToRelative(0.2f, -0.4f, 0f, -0.9f, -0.4f, -1.2f)
                curveToRelative(-0.4f, -0.3f, -0.9f, -0.2f, -1.2f, 0.1f)
                curveToRelative(-1f, 0.9f, -2.3f, 1.5f, -3.8f, 1.5f)
                curveToRelative(-3.3f, 0f, -6f, -2.7f, -6f, -6f)
                curveToRelative(0f, -1.9f, 0.9f, -3.6f, 2.3f, -4.6f)
                close()
            }
        }
    }

    val NightsStay: ImageVector by lazy { NightlightRound }

    val WbSunny: ImageVector by lazy {
        buildIcon("WbSunny") {
            solidPath {
                moveTo(6.76f, 4.84f)
                lineToRelative(-1.8f, -1.79f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(1.79f, 1.79f)
                close()
                moveTo(4f, 10.5f)
                horizontalLineTo(1f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(3f)
                close()
                moveTo(13f, 0.55f)
                horizontalLineToRelative(-2f)
                verticalLineTo(3.5f)
                horizontalLineToRelative(2f)
                close()
                moveTo(20.45f, 4.46f)
                lineToRelative(-1.41f, -1.41f)
                lineToRelative(-1.79f, 1.79f)
                lineToRelative(1.41f, 1.41f)
                close()
                moveTo(17.24f, 18.16f)
                lineToRelative(1.79f, 1.8f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(-1.8f, -1.79f)
                close()
                moveTo(20f, 10.5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-2f)
                close()
                moveTo(12f, 5.5f)
                curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
                reflectiveCurveToRelative(2.69f, 6f, 6f, 6f)
                reflectiveCurveToRelative(6f, -2.69f, 6f, -6f)
                reflectiveCurveToRelative(-2.69f, -6f, -6f, -6f)
                close()
                moveTo(11f, 22.45f)
                horizontalLineToRelative(2f)
                verticalLineTo(19.5f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(3.55f, 18.54f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(1.79f, -1.8f)
                lineToRelative(-1.41f, -1.41f)
                close()
            }
        }
    }

    val AccessTime: ImageVector by lazy {
        buildIcon("AccessTime") {
            solidPath {
                moveTo(11.99f, 2f)
                curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
                reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
                reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
                reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
                close()
                moveTo(12.5f, 7f)
                horizontalLineTo(11f)
                verticalLineToRelative(6f)
                lineToRelative(5.25f, 3.15f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.5f, -2.67f)
                close()
            }
        }
    }

    val Timer: ImageVector by lazy {
        buildIcon("Timer") {
            solidPath {
                moveTo(15f, 1f)
                horizontalLineTo(9f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(6f)
                verticalLineTo(1f)
                close()
                moveTo(11f, 14f)
                horizontalLineToRelative(2f)
                verticalLineTo(8f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(6f)
                close()
                moveTo(19.03f, 7.39f)
                lineToRelative(1.42f, -1.42f)
                curveToRelative(-0.43f, -0.51f, -0.9f, -0.99f, -1.41f, -1.41f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(16.07f, 4.74f, 14.12f, 4f, 12f, 4f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                reflectiveCurveToRelative(4.02f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
                curveToRelative(0f, -2.12f, -0.74f, -4.07f, -1.97f, -5.61f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
                reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                close()
            }
        }
    }

    val HourglassBottom: ImageVector by lazy {
        buildIcon("HourglassBottom") {
            solidPath {
                moveTo(18f, 22f)
                lineToRelative(-0.01f, -6f)
                lineTo(14f, 12f)
                lineToRelative(3.99f, -4.01f)
                lineTo(18f, 2f)
                horizontalLineTo(6f)
                verticalLineToRelative(6f)
                lineToRelative(4f, 4f)
                lineToRelative(-4f, 3.99f)
                verticalLineTo(22f)
                horizontalLineToRelative(12f)
                close()
                moveTo(8f, 7.5f)
                verticalLineTo(4f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(3.5f)
                lineToRelative(-4f, 4f)
                lineToRelative(-4f, -4f)
                close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        buildIcon("Pause") {
            solidPath {
                moveTo(6f, 19f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineToRelative(14f)
                close()
                moveTo(14f, 5f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineToRelative(-4f)
                close()
            }
        }
    }

    val Flag: ImageVector by lazy {
        buildIcon("Flag") {
            solidPath {
                moveTo(14.4f, 6f)
                lineTo(14f, 4f)
                horizontalLineTo(5f)
                verticalLineToRelative(17f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-7f)
                horizontalLineToRelative(5.6f)
                lineToRelative(0.4f, 2f)
                horizontalLineToRelative(7f)
                verticalLineTo(6f)
                close()
            }
        }
    }

    val Backspace: ImageVector by lazy {
        buildIcon("Backspace", autoMirror = true) {
            solidPath {
                moveTo(22f, 3f)
                horizontalLineTo(7f)
                curveToRelative(-0.69f, 0f, -1.23f, 0.35f, -1.59f, 0.88f)
                lineTo(0f, 12f)
                lineToRelative(5.41f, 8.11f)
                curveToRelative(0.36f, 0.53f, 0.9f, 0.89f, 1.59f, 0.89f)
                horizontalLineToRelative(15f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 15.59f)
                lineTo(17.59f, 17f)
                lineTo(14f, 13.41f)
                lineTo(10.41f, 17f)
                lineTo(9f, 15.59f)
                lineTo(12.59f, 12f)
                lineTo(9f, 8.41f)
                lineTo(10.41f, 7f)
                lineTo(14f, 10.59f)
                lineTo(17.59f, 7f)
                lineTo(19f, 8.41f)
                lineTo(15.41f, 12f)
                lineTo(19f, 15.59f)
                close()
            }
        }
    }

    val History: ImageVector by lazy {
        buildIcon("History") {
            solidPath {
                moveTo(13f, 3f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                horizontalLineTo(1f)
                lineToRelative(3.89f, 3.89f)
                lineToRelative(0.07f, 0.14f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                curveToRelative(-1.93f, 0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
                reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
                close()
                moveTo(12f, 8f)
                verticalLineToRelative(5f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }
    }

    val Public: ImageVector by lazy {
        buildIcon("Public") {
            solidPath {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(11f, 19.93f)
                curveToRelative(-3.95f, -0.49f, -7f, -3.85f, -7f, -7.93f)
                curveToRelative(0f, -0.62f, 0.08f, -1.21f, 0.21f, -1.79f)
                lineTo(9f, 15f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                verticalLineToRelative(1.93f)
                close()
                moveTo(17.9f, 17.39f)
                curveToRelative(-0.26f, -0.81f, -1f, -1.39f, -1.9f, -1.39f)
                horizontalLineToRelative(-1f)
                verticalLineToRelative(-3f)
                curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                horizontalLineTo(8f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineTo(7f)
                horizontalLineToRelative(2f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-0.41f)
                curveToRelative(2.93f, 1.19f, 5f, 4.06f, 5f, 7.41f)
                curveToRelative(0f, 2.08f, -0.8f, 3.97f, -2.1f, 5.39f)
                close()
            }
        }
    }

    val Spa: ImageVector by lazy {
        buildIcon("Spa") {
            solidPath {
                moveTo(15.49f, 9.63f)
                curveToRelative(-0.18f, -2.79f, -1.31f, -5.51f, -3.43f, -7.63f)
                curveToRelative(-2.14f, 2.14f, -3.32f, 4.86f, -3.55f, 7.63f)
                curveToRelative(1.28f, 0.68f, 2.46f, 1.56f, 3.49f, 2.63f)
                curveToRelative(1.03f, -1.06f, 2.21f, -1.94f, 3.49f, -2.63f)
                close()
                moveTo(12f, 15.45f)
                curveTo(9.85f, 12.17f, 6.18f, 10f, 2f, 10f)
                curveToRelative(0f, 5.32f, 3.36f, 9.82f, 8.03f, 11.49f)
                curveToRelative(0.63f, 0.23f, 1.29f, 0.4f, 1.97f, 0.51f)
                curveToRelative(0.68f, -0.12f, 1.33f, -0.29f, 1.97f, -0.51f)
                curveTo(18.64f, 19.82f, 22f, 15.32f, 22f, 10f)
                curveToRelative(-4.18f, 0f, -7.85f, 2.17f, -10f, 5.45f)
                close()
            }
        }
    }

    val ContentCut: ImageVector by lazy {
        buildIcon("ContentCut") {
            solidPath {
                moveTo(9.64f, 7.64f)
                curveToRelative(0.23f, -0.5f, 0.36f, -1.05f, 0.36f, -1.64f)
                curveToRelative(0f, -2.21f, -1.79f, -4f, -4f, -4f)
                reflectiveCurveTo(2f, 3.79f, 2f, 6f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                curveToRelative(0.59f, 0f, 1.14f, -0.13f, 1.64f, -0.36f)
                lineTo(10f, 12f)
                lineToRelative(-2.36f, 2.36f)
                curveTo(7.14f, 14.13f, 6.59f, 14f, 6f, 14f)
                curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
                curveToRelative(0f, -0.59f, -0.13f, -1.14f, -0.36f, -1.64f)
                lineTo(12f, 14f)
                lineToRelative(7f, 7f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-1f)
                lineTo(9.64f, 7.64f)
                close()
                moveTo(19f, 3f)
                lineToRelative(-6f, 6f)
                lineToRelative(2f, 2f)
                lineToRelative(7f, -7f)
                verticalLineTo(3f)
                horizontalLineToRelative(-3f)
                close()
            }
        }
    }

    val SelfImprovement: ImageVector by lazy {
        buildIcon("SelfImprovement") {
            solidPath {
                moveTo(12f, 6f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                close()
                moveTo(21f, 16f)
                verticalLineToRelative(-2f)
                curveToRelative(-2.24f, 0f, -4.16f, -0.96f, -5.6f, -2.68f)
                lineToRelative(-1.34f, -1.6f)
                curveTo(13.68f, 9.26f, 13.12f, 9f, 12.53f, 9f)
                horizontalLineToRelative(-1.05f)
                curveToRelative(-0.59f, 0f, -1.15f, 0.26f, -1.53f, 0.72f)
                lineToRelative(-1.34f, 1.6f)
                curveTo(7.16f, 13.04f, 5.24f, 14f, 3f, 14f)
                verticalLineToRelative(2f)
                curveToRelative(2.77f, 0f, 5.19f, -1.17f, 7f, -3.25f)
                verticalLineTo(15f)
                lineToRelative(-3.88f, 1.55f)
                curveTo(5.45f, 16.82f, 5f, 17.48f, 5f, 18.21f)
                curveTo(5f, 19.2f, 5.8f, 20f, 6.79f, 20f)
                horizontalLineTo(9f)
                verticalLineToRelative(-0.5f)
                curveToRelative(0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
                horizontalLineToRelative(3f)
                curveToRelative(0.28f, 0f, 0.5f, 0.22f, 0.5f, 0.5f)
                reflectiveCurveToRelative(-0.22f, 0.5f, -0.5f, 0.5f)
                horizontalLineToRelative(-3f)
                curveToRelative(-0.83f, 0f, -1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineTo(20f)
                horizontalLineToRelative(7.21f)
                curveTo(18.2f, 20f, 19f, 19.2f, 19f, 18.21f)
                curveToRelative(0f, -0.73f, -0.45f, -1.39f, -1.12f, -1.66f)
                lineTo(14f, 15f)
                verticalLineToRelative(-2.25f)
                curveTo(15.81f, 14.83f, 18.23f, 16f, 21f, 16f)
                close()
            }
        }
    }

    val Eco: ImageVector by lazy {
        buildIcon("Eco") {
            solidPath {
                moveTo(6.05f, 8.05f)
                curveToRelative(-2.73f, 2.73f, -2.73f, 7.15f, -0.02f, 9.88f)
                curveToRelative(1.47f, -3.4f, 4.09f, -6.24f, 7.36f, -7.93f)
                curveToRelative(-2.77f, 2.34f, -4.71f, 5.61f, -5.39f, 9.32f)
                curveToRelative(2.6f, 1.23f, 5.8f, 0.78f, 7.95f, -1.37f)
                curveTo(19.43f, 14.47f, 20f, 4f, 20f, 4f)
                reflectiveCurveTo(9.53f, 4.57f, 6.05f, 8.05f)
                close()
            }
        }
    }

    val LocalFlorist: ImageVector by lazy { Spa }

    val Park: ImageVector by lazy {
        buildIcon("Park") {
            solidPath {
                moveTo(17f, 12f)
                horizontalLineToRelative(2f)
                lineTo(12f, 2f)
                lineTo(5.05f, 12f)
                horizontalLineTo(7f)
                lineToRelative(-3.9f, 6f)
                horizontalLineToRelative(6.92f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(3.96f)
                verticalLineToRelative(-4f)
                horizontalLineTo(21f)
                close()
            }
        }
    }

    val TravelExplore: ImageVector by lazy { Public }

    val Waves: ImageVector by lazy {
        buildIcon("Waves") {
            solidPath {
                moveTo(17f, 16.99f)
                curveToRelative(-1.35f, 0f, -2.2f, -0.42f, -2.95f, -0.8f)
                curveToRelative(-0.65f, -0.33f, -1.18f, -0.6f, -2.05f, -0.6f)
                curveToRelative(-0.9f, 0f, -1.4f, 0.25f, -2.05f, 0.6f)
                curveToRelative(-0.75f, 0.38f, -1.6f, 0.8f, -2.95f, 0.8f)
                reflectiveCurveToRelative(-2.2f, -0.42f, -2.95f, -0.8f)
                curveToRelative(-0.65f, -0.33f, -1.17f, -0.6f, -2.05f, -0.6f)
                verticalLineToRelative(1.95f)
                curveToRelative(1.35f, 0f, 2.2f, 0.42f, 2.95f, 0.8f)
                curveToRelative(0.65f, 0.33f, 1.17f, 0.6f, 2.05f, 0.6f)
                reflectiveCurveToRelative(1.4f, -0.25f, 2.05f, -0.6f)
                curveToRelative(0.75f, -0.38f, 1.6f, -0.8f, 2.95f, -0.8f)
                reflectiveCurveToRelative(2.2f, 0.42f, 2.95f, 0.8f)
                curveToRelative(0.65f, 0.33f, 1.18f, 0.6f, 2.05f, 0.6f)
                curveToRelative(0.9f, 0f, 1.4f, -0.25f, 2.05f, -0.6f)
                curveToRelative(0.75f, -0.38f, 1.6f, -0.8f, 2.95f, -0.8f)
                verticalLineToRelative(-1.95f)
                curveToRelative(-0.9f, 0f, -1.4f, 0.25f, -2.05f, 0.6f)
                curveToRelative(-0.75f, 0.38f, -1.6f, 0.8f, -2.95f, 0.8f)
                close()
                moveTo(17f, 10.99f)
                curveToRelative(-1.35f, 0f, -2.2f, -0.42f, -2.95f, -0.8f)
                curveToRelative(-0.65f, -0.33f, -1.18f, -0.6f, -2.05f, -0.6f)
                curveToRelative(-0.9f, 0f, -1.4f, 0.25f, -2.05f, 0.6f)
                curveToRelative(-0.75f, 0.38f, -1.6f, 0.8f, -2.95f, 0.8f)
                reflectiveCurveToRelative(-2.2f, -0.42f, -2.95f, -0.8f)
                curveToRelative(-0.65f, -0.33f, -1.17f, -0.6f, -2.05f, -0.6f)
                verticalLineToRelative(1.95f)
                curveToRelative(1.35f, 0f, 2.2f, 0.42f, 2.95f, 0.8f)
                curveToRelative(0.65f, 0.33f, 1.17f, 0.6f, 2.05f, 0.6f)
                reflectiveCurveToRelative(1.4f, -0.25f, 2.05f, -0.6f)
                curveToRelative(0.75f, -0.38f, 1.6f, -0.8f, 2.95f, -0.8f)
                reflectiveCurveToRelative(2.2f, 0.42f, 2.95f, 0.8f)
                curveToRelative(0.65f, 0.33f, 1.18f, 0.6f, 2.05f, 0.6f)
                curveToRelative(0.9f, 0f, 1.4f, -0.25f, 2.05f, -0.6f)
                curveToRelative(0.75f, -0.38f, 1.6f, -0.8f, 2.95f, -0.8f)
                verticalLineToRelative(-1.95f)
                curveToRelative(-0.9f, 0f, -1.4f, 0.25f, -2.05f, 0.6f)
                curveToRelative(-0.75f, 0.38f, -1.6f, 0.8f, -2.95f, 0.8f)
                close()
            }
        }
    }

    val WaterDrop: ImageVector by lazy {
        buildIcon("WaterDrop") {
            solidPath {
                moveTo(12f, 2f)
                curveToRelative(-5.33f, 4.55f, -8f, 8.48f, -8f, 11.8f)
                curveToRelative(0f, 4.98f, 3.8f, 8.2f, 8f, 8.2f)
                reflectiveCurveToRelative(8f, -3.22f, 8f, -8.2f)
                curveToRelative(0f, -3.32f, -2.67f, -7.25f, -8f, -11.8f)
                close()
            }
        }
    }

    val Phishing: ImageVector by lazy {
        buildIcon("Phishing") {
            solidPath {
                moveTo(19f, 9f)
                curveToRelative(0f, -1.3f, -0.84f, -2.4f, -2f, -2.82f)
                verticalLineTo(2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(4.18f)
                curveTo(13.84f, 6.6f, 13f, 7.7f, 13f, 9f)
                reflectiveCurveToRelative(0.84f, 2.4f, 2f, 2.82f)
                verticalLineToRelative(3.01f)
                curveToRelative(0f, 2.09f, -1.52f, 3.96f, -3.6f, 4.15f)
                curveTo(9.02f, 19.21f, 7f, 17.34f, 7f, 15f)
                verticalLineToRelative(-1f)
                horizontalLineToRelative(3f)
                lineTo(5f, 9f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 3.87f, 3.13f, 7f, 7f, 7f)
                reflectiveCurveToRelative(7f, -3.13f, 7f, -7f)
                verticalLineToRelative(-3.18f)
                curveTo(20.16f, 11.4f, 21f, 10.3f, 21f, 9f)
                horizontalLineToRelative(-2f)
                close()
            }
        }
    }

    val Handyman: ImageVector by lazy {
        buildIcon("Handyman") {
            solidPath {
                moveTo(21.67f, 18.17f)
                lineToRelative(-5.3f, -5.3f)
                horizontalLineToRelative(-0.99f)
                lineToRelative(-2.54f, 2.54f)
                verticalLineToRelative(0.99f)
                lineToRelative(5.3f, 5.3f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
                lineToRelative(2.12f, -2.12f)
                curveToRelative(0.39f, -0.38f, 0.39f, -1.02f, 0f, -1.41f)
                close()
                moveTo(17.34f, 10.19f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(2.12f, 2.12f)
                curveToRelative(1.17f, -1.17f, 1.17f, -3.07f, 0f, -4.24f)
                lineToRelative(-3.54f, -3.54f)
                lineToRelative(-1.41f, 1.41f)
                verticalLineTo(1.71f)
                lineTo(15.22f, 1f)
                lineToRelative(-3.54f, 3.54f)
                lineToRelative(0.71f, 0.71f)
                horizontalLineToRelative(2.83f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(1.06f, 1.06f)
                lineToRelative(-2.89f, 2.89f)
                lineToRelative(-4.13f, -4.13f)
                verticalLineTo(5.06f)
                lineTo(4.83f, 2.04f)
                lineTo(2f, 4.87f)
                lineToRelative(3.03f, 3.03f)
                horizontalLineToRelative(1.41f)
                lineToRelative(4.13f, 4.13f)
                lineToRelative(-0.85f, 0.85f)
                horizontalLineTo(7.6f)
                lineToRelative(-5.3f, 5.3f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
                lineToRelative(2.12f, 2.12f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
                lineToRelative(5.3f, -5.3f)
                verticalLineToRelative(-2.12f)
                lineToRelative(5.15f, -5.15f)
                lineToRelative(1.06f, 1.05f)
                close()
            }
        }
    }
}
