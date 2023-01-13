package com.dr.dairyaccounting.records

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class RestoreRecords(val mContext: Context, val list: ArrayList<RecordsEntity>, private val dataRestoreCallBack: DataRestoreCallBack) {

    fun run() {
        CoroutineScope(IO)
            .launch {
                restore()
            }
    }

   private fun restore() {

       val room = MyDatabase.getDatabase(mContext)

       for ((i,r) in list.withIndex()) {
           CoroutineScope(IO)
               .launch {
                   try {
                       room.recordDao().insertRecord(r)
                   } catch (e: Exception) {
                       room.recordDao().updateRecord(r)
                   }
               }
           if (i == list.lastIndex) {
               Handler(Looper.getMainLooper())
                   .postDelayed({
                       dataRestoreCallBack.onCompleted()
                   }, 200)
           }
       }

   }
}

interface DataRestoreCallBack {
    fun onCompleted()
}