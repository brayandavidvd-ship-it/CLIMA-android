package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LightShapeViolet
import com.example.ui.theme.OpaqueCardColor
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Strictly OPAQUE #14131F container (no transparency, no glass, no blur)
 * with tactile 97% press bounce response per Sections 2 & 5.
 */
@Composable
fun OpaqueCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = OpaqueCardColor,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "card_tactile_scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick ?: {},
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides TextPrimary) {
            content()
        }
    }
}

@Composable
fun OpaqueCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    OpaqueCard(
        shape = CircleShape,
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size),
    ) {
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun OpaqueChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpaqueCard(
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(38.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
            )
        }
    }
}

/**
 * Staggered entrance animation (Section 4 & 5):
 * Delay 100-150ms per step, rising 26dp and scaling 95% -> 100% with soft bounce over ~600ms.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    delayStepMs: Int = 110,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (reduceAnimations) {
        Box(modifier = modifier) { content() }
        return
    }

    val offsetY = remember { Animatable(26f) }
    val scale = remember { Animatable(0.95f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(index) {
        delay((index * delayStepMs).toLong())
        launch {
            alpha.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationY = offsetY.value.dp.toPx()
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
    ) {
        content()
    }
}

/**
 * Sequential 3 jumping dots (#C9C3FF) staggered by 150ms for Chat loading state (Section 6).
 */
@Composable
fun JumpingThreeDots(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "chat_dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val offsetProgress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 150, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_$index",
            )
            Box(
                modifier = Modifier
                    .offset(y = (-5f * offsetProgress).dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(LightShapeViolet),
            )
        }
    }
}

@Composable
fun TypewriterText(
    fullText: String,
    animationKey: Int,
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var visibleCount by remember(fullText, animationKey) {
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
            delay(14L)
        }
    }

    val shown = if (visibleCount >= fullText.length) fullText else fullText.take(visibleCount)
    Text(
        text = shown,
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        modifier = modifier,
    )
}

@Composable
fun OpaqueSkeletonCard(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
        }
    }
}
