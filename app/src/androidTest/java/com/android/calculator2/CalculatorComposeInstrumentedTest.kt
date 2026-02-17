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

import android.content.pm.ActivityInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalculatorComposeInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<CalculatorComposeActivity>()

    @Test
    fun launchRendersComposeNodes() {
        composeRule.onNodeWithTag(TEST_TAG_DISPLAY).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_FORMULA).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_RESULT).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_DIGIT_1).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_ADD).assertIsDisplayed()
    }

    @Test
    fun additionFlowRendersExpectedResult() {
        tap(TEST_TAG_DIGIT_1, TEST_TAG_ADD, TEST_TAG_DIGIT_2, TEST_TAG_EQUALS)
        composeRule.onNodeWithTag(TEST_TAG_RESULT).assertTextContains("3")
    }

    @Test
    fun swipeLeftShowsAdvancedPadAndAppendsFunctionToken() {
        composeRule.onNodeWithTag(TEST_TAG_PAD_PAGER).performTouchInput {
            swipeLeft()
        }

        composeRule.onNodeWithTag(TEST_TAG_FUN_SIN).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(TEST_TAG_FORMULA).assertTextContains("sin(")
    }

    @Test
    fun backFromAdvancedPadReturnsToPrimaryPad() {
        composeRule.onNodeWithTag(TEST_TAG_PAD_PAGER).performTouchInput {
            swipeLeft()
        }
        composeRule.onNodeWithTag(TEST_TAG_ADVANCED_PAD).assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TEST_TAG_DIGIT_1).assertIsDisplayed()
    }

    @Test
    fun landscapeShowsAdvancedPadWithoutPagerSwipe() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(TEST_TAG_PAD_PAGER).assertCountEquals(0)
        composeRule.onNodeWithTag(TEST_TAG_ADVANCED_PAD).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_FUN_SIN).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_EQUALS).assertIsDisplayed()
    }

    @Test
    fun statusBarUsesAccentAndErrorColorsLikeLegacyActivity() {
        composeRule.waitForIdle()
        composeRule.activity.run {
            @Suppress("DEPRECATION")
            assertEquals(getColor(R.color.calculator_accent_color), window.statusBarColor)
        }

        tap(TEST_TAG_DIGIT_0, TEST_TAG_DIVIDE, TEST_TAG_DIGIT_0, TEST_TAG_EQUALS)
        composeRule.waitForIdle()

        composeRule.activity.run {
            @Suppress("DEPRECATION")
            assertEquals(getColor(R.color.calculator_error_color), window.statusBarColor)
        }
    }

    @Test
    fun primaryButtonsExposeTalkbackLabelAndTouchTarget() {
        val minTouchTargetPx = with(composeRule.density) { 48.dp.roundToPx() }
        val tags = listOf(
            TEST_TAG_DIGIT_0,
            TEST_TAG_DIGIT_1,
            TEST_TAG_DIGIT_2,
            TEST_TAG_DIGIT_3,
            TEST_TAG_DIGIT_4,
            TEST_TAG_DIGIT_5,
            TEST_TAG_DIGIT_6,
            TEST_TAG_DIGIT_7,
            TEST_TAG_DIGIT_8,
            TEST_TAG_DIGIT_9,
            TEST_TAG_DECIMAL,
            TEST_TAG_DIVIDE,
            TEST_TAG_MULTIPLY,
            TEST_TAG_SUBTRACT,
            TEST_TAG_ADD,
            TEST_TAG_EQUALS
        )

        tags.forEach { tag ->
            val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
            assertTrue(
                "Missing click semantics for $tag",
                node.config.contains(SemanticsActions.OnClick)
            )

            val hasContentDescription =
                node.config.contains(SemanticsProperties.ContentDescription) &&
                    node.config[SemanticsProperties.ContentDescription]
                        .any { contentDescription -> contentDescription.isNotEmpty() }
            assertTrue(
                "Missing TalkBack label for $tag",
                hasContentDescription
            )

            assertTrue(
                "Touch target for $tag is below 48dp minimum",
                node.size.width >= minTouchTargetPx && node.size.height >= minTouchTargetPx
            )
        }
    }

    private fun tap(vararg tags: String) {
        tags.forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
        }
    }
}
