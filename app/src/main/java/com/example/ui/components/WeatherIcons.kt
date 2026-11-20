package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CloudBaseColor
import com.example.ui.theme.LightningYellow
import com.example.ui.theme.RainCloudColor
import com.example.ui.theme.RainDropBlue
import com.example.ui.theme.StormCloudColor
import com.example.ui.theme.SunYellow

/**
 * Custom flat 4-point vector sparkle icon (zero emojis, zero shading).
 */
@Composable
fun SparkleVectorIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = Color.White,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.45f
        val cy = h * 0.52f
        val rOuter = w * 0.40f
        val rInner = w * 0.11f

        val mainStar = Path().apply {
            moveTo(cx, cy - rOuter)
            quadraticTo(cx + rInner * 0.5f, cy - rInner * 0.5f, cx + rOuter, cy)
            quadraticTo(cx + rInner * 0.5f, cy + rInner * 0.5f, cx, cy + rOuter)
            quadraticTo(cx - rInner * 0.5f, cy + rInner * 0.5f, cx - rOuter, cy)
            quadraticTo(cx - rInner * 0.5f, cy - rInner * 0.5f, cx, cy - rOuter)
            close()
        }
        drawPath(mainStar, color = tint)

        val sx = w * 0.80f
        val sy = h * 0.22f
        val sr = w * 0.14f
        val sInner = w * 0.04f
        val smallStar = Path().apply {
            moveTo(sx, sy - sr)
            quadraticTo(sx + sInner, sy - sInner, sx + sr, sy)
            quadraticTo(sx + sInner, sy + sInner, sx, sy + sr)
            quadraticTo(sx - sInner, sy + sInner, sx - sr, sy)
            quadraticTo(sx - sInner, sy - sInner, sx, sy - sr)
            close()
        }
        drawPath(smallStar, color = tint)
    }
}

/**
 * Reusable flat geometric vector weather icon component.
 * Strictly flat style: no shading, no glows, no gradients, no external image dependencies,
 * using only the solid hex colors defined in the specification.
 */
@Composable
fun WeatherIcon(
    weatherCode: Int,
    isDay: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        when {
            weatherCode == 0 && isDay -> drawFlatSun()
            weatherCode == 0 && !isDay -> drawFlatMoon()
            weatherCode in 1..2 && isDay -> drawFlatPartlyCloudySun()
            weatherCode in 1..2 && !isDay -> drawFlatPartlyCloudyMoon()
            weatherCode == 3 -> drawFlatCloud(CloudBaseColor)
            weatherCode in setOf(45, 48) -> drawFlatFog()
            weatherCode in 51..67 || weatherCode in 80..82 -> drawFlatRain()
            weatherCode in 71..77 || weatherCode in setOf(85, 86) -> drawFlatSnow()
            weatherCode in setOf(95, 96, 99) -> drawFlatStorm()
            else -> drawFlatCloud(CloudBaseColor)
        }
    }
}

private fun DrawScope.drawFlatSun() {
    val c = Offset(size.width * 0.5f, size.height * 0.5f)
    val r = size.minDimension * 0.36f
    drawCircle(color = SunYellow, radius = r, center = c)
}

private fun DrawScope.drawFlatMoon(
    center: Offset = Offset(size.width * 0.48f, size.height * 0.52f),
    radius: Float = size.minDimension * 0.34f,
) {
    val path = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    val cut = Path().apply {
        val cx = center.x + radius * 0.45f
        val cy = center.y - radius * 0.30f
        val cr = radius * 0.85f
        addOval(Rect(cx - cr, cy - cr, cx + cr, cy + cr))
    }
    val crescent = Path.combine(PathOperation.Difference, path, cut)
    drawPath(crescent, color = CloudBaseColor)
}

private fun DrawScope.drawFlatPartlyCloudySun() {
    drawCircle(
        color = SunYellow,
        radius = size.minDimension * 0.22f,
        center = Offset(size.width * 0.68f, size.height * 0.34f),
    )
    drawFlatCloudShape(
        color = CloudBaseColor,
        centerX = size.width * 0.46f,
        centerY = size.height * 0.58f,
        scale = 0.88f,
    )
}

