package com.dr.dairyaccounting.records.summary

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.iterator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.adapters.AdapterPersonData
import com.dr.dairyaccounting.add_data.AddRecords.Companion.getMonthNow
import com.dr.dairyaccounting.add_data.AddRecords.Companion.getYearNow
import com.dr.dairyaccounting.add_data.AddRecords.Companion.timeStampNow
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentSummaryByPersonBinding
import com.dr.dairyaccounting.databinding.RowItemPersonDataBinding
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_ADVANCE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_PAID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_NAME
import com.dr.dairyaccounting.utils.AppConstants.DRAWABLE_RIGHT
import com.dr.dairyaccounting.utils.AppConstants.IS_PAYMENT_INFO
import com.dr.dairyaccounting.utils.AppConstants.MONTH
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.RATE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.AppConstants.TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.AppFunctions
import com.dr.dairyaccounting.utils.AppFunctions.Companion.generateBillExcel
import com.dr.dairyaccounting.utils.AppFunctions.Companion.getMonthInInt
import com.dr.dairyaccounting.utils.AppFunctions.Companion.getMonthInString
import com.dr.dairyaccounting.utils.AppFunctions.Companion.monthlyRecordByClient
import com.dr.dairyaccounting.utils.AppFunctions.Companion.openFile
import com.dr.dairyaccounting.utils.AppFunctions.Companion.shareFile
import com.dr.dairyaccounting.utils.BillGenerateCallbacks
import com.dr.dairyaccounting.utils.Utils.Companion.timeRangeOfMonth
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SummaryByPerson : Fragment() {

    private lateinit var binding: FragmentSummaryByPersonBinding
    private lateinit var mContext: Context
    private val cal = Calendar.getInstance()
    private var year = cal.get(Calendar.YEAR)
    private var month = cal.get(Calendar.MONTH)
    private lateinit var clientId: String
    private lateinit var recordType: String
    private lateinit var room: MyDatabase
    private var isPaymentInfo: Boolean = false
    private lateinit var shift: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryByPersonBinding.inflate(layoutInflater)

        shift = arguments?.getString(SHIFT).toString()
        recordType = arguments?.getString(RECORD_TYPE).toString()
        isPaymentInfo = arguments?.getBoolean(IS_PAYMENT_INFO, false) == true
        room = MyDatabase.getDatabase(mContext)
        clientId = arguments?.getString(CLIENT_ID).toString()
        month = arguments?.getInt(MONTH) ?: cal.get(Calendar.MONTH)
        year = arguments?.getInt(YEAR) ?: cal.get(Calendar.YEAR)

        actions()

        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun actions() {

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.etPayment.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etPayment.right - binding.etPayment.compoundDrawables[DRAWABLE_RIGHT].bounds.width() * 2)) {
                    // save the payment info
                    if (binding.etPayment.text.toString().trim().isEmpty()) {
                        binding.etPayment.error = "Enter Amount"
                    } else {
                        savePayment()
                    }
                }
            }
            false
        }

        toolBar()
    }

    private fun savePayment() {

        val db = FirebaseFirestore.getInstance()

        val todayRecord = room.recordDao().getTodayRecordsByClientIdAndShift(
            recordType,
            getTodayDate(),
            clientId = clientId,
            shift = shift
        ) as ArrayList<RecordsEntity>

        if (todayRecord.isEmpty()) {
            todayRecord.add(generateTodayRecord())
        }

        val record = todayRecord[0]

        record.amountPaid = binding.etPayment.text.toString().toFloat()

        CoroutineScope(Dispatchers.IO)
            .launch {
                try {
                    record.let {
                        room.recordDao().insertRecord(
                            it
                        )
                    }
                } catch (e: Exception) {
                    record.let {
                        room.recordDao().updateRecord(
                            it
                        )
                    }
                }
            }

        CoroutineScope(Dispatchers.IO)
            .launch {

                val map = HashMap<String, Any>()
                map[CLIENT_NAME] = record.clientName
                map[RATE] = record.rate
                map[AMOUNT] = record.amount
                map[AMOUNT_PAID] = record.amountPaid
                map[AMOUNT_ADVANCE] = record.amountAdvance
                map[RECORD_TYPE] = record.recordType
                map[QUANTITY] = record.quantity
                map[TIMESTAMP] = record.timestamp
                map[ORDER_TIMESTAMP] = record.orderTimeStamp
                map[CLIENT_ID] = record.clientId

                db.collection(AppConstants.COLLECTIONS_RECORDS)
                    .document(record.id)
                    .set(map, SetOptions.merge())
                    .addOnFailureListener {
                        Snackbar.make(
                            binding.root,
                            "Something went wrong!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

            }

        Handler(Looper.getMainLooper())
            .postDelayed({ updateUi() }, 100)

        binding.etPayment.setText("")
        Snackbar.make(binding.root, "Saved Successful", Snackbar.LENGTH_SHORT).show()

    }

    private fun getBalance(amount: Float, paid: Float, lastBalance: Float) = run {
        if (amount - paid + (lastBalance) < 0) 0f else amount - paid + (lastBalance) /* last balance is by default negative */
    }

    private fun getLastBalance() = run {

        val time = timeRangeOfMonth("${getMonthInString(month)} $year").first
        val records = room.recordDao().getRecordsByClientIdAndRecordType(clientId, recordType) as ArrayList<RecordsEntity>

        var amount = 0f
        var paid = 0f

        for (r in records) {
            if (r.timestamp < time) {
                amount += r.amount
                paid += r.amountPaid
            }
        }

        amount - paid
    }

    private fun generateTodayRecord() = run {

        val client = room.clientDao().getClientById(clientId)[0]

        RecordsEntity(
            id = UUID.randomUUID().toString(),
            clientId = client.id,
            clientName = client.clientName,
            quantity = 0f,
            shift = shift,
            rate = client.rate,
            amount = 0f,
            amountPaid = 0f,
            amountAdvance = 0f,
            recordType = client.accountType,
            timestamp = getTimestamp(),
            orderTimeStamp = client.orderTimeStamp,
            date = getTodayDate()
        )

    }

    private fun getTodayDate() = run {
        SimpleDateFormat("dd/MM/y", Locale.ENGLISH).format(Date(getTimestamp()))
    }

    private fun getTimestamp() = run {
        if (month == getMonthNow() && year == getYearNow()) {
            timeStampNow()
        } else {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.time.time
        }
    }

    private fun toolBar() {

        val menu = binding.tb.menu
        val changeYear = menu[0]
        val changeMonth = menu[1]
        val excel = menu[2]
        val pdf = menu[3]
        val print = menu[4]
        val share = menu[5]

        for (sub in changeYear.subMenu) {
            sub.setOnMenuItemClickListener {
                changeYear(sub.title.toString().toInt())
                false
            }
        }

        for (sub in changeMonth.subMenu) {
            sub.setOnMenuItemClickListener {
                changeMonth(getMonthInInt(sub.title.toString()))
                false
            }
        }

        pdf.setOnMenuItemClickListener {
            // generate pdf for individual person
            generatePdf()
            false
        }

        print.setOnMenuItemClickListener {
            // generate pdf for individual person and print
            generatePdf(isPrint = true)
            false
        }

        excel.setOnMenuItemClickListener {
            // generate excel for individual person
            generateExcel()
            false
        }

        share.setOnMenuItemClickListener {
             generatePdf(isShare = true)
            false
        }
    }

    private fun generatePdf(isPrint: Boolean = false, isShare: Boolean = false) =
        AppFunctions.generateBillPdf(mContext,
            clientArray(),
            recordType,
            month,
            year,
            isPrint,
            object : BillGenerateCallbacks {
                override fun onSuccessListener(fileName: String) {
                    if (!isPrint && !isShare) {
                        val file = File(fileName)
                        Snackbar.make(binding.root, "Download Successful", Snackbar.LENGTH_SHORT)
                            .setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF018786")))
                            .setActionTextColor(Color.WHITE)
                            .setTextColor(Color.parseColor("#ECECEC"))
                            .setAction(if (fileName.endsWith(".pdf")) "Open" else "") {
                                if (file.exists() && fileName.endsWith(".pdf")) {
                                    openFile(mContext, fileName)
                                }
                            }.show()
                    }
                    if (isShare) {
                        // share pdf
                        shareFile(mContext, fileName)
                    }
                }

                override fun onFailureListener(error: String) {
                    Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            }
        )

    private fun generateExcel() =
        generateBillExcel(
            mContext,
            clientArray(),
            recordType,
            month,
            year,
            object : BillGenerateCallbacks {
                override fun onSuccessListener(fileName: String) {
                    Snackbar.make(binding.root, "Download Successful", Snackbar.LENGTH_SHORT)
                        .setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF018786")))
                        .setActionTextColor(Color.WHITE)
                        .setTextColor(Color.parseColor("#ECECEC"))
                        .setAction(if (fileName.endsWith(".xls")) "Open" else "") {
                            if (fileName.endsWith(".xls")) {
                                openFile(mContext, fileName)
                            }
                        }.show()
                }

                override fun onFailureListener(error: String) {
                    Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            }
        )

    private fun clientArray() = run {
        val list = ArrayList<ClientsEntity>()
        val client = room.clientDao().getClientById(clientId)[0]
        list.add(client)
        list
    }

    private fun changeYear(mYear: Int) {
        year = mYear
        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }
    }

    private fun changeMonth(mMonth: Int) {
        month = mMonth
        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }
    }

    private fun updateUi() {

        if (isPaymentInfo) {
            binding.etPayment.visibility = View.VISIBLE
        }

        binding.tb.title = getClientName()
        binding.tb.subtitle = getMonthInString(month) + " " + year

        val list = monthlyRecordByClient(clientId, room, recordType, month, year)
        binding.rv.adapter = AdapterPersonData(list)
        totalSummaryByPerson(list)

    }

    private fun totalSummaryByPerson(list: ArrayList<ItemPersonData>) {

        var totalAmount = 0f
        var totalQuantityMorning = 0f
        var totalQuantityEvening = 0f
        var totalPaid = 0f

        for (i in list) {
            totalAmount += i.amount
            totalQuantityMorning += i.mQnt
            totalQuantityEvening += i.eQnt
            totalPaid += i.paid
        }

        binding.lastBalance.text = "Last Blc/Adv: " + String.format("%.2f", getLastBalance())
        binding.amount.text = String.format("%.2f", totalAmount)
        binding.quantityMorning.text = String.format("%.2f", totalQuantityMorning)
        binding.quantityEvening.text = String.format("%.2f", totalQuantityEvening)
        binding.amountPaid.text = String.format("%.2f", totalPaid)
        val advance = if (totalPaid - totalAmount > 0) totalPaid - totalAmount else 0f

        val balance = getBalance(totalAmount, totalPaid, getLastBalance())

        if (balance > 0) {
            binding.balance.text = "Balance: " + String.format("%.2f", balance)
        }
        if (advance > 0) {
            binding.balance.text = "Advance: " + String.format("%.2f", advance)
        }

    }

    private fun getClientName() = run {
        val client = room.clientDao().getClientById(clientId)
        if (client.isNotEmpty()) client[0].clientName else "Client not exist"
    }

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

}


class ItemPersonData(
    val date: String,
    val mQnt: Float,
    val eQnt: Float,
    val rate: String,
    val amount: Float,
    val paid: Float
)

/*

    private fun getTimeRange() = run {

        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.YEAR, year)

        cal.set(Calendar.HOUR_OF_DAY,cal.getActualMinimum(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE,cal.getActualMinimum(Calendar.MINUTE))
        cal.set(Calendar.SECOND,cal.getActualMinimum(Calendar.SECOND))

        val start = cal.time.time

        cal.set(Calendar.HOUR_OF_DAY,cal.getActualMaximum(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE,cal.getActualMaximum(Calendar.MINUTE))
        cal.set(Calendar.SECOND,cal.getActualMaximum(Calendar.SECOND))

        val end = cal.time.time

        start..end
    }

 */