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

import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val LEGACY_ACCELERATE_DECELERATE_EASING = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private const val RESULT_ANIMATION_FALLBACK_DURATION_MS = 420

@Composable
internal fun DisplayPanel(
    state: CalculatorUiState,
    style: DisplayStyleSpec,
    backgroundColor: Color,
    formulaColor: Color,
    resultColor: Color,
    animateFormulaAutosize: Boolean,
    onBoundsChanged: (Rect) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val resultInsets = legacyTrimmedInsets(style.resultInsets, style.resultSizeSp.sp)
    val formulaRowHeightPx = remember(style, density) { formulaRowMinHeightPx(style, density) }
    val formulaRowHeightDp =
        remember(style, density) { with(density) { formulaRowHeightPx.toDp() } }
    val resultRowHeightPx = remember(style, density) { resultRowHeightPx(style, density) }
    val resultRowHeightDp = remember(style, density) { with(density) { resultRowHeightPx.toDp() } }
    val displayHeightDp = formulaRowHeightDp + resultRowHeightDp

    val resultAnimationDurationMs = remember(context) {
        context.resources.getInteger(android.R.integer.config_longAnimTime)
            .takeIf { it > 0 }
            ?: RESULT_ANIMATION_FALLBACK_DURATION_MS
    }

    var previousUiState by remember { mutableStateOf(state) }
    var activeTransition by remember { mutableStateOf<DisplayResultTransition?>(null) }
    var settledResultVisualText by remember(state.phase) {
        mutableStateOf(settledResultVisualTextForState(state))
    }
    val transitionProgress = remember { Animatable(1f) }
    val frameTransition = activeTransition ?: resolveDisplayResultTransition(previousUiState, state)
    val settledTransition =
        if (frameTransition == null && state.phase == CalculatorUiPhase.RESULT) {
            settledResultVisualText?.let { text ->
                DisplayResultTransition(
                    movingResultText = text,
                    outgoingFormulaText = null
                )
            }
        } else {
            null
        }
    val renderTransition = frameTransition ?: settledTransition
    val frameProgress =
        if (activeTransition == null && frameTransition != null) {
            0f
        } else {
            transitionProgress.value
        }

    LaunchedEffect(state) {
        val nextTransition = resolveDisplayResultTransition(previousUiState, state)
        if (nextTransition != null) {
            settledResultVisualText = null
            activeTransition = nextTransition
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resultAnimationDurationMs,
                    easing = LEGACY_ACCELERATE_DECELERATE_EASING
                )
            )
            // Hold the exact terminal transform for one frame before swapping to static rows.
            transitionProgress.snapTo(1f)
            withFrameNanos { }
            activeTransition = null
            settledResultVisualText = nextTransition.movingResultText
        } else {
            activeTransition = null
            transitionProgress.snapTo(1f)
            settledResultVisualText = settledResultVisualTextForState(state)
        }

        previousUiState = state
    }

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(displayHeightDp)
                .clipToBounds()
        ) {
            val displayWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(0f) }
            val transition = renderTransition
            if (transition == null) {
                DisplayStaticRows(
                    state = state,
                    style = style,
                    formulaColor = formulaColor,
                    resultColor = resultColor,
                    resultInsets = resultInsets,
                    formulaRowHeightPx = formulaRowHeightPx,
                    animateFormulaAutosize = animateFormulaAutosize
                )
            } else {
                val transitionMetrics = rememberLegacyResultTransitionMetrics(
                    transition = transition,
                    style = style,
                    displayWidthPx = displayWidthPx,
                    formulaRowHeightPx = formulaRowHeightPx,
                    resultRowHeightPx = resultRowHeightPx
                )
                DisplayResultTransitionRows(
                    transition = transition,
                    progress =
                    if (frameTransition != null) {
                        frameProgress
                    } else {
                        1f
                    },
                    transitionMetrics = transitionMetrics,
                    style = style,
                    formulaColor = formulaColor,
                    resultColor = resultColor,
                    resultInsets = resultInsets,
                    formulaRowHeightPx = formulaRowHeightPx,
                    resultRowHeightDp = resultRowHeightDp
                )
            }
        }
    }
}

