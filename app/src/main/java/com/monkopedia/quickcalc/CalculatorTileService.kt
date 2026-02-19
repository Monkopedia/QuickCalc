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

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class CalculatorTileService : TileService() {

    companion object {
        private const val TAG = "CalculatorTileService"
        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

        @Volatile private var activeDialogRef: WeakReference<ComponentDialog>? = null

        fun dismissActiveDialogFromTimeout() {
            mainHandler.post {
                val dialog = activeDialogRef?.get()
                if (dialog?.isShowing == true) {
                    dialog.dismiss()
                }
            }
        }
    }

    private val settingsRepository by lazy { TileSettingsRepository(applicationContext) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeDialog: ComponentDialog? = null
    private var latestCalculatorState: CalculatorUiState = CalculatorUiState()
    private var latestDynamicTransform: DynamicTransform? = null
    private var cachedSettings: TileSettings = TileSettings()
    private var lastPriorityRefreshElapsedRealtimeMs: Long = 0L
    private var inactivityCloseJob: Job? = null
    private var blurEnabledListener: Consumer<Boolean>? = null
    private var lastAppliedBackgroundSignature: String? = null
    private var dialogWindowStabilizer: Runnable? = null
    private var dialogWindowFocusChangeListener:
        ViewTreeObserver.OnWindowFocusChangeListener? = null
    private var hasLoadedSettingsSnapshot: Boolean = false
    private val autosaveSignals = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var lastPersistedTransform: DynamicTransform? = null
    private var lastPersistedCalculatorState: CalculatorUiState? = null
    private var lastPersistedRememberState: Boolean? = null

    override fun onCreate() {
        super.onCreate()
        trace("onCreate")
        registerCrossWindowBlurListener()
        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val backgroundChanged =
                    cachedSettings.dialogBackgroundMode != settings.dialogBackgroundMode ||
                        cachedSettings.themeMode != settings.themeMode
                val timeoutChanged =
                    cachedSettings.dialogInactivityTimeoutSeconds !=
                        settings.dialogInactivityTimeoutSeconds
                val rememberStateChanged =
                    cachedSettings.rememberCalculatorState != settings.rememberCalculatorState
                cachedSettings = settings
                hasLoadedSettingsSnapshot = true
                if (backgroundChanged) {
                    applyDialogWindowBackgroundEffect(activeDialog, settings, force = true)
                }
                if (timeoutChanged && activeDialog?.isShowing == true) {
                    scheduleInactivityAutoClose()
                }
                if (rememberStateChanged) {
                    scheduleAutosave()
                }
            }
        }
        serviceScope.launch {
            autosaveSignals
                .collectLatest {
                    delay(AUTOSAVE_DEBOUNCE_MS)
                    withContext(Dispatchers.IO) {
                        persistAutosaveSnapshot()
                    }
                }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        trace("onStartListening")
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.quick_settings_tile_label)
            icon = Icon.createWithResource(this@CalculatorTileService, R.drawable.fly_calc)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        trace("onClick")
        if (activeDialog?.isShowing == true) {
            trace("onClick_ignored_dialog_showing")
            return
        }
        runCatching {
            showCalculatorDialog()
            refreshPriorityShortService(force = true)
            trace("onClick_showDialog_requested")
        }.onFailure { throwable ->
            Log.w(TAG, "Unable to show Quick Settings calculator dialog", throwable)
            CalculatorTilePriorityService.stop(this)
            trace("onClick_failure")
        }
    }

    override fun onDestroy() {
        trace("onDestroy")
        unregisterCrossWindowBlurListener()
        inactivityCloseJob?.cancel()
        inactivityCloseJob = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showCalculatorDialog() {
        ensureSettingsLoadedForDialog()
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val settings by settingsRepository.settingsFlow.collectAsState(
                    initial = cachedSettings
                )
                val darkTheme = isDialogDarkTheme(settings)
                val palette = calculatorDialogPalette(settings, darkTheme)
                TileSettingsTheme(settings = settings) {
                    var requestedPadPage by remember { mutableStateOf<Int?>(null) }
                    val restoredState =
                        if (settings.rememberCalculatorState) {
                            settings.savedCalculatorState
                        } else {
                            CalculatorUiState()
                        }
                    TileCalculatorDialogLayout(
                        settings = settings,
                        onDynamicTransformChange = { scale, xFraction, yFraction ->
                            latestDynamicTransform = DynamicTransform(scale, xFraction, yFraction)
                        },
                        onDynamicTransformSettled = {
                            scheduleAutosave()
                            recordUserInteraction()
                        },
                        onAnyUserInteraction = { recordUserInteraction() },
                        onRequestDismiss = { activeDialog?.dismiss() }
                    ) { contentModifier ->
                        CalculatorComposeRoute(
                            modifier = contentModifier,
                            initialUiState = restoredState,
                            colorPalette = palette,
                            enableDisplayClipboardGestures = true,
                            showDrawerShortcutButton = settings.sizeMode == TileSizeMode.DYNAMIC,
                            padPageOverride = requestedPadPage,
                            onPadPageOverrideConsumed = { requestedPadPage = null },
                            onRequestPadPage = {
                                requestedPadPage = it
                                recordUserInteraction()
                            },
                            onCalculatorStateChange = { state ->
                                latestCalculatorState = state
                                scheduleAutosave()
                                recordUserInteraction()
                            }
                        )
                    }
                }
            }
        }

        val dialog = ComponentDialog(this, R.style.CalculatorTileDialogTheme).apply {
            setContentView(
                composeView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            setOnShowListener {
                trace("dialog_onShow")
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                window?.setWindowAnimations(0)
                window?.decorView?.setPadding(0, 0, 0, 0)
                window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
                applyDialogWindowBackgroundEffect(this, cachedSettings, force = true)
                installWindowFocusBlurHook(this)
                // Re-apply after attachment/animation settles to avoid first-frame drops.
                window?.decorView?.post {
                    if (isShowing) {
                        applyDialogWindowBackgroundEffect(this, cachedSettings, force = true)
                    }
                }
                mainHandler.postDelayed({
                    if (isShowing) {
                        applyDialogWindowBackgroundEffect(this, cachedSettings, force = true)
                    }
                }, 90L)
                startDialogWindowStabilizer(this)
                scheduleInactivityAutoClose()
            }
            setOnDismissListener {
                trace("dialog_onDismiss")
                stopDialogWindowStabilizer()
                removeWindowFocusBlurHook(this)
                if (activeDialog === this) {
                    activeDialog = null
                }
                if (activeDialogRef?.get() === this) {
                    activeDialogRef = null
                }
                lastAppliedBackgroundSignature = null
                inactivityCloseJob?.cancel()
                inactivityCloseJob = null
                runCatching {
                    runBlocking {
                        withContext(Dispatchers.IO + NonCancellable) {
                            persistAutosaveSnapshot(force = true)
                        }
                    }
                }.onFailure { throwable ->
                    Log.w(TAG, "Failed to persist tile snapshot during dismiss", throwable)
                }
                CalculatorTilePriorityService.stop(this@CalculatorTileService)
            }
        }
        applyDialogWindowBackgroundEffect(dialog, cachedSettings, force = true)
        activeDialog = dialog
        activeDialogRef = WeakReference(dialog)
        trace("showDialog")
        runCatching {
            showDialog(dialog)
        }.getOrElse { throwable ->
            if (activeDialog === dialog) {
                activeDialog = null
            }
            if (activeDialogRef?.get() === dialog) {
                activeDialogRef = null
            }
            throw throwable
        }
    }

    private fun ensureSettingsLoadedForDialog() {
        if (hasLoadedSettingsSnapshot) {
            return
        }
        val snapshot = runBlocking {
            withContext(Dispatchers.IO) {
                settingsRepository.snapshot()
            }
        }
        cachedSettings = snapshot
        hasLoadedSettingsSnapshot = true
    }

    private fun trace(event: String) {
        Log.i(TAG, "TRACE event=$event t=${SystemClock.elapsedRealtimeNanos()}")
    }

    private fun scheduleAutosave() {
        autosaveSignals.tryEmit(Unit)
    }

    private fun recordUserInteraction() {
        scheduleInactivityAutoClose()
        refreshPriorityShortService()
    }

    private fun refreshPriorityShortService(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force &&
            (now - lastPriorityRefreshElapsedRealtimeMs) < PRIORITY_REFRESH_MIN_INTERVAL_MS
        ) {
            return
        }
        lastPriorityRefreshElapsedRealtimeMs = now
        CalculatorTilePriorityService.start(this)
    }

    private suspend fun persistAutosaveSnapshot(force: Boolean = false) {
        val transformSnapshot = latestDynamicTransform
        val calculatorStateSnapshot = latestCalculatorState
        val rememberStateSnapshot = cachedSettings.rememberCalculatorState

        if (transformSnapshot != null &&
            (force || transformSnapshot != lastPersistedTransform)
        ) {
            settingsRepository.setDynamicTransform(
                scale = transformSnapshot.scale,
                offsetXFraction = transformSnapshot.offsetXFraction,
                offsetYFraction = transformSnapshot.offsetYFraction
            )
            lastPersistedTransform = transformSnapshot
        }

        if (rememberStateSnapshot) {
            if (force ||
                calculatorStateSnapshot != lastPersistedCalculatorState ||
                lastPersistedRememberState != true
            ) {
                settingsRepository.saveCalculatorState(calculatorStateSnapshot)
                lastPersistedCalculatorState = calculatorStateSnapshot
            }
            lastPersistedRememberState = true
            return
        }

        if (force || lastPersistedRememberState != false) {
            settingsRepository.clearCalculatorState()
            lastPersistedCalculatorState = CalculatorUiState()
            lastPersistedRememberState = false
        }
    }

    private fun scheduleInactivityAutoClose() {
        inactivityCloseJob?.cancel()
        val timeoutSeconds = cachedSettings.dialogInactivityTimeoutSeconds
        if (timeoutSeconds <= DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS) {
            inactivityCloseJob = null
            return
        }
        inactivityCloseJob = serviceScope.launch {
            delay(timeoutSeconds * 1000L)
            val dialog = activeDialog
            if (dialog?.isShowing == true) {
                trace("dialog_inactivity_timeout")
                dialog.dismiss()
            }
        }
    }

    private fun applyDialogWindowBackgroundEffect(
        dialog: ComponentDialog?,
        settings: TileSettings,
        force: Boolean = false,
        log: Boolean = true
    ) {
        val window = dialog?.window ?: return
        val blurSupported = isDialogBlurSupported(this)
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
            if (log) {
                Log.i(
                    TAG,
                    "applyDialogWindowBackgroundEffect mode=$mode blurSupported=$blurSupported " +
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
        if (log) {
            Log.i(
                TAG,
                "applyDialogWindowBackgroundEffect mode=$mode blurSupported=$blurSupported " +
                    "overlayAlpha=${android.graphics.Color.alpha(overlayColor)} " +
                    "blurBehindRadius=${window.attributes.blurBehindRadius} " +
                    "blurBehindEnabled=$blurBehindEnabled"
            )
        }
    }

    private fun startDialogWindowStabilizer(dialog: ComponentDialog) {
        stopDialogWindowStabilizer()
        val runnable = object : Runnable {
            override fun run() {
                if (!dialog.isShowing) {
                    return
                }
                applyDialogWindowBackgroundEffect(
                    dialog = dialog,
                    settings = cachedSettings,
                    force = true,
                    log = false
                )
                dialog.window?.decorView?.postDelayed(this, DIALOG_WINDOW_STABILIZER_INTERVAL_MS)
            }
        }
        dialogWindowStabilizer = runnable
        dialog.window?.decorView?.postDelayed(runnable, DIALOG_WINDOW_STABILIZER_START_DELAY_MS)
    }

    private fun stopDialogWindowStabilizer() {
        val runnable = dialogWindowStabilizer ?: return
        activeDialog?.window?.decorView?.removeCallbacks(runnable)
        activeDialogRef?.get()?.window?.decorView?.removeCallbacks(runnable)
        dialogWindowStabilizer = null
    }

    private fun registerCrossWindowBlurListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurEnabledListener != null) {
            return
        }
        val windowManager = getSystemService(WindowManager::class.java) ?: return
        val listener = Consumer<Boolean> { enabled ->
            Log.i(TAG, "crossWindowBlurEnabled changed: $enabled")
            applyDialogWindowBackgroundEffect(activeDialog, cachedSettings, force = true)
        }
        blurEnabledListener = listener
        windowManager.addCrossWindowBlurEnabledListener(mainExecutor, listener)
    }

    private fun unregisterCrossWindowBlurListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        val listener = blurEnabledListener ?: return
        val windowManager = getSystemService(WindowManager::class.java) ?: return
        windowManager.removeCrossWindowBlurEnabledListener(listener)
        blurEnabledListener = null
    }

    private fun isSystemNightMode(): Boolean {
        val nightModeMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeMask == Configuration.UI_MODE_NIGHT_YES
    }

    private fun installWindowFocusBlurHook(dialog: ComponentDialog) {
        removeWindowFocusBlurHook(dialog)
        val decorView = dialog.window?.decorView ?: return
        decorView.isFocusableInTouchMode = true
        decorView.requestFocus()
        decorView.requestFocusFromTouch()
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            Log.i(TAG, "dialog_window_focus_changed hasFocus=$hasFocus")
            if (hasFocus) {
                applyDialogWindowBackgroundEffect(dialog, cachedSettings, force = true)
            }
        }
        dialogWindowFocusChangeListener = listener
        decorView.viewTreeObserver.addOnWindowFocusChangeListener(listener)
    }

    private fun removeWindowFocusBlurHook(dialog: ComponentDialog) {
        val listener = dialogWindowFocusChangeListener ?: return
        val decorView = dialog.window?.decorView
        val observer = decorView?.viewTreeObserver
        if (observer != null && observer.isAlive) {
            observer.removeOnWindowFocusChangeListener(listener)
        }
        dialogWindowFocusChangeListener = null
    }
}

