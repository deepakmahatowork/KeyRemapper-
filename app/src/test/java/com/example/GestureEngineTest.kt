package com.example

import com.example.model.ButtonGesture
import com.example.service.GestureEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GestureEngineTest {

    @Test
    fun `fires single press immediately on release when no multi-tap rules are active`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { false },
            onGestureRecognized = { detected.add(it) },
        )

        engine.onKeyDown(0)
        advanceTimeBy(80)
        engine.onKeyUp()
        runCurrent()

        assertEquals(listOf(ButtonGesture.SINGLE_PRESS), detected)
    }

    @Test
    fun `detects single press after double tap timeout when multi-tap is active`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { true },
            onGestureRecognized = { detected.add(it) },
        )

        engine.onKeyDown(0)
        advanceTimeBy(80)
        engine.onKeyUp()
        runCurrent()
        assertEquals(emptyList<ButtonGesture>(), detected)

        advanceTimeBy(260)
        runCurrent()
        assertEquals(listOf(ButtonGesture.SINGLE_PRESS), detected)
    }

    @Test
    fun `detects double press within window`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { true },
            onGestureRecognized = { detected.add(it) },
        )

        engine.onKeyDown(0)
        advanceTimeBy(60)
        engine.onKeyUp()
        advanceTimeBy(100)
        engine.onKeyDown(0)
        advanceTimeBy(60)
        engine.onKeyUp()

        advanceTimeBy(260)
        runCurrent()
        assertEquals(listOf(ButtonGesture.DOUBLE_PRESS), detected)
    }

    @Test
    fun `detects triple press immediately on third tap`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { true },
            onGestureRecognized = { detected.add(it) },
        )

        repeat(3) {
            engine.onKeyDown(0)
            advanceTimeBy(50)
            engine.onKeyUp()
            advanceTimeBy(80)
        }
        runCurrent()
        assertEquals(listOf(ButtonGesture.TRIPLE_PRESS), detected)
    }

    @Test
    fun `detects long press when held past threshold`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { false },
            onGestureRecognized = { detected.add(it) },
        )

        engine.onKeyDown(0)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf(ButtonGesture.LONG_PRESS), detected)

        engine.onKeyUp()
        advanceTimeBy(300)
        runCurrent()
        assertEquals(listOf(ButtonGesture.LONG_PRESS), detected)
    }

    @Test
    fun `recovers when previous ACTION_UP was dropped`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        var nowMs = 1_000L
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { false },
            clockMsProvider = { nowMs },
            onGestureRecognized = { detected.add(it) },
        )

        // First press has ACTION_UP dropped by OS
        engine.onKeyDown(0)
        nowMs += 150L
        advanceTimeBy(150)

        // Subsequent fresh press self-heals and fires single press cleanly
        engine.onKeyDown(0)
        nowMs += 60L
        advanceTimeBy(60)
        engine.onKeyUp()
        runCurrent()

        assertEquals(listOf(ButtonGesture.SINGLE_PRESS), detected)
    }

    @Test
    fun `ignores canceled key up events without firing single press`() = runTest {
        val detected = mutableListOf<ButtonGesture>()
        val engine = GestureEngine(
            scope = this,
            doublePressDelayMsProvider = { 260L },
            longPressThresholdMsProvider = { 500L },
            hasMultiTapRulesProvider = { false },
            onGestureRecognized = { detected.add(it) },
        )

        engine.onKeyDown(0)
        advanceTimeBy(60)
        engine.onKeyUp(canceled = true)
        runCurrent()

        assertEquals(emptyList<ButtonGesture>(), detected)
    }
}
