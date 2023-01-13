package com.dr.dairyaccounting.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.database.RecordsEntity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import java.util.HashMap

class SaveData : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }

    private fun saveData(records: ArrayList<RecordsEntity>) {

        val db = FirebaseFirestore.getInstance()

        for ((i, item) in records.withIndex()) {

            val map = HashMap<String, Any>()
            map[AppConstants.CLIENT_NAME] = item.clientName
            map[AppConstants.RATE] = item.rate
            map[AppConstants.AMOUNT] = item.amount
            map[AppConstants.AMOUNT_PAID] = item.amountPaid
            map[AppConstants.AMOUNT_ADVANCE] = item.amountAdvance
            map[AppConstants.RECORD_TYPE] = item.recordType
            map[AppConstants.QUANTITY] = item.quantity
            map[AppConstants.TIMESTAMP] = item.timestamp
            map[AppConstants.ORDER_TIMESTAMP] = item.orderTimeStamp
            map[AppConstants.CLIENT_ID] = item.clientId
            map[AppConstants.DATE] = Utils.getTodayDate()
            map[AppConstants.SHIFT] = AppConstants.EVENING_SHIFT

            db.collection(AppConstants.COLLECTIONS_RECORDS)
                .document(item.id)
                .set(map, SetOptions.merge())
        }

    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


}