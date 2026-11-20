package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ChatSender
import com.example.model.HourlyRainBar
import com.example.model.WeatherSnapshot
import com.example.model.WeatherVisualState
import com.example.ui.atmospheric.AtmosphericBackground
import com.example.ui.components.JumpingThreeDots
import com.example.ui.components.OpaqueCard
import com.example.ui.components.OpaqueChip
import com.example.ui.components.OpaqueCircleButton
import com.example.ui.components.SparkleVectorIcon
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.TypewriterText
import com.example.ui.components.WeatherIcon
import com.example.ui.state.ClimaUiState
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LightShapeViolet
import com.example.ui.theme.OpaqueCardColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleColor
import kotlin.math.roundToInt

@Composable
fun WeatherChatScreen(
    uiState: ClimaUiState,
    canNavigateBack: Boolean = true,
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryLastQuestion: () -> Unit,
    onClearChat: () -> Unit,
    onRefreshWeather: () -> Unit,
) {
    val snapshot = uiState.activeSnapshot
    val visualState = snapshot?.visualState ?: WeatherVisualState.STORM
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.chatMessages.size, uiState.isChatSending) {
        val total = uiState.chatMessages.size + if (uiState.isChatSending) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    val suggestedChips = listOf(
        stringResource(R.string.chip_umbrella),
        stringResource(R.string.chip_what_to_wear),
        stringResource(R.string.chip_rain_stop),
        stringResource(R.string.chip_tomorrow),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AtmosphericBackground(
            visualState = visualState,
            userReduceAnimations = uiState.userReduceAnimations,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Encabezado "Asistente del clima"
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (canNavigateBack) {
                    OpaqueCircleButton(
                        onClick = onNavigateBack,
                        size = 44.dp,
                        modifier = Modifier.testTag("chat_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.back_content_description),
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Text(
                    text = stringResource(R.string.chat_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Box {
                    OpaqueCircleButton(
                        onClick = { isMenuExpanded = true },
                        size = 44.dp,
                        modifier = Modifier.testTag("chat_menu_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options_content_description),
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        containerColor = OpaqueCardColor,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_refresh_weather),
                                    color = TextPrimary,
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onRefreshWeather()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_clear_chat),
                                    color = TextPrimary,
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onClearChat()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta fija de resumen: "31° · Tormentas fuertes · Se siente como 37°"
            if (snapshot != null) {
                ChatSummaryHeaderCard(
                    snapshot = snapshot,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Lista de mensajes del chat
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.chatMessages.isEmpty() && !uiState.isChatSending) {
                    item {
                        OpaqueCard(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SparkleVectorIcon(size = 14.dp, tint = LightShapeViolet)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.weather_ai_title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.chat_empty_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }

                items(
                    items = uiState.chatMessages,
                    key = { it.id },
                ) { message ->
                    val isNewest = message.id == uiState.chatMessages.lastOrNull()?.id
                    StaggeredEntrance(
                        index = 0,
                        delayStepMs = 0,
                        reduceAnimations = uiState.userReduceAnimations,
                    ) {
                        when (message.sender) {
                            ChatSender.User -> UserChatBubble(message = message)
                            ChatSender.Ai -> AiChatBubble(
                                message = message,
                                animateTypewriter = isNewest && !uiState.userReduceAnimations,
                                onRetry = onRetryLastQuestion,
                            )
                        }
                    }
                }

                if (uiState.isChatSending) {
                    item {
                        OpaqueCard(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.testTag("chat_loading_dots"),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            ) {
                                JumpingThreeDots()
                            }
                        }
                    }
                }
            }

            // Chips rápidos desplazables + barra de escritura
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(suggestedChips) { index, chip ->
                        OpaqueChip(
                            text = chip,
                            onClick = { onSendMessage(chip) },
                            modifier = Modifier.testTag("chat_chip_$index"),
                        )
                    }
                }

                OpaqueCard(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(54.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 18.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.ask_weather_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextPrimary,
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(TextPrimary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        val q = inputText.trim()
                                        if (q.isNotEmpty()) {
                                            inputText = ""
                                            keyboardController?.hide()
                                            onSendMessage(q)
                                        }
                                    },
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_input_field"),
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentBlue)
                                .clickable {
                                    val q = inputText.trim()
                                    if (q.isNotEmpty()) {
                                        inputText = ""
                                        keyboardController?.hide()
                                        onSendMessage(q)
                                    }
                                }
                                .testTag("chat_send_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = stringResource(R.string.send_question_content_description),
                                tint = OpaqueCardColor,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSummaryHeaderCard(
    snapshot: WeatherSnapshot,
    modifier: Modifier = Modifier,
) {
    OpaqueCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.testTag("chat_context_card"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeatherIcon(
                weatherCode = snapshot.weatherCode,
                isDay = !snapshot.isLocalNight,
                size = 22.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Medium)) {
                        append("${snapshot.temperatureC.roundToInt()}° · ${snapshot.conditionLabel}")
                    }
                    withStyle(SpanStyle(color = TextSecondary)) {
                        append(" · ")
                        append(
                            stringResource(
                                R.string.feels_like_format,
                                snapshot.apparentTemperatureC.roundToInt(),
                            ),
                        )
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UserChatBubble(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        OpaqueCard(
            backgroundColor = UserBubbleColor,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun AiChatBubble(
    message: ChatMessage,
    animateTypewriter: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        OpaqueCard(
            backgroundColor = OpaqueCardColor,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(0.90f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SparkleVectorIcon(size = 14.dp, tint = LightShapeViolet)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.weather_ai_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                if (message.isError) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("chat_retry_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.retry_button),
                            color = AccentBlue,
                        )
                    }
                } else {
                    if (animateTypewriter) {
                        TypewriterText(
                            fullText = message.text,
                            animationKey = message.id.hashCode(),
                        )
                    } else {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    }

                    if (message.rainBars.isNotEmpty()) {
                        RainProbabilityBarChart(bars = message.rainBars)
                    }
                }
            }
        }
    }
}

@Composable
private fun RainProbabilityBarChart(bars: List<HourlyRainBar>) {
    val maxChartHeight = 52.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val clamped = bar.probabilityPercent.coerceIn(0, 100)
            val barHeight = (maxChartHeight * (clamped / 100f)).coerceAtLeast(8.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "$clamped%",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentBlue,
                )
                Box(
                    modifier = Modifier.height(maxChartHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(7.dp))
                            .background(AccentBlue),
                    )
                }
                Text(
                    text = bar.hourLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
