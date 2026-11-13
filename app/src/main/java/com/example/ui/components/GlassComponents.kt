package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.RecommendationIconType
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardBorderHighlighted
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GlassCardFillHighlighted
import kotlinx.coroutines.delay

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 15.dp,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = if (highlighted) GlassCardFillHighlighted else GlassCardFill
    val borderColor = if (highlighted) GlassCardBorderHighlighted else GlassCardBorder

    Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = fillColor,
        contentColor = Color.White,
        border = BorderStroke(0.5.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
fun PulsingSparkleIcon(
    reduceAnimations: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val alpha = if (reduceAnimations) {
        0.92f
    } else {
        val transition = rememberInfiniteTransition(label = "sparkle_pulse")
        val animated by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "sparkle_alpha",
        )
        animated
    }

    Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = modifier
            .size(18.dp)
            .graphicsLayer { this.alpha = alpha },
    )
}

@Composable
fun ThreeDotsThinkingIndicator(
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reduceAnimations) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f)),
                )
            }
        } else {
            val transition = rememberInfiniteTransition(label = "thinking_dots")
            repeat(3) { index ->
                val dotAlpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 600,
                            delayMillis = index * 160,
                        ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot_$index",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .graphicsLayer { alpha = dotAlpha }
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
fun TypewriterText(
    fullText: String,
    animationKey: Int,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
) {
    var visibleCount by remember(fullText, animationKey, reduceAnimations) {
        mutableIntStateOf(if (reduceAnimations) fullText.length else 0)
    }

    LaunchedEffect(fullText, animationKey, reduceAnimations) {
        if (reduceAnimations) {
            visibleCount = fullText.length
            return@LaunchedEffect
        }
        visibleCount = 0
        for (i in 1..fullText.length) {
            visibleCount = i
            delay(16L)
        }
    }

    val displayed = if (visibleCount >= fullText.length) {
        fullText
    } else {
        fullText.take(visibleCount)
    }

    Text(
        text = displayed,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.94f),
        modifier = modifier,
    )
}

@Composable
fun RecommendationCircularIcon(
    iconType: RecommendationIconType,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = when (iconType) {
        RecommendationIconType.SUNSCREEN -> Icons.Rounded.WbSunny
        RecommendationIconType.HYDRATE -> Icons.Rounded.WaterDrop
        RecommendationIconType.UMBRELLA -> Icons.Rounded.Umbrella
        RecommendationIconType.COAT -> Icons.Rounded.Checkroom
        RecommendationIconType.FOG_DRIVE -> Icons.Rounded.DirectionsCar
        RecommendationIconType.STORM_SHELTER -> Icons.Rounded.Home
        RecommendationIconType.STARS -> Icons.Rounded.Bedtime
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
