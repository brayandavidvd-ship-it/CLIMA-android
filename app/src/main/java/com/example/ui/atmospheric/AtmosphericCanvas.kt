package com.example.ui.atmospheric

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.WeatherVisualState
import com.example.ui.theme.RainDarkCloud
import com.example.ui.theme.SkyClearNight
import com.example.ui.theme.SkyCloudy
import com.example.ui.theme.SkyDrizzle
import com.example.ui.theme.SkyFog
import com.example.ui.theme.SkyRain
import com.example.ui.theme.SkySnow
import com.example.ui.theme.SkyStorm
import com.example.ui.theme.SkySunny
import com.example.ui.theme.StormCloud
import com.example.ui.theme.SunYellow
import kotlin.math.PI
import kotlin.math.sin

private data class StarSpec(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val cycleSeconds: Float,
    val phase: Float,
)

private data class ParticleSpec(
    val xFraction: Float,
    val phase: Float,
    val durationSeconds: Float,
    val lengthFactor: Float,
    val alpha: Float,
)

private val STARS_24: List<StarSpec> = List(24) { i ->
    StarSpec(
        xFraction = ((i * 37 + 11) % 96 + 2) / 100f,
        yFraction = ((i * 23 + 7) % 52 + 4) / 100f,
        radiusDp = 1.2f + (i % 3) * 0.65f,
        cycleSeconds = 2.0f + (i % 7) * 0.45f,
        phase = (i * 0.47f) % (2f * PI.toFloat()),
    )
}

private val DRIZZLE_12: List<ParticleSpec> = List(12) { i ->
    ParticleSpec(
        xFraction = ((i * 19 + 5) % 94 + 3) / 100f,
        phase = (i * 0.13f) % 1f,
        durationSeconds = 2.2f + (i % 4) * 0.35f,
        lengthFactor = 0.8f,
        alpha = 0.38f + (i % 3) * 0.08f,
    )
}

private val RAIN_26: List<ParticleSpec> = List(26) { i ->
    ParticleSpec(
        xFraction = ((i * 29 + 3) % 96 + 2) / 100f,
        phase = (i * 0.17f) % 1f,
        durationSeconds = 1.0f + (i % 5) * 0.18f,
        lengthFactor = 1.0f + (i % 3) * 0.18f,
        alpha = 0.45f + (i % 4) * 0.10f,
    )
}

private val STORM_34: List<ParticleSpec> = List(34) { i ->
    ParticleSpec(
        xFraction = ((i * 31 + 9) % 98 + 1) / 100f,
        phase = (i * 0.11f) % 1f,
        durationSeconds = 0.72f + (i % 4) * 0.12f,
        lengthFactor = 1.25f + (i % 3) * 0.2f,
        alpha = 0.55f + (i % 3) * 0.12f,
    )
}

private val SNOW_30: List<ParticleSpec> = List(30) { i ->
    ParticleSpec(
        xFraction = ((i * 41 + 13) % 96 + 2) / 100f,
        phase = (i * 0.19f) % 1f,
        durationSeconds = 4.0f + (i % 5) * 0.95f,
        lengthFactor = 1.0f + (i % 3) * 0.35f,
        alpha = 0.55f + (i % 4) * 0.10f,
    )
}

