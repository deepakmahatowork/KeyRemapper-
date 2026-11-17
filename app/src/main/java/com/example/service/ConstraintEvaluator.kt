package com.example.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import com.example.model.StateConstraint

class ConstraintEvaluator(private val context: Context) {

    fun isSatisfied(constraint: StateConstraint): Boolean {
        return when (constraint) {
            StateConstraint.ALWAYS -> true
            StateConstraint.SCREEN_ON -> isScreenInteractive()
            StateConstraint.SCREEN_OFF -> !isScreenInteractive()
            StateConstraint.MEDIA_PLAYING -> isMediaPlaying()
            StateConstraint.WIFI_CONNECTED -> isWifiConnected()
            StateConstraint.CHARGING -> isCharging()
        }
    }

    private fun isScreenInteractive(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive ?: true
    }

    private fun isMediaPlaying(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.isMusicActive ?: false
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
