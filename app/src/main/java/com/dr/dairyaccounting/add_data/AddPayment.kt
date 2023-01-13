package com.dr.dairyaccounting.add_data

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.DialogCalendarBinding
import com.dr.dairyaccounting.databinding.FragmentAddPaymentBinding
import com.dr.dairyaccounting.databinding.RowItemPaymentsBinding
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_ADVANCE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_PAID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_NAME
import com.dr.dairyaccounting.utils.AppConstants.COLLECTIONS_RECORDS
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.RATE
import com.dr.dairyaccounting.utils.AppConstants.RECORDS
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.TIMESTAMP
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 *
 *  This class or fragment currently is not in use
 *  It's functionalities are shifted to SummaryByPerson Class (com.dr.dairyaccounting.records.summary)
 *
 */

class AddPayment : Fragment() {

    private lateinit var binding: FragmentAddPaymentBinding
    private lateinit var mContext: Context
    private var startingTime = 0L
    private var endTime = System.currentTimeMillis()
    private lateinit var clientId: String
    private lateinit var records: ArrayList<RecordsEntity>
    private lateinit var room: MyDatabase
    private var record: RecordsEntity? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddPaymentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startingTime = getMonthTimeRange().first
        endTime = getMonthTimeRange().last

        records = GsonBuilder().create().fromJson(arguments?.getString(
            RECORDS), object : TypeToken<ArrayList<RecordsEntity?>?>(){}.type)

        room = MyDatabase.getDatabase(mContext)

        if (records.isNotEmpty()) {
            record = records[0]
            clientId = record?.clientId ?: ""
        }

        Handler(Looper.getMainLooper()).post {
            try {
                updateUi()
                initData()
            } catch (e: Exception) { }
        }

        actions()

    }

    private fun initData() {

        binding.rv.removeAllViewsInLayout()

        room.recordDao().getRecordsByIClientIdLive(clientId).observe(
            viewLifecycleOwner
        ) {

            val list = it as ArrayList<RecordsEntity>

            list.removeIf { e ->
                e.timestamp !in startingTime..endTime
            }
            // in the total all records of user in timeRange is included, So, here according paid amount is not filtered

            total(list)

            list.removeIf { e ->
                e.amountPaid <= 0f
            }

            binding.noResult.visibility = View.GONE

            if (list.isEmpty()) {
                binding.noResult.visibility = View.VISIBLE
            }

            binding.rv.adapter = AdapterPayments(list)
        }

    }

    private fun actions() {

        binding.startingDate.setOnClickListener {
            calendarDialog(true)
        }

        binding.endDate.setOnClickListener {
            calendarDialog(false)
        }

        binding.save.setOnClickListener {
            if (binding.etPayment.text.toString().trim().isNotEmpty()) {
                saveData(binding.etPayment.text.toString().toFloat())
                binding.etPayment.setText("")
            }
        }

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun saveData(amount: Float) {

        record?.amountPaid = amount
        record?.date = getTodayDate(record?.timestamp ?: System.currentTimeMillis())

        if (record == null) {
            Toast.makeText(mContext, "Record is null", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(IO)
            .launch {
                try {
                    record?.let {
                        room.recordDao().insertRecord(
                            it
                        )
                    }
                } catch (e: Exception) {
                    record?.let {
                        room.recordDao().updateRecord(
                            it
                        )
                    }
                }
            }

        CoroutineScope(IO)
            .launch {

                if (record != null) {

                    val map = HashMap<String, Any>()
                    map[CLIENT_NAME] = record?.clientName ?: ""
                    map[RATE] = record?.rate ?: 0f
                    map[AMOUNT] = record?.amount ?: 0f
                    map[AMOUNT_PAID] = record?.amountPaid ?: 0f
                    map[AMOUNT_ADVANCE] = record?.amountAdvance ?: 0f
                    map[RECORD_TYPE] = record?.recordType ?: ""
                    map[QUANTITY] = record?.quantity ?: 0f
                    map[TIMESTAMP] = record?.timestamp ?: 0L
                    map[ORDER_TIMESTAMP] = record?.orderTimeStamp ?: 0L
                    map[CLIENT_ID] = record?.clientId ?: ""

                    db.collection(COLLECTIONS_RECORDS)
                        .document(record?.id ?: "_")
                        .set(map, SetOptions.merge())
                        .addOnFailureListener {
                            Snackbar.make(
                                binding.root,
                                "Something went wrong!",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                }
            }

        Snackbar.make(binding.root, "Saved Successful", Snackbar.LENGTH_SHORT).show()

    }

    private fun updateUi() {

        binding.startingDate.text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(startingTime))
        binding.endDate.text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(endTime))
        binding.tb.subtitle = room.clientDao().getClientById(clientId)[0].clientName
    }

    private fun calendarDialog(isThisStartingTime: Boolean)  {

        val d = Dialog(mContext)
        val dBinding = DialogCalendarBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        d.show()

        dBinding.calendar.maxDate = Date().time

        dBinding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val ft = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            val dx = ft.parse("$year/${month+1}/$dayOfMonth").time
            dBinding.calendar.date = dx
        }

        dBinding.select.setOnClickListener {
            d.dismiss()
            if (isThisStartingTime) {
                startingTime = dBinding.calendar.date
            } else {
                endTime = dBinding.calendar.date
            }
            updateUi()
            initData()
        }

    }

    private fun getMonthTimeRange() = run {

        val calStart: Calendar = GregorianCalendar()
        calStart.set(Calendar.DAY_OF_MONTH, 1)
        calStart.set(Calendar.HOUR_OF_DAY, 5)
        calStart.set(Calendar.MINUTE, 30)
        calStart.set(Calendar.SECOND, 0)
        calStart.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calStart.time.time

        calStart.set(Calendar.DAY_OF_MONTH, calStart.getActualMaximum(Calendar.DAY_OF_MONTH))
        calStart.set(Calendar.HOUR_OF_DAY, calStart.getActualMaximum(Calendar.HOUR_OF_DAY))
        calStart.set(Calendar.MINUTE, calStart.getActualMaximum(Calendar.MINUTE))
        calStart.set(Calendar.SECOND, calStart.getActualMaximum(Calendar.SECOND))
        calStart.set(Calendar.MILLISECOND, calStart.getActualMaximum(Calendar.MILLISECOND))
        calStart.add(Calendar.HOUR_OF_DAY, 5)
        calStart.add(Calendar.MINUTE, 30)
        val endOfMonth = calStart.time.time

        startOfMonth..endOfMonth
    }

    private fun total(list: ArrayList<RecordsEntity> ) {

        var totalAmount = 0f
        var totalPaid = 0f

        for (i in list) {
            totalAmount += i.amount
            totalPaid += i.amountPaid
        }

        binding.totalPaid.text = "Paid: ₹" + String.format("%.2f", totalPaid)
        binding.totalAmount.text = "Amount: ₹" + String.format("%.2f", totalAmount)
        binding.balance.text = "Balance: ₹" + String.format("%.2f", totalAmount - totalPaid)

    }

    private inner class AdapterPayments(val list: ArrayList<RecordsEntity>): RecyclerView.Adapter<AdapterPayments.PaymentHolder>() {

        inner class PaymentHolder(itemView: View, val dBinding: RowItemPaymentsBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentHolder {
            val dBinding = RowItemPaymentsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PaymentHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(holder: PaymentHolder, position: Int) {

            holder.dBinding.srNo.text = (position + 1).toString()
            holder.dBinding.amount.text = list[position].amountPaid.toString()
            holder.dBinding.date.text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(list[position].timestamp))

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

}