@Composable
fun AtmosphericBackground(
    visualState: WeatherVisualState,
    isDay: Boolean,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val systemAnimatorScale = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
    }
    val effectiveReduce = reduceAnimations || systemAnimatorScale == 0f

    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Continuous frame clock read strictly inside Canvas draw phase (no tree recomposition).
    val animTimeSeconds = remember { mutableFloatStateOf(2.4f) }

    LaunchedEffect(isForeground, effectiveReduce) {
        if (!isForeground || effectiveReduce) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val deltaSec = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                    animTimeSeconds.floatValue += deltaSec
                }
                lastNanos = frameNanos
            }
        }
    }

    val targetColor = remember(visualState, isDay) {
        backgroundColorFor(visualState, isDay)
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
        label = "atmospheric_bg_color",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBgColor),
    ) {
        Crossfade(
            targetState = Pair(visualState, isDay),
            animationSpec = tween(durationMillis = 800),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(),
            label = "atmospheric_scene_crossfade",
        ) { (state, dayFlag) ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(),
            ) {
                val t = animTimeSeconds.floatValue
                when (state) {
                    WeatherVisualState.SUNNY -> {
                        drawSunWithHalos(t)
                        drawDriftingCloud(
                            t = t,
                            durationSeconds = 26f,
                            startFraction = 0.15f,
                            yFraction = 0.25f,
                            scale = 0.95f,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        drawDriftingCloud(
                            t = t,
                            durationSeconds = 34f,
                            startFraction = 0.65f,
                            yFraction = 0.36f,
                            scale = 0.72f,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }

                    WeatherVisualState.CLEAR_NIGHT -> {
                        drawTwinklingStars(t)
                        drawCrescentMoon(bgColor = SkyClearNight)
                    }

                    WeatherVisualState.PARTLY_CLOUDY -> {
                        if (dayFlag) {
                            drawSunWithHalos(t)
                        } else {
                            drawTwinklingStars(t)
                            drawCrescentMoon(bgColor = SkyClearNight)
                        }
                        val cloudAlpha = if (dayFlag) 0.86f else 0.34f
                        drawDriftingCloud(t, 22f, 0.12f, 0.22f, 1.0f, Color.White.copy(alpha = cloudAlpha))
                        drawDriftingCloud(t, 28f, 0.52f, 0.33f, 0.8f, Color.White.copy(alpha = cloudAlpha * 0.9f))
                        drawDriftingCloud(t, 35f, 0.82f, 0.16f, 0.68f, Color.White.copy(alpha = cloudAlpha * 0.8f))
                    }

                    WeatherVisualState.CLOUDY -> {
                        drawDriftingCloud(t, 12f, 0.15f, 0.16f, 1.35f, Color.White.copy(alpha = 0.28f))
                        drawDriftingCloud(t, 16f, 0.55f, 0.27f, 1.45f, Color(0xFFD5DCE3).copy(alpha = 0.34f))
                        drawDriftingCloud(t, 20f, 0.85f, 0.12f, 1.25f, Color.White.copy(alpha = 0.22f))
                    }

                    WeatherVisualState.FOG -> {
                        drawFogBands(t)
                    }

                    WeatherVisualState.DRIZZLE -> {
                        drawDriftingCloud(t, 18f, 0.2f, 0.14f, 1.2f, Color(0xFFB5C2CF).copy(alpha = 0.42f))
                        drawDriftingCloud(t, 24f, 0.7f, 0.22f, 1.05f, Color(0xFF9AA9B8).copy(alpha = 0.38f))
                        drawRainDrops(t = t, drops = DRIZZLE_12, strokeDp = 1.1f, dropLenDp = 11f, slantFactor = 0.08f)
                    }

                    WeatherVisualState.RAIN -> {
                        drawDriftingCloud(t, 15f, 0.22f, 0.13f, 1.35f, RainDarkCloud.copy(alpha = 0.92f))
                        drawDriftingCloud(t, 19f, 0.68f, 0.20f, 1.20f, RainDarkCloud.copy(alpha = 0.82f))
                        drawRainDrops(t = t, drops = RAIN_26, strokeDp = 1.6f, dropLenDp = 18f, slantFactor = 0.12f)
                    }

                    WeatherVisualState.STORM -> {
                        drawDriftingCloud(t, 14f, 0.18f, 0.11f, 1.45f, StormCloud.copy(alpha = 0.96f))
                        drawDriftingCloud(t, 17f, 0.64f, 0.18f, 1.32f, StormCloud.copy(alpha = 0.90f))
                        drawRainDrops(t = t, drops = STORM_34, strokeDp = 1.9f, dropLenDp = 24f, slantFactor = 0.28f)
                        if (!effectiveReduce) {
                            drawSubtleLightning(t)
                        }
                    }

                    WeatherVisualState.SNOW -> {
                        drawDriftingCloud(t, 20f, 0.25f, 0.14f, 1.25f, Color.White.copy(alpha = 0.30f))
                        drawSnowFlakes(t)
                    }
                }
            }
        }
    }
}

private fun backgroundColorFor(state: WeatherVisualState, isDay: Boolean): Color =
    when (state) {
        WeatherVisualState.SUNNY -> SkySunny
        WeatherVisualState.CLEAR_NIGHT -> SkyClearNight
        WeatherVisualState.PARTLY_CLOUDY -> if (isDay) SkySunny else SkyClearNight
        WeatherVisualState.CLOUDY -> SkyCloudy
        WeatherVisualState.FOG -> SkyFog
        WeatherVisualState.DRIZZLE -> SkyDrizzle
        WeatherVisualState.RAIN -> SkyRain
        WeatherVisualState.STORM -> SkyStorm
        WeatherVisualState.SNOW -> SkySnow
    }

private fun DrawScope.drawSunWithHalos(t: Float) {
    val center = Offset(x = size.width * 0.83f, y = size.height * 0.14f)
    val baseRadius = size.minDimension * 0.09f

    val halo1Scale = 1.0f + 0.10f * ((sin(t * (2f * PI.toFloat() / 5.5f)) + 1f) * 0.5f)
    val halo2Scale = 1.0f + 0.10f * ((sin(t * (2f * PI.toFloat() / 6.8f) + 1.4f) + 1f) * 0.5f)

    drawCircle(
        color = SunYellow.copy(alpha = 0.16f),
        radius = baseRadius * 1.85f * halo2Scale,
        center = center,
    )
    drawCircle(
        color = SunYellow.copy(alpha = 0.26f),
        radius = baseRadius * 1.40f * halo1Scale,
        center = center,
    )
    drawCircle(
        color = SunYellow,
        radius = baseRadius,
        center = center,
    )
}

private fun DrawScope.drawCrescentMoon(bgColor: Color) {
    val center = Offset(x = size.width * 0.82f, y = size.height * 0.14f)
    val radius = size.minDimension * 0.075f
    drawCircle(
        color = Color(0xFFF6F1D5),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = bgColor,
        radius = radius * 0.88f,
        center = Offset(center.x + radius * 0.36f, center.y - radius * 0.22f),
    )
}

