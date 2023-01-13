package com.dr.dairyaccounting.add_data

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentAddRecordEveningBinding
import com.dr.dairyaccounting.dialogs.AddPaymentDialog
import com.dr.dairyaccounting.services.SaveTempService
import com.dr.dairyaccounting.ui.BaseFragment
import com.dr.dairyaccounting.utils.AppConstants.DATE
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.POSITION
import com.dr.dairyaccounting.utils.AppConstants.QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.SELECTED_CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.SELECTED_ITEM
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.Temp
import com.dr.dairyaccounting.utils.TempCallBacks
import com.dr.dairyaccounting.utils.Utils.Companion.getCalendarTime
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayRecords
import com.dr.dairyaccounting.utils.Utils.Companion.recordItemToTempItem
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddRecords : BaseFragment(), PaymentChangeCallback {

    private lateinit var binding: FragmentAddRecordEveningBinding
    private var records = ArrayList<RecordsEntity>()
    private lateinit var recordType: String
    private var selectedClientId = ""
    private lateinit var room: MyDatabase
    private lateinit var rateTextWatcher: TextWatcher
    private var selectedPosition = 0
    private var shift = MORNING_SHIFT
    private lateinit var quantityTextWatcher: TextWatcher
    private var saveMenu: MenuItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddRecordEveningBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {

            room = MyDatabase.getDatabase(mContext)
            recordType = arguments?.getString(RECORD_TYPE).toString()
            shift = arguments?.getString(SHIFT).toString()
            selectedClientId = arguments?.getString(SELECTED_CLIENT_ID).toString()
            val list = getTodayRecords(recordType = recordType, shift = shift, mContext = mContext)
            records.addAll(list)

            if (records.isNotEmpty())
                for ((i, r) in records.withIndex())
                    if (r.clientId == selectedClientId) {
                        selectedPosition = i
                        break
                    }

        } catch (e: Exception) {
        }

        updateUi()
        toolBar()
        //  revertPositionOnBack()

    }

