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

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.launch

@Composable
fun CalculatorComposeRoute(
    modifier: Modifier = Modifier,
    initialFormula: String = "",
    evaluateInitialExpression: Boolean = false
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
        modifier = modifier
    )
}

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
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
    val resultScale = remember { Animatable(1f) }

    LaunchedEffect(state) {
        val bounds = displayBounds
        if (bounds != null) {
            val isErrorTransition =
                state.phase == CalculatorUiPhase.ERROR &&
                    previousState.phase != CalculatorUiPhase.ERROR
            val isClearTransition =
                state.formulaText.isEmpty() &&
                    previousState.formulaText.isNotEmpty() &&
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

        val isResultTransition =
            state.phase == CalculatorUiPhase.RESULT &&
                previousState.phase != CalculatorUiPhase.RESULT
        if (isResultTransition) {
            resultScale.snapTo(1.12f)
            resultScale.animateTo(1f, tween(durationMillis = 250))
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
                resultScale = resultScale.value,
                onBoundsChanged = { bounds ->
                    displayBounds = bounds
                }
            )

            when (layoutSpec.mode) {
                ComposeLayoutMode.PHONE_PORTRAIT_PAGER -> {
                    PhonePortraitPagerPad(
                        state = state,
                        onEvent = onEvent,
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

@Composable
private fun DisplayPanel(
    state: CalculatorUiState,
    style: DisplayStyleSpec,
    backgroundColor: Color,
    formulaColor: Color,
    resultColor: Color,
    resultScale: Float,
    onBoundsChanged: (Rect) -> Unit
) {
    val resultInsets = legacyTrimmedInsets(style.resultInsets, style.resultSizeSp.sp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(coordinates.boundsInRoot())
            }
            .testTag(TEST_TAG_DISPLAY),
        horizontalAlignment = Alignment.End
    ) {
        AutoSizeFormulaText(
            text = state.formulaText.ifEmpty { " " },
            style = style,
            color = formulaColor,
            tag = TEST_TAG_FORMULA,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = state.resultText.ifEmpty { " " },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = resultInsets.top,
                    bottom = resultInsets.bottom,
                    start = resultInsets.start,
                    end = resultInsets.end
                )
                .graphicsLayer {
                    scaleX = resultScale
                    scaleY = resultScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                }
                .testTag(TEST_TAG_RESULT),
            color = resultColor,
            style = legacyDisplayTextStyle(style.resultSizeSp.sp),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
private fun AutoSizeFormulaText(
    text: String,
    style: DisplayStyleSpec,
    color: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    val maxFormulaSizeSp = style.formulaMaxSizeSp.sp
    val baseFormulaInsets = legacyTrimmedInsets(style.formulaInsets, maxFormulaSizeSp)
    BoxWithConstraints(
        modifier = modifier.padding(
            start = baseFormulaInsets.start,
            end = baseFormulaInsets.end
        )
    ) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(0f) }
        val targetTextSize = remember(
            text,
            availableWidthPx,
            style.formulaMinSizeSp,
            style.formulaMaxSizeSp,
            style.formulaStepSizeSp
        ) {
            computeFittingTextSizeSp(
                text = text,
                minSizeSp = style.formulaMinSizeSp,
                maxSizeSp = style.formulaMaxSizeSp,
                stepSizeSp = style.formulaStepSizeSp,
                maxWidthPx = availableWidthPx
            ) { candidateSp ->
                textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = legacyDisplayTextStyle(candidateSp.sp),
                    maxLines = 1
                ).size.width.toFloat()
            }
        }

        val animatedSize by animateFloatAsState(
            targetValue = targetTextSize,
            animationSpec = tween(durationMillis = 220),
            label = "formulaAutoSize"
        )
        val formulaInsets = legacyTrimmedInsets(style.formulaInsets, animatedSize.sp)

        Text(
            text = text,
            color = color,
            style = legacyDisplayTextStyle(animatedSize.sp),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = formulaInsets.top,
                    bottom = formulaInsets.bottom
                )
                .testTag(tag)
        )
    }
}

private fun computeFittingTextSizeSp(
    text: String,
    minSizeSp: Int,
    maxSizeSp: Int,
    stepSizeSp: Int,
    maxWidthPx: Float,
    measureWidthPx: (Float) -> Float
): Float {
    if (text.isBlank()) {
        return maxSizeSp.toFloat()
    }
    if (maxWidthPx <= 0f || minSizeSp >= maxSizeSp || stepSizeSp <= 0) {
        return maxSizeSp.toFloat()
    }

    var bestSize = minSizeSp.toFloat()
    var current = minSizeSp.toFloat()
    while (current <= maxSizeSp.toFloat()) {
        if (measureWidthPx(current) <= maxWidthPx) {
            bestSize = current
            current += stepSizeSp
        } else {
            break
        }
    }
    return bestSize.coerceIn(minSizeSp.toFloat(), maxSizeSp.toFloat())
}

@Composable
private fun PhonePortraitPagerPad(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    style: LayoutStyleSpec,
    numericPadBackground: Color,
    operatorPadBackground: Color,
    advancedPadBackground: Color
) {
    val padPageMargin = dimensionResource(R.dimen.pad_page_margin)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { PAD_PAGE_COUNT }
    )
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(TEST_TAG_PAD_PAGER),
        pageSpacing = padPageMargin,
        beyondViewportPageCount = 1
    ) { page ->
        val pagePosition = page - pagerState.currentPage - pagerState.currentPageOffsetFraction
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (pagePosition < 0f) {
                        translationX = size.width * -pagePosition
                        alpha = (1f + pagePosition).coerceAtLeast(0f)
                    } else {
                        translationX = 0f
                        alpha = 1f
                    }
                }
        ) {
            when (page) {
                0 -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NumericPad(
                            onEvent = onEvent,
                            enabled = isSettledOnPage(pagerState, 0),
                            gridStyle = style.numericGrid,
                            textSize = style.numericTextSize,
                            showEquals = true,
                            modifier = Modifier
                                .weight(style.numericWeight)
                                .fillMaxSize()
                                .background(numericPadBackground)
                        )
                        OperatorPadOneColumn(
                            state = state,
                            onEvent = onEvent,
                            enabled = isSettledOnPage(pagerState, 0),
                            gridStyle = style.operatorOneGrid,
                            operatorTextSize = style.operatorTextSize,
                            topLabelTextSize = style.operatorTopLabelTextSize,
                            modifier = Modifier
                                .weight(style.operatorWeight)
                                .fillMaxSize()
                                .background(operatorPadBackground)
                        )
                    }
                }

                1 -> {
                    AdvancedPad(
                        onEvent = onEvent,
                        enabled = isSettledOnPage(pagerState, 1),
                        gridStyle = style.advancedGrid,
                        textSize = style.advancedTextSize,
                        columns = style.advancedColumns,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ADVANCED_PAGE_WIDTH_FRACTION)
                            .background(advancedPadBackground)
                            .testTag(TEST_TAG_ADVANCED_PAD)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeSplitPad(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    style: LayoutStyleSpec,
    numericPadBackground: Color,
    operatorPadBackground: Color,
    advancedPadBackground: Color
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NumericPad(
            onEvent = onEvent,
            enabled = true,
            gridStyle = style.numericGrid,
            textSize = style.numericTextSize,
            showEquals = false,
            modifier = Modifier
                .weight(style.numericWeight)
                .fillMaxSize()
                .background(numericPadBackground)
        )
        OperatorPadTwoColumn(
            state = state,
            onEvent = onEvent,
            enabled = true,
            gridStyle = style.operatorTwoGrid,
            operatorTextSize = style.operatorTextSize,
            topLabelTextSize = style.operatorTopLabelTextSize,
            modifier = Modifier
                .weight(style.operatorWeight)
                .fillMaxSize()
                .background(operatorPadBackground)
        )
        AdvancedPad(
            onEvent = onEvent,
            enabled = true,
            gridStyle = style.advancedGrid,
            textSize = style.advancedTextSize,
            columns = style.advancedColumns,
            modifier = Modifier
                .weight(style.advancedWeight)
                .fillMaxSize()
                .background(advancedPadBackground)
                .testTag(TEST_TAG_ADVANCED_PAD)
        )
    }
}

