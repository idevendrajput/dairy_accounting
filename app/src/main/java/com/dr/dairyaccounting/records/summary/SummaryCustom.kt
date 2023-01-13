package com.dr.dairyaccounting.records.summary

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.databinding.DialogCalendarBinding
import com.dr.dairyaccounting.databinding.FragmentSummaryCustomBinding
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppFunctions
import com.dr.dairyaccounting.utils.Utils.Companion.getPurchaseByTimeRange
import com.dr.dairyaccounting.utils.Utils.Companion.getSaleByTimeRange
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SummaryCustom : Fragment() {

    private lateinit var binding : FragmentSummaryCustomBinding
    private lateinit var mContext: Context
    private var recordType = RECORD_TYPE_PURCHASE
    private var startingTime = startTimeDefault()
    private var endTime = System.currentTimeMillis()
    private lateinit var excel: MenuItem

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryCustomBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordType = arguments?.getString(AppConstants.RECORD_TYPE).toString()

        toolBar()
        initData()
        actions()

    }

    private fun startTimeDefault() = run {
        val cal = GregorianCalendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getGreatestMinimum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, 5)
        cal.add(Calendar.MINUTE, 30)
        cal.time.time
    }

    private fun initData() {

        updateUi()

        binding.rv.removeAllViewsInLayout()
        binding.noResult.visibility = View.GONE

        val list = if (recordType == RECORD_TYPE_PURCHASE) {
               getPurchaseByTimeRange(mContext, startingTime..endTime)
        } else {
              getSaleByTimeRange(mContext, startingTime..endTime)
        }

        if (list.isEmpty() || total(list) == 0f) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterSummaryMonthly(
            list,
            findNavController(),
            recordType, null, null
        )

        val timeFromTo = SimpleDateFormat("dd MM", Locale.ENGLISH).format(Date(startingTime)) + " To " + SimpleDateFormat("dd MM", Locale.ENGLISH).format(Date(endTime))
        val fileName = "Records ($recordType) $timeFromTo"

         excel.setOnMenuItemClickListener {
            // Summary().createExcel(mContext, list, fileName)
            false
        }

        total(list)
    }

    private fun updateUi() {

        binding.startingDate.text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(startingTime))
        binding.endDate.text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(endTime))

    }

    private fun actions() {

        binding.startingDate.setOnClickListener {
            calendarDialog(true)
        }

        binding.endDate.setOnClickListener {
            calendarDialog(false)
        }

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

    }

    private fun toolBar() {

        val menu = binding.tb.menu

        val sale = menu[0]
        val purchase = menu[1]
        excel = menu[2]

        sale.setOnMenuItemClickListener {
            recordType = AppConstants.RECORD_TYPE_SALE
            initData()
            false
        }

        purchase.setOnMenuItemClickListener {
            recordType = RECORD_TYPE_PURCHASE
            initData()
            false
        }

    }

    private fun total(list: ArrayList<MonthlySummaryModel>) = run {

        var totalAmount = 0f
        var totalQuantityMorning = 0f
        var totalQuantityEvening = 0f
        var totalBalance = 0f
        var totalPaid = 0f
        var totalAdvance = 0f
        var totalLastBalance = 0f

        for (i in list) {
            totalAmount += i.amount
            totalQuantityMorning += i.morningQnt
            totalQuantityEvening += i.eveningQnt
            totalPaid += i.paid
            totalBalance += i.balance
            totalAdvance += i.advance
            totalLastBalance += i.lastBalance
        }

        binding.amount.text = String.format("%.2f", totalAmount)
        binding.quantityMorning.text = String.format("%.2f", totalQuantityMorning)
        binding.quantityEvening.text = String.format("%.2f", totalQuantityEvening)
        binding.amountPaid.text = String.format("%.2f", totalPaid)
        binding.balance.text = String.format("%.2f", totalBalance)
        binding.lastBalance.text = String.format("%.2f", totalLastBalance)
        binding.advance.text = String.format("%.2f", totalAdvance)

        totalQuantityEvening + totalQuantityMorning

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
            initData()
        }

    }

}