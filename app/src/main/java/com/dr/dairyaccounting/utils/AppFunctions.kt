package com.dr.dairyaccounting.utils

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract.AUTHORITY
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.records.summary.ItemPersonData
import com.dr.dairyaccounting.records.summary.MonthlySummaryModel
import com.dr.dairyaccounting.utils.AppConstants.COLLECTION_CONTROLS
import com.dr.dairyaccounting.utils.AppConstants.DISPLAY_NAME
import com.dr.dairyaccounting.utils.AppConstants.DISPLAY_PHONE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.UTILS
import com.dr.dairyaccounting.utils.Utils.Companion.getDisplayName
import com.dr.dairyaccounting.utils.Utils.Companion.getDisplayPhone
import com.dr.dairyaccounting.utils.Utils.Companion.getSaleByMonth
import com.dr.dairyaccounting.utils.Utils.Companion.timeRangeOfMonth
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FontFamily
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.util.CellRangeAddress
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AppFunctions {

    companion object {

        fun checkPermission(mContext: Context): Boolean {
            return if (SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                val result = ContextCompat.checkSelfPermission(
                    mContext,
                    READ_EXTERNAL_STORAGE
                )
                val result1 = ContextCompat.checkSelfPermission(
                    mContext,
                    WRITE_EXTERNAL_STORAGE
                )
                result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
            }
        }

        private fun getRecords(mContext: Context, month: Int, year: Int, recordType: String) = run {
            if (recordType == RECORD_TYPE_PURCHASE) Utils.getPurchaseByMonth(
                mContext,
                "${getMonthInString(month)} $year"
            ) else getSaleByMonth(mContext, "${getMonthInString(month)} $year")
        }

        fun createRecordExcel(
            mContext: Context,
            recordType: String,
            month: Int,
            year: Int,
            callbacks: BillGenerateCallbacks
        ) {
            try {

                if (!checkPermission(mContext)) {
                    Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                    return
                }

                val fileName = getMonthInString(month) + " " + year

                val wb = createRecordWorkBook(
                    mContext,
                    getRecords(mContext, month, year, recordType), fileName
                )

                if (wb == null) {
                    callbacks.onFailureListener("Something went wrong")
                }

                wb?.let {
                    if (!storeExcelInStorage(
                            it,
                            "/Summary/${makeFirstLaterCapital(recordType)}/Excel/${
                                getMonthInString(
                                    month
                                )
                            } $year",
                            fileName
                        )
                    ) {
                        callbacks.onFailureListener("Something went wrong")
                    } else {
                        callbacks.onSuccessListener("Successfully downloaded")
                    }
                }

            } catch (e: Exception) {
                callbacks.onFailureListener("Something went wrong")
            }

        }

        fun storeExcelInStorage(workbook: HSSFWorkbook, dirPath: String, fileName: String) = run {
            try {
                val dir = File(
                    Environment.getExternalStorageDirectory().toString() + "/DoodhWala/$dirPath"
                )
                val filePath = File(dir, "/$fileName-${Random().nextInt(1000)}.xls")

                if (!dir.exists()) {
                    dir.mkdirs()
                }

                if (!filePath.exists()) {
                    filePath.createNewFile()
                }

                val fileOutputStream = FileOutputStream(filePath)
                workbook.write(fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
                true

            } catch (e: java.lang.Exception) {

                false
            }

        }

        fun makeFirstLaterCapital(string: String) = run {
            val fl = string.substring(0, 1).uppercase()
            val ns = string.substring(1..string.lastIndex)
            fl.plus(ns)
        }

        fun createRecordWorkBook(
            mContext: Context,
            list: ArrayList<MonthlySummaryModel>,
            fileName: String
        ): HSSFWorkbook? {

            if (!checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                return null
            }

            if (list.isEmpty()) {
                Toast.makeText(mContext, "No Record Found", Toast.LENGTH_SHORT).show()
                return null
            }

            if (!Utils.isExternalStorageAvailable() || Utils.isExternalStorageReadOnly()) {
                Toast.makeText(mContext, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT)
                    .show()
                return null
            }

            // Workbook
            val workbook = HSSFWorkbook()

            // Sheet
            val sheet = workbook.createSheet(fileName)
            sheet.setColumnWidth(0, (15 * 300))
            sheet.setColumnWidth(1, (15 * 150))
            sheet.setColumnWidth(2, (15 * 150))
            sheet.setColumnWidth(3, (15 * 150))
            sheet.setColumnWidth(4, (15 * 150))
            sheet.setColumnWidth(5, (15 * 150))
            sheet.setColumnWidth(6, (15 * 150))

            //  Create row
            val main = sheet.createRow(0)
            val headingRow = sheet.createRow(1)

            // cell style
            val headingCellStyle = workbook.createCellStyle()
            headingCellStyle.fillForegroundColor = HSSFColor.YELLOW.index
            headingCellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
            headingCellStyle.alignment = CellStyle.ALIGN_LEFT

            val basicCellStyle = workbook.createCellStyle()
            basicCellStyle.alignment = CellStyle.ALIGN_LEFT

            val titleCellStyle = workbook.createCellStyle()
            titleCellStyle.alignment = CellStyle.ALIGN_CENTER

            val basicFont = basicCellStyle.getFont(workbook)
            basicFont.fontHeightInPoints = 18.toShort()
            basicCellStyle.setFont(basicFont)


            for (i in 0..7) {
                val cell = main.createCell(i)
                cell.setCellValue(
                    when (i) {
                        0 -> fileName
                        else -> ""
                    }
                )
                cell.setCellStyle(titleCellStyle)
            }

            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))

            for (i in 0..7) {
                val cell = headingRow.createCell(i)
                cell.setCellValue(
                    when (i) {
                        0 -> "Name"
                        1 -> "M.Qty"
                        2 -> "E.Qty"
                        3 -> "Amount"
                        4 -> "Paid"
                        5 -> "Last/Ad. Bal"
                        6 -> "Balance"
                        7 -> "Advance"
                        else -> ""
                    }
                )
                cell.setCellStyle(headingCellStyle)
            }

            for ((i, r) in list.withIndex()) {

                /* row starts with +1 because row number 1 is heading and instruction row */

                val row = sheet.createRow(i + 2)

                val cell1 = row.createCell(0)
                cell1.setCellStyle(basicCellStyle)
                cell1.setCellValue(r.clientName)

                val cell2 = row.createCell(1)
                cell2.setCellStyle(basicCellStyle)
                cell2.setCellValue(String.format("%.2f", r.morningQnt))

                val cell3 = row.createCell(2)
                cell3.setCellStyle(basicCellStyle)
                cell3.setCellValue(String.format("%.2f", r.eveningQnt))

                val cell4 = row.createCell(3)
                cell4.setCellValue(String.format("%.2f", r.amount))
                cell4.setCellStyle(basicCellStyle)

                val cell5 = row.createCell(4)
                cell5.setCellValue(String.format("%.2f", r.paid))
                cell5.setCellStyle(basicCellStyle)

                val cell6 = row.createCell(5)
                cell6.setCellValue(String.format("%.2f", r.lastBalance))
                cell6.setCellStyle(basicCellStyle)

                val cell7 = row.createCell(6)
                cell7.setCellValue(String.format("%.2f", r.balance))
                cell7.setCellStyle(basicCellStyle)

                val cell8 = row.createCell(7)
                cell8.setCellValue(String.format("%.2f", r.advance))
                cell8.setCellStyle(basicCellStyle)

            }

            return workbook

        }

        fun createRecordPdf(
            mContext: Context,
            recordType: String,
            month: Int,
            year: Int,
            isPrint: Boolean,
            callbacks: BillGenerateCallbacks
        ) {

            if (!checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                return
            }

            try {

                val fileName = getMonthInString(month)

                val list = getRecords(mContext, month, year, recordType)

                val workbook = createRecordWorkBook(
                    mContext,
                    list, fileName
                )

                if (workbook != null) {

                    val my_worksheet = workbook.getSheetAt(0)

                    val rowIterator: Iterator<Row> = my_worksheet.iterator()

                    val dir = File(
                        Environment.getExternalStorageDirectory()
                            .toString() + "/DoodhWala/Summary/${makeFirstLaterCapital(recordType)}" + "/PDF/${
                            getMonthInString(
                                month
                            )
                        } $year"
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

                    val regular = Font(regularFontFamily, 8f, Font.NORMAL, BaseColor.BLACK)
                    val totalFont = Font(regularFontFamily, 9f, Font.BOLD, BaseColor.BLACK)
                    val headerFont = Font(baseFont, 20f, Font.BOLD, BaseColor.BLACK)
                    val subHeadingFont = Font(baseFont, 15f, Font.BOLD, BaseColor.BLACK)

                    val pdfDoc = Document(PageSize.A4, 15f, 15f, 15f, 15f)
                    pdfDoc.addCreationDate()
                    pdfDoc.addCreator("Doodhwala App")
                    PdfWriter.getInstance(pdfDoc, FileOutputStream(filePath))
                    pdfDoc.open()

                    val tableHeader = PdfPTable(2)
                    val headingCell = PdfPCell(Phrase(getDisplayName(mContext), headerFont))
                    val headingCell2 = PdfPCell(Phrase(getDisplayPhone(mContext), headerFont))
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

                    val my_table = PdfPTable(8)
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

                    val totalTable = PdfPTable(8)

                    for (cl in 0..7) {

                        val txt = when (cl) {
                            0 -> "Total"
                            1 -> morningTotal(list)
                            2 -> eveningTotal(list)
                            3 -> amountTotal(list)
                            4 -> paidTotal(list)
                            5 -> lastBlcAdv(list)
                            6 -> balance(list)
                            7 -> advance(list)
                            else -> "-"
                        }

                        val totalCell = PdfPCell(
                            Phrase(txt, totalFont)
                        )

                        totalCell.borderWidth = 0.5f
                        totalCell.setPadding(5f)
                        totalCell.backgroundColor = BaseColor.YELLOW
                        totalCell.horizontalAlignment = Element.ALIGN_LEFT
                        totalTable.addCell(totalCell)
                    }

                    pdfDoc.add(totalTable)

                    pdfDoc.close()

                    if (isPrint) {
                        print(mContext, filePath.path)
                    }

                    callbacks.onSuccessListener(fileName)

                }

            } catch (e: Exception) {
                callbacks.onFailureListener(e.message.toString())
            }

        }

        fun generateBillExcel(
            mContext: Context,
            clientList: ArrayList<ClientsEntity>,
            recordType: String,
            month: Int,
            year: Int,
            callbacks: BillGenerateCallbacks
        ) {

            if (!checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                return
            }

            try {

                val room = MyDatabase.getDatabase(mContext)

                for ((i, client) in clientList.withIndex()) {

                    val fileName = client.clientName

                    val wb = generateBillWorkBook(
                        mContext,
                        monthlyRecordByClient(client.id, room, recordType, month, year), fileName
                    )

                    wb?.let {
                        if (!storeExcelInStorage(
                                it,
                                "Bills/${makeFirstLaterCapital(recordType)}/Excel/${
                                    getMonthInString(month)
                                } $year",
                                fileName
                            )
                        ) {
                            Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                }

                val dir = File(
                    Environment.getExternalStorageDirectory().toString() + "/DoodhWala/Bills/${
                        makeFirstLaterCapital(
                            recordType
                        )
                    }/Excel/${getMonthInString(month)} $year"
                )

                callbacks.onSuccessListener(dir.absolutePath)

            } catch (e: Exception) {
                callbacks.onFailureListener("Something went wrong")
            }

        }

        private fun generateBillWorkBook(
            mContext: Context,
            list: ArrayList<ItemPersonData>,
            fileName: String
        ): HSSFWorkbook? {

            if (!checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                return null
            } else if (list.isEmpty()) {
                Toast.makeText(mContext, "No Record Found", Toast.LENGTH_SHORT).show()
                return null
            } else if (!Utils.isExternalStorageAvailable() || Utils.isExternalStorageReadOnly()) {
                Toast.makeText(mContext, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT)
                    .show()
                return null
            } else {
                // Workbook
                val workbook = HSSFWorkbook()

                // Sheet
                val sheet = workbook.createSheet(fileName)
                sheet.setColumnWidth(0, (15 * 300))
                sheet.setColumnWidth(1, (15 * 150))
                sheet.setColumnWidth(2, (15 * 150))
                sheet.setColumnWidth(3, (15 * 150))
                sheet.setColumnWidth(4, (15 * 150))
                sheet.setColumnWidth(5, (15 * 150))
                sheet.setColumnWidth(6, (15 * 150))

                //  Create row
                val main = sheet.createRow(0)
                val headingRow = sheet.createRow(1)

                // cell style
                val headingCellStyle = workbook.createCellStyle()
                headingCellStyle.fillForegroundColor = HSSFColor.YELLOW.index
                headingCellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
                headingCellStyle.alignment = CellStyle.ALIGN_LEFT

                val basicCellStyle = workbook.createCellStyle()
                basicCellStyle.alignment = CellStyle.ALIGN_LEFT

                val titleCellStyle = workbook.createCellStyle()
                titleCellStyle.alignment = CellStyle.ALIGN_CENTER

                val basicFont = basicCellStyle.getFont(workbook)
                basicFont.fontHeightInPoints = 18.toShort()
                basicCellStyle.setFont(basicFont)

                for (i in 0..5) {
                    val cell = main.createCell(i)
                    cell.setCellValue(
                        when (i) {
                            0 -> fileName
                            else -> ""
                        }
                    )
                    cell.setCellStyle(titleCellStyle)
                }

                sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))

                for (i in 0..5) {
                    val cell = headingRow.createCell(i)
                    cell.setCellValue(
                        when (i) {
                            0 -> "Date"
                            1 -> "M.Qty"
                            2 -> "E.Qty"
                            3 -> "Rate"
                            4 -> "Amount"
                            5 -> "Paid"
                            else -> ""
                        }
                    )
                    cell.setCellStyle(headingCellStyle)
                }

                for ((i, r) in list.withIndex()) {

                    /* row starts with +1 because row number 1 is heading and instruction row */

                    val row = sheet.createRow(i + 2)

                    val cell1 = row.createCell(0)
                    cell1.setCellStyle(basicCellStyle)
                    cell1.setCellValue(r.date)

                    val cell2 = row.createCell(1)
                    cell2.setCellStyle(basicCellStyle)
                    cell2.setCellValue(String.format("%.2f", r.mQnt))

                    val cell3 = row.createCell(2)
                    cell3.setCellStyle(basicCellStyle)
                    cell3.setCellValue(String.format("%.2f", r.eQnt))

                    val cell4 = row.createCell(3)
                    cell4.setCellValue(r.rate)
                    cell4.setCellStyle(basicCellStyle)

                    val cell5 = row.createCell(4)
                    cell5.setCellValue(String.format("%.2f", r.amount))
                    cell5.setCellStyle(basicCellStyle)

                    val cell6 = row.createCell(5)
                    cell6.setCellValue(String.format("%.2f", r.paid))
                    cell6.setCellStyle(basicCellStyle)

                }

                return workbook
            }
        }

        fun generateBillPdf(
            mContext: Context,
            clientList: ArrayList<ClientsEntity>,
            recordType: String,
            month: Int,
            year: Int,
            isPrint: Boolean,
            callbacks: BillGenerateCallbacks
        ) {

            if (!checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                return
            }

            try {

                val room = MyDatabase.getDatabase(mContext)

                val dir = if (clientList.size == 1) {
                    File(
                        Environment.getExternalStorageDirectory()
                            .toString() + "/DoodhWala/Bills/${makeFirstLaterCapital(recordType)}" + "/PDF/${
                            getMonthInString(
                                month
                            )
                        } $year/${clientList[0].clientName}"
                    )
                } else {
                    File(
                        Environment.getExternalStorageDirectory()
                            .toString() + "/DoodhWala/Bills/${makeFirstLaterCapital(recordType)}" + "/PDF/${
                            getMonthInString(
                                month
                            )
                        } $year/"
                    )
                }

                val fileName =
                    if (clientList.size == 1) clientList[0].clientName else getMonthInString(month)
                val filePath = File(dir, "/$fileName.pdf")

                if (!dir.exists()) {
                    dir.mkdirs()
                }

                if (!filePath.exists()) {
                    filePath.createNewFile()
                }

                val pdfDoc = Document(PageSize.A4, 15f, 15f, 40f, 15f)
                pdfDoc.addCreationDate()
                pdfDoc.addCreator("Doodhwala App")
                PdfWriter.getInstance(pdfDoc, FileOutputStream(filePath))
                pdfDoc.open()

                for ((i, client) in clientList.withIndex()) {
                    printBillDoc(
                        pdfDoc,
                        mContext,
                        monthlyRecordByClient(client.id, room, recordType, month, year),
                        clientName = client.clientName,
                        clientId = client.id,
                        recordType = recordType,
                        getMonthInString(month) + " " + year
                    )
                    if (i < clientList.size)
                        pdfDoc.newPage()
                }

                pdfDoc.close()

                callbacks.onSuccessListener(filePath.absolutePath)

                if (isPrint) {
                    print(mContext, filePath.absolutePath)
                }

            } catch (e: Exception) {
                callbacks.onFailureListener(e.message.toString())
            }

        }

        fun print(mContext: Context, filePath: String) {

            val printManager = mContext.getSystemService(Context.PRINT_SERVICE) as PrintManager

            try {
                val printAdapter = PdfDocumentAdapter(mContext, filePath)
                printManager.print("Document", printAdapter, PrintAttributes.Builder().build())
            } catch (e: Exception) {

            }
        }

        private fun printBillDoc(
            pdfDoc: Document,
            mContext: Context,
            list: ArrayList<ItemPersonData>,
            clientName: String,
            clientId: String,
            recordType: String,
            monthYear: String
        ) {

            if (!checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                return
            }

            val workbook = generateBillWorkBook(mContext, list, clientName)

            if (workbook != null) {

                val my_worksheet = workbook.getSheetAt(0)
                val rowIterator: Iterator<Row> = my_worksheet.iterator()

                val baseFont = Font.FontFamily.HELVETICA
                val regularFontFamily = Font.FontFamily.HELVETICA

                val regular = Font(regularFontFamily, 12f, Font.NORMAL, BaseColor.BLACK)
                val totalFont = Font(regularFontFamily, 13f, Font.BOLD, BaseColor.BLACK)
                val headerFont = Font(baseFont, 18f, Font.BOLD, BaseColor.BLACK)
                val subHeadingFont = Font(baseFont, 14f, Font.BOLD, BaseColor.BLACK)

                val tableHeader = PdfPTable(2)
                val headingCell = PdfPCell(Phrase(getDisplayName(mContext), headerFont))
                val headingCell2 = PdfPCell(Phrase(getDisplayPhone(mContext), headerFont))
                headingCell.border = Rectangle.NO_BORDER
                headingCell.horizontalAlignment = Element.ALIGN_LEFT
                headingCell2.border = Rectangle.NO_BORDER
                headingCell2.horizontalAlignment = Element.ALIGN_RIGHT
                tableHeader.addCell(headingCell)
                tableHeader.addCell(headingCell2)

                val subHeader = PdfPTable(2)
                val subHeaderCell1 = PdfPCell(Phrase(clientName, subHeadingFont))
                val subHeaderCell2 = PdfPCell(Phrase(monthYear, subHeadingFont))
                subHeaderCell1.border = Rectangle.NO_BORDER
                subHeaderCell1.setPadding(15f)
                subHeaderCell1.horizontalAlignment = Element.ALIGN_CENTER
                subHeaderCell2.border = Rectangle.NO_BORDER
                subHeaderCell2.setPadding(15f)
                subHeaderCell2.horizontalAlignment = Element.ALIGN_RIGHT

                subHeader.addCell(subHeaderCell1)
                subHeader.addCell(subHeaderCell2)

                pdfDoc.add(tableHeader)
                pdfDoc.add(subHeader)

                val my_table = PdfPTable(6)
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
                                    table_cell.setPadding(4f)
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

                val timeRange = timeRangeOfMonth(monthYear)

                val room = MyDatabase.getDatabase(mContext)

                val allRecordsOfClient =
                    room.recordDao().getRecordsByClientIdAndRecordType(clientId, recordType)

                val totalTable = PdfPTable(6)

                for (cl in 0..5) {
                    val txt = when (cl) {
                        0 -> "Total"
                        1 -> morningTotal(list)
                        2 -> eveningTotal(list)
                        3 -> rateTotal(list)
                        4 -> amountTotal(list)
                        5 -> paidTotal(list)
                        else -> "-"
                    }
                    val totalCell = PdfPCell(
                        Phrase(txt.toString(), totalFont)
                    )
                    totalCell.borderWidth = 0.5f
                    totalCell.setPadding(5f)
                    totalCell.horizontalAlignment = Element.ALIGN_LEFT
                    totalTable.addCell(totalCell)
                }

                val lastAdvanceTable = PdfPTable(1)

                val lAdv = getLastAdvance(
                    timeRange,
                    allRecordsOfClient
                )

                val lastAdvanceCell = PdfPCell(
                    Phrase(
                        "Last  ${if (lAdv < 0) "Advance" else "Balance"}: ${if (lAdv < 0) -lAdv else lAdv}", regular
                    )
                )

                lastAdvanceCell.borderWidth = 0.5f
                lastAdvanceCell.setPadding(5f)
                lastAdvanceCell.horizontalAlignment = Element.ALIGN_LEFT
                lastAdvanceTable.addCell(lastAdvanceCell)

                val adv = getAdvance(list) + lAdv

                val advanceTable = PdfPTable(1)
                val advanceCell = PdfPCell(
                    Phrase(
                        "${if (adv < 0) "Advance" else "Balance"}: ${if (adv < 0) -adv else adv}",
                        regular
                    )
                )

                advanceCell.borderWidth = 0.5f
                advanceCell.setPadding(5f)
                advanceCell.horizontalAlignment = Element.ALIGN_LEFT
                advanceTable.addCell(advanceCell)

                pdfDoc.add(totalTable)
                pdfDoc.add(lastAdvanceTable)
                pdfDoc.add(advanceTable)

            }

        }

        private fun morningTotal(list: ArrayList<MonthlySummaryModel>, i : Any? = null) = run {
            var m = 0f
            for (r in list)
                m += r.morningQnt
            String.format("%.2f", m)
        }

        private fun eveningTotal(list: ArrayList<MonthlySummaryModel>, i: Any? = null) = run {
            var e = 0f
            for (r in list)
                e += r.eveningQnt
            String.format("%.2f", e)
        }

        private fun amountTotal(list: ArrayList<MonthlySummaryModel>, i: Any? = null) = run {
            var a = 0f
            for (r in list)
                a += r.amount
            String.format("%.2f", a)
        }

        private fun paidTotal(list: ArrayList<MonthlySummaryModel>, i: Any? = null) = run {
            var p = 0f
            for (r in list)
                p += r.paid
            String.format("%.2f", p)
        }

        private fun lastBlcAdv(list: ArrayList<MonthlySummaryModel>, i: Any? = null) = run {
            var p = 0f
            for (r in list)
                p += r.lastBalance
            String.format("%.2f", p)
        }

        private fun balance(list: ArrayList<MonthlySummaryModel>) = run {
            var p = 0f
            for (r in list)
                p += r.balance
            String.format("%.2f", p)
        }

        private fun advance(list: ArrayList<MonthlySummaryModel>) = run {
            var p = 0f
            for (r in list)
                p += r.advance
            String.format("%.2f", p)
        }

        private fun morningTotal(list: ArrayList<ItemPersonData>) = run {
            var m = 0f
            for (r in list)
                m += r.mQnt
            String.format("%.2f", m)
        }

        private fun eveningTotal(list: ArrayList<ItemPersonData>) = run {
            var e = 0f
            for (r in list)
                e += r.eQnt
            String.format("%.2f", e)
        }

        private fun amountTotal(list: ArrayList<ItemPersonData>) = run {
            var a = 0f
            for (r in list)
                a += r.amount
            String.format("%.2f", a)
        }

        private fun paidTotal(list: ArrayList<ItemPersonData>) = run {
            var p = 0f
            for (r in list)
                p += r.paid
            String.format("%.2f", p)
        }

        private fun rateTotal(list: ArrayList<ItemPersonData>) = run {
            try {
                var rt = 0f
                var c = 0
                for (r in list) {
                    rt += r.rate.toFloat()
                    if (r.rate.toFloat() > 0f) c++
                }
                if (rt != 0f) String.format("%.2f", rt / c) else 0f
            } catch (e: Exception) {
                0f
            }
        }

        private fun getAdvance(record: ArrayList<ItemPersonData>) = run {

            var paid = 0f
            var amount = 0f

            for (r in record) {
                paid += r.paid
                amount += r.amount
            }

            amount - paid
        }

        private fun  getLastAdvance(timeRange: LongRange, record: List<RecordsEntity>) = run {

            var paid = 0f
            var amount = 0f

            for (r in record) {
                if (r.timestamp < timeRange.first) {
                    paid += r.amountPaid
                    amount += r.amount
                }
            }

            amount - paid
        }

        fun monthlyRecordByClient(
            clientId: String,
            room: MyDatabase,
            recordType: String,
            month: Int,
            year: Int
        ) = run {

            val list = ArrayList<ItemPersonData>()

            val monthByRecord = room.recordDao()
                .getRecordsByClientIdAndRecordType(clientId, recordType) as ArrayList<RecordsEntity>

            monthByRecord.removeIf {
                SimpleDateFormat(
                    "MMMM y",
                    Locale.ENGLISH
                ).format(Date(it.timestamp)) != "${getMonthInString(month)} $year"
            }

            val cal = Calendar.getInstance()

            val currentDay = cal.get(Calendar.DAY_OF_MONTH)

            for (d in getDays(month)) {

                if (!(month == cal.get(Calendar.MONTH) && year == cal.get(Calendar.YEAR) && d > currentDay)) {

                    var mQnt = 0f
                    var eQnt = 0f
                    var amount = 0f
                    var paid = 0f

                    for (r in monthByRecord) {
                        if (SimpleDateFormat(
                                "d",
                                Locale.ENGLISH
                            ).format(Date(r.timestamp)) == d.toString()
                        ) {
                            mQnt += if (r.shift == AppConstants.MORNING_SHIFT) r.quantity else 0f
                            eQnt += if (r.shift == AppConstants.EVENING_SHIFT) r.quantity else 0f
                            amount += r.amount
                            paid += r.amountPaid
                        }
                    }

                    val averageRate = if (mQnt + eQnt == 0f) 0.0 else amount / (mQnt + eQnt)

                    if (!(paid == 0f && amount == 0f && mQnt == 0f && eQnt == 0f)) {
                        list.add(
                            ItemPersonData(
                                d.toString(),
                                mQnt, eQnt, String.format("%.2f", averageRate), amount, paid
                            )
                        )
                    }

                }
            }
            list
        }

        private fun getDays(month: Int) = run {

            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, month)

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
            val start = cal.get(Calendar.DAY_OF_MONTH)

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val end = cal.get(Calendar.DAY_OF_MONTH)

            start..end
        }

        fun getMonthInString(month: Int): String = run {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, month)
            SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date(cal.time.time))
        }

        @Throws(Exception::class)
        fun getMonthInInt(month: String) = run {
            val date = SimpleDateFormat("MMMM", Locale.ENGLISH).parse(month)
            val c = Calendar.getInstance()
            date.let { c.time = it }
            c.get(Calendar.MONTH)
        }

        fun getDisplayHeaders(callBack: DisplayCallBack) {

            val db = FirebaseFirestore.getInstance()

            db.collection(COLLECTION_CONTROLS)
                .document(UTILS)
                .get().addOnSuccessListener {
                    try {
                        val phone = it[DISPLAY_PHONE].toString()
                        val name = it[DISPLAY_NAME].toString()
                        callBack.callBack(phone, name)
                    } catch (e: Exception) {
                    }
                }

        }

        interface DisplayCallBack {
            fun callBack(phone: String, name: String)
        }

        fun openFile(mContext: Context, filePath: String) {
            val file = File(filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    mContext,
                    "com.dr.dairyaccounting.fileprovider",
                    file
                )
                val mime =
                    if (filePath.endsWith(".pdf")) "application/pdf" else if (filePath.endsWith(".xls")) "application/xls" else "*/*"
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uri, mime)
                mContext.startActivity(intent)
            } else {
                Toast.makeText(mContext, "File not found", Toast.LENGTH_SHORT).show()
            }
        }

        fun shareFile(mContext: Context, filePath: String) {

            val outputPath = File(
                filePath
            )

            if (!outputPath.absolutePath.endsWith("pdf")) {
                Toast.makeText(mContext, "Can not share this file", Toast.LENGTH_SHORT).show()
                return
            }

            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    mContext,
                    "com.dr.dairyaccounting.fileprovider",
                    outputPath
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "File Selector",
                    "The selected file can't be shared"
                )
                null
            }

            if (fileUri != null) {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "application/pdf";
                intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                //intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                mContext.startActivity(intent)
            } else {
                Toast.makeText(mContext, "File not found", Toast.LENGTH_SHORT).show()
            }

        }

    }

}