@Composable
private fun TabletPortraitSplitPad(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    style: LayoutStyleSpec,
    numericPadBackground: Color,
    operatorPadBackground: Color,
    advancedPadBackground: Color
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AdvancedPad(
            onEvent = onEvent,
            enabled = true,
            gridStyle = style.advancedGrid,
            textSize = style.advancedTextSize,
            columns = style.advancedColumns,
            modifier = Modifier
                .weight(style.tabletPortraitAdvancedWeight)
                .fillMaxWidth()
                .background(advancedPadBackground)
                .testTag(TEST_TAG_ADVANCED_PAD)
        )

        Row(
            modifier = Modifier
                .weight(style.tabletPortraitBottomRowWeight)
                .fillMaxWidth()
        ) {
            NumericPad(
                onEvent = onEvent,
                enabled = true,
                gridStyle = style.numericGrid,
                textSize = style.numericTextSize,
                showEquals = false,
                modifier = Modifier
                    .weight(style.numericWeight)
                    .fillMaxSize()
                    .background(numericPadBackground)
            )
            OperatorPadTwoColumn(
                state = state,
                onEvent = onEvent,
                enabled = true,
                gridStyle = style.operatorTwoGrid,
                operatorTextSize = style.operatorTextSize,
                topLabelTextSize = style.operatorTopLabelTextSize,
                modifier = Modifier
                    .weight(style.operatorWeight)
                    .fillMaxSize()
                    .background(operatorPadBackground)
            )
        }
    }
}

private fun isSettledOnPage(pagerState: PagerState, page: Int): Boolean =
    pagerState.currentPage == page && abs(pagerState.currentPageOffsetFraction) < 0.001f

