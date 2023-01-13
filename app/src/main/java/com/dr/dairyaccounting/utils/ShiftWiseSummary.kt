package com.dr.dairyaccounting.utils

import android.content.Context
import android.widget.Toast
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.records.summary.ItemPersonData
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppFunctions.Companion.getMonthInString
import com.dr.dairyaccounting.utils.Utils.Companion.getSaleByMonth
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.util.CellRangeAddress
import java.util.ArrayList

class ShiftWiseSummary {

//    fun generateExcel(
//        mContext: Context,
//        clientList: ArrayList<ClientsEntity>,
//        recordType: String,
//        shift: Int,
//        callbacks: BillGenerateCallbacks
//    ) {
//
//        if (!AppFunctions.checkPermission(mContext)) {
//            Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        try {
//
//            val room = MyDatabase.getDatabase(mContext)
//
//            for ((i, client) in clientList.withIndex()) {
//
//                val fileName = client.clientName
//
//                val wb =  generateBillWorkBook(
//                    mContext,
//                    AppFunctions.monthlyRecordByClient(client.id, room, recordType, month, year),
//                    fileName
//                )
//
//                wb?.let {
//                    if (!AppFunctions.storeExcelInStorage(
//                            it,
//                            "Bills/${AppFunctions.makeFirstLaterCapital(recordType)}/Excel/${
//                                getMonthInString(month)
//                            } $year",
//                            fileName
//                        )
//                    ) {
//                        Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                }
//            }
//
//            callbacks.onSuccessListener(getMonthInString(month))
//
//        } catch (e: Exception) {
//            callbacks.onFailureListener("Something went wrong")
//        }
//
//    }
//
//    private fun generateBillWorkBook(
//        mContext: Context,
//        list: ArrayList<RecordsEntity>,
//        fileName: String
//    ): HSSFWorkbook? {
//
//        if (!AppFunctions.checkPermission(mContext)) {
//            Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
//            return null
//        } else if (list.isEmpty()) {
//            Toast.makeText(mContext, "No Record Found", Toast.LENGTH_SHORT).show()
//            return null
//        } else if (!Utils.isExternalStorageAvailable() || Utils.isExternalStorageReadOnly()) {
//            Toast.makeText(mContext, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT)
//                .show()
//            return null
//        } else {
//            // Workbook
//            val workbook = HSSFWorkbook()
//
//            // Sheet
//            val sheet = workbook.createSheet(fileName)
//            sheet.setColumnWidth(0, (15 * 300))
//            sheet.setColumnWidth(1, (15 * 150))
//            sheet.setColumnWidth(2, (15 * 150))
//            sheet.setColumnWidth(3, (15 * 150))
//            sheet.setColumnWidth(4, (15 * 150))
//            sheet.setColumnWidth(5, (15 * 150))
//            sheet.setColumnWidth(6, (15 * 150))
//
//            //  Create row
//            val main = sheet.createRow(0)
//            val headingRow = sheet.createRow(1)
//
//            // cell style
//            val headingCellStyle = workbook.createCellStyle()
//            headingCellStyle.fillForegroundColor = HSSFColor.YELLOW.index
//            headingCellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
//            headingCellStyle.alignment = CellStyle.ALIGN_LEFT
//
//            val basicCellStyle = workbook.createCellStyle()
//            basicCellStyle.alignment = CellStyle.ALIGN_LEFT
//
//            val titleCellStyle = workbook.createCellStyle()
//            titleCellStyle.alignment = CellStyle.ALIGN_CENTER
//
//            val basicFont = basicCellStyle.getFont(workbook)
//            basicFont.fontHeightInPoints = 18.toShort()
//            basicCellStyle.setFont(basicFont)
//
//            for (i in 0..5) {
//                val cell = main.createCell(i)
//                cell.setCellValue(
//                    when (i) {
//                        0 -> fileName
//                        else -> ""
//                    }
//                )
//                cell.setCellStyle(titleCellStyle)
//            }
//
//            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))
//
//            for (i in 0..5) {
//                val cell = headingRow.createCell(i)
//                cell.setCellValue(
//                    when (i) {
//                        0 -> "Date"
//                        1 -> "M.Qty"
//                        2 -> "E.Qty"
//                        3 -> "Rate"
//                        4 -> "Amount"
//                        5 -> "Paid"
//                        else -> ""
//                    }
//                )
//                cell.setCellStyle(headingCellStyle)
//            }
//
//            for ((i, r) in list.withIndex()) {
//
//                /* row starts with +1 because row number 1 is heading and instruction row */
//
//                val row = sheet.createRow(i + 2)
//
//                val cell1 = row.createCell(0)
//                cell1.setCellStyle(basicCellStyle)
//                cell1.setCellValue(r.date)
//
//                val cell2 = row.createCell(1)
//                cell2.setCellStyle(basicCellStyle)
//                cell2.setCellValue(String.format("%.2f", r.mQnt))
//
//                val cell3 = row.createCell(2)
//                cell3.setCellStyle(basicCellStyle)
//                cell3.setCellValue(String.format("%.2f", r.eQnt))
//
//                val cell4 = row.createCell(3)
//                cell4.setCellValue(r.rate)
//                cell4.setCellStyle(basicCellStyle)
//
//                val cell5 = row.createCell(4)
//                cell5.setCellValue(String.format("%.2f", r.amount))
//                cell5.setCellStyle(basicCellStyle)
//
//                val cell6 = row.createCell(5)
//                cell6.setCellValue(String.format("%.2f", r.paid))
//                cell6.setCellStyle(basicCellStyle)
//
//            }
//
//            return workbook
//        }
//    }

}