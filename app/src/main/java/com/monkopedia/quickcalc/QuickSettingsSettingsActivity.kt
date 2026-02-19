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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class QuickSettingsSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
            QuickSettingsSettingsScreen(
                statusMessage = statusMessage,
                onRequestAddTileClick = {
                    requestAddTile { resultMessage ->
                        statusMessage = resultMessage
                    }
                }
            )
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
                Icon.createWithResource(this, R.mipmap.ic_launcher_calculator),
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
private fun QuickSettingsSettingsScreen(statusMessage: String?, onRequestAddTileClick: () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.quick_settings_settings_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.quick_settings_settings_description),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onRequestAddTileClick,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.quick_settings_add_tile_action))
                }
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
