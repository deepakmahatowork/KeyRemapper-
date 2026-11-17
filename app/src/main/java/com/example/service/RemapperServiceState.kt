package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedKeyEvent(
    val keyCode: Int,
    val scanCode: Int,
    val sourceLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
)

object RemapperServiceState {
    private val _isAccessibilityRunning = MutableStateFlow(false)
    val isAccessibilityRunning: StateFlow<Boolean> = _isAccessibilityRunning.asStateFlow()

    private val _activeForegroundPackage = MutableStateFlow("")
    val activeForegroundPackage: StateFlow<String> = _activeForegroundPackage.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _lastTriggeredBanner = MutableStateFlow<String?>(null)
    val lastTriggeredBanner: StateFlow<String?> = _lastTriggeredBanner.asStateFlow()

    private val _isCapturingKey = MutableStateFlow(false)
    val isCapturingKey: StateFlow<Boolean> = _isCapturingKey.asStateFlow()

    private val _capturedKeys = MutableSharedFlow<CapturedKeyEvent>(extraBufferCapacity = 16)
    val capturedKeys: SharedFlow<CapturedKeyEvent> = _capturedKeys.asSharedFlow()

    fun setAccessibilityRunning(running: Boolean) {
        _isAccessibilityRunning.value = running
    }

    fun setForegroundPackage(pkg: String) {
        _activeForegroundPackage.value = pkg
    }

    fun setTorchOn(on: Boolean) {
        _isTorchOn.value = on
    }

    fun setLastTriggeredBanner(message: String?) {
        _lastTriggeredBanner.value = message
    }

    fun setCapturingKey(capturing: Boolean) {
        _isCapturingKey.value = capturing
    }

    fun emitCapturedKey(event: CapturedKeyEvent) {
        _capturedKeys.tryEmit(event)
    }
}
