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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
internal fun PhonePortraitPagerPad(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit,
    initialPadPage: Int,
    style: LayoutStyleSpec,
    numericPadBackground: Color,
    operatorPadBackground: Color,
    advancedPadBackground: Color
) {
    val padPageMargin = dimensionResource(R.dimen.pad_page_margin)
    val pinnedPageMinAlpha = 0.25f
    val pagerBackdropColor = lerp(Color.Black, numericPadBackground, pinnedPageMinAlpha)
    val advancedPeekWidth =
        (if (padPageMargin < 0.dp) -padPageMargin else 0.dp) * ADVANCED_PEEK_WIDTH_MULTIPLIER
    val density = LocalDensity.current
    val pageSpacingPx = with(density) { padPageMargin.toPx() }
    val resolvedInitialPage = remember(initialPadPage) {
        initialPadPage.coerceIn(0, PAD_PAGE_COUNT - 1)
    }
    val pagerState = rememberPagerState(
        initialPage = resolvedInitialPage,
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
            .background(pagerBackdropColor)
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
                        translationX = (size.width + pageSpacingPx) * -pagePosition
                        val baseAlpha = (1f + pagePosition).coerceAtLeast(0f)
                        alpha =
                            if (page == 0) {
                                baseAlpha.coerceAtLeast(pinnedPageMinAlpha)
                            } else {
                                baseAlpha
                            }
                    } else {
                        translationX = 0f
                        alpha = 1f
                    }
                }
        ) {
            when (page) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                    .clipToBounds()
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
                                    .clipToBounds()
                            )
                        }
                        if (advancedPeekWidth > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(advancedPeekWidth)
                                    .background(advancedPadBackground)
                            )
                        }
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
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(ADVANCED_PAGE_WIDTH_FRACTION)
                            .background(advancedPadBackground)
                            .clipToBounds()
                            .testTag(TEST_TAG_ADVANCED_PAD)
                    )
                }
            }
        }
    }
}

@Composable
internal fun LandscapeSplitPad(
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
                .clipToBounds()
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
                .clipToBounds()
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
                .clipToBounds()
                .testTag(TEST_TAG_ADVANCED_PAD)
        )
    }
}

@Composable
internal fun TabletPortraitSplitPad(
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
                .clipToBounds()
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
                    .clipToBounds()
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
                    .clipToBounds()
            )
        }
    }
}

private fun isSettledOnPage(pagerState: PagerState, page: Int): Boolean =
    pagerState.currentPage == page && abs(pagerState.currentPageOffsetFraction) < 0.05f