//    private fun revertPositionOnBack() {
//        setFragmentResultListener("sbp") { _, bundle ->
//            try {
//                val position = bundle.getInt(AppConstants.POSITION)
//                selectedPosition = position
//                updateUi()
//            } catch (e: Exception) { }
//        }
//    }


    private fun updateUi() {

        visibility()

        if (records.isNotEmpty()) {

            val selectedItem = records[selectedPosition]

            Log.d(
                "!--->",
                "" + selectedItem.amountPaid + " " + selectedItem.amount + " " + selectedItem.amountAdvance
            )

            if (selectedItem.quantity > 0f) {
                binding.quantity.setText(selectedItem.quantity.toString())
            } else {
                binding.quantity.setText("")
            }

            if (selectedItem.amount > 0f) {
                binding.amount.setText(selectedItem.amount.toString())
            } else {
                binding.amount.setText("")
            }

            binding.rate.setText(selectedItem.rate.toString())
            binding.rate.setSelection(binding.rate.length())

            if (selectedItem.quantity > 0) {
                binding.quantity.setText(selectedItem.quantity.toString())
                binding.quantity.setSelection(binding.quantity.length())
            } else {
                binding.quantity.setText("")
            }

            binding.clientName.text = selectedItem.clientName

        }

        Log.d("!--->", "Temp size 2: " + Temp.tempDataSize(room, shift, recordType))

        actions()

    }

    private fun visibility() {
        binding.forward.visibility = View.VISIBLE
        binding.back.visibility = View.VISIBLE
        if (selectedPosition == 0) binding.back.visibility = View.INVISIBLE
        if (selectedPosition == records.lastIndex) binding.forward.visibility = View.INVISIBLE
    }

    private fun forward() {
        binding.quantity.removeTextChangedListener(quantityTextWatcher)
        binding.rate.removeTextChangedListener(rateTextWatcher)
        if (selectedPosition < records.lastIndex) {
            selectedPosition++
            updateUi()
        }
    }

    override fun onPause() {
        binding.rate.removeTextChangedListener(rateTextWatcher)
        binding.quantity.removeTextChangedListener(quantityTextWatcher)
        Log.d("!--->", "onPause: ")
        super.onPause()
    }

    private fun previous() {
        binding.quantity.removeTextChangedListener(quantityTextWatcher)
        binding.rate.removeTextChangedListener(rateTextWatcher)
        if (selectedPosition > 0) {
            selectedPosition--
            updateUi()
        }
    }

    private fun actions() {

        rateTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                changeValues()
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        }

        quantityTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                changeValues()
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        }

        binding.rate.addTextChangedListener(rateTextWatcher)
        binding.quantity.addTextChangedListener(quantityTextWatcher)

        binding.back.setOnClickListener {
            previous()
        }

        binding.forward.setOnClickListener {
            forward()
        }

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.edit.setOnClickListener {
            val args = Bundle()
            args.putString(CLIENT_ID, records[selectedPosition].clientId)
            findNavController().navigate(R.id.addClient, args)
        }

        binding.payments.setOnClickListener {
            AddPaymentDialog(
                mContext,
                recordType,
                room,
                records[selectedPosition],
                this
            ).build().showDialog()
        }


    }

    companion object {

        fun getMonthNow() = run {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("dd/MM/y", Locale.ENGLISH).parse(getTodayDate())
            cal.time = sdf
            cal.get(Calendar.MONTH)
        }

        fun getYearNow() = run {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("dd/MM/y", Locale.ENGLISH).parse(getTodayDate())
            cal.time = sdf
            cal.get(Calendar.YEAR)
        }

        fun timeStampNow() = run {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("dd/MM/y", Locale.ENGLISH).parse(getTodayDate())
            cal.time = sdf
            cal.time.time
        }

    }

    private fun changeValues() {

        //setDataChanged(mContext)

        if (binding.rate.text.toString().isNotEmpty() && !binding.rate.text.toString()
                .endsWith(".") && binding.quantity.text.toString()
                .isNotEmpty() && !binding.quantity.text.toString().endsWith(".")
        ) {
            binding.amount.setText(
                (binding.rate.text.toString().toFloat() * binding.quantity.text.toString()
                    .toFloat()).toString()
            )
        }

        val quantity = try {
            binding.quantity.text.toString().toFloat()
        } catch (e: Exception) {
            0f
        }
        val rate = try {
            binding.rate.text.toString().toFloat()
        } catch (e: Exception) {
            records[selectedPosition].rate
        }
        val amount = try {
            binding.amount.text.toString().toFloat()
        } catch (e: Exception) {
            0f
        }

        val selectedItem = records[selectedPosition]

        selectedItem.rate = rate
        selectedItem.quantity = quantity
        selectedItem.amount = amount

        Handler(Looper.getMainLooper())
            .post {
                saveUpdate()
            }

    }

    private fun toolBar() {

        binding.tb.title =
            "${if (shift == EVENING_SHIFT) "Evening" else "Morning"}  ${if (recordType == RECORD_TYPE_PURCHASE) "Purchase" else "Sale"} "

        saveMenu = binding.tb.menu[0]

        saveMenu?.setOnMenuItemClickListener {
            saveMenu?.isEnabled = false
            Handler(Looper.getMainLooper())
                .post {
                    showLoading()
                    Temp.saveDataAndClearTemp(room, object : TempCallBacks {
                        override fun onCompleted() {
                            mContext.startService(Intent(mContext, SaveTempService::class.java))
                            dismissLoading()
                        }
                    })
                }
            false
        }

    }

    private fun saveUpdate() {

        saveMenu?.isEnabled = false

        val selectedItem = records[selectedPosition]

        val isRecordExist = room.recordDao().getRecordsById(selectedItem.id).isNotEmpty()

        if (!isRecordExist) {
            selectedItem.timestamp = getCalendarTime(mContext)
            selectedItem.orderTimeStamp = getCalendarTime(mContext)
        }

        CoroutineScope(IO)
            .launch {
                try {
                    room.recordDao().insertRecord(selectedItem)
                } catch (e: Exception) {
                    room.recordDao().updateRecord(selectedItem)
                }
            }

        CoroutineScope(IO)
            .launch {
                try {
                    room.tempRecordDao().insertRecord(recordItemToTempItem(selectedItem))
                } catch (e: Exception) {
                    room.tempRecordDao().updateRecord(recordItemToTempItem(selectedItem))
                }
            }

        Handler(Looper.getMainLooper())
            .postDelayed({
                saveMenu?.isEnabled = true
            }, 200)

    }

    override fun onPaymentChanged() {
        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }
    }


}