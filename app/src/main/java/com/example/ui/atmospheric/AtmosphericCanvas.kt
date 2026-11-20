package com.example.ui.atmospheric

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.WeatherVisualState
import com.example.ui.theme.CloudyBottom
import com.example.ui.theme.CloudyMid
import com.example.ui.theme.CloudyTop
import com.example.ui.theme.FogBottom
import com.example.ui.theme.FogMid
import com.example.ui.theme.FogTop
import com.example.ui.theme.LightningFlashColor
import com.example.ui.theme.NightBottom
import com.example.ui.theme.NightMid
import com.example.ui.theme.NightTop
import com.example.ui.theme.RainBottom
import com.example.ui.theme.RainMid
import com.example.ui.theme.RainTop
import com.example.ui.theme.SnowBottom
import com.example.ui.theme.SnowMid
import com.example.ui.theme.SnowTop
import com.example.ui.theme.StormBottom
import com.example.ui.theme.StormCloudDark
import com.example.ui.theme.StormMid
import com.example.ui.theme.StormRainStreak
import com.example.ui.theme.StormTop
import com.example.ui.theme.SunAmber
import com.example.ui.theme.SunnyBottom
import com.example.ui.theme.SunnyMid
import com.example.ui.theme.SunnyTop
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private class PooledRainDrop(
    var xFraction: Float,
    var yFraction: Float,
    val speedCyclesPerSec: Float,
    val lengthDp: Float,
    val strokeDp: Float,
    val alpha: Float,
)

private class PooledSplashParticle(
    var active: Boolean = false,
    var originXFrac: Float = 0f,
    var originYFrac: Float = 0f,
    var vxDp: Float = 0f,
    var vyDp: Float = 0f,
    var ageSec: Float = 0f,
    var maxLifeSec: Float = 0.4f,
)

private class StormEnginePool(isLowEndDevice: Boolean) {
    val farDrops: Array<PooledRainDrop>
    val midDrops: Array<PooledRainDrop>
    val nearDrops: Array<PooledRainDrop>
    val splashes: Array<PooledSplashParticle>
    val snowFlakes: Array<PooledRainDrop>

    var seed: Int = 1337
    var nextSplashCooldownSec: Float = 0.14f
    var nextLightningCooldownSec: Float = 6.2f
    var lightningAgeSec: Float = -1f

    init {
        val farCount = if (isLowEndDevice) 14 else 26
        val midCount = if (isLowEndDevice) 12 else 22
        val nearCount = if (isLowEndDevice) 10 else 16
        val splashCount = if (isLowEndDevice) 16 else 32

        farDrops = Array(farCount) { i ->
            PooledRainDrop(
                xFraction = ((i * 37 + 11) % 100) / 100f,
                yFraction = ((i * 23 + 7) % 115) / 100f - 0.10f,
                speedCyclesPerSec = 1f / 1.30f,
                lengthDp = 24f + (i % 3) * 4f,
                strokeDp = 1.0f,
                alpha = 0.20f,
            )
        }
        midDrops = Array(midCount) { i ->
            PooledRainDrop(
                xFraction = ((i * 43 + 19) % 100) / 100f,
                yFraction = ((i * 31 + 13) % 115) / 100f - 0.10f,
                speedCyclesPerSec = 1f / 0.85f,
                lengthDp = 38f + (i % 3) * 6f,
                strokeDp = 1.4f,
                alpha = 0.32f,
            )
        }
        nearDrops = Array(nearCount) { i ->
            PooledRainDrop(
                xFraction = ((i * 53 + 29) % 100) / 100f,
                yFraction = ((i * 41 + 17) % 115) / 100f - 0.10f,
                speedCyclesPerSec = 1f / 0.55f,
                lengthDp = 56f + (i % 3) * 8f,
                strokeDp = 1.9f,
                alpha = 0.50f,
            )
        }
        splashes = Array(splashCount) { PooledSplashParticle() }
        snowFlakes = Array(if (isLowEndDevice) 16 else 28) { i ->
            PooledRainDrop(
                xFraction = ((i * 41 + 13) % 96 + 2) / 100f,
                yFraction = ((i * 19 + 3) % 100) / 100f,
                speedCyclesPerSec = 1f / (4.5f + (i % 5) * 0.8f),
                lengthDp = 2.2f + (i % 3) * 0.7f,
                strokeDp = 0f,
                alpha = 0.45f,
            )
        }
    }

