package com.kuzatuv.app

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramHelper {

    // Шу иккита қийматни ўзингиз тўлдиринг:
    private const val BOT_TOKEN = "9124506151:AAGqAGpca5jzSV7-8Z2f1igq3YadYIekC-Q"
    private const val CHAT_ID = "176764101"

    fun send(text: String) {
        
        Thread {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendMessage?chat_id=$CHAT_ID&text=$encoded")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.inputStream.close()
                conn.disconnect()
            } catch (e: Exception) {
                // жим ўтказиб юборамиз
            }
        }.start()
    }
}
