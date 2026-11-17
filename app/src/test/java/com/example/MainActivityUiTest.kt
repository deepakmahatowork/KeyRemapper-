package com.example

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app launches and renders remap and setup tabs`() {
        composeRule.onNodeWithTag("test_gesture_single_press").assertExists()
        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("global_single_press_card"))
        composeRule.onNodeWithTag("global_single_press_card").assertExists()

        composeRule.onNodeWithTag("nav_setup").performClick()
        composeRule.onNodeWithTag("setup_accessibility_button").assertExists()
    }
}
