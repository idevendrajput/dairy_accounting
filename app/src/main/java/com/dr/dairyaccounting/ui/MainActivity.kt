/**
 *
 *@author  Devendra Singh, Sikar Infotech , Sikar , Rajasthan, 332001
 * @version 1.3
 * @since   2022-06-15
 * @host: Google Cloud's Firestore Database
 *
 */

package com.dr.dairyaccounting.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppFunctions
import com.dr.dairyaccounting.utils.AppFunctions.Companion.getDisplayHeaders
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.intentDataEvening
import com.dr.dairyaccounting.utils.Utils.Companion.intentDataMorning
import com.dr.dairyaccounting.utils.Utils.Companion.setCalendarTime
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var room : MyDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Firebase.initialize(this)

        room = MyDatabase.getDatabase(this)

        setCalendarTime(this)

        Handler(Looper.getMainLooper())
            .post {
                getDisplayHeaders(object : AppFunctions.Companion.DisplayCallBack {
                    override fun callBack(phone: String, name: String) {
                        Utils.setDisplayPhone(this@MainActivity, phone)
                        Utils.setDisplayName(this@MainActivity, name)
                    }
                })
            }

        Handler(Looper.getMainLooper())
            .post {
                updateDataInBackground()
            }

    }

    override fun onResume() {
        versionControls()
        super.onResume()
    }

    private fun updateDataInBackground() {

        val list = room.recordDao().getAllRecords() as ArrayList<RecordsEntity>

        val morning = ArrayList<RecordsEntity>()
        val evening = ArrayList<RecordsEntity>()

        for (i in list) {
            if (i.shift == AppConstants.MORNING_SHIFT)
                morning.add(i)
            else
                evening.add(i)
        }

//        intentDataEvening(this, Gson().toJson(evening))
//        intentDataMorning(this, Gson().toJson(morning))
//
//        Handler(Looper.getMainLooper())
//            .postDelayed({
//
//                startService(Intent(this, UpdateDataService::class.java).apply {
//                    putExtra(AppConstants.SHIFT, AppConstants.MORNING_SHIFT)
//                })
//
//                startService(Intent(this, UpdateDataService::class.java).apply {
//                    putExtra(AppConstants.SHIFT, AppConstants.EVENING_SHIFT)
//                })
//            }, 200)

    }

    private fun versionControls() {

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val version = pInfo.versionName

        db.collection("Versions")
            .document(version)
            .get().addOnSuccessListener {
                try {
                    if (it.exists()) {
                        val isDisabled = it["isDisabled"] as Boolean
                        val isShowUpdateDialog = it["isShowUpdateAlert"] as Boolean
                        if (isDisabled) {
                            AlertDialog.Builder(applicationContext)
                                .setTitle("Update Alert!")
                                .setMessage("This version of application is disabled. Please update the App.")
                                .setCancelable(false)
                                .setNeutralButton("Update"){_,_->
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.GDA")
                                    startActivity(intent)
                                }.create().show()
                        }
                        if (isShowUpdateDialog) {
                            AlertDialog.Builder(applicationContext)
                                .setTitle("Update Alert!")
                                .setMessage("A newer version of this application is available. Please update for better experience.")
                                .setNeutralButton("Update"){_,_->
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.dr.oldmobile")
                                    startActivity(intent)
                                }.create().show()
                        }
                    }
                } catch (e : Exception) {}
            }
    }

}