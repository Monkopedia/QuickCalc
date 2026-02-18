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

import android.content.res.Configuration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class DeviceTier {
    PHONE,
    TABLET_600,
    TABLET_800
}

internal enum class ComposeLayoutMode {
    PHONE_PORTRAIT_PAGER,
    LANDSCAPE_SPLIT,
    TABLET_PORTRAIT_SPLIT
}

internal data class EdgeInsets(val start: Dp, val end: Dp, val top: Dp, val bottom: Dp)

internal data class GridStyleSpec(val insets: EdgeInsets, val rowGap: Dp, val columnGap: Dp)

internal data class DisplayStyleSpec(
    val formulaInsets: EdgeInsets,
    val resultInsets: EdgeInsets,
    val formulaMinSizeSp: Int,
    val formulaMaxSizeSp: Int,
    val formulaStepSizeSp: Int,
    val resultSizeSp: Int
)

internal data class LayoutStyleSpec(
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

internal fun resolveLayoutSpec(configuration: Configuration): LayoutStyleSpec {
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

internal fun insets(start: Dp, end: Dp, top: Dp, bottom: Dp): EdgeInsets =
    EdgeInsets(start = start, end = end, top = top, bottom = bottom)
