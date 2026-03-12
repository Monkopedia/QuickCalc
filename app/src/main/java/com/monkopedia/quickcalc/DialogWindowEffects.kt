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
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.annotation.RequiresApi
import java.util.function.Consumer

private const val DIALOG_WINDOW_STABILIZER_START_DELAY_MS = 1_250L
private const val DIALOG_WINDOW_STABILIZER_INTERVAL_MS = 700L
private const val BLUR_DIM_AMOUNT = 0.01f

@RequiresApi(Build.VERSION_CODES.N)
internal class DialogWindowEffects(private val context: Context) {

    private var blurEnabledListener: Consumer<Boolean>? = null
    private var lastAppliedBackgroundSignature: String? = null
    private var dialogWindowStabilizer: Runnable? = null
    private var dialogWindowFocusChangeListener:
        ViewTreeObserver.OnWindowFocusChangeListener? = null

    fun applyBackgroundEffect(
        dialog: ComponentDialog?,
        settings: TileSettings,
        force: Boolean = false,
        log: Boolean = true
    ) {
        val window = dialog?.window ?: return
        val blurSupported = isDialogBlurSupported(context)
        val mode = settings.dialogBackgroundMode
        val darkTheme = isDialogDarkTheme(settings, isSystemDark = isSystemNightMode())
        val overlayColor = dialogWindowOverlayColorArgb(mode, darkTheme)
        val blurRadius = dialogWindowBackgroundBlurRadiusPx(mode)
        val signature = "$mode|$darkTheme|$overlayColor|$blurRadius|$blurSupported"
        if (!force && signature == lastAppliedBackgroundSignature) {
            return
        }
        lastAppliedBackgroundSignature = signature
        window.setBackgroundDrawable(ColorDrawable(overlayColor))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (log && BuildConfig.DEBUG) {
                Log.i(
                    TAG,
                    "applyBackgroundEffect mode=$mode blurSupported=$blurSupported " +
                        "overlayAlpha=${android.graphics.Color.alpha(overlayColor)} " +
                        "blurRadius=$blurRadius api=${Build.VERSION.SDK_INT}"
                )
            }
            return
        }
        val attributes = window.attributes
        if (blurRadius > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(BLUR_DIM_AMOUNT)
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            attributes.blurBehindRadius = blurRadius
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            attributes.blurBehindRadius = 0
        }
        window.attributes = attributes
        window.setBackgroundBlurRadius(blurRadius)
        // Force a frame after blur attribute changes so the blur shows immediately.
        window.decorView.postInvalidateOnAnimation()
        val blurBehindEnabled =
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) != 0
        if (log && BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "applyBackgroundEffect mode=$mode blurSupported=$blurSupported " +
                    "overlayAlpha=${android.graphics.Color.alpha(overlayColor)} " +
                    "blurBehindRadius=${window.attributes.blurBehindRadius} " +
                    "blurBehindEnabled=$blurBehindEnabled"
            )
        }
    }

    fun startStabilizer(dialog: ComponentDialog, settings: TileSettings) {
        stopStabilizer(dialog)
        val runnable = object : Runnable {
            override fun run() {
                if (!dialog.isShowing) {
                    return
                }
                applyBackgroundEffect(
                    dialog = dialog,
                    settings = settings,
                    force = true,
                    log = false
                )
                dialog.window?.decorView?.postDelayed(this, DIALOG_WINDOW_STABILIZER_INTERVAL_MS)
            }
        }
        dialogWindowStabilizer = runnable
        dialog.window?.decorView?.postDelayed(runnable, DIALOG_WINDOW_STABILIZER_START_DELAY_MS)
    }

    fun stopStabilizer(dialog: ComponentDialog?) {
        val runnable = dialogWindowStabilizer ?: return
        dialog?.window?.decorView?.removeCallbacks(runnable)
        dialogWindowStabilizer = null
    }

    fun registerCrossWindowBlurListener(onChanged: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurEnabledListener != null) {
            return
        }
        val windowManager = context.getSystemService(WindowManager::class.java) ?: return
        val listener = Consumer<Boolean> { enabled ->
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "crossWindowBlurEnabled changed: $enabled")
            }
            onChanged()
        }
        blurEnabledListener = listener
        windowManager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
    }

    fun unregisterCrossWindowBlurListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        val listener = blurEnabledListener ?: return
        val windowManager = context.getSystemService(WindowManager::class.java) ?: return
        windowManager.removeCrossWindowBlurEnabledListener(listener)
        blurEnabledListener = null
    }

    fun installFocusBlurHook(dialog: ComponentDialog, settings: TileSettings) {
        removeFocusBlurHook(dialog)
        val decorView = dialog.window?.decorView ?: return
        decorView.isFocusableInTouchMode = true
        decorView.requestFocus()
        decorView.requestFocusFromTouch()
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "dialog_window_focus_changed hasFocus=$hasFocus")
            }
            if (hasFocus) {
                applyBackgroundEffect(dialog, settings, force = true)
            }
        }
        dialogWindowFocusChangeListener = listener
        decorView.viewTreeObserver.addOnWindowFocusChangeListener(listener)
    }

    fun removeFocusBlurHook(dialog: ComponentDialog) {
        val listener = dialogWindowFocusChangeListener ?: return
        val decorView = dialog.window?.decorView
        val observer = decorView?.viewTreeObserver
        if (observer != null && observer.isAlive) {
            observer.removeOnWindowFocusChangeListener(listener)
        }
        dialogWindowFocusChangeListener = null
    }

    fun clearSignature() {
        lastAppliedBackgroundSignature = null
    }

    private fun isSystemNightMode(): Boolean {
        val nightModeMask =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeMask == Configuration.UI_MODE_NIGHT_YES
    }

    private companion object {
        private const val TAG = "DialogWindowEffects"
    }
}
