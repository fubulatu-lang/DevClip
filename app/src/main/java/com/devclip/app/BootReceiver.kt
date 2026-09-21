package com.devclip.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Android kills all foreground services on reboot. If the user had the
 * bubble running and enabled "start automatically on boot" in Settings,
 * this restarts OverlayService once the device finishes booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val shouldAutoStart = prefs.getBoolean(Prefs.KEY_AUTO_START_ON_BOOT, true)
        val wasRunning = prefs.getBoolean(Prefs.KEY_BUBBLE_RUNNING, false)

        if (shouldAutoStart && wasRunning) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
