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

package com.monkopedia.quickcalc

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
internal fun NumericPad(
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
internal fun OperatorPadOneColumn(
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
internal fun OperatorPadTwoColumn(
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
internal fun AdvancedPad(
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
                            rippleColorRes = R.color.pad_button_advanced_ripple_color,
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
