package com.example.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/**
 * Zero-frame transparent trampoline Activity that invokes the active Digital Assistant
 * VoiceInteractionSession (Gemini floating voice overlay with microphone input) via
 * [Activity.showAssist], which Android permits for the top focused Activity without root or Shizuku.
 */
class AssistTriggerActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var assistRequested = false
    private val finishRunnable = Runnable { finishSilently() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        // Safety timeout in case window focus is delayed or stolen
        handler.postDelayed(finishRunnable, 900L)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !assistRequested) {
            assistRequested = true
            val invoked = triggerSystemVoiceSession()
            if (!invoked) {
                launchGeminiFallback()
            }
            // Close immediately after the overlay steals focus, or after a brief handoff delay
            handler.removeCallbacks(finishRunnable)
            handler.postDelayed(finishRunnable, if (invoked) 180L else 20L)
        } else if (!hasFocus && assistRequested) {
            // The Gemini VoiceInteractionSession overlay has taken window focus; close immediately
            handler.removeCallbacks(finishRunnable)
            finishSilently()
        }
    }

    private fun triggerSystemVoiceSession(): Boolean {
        val args = buildAssistArgs()
        return runCatching { showAssist(args) }.getOrDefault(false)
    }

    private fun launchGeminiFallback() {
        val candidates = com.example.service.ActionExecutor.buildAssistantIntentCandidates(
            assistantPackage = com.example.service.ActionExecutor.GEMINI_APP_PACKAGE,
        )
        for (intent in candidates) {
            if (runCatching { startActivity(intent) }.isSuccess) return
        }
        packageManager.getLaunchIntentForPackage(com.example.service.ActionExecutor.GEMINI_APP_PACKAGE)
            ?.let { launch ->
                launch.addFlags( android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(launch) }
            }
    }

    private fun finishSilently() {
        if (!isFinishing) {
            finish()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_INVOCATION_TYPE = "invocation_type"
        const val EXTRA_SYSUI_INVOCATION_TYPE = "com.android.systemui.assist.invocation_type"
        const val INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS = 6

        fun buildAssistArgs(): Bundle {
            return Bundle().apply {
                putInt(EXTRA_INVOCATION_TYPE, INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS)
                putInt(EXTRA_SYSUI_INVOCATION_TYPE, INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS)
                putLong("invocation_time_ms", System.currentTimeMillis())
            }
        }
    }
}
