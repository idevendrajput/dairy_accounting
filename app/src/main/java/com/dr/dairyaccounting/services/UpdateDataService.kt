package com.dr.dairyaccounting.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.clearUpdateServiceRunning
import com.dr.dairyaccounting.utils.Utils.Companion.intentDataEvening
import com.dr.dairyaccounting.utils.Utils.Companion.intentDataMorning
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class UpdateDataService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val db = FirebaseFirestore.getInstance()
        val shift = intent?.getStringExtra(SHIFT) ?: ""

        val d = if (shift == MORNING_SHIFT) intentDataMorning(this) else intentDataEvening(this)

        if (d.isEmpty()) {
            Toast.makeText(this, "No Data found", Toast.LENGTH_SHORT).show()
            return super.onStartCommand(intent, flags, startId)
        }

        val records = GsonBuilder().create().fromJson<ArrayList<RecordsEntity>>(d, object : TypeToken<ArrayList<RecordsEntity?>?>() {}.type)

        var x = 0

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
            map[SHIFT] = shift

            db.collection(AppConstants.COLLECTIONS_RECORDS)
                .document(item.id).set(map)
                .addOnCompleteListener {
                    x++
                    if (x == i) {
                        clearUpdateServiceRunning(this)
                    }
                }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


}