@Composable
private fun NumericPad(
    onEvent: (CalculatorUiEvent) -> Unit,
    enabled: Boolean,
    gridStyle: GridStyleSpec,
    textSize: TextUnit,
    showEquals: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val composeConfiguration = LocalConfiguration.current
    val symbols = remember(context, composeConfiguration) {
        val configuration = composeConfiguration
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        val digitLocale = if (context.resources.getBoolean(R.bool.use_localized_digits)) {
            locale
        } else {
            Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword("nu", "latn")
                .build()
        }
        DecimalFormatSymbols.getInstance(digitLocale)
    }

    val decimalSeparator = remember(symbols) { symbols.decimalSeparator.toString() }
    val digits = remember(symbols) {
        val zeroCodePoint = symbols.zeroDigit.code
        List(10) { digit ->
            (zeroCodePoint + digit).toChar().toString()
        }
    }
    val decimalDescription = stringResource(R.string.desc_dec_point)

    val lastSlot: PadButtonSpec? =
        if (showEquals) {
            PadButtonSpec(
                stringResource(R.string.eq),
                TEST_TAG_EQUALS,
                stringResource(R.string.desc_eq),
                CalculatorUiEvent.Equals
            )
        } else {
            null
        }

    val numericButtons = listOf(
        listOf(
            PadButtonSpec(digits[7], TEST_TAG_DIGIT_7),
            PadButtonSpec(digits[8], TEST_TAG_DIGIT_8),
            PadButtonSpec(digits[9], TEST_TAG_DIGIT_9)
        ),
        listOf(
            PadButtonSpec(digits[4], TEST_TAG_DIGIT_4),
            PadButtonSpec(digits[5], TEST_TAG_DIGIT_5),
            PadButtonSpec(digits[6], TEST_TAG_DIGIT_6)
        ),
        listOf(
            PadButtonSpec(digits[1], TEST_TAG_DIGIT_1),
            PadButtonSpec(digits[2], TEST_TAG_DIGIT_2),
            PadButtonSpec(digits[3], TEST_TAG_DIGIT_3)
        ),
        listOf(
            PadButtonSpec(decimalSeparator, TEST_TAG_DECIMAL, decimalDescription),
            PadButtonSpec(digits[0], TEST_TAG_DIGIT_0),
            lastSlot
        )
    )

    Column(
        modifier = modifier.padding(
            start = gridStyle.insets.start,
            end = gridStyle.insets.end,
            top = gridStyle.insets.top,
            bottom = gridStyle.insets.bottom
        ),
        verticalArrangement = Arrangement.spacedBy(gridStyle.rowGap)
    ) {
        numericButtons.forEach { rowButtons ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(gridStyle.columnGap)
            ) {
                rowButtons.forEach { spec ->
                    if (spec == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    } else {
                        CalculatorPadButton(
                            label = spec.label,
                            contentDescription = spec.contentDescription,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag(spec.tag),
                            textSize = textSize,
                            textColorRes = R.color.pad_button_text_color,
                            enabled = enabled,
                            onClick = {
                                onEvent(spec.event ?: CalculatorUiEvent.Append(spec.label))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorPadOneColumn(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    enabled: Boolean,
    gridStyle: GridStyleSpec,
    operatorTextSize: TextUnit,
    topLabelTextSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val deleteLabel = stringResource(R.string.del)
    val clearLabel = stringResource(R.string.clr)
    val divideSymbol = stringResource(R.string.op_div)
    val multiplySymbol = stringResource(R.string.op_mul)
    val subtractSymbol = stringResource(R.string.op_sub)
    val addSymbol = stringResource(R.string.op_add)

    val topLabel = if (state.showsClearButton) clearLabel else deleteLabel
    val topDescription =
        if (state.showsClearButton) {
            stringResource(R.string.desc_clr)
        } else {
            stringResource(R.string.desc_del)
        }

    Column(
        modifier = modifier.padding(
            start = gridStyle.insets.start,
            end = gridStyle.insets.end,
            top = gridStyle.insets.top,
            bottom = gridStyle.insets.bottom
        ),
        verticalArrangement = Arrangement.spacedBy(gridStyle.rowGap)
    ) {
        CalculatorPadButton(
            label = topLabel.uppercase(Locale.getDefault()),
            contentDescription = topDescription,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .testTag(if (state.showsClearButton) TEST_TAG_CLEAR else TEST_TAG_DELETE),
            textSize = topLabelTextSize,
            textColorRes = R.color.pad_button_text_color,
            enabled = enabled,
            onClick = {
                onEvent(
                    if (state.showsClearButton) {
                        CalculatorUiEvent.Clear
                    } else {
                        CalculatorUiEvent.Delete
                    }
                )
            },
            onLongClick = {
                if (!state.showsClearButton) {
                    onEvent(CalculatorUiEvent.Clear)
                }
            }
        )
        CalculatorPadButton(
            label = divideSymbol,
            contentDescription = stringResource(R.string.desc_op_div),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .testTag(TEST_TAG_DIVIDE),
            textSize = operatorTextSize,
            textColorRes = R.color.pad_button_text_color,
            enabled = enabled,
            onClick = { onEvent(CalculatorUiEvent.Append(divideSymbol)) }
        )
        CalculatorPadButton(
            label = multiplySymbol,
            contentDescription = stringResource(R.string.desc_op_mul),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .testTag(TEST_TAG_MULTIPLY),
            textSize = operatorTextSize,
            textColorRes = R.color.pad_button_text_color,
            enabled = enabled,
            onClick = { onEvent(CalculatorUiEvent.Append(multiplySymbol)) }
        )
        CalculatorPadButton(
            label = subtractSymbol,
            contentDescription = stringResource(R.string.desc_op_sub),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .testTag(TEST_TAG_SUBTRACT),
            textSize = operatorTextSize,
            textColorRes = R.color.pad_button_text_color,
            enabled = enabled,
            onClick = { onEvent(CalculatorUiEvent.Append(subtractSymbol)) }
        )
        CalculatorPadButton(
            label = addSymbol,
            contentDescription = stringResource(R.string.desc_op_add),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .testTag(TEST_TAG_ADD),
            textSize = operatorTextSize,
            textColorRes = R.color.pad_button_text_color,
            enabled = enabled,
            onClick = { onEvent(CalculatorUiEvent.Append(addSymbol)) }
        )
    }
}

@Composable
private fun OperatorPadTwoColumn(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    enabled: Boolean,
    gridStyle: GridStyleSpec,
    operatorTextSize: TextUnit,
    topLabelTextSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val deleteLabel = stringResource(R.string.del)
    val clearLabel = stringResource(R.string.clr)
    val divideSymbol = stringResource(R.string.op_div)
    val multiplySymbol = stringResource(R.string.op_mul)
    val subtractSymbol = stringResource(R.string.op_sub)
    val addSymbol = stringResource(R.string.op_add)

    val topLabel = if (state.showsClearButton) clearLabel else deleteLabel
    val topDescription =
        if (state.showsClearButton) {
            stringResource(R.string.desc_clr)
        } else {
            stringResource(R.string.desc_del)
        }

    val rows = listOf(
        listOf(
            PadButtonSpec(
                divideSymbol,
                TEST_TAG_DIVIDE,
                stringResource(R.string.desc_op_div),
                CalculatorUiEvent.Append(divideSymbol)
            ),
            PadButtonSpec(
                topLabel.uppercase(Locale.getDefault()),
                if (state.showsClearButton) TEST_TAG_CLEAR else TEST_TAG_DELETE,
                topDescription
            )
        ),
        listOf(
            PadButtonSpec(
                multiplySymbol,
                TEST_TAG_MULTIPLY,
                stringResource(R.string.desc_op_mul),
                CalculatorUiEvent.Append(multiplySymbol)
            ),
            null
        ),
        listOf(
            PadButtonSpec(
                subtractSymbol,
                TEST_TAG_SUBTRACT,
                stringResource(R.string.desc_op_sub),
                CalculatorUiEvent.Append(subtractSymbol)
            ),
            null
        ),
        listOf(
            PadButtonSpec(
                addSymbol,
                TEST_TAG_ADD,
                stringResource(R.string.desc_op_add),
                CalculatorUiEvent.Append(addSymbol)
            ),
            PadButtonSpec(
                stringResource(R.string.eq),
                TEST_TAG_EQUALS,
                stringResource(R.string.desc_eq),
                CalculatorUiEvent.Equals
            )
        )
    )

    Column(
        modifier = modifier.padding(
            start = gridStyle.insets.start,
            end = gridStyle.insets.end,
            top = gridStyle.insets.top,
            bottom = gridStyle.insets.bottom
        ),
        verticalArrangement = Arrangement.spacedBy(gridStyle.rowGap)
    ) {
        rows.forEach { rowSpecs ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(gridStyle.columnGap)
            ) {
                rowSpecs.forEach { spec ->
                    if (spec == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    } else {
                        val isTopLabel =
                            spec.tag == TEST_TAG_DELETE || spec.tag == TEST_TAG_CLEAR
                        CalculatorPadButton(
                            label = spec.label,
                            contentDescription = spec.contentDescription,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag(spec.tag),
                            textSize = if (isTopLabel) topLabelTextSize else operatorTextSize,
                            textColorRes = R.color.pad_button_text_color,
                            enabled = enabled,
                            onClick = {
                                if (isTopLabel) {
                                    onEvent(
                                        if (state.showsClearButton) {
                                            CalculatorUiEvent.Clear
                                        } else {
                                            CalculatorUiEvent.Delete
                                        }
                                    )
                                } else {
                                    onEvent(spec.event ?: CalculatorUiEvent.Append(spec.label))
                                }
                            },
                            onLongClick = {
                                if (spec.tag == TEST_TAG_DELETE) {
                                    onEvent(CalculatorUiEvent.Clear)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedPad(
    onEvent: (CalculatorUiEvent) -> Unit,
    enabled: Boolean,
    gridStyle: GridStyleSpec,
    textSize: TextUnit,
    columns: Int,
    modifier: Modifier = Modifier
) {
    val sinSymbol = stringResource(R.string.fun_sin)
    val cosSymbol = stringResource(R.string.fun_cos)
    val tanSymbol = stringResource(R.string.fun_tan)
    val lnSymbol = stringResource(R.string.fun_ln)
    val logSymbol = stringResource(R.string.fun_log)
    val factorialSymbol = stringResource(R.string.op_fact)
    val piSymbol = stringResource(R.string.const_pi)
    val eSymbol = stringResource(R.string.const_e)
    val powerSymbol = stringResource(R.string.op_pow)
    val leftParenthesis = stringResource(R.string.lparen)
    val rightParenthesis = stringResource(R.string.rparen)
    val squareRootSymbol = stringResource(R.string.op_sqrt)

    val buttonSpecs = listOf(
        PadButtonSpec(
            sinSymbol,
            TEST_TAG_FUN_SIN,
            stringResource(R.string.desc_fun_sin),
            CalculatorUiEvent.Append(sinSymbol, appendLeftParenthesis = true)
        ),
        PadButtonSpec(
            cosSymbol,
            TEST_TAG_FUN_COS,
            stringResource(R.string.desc_fun_cos),
            CalculatorUiEvent.Append(cosSymbol, appendLeftParenthesis = true)
        ),
        PadButtonSpec(
            tanSymbol,
            TEST_TAG_FUN_TAN,
            stringResource(R.string.desc_fun_tan),
            CalculatorUiEvent.Append(tanSymbol, appendLeftParenthesis = true)
        ),
        PadButtonSpec(
            lnSymbol,
            TEST_TAG_FUN_LN,
            stringResource(R.string.desc_fun_ln),
            CalculatorUiEvent.Append(lnSymbol, appendLeftParenthesis = true)
        ),
        PadButtonSpec(
            logSymbol,
            TEST_TAG_FUN_LOG,
            stringResource(R.string.desc_fun_log),
            CalculatorUiEvent.Append(logSymbol, appendLeftParenthesis = true)
        ),
        PadButtonSpec(
            factorialSymbol,
            TEST_TAG_OP_FACT,
            stringResource(R.string.desc_op_fact),
            CalculatorUiEvent.Append(factorialSymbol)
        ),
        PadButtonSpec(
            piSymbol,
            TEST_TAG_CONST_PI,
            stringResource(R.string.desc_const_pi),
            CalculatorUiEvent.Append(piSymbol)
        ),
        PadButtonSpec(
            eSymbol,
            TEST_TAG_CONST_E,
            stringResource(R.string.desc_const_e),
            CalculatorUiEvent.Append(eSymbol)
        ),
        PadButtonSpec(
            powerSymbol,
            TEST_TAG_OP_POWER,
            stringResource(R.string.desc_op_pow),
            CalculatorUiEvent.Append(powerSymbol)
        ),
        PadButtonSpec(
            leftParenthesis,
            TEST_TAG_LEFT_PAREN,
            stringResource(R.string.desc_lparen),
            CalculatorUiEvent.Append(leftParenthesis)
        ),
        PadButtonSpec(
            rightParenthesis,
            TEST_TAG_RIGHT_PAREN,
            stringResource(R.string.desc_rparen),
            CalculatorUiEvent.Append(rightParenthesis)
        ),
        PadButtonSpec(
            squareRootSymbol,
            TEST_TAG_OP_SQRT,
            stringResource(R.string.desc_op_sqrt),
            CalculatorUiEvent.Append(squareRootSymbol)
        )
    )

    val rowCount = (buttonSpecs.size + columns - 1) / columns
    val rows = List(rowCount) { row ->
        List(columns) { column ->
            val index = row * columns + column
            buttonSpecs.getOrNull(index)
        }
    }

    Column(
        modifier = modifier.padding(
            start = gridStyle.insets.start,
            end = gridStyle.insets.end,
            top = gridStyle.insets.top,
            bottom = gridStyle.insets.bottom
        ),
        verticalArrangement = Arrangement.spacedBy(gridStyle.rowGap)
    ) {
        rows.forEach { rowSpecs ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(gridStyle.columnGap)
            ) {
                rowSpecs.forEach { spec ->
                    if (spec == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    } else {
                        CalculatorPadButton(
                            label = spec.label,
                            contentDescription = spec.contentDescription,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag(spec.tag),
                            textSize = textSize,
                            textColorRes = R.color.pad_button_advanced_text_color,
                            enabled = enabled,
                            onClick = {
                                onEvent(spec.event ?: CalculatorUiEvent.Append(spec.label))
                            }
                        )
                    }
                }
            }
        }
    }
}

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

@Composable
private fun CalculatorPadButton(
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

private data class PadButtonSpec(
    val label: String,
    val tag: String,
    val contentDescription: String = label,
    val event: CalculatorUiEvent? = null
)

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

private enum class DeviceTier {
    PHONE,
    TABLET_600,
    TABLET_800
}

private enum class ComposeLayoutMode {
    PHONE_PORTRAIT_PAGER,
    LANDSCAPE_SPLIT,
    TABLET_PORTRAIT_SPLIT
}

private data class EdgeInsets(val start: Dp, val end: Dp, val top: Dp, val bottom: Dp)

private data class GridStyleSpec(val insets: EdgeInsets, val rowGap: Dp, val columnGap: Dp)

private data class DisplayStyleSpec(
    val formulaInsets: EdgeInsets,
    val resultInsets: EdgeInsets,
    val formulaMinSizeSp: Int,
    val formulaMaxSizeSp: Int,
    val formulaStepSizeSp: Int,
    val resultSizeSp: Int
)

private data class LayoutStyleSpec(
    val mode: ComposeLayoutMode,
    val display: DisplayStyleSpec,
    val numericGrid: GridStyleSpec,
    val operatorOneGrid: GridStyleSpec,
    val operatorTwoGrid: GridStyleSpec,
    val advancedGrid: GridStyleSpec,
    val numericTextSize: TextUnit,
    val operatorTextSize: TextUnit,
    val operatorTopLabelTextSize: TextUnit,
    val advancedTextSize: TextUnit,
    val numericWeight: Float,
    val operatorWeight: Float,
    val advancedWeight: Float = 0f,
    val advancedColumns: Int = 3,
    val tabletPortraitAdvancedWeight: Float = 0f,
    val tabletPortraitBottomRowWeight: Float = 0f
)

private fun resolveLayoutSpec(configuration: Configuration): LayoutStyleSpec {
    val deviceTier = when {
        configuration.smallestScreenWidthDp >= 800 -> DeviceTier.TABLET_800
        configuration.smallestScreenWidthDp >= 600 -> DeviceTier.TABLET_600
        else -> DeviceTier.PHONE
    }
    val mode = when {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ->
            ComposeLayoutMode.LANDSCAPE_SPLIT

        deviceTier != DeviceTier.PHONE -> ComposeLayoutMode.TABLET_PORTRAIT_SPLIT
        else -> ComposeLayoutMode.PHONE_PORTRAIT_PAGER
    }

    return when (mode) {
        ComposeLayoutMode.PHONE_PORTRAIT_PAGER -> {
            LayoutStyleSpec(
                mode = mode,
                display = DisplayStyleSpec(
                    formulaInsets = insets(16.dp, 16.dp, 48.dp, 24.dp),
                    resultInsets = insets(16.dp, 16.dp, 24.dp, 48.dp),
                    formulaMinSizeSp = 36,
                    formulaMaxSizeSp = 64,
                    formulaStepSizeSp = 8,
                    resultSizeSp = 36
                ),
                numericGrid = GridStyleSpec(
                    insets = insets(12.dp, 12.dp, 12.dp, 20.dp),
                    rowGap = 8.dp,
                    columnGap = 8.dp
                ),
                operatorOneGrid = GridStyleSpec(
                    insets = insets(4.dp, 28.dp, 8.dp, 24.dp),
                    rowGap = 16.dp,
                    columnGap = 0.dp
                ),
                operatorTwoGrid = GridStyleSpec(
                    insets = insets(4.dp, 28.dp, 8.dp, 24.dp),
                    rowGap = 16.dp,
                    columnGap = 16.dp
                ),
                advancedGrid = GridStyleSpec(
                    insets = insets(20.dp, 20.dp, 12.dp, 20.dp),
                    rowGap = 8.dp,
                    columnGap = 8.dp
                ),
                numericTextSize = 32.sp,
                operatorTextSize = 23.sp,
                operatorTopLabelTextSize = 15.sp,
                advancedTextSize = 20.sp,
                numericWeight = 264f,
                operatorWeight = 96f,
                advancedColumns = 3
            )
        }

        ComposeLayoutMode.LANDSCAPE_SPLIT -> {
            when (deviceTier) {
                DeviceTier.PHONE -> {
                    LayoutStyleSpec(
                        mode = mode,
                        display = DisplayStyleSpec(
                            formulaInsets = insets(16.dp, 16.dp, 24.dp, 8.dp),
                            resultInsets = insets(16.dp, 16.dp, 8.dp, 24.dp),
                            formulaMinSizeSp = 30,
                            formulaMaxSizeSp = 30,
                            formulaStepSizeSp = 1,
                            resultSizeSp = 30
                        ),
                        numericGrid = GridStyleSpec(
                            insets = insets(8.dp, 8.dp, 4.dp, 4.dp),
                            rowGap = 8.dp,
                            columnGap = 8.dp
                        ),
                        operatorOneGrid = GridStyleSpec(
                            insets = insets(12.dp, 12.dp, 4.dp, 4.dp),
                            rowGap = 8.dp,
                            columnGap = 0.dp
                        ),
                        operatorTwoGrid = GridStyleSpec(
                            insets = insets(12.dp, 12.dp, 4.dp, 4.dp),
                            rowGap = 8.dp,
                            columnGap = 16.dp
                        ),
                        advancedGrid = GridStyleSpec(
                            insets = insets(8.dp, 8.dp, 4.dp, 4.dp),
                            rowGap = 8.dp,
                            columnGap = 16.dp
                        ),
                        numericTextSize = 23.sp,
                        operatorTextSize = 20.sp,
                        operatorTopLabelTextSize = 15.sp,
                        advancedTextSize = 15.sp,
                        numericWeight = 240f,
                        operatorWeight = 144f,
                        advancedWeight = 208f,
                        advancedColumns = 3
                    )
                }

                DeviceTier.TABLET_600 -> {
                    LayoutStyleSpec(
                        mode = mode,
                        display = DisplayStyleSpec(
                            formulaInsets = insets(44.dp, 44.dp, 46.dp, 26.dp),
                            resultInsets = insets(44.dp, 44.dp, 26.dp, 46.dp),
                            formulaMinSizeSp = 48,
                            formulaMaxSizeSp = 48,
                            formulaStepSizeSp = 1,
                            resultSizeSp = 48
                        ),
                        numericGrid = GridStyleSpec(
                            insets = insets(22.dp, 18.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 12.dp
                        ),
                        operatorOneGrid = GridStyleSpec(
                            insets = insets(18.dp, 18.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 0.dp
                        ),
                        operatorTwoGrid = GridStyleSpec(
                            insets = insets(18.dp, 18.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 12.dp
                        ),
                        advancedGrid = GridStyleSpec(
                            insets = insets(18.dp, 22.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 12.dp
                        ),
                        numericTextSize = 36.sp,
                        operatorTextSize = 36.sp,
                        operatorTopLabelTextSize = 24.sp,
                        advancedTextSize = 27.sp,
                        numericWeight = 532f,
                        operatorWeight = 240f,
                        advancedWeight = 508f,
                        advancedColumns = 3
                    )
                }

                DeviceTier.TABLET_800 -> {
                    LayoutStyleSpec(
                        mode = mode,
                        display = DisplayStyleSpec(
                            formulaInsets = insets(44.dp, 44.dp, 72.dp, 26.dp),
                            resultInsets = insets(44.dp, 44.dp, 26.dp, 46.dp),
                            formulaMinSizeSp = 56,
                            formulaMaxSizeSp = 96,
                            formulaStepSizeSp = 8,
                            resultSizeSp = 56
                        ),
                        numericGrid = GridStyleSpec(
                            insets = insets(22.dp, 18.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 12.dp
                        ),
                        operatorOneGrid = GridStyleSpec(
                            insets = insets(18.dp, 18.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 0.dp
                        ),
                        operatorTwoGrid = GridStyleSpec(
                            insets = insets(18.dp, 18.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 12.dp
                        ),
                        advancedGrid = GridStyleSpec(
                            insets = insets(18.dp, 22.dp, 10.dp, 10.dp),
                            rowGap = 12.dp,
                            columnGap = 12.dp
                        ),
                        numericTextSize = 48.sp,
                        operatorTextSize = 48.sp,
                        operatorTopLabelTextSize = 32.sp,
                        advancedTextSize = 36.sp,
                        numericWeight = 532f,
                        operatorWeight = 240f,
                        advancedWeight = 508f,
                        advancedColumns = 3
                    )
                }
            }
        }

        ComposeLayoutMode.TABLET_PORTRAIT_SPLIT -> {
            when (deviceTier) {
                DeviceTier.TABLET_800 -> {
                    LayoutStyleSpec(
                        mode = mode,
                        display = DisplayStyleSpec(
                            formulaInsets = insets(44.dp, 44.dp, 72.dp, 32.dp),
                            resultInsets = insets(44.dp, 44.dp, 32.dp, 90.dp),
                            formulaMinSizeSp = 56,
                            formulaMaxSizeSp = 96,
                            formulaStepSizeSp = 8,
                            resultSizeSp = 56
                        ),
                        numericGrid = GridStyleSpec(
                            insets = insets(16.dp, 16.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 16.dp
                        ),
                        operatorOneGrid = GridStyleSpec(
                            insets = insets(0.dp, 8.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 0.dp
                        ),
                        operatorTwoGrid = GridStyleSpec(
                            insets = insets(0.dp, 8.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 32.dp
                        ),
                        advancedGrid = GridStyleSpec(
                            insets = insets(16.dp, 16.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 24.dp
                        ),
                        numericTextSize = 48.sp,
                        operatorTextSize = 48.sp,
                        operatorTopLabelTextSize = 32.sp,
                        advancedTextSize = 36.sp,
                        numericWeight = 532f,
                        operatorWeight = 240f,
                        advancedColumns = 6,
                        tabletPortraitAdvancedWeight = 256f,
                        tabletPortraitBottomRowWeight = 508f
                    )
                }

                DeviceTier.PHONE,
                DeviceTier.TABLET_600 -> {
                    LayoutStyleSpec(
                        mode = mode,
                        display = DisplayStyleSpec(
                            formulaInsets = insets(44.dp, 44.dp, 72.dp, 32.dp),
                            resultInsets = insets(44.dp, 44.dp, 32.dp, 90.dp),
                            formulaMinSizeSp = 48,
                            formulaMaxSizeSp = 80,
                            formulaStepSizeSp = 8,
                            resultSizeSp = 48
                        ),
                        numericGrid = GridStyleSpec(
                            insets = insets(16.dp, 16.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 16.dp
                        ),
                        operatorOneGrid = GridStyleSpec(
                            insets = insets(0.dp, 8.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 0.dp
                        ),
                        operatorTwoGrid = GridStyleSpec(
                            insets = insets(0.dp, 8.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 32.dp
                        ),
                        advancedGrid = GridStyleSpec(
                            insets = insets(16.dp, 16.dp, 8.dp, 8.dp),
                            rowGap = 16.dp,
                            columnGap = 24.dp
                        ),
                        numericTextSize = 36.sp,
                        operatorTextSize = 36.sp,
                        operatorTopLabelTextSize = 24.sp,
                        advancedTextSize = 27.sp,
                        numericWeight = 532f,
                        operatorWeight = 240f,
                        advancedColumns = 6,
                        tabletPortraitAdvancedWeight = 256f,
                        tabletPortraitBottomRowWeight = 508f
                    )
                }
            }
        }
    }
}

private fun insets(start: Dp, end: Dp, top: Dp, bottom: Dp): EdgeInsets =
    EdgeInsets(start = start, end = end, top = top, bottom = bottom)

@Composable
private fun legacyTrimmedInsets(baseInsets: EdgeInsets, fontSize: TextUnit): EdgeInsets {
    val density = LocalDensity.current
    val topPx = with(density) { baseInsets.top.toPx() }
    val bottomPx = with(density) { baseInsets.bottom.toPx() }
    val textSizePx = with(density) { fontSize.toPx() }
    val (trimmedTopPx, trimmedBottomPx) = remember(topPx, bottomPx, textSizePx) {
        computeLegacyVerticalInsetsPx(topPx, bottomPx, textSizePx)
    }

    return EdgeInsets(
        start = baseInsets.start,
        end = baseInsets.end,
        top = with(density) { trimmedTopPx.toDp() },
        bottom = with(density) { trimmedBottomPx.toDp() }
    )
}

private fun computeLegacyVerticalInsetsPx(
    baseTopPx: Float,
    baseBottomPx: Float,
    textSizePx: Float
): Pair<Float, Float> {
    val paint = Paint().apply {
        isAntiAlias = true
        textSize = textSizePx
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    val capBounds = AndroidRect()
    paint.getTextBounds("H", 0, 1, capBounds)
    val metrics = paint.fontMetricsInt

    val topOffset = -(metrics.ascent + capBounds.height()).toFloat()
    val topAdjustment = minOf(baseTopPx, topOffset)
    val bottomAdjustment = minOf(baseBottomPx, metrics.descent.toFloat())

    return (baseTopPx - topAdjustment).coerceAtLeast(0f) to
        (baseBottomPx - bottomAdjustment).coerceAtLeast(0f)
}

private fun legacyDisplayTextStyle(fontSize: TextUnit): TextStyle = TextStyle(
    fontSize = fontSize,
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private const val PAD_PAGE_COUNT = 2
private const val ADVANCED_PAGE_WIDTH_FRACTION = 7f / 9f

private const val KEY_FORMULA = "formula"
private const val KEY_RESULT = "result"
private const val KEY_PHASE = "phase"

const val TEST_TAG_PAD_PAGER = "pad_pager"
const val TEST_TAG_ADVANCED_PAD = "pad_advanced"

const val TEST_TAG_DISPLAY = "display"
const val TEST_TAG_FORMULA = "formula"
const val TEST_TAG_RESULT = "result"
const val TEST_TAG_DECIMAL = "dec_point"
const val TEST_TAG_EQUALS = "eq"
const val TEST_TAG_DELETE = "del"
const val TEST_TAG_CLEAR = "clr"
const val TEST_TAG_DIVIDE = "op_div"
const val TEST_TAG_MULTIPLY = "op_mul"
const val TEST_TAG_SUBTRACT = "op_sub"
const val TEST_TAG_ADD = "op_add"
const val TEST_TAG_DIGIT_0 = "digit_0"
const val TEST_TAG_DIGIT_1 = "digit_1"
const val TEST_TAG_DIGIT_2 = "digit_2"
const val TEST_TAG_DIGIT_3 = "digit_3"
const val TEST_TAG_DIGIT_4 = "digit_4"
const val TEST_TAG_DIGIT_5 = "digit_5"
const val TEST_TAG_DIGIT_6 = "digit_6"
const val TEST_TAG_DIGIT_7 = "digit_7"
const val TEST_TAG_DIGIT_8 = "digit_8"
const val TEST_TAG_DIGIT_9 = "digit_9"

const val TEST_TAG_FUN_SIN = "fun_sin"
const val TEST_TAG_FUN_COS = "fun_cos"
const val TEST_TAG_FUN_TAN = "fun_tan"
const val TEST_TAG_FUN_LN = "fun_ln"
const val TEST_TAG_FUN_LOG = "fun_log"
const val TEST_TAG_OP_FACT = "op_fact"
const val TEST_TAG_CONST_PI = "const_pi"
const val TEST_TAG_CONST_E = "const_e"
const val TEST_TAG_OP_POWER = "op_pow"
const val TEST_TAG_LEFT_PAREN = "lparen"
const val TEST_TAG_RIGHT_PAREN = "rparen"
const val TEST_TAG_OP_SQRT = "op_sqrt"