internal data class DynamicTransform(
    val scale: Float,
    val offsetXFraction: Float,
    val offsetYFraction: Float
)

private const val DYNAMIC_OFFSCREEN_DRAG_FRACTION = 0.22f
private const val AUTOSAVE_DEBOUNCE_MS = 180L
private const val PRIORITY_REFRESH_MIN_INTERVAL_MS = 15_000L
private const val DIALOG_WINDOW_STABILIZER_START_DELAY_MS = 1_250L
private const val DIALOG_WINDOW_STABILIZER_INTERVAL_MS = 700L
private const val BLUR_DIM_AMOUNT = 0.01f
private val STATIC_EDGE_MARGIN_DP = 12.dp

internal fun dynamicMaxOffsetFraction(baseFraction: Float, scale: Float): Float =
    (((1f - (baseFraction * scale)) / 2f) + DYNAMIC_OFFSCREEN_DRAG_FRACTION)
        .coerceAtLeast(DYNAMIC_OFFSCREEN_DRAG_FRACTION)

internal fun applyDynamicGestureDelta(
    current: DynamicTransform,
    zoomDelta: Float,
    panXPx: Float,
    panYPx: Float,
    baseFraction: Float,
    containerWidthPx: Float,
    containerHeightPx: Float
): DynamicTransform {
    val newScale = (current.scale * zoomDelta).coerceIn(MIN_DIALOG_SCALE, MAX_DIALOG_SCALE)
    val panXFraction = if (containerWidthPx == 0f) 0f else panXPx / containerWidthPx
    val panYFraction = if (containerHeightPx == 0f) 0f else panYPx / containerHeightPx
    val newMaxX = dynamicMaxOffsetFraction(baseFraction, newScale)
    val newMaxY = dynamicMaxOffsetFraction(baseFraction, newScale)
    return DynamicTransform(
        scale = newScale,
        offsetXFraction = (current.offsetXFraction + panXFraction).coerceIn(-newMaxX, newMaxX),
        offsetYFraction = (current.offsetYFraction + panYFraction).coerceIn(-newMaxY, newMaxY)
    )
}

