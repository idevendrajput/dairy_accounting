package com.dr.dairyaccounting.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.ui.MainActivity
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

object ServiceUtils {

     fun saveTemp(mContext: Context, dataUpdateCallBack: DataUpdateCallBack) {

        Log.d("!--->", "Temp Saving")

        val room = MyDatabase.getDatabase(mContext)
        val temp = room.tempRecordDao().getAllRecords()
        val db = FirebaseFirestore.getInstance()

        var x = 0

        for ((i, item) in temp.withIndex()) {

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
            map[AppConstants.SHIFT] = item.shift

            db.collection(AppConstants.COLLECTIONS_RECORDS)
                .document(item.id).set(map).addOnSuccessListener {
                    Log.d("!--->", "Temp Saved")
                    room.tempRecordDao().deleteById(item.id)
                    x++
                    if (x == temp.size) {
                        CoroutineScope(IO).launch {
                            dataUpdateCallBack.onSuccess()
                        }
                    }
                }

        }

    }


    fun sendNotification(title: String, shortMsg: String, content: String, notificationId: Int, applicationContext: Context) {

        // SharedPref.setData(getApplicationContext(),"Massage","Pending");
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(applicationContext, notification)
        r.play()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.isLooping = false
        }

        // vibration

        // vibration
        val v = applicationContext.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(100, 300, 300, 300)
        v.vibrate(pattern, -1)

        val resourceImage = applicationContext.resources.getIdentifier(
            "logo",
            "drawable",
            "com.dr.dairyaccounting"
        )


        val builder = NotificationCompat.Builder(applicationContext, "Doodhwala")
        builder.setSmallIcon(resourceImage)

        val resultIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(applicationContext, 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentTitle(title)
        builder.setContentText(shortMsg)
        builder.setContentIntent(pendingIntent)
        builder.setStyle(
            NotificationCompat.BigTextStyle().bigText(content)
        )
        builder.setAutoCancel(true)
        builder.priority = Notification.PRIORITY_MAX

        val mNotificationManager =
            applicationContext.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "Doodhwala"
            val channel = NotificationChannel(
                channelId,
                "Doodhwala notification channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }

        mNotificationManager.notify(notificationId, builder.build())

    }

    fun updateRecords(records: ArrayList<RecordsEntity>, callBack: DataUpdateCallBack) {

        val db = FirebaseFirestore.getInstance()

        var success = 0
        var failed = 0

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
            map[AppConstants.SHIFT] = item.shift

            db.collection(AppConstants.COLLECTIONS_RECORDS)
                .document(item.id).set(map)
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        success++
                    else
                        failed++

                    if (success + failed >= records.size) {
                        if (success == records.size) {
                            CoroutineScope(IO)
                                .launch {
                                    callBack.onSuccess()
                                }
                        } else {
                            callBack.onFailure("$success Tasks Succeed & $failed Tasks Failed")
                        }

                    }

                }
        }


    }


}