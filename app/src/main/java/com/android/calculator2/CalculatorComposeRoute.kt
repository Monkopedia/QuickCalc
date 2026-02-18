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

package com.android.calculator2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@Composable
fun CalculatorComposeRoute(
    modifier: Modifier = Modifier,
    initialFormula: String = "",
    evaluateInitialExpression: Boolean = false,
    initialPadPage: Int = 0
) {
    val context = LocalContext.current
    val reducer = remember(context) {
        CalculatorUiReducer(context.applicationContext)
    }
    var uiState by rememberSaveable(
        initialFormula,
        evaluateInitialExpression,
        stateSaver = calculatorUiStateSaver()
    ) {
        mutableStateOf(
            reducer.initialState(
                initialFormula = initialFormula,
                evaluateAsResult = evaluateInitialExpression
            )
        )
    }

    CalculatorScreen(
        state = uiState,
        onEvent = { event ->
            uiState = reducer.reduce(uiState, event)
        },
        initialPadPage = initialPadPage,
        modifier = modifier
    )
}

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    initialPadPage: Int = 0,
    modifier: Modifier = Modifier
) {
    ApplyLegacyStatusBarColor(state)

    val configuration = LocalConfiguration.current
    val layoutSpec = remember(configuration) {
        resolveLayoutSpec(configuration)
    }

    val displayBackground = colorResource(R.color.display_background_color)
    val numericPadBackground = colorResource(R.color.pad_numeric_background_color)
    val operatorPadBackground = colorResource(R.color.pad_operator_background_color)
    val advancedPadBackground = colorResource(R.color.pad_advanced_background_color)
    val accentColor = colorResource(R.color.calculator_accent_color)
    val errorColor = colorResource(R.color.calculator_error_color)
    val formulaColor =
        if (state.hasError) {
            errorColor
        } else {
            colorResource(R.color.display_formula_text_color)
        }
    val resultColor =
        if (state.hasError) {
            errorColor
        } else {
            colorResource(R.color.display_result_text_color)
        }

    var previousState by remember { mutableStateOf(state) }
    var displayBounds by remember { mutableStateOf<Rect?>(null) }
    var revealColor by remember { mutableStateOf<Color?>(null) }
    val revealProgress = remember { Animatable(0f) }
    val revealAlpha = remember { Animatable(0f) }

    LaunchedEffect(state) {
        val previous = previousState
        val bounds = displayBounds
        if (bounds != null) {
            val isErrorTransition =
                state.phase == CalculatorUiPhase.ERROR &&
                    previous.phase != CalculatorUiPhase.ERROR
            val isClearTransition =
                state.formulaText.isEmpty() &&
                    previous.formulaText.isNotEmpty() &&
                    state.phase != CalculatorUiPhase.ERROR

            if (isErrorTransition || isClearTransition) {
                revealColor = if (isErrorTransition) errorColor else accentColor
                revealProgress.snapTo(0f)
                revealAlpha.snapTo(1f)
                revealProgress.animateTo(1f, tween(durationMillis = 360))
                revealAlpha.animateTo(0f, tween(durationMillis = 220))
                revealColor = null
            }
        }

        previousState = state
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DisplayPanel(
                state = state,
                style = layoutSpec.display,
                backgroundColor = displayBackground,
                formulaColor = formulaColor,
                resultColor = resultColor,
                animateFormulaAutosize = state.phase == CalculatorUiPhase.INPUT,
                onBoundsChanged = { bounds ->
                    displayBounds = bounds
                }
            )

            when (layoutSpec.mode) {
                ComposeLayoutMode.PHONE_PORTRAIT_PAGER -> {
                    PhonePortraitPagerPad(
                        state = state,
                        onEvent = onEvent,
                        initialPadPage = initialPadPage,
                        style = layoutSpec,
                        numericPadBackground = numericPadBackground,
                        operatorPadBackground = operatorPadBackground,
                        advancedPadBackground = advancedPadBackground
                    )
                }

                ComposeLayoutMode.LANDSCAPE_SPLIT -> {
                    LandscapeSplitPad(
                        state = state,
                        onEvent = onEvent,
                        style = layoutSpec,
                        numericPadBackground = numericPadBackground,
                        operatorPadBackground = operatorPadBackground,
                        advancedPadBackground = advancedPadBackground
                    )
                }

                ComposeLayoutMode.TABLET_PORTRAIT_SPLIT -> {
                    TabletPortraitSplitPad(
                        state = state,
                        onEvent = onEvent,
                        style = layoutSpec,
                        numericPadBackground = numericPadBackground,
                        operatorPadBackground = operatorPadBackground,
                        advancedPadBackground = advancedPadBackground
                    )
                }
            }
        }

        val bounds = displayBounds
        val activeRevealColor = revealColor
        if (bounds != null && activeRevealColor != null && revealAlpha.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val revealCenter = Offset(
                    x = bounds.right - 24.dp.toPx(),
                    y = bounds.bottom + 24.dp.toPx()
                )
                val maxRadius = maxDistanceToBounds(
                    source = revealCenter,
                    left = bounds.left,
                    top = 0f,
                    right = bounds.right,
                    bottom = bounds.bottom
                )
                clipRect(
                    left = bounds.left,
                    top = 0f,
                    right = bounds.right,
                    bottom = bounds.bottom
                ) {
                    drawCircle(
                        color = activeRevealColor.copy(alpha = revealAlpha.value),
                        radius = maxRadius * revealProgress.value,
                        center = revealCenter
                    )
                }
            }
        }
    }
}

private fun maxDistanceToBounds(
    source: Offset,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
): Float {
    val topLeft = distance(source, Offset(left, top))
    val topRight = distance(source, Offset(right, top))
    val bottomLeft = distance(source, Offset(left, bottom))
    val bottomRight = distance(source, Offset(right, bottom))
    return maxOf(topLeft, topRight, bottomLeft, bottomRight)
}

private fun distance(first: Offset, second: Offset): Float =
    hypot(first.x - second.x, first.y - second.y)

@Composable
private fun ApplyLegacyStatusBarColor(state: CalculatorUiState) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity ?: return
    val statusBarColor = colorResource(
        if (state.hasError) {
            R.color.calculator_error_color
        } else {
            R.color.calculator_accent_color
        }
    )
    SideEffect {
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = statusBarColor.toArgb()
    }
}

private fun calculatorUiStateSaver(): Saver<CalculatorUiState, Any> = mapSaver(
    save = { state ->
        mapOf(
            KEY_FORMULA to state.formulaText,
            KEY_RESULT to state.resultText,
            KEY_PHASE to state.phase.name
        )
    },
    restore = { saved ->
        CalculatorUiState(
            formulaText = saved[KEY_FORMULA] as String,
            resultText = saved[KEY_RESULT] as String,
            phase = CalculatorUiPhase.valueOf(saved[KEY_PHASE] as String)
        )
    }
)

private const val KEY_FORMULA = "formula"
private const val KEY_RESULT = "result"
private const val KEY_PHASE = "phase"
