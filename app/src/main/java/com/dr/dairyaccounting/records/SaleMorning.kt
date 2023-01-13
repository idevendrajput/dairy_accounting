package com.dr.dairyaccounting.records

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.ui.AdapterDefaultRecords
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.ui.RecordView
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayMorningSale
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentSaleMorningBinding
import com.dr.dairyaccounting.services.UpdateDataService
import com.dr.dairyaccounting.ui.Loading
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.createExcel
import com.dr.dairyaccounting.utils.Utils.Companion.getCalendarTime
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SaleMorning : Fragment() {

    private lateinit var binding: FragmentSaleMorningBinding
    private lateinit var mContext: Context
    private lateinit var printExcel: MenuItem
    private lateinit var printPdf: MenuItem
    private lateinit var print: MenuItem
    private lateinit var room: MyDatabase
    private lateinit var loading: Loading
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSaleMorningBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actions()
        toolBar()

        room = MyDatabase.getDatabase(mContext)
        loading = Loading(mContext)
        loading.build()

    }

    override fun onResume() {
        initData()
        super.onResume()
    }

    private fun initData() {

        binding.noResult.visibility = View.GONE
        binding.pb.visibility = View.VISIBLE

        val list = getTodayMorningSale(mContext)

        RestoreRecords(mContext, list, object : DataRestoreCallBack {
            override fun onCompleted() {
                binding.pb.visibility = View.GONE
                update(list)
            }
        }).run()

    }

    private fun update(list: ArrayList<RecordsEntity>) {

        if (list.size == 0) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterDefaultRecords(list, object : RecordView {
            override fun recordViw(itemView: View, recordId: String) {
                itemView.setOnClickListener {
                    val args = Bundle()
                    args.putString(AppConstants.RECORD_TYPE, AppConstants.RECORD_TYPE_SALE)
                    args.putString(AppConstants.SELECTED_ITEM, recordId)
                    findNavController().navigate(R.id.addRecordMorning, args)
                }
            }
        }, mContext)

        printExcel.setOnMenuItemClickListener {
            Utils.createExcel(
                mContext, list, "Morning Sale ".plus(
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
                        Date(
                            getCalendarTime(mContext)
                        )
                    )
                )
            )
            true
        }

        printPdf.setOnMenuItemClickListener {
            Utils.generatePDF(
                list, "Morning Sale ".plus(
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
                        Date(
                            getCalendarTime(mContext)
                        )
                    )
                ), activity as AppCompatActivity
            )
            true
        }

        print.setOnMenuItemClickListener {
            Utils.generatePDF(
                list, "Morning Sale ".plus(
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
                        Date(
                            getCalendarTime(mContext)
                        )
                    )
                ), activity as AppCompatActivity, true
            )
            true
        }

        total(list)

    }

    private fun actions() {

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
//                    if (isDataChanged(mContext)) {
//                        backDialog()
//                    } else {
//                        findNavController().popBackStack()
//                    }
                }
            })

        binding.tb.setNavigationOnClickListener {
//            if (isDataChanged(mContext)) {
//                backDialog()
//            } else {
//                findNavController().popBackStack()
//            }
        }

    }

    private fun toolBar() {

        val menu = binding.tb.menu
        val searchView = menu[0].actionView as SearchView
        printExcel = menu[1]
        printPdf = menu[2]
        print = menu[3]

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                search(searchView.query.toString())
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                search(searchView.query.toString())
                return false
            }
        })

    }

    private fun search(s : String) {
        binding.noResult.visibility = View.GONE

        if (s.trim().isEmpty()) {
            initData()
            return
        }

        val list = getTodayMorningSale(mContext)

        val result = ArrayList<RecordsEntity>()

        for (x in list) {
            if (x.clientName.trim().lowercase().contains(s.lowercase())) {
                result.add(x)
            }
        }

        if (result.size == 0) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterDefaultRecords(result, object : RecordView {
            override fun recordViw(itemView: View, recordId: String) {
                itemView.setOnClickListener {
                    val args = Bundle()
                    args.putString(AppConstants.RECORD_TYPE, AppConstants.RECORD_TYPE_SALE)
                    args.putString(AppConstants.SELECTED_ITEM, recordId)
                    findNavController().navigate(R.id.addRecordMorning, args)
                }
            }
        }, mContext)

        printExcel.setOnMenuItemClickListener {
           createExcel(
                mContext, result, "Morning Sale ".plus(
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
                        Date(
                            getCalendarTime(mContext)
                        )
                    )
                )
            )
            true
        }

        printPdf.setOnMenuItemClickListener {
            Utils.generatePDF(
                result, "Morning Sale ".plus(
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
                        Date(
                            Utils.getCalendarTime(mContext)
                        )
                    )
                ), activity as AppCompatActivity
            )
            true
        }

        print.setOnMenuItemClickListener {
            Utils.generatePDF(
                result, "Morning Sale ".plus(
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
                        Date(
                            getCalendarTime(mContext)
                        )
                    )
                ), activity as AppCompatActivity, true
            )
            true
        }


        total(result)

    }

    private fun total(list: ArrayList<RecordsEntity>) {

        var totalAmount = 0f
        var totalQuantity = 0f
        var totalPaid = 0f

        for (i in list) {
            totalAmount += i.amount
            totalQuantity += i.quantity
            totalPaid += i.amountPaid
        }

        binding.amount.text = String.format("%.2f", totalAmount)
        binding.quantity.text = String.format("%.2f", totalQuantity)
        binding.amountPaid.text = String.format("%.2f", totalPaid)

    }

    private fun backDialog() {

        AlertDialog.Builder(mContext)
            .setTitle("Are you want to save changes?")
            .setPositiveButton("Save") { _, _ ->
                Handler(Looper.getMainLooper())
                    .post {
                        loading.changeTitle("Saving...")
                        loading.showLoading()
                        saveUpdate()
                    }
            }
            .setNegativeButton("Cancel") { _, _ ->
                loading.changeTitle("Canceling...")
                loading.showLoading()
                cancelUpdates()
            }.create().show()

    }

    private fun cancelUpdates() {

        // reinstall updates firebase to room while canceling updates

        db.collection(AppConstants.COLLECTIONS_RECORDS)
            .whereEqualTo(AppConstants.DATE, getTodayDate())
            .get().addOnSuccessListener {

                room.recordDao().deleteTodayRecords()

                if (it.size() == 0) {
                    findNavController().popBackStack()
                    loading.dismissLoading()
                    return@addOnSuccessListener
                }

                for ((i,d) in it.withIndex()) {

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
                                            date = getTodayDate(timestamp)
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
                                            date = getTodayDate(timestamp)
                                        )
                                    )
                                }
                            }
                    } catch (e: Exception) { }

                    if (i == it.documents.lastIndex) {
                        loading.dismissLoading()
                        findNavController().popBackStack()
                    }

                }

            }
    }

    private fun saveUpdate() {

        val records = getTodayMorningSale(mContext)

        Utils.intentDataMorning(mContext, Gson().toJson(records))

        Handler(Looper.getMainLooper())
            .postDelayed({
                loading.dismissLoading()
                findNavController().popBackStack()
                mContext.startService(Intent(mContext, UpdateDataService::class.java).apply {
                    putExtra(AppConstants.SHIFT, AppConstants.MORNING_SHIFT)
                })
            }, 200)

//        for ((i, item) in records.withIndex()) {
//
//            val map = HashMap<String, Any>()
//            map[AppConstants.CLIENT_NAME] = item.clientName
//            map[AppConstants.RATE] = item.rate
//            map[AppConstants.AMOUNT] = item.amount
//            map[AppConstants.AMOUNT_PAID] = item.amountPaid
//            map[AppConstants.AMOUNT_ADVANCE] = item.amountAdvance
//            map[AppConstants.RECORD_TYPE] = item.recordType
//            map[AppConstants.QUANTITY] = item.quantity
//            map[AppConstants.DATE] = getTodayDate()
//            map[AppConstants.TIMESTAMP] = item.timestamp
//            map[AppConstants.ORDER_TIMESTAMP] = item.orderTimeStamp
//            map[AppConstants.CLIENT_ID] = item.clientId
//            map[AppConstants.SHIFT] = AppConstants.MORNING_SHIFT
//
//            db.collection(AppConstants.COLLECTIONS_RECORDS)
//                .document(item.id)
//                .set(map, SetOptions.merge())
//                .addOnSuccessListener {
//                    if (i == records.lastIndex) {
//                        loading.dismissLoading()
//                        Utils.clearDataChanged(mContext)
//                        Snackbar.make(binding.root, "Saved Successfully", Snackbar.LENGTH_SHORT).show()
//                        findNavController().popBackStack()
//                    }
//                }
//                .addOnFailureListener {
//                    Snackbar.make(binding.root, "Something went wrong!", Snackbar.LENGTH_SHORT)
//                        .show()
//                }
//
//        }


    }


}