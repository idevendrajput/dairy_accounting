package com.dr.dairyaccounting.utils

import android.content.Context
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dr.dairyaccounting.MyApp.Companion.appContext
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.database.TempRecordEntity
import com.dr.dairyaccounting.records.summary.MonthlySummaryModel
import com.dr.dairyaccounting.records.summary.SummaryItems
import com.dr.dairyaccounting.ui.Home
import com.dr.dairyaccounting.utils.AppConstants.AUTH_STATUS
import com.dr.dairyaccounting.utils.AppConstants.DATA_CHANGED
import com.dr.dairyaccounting.utils.AppConstants.DISPLAY_NAME
import com.dr.dairyaccounting.utils.AppConstants.DISPLAY_PHONE
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.INTENT_DATA_EVENING
import com.dr.dairyaccounting.utils.AppConstants.INTENT_DATA_MORNING
import com.dr.dairyaccounting.utils.AppConstants.IS_UPDATE_SERVICE_RUNNING
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.PASSWORD
import com.dr.dairyaccounting.utils.AppConstants.PHONE
import com.dr.dairyaccounting.utils.AppConstants.SELECTED_CALENDAR_TIME
import com.dr.dairyaccounting.utils.AppConstants.USER_ID
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Utils {

    companion object {

        fun getTodayDate(timestamp: Long = 0L): String = run {
            if (timestamp != 0L) {
                SimpleDateFormat("dd/MM/y", Locale.ENGLISH).format(Date(timestamp))
            } else {
                val context = appContext
                SimpleDateFormat("dd/MM/y", Locale.ENGLISH).format(Date(getCalendarTime(context!!)))
            }
        }

        fun isExternalStorageReadOnly(): Boolean {
            val externalStorageState: String = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED_READ_ONLY == externalStorageState
        }

        fun isExternalStorageAvailable(): Boolean {
            val externalStorageState = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == externalStorageState
        }

        fun getSaleByTimeRange(mContext: Context, timeRange: LongRange) = run {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllSalesClients() as ArrayList<ClientsEntity>

            val list = ArrayList<MonthlySummaryModel>()

            for (x in clients) {

                val clientRecord = room.recordDao()
                    .getSaleRecordsByClientId(clientId = x.id) as ArrayList<RecordsEntity>
                val lastBalance = getLastBalance(timeTill = timeRange.first, records = clientRecord)
                val advance =
                    getAdvance(timeTill = timeRange.last, records = clientRecord /*, lastBalance*/)

                clientRecord.removeIf { it.timestamp !in timeRange }

                if (clientRecord.isNotEmpty()) {

                    var amount = 0f
                    var quantity = 0f
                    var paid = 0f
                    var mQnt = 0f
                    var eQnt = 0f

                    for (r in clientRecord) {
                        amount += r.amount
                        quantity += r.quantity
                        paid += r.amountPaid
                        if (r.shift == MORNING_SHIFT) {
                            mQnt += r.quantity
                        }
                        if (r.shift == EVENING_SHIFT) {
                            eQnt += r.quantity
                        }
                    }

                    list.add(
                        MonthlySummaryModel(
                            x.clientName,
                            eveningQnt = eQnt,
                            morningQnt = mQnt,
                            amount = amount,
                            paid = paid,
                            lastBalance = lastBalance,
                            balance = getBalance(amount, paid, lastBalance),
                            advance = advance,
                            clientId = x.id
                        )
                    )

                } else {
                    list.add(
                        MonthlySummaryModel(
                            x.clientName,
                            eveningQnt = 0f,
                            morningQnt = 0f,
                            amount = 0f,
                            paid = 0f,
                            lastBalance = 0f,
                            balance = 0f,
                            advance = 0f,
                            clientId = x.id
                        )
                    )
                }
            }
            list
        }

        fun getPurchaseByTimeRange(mContext: Context, timeRange: LongRange) = run {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllPurchaseClients() as ArrayList<ClientsEntity>

            val list = ArrayList<MonthlySummaryModel>()

            for (x in clients) {

                val clientRecord = room.recordDao()
                    .getPurchaseRecordsByClientId(clientId = x.id) as ArrayList<RecordsEntity>

                val lastBalance = getLastBalance(timeTill = timeRange.first, records = clientRecord)
                val advance =
                    getAdvance(timeTill = timeRange.last, records = clientRecord /*, lastBalance*/)
                clientRecord.removeIf { it.timestamp !in timeRange }

                if (clientRecord.isNotEmpty()) {

                    var amount = 0f
                    var quantity = 0f
                    var paid = 0f
                    var mQnt = 0f
                    var eQnt = 0f

                    for (r in clientRecord) {
                        amount += r.amount
                        quantity += r.quantity
                        paid += r.amountPaid
                        if (r.shift == MORNING_SHIFT) {
                            mQnt += r.quantity
                        }
                        if (r.shift == EVENING_SHIFT) {
                            eQnt += r.quantity
                        }
                    }

                    list.add(
                        MonthlySummaryModel(
                            x.clientName,
                            eveningQnt = eQnt,
                            morningQnt = mQnt,
                            amount = amount,
                            paid = paid,
                            lastBalance = lastBalance,
                            balance = getBalance(amount, paid, lastBalance),
                            advance = advance,
                            clientId = x.id
                        )
                    )

                } else {
                    list.add(
                        MonthlySummaryModel(
                            x.clientName,
                            eveningQnt = 0f,
                            morningQnt = 0f,
                            amount = 0f,
                            paid = 0f,
                            lastBalance = 0f,
                            balance = 0f,
                            advance = 0f,
                            clientId = x.id
                        )
                    )
                }
            }
            list
        }

        private fun getBalance(amount: Float, paid: Float, lastBalance: Float) = run {
            if (amount - paid + (lastBalance) < 0) 0f else amount - paid + (lastBalance) /* last balance is by default negative*/
        }

        private fun getLastBalance(timeTill: Long, records: ArrayList<RecordsEntity>) = run {

            var amount = 0f
            var paid = 0f

            for (r in records) {
                if (r.timestamp < timeTill) {
                    amount += r.amount
                    paid += r.amountPaid
                }
            }

            amount - paid
        }

        private fun getAdvance (
            timeTill: Long,
            records: ArrayList<RecordsEntity> /*, lastBalance: Float */
        ) = run {

            var amount = 0f
            var paid = 0f

            for (r in records) {
                if (r.timestamp < timeTill) {
                    amount += r.amount
                    paid += r.amountPaid
                }
            }

            if (amount - paid < 0) (paid - amount) /* + lastBalance */ else 0f

        }

        fun getPurchaseByMonth(mContext: Context, monthYear: String) =
            getPurchaseByTimeRange(mContext, timeRangeOfMonth(monthYear))

        fun timeRangeOfMonth(monthYear: String) = run {

            val date = SimpleDateFormat("MMMM y", Locale.ENGLISH).parse(monthYear)

            val calStart: Calendar = GregorianCalendar()
            calStart.time = date
            calStart.set(Calendar.DAY_OF_MONTH, calStart.getActualMinimum(Calendar.DAY_OF_MONTH))
            calStart.set(Calendar.HOUR_OF_DAY, calStart.getActualMinimum(Calendar.HOUR_OF_DAY))
            calStart.set(Calendar.MINUTE, calStart.getActualMinimum(Calendar.MINUTE))
            calStart.set(Calendar.SECOND, calStart.getActualMinimum(Calendar.SECOND))
            calStart.set(Calendar.MILLISECOND, calStart.getActualMinimum(Calendar.MILLISECOND))
            val startOfMonth = calStart.time.time

            calStart.set(Calendar.DAY_OF_MONTH, calStart.getActualMaximum(Calendar.DAY_OF_MONTH))
            calStart.set(Calendar.HOUR_OF_DAY, calStart.getActualMaximum(Calendar.HOUR_OF_DAY))
            calStart.set(Calendar.MINUTE, calStart.getActualMaximum(Calendar.MINUTE))
            calStart.set(Calendar.SECOND, calStart.getActualMaximum(Calendar.SECOND))
            calStart.set(Calendar.MILLISECOND, calStart.getActualMaximum(Calendar.MILLISECOND))
            val endOfMonth = calStart.time.time

            startOfMonth..endOfMonth
        }

        fun getSaleByMonth(mContext: Context, monthYear: String) =
            getSaleByTimeRange(mContext, timeRangeOfMonth(monthYear))

        fun getTodayMorningPurchase(mContext: Context): ArrayList<RecordsEntity> {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllPurchaseClients() as ArrayList<ClientsEntity>

            val list = ArrayList<RecordsEntity>()

            for (x in clients) {

                if (x.morningShift) {

                    val todayRecord =
                        room.recordDao().getTodayPurchaseRecordsMorningByClientId(clientId = x.id)

                    if (todayRecord.isNotEmpty()) {
                        list.add(todayRecord[0])
                    } else {
                        list.add(
                            RecordsEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = x.id,
                                clientName = x.clientName,
                                quantity = 0f,
                                shift = MORNING_SHIFT,
                                rate = x.rate,
                                amount = 0f,
                                amountPaid = 0f,
                                amountAdvance = 0f,
                                recordType = x.accountType,
                                timestamp = getCalendarTime(mContext),
                                orderTimeStamp = x.orderTimeStamp,
                                date = getTodayDate()
                            )
                        )
                    }
                }
            }
            return list
        }

        fun getTodayRecords(mContext: Context, shift: String, recordType: String): ArrayList<RecordsEntity> {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllActiveClients(recordType) as ArrayList<ClientsEntity>

            val list = ArrayList<RecordsEntity>()

            for (x in clients) {
                if (if (shift == MORNING_SHIFT) x.morningShift else x.eveningShift) {
                    val todayRecord = room.recordDao().getTodayRecordsByClientId(clientId = x.id, recordType = recordType, shift = shift)
                    if (todayRecord.isNotEmpty()) {
                        list.add(todayRecord[0])
                    } else {
                        list.add(
                            RecordsEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = x.id,
                                clientName = x.clientName,
                                quantity = 0f,
                                shift = shift,
                                rate = x.rate,
                                amount = 0f,
                                amountPaid = 0f,
                                amountAdvance = 0f,
                                recordType = recordType,
                                timestamp = getCalendarTime(mContext),
                                orderTimeStamp = x.orderTimeStamp,
                                date = getTodayDate()
                            )
                        )
                    }
                }
            }

            return list

        }

        fun getTodayEveningPurchase(mContext: Context): ArrayList<RecordsEntity> {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllPurchaseClients() as ArrayList<ClientsEntity>

            val list = ArrayList<RecordsEntity>()

            for (x in clients) {
                if (x.eveningShift) {
                    val todayRecord = room.recordDao().getTodayPurchaseRecordsEveningByClientId(clientId = x.id)
                    if (todayRecord.isNotEmpty()) {
                        list.add(todayRecord[0])
                    } else {
                        list.add(
                            RecordsEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = x.id,
                                clientName = x.clientName,
                                quantity = 0f,
                                shift = EVENING_SHIFT,
                                rate = x.rate,
                                amount = 0f,
                                amountPaid = 0f,
                                amountAdvance = 0f,
                                recordType = x.accountType,
                                timestamp = getCalendarTime(mContext),
                                orderTimeStamp = x.orderTimeStamp,
                                date = getTodayDate()
                            )
                        )
                    }
                }
            }
            return list
        }

        fun getTodayEveningSale(mContext: Context): ArrayList<RecordsEntity> {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllSalesClients() as ArrayList<ClientsEntity>

            val list = ArrayList<RecordsEntity>()

            for (x in clients) {
                if (x.eveningShift) {
                    val todayRecord =
                        room.recordDao().getTodaySaleRecordsEveningByClientId(clientId = x.id)
                    if (todayRecord.isNotEmpty()) {
                        list.add(todayRecord[0])
                    } else {
                        list.add(
                            RecordsEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = x.id,
                                clientName = x.clientName,
                                shift = EVENING_SHIFT,
                                quantity = 0f,
                                rate = x.rate,
                                amount = 0f,
                                amountPaid = 0f,
                                amountAdvance = 0f,
                                recordType = x.accountType,
                                timestamp = getCalendarTime(mContext),
                                orderTimeStamp = x.orderTimeStamp,
                                date = getTodayDate()
                            )
                        )
                    }
                }
            }

            return list
        }

        fun getTodayMorningSale(mContext: Context): ArrayList<RecordsEntity> {

            val room = MyDatabase.getDatabase(mContext)

            val clients = room.clientDao().getAllSalesClients() as ArrayList<ClientsEntity>

            val list = ArrayList<RecordsEntity>()

            for (x in clients) {
                if (x.morningShift) {
                    val todayRecord =
                        room.recordDao().getTodaySaleRecordsMorningByClientId(clientId = x.id)
                    if (todayRecord.isNotEmpty()) {
                        list.add(todayRecord[0])
                    } else {
                        list.add(
                            RecordsEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = x.id,
                                clientName = x.clientName,
                                shift = MORNING_SHIFT,
                                quantity = 0f,
                                rate = x.rate,
                                amount = 0f,
                                amountPaid = 0f,
                                amountAdvance = 0f,
                                recordType = x.accountType,
                                timestamp = getCalendarTime(mContext),
                                orderTimeStamp = x.orderTimeStamp,
                                date = getTodayDate()
                            )
                        )
                    }
                }
            }
            return list
        }

        fun todayTimeRange(mContext: Context): LongRange {

            val calStart: Calendar = GregorianCalendar()
            calStart.time = Date(getCalendarTime(mContext))
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            val midnightYesterday = calStart.time.time

            val calEnd: Calendar = GregorianCalendar()
            calEnd.time = Date(getCalendarTime(mContext))
            calEnd.set(Calendar.DAY_OF_YEAR, calEnd.get(Calendar.DAY_OF_YEAR) + 1)
            calEnd.set(Calendar.HOUR_OF_DAY, 0)
            calEnd.set(Calendar.MINUTE, 0)
            calEnd.set(Calendar.SECOND, 0)
            calEnd.set(Calendar.MILLISECOND, 0)
            val midnightTonight = calEnd.time.time - 1

            return midnightYesterday..midnightTonight
        }

        fun getYearlyData(mContext: Context, year: String, recordType: String) = run {

            val room = MyDatabase.getDatabase(mContext)

            val list = ArrayList<SummaryItems>()

            val months = arrayOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December"
            )

            for (m in months) {

                val records =
                    room.recordDao().getRecordsByRecordType(recordType) as ArrayList<RecordsEntity>

                records.removeIf {
                    SimpleDateFormat(
                        "MMMM y",
                        Locale.ENGLISH
                    ).format(Date(it.timestamp)) != "$m $year"
                }

                if (records.isNotEmpty()) {

                    var amount = 0f
                    var quantity = 0f
                    var paid = 0f
                    var mQnt = 0f
                    var eQnt = 0f

                    for (r in records) {
                        amount += r.amount
                        quantity += r.quantity
                        paid += r.amountPaid
                        if (r.shift == MORNING_SHIFT) {
                            mQnt += r.quantity
                        }
                        if (r.shift == EVENING_SHIFT) {
                            eQnt += r.quantity
                        }
                    }

                    records[0].clientName = m
                    records[0].amount = amount
                    records[0].quantity = quantity
                    records[0].amountPaid = paid

                    list.add(
                        SummaryItems(
                            morningQuantity = mQnt,
                            eveningQuantity = eQnt,
                            recordsEntity = records[0]
                        )
                    )

                } else {
                    list.add(
                        SummaryItems(
                            morningQuantity = 0f,
                            eveningQuantity = 0f,
                            RecordsEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = "",
                                clientName = m,
                                quantity = 0f,
                                shift = MORNING_SHIFT,
                                rate = 0f,
                                amount = 0f,
                                amountPaid = 0f,
                                amountAdvance = 0f,
                                recordType = recordType,
                                timestamp = System.currentTimeMillis(),
                                orderTimeStamp = System.currentTimeMillis(),
                                date = getTodayDate()
                            )
                        )
                    )
                }

            }

            list
        }

        fun createExcel(mContext: Context, list: ArrayList<RecordsEntity>, fileName: String) {

            if (!AppFunctions.checkPermission(mContext)) {
                Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                Home().requestPermission(mContext)
                return
            }
            excelOutPut(mContext, list, fileName)

        }

        private fun excelOutPut(
            mContext: Context,
            list: ArrayList<RecordsEntity>,
            fileName: String
        ) {

            if (list.isEmpty()) {
                Toast.makeText(mContext, "No Record Found", Toast.LENGTH_SHORT).show()
                return
            }

            if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                Toast.makeText(mContext, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT)
                    .show()
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

            for (i in 0..5) {
                val cell = headingRow.createCell(i)
                cell.setCellValue(
                    when (i) {
                        0 -> "Date"
                        1 -> "Client Name"
                        2 -> "Quantity"
                        3 -> "Rate"
                        4 -> "Amount"
                        5 -> "Paid"
                        else -> ""
                    }
                )
                cell.setCellStyle(cellStyle)
            }

            for ((i, r) in list.withIndex()) {

                /* row starts with +1 because row number 1 is heading and instruction row */

                val row = sheet.createRow(i + 1)
                row.createCell(0)
                    .setCellValue(
                        SimpleDateFormat(
                            "dd/MMM/y",
                            Locale.ENGLISH
                        ).format(Date(r.timestamp))
                    )
                row.createCell(1)
                    .setCellValue(r.clientName)
                row.createCell(2)
                    .setCellValue(r.quantity.toString())
                row.createCell(3)
                    .setCellValue(r.rate.toString())
                row.createCell(4)
                    .setCellValue(r.amount.toString())
                row.createCell(5)
                    .setCellValue(r.amountPaid.toString())

            }

            try {

                val dir = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + "/DoodhWala/DayWiseSummary/Excel/${
                        getTodayDate().replace(
                            "/",
                            " "
                        )
                    }"
                )

                val filePath = File(dir, "/$fileName.xls")

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

                Toast.makeText(mContext, "File Saved Successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show()
            }

        }

        fun generatePDF(
            list: ArrayList<RecordsEntity>,
            fileName: String,
            activity: AppCompatActivity,
            isPrint: Boolean = false
        ) {

            if (!AppFunctions.checkPermission(activity)) {
                Toast.makeText(
                    activity.applicationContext,
                    "Permission Denied!",
                    Toast.LENGTH_SHORT
                ).show()
                Home().requestPermission(activity)
                return
            }

            pdfOutPut(list, fileName, activity, isPrint)

        }

        fun pdfOutPut(
            list: ArrayList<RecordsEntity>,
            fileName: String,
            activity: AppCompatActivity,
            isPrint: Boolean
        ) {

            if (list.isEmpty()) {
                Toast.makeText(activity, "No Record Found", Toast.LENGTH_SHORT).show()
                return
            }

            if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                Toast.makeText(
                    activity.applicationContext,
                    AppConstants.SOMETHING_WENT_WRONG,
                    Toast.LENGTH_SHORT
                ).show()
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

            for (i in 0..5) {
                val cell = headingRow.createCell(i)
                cell.setCellValue(
                    when (i) {
                        0 -> "Date"
                        1 -> "Client Name"
                        2 -> "Quantity"
                        3 -> "Rate"
                        4 -> "Amount"
                        5 -> "Paid"
                        else -> ""
                    }
                )
                cell.setCellStyle(cellStyle)
            }

            for ((i, r) in list.withIndex()) {

                /* row starts with +1 because row number 1 is heading and instruction row */

                val row = sheet.createRow(i + 1)
                row.createCell(0)
                    .setCellValue(
                        SimpleDateFormat(
                            "dd/MMM/yyyy",
                            Locale.ENGLISH
                        ).format(Date(r.timestamp))
                    )
                row.createCell(1)
                    .setCellValue(r.clientName)
                row.createCell(2)
                    .setCellValue(r.quantity.toString())
                row.createCell(3)
                    .setCellValue(r.rate.toString())
                row.createCell(4)
                    .setCellValue(r.amount.toString())
                row.createCell(5)
                    .setCellValue(r.amountPaid.toString())

            }

            val my_worksheet = workbook.getSheetAt(0)

            val rowIterator: Iterator<Row> = my_worksheet.iterator()

            val dir = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/DoodhWala/DayWiseSummary/PDF/${
                    getTodayDate().replace(
                        "/",
                        " "
                    )
                }"
            )
            val filePath = File(dir, "/$fileName.pdf")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            if (!filePath.exists()) {
                filePath.createNewFile()
            }

            val iText_xls_2_pdf = Document(PageSize.A4)
            PdfWriter.getInstance(iText_xls_2_pdf, FileOutputStream(filePath))
            iText_xls_2_pdf.open()

            val baseFont = Font.FontFamily.HELVETICA
            val regularFontFamily = Font.FontFamily.HELVETICA

            val regular = Font(regularFontFamily, 9f, Font.NORMAL, BaseColor.BLACK)
            val headerFont = Font(baseFont, 18f, Font.BOLD, BaseColor.BLACK)
            val subHeadingFont = Font(baseFont, 14f, Font.BOLD, BaseColor.BLACK)

            val tableHeader = PdfPTable(2)
            val headingCell = PdfPCell(Phrase(getDisplayName(activity), headerFont))
            val headingCell2 = PdfPCell(Phrase(getDisplayPhone(activity), headerFont))
            headingCell.border = Rectangle.NO_BORDER
            headingCell.horizontalAlignment = Element.ALIGN_LEFT
            headingCell2.border = Rectangle.NO_BORDER
            headingCell2.horizontalAlignment = Element.ALIGN_RIGHT
            tableHeader.addCell(headingCell)
            tableHeader.addCell(headingCell2)

            val subHeader = PdfPTable(1)
            val subHeaderCell1 = PdfPCell(Phrase(fileName, subHeadingFont))
            subHeaderCell1.border = Rectangle.NO_BORDER
            subHeaderCell1.setPadding(15f)
            subHeaderCell1.horizontalAlignment = Element.ALIGN_CENTER

            subHeader.addCell(subHeaderCell1)

            iText_xls_2_pdf.add(tableHeader)
            iText_xls_2_pdf.add(subHeader)

            val my_table = PdfPTable(6)
            var table_cell: PdfPCell?
            var i = 0
            my_table.horizontalAlignment = Element.ALIGN_CENTER

            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                val cellIterator: Iterator<Cell> = row.cellIterator()
                while (cellIterator.hasNext()) {
                    val cell: Cell = cellIterator.next()
                    when (cell.cellType) {
                        Cell.CELL_TYPE_STRING -> {
                            table_cell = PdfPCell(Phrase(cell.stringCellValue, regular))
                            table_cell.borderWidth = 0.5f
                            table_cell.setPadding(4f)
                            if (i == 0) {
                                table_cell.backgroundColor = BaseColor.YELLOW
                                table_cell.horizontalAlignment = Element.ALIGN_CENTER
                                table_cell.setPadding(10f)
                                table_cell.verticalAlignment = Element.ALIGN_CENTER
                            }
                            my_table.addCell(table_cell)
                        }
                    }
                }
                i++
            }

            iText_xls_2_pdf.add(my_table)
            iText_xls_2_pdf.close()

            if (isPrint) {
                print(activity, filePath.path)
            } else {
                Toast.makeText(activity, "File Saved Successfully", Toast.LENGTH_SHORT).show()
            }
        }

        fun print(activity: AppCompatActivity, filePath: String) {

            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager

            try {
                val printAdapter = PdfDocumentAdapter(activity, filePath)
                printManager.print("Document", printAdapter, PrintAttributes.Builder().build())
            } catch (e: Exception) {

            }
        }

        fun storeExcelInStorage(workbook: HSSFWorkbook, fileName: String) = run {
            try {
                val dir = File(Environment.getExternalStorageDirectory().toString() + "/DoodhWala")

                val filePath = File(dir, "/$fileName.xls")

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

        fun getCalendarTime(mContext: Context) = run {
            if (SharedPref.getLong(
                    mContext,
                    SELECTED_CALENDAR_TIME
                ) == 0L
            ) System.currentTimeMillis() else SharedPref.getLong(mContext, SELECTED_CALENDAR_TIME)
        }

        fun setCalendarTime(mContext: Context, timeInMillis: Long = System.currentTimeMillis()) =
            run {
                SharedPref.setLong(mContext, SELECTED_CALENDAR_TIME, timeInMillis)
            }

        fun setPassword(mContext: Context, password: String) {
            SharedPref.setData(mContext, PASSWORD, password)
        }

        //fun setDataChanged(mContext: Context) = SharedPref.setBoolean(mContext, DATA_CHANGED, true)

        //fun clearDataChanged(mContext: Context) = SharedPref.setBoolean(mContext, DATA_CHANGED, false)

        //fun isDataChanged(mContext: Context) = SharedPref.getBoolean(mContext, DATA_CHANGED)

        fun userId(mContext: Context) = run {
            val uid = SharedPref.getData(mContext, USER_ID)
            uid
        }

        fun getPhone(mContext: Context) = run {
            val uid = SharedPref.getData(mContext, PHONE)
            uid
        }

        fun getDisplayPhone(mContext: Context) = run {
            val uid = SharedPref.getData(mContext, DISPLAY_PHONE)
            uid
        }

        fun getDisplayName(mContext: Context) = run {
            val uid = SharedPref.getData(mContext, DISPLAY_NAME)
            uid
        }

        fun getPassword(mContext: Context) = run {
            SharedPref.getData(mContext, PASSWORD)
        }

        fun getAuthStatus(mContext: Context): String {
            return SharedPref.getData(mContext, AUTH_STATUS).toString()
        }

        fun setAuthStatus(mContext: Context, value: String) {
            SharedPref.setData(mContext, AUTH_STATUS, value)
        }

        fun setPhone(mContext: Context, value: String) {
            SharedPref.setData(mContext, PHONE, value)
        }

        fun setDisplayPhone(mContext: Context, value: String) {
            SharedPref.setData(mContext, DISPLAY_PHONE, value)
        }

        fun setDisplayName(mContext: Context, value: String) {
            SharedPref.setData(mContext, DISPLAY_NAME, value)
        }

        fun setUserId(mContext: Context, value: String) {
            SharedPref.setData(mContext, USER_ID, value)
        }

        fun isUpdateServiceRunning(mContext: Context) = SharedPref.getBoolean(mContext, IS_UPDATE_SERVICE_RUNNING)

        fun setUpdateServiceRunning(mContext: Context, isRunning: Boolean = true) = SharedPref.setBoolean(mContext, IS_UPDATE_SERVICE_RUNNING, isRunning)

        fun clearUpdateServiceRunning(mContext: Context) = SharedPref.setBoolean(mContext, IS_UPDATE_SERVICE_RUNNING, false)

        fun intentDataMorning(mContext: Context, data: String = "") = run {
            if (data.isEmpty())
                SharedPref.getData(mContext, INTENT_DATA_MORNING) ?: ""
            else {
                SharedPref.setData(mContext, INTENT_DATA_MORNING, data)
                data
            }
        }

        fun intentDataEvening(mContext: Context, data: String = "") = run {
            if (data.isEmpty())
                SharedPref.getData(mContext, INTENT_DATA_EVENING) ?: ""
            else {
                SharedPref.setData(mContext, INTENT_DATA_EVENING, data)
                data
            }
        }

        fun recordToTempRecord(list: ArrayList<RecordsEntity>) = ArrayList<TempRecordEntity>().apply { list.forEach {
                this.add(
                    TempRecordEntity(
                        it.id,
                        it.clientId,
                        it.clientName,
                        it.rate,
                        it.amount,
                        it.amountPaid,
                        it.amountAdvance,
                        it.recordType,
                        it.shift,
                        it.quantity,
                        it.timestamp,
                        it.orderTimeStamp,
                        it.date
                    )
                )
            } }

        fun recordItemToTempItem(item: RecordsEntity) = TempRecordEntity(
            item.id,
            item.clientId,
            item.clientName,
            item.rate,
            item.amount,
            item.amountPaid,
            item.amountAdvance,
            item.recordType,
            item.shift,
            item.quantity,
            item.timestamp,
            item.orderTimeStamp,
            item.date
        )

        fun tempToRecord(list: ArrayList<TempRecordEntity>) = ArrayList<RecordsEntity>().apply { list.forEach {
                this.add(
                    RecordsEntity(
                        it.id,
                        it.clientId,
                        it.clientName,
                        it.rate,
                        it.amount,
                        it.amountPaid,
                        it.amountAdvance,
                        it.recordType,
                        it.shift,
                        it.quantity,
                        it.timestamp,
                        it.orderTimeStamp,
                        it.date
                    )
                )
            } }

        fun tempToRecordItem(item: TempRecordEntity) = RecordsEntity(
            item.id,
            item.clientId,
            item.clientName,
            item.rate,
            item.amount,
            item.amountPaid,
            item.amountAdvance,
            item.recordType,
            item.shift,
            item.quantity,
            item.timestamp,
            item.orderTimeStamp,
            item.date
        )




    }

}

