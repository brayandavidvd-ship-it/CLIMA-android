package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.ConnectionTestResult
import com.example.model.CitySearchResult
import com.example.model.GeminiModelOption
import com.example.model.WeatherProviderOption
import com.example.model.WeatherVisualState
import com.example.ui.components.CitySearchBottomSheet
import com.example.ui.components.DebugTestModeSection
import com.example.ui.state.ClimaUiState
import com.example.ui.theme.SettingsCardDark
import com.example.ui.theme.SettingsSurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: ClimaUiState,
    hasLocationPermission: () -> Boolean,
    onNavigateBack: () -> Unit,
    onOpenCitySearch: () -> Unit,
    onCloseCitySearch: () -> Unit,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (CitySearchResult) -> Unit,
    onUseAutoLocation: () -> Unit,
    onSetAiEnabled: (Boolean, Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onTestConnection: (String) -> Unit,
    onSelectWeatherProvider: (WeatherProviderOption) -> Unit,
    onSetReduceAnimations: (Boolean) -> Unit,
    onDebugSelectState: (WeatherVisualState?) -> Unit,
) {
    var apiKeyDraft by rememberSaveable(uiState.settings.apiKey) {
        mutableStateOf(uiState.settings.apiKey)
    }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                onUseAutoLocation()
            }
        },
    )

    Scaffold(
        containerColor = SettingsSurfaceDark,
        contentColor = Color.White,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsSurfaceDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Ciudad
                SettingsCard {
                    Text(
                        text = stringResource(R.string.settings_city_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.activeSnapshot?.cityName ?: stringResource(R.string.your_location_fallback),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenCitySearch,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_change_city_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_change_city),
                                color = Color.White,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (hasLocationPermission()) {
                                    onUseAutoLocation()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_auto_location_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_auto_location),
                                color = Color.White,
                            )
                        }
                    }
                }

                // 1b. Proveedor de Clima
                WeatherProviderSelectorCard(
                    selectedProvider = uiState.settings.selectedWeatherProvider,
                    onSelectProvider = onSelectWeatherProvider,
                )

                // 2. IA en la nube (Gemini)
                SettingsCard {
                    Text(
                        text = stringResource(R.string.settings_ai_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_ai_enable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.settings.aiEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !uiState.settings.aiConsentAccepted) {
                                    showConsentDialog = true
                                } else {
                                    onSetAiEnabled(checked, uiState.settings.aiConsentAccepted)
                                }
                            },
                            modifier = Modifier.testTag("settings_ai_switch"),
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_ai_privacy_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = {
                            apiKeyDraft = it
                            onSaveApiKey(it)
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_api_key_label)) },
                        placeholder = { Text(stringResource(R.string.settings_api_key_hint)) },
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showApiKey = !showApiKey },
                                modifier = Modifier.testTag("toggle_api_key_visibility"),
                            ) {
                                Icon(
                                    imageVector = if (showApiKey) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = stringResource(R.string.settings_toggle_key_visibility),
                                    tint = Color.White.copy(alpha = 0.8f),
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_key_input"),
                    )

                    AnimatedVisibility(visible = apiKeyDraft.isBlank()) {
                        TextButton(
                            onClick = {
                                runCatching {
                                    uriHandler.openUri("https://aistudio.google.com/app/apikey")
                                }
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("create_api_key_link"),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_create_api_key_link),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFD54A),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.settings_model_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    GeminiModelOption.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectModel(option) }
                                .padding(vertical = 4.dp)
                                .testTag("model_option_${option.modelId}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = uiState.settings.selectedModel == option,
                                onClick = { onSelectModel(option) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.White,
                                    unselectedColor = Color.White.copy(alpha = 0.6f),
                                ),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onTestConnection(apiKeyDraft) },
                        enabled = uiState.connectionTestResult !is ConnectionTestResult.Testing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = SettingsSurfaceDark,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("test_connection_button"),
                    ) {
                        if (uiState.connectionTestResult is ConnectionTestResult.Testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SettingsSurfaceDark,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_testing_connection))
                        } else {
                            Text(stringResource(R.string.settings_test_connection))
                        }
                    }

                    when (val testState = uiState.connectionTestResult) {
                        is ConnectionTestResult.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = testState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AE6B4),
                                modifier = Modifier.testTag("connection_test_success"),
                            )
                        }
                        is ConnectionTestResult.Failure -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = testState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.testTag("connection_test_failure"),
                            )
                        }
                        else -> Unit
                    }
                }

                // 3. Accesibilidad
                SettingsCard {
                    Text(
                        text = stringResource(R.string.settings_accessibility_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.settings_reduce_animations),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(R.string.settings_reduce_animations_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                            )
                        }
                        Switch(
                            checked = uiState.settings.reduceAnimations,
                            onCheckedChange = onSetReduceAnimations,
                            modifier = Modifier.testTag("settings_reduce_animations_switch"),
                        )
                    }
                }

                // 4. Información de la app
                SettingsCard {
                    Text(
                        text = stringResource(R.string.settings_info_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_app_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                }

                // 5. Debug-Only Test Mode Section
                if (BuildConfig.DEBUG) {
                    DebugTestModeSection(
                        selectedSimulatedState = uiState.debugSimulatedState,
                        onSelectState = onDebugSelectState,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            containerColor = SettingsCardDark,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.88f),
            title = { Text(stringResource(R.string.settings_ai_section)) },
            text = { Text(stringResource(R.string.settings_ai_privacy_notice)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConsentDialog = false
                        onSetAiEnabled(true, true)
                    },
                    modifier = Modifier.testTag("confirm_ai_consent_button"),
                ) {
                    Text("Aceptar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConsentDialog = false },
                ) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.75f))
                }
            },
        )
    }

    if (uiState.isCitySearchOpen) {
        CitySearchBottomSheet(
            query = uiState.citySearchQuery,
            results = uiState.citySearchResults,
            isSearching = uiState.isSearchingCities,
            onQueryChange = onCityQueryChange,
            onCitySelected = onCitySelected,
            onUseCurrentLocation = {
                if (hasLocationPermission()) {
                    onUseAutoLocation()
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onDismiss = onCloseCitySearch,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherProviderSelectorCard(
    selectedProvider: WeatherProviderOption,
    onSelectProvider: (WeatherProviderOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SettingsCard {
        Text(
            text = stringResource(R.string.settings_provider_section),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(10.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedProvider.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_provider_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .testTag("settings_weather_provider_dropdown"),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                WeatherProviderOption.entries.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.displayName) },
                        onClick = {
                            expanded = false
                            onSelectProvider(provider)
                        },
                        modifier = Modifier.testTag("weather_provider_option_${provider.id}"),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_provider_extensible_note),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SettingsCardDark,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