@Composable
private fun DisplayStaticRows(
    state: CalculatorUiState,
    style: DisplayStyleSpec,
    formulaColor: Color,
    resultColor: Color,
    resultInsets: EdgeInsets,
    formulaRowHeightPx: Float,
    animateFormulaAutosize: Boolean
) {
    AutoSizeFormulaText(
        text = state.formulaText.orDisplayText(),
        style = style,
        color = formulaColor,
        tag = TEST_TAG_FORMULA,
        animateSizeChanges = state.phase == CalculatorUiPhase.INPUT && animateFormulaAutosize,
        modifier = Modifier.fillMaxWidth()
    )

    val showsVisibleResult = state.phase != CalculatorUiPhase.RESULT
    ResultRowText(
        text = state.resultText.orDisplayText(),
        color = if (showsVisibleResult) resultColor else resultColor.copy(alpha = 0f),
        style = style,
        resultInsets = resultInsets,
        tag = TEST_TAG_RESULT,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = formulaRowHeightPx }
    )
}

@Composable
private fun DisplayResultTransitionRows(
    transition: DisplayResultTransition,
    progress: Float,
    transitionMetrics: LegacyResultTransitionMetrics,
    style: DisplayStyleSpec,
    formulaColor: Color,
    resultColor: Color,
    resultInsets: EdgeInsets,
    formulaRowHeightPx: Float,
    resultRowHeightDp: androidx.compose.ui.unit.Dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    transition.outgoingFormulaText?.let { outgoingFormulaText ->
        AutoSizeFormulaText(
            text = outgoingFormulaText,
            style = style,
            color = formulaColor,
            tag = TEST_TAG_FORMULA,
            animateSizeChanges = false,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = transitionMetrics.formulaTranslationY * clampedProgress
                }
        )
    }

    val movingColor = lerp(resultColor, formulaColor, clampedProgress)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(resultRowHeightDp)
            .graphicsLayer {
                scaleX = lerpFloat(1f, transitionMetrics.resultScale, clampedProgress)
                scaleY = lerpFloat(1f, transitionMetrics.resultScale, clampedProgress)
                translationX = lerpFloat(0f, transitionMetrics.resultTranslationX, clampedProgress)
                translationY =
                    formulaRowHeightPx +
                    lerpFloat(0f, transitionMetrics.resultTranslationY, clampedProgress)
            }
    ) {
        ResultRowText(
            text = transition.movingResultText,
            color = movingColor,
            style = style,
            resultInsets = resultInsets,
            tag = TEST_TAG_RESULT,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun rememberLegacyResultTransitionMetrics(
    transition: DisplayResultTransition,
    style: DisplayStyleSpec,
    displayWidthPx: Float,
    formulaRowHeightPx: Float,
    resultRowHeightPx: Float
): LegacyResultTransitionMetrics {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    return remember(
        transition,
        style,
        displayWidthPx,
        formulaRowHeightPx,
        resultRowHeightPx,
        density
    ) {
        val maxFormulaSize = style.formulaMaxSizeSp.sp
        val baseFormulaInsetsPx =
            legacyTrimmedInsetsPx(style.formulaInsets, maxFormulaSize, density)
        val formulaAvailableWidthPx =
            (displayWidthPx - baseFormulaInsetsPx.start - baseFormulaInsetsPx.end)
                .coerceAtLeast(0f)

        val targetFormulaSizeSp = computeFittingTextSizeSp(
            text = transition.movingResultText,
            minSizeSp = style.formulaMinSizeSp,
            maxSizeSp = style.formulaMaxSizeSp,
            stepSizeSp = style.formulaStepSizeSp,
            maxWidthPx = formulaAvailableWidthPx
        ) { candidateSp ->
            textMeasurer.measure(
                text = AnnotatedString(transition.movingResultText),
                style = legacyDisplayTextStyle(candidateSp.sp),
                maxLines = 1
            ).size.width.toFloat()
        }

        val resultInsetsPx =
            legacyTrimmedInsetsPx(style.resultInsets, style.resultSizeSp.sp, density)
        val formulaInsetsPx =
            legacyTrimmedInsetsPx(style.formulaInsets, targetFormulaSizeSp.sp, density)

        computeLegacyResultTransitionMetrics(
            resultTextSizePx = with(density) { style.resultSizeSp.sp.toPx() },
            targetFormulaTextSizePx = with(density) { targetFormulaSizeSp.sp.toPx() },
            resultViewWidthPx = displayWidthPx,
            resultViewHeightPx = resultRowHeightPx,
            resultPaddingEndPx = resultInsetsPx.end,
            resultPaddingBottomPx = resultInsetsPx.bottom,
            formulaPaddingBottomPx = formulaInsetsPx.bottom,
            formulaBottomPx = formulaRowHeightPx,
            resultBottomPx = formulaRowHeightPx + resultRowHeightPx
        )
    }
}

@Composable
private fun ResultRowText(
    text: String,
    color: Color,
    style: DisplayStyleSpec,
    resultInsets: EdgeInsets,
    tag: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .padding(
                top = resultInsets.top,
                bottom = resultInsets.bottom,
                start = resultInsets.start,
                end = resultInsets.end
            )
            .then(
                if (tag != null) {
                    Modifier.testTag(tag)
                } else {
                    Modifier
                }
            ),
        color = color,
        style = legacyDisplayTextStyle(style.resultSizeSp.sp),
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AutoSizeFormulaText(
    text: String,
    style: DisplayStyleSpec,
    color: Color,
    tag: String?,
    animateSizeChanges: Boolean,
    modifier: Modifier = Modifier
) {
    val maxFormulaSizeSp = style.formulaMaxSizeSp.sp
    val baseFormulaInsets = legacyTrimmedInsets(style.formulaInsets, maxFormulaSizeSp)
    val density = LocalDensity.current
    val minFormulaHeight = remember(style, density) {
        with(density) { formulaRowMinHeightPx(style, density).toDp() }
    }
    BoxWithConstraints(
        modifier = modifier
            .height(minFormulaHeight)
            .padding(
                start = baseFormulaInsets.start,
                end = baseFormulaInsets.end
            )
    ) {
        val textMeasurer = rememberTextMeasurer()
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

        val animatedSize =
            if (animateSizeChanges) {
                animateFloatAsState(
                    targetValue = targetTextSize,
                    animationSpec = tween(durationMillis = 220),
                    label = "formulaAutoSize"
                ).value
            } else {
                targetTextSize
            }
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
                .then(
                    if (tag != null) {
                        Modifier.testTag(tag)
                    } else {
                        Modifier
                    }
                )
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

private data class PxInsets(val start: Float, val end: Float, val top: Float, val bottom: Float)

private fun legacyTrimmedInsetsPx(
    baseInsets: EdgeInsets,
    fontSize: TextUnit,
    density: Density
): PxInsets {
    val baseTopPx = with(density) { baseInsets.top.toPx() }
    val baseBottomPx = with(density) { baseInsets.bottom.toPx() }
    val textSizePx = with(density) { fontSize.toPx() }
    val (trimmedTopPx, trimmedBottomPx) =
        computeLegacyVerticalInsetsPx(baseTopPx, baseBottomPx, textSizePx)
    return with(density) {
        PxInsets(
            start = baseInsets.start.toPx(),
            end = baseInsets.end.toPx(),
            top = trimmedTopPx,
            bottom = trimmedBottomPx
        )
    }
}

private fun formulaRowMinHeightPx(style: DisplayStyleSpec, density: Density): Float {
    val maxFormulaSize = style.formulaMaxSizeSp.sp
    val maxInsetsPx = legacyTrimmedInsetsPx(style.formulaInsets, maxFormulaSize, density)
    return legacyTextLineHeightPx(maxFormulaSize, density) + maxInsetsPx.top + maxInsetsPx.bottom
}

private fun resultRowHeightPx(style: DisplayStyleSpec, density: Density): Float {
    val resultSize = style.resultSizeSp.sp
    val resultInsetsPx = legacyTrimmedInsetsPx(style.resultInsets, resultSize, density)
    return legacyTextLineHeightPx(resultSize, density) + resultInsetsPx.top + resultInsetsPx.bottom
}

private fun legacyTextLineHeightPx(fontSize: TextUnit, density: Density): Float {
    val paint = Paint().apply {
        isAntiAlias = true
        textSize = with(density) { fontSize.toPx() }
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    val metrics = paint.fontMetrics
    return metrics.descent - metrics.ascent
}

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

private fun settledResultVisualTextForState(state: CalculatorUiState): String? =
    if (state.phase == CalculatorUiPhase.RESULT) {
        state.formulaText.orDisplayText()
    } else {
        null
    }
