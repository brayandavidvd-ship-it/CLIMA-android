package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.model.WeatherVisualState
import com.example.ui.theme.SettingsCardDark

/**
 * Debug-only section for SettingsScreen.
 * Strictly guarded by BuildConfig.DEBUG so it is stripped from release builds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugTestModeSection(
    selectedSimulatedState: WeatherVisualState?,
    onSelectState: (WeatherVisualState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return

    var expanded by remember { mutableStateOf(false) }
    val realOptionLabel = stringResource(R.string.debug_test_mode_real_option)
    val selectedLabel = selectedSimulatedState?.displayNameEs ?: realOptionLabel

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("debug_test_mode_section"),
        shape = RoundedCornerShape(16.dp),
        color = SettingsCardDark,
        border = BorderStroke(1.dp, Color(0xFFFFD54A).copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = Color(0xFFFFD54A),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.debug_test_mode_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFD54A),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.debug_test_mode_state_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD54A),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFFFD54A),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("debug_weather_state_dropdown"),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(realOptionLabel) },
                        onClick = {
                            expanded = false
                            onSelectState(null)
                        },
                        modifier = Modifier.testTag("debug_state_option_real"),
                    )
                    WeatherVisualState.entries.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state.displayNameEs) },
                            onClick = {
                                expanded = false
                                onSelectState(state)
                            },
                            modifier = Modifier.testTag("debug_state_option_${state.name.lowercase()}"),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Discreet floating indicator shown on the Main Screen while Debug Test Mode is active.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulationStatusBanner(
    simulatedState: WeatherVisualState?,
    onExitSimulation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG || simulatedState == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("debug_simulation_banner"),
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.32f),
        border = BorderStroke(0.5.dp, Color(0xFFFFD54A).copy(alpha = 0.55f)),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = Color(0xFFFFD54A),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.debug_simulating_banner, simulatedState.displayNameEs),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }

            TextButton(
                onClick = onExitSimulation,
                modifier = Modifier.testTag("debug_exit_simulation_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color(0xFFFFD54A),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.debug_exit_simulation),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFD54A),
                )
            }
        }
    }
}
