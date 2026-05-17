package com.gesturepassword

import android.graphics.Color
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter

/**
 * Paper-style SimpleViewManager for PatternLockerNativeView.
 * Bridges JS properties and events to the native Kotlin view.
 * Works on both RN 0.65 (Paper) and RN 0.79+ (Fabric via compat layer).
 */
class PatternLockerViewManager : ViewGroupManager<PatternLockerNativeView>() {

    companion object {
        const val REACT_CLASS = "PatternLockerView"

        const val EVENT_ON_START = "onStartEvent"
        const val EVENT_ON_END = "onEndEvent"
        const val EVENT_ON_RESET = "onResetEvent"

    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(reactContext: ThemedReactContext): PatternLockerNativeView {
        return createViewInstanceInternal(reactContext)
    }

    /**
     * Internal implementation of createViewInstance, made public for testing.
     */
    fun createViewInstanceInternal(reactContext: ThemedReactContext): PatternLockerNativeView {
        return PatternLockerNativeView(reactContext).apply {

            onPatternStart = {
                emitEvent(reactContext, id, EVENT_ON_START, Arguments.createMap())
            }

            onPatternEnd = { password ->
                val event = Arguments.createMap().apply {
                    putString("password", password)
                }
                emitEvent(reactContext, id, EVENT_ON_END, event)
            }

            onPatternReset = {
                emitEvent(reactContext, id, EVENT_ON_RESET, Arguments.createMap())
            }
        }
    }

    private fun emitEvent(context: ThemedReactContext, viewId: Int, eventName: String, params: WritableMap) {
        context
            .getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(viewId, eventName, params)
    }

    // ========================
    // React Props
    // ========================

    /**
     * Maps JS status prop to native error state.
     * 'wrong' → isError = true; 'normal' or 'right' → false.
     */
    @ReactProp(name = "status")
    fun setStatus(view: PatternLockerNativeView, status: String?) {
        view.isError = status == "wrong"
    }

    @ReactProp(name = "normalColor")
    fun setNormalColor(view: PatternLockerNativeView, color: String?) {
        if (color != null) {
            try { view.normalColor = Color.parseColor(color) } catch(e: Exception) { }
        }
    }

    @ReactProp(name = "rightColor")
    fun setRightColor(view: PatternLockerNativeView, color: String?) {
        if (color != null) {
            try { view.hitColor = Color.parseColor(color) } catch(e: Exception) { }
        }
    }

    @ReactProp(name = "wrongColor")
    fun setWrongColor(view: PatternLockerNativeView, color: String?) {
        if (color != null) {
            try { view.errorColor = Color.parseColor(color) } catch(e: Exception) { }
        }
    }

    @ReactProp(name = "allowCross")
    fun setAllowCross(view: PatternLockerNativeView, allowCross: Boolean) {
        view.enableSkip = allowCross
    }

    @ReactProp(name = "interval")
    fun setInterval(view: PatternLockerNativeView, intervalMs: Int) {
        view.freezeDurationMs = intervalMs.toLong()
        view.enableAutoClean = intervalMs > 0
    }

    @ReactProp(name = "transparentLine")
    fun setTransparentLine(view: PatternLockerNativeView, transparent: Boolean) {
        view.transparentLine = transparent
    }

    @ReactProp(name = "innerCircle")
    fun setInnerCircle(view: PatternLockerNativeView, show: Boolean) {
        view.showInnerCircle = show
    }

    @ReactProp(name = "outerCircle")
    fun setOuterCircle(view: PatternLockerNativeView, show: Boolean) {
        view.showOuterCircle = show
    }

    // ========================
    // Event registration (required for Paper bridge)
    // ========================

    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any>? {
        return MapBuilder.builder<String, Any>()
            .put(EVENT_ON_START, MapBuilder.of("registrationName", EVENT_ON_START))
            .put(EVENT_ON_END, MapBuilder.of("registrationName", EVENT_ON_END))
            .put(EVENT_ON_RESET, MapBuilder.of("registrationName", EVENT_ON_RESET))
            .build()
    }
}
