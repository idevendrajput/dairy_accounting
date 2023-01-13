package com.dr.dairyaccounting.records

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.get
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.ui.AdapterDefaultRecords
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.ui.RecordView
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentPurchaseEveningBinding
import com.dr.dairyaccounting.services.SaveTempService
import com.dr.dairyaccounting.ui.BaseFragment
import com.dr.dairyaccounting.ui.Loading
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.SELECTED_CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.Temp
import com.dr.dairyaccounting.utils.TempCallBacks
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.createExcel
import com.dr.dairyaccounting.utils.Utils.Companion.generatePDF
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayRecords
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AllRecords : BaseFragment() {

    private lateinit var binding: FragmentPurchaseEveningBinding
    private lateinit var printExcel: MenuItem
    private lateinit var printPdf: MenuItem
    private lateinit var print: MenuItem
    private lateinit var room: MyDatabase
    private lateinit var shift: String
    private lateinit var recordType: String
    private var list = ArrayList<RecordsEntity>()
    private val tempList = ArrayList<RecordsEntity>()
    private var isLoading = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPurchaseEveningBinding.inflate(layoutInflater)
        shift = arguments?.getString(SHIFT).toString()
        recordType = arguments?.getString(RECORD_TYPE).toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        room = MyDatabase.getDatabase(mContext)
        loading = Loading(mContext)
        loading.build()

        actions()
        toolBar()
        updateUi()

    }

    private fun updateUi() {
        binding.tb.title = "${if (shift == EVENING_SHIFT) "Evening" else "Morning"}  ${if (recordType == RECORD_TYPE_PURCHASE) "Purchase" else "Sale"} "
        binding.tb.subtitle = "${Temp.tempDataSize(room, shift, recordType)} Changes"
    }

    override fun onResume() {
        showLoading()
        Handler(Looper.getMainLooper())
            .postDelayed({
                initData()
            }, 200)
        super.onResume()
    }

    private fun initData() {

        binding.noResult.visibility = View.GONE

        list = getTodayRecords(mContext, shift, recordType)

        tempList.clear()

        update(list)

    }

    private fun update(list: ArrayList<RecordsEntity>) {

        if (list.size == 0) {
            binding.noResult.visibility = View.VISIBLE
            dismissLoading()
            return
        }

        val adapter = AdapterDefaultRecords(tempList, object : RecordView {
            override fun recordViw(itemView: View, clientId: String) {
                itemView.setOnClickListener {
                    val args = Bundle()
                    args.putString(RECORD_TYPE, recordType)
                    args.putString(SELECTED_CLIENT_ID, clientId)
                    args.putString(SHIFT, shift)
                    findNavController().navigate(R.id.addRecordEvening, args)
                }
            }
        },mContext)

        binding.rv.adapter = adapter

        addInRv(0,19,adapter, list)

        dismissLoading()

        val fn = "${if (shift == EVENING_SHIFT) "Evening" else "Morning"}  ${if (recordType == RECORD_TYPE_PURCHASE) "Purchase" else "Sale"} ".plus(SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
            Date(
                Utils.getCalendarTime(mContext)
            )
        ))

        printExcel.setOnMenuItemClickListener {
            createExcel(
                mContext, list, fn
            )
            true
        }

        printPdf.setOnMenuItemClickListener {
            generatePDF(list, fn, activity as AppCompatActivity)
            true
        }

        print.setOnMenuItemClickListener {
            generatePDF(list, fn, activity as AppCompatActivity, true)
            true
        }

        total(list)

    }

    private fun addInRv(startIndex: Int, endIndex: Int, adapter: AdapterDefaultRecords, list: ArrayList<RecordsEntity>) {

        showLoading()

        for (i in startIndex..endIndex) {
            if (i < list.size) {
                tempList.add(list[i])
                adapter.notifyItemInserted(tempList.lastIndex)
            }
        }

        isLoading = false
        dismissLoading()

        binding.nsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                if (!isLoading) {
                    isLoading = true
                    addInRv(endIndex+1, endIndex + 20 , adapter, list)
                }
            }
        })

    }

    private fun actions() {

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (Temp.isTempListNotEmpty(room, shift, recordType)) {
                        backDialog()
                    } else {
                        findNavController().popBackStack()
                    }
                }
            })

        binding.tb.setNavigationOnClickListener {
            if (Temp.isTempListNotEmpty(room, shift, recordType)) {
                backDialog()
            } else {
                findNavController().popBackStack()
            }
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
                Handler(Looper.getMainLooper())
                    .post {
                        search(searchView.query.toString().trim())
                    }
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                Handler(Looper.getMainLooper())
                    .post {
                        search(searchView.query.toString().trim())
                    }
                return false
            }
        })

    }

    private fun search(s : String) {

        binding.noResult.visibility = View.GONE

        tempList.clear()

        if (s.trim().isEmpty()) {
            initData()
            return
        }

        val result = ArrayList<RecordsEntity>()

        for (x in list) {
            if (x.clientName.trim().lowercase().contains(s.lowercase())) {
                result.add(x)
            }
        }
        binding.noResult.visibility = View.GONE

        if (result.isEmpty()) {
            binding.noResult.visibility = View.VISIBLE
        }

        val adapter = AdapterDefaultRecords(tempList, object : RecordView {
            override fun recordViw(itemView: View, clientId: String) {
                itemView.setOnClickListener {
                    val args = Bundle()
                    args.putString(RECORD_TYPE, recordType)
                    args.putString(SELECTED_CLIENT_ID, clientId)
                    args.putString(SHIFT, shift)
                    findNavController().navigate(R.id.addRecordEvening, args)
                }
            }
        }, mContext)

        binding.rv.adapter = adapter

        addInRv(0, 19, adapter, result)

        val fn = "${if (shift == EVENING_SHIFT) "Evening" else "Morning"}  ${if (recordType == RECORD_TYPE_PURCHASE) "Purchase" else "Sale"} ".plus(SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
            Date(
                Utils.getCalendarTime(mContext)
            )
        ))

        printExcel.setOnMenuItemClickListener {
            createExcel(
                mContext, result, fn
            )
            true
        }

        printPdf.setOnMenuItemClickListener {
             generatePDF(result, fn, activity as AppCompatActivity)
            true
        }

        print.setOnMenuItemClickListener {
            generatePDF(result, fn, activity as AppCompatActivity, true)
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
            .setTitle("${Temp.tempDataSize(room, shift, recordType)} Changes found. Choose what you want to do.")
            .setPositiveButton("Save") { _, _ ->
                Handler(Looper.getMainLooper())
                    .post {
                        loading.changeTitle("Saving...")
                        loading.showLoading()
                        saveUpdate()
                    }
            }
            .setNegativeButton("Discard") { _, _ ->
                loading.changeTitle("Canceling...")
                loading.showLoading()
                cancelUpdates()
            }.create().show()

    }

    private fun cancelUpdates() {

        Temp.cancelDataAndClearTemp(room, shift, recordType, object: TempCallBacks {
            override fun onCompleted() {
//                mContext.startService(Intent(mContext, UpdateDataTodayService::class.java).apply {
//                    putExtra(SHIFT, shift)
//                    putExtra(recordType, recordType)
//                })
                loading.dismissLoading()
                findNavController().popBackStack()
            }
        })

        // reinstall updates firebase to room while canceling updates

//        db.collection(AppConstants.COLLECTIONS_RECORDS)
//            .whereEqualTo(DATE, getTodayDate())
//            .get().addOnSuccessListener {
//
//                room.recordDao().deleteTodayRecords()
//
//                if (it.size() == 0) {
//                    findNavController().popBackStack()
//                    loading.dismissLoading()
//                    return@addOnSuccessListener
//                }
//
//                for ((i,d) in it.withIndex()) {
//
//                    try {
//
//                        val id = d.id
//                        val clientName = d[AppConstants.CLIENT_NAME].toString()
//                        val rate = d[AppConstants.RATE].toString().toFloat()
//                        val amount = d[AppConstants.AMOUNT].toString().toFloat()
//                        val amountPaid = d[AppConstants.AMOUNT_PAID].toString().toFloat()
//                        val amountAdvance = d[AppConstants.AMOUNT_ADVANCE].toString().toFloat()
//                        val recordType = d[AppConstants.RECORD_TYPE].toString()
//                        val quantity = d[AppConstants.QUANTITY].toString().toFloat()
//                        val shift = d[SHIFT].toString()
//                        val timestamp = d[AppConstants.TIMESTAMP].toString().toLong()
//                        val orderTimeStamp = d[AppConstants.ORDER_TIMESTAMP].toString().toLong()
//                        val clientId = d[AppConstants.CLIENT_ID].toString()
//
//                        CoroutineScope(Dispatchers.IO)
//                            .launch {
//                                try {
//                                    room.recordDao().insertRecord(
//                                        RecordsEntity(
//                                            id,
//                                            clientId,
//                                            clientName,
//                                            rate,
//                                            amount,
//                                            amountPaid,
//                                            amountAdvance,
//                                            recordType,
//                                            quantity = quantity,
//                                            shift = shift,
//                                            timestamp = timestamp,
//                                            orderTimeStamp = orderTimeStamp,
//                                            date =  getTodayDate(timestamp)
//                                        )
//                                    )
//                                } catch (e: Exception) {
//                                    room.recordDao().updateRecord(
//                                        RecordsEntity(
//                                            id,
//                                            clientId,
//                                            clientName,
//                                            rate,
//                                            amount,
//                                            amountPaid,
//                                            amountAdvance,
//                                            recordType,
//                                            quantity = quantity,
//                                            shift = shift,
//                                            timestamp = timestamp,
//                                            orderTimeStamp = orderTimeStamp,
//                                            date =  getTodayDate(timestamp)
//                                        )
//                                    )
//                                }
//                            }
//
//                    } catch (e: Exception) { }
//
//                    if (i == it.documents.lastIndex) {
//                        loading.dismissLoading()
//                        clearDataChanged(mContext)
//                        findNavController().popBackStack()
//                    }
//
//                }
//            }
    }

    private fun saveUpdate() {

        Temp.saveDataAndClearTemp(room, object: TempCallBacks {
            override fun onCompleted() {
                mContext.startService(Intent(mContext, SaveTempService::class.java))
                loading.dismissLoading()
                findNavController().popBackStack()
            }
        })

//        val records = getTodayEveningPurchase(mContext)
//
//        Utils.intentDataEvening(mContext, Gson().toJson(records))
//
//        Handler(Looper.getMainLooper())
//            .postDelayed({
//                loading.dismissLoading()
//                clearDataChanged(mContext)
//                findNavController().popBackStack()
//                mContext.startService(Intent(mContext, UpdateDataService::class.java).apply {
//                    putExtra(SHIFT, EVENING_SHIFT)
//                })
//            }, 200)



//        for ((i, item) in records.withIndex()) {
//
//            val map = HashMap<String, Any>()
//            map[AppConstants.CLIENT_NAME] = item.clientName
//            map[AppConstants.RATE] = item.rate
//            map[AppConstants.AMOUNT] = item.amount
//            map[AppConstants.AMOUNT_PAID] = item.amountPaid
//            map[AppConstants.AMOUNT_ADVANCE] = item.amountAdvance
//            map[RECORD_TYPE] = item.recordType
//            map[AppConstants.QUANTITY] = item.quantity
//            map[AppConstants.TIMESTAMP] = item.timestamp
//            map[AppConstants.ORDER_TIMESTAMP] = item.orderTimeStamp
//            map[AppConstants.CLIENT_ID] = item.clientId
//            map[DATE] = getTodayDate()
//            map[SHIFT] = EVENING_SHIFT
//
//            db.collection(AppConstants.COLLECTIONS_RECORDS)
//                .document(item.id)
//                .set(map, SetOptions.merge())
//                .addOnSuccessListener {
//                    if (i == records.lastIndex) {
//                        loading.dismissLoading()
//                        clearDataChanged(mContext)
//                        Snackbar.make(binding.root, "Saved Successfully", Snackbar.LENGTH_SHORT).show()
//                        findNavController().popBackStack()
//                    }
//                }
//
//        }

    }

}