    fun nextFloat(): Float {
        seed = seed * 1664525 + 1013904223
        val bits = (seed ushr 8) and 0xFFFFFF
        return bits / 16777216f
    }

    fun step(
        dt: Float,
        state: WeatherVisualState,
        reduceAnimations: Boolean,
    ) {
        val isStorm = state == WeatherVisualState.STORM
        val isRain = state == WeatherVisualState.RAIN
        if (isStorm || isRain) {
            stepRainLayer(farDrops, dt)
            if (!reduceAnimations && isStorm) {
                stepRainLayer(midDrops, dt)
                stepRainLayer(nearDrops, dt)
            } else if (!reduceAnimations && isRain) {
                stepRainLayer(midDrops, dt * 0.72f)
            }
        }

        if (state == WeatherVisualState.SNOW) {
            for (flake in snowFlakes) {
                flake.yFraction += dt * flake.speedCyclesPerSec
                if (flake.yFraction > 1.05f) {
                    flake.yFraction = -0.05f
                    flake.xFraction = nextFloat()
                }
            }
        }

        // Layer (d): Splashes over temperature and top card edges (only when not reduced)
        if ((isStorm || isRain) && !reduceAnimations) {
            for (p in splashes) {
                if (p.active) {
                    p.ageSec += dt
                    if (p.ageSec >= p.maxLifeSec) {
                        p.active = false
                    }
                }
            }
            nextSplashCooldownSec -= dt
            if (nextSplashCooldownSec <= 0f) {
                nextSplashCooldownSec = if (isStorm) 0.14f + nextFloat() * 0.16f else 0.34f + nextFloat() * 0.25f
                spawnSplashCluster()
            }
        } else {
            for (p in splashes) p.active = false
        }

        // Layer (e): Lightning ONLY in storm and when animations are not reduced
        if (isStorm && !reduceAnimations) {
            if (lightningAgeSec >= 0f) {
                lightningAgeSec += dt
                if (lightningAgeSec > 0.44f) {
                    lightningAgeSec = -1f
                }
            } else {
                nextLightningCooldownSec -= dt
                if (nextLightningCooldownSec <= 0f) {
                    lightningAgeSec = 0f
                    nextLightningCooldownSec = 5.4f + nextFloat() * 3.4f // ~7s avg with random variance
                }
            }
        } else {
            lightningAgeSec = -1f
        }
    }

    private fun stepRainLayer(
        pool: Array<PooledRainDrop>,
        dt: Float,
    ) {
        for (drop in pool) {
            drop.yFraction += dt * drop.speedCyclesPerSec
            if (drop.yFraction > 1.12f) {
                // Object pooling: recycle existing drop instance in place
                drop.yFraction = -0.10f - nextFloat() * 0.12f
                drop.xFraction = nextFloat()
            }
        }
    }

    private fun spawnSplashCluster() {
        // Impact zones: hero temperature top (~0.18f) and top borders of cards (~0.43f, 0.61f, 0.78f)
        val zoneRoll = nextFloat()
        val (impactX, impactY) = when {
            zoneRoll < 0.30f -> (0.37f + nextFloat() * 0.26f) to 0.18f
            zoneRoll < 0.56f -> (0.12f + nextFloat() * 0.76f) to 0.43f
            zoneRoll < 0.80f -> (0.12f + nextFloat() * 0.76f) to 0.61f
            else -> (0.12f + nextFloat() * 0.76f) to 0.78f
        }
        val count = 2 + (nextFloat() * 3f).toInt().coerceAtMost(2) // 2 to 4 particles
        var spawned = 0
        for (p in splashes) {
            if (!p.active) {
                p.active = true
                p.originXFrac = impactX
                p.originYFrac = impactY
                p.vxDp = (nextFloat() - 0.5f) * 34f
                p.vyDp = -(18f + nextFloat() * 20f)
                p.ageSec = 0f
                p.maxLifeSec = 0.30f + nextFloat() * 0.20f // 300 to 500 ms
                spawned++
                if (spawned >= count) break
            }
        }
    }