private fun DrawScope.drawTwinklingStars(t: Float) {
    STARS_24.forEach { star ->
        val twinkle = (sin(t * (2f * PI.toFloat() / star.cycleSeconds) + star.phase) + 1f) * 0.5f
        val alpha = 0.25f + 0.70f * twinkle
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = star.radiusDp.dp.toPx(),
            center = Offset(size.width * star.xFraction, size.height * star.yFraction),
        )
    }
}

private fun DrawScope.drawDriftingCloud(
    t: Float,
    durationSeconds: Float,
    startFraction: Float,
    yFraction: Float,
    scale: Float,
    color: Color,
) {
    val progress = ((t / durationSeconds) + startFraction) % 1f
    val cloudWidth = 140.dp.toPx() * scale
    val travelSpan = size.width + cloudWidth * 2f
    val cx = -cloudWidth + travelSpan * progress
    val cy = size.height * yFraction

    // Clean minimalist multi-lobe flat cloud silhouette
    val rMain = 26.dp.toPx() * scale
    drawCircle(color = color, radius = rMain * 0.85f, center = Offset(cx - rMain * 1.05f, cy + rMain * 0.18f))
    drawCircle(color = color, radius = rMain * 1.15f, center = Offset(cx - rMain * 0.15f, cy - rMain * 0.22f))
    drawCircle(color = color, radius = rMain * 0.92f, center = Offset(cx + rMain * 0.95f, cy + rMain * 0.12f))
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - rMain * 1.85f, cy + rMain * 0.05f),
        size = Size(rMain * 3.65f, rMain * 1.05f),
        cornerRadius = CornerRadius(rMain * 0.55f, rMain * 0.55f),
    )
}

private fun DrawScope.drawFogBands(t: Float) {
    val bands = listOf(
        Triple(0.14f, 22f, 0.24f),
        Triple(0.28f, -26f, 0.18f),
        Triple(0.42f, 24f, 0.28f),
        Triple(0.58f, -29f, 0.16f),
    )
    val bandHeight = 36.dp.toPx()
    bands.forEachIndexed { idx, (yFrac, dur, alpha) ->
        val dir = if (dur > 0) 1f else -1f
        val absDur = kotlin.math.abs(dur)
        val drift = sin(t * (2f * PI.toFloat() / absDur) + idx * 1.1f) * dir * size.width * 0.12f
        val bandWidth = size.width * 0.92f
        val left = (size.width - bandWidth) * 0.5f + drift
        drawRoundRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(left, size.height * yFrac),
            size = Size(bandWidth, bandHeight),
            cornerRadius = CornerRadius(bandHeight / 2f, bandHeight / 2f),
        )
    }
}

private fun DrawScope.drawRainDrops(
    t: Float,
    drops: List<ParticleSpec>,
    strokeDp: Float,
    dropLenDp: Float,
    slantFactor: Float,
) {
    val strokePx = strokeDp.dp.toPx()
    val baseLenPx = dropLenDp.dp.toPx()
    drops.forEach { drop ->
        val yProgress = ((t / drop.durationSeconds) + drop.phase) % 1f
        val startY = -baseLenPx + (size.height + baseLenPx * 2f) * yProgress
        val startX = (size.width * drop.xFraction + yProgress * size.width * slantFactor * 0.25f) % size.width
        val len = baseLenPx * drop.lengthFactor
        drawLine(
            color = Color.White.copy(alpha = drop.alpha),
            start = Offset(startX, startY),
            end = Offset(startX - len * slantFactor, startY + len),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSubtleLightning(t: Float) {
    // Cycle of 11 seconds (~5.4 flashes/min max, strictly <= 3 flashes per minute? Let's use 21s cycle!)
    // Wait: max 1 every 8-14s AND never > 3/min -> 21s cycle guarantees <= 3/min while feeling natural,
    // or irregular 20s window with 1 flash per 20s (= 3/min) with 12s & 20s spacing!
    val cycle = 21f
    val inCycle = t % cycle
    val flashAlpha = when {
        inCycle in 8.0f..8.15f -> {
            val p = (inCycle - 8.0f) / 0.15f
            0.20f * sin(p * PI.toFloat())
        }
        else -> 0f
    }
    if (flashAlpha > 0.005f) {
        drawRect(color = Color.White.copy(alpha = flashAlpha))
    }
}

private fun DrawScope.drawSnowFlakes(t: Float) {
    val path = Path()
    path.reset()
    SNOW_30.forEachIndexed { index, flake ->
        val progress = ((t / flake.durationSeconds) + flake.phase) % 1f
        val sway = sin(t * 1.4f + index) * 14.dp.toPx()
        val cx = (size.width * flake.xFraction + sway).coerceIn(4f, size.width - 4f)
        val cy = -12f + (size.height + 24f) * progress
        val r = (2.1f * flake.lengthFactor).dp.toPx()
        drawCircle(
            color = Color.White.copy(alpha = flake.alpha),
            radius = r,
            center = Offset(cx, cy),
        )
    }
}
