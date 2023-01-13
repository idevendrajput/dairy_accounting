package com.dr.dairyaccounting.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.services.DataUpdateCallBack
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.COLLECTIONS_RECORDS
import com.dr.dairyaccounting.utils.AppConstants.SOMETHING_WENT_WRONG
import com.dr.dairyaccounting.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object HomeUtils {

    fun downloadOnlineData(
        room: MyDatabase,
        mContext: Context,
        downloadDataCallbacks: DownloadDataCallbacks
    ) {
        onlineToOffline(room, mContext, downloadDataCallbacks)
    }

//    private fun offlineToOnline(
//        room: MyDatabase,
//        mContext: Context,
//        downloadDataCallbacks: DownloadDataCallbacks
//    ) {
//
//        downloadDataCallbacks.onRestoreStart()
//
//        val allRecords = room.recordDao().getAllRecords() as ArrayList<RecordsEntity>
//
//        ServiceUtils.updateRecords(allRecords, object : DataUpdateCallBack {
//            override fun onSuccess() {
//                downloadDataCallbacks.onRestored()
//                onlineToOffline(room, mContext, downloadDataCallbacks)
//            }
//
//            override fun onFailure(msg: String) {
//                downloadDataCallbacks.onFailed(msg)
//                Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
//            }
//        })
//
//    }

    private fun onlineToOffline(
        room: MyDatabase,
        mContext: Context,
        downloadDataCallbacks: DownloadDataCallbacks
    ) {

        downloadDataCallbacks.onRecordDownloadStart()

        val db = FirebaseFirestore.getInstance()

        db.collection(COLLECTIONS_RECORDS)
            .orderBy(AppConstants.ORDER_TIMESTAMP, Query.Direction.DESCENDING)
            .get().addOnSuccessListener {

                if (it.size() != 0) {

                    for ((i, d) in it.withIndex()) {

                        try {

                            val id = d.id
                            val clientName = d[AppConstants.CLIENT_NAME].toString()
                            val rate = d[AppConstants.RATE].toString().toFloat()
                            val amount = d[AppConstants.AMOUNT].toString().toFloat()
                            val amountPaid = d[AppConstants.AMOUNT_PAID].toString().toFloat()
                            val amountAdvance = d[AppConstants.AMOUNT_ADVANCE].toString().toFloat()
                            val recordType = d[AppConstants.RECORD_TYPE].toString()
                            val quantity = d[AppConstants.QUANTITY].toString().toFloat()
                            val shift = d[AppConstants.SHIFT].toString()
                            val timestamp = d[AppConstants.TIMESTAMP].toString().toLong()
                            val orderTimeStamp = d[AppConstants.ORDER_TIMESTAMP].toString().toLong()
                            val clientId = d[AppConstants.CLIENT_ID].toString()

                            CoroutineScope(Dispatchers.IO)
                                .launch {
                                    try {
                                        room.recordDao().insertRecord(
                                            RecordsEntity(
                                                id,
                                                clientId,
                                                clientName,
                                                rate,
                                                amount,
                                                amountPaid,
                                                amountAdvance,
                                                recordType,
                                                quantity = quantity,
                                                shift = shift,
                                                timestamp = timestamp,
                                                orderTimeStamp = orderTimeStamp,
                                                date = Utils.getTodayDate(timestamp)
                                            )
                                        )
                                    } catch (e: Exception) {
                                        room.recordDao().updateRecord(
                                            RecordsEntity(
                                                id,
                                                clientId,
                                                clientName,
                                                rate,
                                                amount,
                                                amountPaid,
                                                amountAdvance,
                                                recordType,
                                                quantity = quantity,
                                                shift = shift,
                                                timestamp = timestamp,
                                                orderTimeStamp = orderTimeStamp,
                                                date = Utils.getTodayDate(timestamp)
                                            )
                                        )
                                    }
                                }

                        } catch (e: Exception) {
                        }

                        if (i >= it.size() - 1) {
                            downloadDataCallbacks.onRecordDownloaded()
                            Handler(Looper.getMainLooper())
                                .postDelayed({
                                    downloadClients(room, mContext, db, downloadDataCallbacks)
                                }, 200)
                        }

                    }

                } else {
                    downloadDataCallbacks.onRecordDownloaded()
                    Handler(Looper.getMainLooper())
                        .postDelayed({
                            downloadClients(room, mContext, db, downloadDataCallbacks)
                        }, 500)
                }
            }
            .addOnFailureListener {
                downloadDataCallbacks.onRecordDownloadFailed()
                downloadDataCallbacks.onFailed(it.message.toString())
                Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
            }

    }

    private fun downloadClients(
        room: MyDatabase,
        mContext: Context,
        db: FirebaseFirestore,
        downloadDataCallbacks: DownloadDataCallbacks
    ) {

        downloadDataCallbacks.onClientDownloadStart()

        db.collection(AppConstants.COLLECTION_CLIENTS)
            .orderBy(AppConstants.ORDER_TIMESTAMP, Query.Direction.DESCENDING)
            .get().addOnSuccessListener {

                if (it.size() != 0) {

                    for ((i, d) in it.withIndex()) {

                        val id = d.id
                        val clientName = d[AppConstants.CLIENT_NAME].toString()
                        val isActive =
                            d[AppConstants.IS_CLIENT_ACTIVE] == null || d[AppConstants.IS_CLIENT_ACTIVE] as Boolean
                        val phone = d[AppConstants.PHONE].toString()
                        val accountType = d[AppConstants.ACCOUNT_TYPE].toString()
                        val morningQuantity = d[AppConstants.MORNING_QUANTITY].toString().toFloat()
                        val eveningQuantity = d[AppConstants.EVENING_QUANTITY].toString().toFloat()
                        val rate = d[AppConstants.RATE].toString().toFloat()
                        val timestamp = d[AppConstants.TIMESTAMP].toString().toLong()
                        val orderTimeStamp = d[AppConstants.ORDER_TIMESTAMP].toString().toLong()
                        val eveningShift = try {
                            d[AppConstants.EVENING_SHIFT] as Boolean
                        } catch (e: Exception) {
                            eveningQuantity > 0f
                        }
                        val morningShift = try {
                            d[AppConstants.MORNING_SHIFT] as Boolean
                        } catch (e: Exception) {
                            morningQuantity > 0f
                        }

                        CoroutineScope(Dispatchers.IO)
                            .launch {
                                try {
                                    room.clientDao().insertClient(
                                        ClientsEntity(
                                            id,
                                            clientName,
                                            isActive,
                                            phone,
                                            accountType,
                                            morningQuantity,
                                            eveningQuantity,
                                            rate,
                                            timestamp,
                                            orderTimeStamp,
                                            eveningShift = eveningShift,
                                            morningShift = morningShift
                                        )
                                    )
                                } catch (e: Exception) {
                                    try {
                                        room.clientDao().updateClient(
                                            ClientsEntity(
                                                id,
                                                clientName,
                                                isActive,
                                                phone,
                                                accountType,
                                                morningQuantity,
                                                eveningQuantity,
                                                rate,
                                                timestamp,
                                                orderTimeStamp,
                                                eveningShift = eveningShift,
                                                morningShift = morningShift
                                            )
                                        )
                                    } catch (e: Exception) {
                                    }
                                }
                            }

                        if (i >= it.size() - 1) {
                            downloadDataCallbacks.onClientDownloaded()
                            Handler(Looper.getMainLooper())
                                .postDelayed({
                                    downloadDataCallbacks.onSuccess()
                                }, 2000)
                        }

                    }
                }
            }.addOnFailureListener {
                Toast.makeText(
                    mContext,
                    SOMETHING_WENT_WRONG,
                    Toast.LENGTH_SHORT
                ).show()
                downloadDataCallbacks.onClientDownloadFailed()
                downloadDataCallbacks.onFailed(it.message.toString())
            }

    }

    suspend fun deleteOffline(room: MyDatabase, dataUpdateCallBack: DataUpdateCallBack) {

        CoroutineScope(IO)
            .launch {
                room.recordDao().deleteAllRecords()
            }
         delay(200)

        CoroutineScope(IO)
            .launch {
                room.tempRecordDao().deleteAllRecords()
            }

        delay(200)

        CoroutineScope(IO)
            .launch {
                room.clientDao().deleteAllClients()
            }

        delay(200)

        dataUpdateCallBack.onSuccess()

    }

}