    fun lightningAlpha(): Float {
        val age = lightningAgeSec
        if (age < 0f) return 0f
        return when {
            age < 0.05f -> (age / 0.05f) * 0.28f
            age < 0.11f -> 0.28f * (1f - (age - 0.05f) / 0.06f) + 0.03f
            age < 0.16f -> 0.03f
            age < 0.22f -> 0.03f + ((age - 0.16f) / 0.06f) * 0.21f
            age < 0.44f -> 0.24f * (1f - (age - 0.22f) / 0.22f)
            else -> 0f
        }.coerceIn(0f, 0.28f)
    }
}

private data class StarSpec(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val cycleSeconds: Float,
    val phase: Float,
)

private val STARS_24: List<StarSpec> = List(24) { i ->
    StarSpec(
        xFraction = ((i * 37 + 11) % 96 + 2) / 100f,
        yFraction = ((i * 23 + 7) % 52 + 4) / 100f,
        radiusDp = 1.1f + (i % 3) * 0.55f,
        cycleSeconds = 2.5f + (i % 6) * 0.5f,
        phase = (i * 0.47f) % (2f * PI.toFloat()),
    )
}

/**
 * Single-Canvas atmospheric background engine supporting:
 * - Storm 5-layer engine (Base gradient, blurred drifting dark clouds, 3-depth pooled rain,
 *   impact splashes on temperature/card edges, and ~7s double-blink lightning)
 * - Other climates (Sunny, Cloudy, Light Rain, Fog, Night, Snow)
 * - Auto-pause in background and automatic/manual "Reducir animaciones"
 */
