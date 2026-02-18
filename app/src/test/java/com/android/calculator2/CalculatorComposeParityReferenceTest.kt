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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalculatorComposeParityReferenceTest {

    @Test
    fun composeReferenceScreensStayCloseToLegacyBaselines() {
        val pairs = listOf(
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/phone_portrait_light_initial.png",
                legacyPath = "src/test/screenshots/legacy/phone_portrait_light.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/phone_portrait_light_drawer_open_initial.png",
                legacyPath = "src/test/screenshots/legacy/phone_portrait_light_drawer_open.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/phone_portrait_dark_initial.png",
                legacyPath = "src/test/screenshots/legacy/phone_portrait_dark.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/phone_landscape_light_initial.png",
                legacyPath = "src/test/screenshots/legacy/phone_landscape_light.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/tablet_portrait_light_initial.png",
                legacyPath = "src/test/screenshots/legacy/tablet_portrait_light.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/tablet_portrait_dark_initial.png",
                legacyPath = "src/test/screenshots/legacy/tablet_portrait_dark.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/windowed_light_initial.png",
                legacyPath = "src/test/screenshots/legacy/windowed_light.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/windowed_medium_light_initial.png",
                legacyPath = "src/test/screenshots/legacy/windowed_medium_light.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/windowed_expanded_light_initial.png",
                legacyPath = "src/test/screenshots/legacy/windowed_expanded_light.png"
            ),
            ScreenshotPair(
                composePath = "src/test/screenshots/compose/phone_portrait_large_font_initial.png",
                legacyPath = "src/test/screenshots/legacy/phone_portrait_large_font.png"
            )
        )

        pairs.forEach { pair ->
            val compose = screenshotFile(pair.composePath)
            val legacy = screenshotFile(pair.legacyPath)
            val composeImage = decodeBitmap(compose)
            val legacyImage = decodeBitmap(legacy)

            assertEquals(
                "Compose and legacy screenshot widths differ for ${pair.composePath}",
                legacyImage.width,
                composeImage.width
            )
            assertEquals(
                "Compose and legacy screenshot heights differ for ${pair.composePath}",
                legacyImage.height,
                composeImage.height
            )

            val normalizedDiff = normalizedRgbDifference(composeImage, legacyImage)
            assertTrue(
                "Compose screenshot drifted from legacy reference " +
                    "for ${pair.composePath} (diff=$normalizedDiff)",
                normalizedDiff <= MAX_NORMALIZED_DIFF
            )
        }
    }

    private fun screenshotFile(path: String): File {
        val file = File(path)
        assertTrue("Missing screenshot baseline: $path", file.exists())
        return file
    }

    private fun decodeBitmap(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        assertTrue("Unable to decode screenshot: ${file.path}", bitmap != null)
        return bitmap!!
    }

    private fun normalizedRgbDifference(first: Bitmap, second: Bitmap): Double {
        var diffSum = 0L
        val totalPixels = first.width * first.height

        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                val firstPixel = first.getPixel(x, y)
                val secondPixel = second.getPixel(x, y)

                diffSum += kotlin.math.abs(Color.red(firstPixel) - Color.red(secondPixel))
                diffSum += kotlin.math.abs(Color.green(firstPixel) - Color.green(secondPixel))
                diffSum += kotlin.math.abs(Color.blue(firstPixel) - Color.blue(secondPixel))
            }
        }

        return diffSum.toDouble() / (totalPixels * 255.0 * 3.0)
    }

    companion object {
        private const val MAX_NORMALIZED_DIFF = 0.35
    }
}

private data class ScreenshotPair(val composePath: String, val legacyPath: String)
