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

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.test.core.app.ApplicationProvider
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorAccessibilityColorContrastTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun displayTextColorsMeetLargeTextContrastThreshold() {
        assertContrastAtLeast(
            foregroundColor = resolveColor(R.color.display_formula_text_color),
            backgroundColor = resolveColor(R.color.display_background_color),
            minimumContrast = 3.0,
            label = "Display formula text"
        )
        assertContrastAtLeast(
            foregroundColor = resolveColor(R.color.display_result_text_color),
            backgroundColor = resolveColor(R.color.display_background_color),
            minimumContrast = 3.0,
            label = "Display result text"
        )
    }

    @Test
    fun padTextColorsMeetStandardContrastThreshold() {
        assertContrastAtLeast(
            foregroundColor = resolveColor(R.color.pad_button_text_color),
            backgroundColor = resolveColor(R.color.pad_numeric_background_color),
            minimumContrast = 4.5,
            label = "Numeric pad button text"
        )
        assertContrastAtLeast(
            foregroundColor = resolveColor(R.color.pad_button_text_color),
            backgroundColor = resolveColor(R.color.pad_operator_background_color),
            minimumContrast = 4.5,
            label = "Operator pad button text"
        )
    }

    private fun assertContrastAtLeast(
        foregroundColor: Int,
        backgroundColor: Int,
        minimumContrast: Double,
        label: String
    ) {
        val contrast = contrastRatio(foregroundColor, backgroundColor)
        assertTrue(
            "$label contrast was %.2f (minimum %.2f)".format(contrast, minimumContrast),
            contrast >= minimumContrast
        )
    }

    private fun resolveColor(@ColorRes colorId: Int): Int = context.getColor(colorId)

    private fun contrastRatio(foregroundColor: Int, backgroundColor: Int): Double {
        val compositedForeground = compositeOverBackground(foregroundColor, backgroundColor)
        val foregroundLuminance = luminance(compositedForeground)
        val backgroundLuminance = luminance(backgroundColor)
        val lighter = max(foregroundLuminance, backgroundLuminance)
        val darker = min(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun compositeOverBackground(foregroundColor: Int, backgroundColor: Int): Int {
        val alpha = Color.alpha(foregroundColor) / 255.0
        if (alpha >= 1.0) {
            return foregroundColor
        }
        val red = (
            Color.red(foregroundColor) * alpha +
                Color.red(backgroundColor) * (1.0 - alpha)
            ).roundToInt()
        val green = (
            Color.green(foregroundColor) * alpha +
                Color.green(backgroundColor) * (1.0 - alpha)
            ).roundToInt()
        val blue = (
            Color.blue(foregroundColor) * alpha +
                Color.blue(backgroundColor) * (1.0 - alpha)
            ).roundToInt()
        return Color.rgb(red, green, blue)
    }

    private fun luminance(color: Int): Double {
        fun channel(value: Int): Double {
            val normalized = value / 255.0
            return if (normalized <= 0.03928) {
                normalized / 12.92
            } else {
                ((normalized + 0.055) / 1.055).pow(2.4)
            }
        }
        val red = channel(Color.red(color))
        val green = channel(Color.green(color))
        val blue = channel(Color.blue(color))
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }
}