private fun DrawScope.drawFlatPartlyCloudyMoon() {
    drawFlatMoon(
        center = Offset(size.width * 0.70f, size.height * 0.28f),
        radius = size.minDimension * 0.19f,
    )
    drawFlatCloudShape(
        color = RainCloudColor,
        centerX = size.width * 0.45f,
        centerY = size.height * 0.60f,
        scale = 0.86f,
    )
}

private fun DrawScope.drawFlatCloud(color: Color) {
    drawFlatCloudShape(
        color = color,
        centerX = size.width * 0.50f,
        centerY = size.height * 0.54f,
        scale = 1.0f,
    )
}

private fun DrawScope.drawFlatFog() {
    drawFlatCloudShape(
        color = CloudBaseColor,
        centerX = size.width * 0.50f,
        centerY = size.height * 0.40f,
        scale = 0.82f,
    )
    val stroke = (size.minDimension * 0.08f).coerceAtLeast(1.5f)
    drawLine(
        color = CloudBaseColor,
        start = Offset(size.width * 0.20f, size.height * 0.72f),
        end = Offset(size.width * 0.80f, size.height * 0.72f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = CloudBaseColor,
        start = Offset(size.width * 0.28f, size.height * 0.86f),
        end = Offset(size.width * 0.72f, size.height * 0.86f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFlatRain() {
    drawFlatCloudShape(
        color = RainCloudColor,
        centerX = size.width * 0.50f,
        centerY = size.height * 0.40f,
        scale = 0.90f,
    )
    val stroke = (size.minDimension * 0.075f).coerceAtLeast(1.4f)
    val xOffsets = listOf(0.34f, 0.50f, 0.66f)
    xOffsets.forEachIndexed { idx, xFrac ->
        val topY = size.height * (0.68f + (idx % 2) * 0.05f)
        val botY = topY + size.height * 0.16f
        drawLine(
            color = RainDropBlue,
            start = Offset(size.width * xFrac, topY),
            end = Offset(size.width * (xFrac - 0.05f), botY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawFlatSnow() {
    drawFlatCloudShape(
        color = CloudBaseColor,
        centerX = size.width * 0.50f,
        centerY = size.height * 0.40f,
        scale = 0.90f,
    )
    val r = (size.minDimension * 0.055f).coerceAtLeast(1.2f)
    drawCircle(color = RainDropBlue, radius = r, center = Offset(size.width * 0.34f, size.height * 0.76f))
    drawCircle(color = CloudBaseColor, radius = r, center = Offset(size.width * 0.50f, size.height * 0.82f))
    drawCircle(color = RainDropBlue, radius = r, center = Offset(size.width * 0.66f, size.height * 0.76f))
}

private fun DrawScope.drawFlatStorm() {
    drawFlatCloudShape(
        color = StormCloudColor,
        centerX = size.width * 0.50f,
        centerY = size.height * 0.39f,
        scale = 0.92f,
    )
    val bolt = Path().apply {
        moveTo(size.width * 0.52f, size.height * 0.52f)
        lineTo(size.width * 0.41f, size.height * 0.72f)
        lineTo(size.width * 0.50f, size.height * 0.72f)
        lineTo(size.width * 0.45f, size.height * 0.92f)
        lineTo(size.width * 0.62f, size.height * 0.67f)
        lineTo(size.width * 0.52f, size.height * 0.67f)
        close()
    }
    drawPath(bolt, color = LightningYellow)
}

private fun DrawScope.drawFlatCloudShape(
    color: Color,
    centerX: Float,
    centerY: Float,
    scale: Float,
) {
    val unit = size.minDimension * 0.16f * scale
    drawCircle(color = color, radius = unit * 1.15f, center = Offset(centerX - unit * 1.15f, centerY + unit * 0.20f))
    drawCircle(color = color, radius = unit * 1.48f, center = Offset(centerX - unit * 0.10f, centerY - unit * 0.35f))
    drawCircle(color = color, radius = unit * 1.12f, center = Offset(centerX + unit * 1.10f, centerY + unit * 0.12f))
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - unit * 2.28f, centerY + unit * 0.05f),
        size = Size(unit * 4.50f, unit * 1.32f),
        cornerRadius = CornerRadius(unit * 0.66f, unit * 0.66f),
    )
}
