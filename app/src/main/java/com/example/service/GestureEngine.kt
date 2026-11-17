package com.example.service

import com.example.model.ButtonGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Multi-gesture state machine distinguishing Single, Double, Triple, and Long presses.
 * Fires SINGLE_PRESS immediately with 0ms delay when no Double/Triple press rules are active,
 * and self-heals if an ACTION_UP event is ever dropped by the OS.
 */
class GestureEngine(
    private val scope: CoroutineScope,
    private val doublePressDelayMsProvider: () -> Long,
    private val longPressThresholdMsProvider: () -> Long,
    private val hasMultiTapRulesProvider: () -> Boolean = { true },
    private val clockMsProvider: () -> Long = { System.currentTimeMillis() },
    private val onGestureRecognized: (ButtonGesture) -> Unit,
) {
    private var tapCount = 0
    private var isPressed = false
    private var pressStartedAtMs = 0L
    private var longPressTriggered = false
    private var longPressJob: Job? = null
    private var tapFinalizeJob: Job? = null

    fun onKeyDown(repeatCount: Int) {
        if (repeatCount > 0) return

        val threshold = longPressThresholdMsProvider().coerceIn(300L, 1200L)
        val now = clockMsProvider()
        if (isPressed) {
            // Self-heal immediately if a previous ACTION_UP was dropped by the OS (ignore <50ms bounce)
            if (now - pressStartedAtMs < 50L) {
                return
            }
            reset()
        }

        isPressed = true
        pressStartedAtMs = now
        longPressTriggered = false
        tapFinalizeJob?.cancel()

        longPressJob?.cancel()
        longPressJob = scope.launch {
            delay(threshold)
            if (isPressed && !longPressTriggered) {
                longPressTriggered = true
                tapCount = 0
                onGestureRecognized(ButtonGesture.LONG_PRESS)
            }
        }
    }

    fun onKeyUp(canceled: Boolean = false) {
        if (!isPressed) return
        isPressed = false
        longPressJob?.cancel()

        if (canceled) {
            longPressTriggered = false
            tapCount = 0
            return
        }

        if (longPressTriggered) {
            longPressTriggered = false
            tapCount = 0
            return
        }

        if (!hasMultiTapRulesProvider()) {
            tapCount = 0
            tapFinalizeJob?.cancel()
            onGestureRecognized(ButtonGesture.SINGLE_PRESS)
            return
        }

        tapCount += 1
        if (tapCount >= 3) {
            tapFinalizeJob?.cancel()
            tapCount = 0
            onGestureRecognized(ButtonGesture.TRIPLE_PRESS)
            return
        }

        val currentTapCount = tapCount
        val delayMs = doublePressDelayMsProvider().coerceIn(150L, 600L)
        tapFinalizeJob?.cancel()
        tapFinalizeJob = scope.launch {
            delay(delayMs)
            if (tapCount == currentTapCount) {
                tapCount = 0
                when (currentTapCount) {
                    1 -> onGestureRecognized(ButtonGesture.SINGLE_PRESS)
                    2 -> onGestureRecognized(ButtonGesture.DOUBLE_PRESS)
                    else -> onGestureRecognized(ButtonGesture.TRIPLE_PRESS)
                }
            }
        }
    }

    fun reset() {
        isPressed = false
        pressStartedAtMs = 0L
        longPressTriggered = false
        tapCount = 0
        longPressJob?.cancel()
        tapFinalizeJob?.cancel()
    }
}
