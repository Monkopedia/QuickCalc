/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("ktlint:standard:function-naming")

package com.monkopedia.quickcalc

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class QuickSettingsSettingsActivity : ComponentActivity() {

    private val settingsRepository by lazy { TileSettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blurSupported = isDialogBlurSupported(this)
        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState(initial = TileSettings())
            val coroutineScope = rememberCoroutineScope()
            var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }

            TileSettingsTheme(settings = settings) {
                QuickSettingsSettingsScreen(
                    settings = settings,
                    blurSupported = blurSupported,
                    statusMessage = statusMessage,
                    onRequestAddTileClick = {
                        requestAddTile { resultMessage ->
                            statusMessage = resultMessage
                        }
                    },
                    onThemeModeSelected = { mode ->
                        coroutineScope.launch { settingsRepository.setThemeMode(mode) }
                    },
                    onAccentColorSelected = { colorArgb ->
                        coroutineScope.launch { settingsRepository.setAccentColor(colorArgb) }
                    },
                    onDialogBackgroundModeSelected = { mode ->
                        coroutineScope.launch { settingsRepository.setDialogBackgroundMode(mode) }
                    },
                    onSizeModeSelected = { mode ->
                        coroutineScope.launch { settingsRepository.setSizeMode(mode) }
                    },
                    onStaticAlignmentSelected = { alignment ->
                        coroutineScope.launch { settingsRepository.setStaticAlignment(alignment) }
                    },
                    onStaticSizeFractionChanged = { fraction ->
                        coroutineScope.launch { settingsRepository.setStaticSizeFraction(fraction) }
                    },
                    onDialogInactivityTimeoutSecondsChanged = { seconds ->
                        coroutineScope.launch {
                            settingsRepository.setDialogInactivityTimeoutSeconds(seconds)
                        }
                    },
                    onRememberCalculatorStateChanged = { enabled ->
                        coroutineScope.launch {
                            settingsRepository.setRememberCalculatorState(enabled)
                        }
                    }
                )
            }
        }
    }

    private fun requestAddTile(onResult: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(getString(R.string.quick_settings_add_tile_requires_tiramisu))
            return
        }

        val statusBarManager = getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
            onResult(getString(R.string.quick_settings_add_tile_status_bar_unavailable))
            return
        }

        try {
            statusBarManager.requestAddTileService(
                ComponentName(this, CalculatorTileService::class.java),
                getString(R.string.quick_settings_tile_label),
                Icon.createWithResource(this, R.drawable.fly_calc),
                mainExecutor
            ) { result ->
                onResult(
                    when (result) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            getString(R.string.quick_settings_add_tile_result_added)

                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            getString(R.string.quick_settings_add_tile_result_already_added)

                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                            getString(R.string.quick_settings_add_tile_result_not_added)

                        else ->
                            getString(R.string.quick_settings_add_tile_result_other, result)
                    }
                )
            }
        } catch (_: SecurityException) {
            onResult(getString(R.string.quick_settings_add_tile_result_not_foreground))
        } catch (_: IllegalArgumentException) {
            onResult(getString(R.string.quick_settings_add_tile_result_bad_component))
        }
    }
}

