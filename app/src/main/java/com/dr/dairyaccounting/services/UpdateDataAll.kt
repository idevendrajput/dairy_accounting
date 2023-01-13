package com.dr.dairyaccounting.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.services.ServiceUtils.updateRecords
import com.dr.dairyaccounting.utils.AppConstants

//
//class UpdateDataAll: Service() {
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//
//        val shift = intent?.getStringExtra(AppConstants.SHIFT) ?: ""
//        val recordType = intent?.getStringExtra(AppConstants.RECORD_TYPE) ?: ""
//
//        val room = MyDatabase.getDatabase(this)
//
//        val records = room.recordDao().getRecordsByShiftAndRecordType(recordType, shift) as ArrayList<RecordsEntity>
//
//        if (recordType.isEmpty() || shift.isEmpty()) {
//            Toast.makeText(this, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
//            return super.onStartCommand(intent, flags, startId)
//        }
//
//        updateRecords(records, object: DataUpdateCallBack {
//            override fun onSuccess() {
//                ServiceUtils.sendNotification("Doodhwala", "Data update success", "All data updated successfully in the server",100, this@UpdateDataAll)
//            }
//
//            override fun onFailure(msg: String) {
//                ServiceUtils.sendNotification("Doodhwala", "Data update failed", msg,100, this@UpdateDataAll)
//                // Notify Data No Updated
//            }
//        })
//
//        return super.onStartCommand(intent, flags, startId)
//    }
//
//    override fun onBind(p0: Intent?): IBinder? {
//        return null
//    }
//
//}