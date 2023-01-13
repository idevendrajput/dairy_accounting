package com.dr.dairyaccounting.records.summary

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.FragmentSummayChildBinding
import com.dr.dairyaccounting.databinding.RowItemMonthlySummaryBinding
import com.dr.dairyaccounting.records.summary.Summary.Companion.excel
import com.dr.dairyaccounting.records.summary.Summary.Companion.pdf
import com.dr.dairyaccounting.records.summary.Summary.Companion.print
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.MONTH
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.TAB
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.AppFunctions
import com.dr.dairyaccounting.utils.AppFunctions.Companion.checkPermission
import com.dr.dairyaccounting.utils.GenerateSummary
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.getPurchaseByTimeRange
import com.dr.dairyaccounting.utils.Utils.Companion.getSaleByTimeRange
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import com.dr.dairyaccounting.utils.Utils.Companion.setDisplayName
import com.dr.dairyaccounting.utils.Utils.Companion.setDisplayPhone
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SummaryChild : Fragment() {

    private lateinit var binding: FragmentSummayChildBinding
    private lateinit var mContext: Context
    private var recordType = RECORD_TYPE_PURCHASE
    private lateinit var room: MyDatabase
    private var tab = 0

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummayChildBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        room = MyDatabase.getDatabase(mContext)
        tab = arguments?.getInt(TAB) as Int
        recordType = arguments?.getString(RECORD_TYPE).toString()

    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper())
            .post {
                initData()
            }
    }

    private fun initData() {

        binding.noResult.visibility = View.GONE

        val list: ArrayList<MonthlySummaryModel> = if (recordType == RECORD_TYPE_PURCHASE) {
            getTodayPurchases()
        } else {
            getTodaySales()
        }

        if (list.isEmpty()) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterSummaryRecords(list)

        total(list)

        var fileName = "Records ($recordType)"

        fileName += when (tab) {
            0 -> SimpleDateFormat("dd MMM y", Locale.ENGLISH).format(Date())
            1 -> SimpleDateFormat("MMM y", Locale.ENGLISH).format(Date())
            else -> {
                val calendar = GregorianCalendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                SimpleDateFormat("MMM y", Locale.ENGLISH).format(calendar.time)
            }
        }

        excel.setOnMenuItemClickListener {
            createExcel(list)
            false
        }

        pdf.setOnMenuItemClickListener {
            generatePdf(list)
            false
        }

        print.setOnMenuItemClickListener {
            generatePdf(list, true)
            false
        }

        excel.isVisible = true
        pdf.isVisible = true
        print.isVisible = true

    }

    private fun createExcel(list: ArrayList<MonthlySummaryModel>) {

        Handler(Looper.getMainLooper())
            .post {

                val wb = AppFunctions.createRecordWorkBook(
                    mContext,
                    list, getTodayDate()
                )

                wb?.let {
                    if (AppFunctions.storeExcelInStorage(
                            it,
                            "Summary/${AppFunctions.makeFirstLaterCapital(recordType)}/Excel/${
                                AppFunctions.getMonthInString(
                                    Calendar.getInstance().get(Calendar.MONTH)
                                )
                            } ${Calendar.getInstance().get(Calendar.YEAR)}",
                            getTodayDate()
                        )
                    ) {
                        Toast.makeText(mContext, "Successfully downloaded", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

    }

    private fun getTodayDate() = run {
        Utils.getTodayDate().replace("/", " ")
    }

    private fun generatePdf(list: ArrayList<MonthlySummaryModel>, isPrint: Boolean = false) {

        if (!checkPermission(mContext)) {
            Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = getTodayDate()

        val workbook = AppFunctions.createRecordWorkBook(
            mContext,
            list, fileName
        )

        if (workbook != null) {

            val my_worksheet = workbook.getSheetAt(0)

            val rowIterator: Iterator<Row> = my_worksheet.iterator()

            val dir = File(
                Environment.getExternalStorageDirectory().toString() + "/DoodhWala/Summary/${
                    AppFunctions.makeFirstLaterCapital(
                        recordType
                    )
                }" + "/PDF/${
                    AppFunctions.getMonthInString(Calendar.getInstance().get(Calendar.MONTH))
                } ${Calendar.getInstance().get(Calendar.YEAR)}"
            )

            val filePath = File(dir, "/$fileName.pdf")

            if (!dir.exists()) {
                dir.mkdirs()
            }

            if (!filePath.exists()) {
                filePath.createNewFile()
            }

            val baseFont = Font.FontFamily.HELVETICA
            val regularFontFamily = Font.FontFamily.HELVETICA

            val regular = Font(regularFontFamily, 10f, Font.NORMAL, BaseColor.BLACK)
            val headerFont = Font(baseFont, 20f, Font.BOLD, BaseColor.BLACK)
            val subHeadingFont = Font(baseFont, 15f, Font.BOLD, BaseColor.BLACK)

            val pdfDoc = Document(PageSize.A4, 15f, 15f, 15f, 15f)
            pdfDoc.addCreationDate()
            pdfDoc.addCreator("Doodhwala App")
            PdfWriter.getInstance(pdfDoc, FileOutputStream(filePath))
            pdfDoc.open()

            val tableHeader = PdfPTable(2)
            val headingCell = PdfPCell(Phrase(Utils.getDisplayName(mContext), headerFont))
            val headingCell2 = PdfPCell(Phrase(Utils.getDisplayPhone(mContext), headerFont))
            headingCell.border = Rectangle.NO_BORDER
            headingCell.horizontalAlignment = Element.ALIGN_LEFT
            headingCell2.border = Rectangle.NO_BORDER
            headingCell2.horizontalAlignment = Element.ALIGN_RIGHT
            tableHeader.addCell(headingCell)
            tableHeader.addCell(headingCell2)

            val subHeader = PdfPTable(1)
            val subHeaderCell = PdfPCell(Phrase(fileName, subHeadingFont))
            subHeaderCell.border = Rectangle.NO_BORDER
            subHeaderCell.setPadding(10f)
            subHeaderCell.horizontalAlignment = Element.ALIGN_CENTER
            subHeader.addCell(subHeaderCell)

            pdfDoc.add(tableHeader)
            pdfDoc.add(subHeader)

            val my_table = PdfPTable(7)
            var table_cell: PdfPCell?

            var i = 0

            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                if (i != 0) {
                    val cellIterator: Iterator<Cell> = row.cellIterator()
                    while (cellIterator.hasNext()) {
                        val cell: Cell = cellIterator.next()
                        when (cell.cellType) {
                            Cell.CELL_TYPE_STRING -> {
                                table_cell = PdfPCell(Phrase(cell.stringCellValue, regular))
                                table_cell.borderWidth = 0.5f
                                if (i == 1) {
                                    table_cell.backgroundColor = BaseColor.YELLOW
                                    table_cell.horizontalAlignment = Element.ALIGN_CENTER
                                    table_cell.setPadding(10f)
                                    table_cell.verticalAlignment = Element.ALIGN_CENTER
                                }
                                my_table.addCell(table_cell)
                            }
                        }
                    }
                }
                i++
            }

            pdfDoc.add(my_table)
            pdfDoc.close()

            if (isPrint) {
                AppFunctions.print(mContext, filePath.path)
            } else {
                Toast.makeText(mContext, "Download successful", Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun getTodaySales() = getSaleByTimeRange(mContext, getTodayTimeRange())

    private fun getTodayPurchases() = getPurchaseByTimeRange(mContext, getTodayTimeRange())

    private fun getTodayTimeRange() = run {

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE))
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND))

        val start = cal.time.time

        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE))
        cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND))

        val end = cal.time.time

        start..end
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
        //binding.lastBalance.text = String.format("%.2f", totalLastBalance)
        binding.advance.text = String.format("%.2f", totalAdvance)

        totalQuantityEvening + totalQuantityMorning
    }

    inner class AdapterSummaryRecords(
        private val list: ArrayList<MonthlySummaryModel>
    ) : RecyclerView.Adapter<AdapterSummaryRecords.RecordHolder>() {

        inner class RecordHolder(itemView: View, val dBinding: RowItemMonthlySummaryBinding) :
            RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecordHolder {
            val dBinding = RowItemMonthlySummaryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return RecordHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(
            holder: RecordHolder,
            position: Int
        ) {

            val srNo = position + 1
            holder.dBinding.srNo.text = srNo.toString()
            holder.dBinding.name.text = list[position].clientName
            holder.dBinding.quantityMorning.text = list[position].morningQnt.toString()
            holder.dBinding.quantityEvening.text = list[position].eveningQnt.toString()
            holder.dBinding.balance.text = list[position].balance.toString()
            holder.dBinding.amount.text = list[position].amount.toString()
            holder.dBinding.amountPaid.text = list[position].paid.toString()
            holder.dBinding.advance.text = list[position].advance.toString()
            holder.dBinding.lastBalance.text = list[position].lastBalance.toString()

            holder.dBinding.lastBalance.visibility = View.GONE

//            holder.itemView.setOnClickListener {
//                val args = Bundle()
//                args.putString(CLIENT_ID, list[position].clientId)
//                args.putString(RECORD_TYPE, recordType)
//                args.putInt(YEAR, Calendar.getInstance().get(Calendar.YEAR))
//                args.putInt(MONTH, Calendar.getInstance().get(Calendar.MONTH))
//                findNavController().navigate(R.id.summaryByPerson, args)
//            }

        }

        override fun getItemCount() = list.size

    }

}

class SummaryItems(
    val morningQuantity: Float,
    var eveningQuantity: Float,
    var recordsEntity: RecordsEntity,
    var year: String = ""
)



