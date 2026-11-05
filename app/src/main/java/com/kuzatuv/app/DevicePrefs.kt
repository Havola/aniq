package com.kuzatuv.app

import android.content.Context
import android.os.Build

object DevicePrefs {
    private const val PREFS = "kuzatuv_prefs"
    private const val KEY_DEVICE = "device_name"

    fun getDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE, null) ?: (Build.MODEL ?: "Nomalum")
    }

    fun setDeviceName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEVICE, name).apply()
    }
}
