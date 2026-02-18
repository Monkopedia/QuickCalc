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

package com.android.calculator2

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
internal fun CalculatorPadButton(
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    textSize: TextUnit,
    textColorRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val clickModifier = if (!enabled) {
        modifier
    } else if (onLongClick == null) {
        modifier.combinedClickable(onClick = onClick)
    } else {
        modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }

    Box(
        modifier = clickModifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = colorResource(textColorRes),
            fontSize = textSize,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
    }
}

internal data class PadButtonSpec(
    val label: String,
    val tag: String,
    val contentDescription: String = label,
    val event: CalculatorUiEvent? = null
)
