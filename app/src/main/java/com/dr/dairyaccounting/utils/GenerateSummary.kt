package com.dr.dairyaccounting.utils

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import kotlin.jvm.Throws

class GenerateSummary {

    var isPrint = false

    @Throws(Exception::class)
    fun generateSummary(
        mContext: Context, print: Boolean = false
    ): GenerateSummary {
        isPrint = print
        chooseYearMonth(mContext, object : CallBack {
            override fun callBackListener(
                month: Int,
                year: Int,
                recordType: String,
                isExcel: Boolean
            ) {
                if (isExcel) {
                    AppFunctions.createRecordExcel(
                        mContext,
                        recordType,
                        month,
                        year,
                        object : BillGenerateCallbacks {
                            override fun onSuccessListener(fileName: String) {
                                Toast.makeText(
                                    mContext,
                                    "Successfully Downloaded",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            override fun onFailureListener(error: String) {
                                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
                            }
                        })
                } else {

                    AppFunctions.createRecordPdf(
                        mContext, recordType, month, year, isPrint, object : BillGenerateCallbacks {
                            override fun onSuccessListener(fileName: String) {
                                Toast.makeText(
                                    mContext,
                                    "Successfully Downloaded",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onFailureListener(error: String) {
                                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
                            }

                        }
                    )
                }
            }
        })
        return this
    }

    private fun chooseYearMonth(mContext: Context, yearCallBack: CallBack) {
        val years = arrayOf(
            "2020",
            "2021",
            "2022",
            "2023",
            "2024",
            "2025",
            "2026",
            "2027"
        )
        AlertDialog.Builder(mContext)
            .setTitle("Choose Year")
            .setItems(years) { _, i ->
                chooseMonth(mContext, years[i].toInt(), yearCallBack)
            }.create().show()
    }

    private fun chooseMonth(mContext: Context, year: Int, callBack: CallBack) {

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
        AlertDialog.Builder(mContext)
            .setTitle("Choose Month")
            .setItems(months) { _, i ->
                chooseRecordType(mContext, year, AppFunctions.getMonthInInt(months[i]), callBack)
            }.create().show()

    }

    private fun chooseRecordType(mContext: Context, year: Int, month: Int, callBack: CallBack) {
        val recordTypes = arrayOf("Purchase", "Sale")
        AlertDialog.Builder(mContext)
            .setTitle("Choose Record Type")
            .setItems(recordTypes) { _, i ->
                if (isPrint) {
                    callBack.callBackListener(
                        month, year, if (i == 0) AppConstants.RECORD_TYPE_PURCHASE else AppConstants.RECORD_TYPE_SALE, false
                    )
                } else {
                    outPutType(
                        mContext,
                        year,
                        month,
                        if (i == 0) AppConstants.RECORD_TYPE_PURCHASE else AppConstants.RECORD_TYPE_SALE,
                        callBack
                    )
                }
            }.create().show()
    }

    private fun outPutType(
        mContext: Context,
        year: Int,
        month: Int,
        recordType: String,
        callBack: CallBack
    ) {
        val fileTypes = arrayOf(AppConstants.FILE_FORMAT_PDF, AppConstants.FILE_FORMAT_XSL)
        AlertDialog.Builder(mContext)
            .setTitle("Choose File Type")
            .setItems(fileTypes) { _, i ->
                callBack.callBackListener(
                    month, year, recordType, i == 1
                )
            }.create().show()
    }

    interface CallBack {
        fun callBackListener(month: Int, year: Int, recordType: String, isExcel: Boolean)
    }

}