package com.dr.dairyaccounting.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore

class SaveTempService: Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        ServiceUtils.saveTemp(this, object: DataUpdateCallBack {
            override suspend fun onSuccess() {
                 //
                Log.d("!--->", "Temp Saved Callback")
            }

            override fun onFailure(msg: String) {
                //
            }
        })

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}