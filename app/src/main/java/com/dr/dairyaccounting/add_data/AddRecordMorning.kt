package com.dr.dairyaccounting.add_data

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.add_data.AddRecords.Companion.getMonthNow
import com.dr.dairyaccounting.add_data.AddRecords.Companion.getYearNow
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_ADVANCE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_PAID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_NAME
import com.dr.dairyaccounting.utils.AppConstants.COLLECTIONS_RECORDS
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.RATE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_SALE
import com.dr.dairyaccounting.utils.AppConstants.TIMESTAMP
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentAddRecordBinding
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.DATE
import com.dr.dairyaccounting.utils.AppConstants.IS_PAYMENT_INFO
import com.dr.dairyaccounting.utils.AppConstants.MONTH
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.POSITION
import com.dr.dairyaccounting.utils.AppConstants.QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.SharedPref
import com.dr.dairyaccounting.utils.Utils.Companion.getCalendarTime
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayMorningPurchase
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayMorningSale
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddRecordMorning : Fragment() {

    private lateinit var binding: FragmentAddRecordBinding
    private lateinit var mContext: Context
    private var records = ArrayList<RecordsEntity>()
    private var selectedPosition = 0
    private lateinit var room: MyDatabase
    private val db = FirebaseFirestore.getInstance()
    private lateinit var rateTextWatcher: TextWatcher
    private lateinit var quantityTextWatcher: TextWatcher
    private lateinit var recordType: String
    private var selectedItem = ""

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddRecordBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onPause() {
        binding.rate.removeTextChangedListener(rateTextWatcher)
        binding.quantity.removeTextChangedListener(quantityTextWatcher)
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {

            room = MyDatabase.getDatabase(mContext)
            recordType = arguments?.getString(RECORD_TYPE).toString()
            val list = if (recordType == RECORD_TYPE_PURCHASE) getTodayMorningPurchase(
                mContext
            ) else getTodayMorningSale(mContext)
            selectedItem = arguments?.getString(AppConstants.SELECTED_ITEM).toString()

            records.addAll(list)

            if (records.isNotEmpty())
                for ((i,r) in records.withIndex())
                    if (r.id == selectedItem)
                        selectedPosition = i


        } catch (e: Exception) { }

//        if (isReposition(mContext)) {
//            selectedPosition = reposition(mContext)
//        }

        updateUi()
        toolBar()

    }

    private fun updateUi() {

        visibility()

        if (records.isNotEmpty()) {

            val selectedItem = records[selectedPosition]

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

            if (selectedItem.shift == MORNING_SHIFT) {
                if (selectedItem.quantity > 0) {
                    binding.quantity.setText(selectedItem.quantity.toString())
                    binding.quantity.setSelection(binding.quantity.length())
                } else {
                    binding.quantity.setText("")
                }
            }

            binding.clientName.text = selectedItem.clientName

            if (selectedItem.recordType == RECORD_TYPE_SALE) {
                binding.tb.title = "Morning Sale"
            } else {
                binding.tb.title = "Morning Purchases"
            }

        }

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

        binding.edit.setOnClickListener {
            val args = Bundle()
            args.putString(CLIENT_ID, records[selectedPosition].clientId)
            findNavController().navigate(R.id.addClient, args)
        }

        binding.payments.setOnClickListener {
            val args = Bundle()
            args.putString(CLIENT_ID, records[selectedPosition].clientId)
            args.putString(RECORD_TYPE, records[0].recordType)
            args.putBoolean(IS_PAYMENT_INFO, true)
            args.putInt(YEAR, getYearNow())
            args.putInt(MONTH, getMonthNow())
            args.putString(SHIFT, MORNING_SHIFT)
            args.putInt(POSITION, selectedPosition)
            args.putInt(POSITION, selectedPosition)
            findNavController().navigate(R.id.action_addRecordMorning_to_summaryByPerson, args)
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

        saveUpdate(false)

    }

    private fun toolBar() {

        val saveMenu = binding.tb.menu[0]

        saveMenu.setOnMenuItemClickListener {
            saveUpdate(true)
            false
        }

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun saveUpdate(saveInFirestore: Boolean) {

        for ((i, item) in records.withIndex()) {

            val isRecordExist = room.recordDao().getRecordsById(item.id).isNotEmpty()

            if (!isRecordExist) {
                item.timestamp = getCalendarTime(mContext)
                item.orderTimeStamp = getCalendarTime(mContext)
            }

            CoroutineScope(IO)
                .launch {
                    try {
                        room.recordDao().insertRecord(item)
                    } catch (e: Exception) {
                        room.recordDao().updateRecord(item)
                    }
                }

            if (saveInFirestore) {
                saveInFirebase(item)
                if (i == records.lastIndex) {
                    Snackbar.make(binding.root, "Saved Successfully", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveInFirebase(item: RecordsEntity) {

        val map = HashMap<String, Any>()
        map[CLIENT_NAME] = item.clientName
        map[RATE] = item.rate
        map[AMOUNT] = item.amount
        map[AMOUNT_PAID] = item.amountPaid
        map[AMOUNT_ADVANCE] = item.amountAdvance
        map[RECORD_TYPE] = item.recordType
        map[QUANTITY] = item.quantity
        map[TIMESTAMP] = item.timestamp
        map[ORDER_TIMESTAMP] = item.orderTimeStamp
        map[CLIENT_ID] = item.clientId
        map[SHIFT] = MORNING_SHIFT
        map[DATE] = getTodayDate()

        db.collection(COLLECTIONS_RECORDS)
            .document(item.id)
            .set(map, SetOptions.merge())
            .addOnFailureListener {
                Snackbar.make(binding.root, "Something went wrong!", Snackbar.LENGTH_SHORT).show()
            }

    }

    companion object {

//        private const val IS_REPOSITION = "isReposition"
//        private const val REPOSITION = "reposition"
//
//        fun setReposition(mContext: Context, position: Int) {
//            SharedPref.setInt(mContext, REPOSITION, position)
//            SharedPref.setBoolean(mContext, IS_REPOSITION, true)
//        }

//        fun clearReposition(mContext: Context) {
//            SharedPref.setBoolean(mContext, IS_REPOSITION, false)
//            SharedPref.setInt(mContext, REPOSITION, 0)
//        }

       //  fun isReposition(mContext: Context) = SharedPref.getBoolean(mContext, IS_REPOSITION)

       // fun reposition(mContext: Context) = SharedPref.getInt(mContext, REPOSITION)

    }

}