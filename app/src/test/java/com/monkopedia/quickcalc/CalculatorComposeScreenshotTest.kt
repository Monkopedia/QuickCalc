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

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import java.util.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CalculatorComposeScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "src/test/screenshots/compose",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.01f,
                    imageComparator = SimpleImageComparator(
                        maxDistance = 0.007f,
                        hShift = 1,
                        vShift = 1
                    )
                )
            )
        )
    )

    private lateinit var context: Context

    @Before
    fun configureDeterministicRendering() {
        context = ApplicationProvider.getApplicationContext()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        disableAnimations()
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_LIGHT_QUALIFIERS)
    fun phonePortraitLightInitial() {
        captureCalculatorCompose("phone_portrait_light_initial")
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_LIGHT_QUALIFIERS)
    fun phonePortraitLightDrawerOpenInitial() {
        captureCalculatorCompose(
            screenshotName = "phone_portrait_light_drawer_open_initial",
            initialPadPage = 1
        )
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_LIGHT_QUALIFIERS)
    fun phonePortraitLightAdditionResult() {
        captureCalculatorCompose(
            screenshotName = "phone_portrait_light_addition",
            initialFormula = "1+2",
            evaluateInitialExpression = true
        )
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_DARK_QUALIFIERS)
    fun phonePortraitDarkInitial() {
        captureCalculatorCompose("phone_portrait_dark_initial")
    }

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE_LIGHT_QUALIFIERS)
    fun phoneLandscapeLightInitial() {
        captureCalculatorCompose("phone_landscape_light_initial")
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT_LIGHT_QUALIFIERS)
    fun tabletPortraitLightInitial() {
        captureCalculatorCompose("tablet_portrait_light_initial")
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT_DARK_QUALIFIERS)
    fun tabletPortraitDarkInitial() {
        captureCalculatorCompose("tablet_portrait_dark_initial")
    }

    @Test
    @Config(qualifiers = WINDOWED_LIGHT_QUALIFIERS)
    fun windowedLightInitial() {
        captureCalculatorCompose("windowed_light_initial")
    }

    @Test
    @Config(qualifiers = WINDOWED_MEDIUM_LIGHT_QUALIFIERS)
    fun windowedMediumLightInitial() {
        captureCalculatorCompose("windowed_medium_light_initial")
    }

    @Test
    @Config(qualifiers = WINDOWED_EXPANDED_LIGHT_QUALIFIERS)
    fun windowedExpandedLightInitial() {
        captureCalculatorCompose("windowed_expanded_light_initial")
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_LIGHT_QUALIFIERS)
    fun phonePortraitLargeFontInitial() {
        withFontScale(1.3f) {
            captureCalculatorCompose("phone_portrait_large_font_initial")
        }
    }

    private fun captureCalculatorCompose(
        screenshotName: String,
        initialFormula: String = "",
        evaluateInitialExpression: Boolean = false,
        initialPadPage: Int = 0
    ) {
        val scenario = ActivityScenario.launch<CalculatorComposeActivity>(
            CalculatorComposeActivity.newIntent(
                context = context,
                initialFormula = initialFormula,
                evaluateInitialExpression = evaluateInitialExpression,
                initialPadPage = initialPadPage
            )
        )
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(isRoot()).captureRoboImage(filePath = "$screenshotName.png")
        } finally {
            scenario.close()
        }
    }

    private fun disableAnimations() {
        val contentResolver = context.contentResolver
        Settings.Global.putFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Settings.Global.putFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        Settings.Global.putFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
    }

    @Suppress("DEPRECATION")
    private fun withFontScale(fontScale: Float, block: () -> Unit) {
        val resources = context.resources
        val originalConfiguration = Configuration(resources.configuration)
        val updatedConfiguration = Configuration(resources.configuration)
        updatedConfiguration.fontScale = fontScale
        resources.updateConfiguration(updatedConfiguration, resources.displayMetrics)
        try {
            block()
        } finally {
            resources.updateConfiguration(originalConfiguration, resources.displayMetrics)
        }
    }

    companion object {
        private const val PHONE_PORTRAIT_LIGHT_QUALIFIERS =
            "en-rUS-w411dp-h891dp-port-notnight"
        private const val PHONE_PORTRAIT_DARK_QUALIFIERS =
            "en-rUS-w411dp-h891dp-port-night"
        private const val PHONE_LANDSCAPE_LIGHT_QUALIFIERS =
            "en-rUS-w891dp-h411dp-land-notnight"
        private const val TABLET_PORTRAIT_LIGHT_QUALIFIERS =
            "en-rUS-w800dp-h1280dp-port-notnight"
        private const val TABLET_PORTRAIT_DARK_QUALIFIERS =
            "en-rUS-w800dp-h1280dp-port-night"
        private const val WINDOWED_LIGHT_QUALIFIERS =
            "en-rUS-w540dp-h380dp-land-notnight"
        private const val WINDOWED_MEDIUM_LIGHT_QUALIFIERS =
            "en-rUS-w700dp-h500dp-land-notnight"
        private const val WINDOWED_EXPANDED_LIGHT_QUALIFIERS =
            "en-rUS-w960dp-h700dp-land-notnight"
    }
}
