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

@file:Suppress("ktlint:standard:function-naming", "DEPRECATION")

package com.monkopedia.quickcalc

import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalculatorComposeRoute(
    modifier: Modifier = Modifier,
    initialFormula: String = "",
    evaluateInitialExpression: Boolean = false,
    initialPadPage: Int = 0,
    initialUiState: CalculatorUiState? = null,
    colorPalette: CalculatorColorPalette? = null,
    enableDisplayClipboardGestures: Boolean = false,
    showDrawerShortcutButton: Boolean = false,
    padPageOverride: Int? = null,
    onPadPageOverrideConsumed: (() -> Unit)? = null,
    onRequestPadPage: ((Int) -> Unit)? = null,
    onCalculatorStateChange: ((CalculatorUiState) -> Unit)? = null
) {
    val context = LocalContext.current
    val reducer = remember(context) {
        CalculatorUiReducer(context.applicationContext)
    }
    var uiState by rememberSaveable(
        initialFormula,
        evaluateInitialExpression,
        initialUiState,
        stateSaver = calculatorUiStateSaver()
    ) {
        mutableStateOf(
            initialUiState ?: reducer.initialState(
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
        colorPalette = colorPalette,
        enableDisplayClipboardGestures = enableDisplayClipboardGestures,
        showDrawerShortcutButton = showDrawerShortcutButton,
        padPageOverride = padPageOverride,
        onPadPageOverrideConsumed = onPadPageOverrideConsumed,
        onRequestPadPage = onRequestPadPage,
        onCalculatorStateChange = onCalculatorStateChange,
        modifier = modifier
    )
}

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    initialPadPage: Int = 0,
    colorPalette: CalculatorColorPalette? = null,
    enableDisplayClipboardGestures: Boolean = false,
    showDrawerShortcutButton: Boolean = false,
    padPageOverride: Int? = null,
    onPadPageOverrideConsumed: (() -> Unit)? = null,
    onRequestPadPage: ((Int) -> Unit)? = null,
    onCalculatorStateChange: ((CalculatorUiState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val defaultAccentColor = colorResource(R.color.calculator_accent_color)
    val defaultErrorColor = colorResource(R.color.calculator_error_color)
    val defaultPalette = CalculatorColorPalette(
        accentColor = defaultAccentColor,
        errorColor = defaultErrorColor,
        displayBackgroundColor = colorResource(R.color.display_background_color),
        formulaColor = colorResource(R.color.display_formula_text_color),
        resultColor = colorResource(R.color.display_result_text_color),
        numericPadBackgroundColor = colorResource(R.color.pad_numeric_background_color),
        operatorPadBackgroundColor = colorResource(R.color.pad_operator_background_color),
        advancedPadBackgroundColor = colorResource(R.color.pad_advanced_background_color)
    )
    val palette = colorPalette ?: defaultPalette

    ApplyLegacyStatusBarColor(
        hasError = state.hasError,
        accentColor = palette.accentColor,
        errorColor = palette.errorColor
    )
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }

    val configuration = LocalConfiguration.current
    val layoutSpec = remember(configuration) {
        resolveLayoutSpec(configuration)
    }

    val displayBackground = palette.displayBackgroundColor
    val numericPadBackground = palette.numericPadBackgroundColor
    val operatorPadBackground = palette.operatorPadBackgroundColor
    val advancedPadBackground = palette.advancedPadBackgroundColor
    val accentColor = palette.accentColor
    val errorColor = palette.errorColor
    val formulaColor =
        if (state.hasError) {
            errorColor
        } else {
            palette.formulaColor
        }
    val resultColor =
        if (state.hasError) {
            errorColor
        } else {
            palette.resultColor
        }

    var previousState by remember { mutableStateOf(state) }
    var revealColor by remember { mutableStateOf<Color?>(null) }
    var isDrawerOpen by remember { mutableStateOf(initialPadPage > 0) }
    val revealProgress = remember { Animatable(0f) }
    val revealAlpha = remember { Animatable(0f) }
    var copyRevealTrigger by remember { mutableStateOf(0) }
    val copyRevealProgress = remember { Animatable(0f) }
    val copyRevealAlpha = remember { Animatable(0f) }
    val copyIndicatorAlpha = remember { Animatable(0f) }
    val copyRevealColor = remember(displayBackground) {
        if (displayBackground.luminance() > 0.5f) {
            Color(0xFFF9A825)
        } else {
            Color(0xFFFFD54F)
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state) {
        val previous = previousState
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

        previousState = state
    }

    LaunchedEffect(copyRevealTrigger) {
        if (copyRevealTrigger == 0) {
            return@LaunchedEffect
        }
        copyRevealProgress.snapTo(0f)
        copyRevealAlpha.snapTo(1f)
        copyIndicatorAlpha.snapTo(0f)
        launch {
            copyIndicatorAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            )
        }
        // Keep copy ripple timing aligned with clear ripple behavior.
        copyRevealProgress.animateTo(1f, tween(durationMillis = 360))
        copyRevealAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220)
        )
        // Start icon fade earlier but make it longer, keeping the same final endpoint.
        delay(300)
        copyIndicatorAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(state) {
        onCalculatorStateChange?.invoke(state)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                handleHardwareKeyEvent(
                    event = event,
                    state = state,
                    onEvent = onEvent,
                    clipboardManager = clipboardManager
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DisplayPanel(
                state = state,
                style = layoutSpec.display,
                backgroundColor = displayBackground,
                formulaColor = formulaColor,
                resultColor = resultColor,
                animateFormulaAutosize = state.phase == CalculatorUiPhase.INPUT,
                showDrawerShortcutButton = showDrawerShortcutButton &&
                    layoutSpec.mode == ComposeLayoutMode.PHONE_PORTRAIT_PAGER,
                isDrawerOpen = isDrawerOpen,
                onDrawerShortcutClick = {
                    onRequestPadPage?.invoke(if (isDrawerOpen) 0 else 1)
                },
                onDisplayClick = if (enableDisplayClipboardGestures) {
                    {
                        copyExpressionToClipboard(state, clipboardManager)
                        copyRevealTrigger += 1
                    }
                } else {
                    null
                },
                onDisplayLongClick = if (enableDisplayClipboardGestures) {
                    {
                        pasteClipboardExpression(clipboardManager, onEvent)
                    }
                } else {
                    null
                },
                showCopiedIndicator = copyIndicatorAlpha.value > 0.01f,
                clearRevealColor = revealColor,
                clearRevealProgress = revealProgress.value,
                clearRevealAlpha = revealAlpha.value,
                copyRevealColor = copyRevealColor,
                copyRevealProgress = copyRevealProgress.value,
                copyRevealAlpha = copyRevealAlpha.value,
                copyIndicatorAlpha = copyIndicatorAlpha.value
            )

            when (layoutSpec.mode) {
                ComposeLayoutMode.PHONE_PORTRAIT_PAGER -> {
                    PhonePortraitPagerPad(
                        state = state,
                        onEvent = onEvent,
                        initialPadPage = initialPadPage,
                        padPageOverride = padPageOverride,
                        onPadPageOverrideConsumed = onPadPageOverrideConsumed,
                        onDrawerOpenChanged = { isOpen -> isDrawerOpen = isOpen },
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
    }
}

private fun handleHardwareKeyEvent(
    event: KeyEvent,
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    clipboardManager: ClipboardManager
): Boolean {
    if (event.type != KeyEventType.KeyUp) {
        return false
    }
    val nativeEvent = event.nativeKeyEvent

    if (nativeEvent.isCtrlPressed) {
        when (nativeEvent.keyCode) {
            AndroidKeyEvent.KEYCODE_V,
            AndroidKeyEvent.KEYCODE_PASTE -> {
                pasteClipboardExpression(clipboardManager, onEvent)
                return true
            }

            AndroidKeyEvent.KEYCODE_C,
            AndroidKeyEvent.KEYCODE_COPY -> {
                copyExpressionToClipboard(state, clipboardManager)
                return true
            }
        }
    }

    when (nativeEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_ENTER,
        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
            onEvent(CalculatorUiEvent.Equals)
            return true
        }

        AndroidKeyEvent.KEYCODE_DEL,
        AndroidKeyEvent.KEYCODE_FORWARD_DEL -> {
            onEvent(CalculatorUiEvent.Delete)
            return true
        }
    }

    val typedChar = nativeEvent.unicodeChar.takeIf { it != 0 }?.toChar() ?: return false
    if (typedChar == '=') {
        onEvent(CalculatorUiEvent.Equals)
        return true
    }
    if (!typedChar.isCalculatorInputChar()) {
        return false
    }

    onEvent(CalculatorUiEvent.Append(typedChar.normalizedCalculatorToken()))
    return true
}

private fun pasteClipboardExpression(
    clipboardManager: ClipboardManager,
    onEvent: (CalculatorUiEvent) -> Unit
) {
    val clipboardText = clipboardManager.getText()?.text.orEmpty()
    clipboardText.forEach { character ->
        when {
            character == '=' -> onEvent(CalculatorUiEvent.Equals)
            character.isCalculatorInputChar() -> {
                onEvent(CalculatorUiEvent.Append(character.normalizedCalculatorToken()))
            }
        }
    }
}

private fun copyExpressionToClipboard(
    state: CalculatorUiState,
    clipboardManager: ClipboardManager
) {
    val expressionToCopy = state.formulaText.ifEmpty { state.resultText }
    if (expressionToCopy.isNotEmpty()) {
        clipboardManager.setText(AnnotatedString(expressionToCopy))
    }
}

private fun Char.isCalculatorInputChar(): Boolean = isDigit() ||
    isLetter() ||
    this in "+-*/().,%!^" ||
    this in setOf('×', '÷', '−', '–', '—')

private fun Char.normalizedCalculatorToken(): String = when (this) {
    '×', 'x', 'X' -> "*"
    '÷' -> "/"
    '−', '–', '—' -> "-"
    else -> toString()
}

@Composable
private fun ApplyLegacyStatusBarColor(hasError: Boolean, accentColor: Color, errorColor: Color) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
    val statusBarColor = if (hasError) errorColor else accentColor
    SideEffect {
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = statusBarColor.toArgb()
    }
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
