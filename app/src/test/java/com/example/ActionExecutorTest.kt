package com.example

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.ActionExecutor
import com.example.ui.AssistTriggerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActionExecutorTest {

    @Test
    fun `power long press assist enabled for assistant setting or Nothing default`() {
        assertTrue(ActionExecutor.shouldUsePowerLongPressAssist(5, "Google", "Pixel"))
        assertTrue(ActionExecutor.shouldUsePowerLongPressAssist(4, "Google", "Pixel"))
        assertTrue(ActionExecutor.shouldUsePowerLongPressAssist(-1, "Nothing", "Nothing"))
        assertTrue(ActionExecutor.shouldUsePowerLongPressAssist(-1, "Nothing", "CMF"))
        assertFalse(ActionExecutor.shouldUsePowerLongPressAssist(1, "Nothing", "Nothing"))
        assertFalse(ActionExecutor.shouldUsePowerLongPressAssist(-1, "Google", "Pixel"))
    }

    @Test
    fun `showAssist trigger intent launches AssistTriggerActivity with power long press invocation args`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val triggerIntent = ActionExecutor.buildShowAssistTriggerIntent(context)

        assertEquals(AssistTriggerActivity::class.java.name, triggerIntent.component?.className)
        assertTrue((triggerIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue((triggerIntent.flags and Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0)

        val args = AssistTriggerActivity.buildAssistArgs()
        assertEquals(6, args.getInt("invocation_type"))
        assertEquals(6, args.getInt("com.android.systemui.assist.invocation_type"))
    }

    @Test
    fun `fallback assistant candidates prioritize Gemini and never target googlequicksearchbox`() {
        val candidates = ActionExecutor.buildAssistantIntentCandidates(
            assistantPackage = "com.google.android.googlequicksearchbox",
        )

        assertTrue(candidates.all { it.action == Intent.ACTION_ASSIST })
        assertEquals("com.google.android.apps.bard", candidates.first().`package`)
        assertNull(candidates.last().`package`)
        assertTrue(candidates.none { it.`package` == "com.google.android.googlequicksearchbox" })
        assertTrue(candidates.all { (it.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0 })
        assertTrue(candidates.all { it.getIntExtra("invocation_type", -1) == 6 })
    }
}