@Composable
fun AtmosphericBackground(
    visualState: WeatherVisualState,
    userReduceAnimations: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isLowEndDevice = remember(context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        (am?.isLowRamDevice == true) || Runtime.getRuntime().availableProcessors() <= 4
    }

    var isBatteryLowOrSaver by remember { mutableStateOf(checkLowBatteryOrPowerSaver(context)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                isBatteryLowOrSaver = checkLowBatteryOrPowerSaver(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val systemAnimatorScale = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
    }

    val effectiveReduceAnimations = userReduceAnimations || systemAnimatorScale == 0f || isBatteryLowOrSaver
    val freezeMotion = systemAnimatorScale == 0f

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

    val pool = remember(isLowEndDevice) { StormEnginePool(isLowEndDevice) }
    val animTimeSeconds = remember { mutableFloatStateOf(2.4f) }

    LaunchedEffect(isForeground, freezeMotion, visualState, effectiveReduceAnimations) {
        if (!isForeground || freezeMotion) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    animTimeSeconds.floatValue += dt
                    pool.step(
                        dt = dt,
                        state = visualState,
                        reduceAnimations = effectiveReduceAnimations,
                    )
                }
                lastNanos = frameNanos
            }
        }
    }

    val stops = remember(visualState) { gradientStopsFor(visualState) }
    val c0 by animateColorAsState(stops[0], tween(700), label = "bg0")
    val c1 by animateColorAsState(stops[1], tween(700), label = "bg1")
    val c2 by animateColorAsState(stops[2], tween(700), label = "bg2")

    // Single Canvas executing all background layers
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = animTimeSeconds.floatValue

        // Layer (a): Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to c0,
                0.50f to c1,
                1.0f to c2,
            ),
        )

        when (visualState) {
            WeatherVisualState.STORM -> {
                // Layer (b): 3 dark blurred oval clouds drifting horizontally back & forth (12s-19s)
                drawBlurredOvalCloud(t, cycleSec = 13.0f, baseXFrac = 0.24f, yFrac = 0.14f, rxFrac = 0.44f, ryFrac = 0.14f, color = StormCloudDark)
                drawBlurredOvalCloud(t, cycleSec = 16.5f, baseXFrac = 0.72f, yFrac = 0.24f, rxFrac = 0.48f, ryFrac = 0.15f, color = StormCloudDark)
                drawBlurredOvalCloud(t, cycleSec = 19.0f, baseXFrac = 0.48f, yFrac = 0.08f, rxFrac = 0.40f, ryFrac = 0.12f, color = StormCloudDark)

                // Layer (c): 3 depth layers of ~10° slanted #E1D7FF rain (or 1 layer if reduced)
                drawRainPool(pool.farDrops)
                if (!effectiveReduceAnimations) {
                    drawRainPool(pool.midDrops)
                    drawRainPool(pool.nearDrops)
                }

                // Layer (d): Splashes over temperature and card top borders
                if (!effectiveReduceAnimations) {
                    drawSplashes(pool.splashes)
                }

                // Layer (e): Full-screen lightning double-blink flash (#D8D2FF, max 28%)
                val flashAlpha = pool.lightningAlpha()
                if (flashAlpha > 0.005f) {
                    drawRect(color = LightningFlashColor.copy(alpha = flashAlpha))
                }
            }

            WeatherVisualState.RAIN -> {
                drawBlurredOvalCloud(t, cycleSec = 15.0f, baseXFrac = 0.35f, yFrac = 0.15f, rxFrac = 0.42f, ryFrac = 0.13f, color = StormCloudDark.copy(alpha = 0.35f))
                drawBlurredOvalCloud(t, cycleSec = 18.0f, baseXFrac = 0.70f, yFrac = 0.22f, rxFrac = 0.45f, ryFrac = 0.14f, color = StormCloudDark.copy(alpha = 0.30f))
                drawRainPool(pool.farDrops, alphaScale = 0.85f)
                if (!effectiveReduceAnimations) {
                    drawRainPool(pool.midDrops, alphaScale = 0.65f)
                    drawSplashes(pool.splashes)
                }
            }

            WeatherVisualState.SUNNY -> {
                val center = Offset(size.width * 0.80f, size.height * 0.14f)
                val sunR = size.minDimension * 0.10f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SunAmber.copy(alpha = 0.34f), Color.Transparent),
                        center = center,
                        radius = sunR * 2.8f,
                    ),
                    radius = sunR * 2.8f,
                    center = center,
                )
                drawCircle(color = SunAmber, radius = sunR, center = center)
                drawBlurredOvalCloud(t, 16.0f, 0.28f, 0.22f, 0.30f, 0.09f, Color.White.copy(alpha = 0.16f))
                drawBlurredOvalCloud(t, 19.0f, 0.62f, 0.33f, 0.26f, 0.08f, Color.White.copy(alpha = 0.12f))
            }

            WeatherVisualState.CLOUDY -> {
                drawBlurredOvalCloud(t, 14.0f, 0.22f, 0.16f, 0.42f, 0.13f, Color.White.copy(alpha = 0.14f))
                drawBlurredOvalCloud(t, 17.5f, 0.68f, 0.26f, 0.46f, 0.14f, StormCloudDark.copy(alpha = 0.35f))
                drawBlurredOvalCloud(t, 19.0f, 0.45f, 0.36f, 0.38f, 0.11f, Color.White.copy(alpha = 0.10f))
            }

            WeatherVisualState.FOG -> {
                val bandH = 44.dp.toPx()
                listOf(0.16f, 0.29f, 0.44f, 0.59f).forEachIndexed { idx, yFrac ->
                    val drift = sin(t * (2f * PI.toFloat() / (14f + idx * 2f)) + idx) * size.width * 0.10f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f + (idx % 2) * 0.05f),
                        topLeft = Offset(size.width * 0.04f + drift, size.height * yFrac),
                        size = Size(size.width * 0.92f, bandH),
                        cornerRadius = CornerRadius(bandH / 2f, bandH / 2f),
                    )
                }
            }

            WeatherVisualState.NIGHT -> {
                STARS_24.forEach { star ->
                    val twinkle = (sin(t * (2f * PI.toFloat() / star.cycleSeconds) + star.phase) + 1f) * 0.5f
                    val alpha = 0.20f + 0.55f * twinkle
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = star.radiusDp.dp.toPx(),
                        center = Offset(size.width * star.xFraction, size.height * star.yFraction),
                    )
                }
            }

            WeatherVisualState.SNOW -> {
                pool.snowFlakes.forEachIndexed { idx, flake ->
                    val sway = sin(t * 1.2f + idx) * 10.dp.toPx()
                    val cx = (size.width * flake.xFraction + sway).coerceIn(4f, size.width - 4f)
                    val cy = size.height * flake.yFraction
                    drawCircle(
                        color = Color.White.copy(alpha = flake.alpha),
                        radius = flake.lengthDp.dp.toPx(),
                        center = Offset(cx, cy),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBlurredOvalCloud(
    t: Float,
    cycleSec: Float,
    baseXFrac: Float,
    yFrac: Float,
    rxFrac: Float,
    ryFrac: Float,
    color: Color,
) {
    val sway = sin(t * (2f * PI.toFloat() / cycleSec)) * size.width * 0.14f
    val cx = size.width * baseXFrac + sway
    val cy = size.height * yFrac
    val rx = size.width * rxFrac
    val ry = size.height * ryFrac

    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = 1f, scaleY = (ry / rx).coerceAtLeast(0.2f), pivot = Offset.Zero)
    }) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, color.copy(alpha = color.alpha * 0.5f), Color.Transparent),
                center = Offset.Zero,
                radius = rx,
            ),
            radius = rx,
            center = Offset.Zero,
        )
    }
}