@Composable
private fun TileCalculatorDialogLayout(
    settings: TileSettings,
    onDynamicTransformChange: (scale: Float, xFraction: Float, yFraction: Float) -> Unit,
    onDynamicTransformSettled: (() -> Unit)?,
    onAnyUserInteraction: (() -> Unit)?,
    onRequestDismiss: (() -> Unit)?,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val baseFraction = settings.staticSizeFraction.coerceIn(
            MIN_STATIC_SIZE_FRACTION,
            MAX_STATIC_SIZE_FRACTION
        )
        val panelWidth = maxWidth * baseFraction
        val panelHeight = maxHeight * baseFraction

        if (settings.sizeMode == TileSizeMode.DYNAMIC) {
            var scale by remember(settings.dynamicScale) {
                mutableFloatStateOf(
                    settings.dynamicScale.coerceIn(MIN_DIALOG_SCALE, MAX_DIALOG_SCALE)
                )
            }
            var offsetXFraction by remember(settings.dynamicOffsetXFraction) {
                mutableFloatStateOf(settings.dynamicOffsetXFraction)
            }
            var offsetYFraction by remember(settings.dynamicOffsetYFraction) {
                mutableFloatStateOf(settings.dynamicOffsetYFraction)
            }

            val maxOffsetXFraction = dynamicMaxOffsetFraction(baseFraction, scale)
            val maxOffsetYFraction = dynamicMaxOffsetFraction(baseFraction, scale)
            offsetXFraction = offsetXFraction.coerceIn(-maxOffsetXFraction, maxOffsetXFraction)
            offsetYFraction = offsetYFraction.coerceIn(-maxOffsetYFraction, maxOffsetYFraction)
            val applyPanAndScale = { zoomDelta: Float, panXPx: Float, panYPx: Float ->
                val updated =
                    applyDynamicGestureDelta(
                        current = DynamicTransform(scale, offsetXFraction, offsetYFraction),
                        zoomDelta = zoomDelta,
                        panXPx = panXPx,
                        panYPx = panYPx,
                        baseFraction = baseFraction,
                        containerWidthPx = containerWidthPx,
                        containerHeightPx = containerHeightPx
                    )
                scale = updated.scale
                offsetXFraction = updated.offsetXFraction
                offsetYFraction = updated.offsetYFraction
                onDynamicTransformChange(scale, offsetXFraction, offsetYFraction)
            }
            val outsideDismissModifier =
                if (onRequestDismiss != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRequestDismiss
                    )
                } else {
                    Modifier
                }
            val consumePanelTapModifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            Box(
                modifier = Modifier
                    .pointerInput(onAnyUserInteraction) {
                        if (onAnyUserInteraction != null) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        break
                                    }
                                }
                                onAnyUserInteraction.invoke()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.none { it.pressed }) {
                                        break
                                    }
                                }
                            }
                        }
                    }
                    .fillMaxSize()
                    .clipToBounds()
                    .then(outsideDismissModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(panelWidth)
                        .height(panelHeight)
                        .pointerInput(baseFraction, containerWidthPx, containerHeightPx) {
                            awaitEachGesture {
                                var didTransform = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activeChanges = event.changes.filter { it.pressed }
                                    if (activeChanges.isEmpty()) {
                                        break
                                    }
                                    val pan =
                                        if (activeChanges.size == 1) {
                                            activeChanges.first().positionChange()
                                        } else {
                                            event.calculatePan()
                                        }
                                    val zoom =
                                        if (activeChanges.size > 1) {
                                            event.calculateZoom()
                                        } else {
                                            1f
                                        }
                                    if (pan.x != 0f || pan.y != 0f || zoom != 1f) {
                                        applyPanAndScale(zoom, pan.x, pan.y)
                                        didTransform = true
                                        activeChanges.forEach { it.consume() }
                                    }
                                }
                                if (didTransform) {
                                    onDynamicTransformSettled?.invoke()
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = containerWidthPx * offsetXFraction
                            translationY = containerHeightPx * offsetYFraction
                        }
                        .then(consumePanelTapModifier)
                ) {
                    Surface(shape = RoundedCornerShape(28.dp)) {
                        content(Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            val outsideDismissModifier =
                if (onRequestDismiss != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRequestDismiss
                    )
                } else {
                    Modifier
                }
            Box(
                modifier = Modifier
                    .pointerInput(onAnyUserInteraction) {
                        if (onAnyUserInteraction != null) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        break
                                    }
                                }
                                onAnyUserInteraction.invoke()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.none { it.pressed }) {
                                        break
                                    }
                                }
                            }
                        }
                    }
                    .fillMaxSize()
                    .then(outsideDismissModifier),
                contentAlignment = staticAlignmentToCompose(settings.staticAlignment)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(STATIC_EDGE_MARGIN_DP)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = baseFraction
                                scaleY = baseFraction
                                transformOrigin =
                                    staticAlignmentToTransformOrigin(settings.staticAlignment)
                            }
                    ) {
                        Surface(shape = RoundedCornerShape(28.dp)) {
                            content(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

private fun staticAlignmentToCompose(alignment: TileStaticAlignment): Alignment = when (alignment) {
    TileStaticAlignment.TOP_START -> Alignment.TopStart
    TileStaticAlignment.TOP_CENTER -> Alignment.TopCenter
    TileStaticAlignment.TOP_END -> Alignment.TopEnd
    TileStaticAlignment.CENTER_START -> Alignment.CenterStart
    TileStaticAlignment.CENTER -> Alignment.Center
    TileStaticAlignment.CENTER_END -> Alignment.CenterEnd
    TileStaticAlignment.BOTTOM_START -> Alignment.BottomStart
    TileStaticAlignment.BOTTOM_CENTER -> Alignment.BottomCenter
    TileStaticAlignment.BOTTOM_END -> Alignment.BottomEnd
}

private fun staticAlignmentToTransformOrigin(alignment: TileStaticAlignment): TransformOrigin =
    when (alignment) {
        TileStaticAlignment.TOP_START -> TransformOrigin(0f, 0f)
        TileStaticAlignment.TOP_CENTER -> TransformOrigin(0.5f, 0f)
        TileStaticAlignment.TOP_END -> TransformOrigin(1f, 0f)
        TileStaticAlignment.CENTER_START -> TransformOrigin(0f, 0.5f)
        TileStaticAlignment.CENTER -> TransformOrigin(0.5f, 0.5f)
        TileStaticAlignment.CENTER_END -> TransformOrigin(1f, 0.5f)
        TileStaticAlignment.BOTTOM_START -> TransformOrigin(0f, 1f)
        TileStaticAlignment.BOTTOM_CENTER -> TransformOrigin(0.5f, 1f)
        TileStaticAlignment.BOTTOM_END -> TransformOrigin(1f, 1f)
    }
