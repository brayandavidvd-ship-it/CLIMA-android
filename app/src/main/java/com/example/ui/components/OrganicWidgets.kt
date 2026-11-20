package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.WeatherSnapshot
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LightShapeViolet
import com.example.ui.theme.OpaqueCardColor
import com.example.ui.theme.PressureTrackDark
import com.example.ui.theme.SunAmber
import com.example.ui.theme.SunriseArcTrack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Section 5: 2-column grid of 7 opaque #14131F animated detail widgets (16dp grid spacing).
 */
@Composable
fun WeatherDetailsGrid(
    snapshot: WeatherSnapshot,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Row 1: 1. Lluvia del día | 2. Viento
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaggeredEntrance(
                index = 0,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                RainTodayWidget(
                    precipitationMm = snapshot.precipitationTodayMm,
                    reduceAnimations = reduceAnimations,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StaggeredEntrance(
                index = 1,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    WindCircleWidget(
                        speedKmh = snapshot.windSpeedKmh,
                        directionDegrees = snapshot.windDirectionDegrees,
                        reduceAnimations = reduceAnimations,
                    )
                }
            }
        }

        // Row 2: 3. Amanecer y atardecer | 4. Índice UV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaggeredEntrance(
                index = 2,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                SunriseSunsetWidget(
                    sunriseIso = snapshot.sunriseIso,
                    sunsetIso = snapshot.sunsetIso,
                    localTimeIso = snapshot.localTimeIso,
                    reduceAnimations = reduceAnimations,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StaggeredEntrance(
                index = 3,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    UvFlowerWidget(
                        uvIndex = snapshot.uvIndexMax,
                        reduceAnimations = reduceAnimations,
                    )
                }
            }
        }

        // Row 3: 5. Visibilidad | 6. Humedad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaggeredEntrance(
                index = 4,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    VisibilityCircleWidget(
                        visibilityKm = snapshot.visibilityKm,
                        reduceAnimations = reduceAnimations,
                    )
                }
            }
            StaggeredEntrance(
                index = 5,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                HumidityWaveWidget(
                    humidityPercent = snapshot.humidityPercent,
                    dewPointC = snapshot.dewPointC,
                    reduceAnimations = reduceAnimations,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Row 4: 7. Presión
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaggeredEntrance(
                index = 6,
                delayStepMs = 100,
                reduceAnimations = reduceAnimations,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PressureGaugeWidget(
                        pressureHpa = snapshot.pressureHpa,
                        reduceAnimations = reduceAnimations,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 1) LLUVIA DEL DÍA: Count-up from 0 (600ms) + 3 #8FB4FF droplets falling in loop staggered by 0.4s.
 */
@Composable
private fun RainTodayWidget(
    precipitationMm: Double,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetMm = precipitationMm.roundToInt().coerceAtLeast(0)
    val countAnim = remember(targetMm) { Animatable(if (reduceAnimations) targetMm.toFloat() else 0f) }

    LaunchedEffect(targetMm, reduceAnimations) {
        if (reduceAnimations) {
            countAnim.snapTo(targetMm.toFloat())
        } else {
            countAnim.snapTo(0f)
            countAnim.animateTo(
                targetValue = targetMm.toFloat(),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            )
        }
    }

    val dropletCycle = if (!reduceAnimations) {
        rememberInfiniteTransition(label = "rain_widget_drops").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rain_drop_phase",
        ).value
    } else {
        0.5f
    }

    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .height(144.dp)
            .testTag("detail_widget_rain"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Top-right 3 staggered droplets (0.4s stagger over 1.2s cycle -> +0.33f each)
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 40.dp, height = 46.dp),
            ) {
                val xFracs = listOf(0.24f, 0.56f, 0.86f)
                xFracs.forEachIndexed { idx, xFrac ->
                    val phase = (dropletCycle + idx * (1f / 3f)) % 1f
                    val y = size.height * (0.10f + 0.78f * phase)
                    val fade = when {
                        phase < 0.20f -> phase / 0.20f
                        phase > 0.75f -> (1f - phase) / 0.25f
                        else -> 1f
                    }.coerceIn(0f, 1f)
                    val cx = size.width * xFrac
                    val r = 3.2.dp.toPx()
                    val dropPath = Path().apply {
                        moveTo(cx, y - r * 1.55f)
                        quadraticTo(cx + r * 1.15f, y + r * 0.25f, cx, y + r * 1.25f)
                        quadraticTo(cx - r * 1.15f, y + r * 0.25f, cx, y - r * 1.55f)
                        close()
                    }
                    drawPath(dropPath, color = AccentBlue.copy(alpha = fade))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.details_rain_today_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "${countAnim.value.roundToInt()} mm",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 2) VIENTO (Circular): Rotates arrow to real wind direction then sways ~14° back & forth (3s cycle).
 */
@Composable
private fun WindCircleWidget(
    speedKmh: Double,
    directionDegrees: Int,
    reduceAnimations: Boolean,
) {
    val targetAngle = directionDegrees.toFloat()
    val entryAngle = remember(targetAngle) {
        Animatable(if (reduceAnimations) targetAngle else 0f)
    }

    LaunchedEffect(targetAngle, reduceAnimations) {
        if (reduceAnimations) {
            entryAngle.snapTo(targetAngle)
        } else {
            entryAngle.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }
    }

    val swayOffset = if (!reduceAnimations) {
        rememberInfiniteTransition(label = "wind_sway").animateFloat(
            initialValue = -7f,
            targetValue = 7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "wind_sway_deg",
        ).value
    } else {
        0f
    }

    OpaqueCard(
        shape = CircleShape,
        modifier = Modifier
            .size(156.dp)
            .testTag("detail_widget_wind"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val angle = entryAngle.value + swayOffset
                rotate(degrees = angle, pivot = center) {
                    val arrow = Path().apply {
                        moveTo(size.width * 0.50f, size.height * 0.08f)
                        lineTo(size.width * 0.84f, size.height * 0.86f)
                        lineTo(size.width * 0.50f, size.height * 0.68f)
                        lineTo(size.width * 0.16f, size.height * 0.86f)
                        close()
                    }
                    drawPath(arrow, color = LightShapeViolet)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${speedKmh.roundToInt()} km/h",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = windDirectionEs(directionDegrees),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

/**
 * 3) AMANECER Y ATARDECER: Dashed arc #4A4770 with #FFB74D sun traveling sunrise -> current over 4s.
 */
@Composable
private fun SunriseSunsetWidget(
    sunriseIso: String,
    sunsetIso: String,
    localTimeIso: String,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetProgress = computeDaylightProgress(sunriseIso, sunsetIso, localTimeIso)
    val sunProgressAnim = remember(targetProgress) {
        Animatable(if (reduceAnimations) targetProgress else 0f)
    }

    LaunchedEffect(targetProgress, reduceAnimations) {
        if (reduceAnimations) {
            sunProgressAnim.snapTo(targetProgress)
        } else {
            sunProgressAnim.snapTo(0f)
            sunProgressAnim.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            )
        }
    }

    val sunriseLabel = formatMexicanClock(sunriseIso)
    val sunsetLabel = formatMexicanClock(sunsetIso)

    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .height(162.dp)
            .testTag("detail_widget_sunrise_sunset"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.details_sunrise_sunset_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                val left = size.width * 0.08f
                val right = size.width * 0.92f
                val horizonY = size.height * 0.86f
                val arcRadiusX = (right - left) / 2f
                val arcRadiusY = size.height * 0.72f
                val centerX = (left + right) / 2f

                drawLine(
                    color = SunriseArcTrack.copy(alpha = 0.7f),
                    start = Offset(0f, horizonY),
                    end = Offset(size.width, horizonY),
                    strokeWidth = 1.dp.toPx(),
                )

                drawArc(
                    color = SunriseArcTrack,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - arcRadiusX, horizonY - arcRadiusY),
                    size = Size(arcRadiusX * 2f, arcRadiusY * 2f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                    ),
                )

                val angle = PI * (1.0 - sunProgressAnim.value.toDouble())
                val sunX = centerX + (cos(angle) * arcRadiusX).toFloat()
                val sunY = horizonY - (sin(angle) * arcRadiusY).toFloat()
                drawCircle(
                    color = SunAmber,
                    radius = 5.5.dp.toPx(),
                    center = Offset(sunX, sunY),
                )
            }

            Text(
                text = "$sunriseLabel · $sunsetLabel",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
            )
        }
    }
}

/**
 * 4) ÍNDICE UV: Wavy flower shape in #14131F rotating 360° in 18s while text stays static + colored dot.
 */
@Composable
private fun UvFlowerWidget(
    uvIndex: Double,
    reduceAnimations: Boolean,
) {
    val uvInt = uvIndex.roundToInt()
    val rotationDeg = if (!reduceAnimations) {
        rememberInfiniteTransition(label = "uv_flower_rot").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "uv_angle",
        ).value
    } else {
        0f
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "uv_press",
    )

    Box(
        modifier = Modifier
            .size(156.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
            .testTag("detail_widget_uv"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDeg },
        ) {
            val path = buildScallopedOrganicPath(
                width = size.width,
                height = size.height,
                lobes = 8,
                amplitudeRatio = 0.085f,
            )
            drawPath(path = path, color = OpaqueCardColor)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.details_uv_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Text(
                text = "$uvInt",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
                color = TextPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(uvDotColor(uvIndex)),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = uvLevelEs(uvIndex),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

/**
 * 5) VISIBILIDAD: Circular #14131F card breathing smoothly 100% -> 107% -> 100% (4s cycle).
 */
@Composable
private fun VisibilityCircleWidget(
    visibilityKm: Double,
    reduceAnimations: Boolean,
) {
    val breathScale = if (!reduceAnimations) {
        rememberInfiniteTransition(label = "visibility_breath").animateFloat(
            initialValue = 1.00f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "visibility_scale",
        ).value
    } else {
        1.0f
    }

    OpaqueCard(
        shape = CircleShape,
        modifier = Modifier
            .size(148.dp)
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
            }
            .testTag("detail_widget_visibility"),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.details_visibility_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${visibilityKm.roundToInt()} km",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                color = TextPrimary,
            )
        }
    }
}

/**
 * 6) HUMEDAD: Liquid #C9C3FF rises over 1.6s to humidity % with continuous 3s horizontal surface wave.
 */
@Composable
private fun HumidityWaveWidget(
    humidityPercent: Int,
    dewPointC: Double,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetFraction = (humidityPercent / 100f).coerceIn(0.12f, 0.88f)
    val levelAnim = remember(targetFraction) {
        Animatable(if (reduceAnimations) targetFraction else 0f)
    }

    LaunchedEffect(targetFraction, reduceAnimations) {
        if (reduceAnimations) {
            levelAnim.snapTo(targetFraction)
        } else {
            levelAnim.snapTo(0f)
            levelAnim.animateTo(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            )
        }
    }

    val wavePhase = if (!reduceAnimations) {
        rememberInfiniteTransition(label = "humidity_wave").animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wave_phase",
        ).value
    } else {
        0f
    }

    val textOverLiquid = if (humidityPercent >= 42) OpaqueCardColor else TextPrimary

    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .height(162.dp)
            .testTag("detail_widget_humidity"),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp)),
        ) {
            val waveTopY = size.height * (1f - levelAnim.value * 0.78f)
            val amp = 5.5.dp.toPx()
            val steps = 36
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, waveTopY + sin(wavePhase) * amp)
                for (i in 1..steps) {
                    val x = size.width * (i.toFloat() / steps)
                    val theta = wavePhase + (i.toFloat() / steps) * (2f * PI.toFloat())
                    val y = waveTopY + sin(theta) * amp
                    lineTo(x, y)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path = path, color = LightShapeViolet)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.details_humidity_title),
                style = MaterialTheme.typography.bodySmall,
                color = if (humidityPercent >= 88) OpaqueCardColor else TextSecondary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$humidityPercent%",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
                    color = textOverLiquid,
                )
                Text(
                    text = stringResource(R.string.details_dew_point_format, dewPointC.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = textOverLiquid.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/**
 * 7) PRESIÓN: Circular gauge filling from 0 to normalized hPa over 2s (#8FB4FF on #2A2848 track).
 */
@Composable
private fun PressureGaugeWidget(
    pressureHpa: Double,
    reduceAnimations: Boolean,
) {
    val targetNorm = ((pressureHpa - 960.0) / (1050.0 - 960.0)).toFloat().coerceIn(0.08f, 1f)
    val gaugeAnim = remember(targetNorm) {
        Animatable(if (reduceAnimations) targetNorm else 0f)
    }

    LaunchedEffect(targetNorm, reduceAnimations) {
        if (reduceAnimations) {
            gaugeAnim.snapTo(targetNorm)
        } else {
            gaugeAnim.snapTo(0f)
            gaugeAnim.animateTo(
                targetValue = targetNorm,
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            )
        }
    }

    OpaqueCard(
        shape = CircleShape,
        modifier = Modifier
            .size(156.dp)
            .testTag("detail_widget_pressure"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 14.dp.toPx()
            val strokePx = 7.dp.toPx()
            val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
            val topLeft = Offset(pad, pad)

            drawArc(
                color = PressureTrackDark,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            drawArc(
                color = AccentBlue,
                startAngle = 135f,
                sweepAngle = 270f * gaugeAnim.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.details_pressure_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Text(
                text = "${pressureHpa.roundToInt()}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.details_pressure_unit),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

private fun buildScallopedOrganicPath(
    width: Float,
    height: Float,
    lobes: Int,
    amplitudeRatio: Float,
): Path {
    val cx = width / 2f
    val cy = height / 2f
    val baseRadius = (minOf(width, height) / 2f) * (1f - amplitudeRatio - 0.02f)
    val amp = (minOf(width, height) / 2f) * amplitudeRatio
    val steps = 180
    val path = Path()
    for (i in 0 until steps) {
        val theta = (i.toDouble() / steps) * 2.0 * PI
        val r = baseRadius + amp * cos(lobes * theta).toFloat()
        val x = cx + (r * cos(theta)).toFloat()
        val y = cy + (r * sin(theta)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun windDirectionEs(degrees: Int): String {
    val normalized = ((degrees % 360) + 360) % 360
    return when {
        normalized < 23 || normalized >= 338 -> "Del norte"
        normalized < 68 -> "Del noreste"
        normalized < 113 -> "Del este"
        normalized < 158 -> "Del sureste"
        normalized < 203 -> "Del sur"
        normalized < 248 -> "Del suroeste"
        normalized < 293 -> "Del oeste"
        else -> "Del noroeste"
    }
}

private fun uvLevelEs(uv: Double): String = when {
    uv < 3.0 -> "Bajo"
    uv < 6.0 -> "Moderado"
    uv < 8.0 -> "Alto"
    uv < 11.0 -> "Muy alto"
    else -> "Extremo"
}

private fun uvDotColor(uv: Double): Color = when {
    uv < 3.0 -> Color(0xFF66BB6A)
    uv < 6.0 -> SunAmber
    uv < 8.0 -> Color(0xFFFF8A65)
    uv < 11.0 -> Color(0xFFEF5350)
    else -> Color(0xFFBA68C8)
}

private fun computeDaylightProgress(
    sunriseIso: String,
    sunsetIso: String,
    localTimeIso: String,
): Float {
    val sunriseMin = clockToMinutes(sunriseIso) ?: (6 * 60 + 48)
    val sunsetMin = clockToMinutes(sunsetIso) ?: (17 * 60 + 52)
    val nowMin = clockToMinutes(localTimeIso) ?: (12 * 60)
    if (sunsetMin <= sunriseMin) return 0.5f
    return ((nowMin - sunriseMin).toFloat() / (sunsetMin - sunriseMin).toFloat()).coerceIn(0f, 1f)
}

private fun clockToMinutes(iso: String): Int? {
    val clock = iso.substringAfter("T", "").take(5)
    val parts = clock.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

private fun formatMexicanClock(iso: String): String {
    val clock = iso.substringAfter("T", "").take(5)
    val parts = clock.split(":")
    if (parts.size != 2) return "--:--"
    val h = parts[0].toIntOrNull() ?: return "--:--"
    val m = parts[1].toIntOrNull() ?: return "--:--"
    return when {
        h == 0 -> "12:%02d a. m.".format(m)
        h < 12 -> "%d:%02d a. m.".format(h, m)
        h == 12 -> "12:%02d p. m.".format(m)
        else -> "%d:%02d p. m.".format(h - 12, m)
    }
}
