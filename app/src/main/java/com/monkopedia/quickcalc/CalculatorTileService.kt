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

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp

@RequiresApi(Build.VERSION_CODES.N)
class CalculatorTileService : TileService() {

    companion object {
        private const val TAG = "CalculatorTileService"
    }

    private var activeDialog: ComponentDialog? = null

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.quick_settings_tile_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (activeDialog?.isShowing == true) {
            return
        }
        CalculatorTilePriorityService.start(this)
        runCatching {
            showCalculatorDialog()
        }.onFailure { throwable ->
            Log.w(TAG, "Unable to show Quick Settings calculator dialog", throwable)
            CalculatorTilePriorityService.stop(this)
        }
    }

    private fun showCalculatorDialog() {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                QuickSettingsCalculatorDialogContent()
            }
        }

        val dialog = ComponentDialog(this, R.style.CalculatorTileDialogTheme).apply {
            setContentView(
                composeView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            window?.decorView?.setPadding(0, 0, 0, 0)
            setOnDismissListener {
                if (activeDialog === this) {
                    activeDialog = null
                }
                CalculatorTilePriorityService.stop(this@CalculatorTileService)
            }
        }
        activeDialog = dialog
        showDialog(dialog)
    }

    @Composable
    private fun QuickSettingsCalculatorDialogContent() {
        MaterialTheme {
            Surface(shape = RoundedCornerShape(28.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 420.dp, max = 620.dp)
                ) {
                    CalculatorComposeRoute(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
