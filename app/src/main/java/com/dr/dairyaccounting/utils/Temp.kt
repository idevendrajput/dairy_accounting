package com.dr.dairyaccounting.utils

import android.os.Handler
import android.os.Looper
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.utils.Utils.Companion.tempToRecordItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Temp {

    fun isTempListNotEmpty(room: MyDatabase, shift: String, recordType: String) = room.tempRecordDao().getRecordsByShiftAndRecordType(shift, recordType).isNotEmpty()

    fun tempDataSize(room: MyDatabase, shift: String, recordType: String) = room.tempRecordDao().getRecordsByShiftAndRecordType(shift, recordType).size

    fun allTempDataSize(room: MyDatabase) = room.tempRecordDao().getAllRecords().size

    fun deleteTempList(room: MyDatabase) = room.tempRecordDao().deleteAllRecords()

    fun deleteTempItem(room: MyDatabase, id: String) = room.tempRecordDao().deleteById(id)

    fun saveDataAndClearTemp(room: MyDatabase, callBack: TempCallBacks) {

        val tempList = room.tempRecordDao().getAllRecords()

        for ((i, t) in tempList.withIndex()) {
            CoroutineScope(IO)
                .launch {
                    try {
                        room.recordDao().insertRecord(tempToRecordItem(t))
                    } catch (e: Exception) {
                        room.recordDao().updateRecord(tempToRecordItem(t))
                    }
                    if (i == tempList.lastIndex) {
                        withContext(Main) {
                            callBack.onCompleted()
                        }
                    }
                }
        }
    }

    fun cancelDataAndClearTemp(room: MyDatabase, shift: String, recordType: String, callBack: TempCallBacks) {

        val tempList = room.tempRecordDao().getRecordsByShiftAndRecordType(shift, recordType)

        for ((i, t) in tempList.withIndex()) {

            room.recordDao().deleteById(t.id)
            room.tempRecordDao().deleteById(t.id)

            if (i == tempList.lastIndex) {
                Handler(Looper.getMainLooper())
                    .postDelayed({
                        callBack.onCompleted()
                    }, 200)
            }

        }

    }

}