@Composable
private fun QuickSettingsSettingsScreen(
    settings: TileSettings,
    blurSupported: Boolean,
    statusMessage: String?,
    onRequestAddTileClick: () -> Unit,
    onThemeModeSelected: (TileThemeMode) -> Unit,
    onAccentColorSelected: (Int) -> Unit,
    onDialogBackgroundModeSelected: (TileDialogBackgroundMode) -> Unit,
    onSizeModeSelected: (TileSizeMode) -> Unit,
    onStaticAlignmentSelected: (TileStaticAlignment) -> Unit,
    onStaticSizeFractionChanged: (Float) -> Unit,
    onDialogInactivityTimeoutSecondsChanged: (Int) -> Unit,
    onRememberCalculatorStateChanged: (Boolean) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.quick_settings_settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                    )
                }
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.35f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.quick_settings_settings_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FilledTonalButton(
                                onClick = onRequestAddTileClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.quick_settings_add_tile_action))
                            }
                            if (statusMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = statusMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.settings_section_theme)
                    ) {
                        ChoicePillRow(
                            options = TileThemeMode.entries,
                            selectedOption = settings.themeMode,
                            optionLabel = { modeLabel(it) },
                            onOptionSelected = onThemeModeSelected
                        )
                    }
                }
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.settings_section_accent_color)
                    ) {
                        AccentChooser(
                            selectedColor = settings.accentColorArgb,
                            darkTheme = isDialogDarkTheme(settings),
                            onColorSelected = onAccentColorSelected
                        )
                    }
                }
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.settings_section_dialog_background)
                    ) {
                        ChoicePillRow(
                            options = BASIC_DIALOG_BACKGROUND_OPTIONS,
                            selectedOption = settings.dialogBackgroundMode,
                            optionLabel = { dialogBackgroundLabel(it) },
                            onOptionSelected = onDialogBackgroundModeSelected
                        )
                        if (blurSupported) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ChoicePillRow(
                                options = BLUR_DIALOG_BACKGROUND_OPTIONS,
                                selectedOption = settings.dialogBackgroundMode,
                                optionLabel = { dialogBackgroundLabel(it) },
                                onOptionSelected = onDialogBackgroundModeSelected
                            )
                        }
                    }
                }
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.settings_section_size_mode)
                    ) {
                        ChoicePillRow(
                            options = TileSizeMode.entries,
                            selectedOption = settings.sizeMode,
                            optionLabel = { sizeModeLabel(it) },
                            onOptionSelected = onSizeModeSelected
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (settings.sizeMode == TileSizeMode.STATIC) {
                            Text(
                                text = stringResource(R.string.settings_static_alignment),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AlignmentChooser(
                                selected = settings.staticAlignment,
                                onSelected = onStaticAlignmentSelected
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_static_size_percent,
                                    (settings.staticSizeFraction * 100f).toInt()
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = settings.staticSizeFraction,
                                onValueChange = onStaticSizeFractionChanged,
                                valueRange = MIN_STATIC_SIZE_FRACTION..MAX_STATIC_SIZE_FRACTION,
                                colors = SliderDefaults.colors()
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                    alpha = 0.45f
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_dynamic_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 10.dp
                                    )
                                )
                            }
                        }
                    }
                }
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.settings_section_behavior)
                    ) {
                        val isAutoCloseEnabled =
                            settings.dialogInactivityTimeoutSeconds >
                                DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS
                        SwitchRow(
                            text = stringResource(R.string.settings_auto_close_on_inactivity),
                            checked = isAutoCloseEnabled,
                            onCheckedChange = { enabled ->
                                onDialogInactivityTimeoutSecondsChanged(
                                    if (enabled) {
                                        DEFAULT_DIALOG_INACTIVITY_TIMEOUT_SECONDS
                                    } else {
                                        DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS
                                    }
                                )
                            }
                        )
                        if (isAutoCloseEnabled) {
                            val timeoutRange =
                                MIN_DIALOG_INACTIVITY_TIMEOUT_SECONDS
                                    .toFloat()
                                    .rangeTo(MAX_DIALOG_INACTIVITY_TIMEOUT_SECONDS.toFloat())
                            val timeoutSteps =
                                MAX_DIALOG_INACTIVITY_TIMEOUT_SECONDS -
                                    MIN_DIALOG_INACTIVITY_TIMEOUT_SECONDS - 1
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_auto_close_timeout_seconds,
                                    settings.dialogInactivityTimeoutSeconds
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = settings.dialogInactivityTimeoutSeconds.toFloat(),
                                onValueChange = { value ->
                                    onDialogInactivityTimeoutSecondsChanged(value.roundToInt())
                                },
                                valueRange = timeoutRange,
                                steps = timeoutSteps,
                                colors = SliderDefaults.colors()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SwitchRow(
                            text = stringResource(R.string.settings_remember_last_values),
                            checked = settings.rememberCalculatorState,
                            onCheckedChange = onRememberCalculatorStateChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun <T> ChoicePillRow(
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOptionSelected(option) }
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AccentChooser(selectedColor: Int, darkTheme: Boolean, onColorSelected: (Int) -> Unit) {
    val systemAccentColor = resolveTileAccentColor(SYSTEM_ACCENT_COLOR_ARGB, darkTheme)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TILE_ACCENT_OPTIONS.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { option ->
                    val selected = option.colorArgb == selectedColor
                    val swatchColor =
                        if (option.colorArgb == SYSTEM_ACCENT_COLOR_ARGB) {
                            systemAccentColor
                        } else {
                            Color(option.colorArgb)
                        }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onColorSelected(option.colorArgb) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    },
                                    shape = CircleShape
                                )
                                .padding(5.dp)
                                .background(
                                    swatchColor,
                                    shape = CircleShape
                                )
                        ) {
                            if (selected) {
                                Text(
                                    text = "\u2713",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(option.labelResId),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlignmentChooser(
    selected: TileStaticAlignment,
    onSelected: (TileStaticAlignment) -> Unit
) {
    val rows = listOf(
        listOf(
            TileStaticAlignment.TOP_START,
            TileStaticAlignment.TOP_CENTER,
            TileStaticAlignment.TOP_END
        ),
        listOf(
            TileStaticAlignment.CENTER_START,
            TileStaticAlignment.CENTER,
            TileStaticAlignment.CENTER_END
        ),
        listOf(
            TileStaticAlignment.BOTTOM_START,
            TileStaticAlignment.BOTTOM_CENTER,
            TileStaticAlignment.BOTTOM_END
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { alignment ->
                    val isSelected = alignment == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelected(alignment) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(alignmentMarkerAlignment(alignment))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun alignmentMarkerAlignment(alignment: TileStaticAlignment): Alignment = when (alignment) {
    TileStaticAlignment.TOP_START -> Alignment.TopStart
    TileStaticAlignment.TOP_CENTER -> Alignment.TopCenter
    TileStaticAlignment.TOP_END -> Alignment.TopEnd
    TileStaticAlignment.CENTER_START -> Alignment.CenterStart
    TileStaticAlignment.CENTER -> Alignment.Center
    TileStaticAlignment.CENTER_END -> Alignment.CenterEnd
    TileStaticAlignment.BOTTOM_START -> Alignment.BottomStart
    TileStaticAlignment.BOTTOM_CENTER -> Alignment.BottomCenter
    TileStaticAlignment.BOTTOM_END -> Alignment.BottomEnd
}

@Composable
private fun modeLabel(mode: TileThemeMode): String = when (mode) {
    TileThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    TileThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    TileThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun sizeModeLabel(mode: TileSizeMode): String = when (mode) {
    TileSizeMode.STATIC -> stringResource(R.string.settings_size_static)
    TileSizeMode.DYNAMIC -> stringResource(R.string.settings_size_dynamic)
}

private val BASIC_DIALOG_BACKGROUND_OPTIONS = listOf(
    TileDialogBackgroundMode.CLEAR,
    TileDialogBackgroundMode.LIGHT,
    TileDialogBackgroundMode.DARK
)

private val BLUR_DIALOG_BACKGROUND_OPTIONS = listOf(
    TileDialogBackgroundMode.BLUR_SUBTLE,
    TileDialogBackgroundMode.BLUR_LIGHT,
    TileDialogBackgroundMode.BLUR_HEAVY
)

@Composable
private fun dialogBackgroundLabel(mode: TileDialogBackgroundMode): String = when (mode) {
    TileDialogBackgroundMode.CLEAR -> stringResource(R.string.settings_bg_clear)
    TileDialogBackgroundMode.LIGHT -> stringResource(R.string.settings_bg_light)
    TileDialogBackgroundMode.DARK -> stringResource(R.string.settings_bg_dark)
    TileDialogBackgroundMode.BLUR_SUBTLE -> stringResource(R.string.settings_bg_blur_light)
    TileDialogBackgroundMode.BLUR_LIGHT -> stringResource(R.string.settings_bg_blur_medium)
    TileDialogBackgroundMode.BLUR_HEAVY -> stringResource(R.string.settings_bg_blur_heavy)
    TileDialogBackgroundMode.BLUR_DEBUG -> stringResource(R.string.settings_bg_blur_heavy)
}
