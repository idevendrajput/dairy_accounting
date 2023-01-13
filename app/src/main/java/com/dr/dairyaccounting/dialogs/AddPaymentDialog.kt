package com.dr.dairyaccounting.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.iterator
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.adapters.AdapterPersonData
import com.dr.dairyaccounting.add_data.AddRecords.Companion.getMonthNow
import com.dr.dairyaccounting.add_data.AddRecords.Companion.getYearNow
import com.dr.dairyaccounting.add_data.PaymentChangeCallback
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentSummaryByPersonBinding
import com.dr.dairyaccounting.records.summary.ItemPersonData
import com.dr.dairyaccounting.records.summary.SummaryByPerson
import com.dr.dairyaccounting.ui.BaseDialog
import com.dr.dairyaccounting.utils.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class AddPaymentDialog(
    context: Context,
    val recordType: String,
    val room: MyDatabase,
    val item: RecordsEntity,
    val paymentChangeCallback: PaymentChangeCallback
) : BaseDialog(context) {

    val binding = FragmentSummaryByPersonBinding.inflate(this.layoutInflater)
    private var isBuilt = false
    private var year = getYearNow()
    private var month = getMonthNow()
    private var clientId = item.clientId

    fun build() = run {
        this.setContentView(binding.root)
        this.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        listeners()
        isBuilt = true
        this
    }

    override fun showDialog() {
        if (isBuilt) {
            super.showDialog()
            showLoading()
            Handler(Looper.getMainLooper())
                .postDelayed({
                    updateUi()
                    toolBar()
                }, 200)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {

        binding.tb.setNavigationOnClickListener {
            this.dismiss()
        }

        binding.etPayment.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etPayment.right - binding.etPayment.compoundDrawables[AppConstants.DRAWABLE_RIGHT].bounds.width() * 2)) {
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

    }

    private fun savePayment() {

        showLoading()

        var txt = binding.etPayment.text.toString()
        if (txt.startsWith("."))
            txt = "0".plus(txt)

        item.amountPaid = txt.toFloat()

        CoroutineScope(Dispatchers.IO)
            .launch {
                try {
                    room.recordDao().insertRecord(item)
                } catch (e: Exception) {
                    room.recordDao().updateRecord(item)
                }
            }

        CoroutineScope(IO)
            .launch {
                try {
                    room.tempRecordDao().insertRecord(Utils.recordItemToTempItem(item))
                } catch (e: Exception) {
                    room.tempRecordDao().updateRecord(Utils.recordItemToTempItem(item))
                }
            }

        Handler(Looper.getMainLooper())
            .postDelayed({
                updateUi()
                binding.etPayment.setText("")
                paymentChangeCallback.onPaymentChanged()
                Snackbar.make(binding.root, "Saved Successful", Snackbar.LENGTH_SHORT).show()
            }, 200)

    }

    private fun updateUi() {

        binding.etPayment.visibility = View.VISIBLE

        binding.tb.title = getClientName()
        binding.tb.subtitle = AppFunctions.getMonthInString(month) + " " + year

        val list = AppFunctions.monthlyRecordByClient(clientId, room, recordType, month, year)
        binding.rv.adapter = AdapterPersonData(list)
        total(list)
        Log.d("!--->", "" + list.size)
        dismissLoading()

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
                changeMonth(AppFunctions.getMonthInInt(sub.title.toString()))
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

    private fun clientArray() = run {
        val list = ArrayList<ClientsEntity>()
        val client = room.clientDao().getClientById(clientId)[0]
        list.add(client)
        list
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
                                    AppFunctions.openFile(mContext, fileName)
                                }
                            }.show()
                    }
                    if (isShare) {
                        // share pdf
                        AppFunctions.shareFile(mContext, fileName)
                    }
                }

                override fun onFailureListener(error: String) {
                    Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            }
        )

    private fun generateExcel() =
        AppFunctions.generateBillExcel(
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
                                AppFunctions.openFile(mContext, fileName)
                            }
                        }.show()
                }

                override fun onFailureListener(error: String) {
                    Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            }
        )

    private fun total(list: ArrayList<ItemPersonData>) {

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

    private fun getBalance(amount: Float, paid: Float, lastBalance: Float) = run {
        if (amount - paid + (lastBalance) < 0) 0f else amount - paid + (lastBalance) /* last balance is by default negative */
    }

    private fun getLastBalance() = run {

        val time = Utils.timeRangeOfMonth("${AppFunctions.getMonthInString(month)} $year").first
        val records = room.recordDao()
            .getRecordsByClientIdAndRecordType(clientId, recordType) as ArrayList<RecordsEntity>

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

    private fun getClientName() = run {
        val client = room.clientDao().getClientById(clientId)
        if (client.isNotEmpty()) client[0].clientName else "Client not exist"
    }

}