private fun DrawScope.drawRainPool(
    pool: Array<PooledRainDrop>,
    alphaScale: Float = 1f,
) {
    // 10-degree slant: tan(10°) ≈ 0.176f
    val slantRatio = -0.176f
    for (drop in pool) {
        val lenPx = drop.lengthDp.dp.toPx()
        val startX = drop.xFraction * size.width
        val startY = drop.yFraction * size.height
        drawLine(
            color = StormRainStreak.copy(alpha = drop.alpha * alphaScale),
            start = Offset(startX, startY),
            end = Offset(startX + lenPx * slantRatio, startY + lenPx),
            strokeWidth = drop.strokeDp.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSplashes(splashes: Array<PooledSplashParticle>) {
    val gravityDp = 135f
    for (p in splashes) {
        if (!p.active) continue
        val age = p.ageSec
        val progress = (age / p.maxLifeSec).coerceIn(0f, 1f)
        val dx = (p.vxDp * age).dp.toPx()
        val dy = (p.vyDp * age + 0.5f * gravityDp * age * age).dp.toPx()
        val cx = p.originXFrac * size.width + dx
        val cy = p.originYFrac * size.height + dy
        val alpha = (1f - progress) * 0.65f
        drawCircle(
            color = StormRainStreak.copy(alpha = alpha),
            radius = 1.6.dp.toPx(),
            center = Offset(cx, cy),
        )
    }
}

private fun gradientStopsFor(state: WeatherVisualState): List<Color> = when (state) {
    WeatherVisualState.STORM -> listOf(StormTop, StormMid, StormBottom)
    WeatherVisualState.SUNNY -> listOf(SunnyTop, SunnyMid, SunnyBottom)
    WeatherVisualState.CLOUDY -> listOf(CloudyTop, CloudyMid, CloudyBottom)
    WeatherVisualState.RAIN -> listOf(RainTop, RainMid, RainBottom)
    WeatherVisualState.FOG -> listOf(FogTop, FogMid, FogBottom)
    WeatherVisualState.NIGHT -> listOf(NightTop, NightMid, NightBottom)
    WeatherVisualState.SNOW -> listOf(SnowTop, SnowMid, SnowBottom)
}

private fun checkLowBatteryOrPowerSaver(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    if (pm?.isPowerSaveMode == true) return true
    val batteryIntent = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull() ?: return false
    val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    if (level >= 0 && scale > 0 && !isCharging) {
        val pct = (level * 100f) / scale
        return pct <= 15f
    }
    return false
}
