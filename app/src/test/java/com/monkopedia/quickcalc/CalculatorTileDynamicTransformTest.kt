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

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorTileDynamicTransformTest {

    @Test
    fun applyDynamicGestureDelta_appliesPanInContainerFractions() {
        val initial = DynamicTransform(scale = 0.55f, offsetXFraction = 0f, offsetYFraction = 0f)

        val updated = applyDynamicGestureDelta(
            current = initial,
            zoomDelta = 1f,
            panXPx = 96f,
            panYPx = -48f,
            baseFraction = 0.8f,
            containerWidthPx = 960f,
            containerHeightPx = 960f
        )

        assertEquals(0.55f, updated.scale, 0.0001f)
        assertEquals(0.1f, updated.offsetXFraction, 0.0001f)
        assertEquals(-0.05f, updated.offsetYFraction, 0.0001f)
    }

    @Test
    fun applyDynamicGestureDelta_clampsScale() {
        val initial = DynamicTransform(scale = 1f, offsetXFraction = 0f, offsetYFraction = 0f)

        val minUpdated = applyDynamicGestureDelta(
            current = initial,
            zoomDelta = 0.01f,
            panXPx = 0f,
            panYPx = 0f,
            baseFraction = 0.8f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f
        )
        val maxUpdated = applyDynamicGestureDelta(
            current = initial,
            zoomDelta = 100f,
            panXPx = 0f,
            panYPx = 0f,
            baseFraction = 0.8f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f
        )

        assertEquals(MIN_DIALOG_SCALE, minUpdated.scale, 0.0001f)
        assertEquals(MAX_DIALOG_SCALE, maxUpdated.scale, 0.0001f)
    }

    @Test
    fun applyDynamicGestureDelta_clampsOffsetsForScaleBounds() {
        val initial = DynamicTransform(scale = 1f, offsetXFraction = 0f, offsetYFraction = 0f)

        val updated = applyDynamicGestureDelta(
            current = initial,
            zoomDelta = 0.6f,
            panXPx = 10_000f,
            panYPx = -10_000f,
            baseFraction = 0.8f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f
        )

        val expectedMax = dynamicMaxOffsetFraction(baseFraction = 0.8f, scale = updated.scale)
        assertEquals(expectedMax, updated.offsetXFraction, 0.0001f)
        assertEquals(-expectedMax, updated.offsetYFraction, 0.0001f)
    }

    @Test
    fun applyDynamicGestureDelta_ignoresPanForZeroSizeContainer() {
        val initial =
            DynamicTransform(scale = 1f, offsetXFraction = 0.08f, offsetYFraction = -0.06f)

        val updated = applyDynamicGestureDelta(
            current = initial,
            zoomDelta = 1f,
            panXPx = 100f,
            panYPx = -100f,
            baseFraction = 0.8f,
            containerWidthPx = 0f,
            containerHeightPx = 0f
        )

        assertEquals(0.08f, updated.offsetXFraction, 0.0001f)
        assertEquals(-0.06f, updated.offsetYFraction, 0.0001f)
    }
}
