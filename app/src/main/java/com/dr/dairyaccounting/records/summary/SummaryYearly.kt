package com.dr.dairyaccounting.records.summary

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.databinding.FragmentSummaryYearlyBinding
import com.dr.dairyaccounting.databinding.RowItemSummaryBinding
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.MONTH
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.getYearlyData
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.CellStyle
import kotlin.collections.ArrayList

class SummaryYearly : Fragment() {

    private lateinit var binding : FragmentSummaryYearlyBinding
    private lateinit var mContext: Context
    private var recordType = RECORD_TYPE_PURCHASE
    private var tab = 0
    private var year = "2022"

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryYearlyBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            tab = arguments?.getInt(AppConstants.TAB) as Int
            recordType = arguments?.getString(RECORD_TYPE).toString()
            year = arguments?.getString(YEAR).toString()
        } catch (e: Exception) { }

    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).post {
            initData()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.rv.removeAllViewsInLayout()
    }

    private fun initData() {

        binding.noResult.visibility = View.GONE

        val list = getYearlyData(mContext, year, recordType)

        if (list.isEmpty()) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterSummaryRecords(list)

        total(list)

        Summary.excel.isVisible = false
        Summary.pdf.isVisible = false
        Summary.print.isVisible = false

        binding.pb.visibility = View.GONE
    }

    private fun total(list: ArrayList<SummaryItems>) = run {

        var totalAmount = 0f
        var totalQuantityMorning = 0f
        var totalQuantityEvening = 0f
        var totalBalance = 0f
        var totalPaid = 0f

        for (i in list) {
            totalAmount += i.recordsEntity.amount
            totalQuantityMorning += i.morningQuantity
            totalQuantityEvening += i.eveningQuantity
            totalPaid += i.recordsEntity.amountPaid
            totalBalance = i.recordsEntity.amount - i.recordsEntity.amountPaid
        }

        binding.amount.text = String.format("%.2f", totalAmount)
        binding.quantityMorning.text = String.format("%.2f", totalQuantityMorning)
        binding.quantityEvening.text = String.format("%.2f", totalQuantityEvening)
        binding.amountPaid.text = String.format("%.2f", totalPaid)
        binding.balance.text = String.format("%.2f", totalBalance)

    }

    fun createExcel(mContext: Context, list: ArrayList<SummaryItems>, fileName: String) {

        Dexter.withContext(mContext)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    excelOutPut(mContext, list, fileName)
                }
                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {}
            }).check()

    }

    private fun excelOutPut(mContext: Context, list: ArrayList<SummaryItems>, fileName: String) {

        if (list.isEmpty()) {
            Toast.makeText(mContext, "No Record Found", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Utils.isExternalStorageAvailable() || Utils.isExternalStorageReadOnly()) {
            Toast.makeText(mContext, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
            return
        }

        // Workbook
        val workbook = HSSFWorkbook()

        // Sheet
        val sheet = workbook.createSheet(fileName)
        sheet.setColumnWidth(0, (15 * 400));
        sheet.setColumnWidth(1, (15 * 400));
        sheet.setColumnWidth(2, (15 * 400));
        sheet.setColumnWidth(3, (15 * 400));
        sheet.setColumnWidth(4, (15 * 400));
        sheet.setColumnWidth(5, (15 * 400));
        sheet.setColumnWidth(6, (15 * 400));

        //  Create row
        val headingRow = sheet.createRow(0)

        // cell style
        val cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = HSSFColor.YELLOW.index
        cellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle.alignment = CellStyle.ALIGN_LEFT

        for (i in 0..6) {
            val cell = headingRow.createCell(i)
            cell.setCellValue(when(i) {
                0-> "Sr. No."
                1-> "Month"
                2-> "Morning Quantity"
                3-> "Evening Quantity"
                4-> "Rate"
                5-> "Amount"
                6-> "Paid"
                else-> ""
            })
            cell.setCellStyle(cellStyle)
        }

        for ((i, r) in list.withIndex()) {

            /* row starts with +1 because row number 1 is heading and instruction row */

            val row = sheet.createRow(i + 1)
            row.createCell(0)
                .setCellValue((i + 1).toString())
            row.createCell(1)
                .setCellValue(r.recordsEntity.clientName)
            row.createCell(2)
                .setCellValue(r.morningQuantity.toString())
            row.createCell(3)
                .setCellValue(r.eveningQuantity.toString())
            row.createCell(4)
                .setCellValue(r.recordsEntity.rate.toString())
            row.createCell(5)
                .setCellValue(r.recordsEntity.amount.toString())
            row.createCell(6)
                .setCellValue(r.recordsEntity.amountPaid.toString())

        }

        Utils.storeExcelInStorage(workbook, fileName)
        Toast.makeText(mContext, "File Saved Successfully", Toast.LENGTH_SHORT).show()

    }

   inner class AdapterSummaryRecords(private val list: ArrayList<SummaryItems>): RecyclerView.Adapter<AdapterSummaryRecords.RecordHolder>() {

        inner class RecordHolder(itemView: View, val dBinding: RowItemSummaryBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecordHolder {
            val dBinding = RowItemSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RecordHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(
            holder: RecordHolder,
            position: Int
        ) {

            val srNo = position + 1
            holder.dBinding.srNo.text = srNo.toString()
            holder.dBinding.name.text = list[position].recordsEntity.clientName
            holder.dBinding.quantityMorning.text = list[position].morningQuantity.toString()
            holder.dBinding.quantityEvening.text = list[position].eveningQuantity.toString()
            holder.dBinding.balance.text = (list[position].recordsEntity.amount - list[position].recordsEntity.amountPaid).toString()
            holder.dBinding.amount.text = list[position].recordsEntity.amount.toString()
            holder.dBinding.amountPaid.text = list[position].recordsEntity.amountPaid.toString()

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(MONTH, list[position].recordsEntity.clientName)
                args.putString(YEAR, year)
                args.putString(RECORD_TYPE, recordType)
                findNavController().navigate(R.id.summaryByMonth, args)
            }

        }

        override fun getItemCount() = list.size

    }


}