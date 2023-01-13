package com.dr.dairyaccounting.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.databinding.DialogAllClientsBinding
import com.dr.dairyaccounting.databinding.RowItemClientsDialogBinding
import com.dr.dairyaccounting.ui.Home.Companion.homePageBinding
import com.dr.dairyaccounting.utils.AppConstants.FILE_FORMAT_PDF
import com.dr.dairyaccounting.utils.AppConstants.FILE_FORMAT_XSL
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_SALE
import com.dr.dairyaccounting.utils.AppFunctions.Companion.generateBillExcel
import com.dr.dairyaccounting.utils.AppFunctions.Companion.generateBillPdf
import com.dr.dairyaccounting.utils.AppFunctions.Companion.getMonthInInt
import com.dr.dairyaccounting.utils.AppFunctions.Companion.openFile
import com.google.android.material.snackbar.Snackbar
import kotlin.collections.ArrayList
import kotlin.jvm.Throws

class Bills {

    var isPrint: Boolean = false
    var isManualClient = false
    private lateinit var room: MyDatabase
    private val resultList = ArrayList<ClientsEntity>()

    @Throws(Exception::class)
    fun generateBill(
        mContext: Context, print: Boolean = false
    ): Bills {
        isPrint = print
        room = MyDatabase.getDatabase(mContext)
        chooseYearMonth(mContext, object : CallBack {
            override fun callBackListener(
                month: Int,
                year: Int,
                recordType: String,
                isSingleClient: Boolean,
                isExcel: Boolean
            ) {

                val client = ArrayList<ClientsEntity>()

                if (isManualClient) {

                    if (isExcel) {
                        generateExcel(mContext, resultList, recordType, month, year)
                    } else {
                        generatePdf(
                            mContext, resultList, recordType, month, year
                        )
                    }

                } else {


                    if (isSingleClient) {

                        chooseClient(recordType, room, mContext, object : ClientCallBack {
                            override fun callBackListener(clientId: String) {

                                client.add(
                                    (room.clientDao()
                                        .getClientById(clientId) as ArrayList<ClientsEntity>)[0]
                                )

                                if (isExcel) {
                                    generateExcel(mContext, client, recordType, month, year)
                                } else {
                                    generatePdf(
                                        mContext, client, recordType, month, year
                                    )
                                }

                            }
                        })

                    } else {

                        client.addAll(
                            room.clientDao()
                                .getAllClientsByClientType(recordType) as ArrayList<ClientsEntity>
                        )

                        if (isExcel) {
                            generateExcel(mContext, client, recordType, month, year)
                        } else {
                            generatePdf(
                                mContext, client, recordType, month, year
                            )
                        }
                    }

                }

            }
        })

        return this
    }

    private fun generatePdf(
        mContext: Context,
        client: ArrayList<ClientsEntity>,
        recordType: String, month: Int, year: Int
    ) = generateBillPdf(
        mContext,
        client,
        recordType,
        month,
        year,
        isPrint,
        object : BillGenerateCallbacks {
            override fun onSuccessListener(fileName: String) {
                try {
                    Snackbar.make(homePageBinding, "Download Successful", Snackbar.LENGTH_SHORT)
                        .setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF018786")))
                        .setActionTextColor(Color.WHITE)
                        .setTextColor(Color.parseColor("#ECECEC"))
                        .setAction(if (fileName.endsWith(".pdf")) "Open" else "") {
                            if (fileName.endsWith(".pdf")) {
                                openFile(mContext, fileName)
                            }
                        }.show()
                } catch (e: Exception) {
                    Toast.makeText(mContext, "Successfully Downloaded", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailureListener(error: String) {
                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
            }

        }
    )

    private fun generateExcel(
        mContext: Context,
        client: ArrayList<ClientsEntity>,
        recordType: String, month: Int, year: Int
    ) = generateBillExcel(
        mContext,
        client,
        recordType,
        month,
        year,
        object : BillGenerateCallbacks {
            override fun onSuccessListener(fileName: String) {
                Toast.makeText(mContext, "Successfully Downloaded", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onFailureListener(error: String) {
                Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
            }

        }
    )

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
                chooseRecordType(mContext, year, getMonthInInt(months[i]), callBack)
            }.create().show()

    }

    private fun chooseRecordType(mContext: Context, year: Int, month: Int, callBack: CallBack) {
        val recordTypes = arrayOf("Purchase", "Sale")
        AlertDialog.Builder(mContext)
            .setTitle("Choose Record Type")
            .setItems(recordTypes) { _, i ->
                chooseBillFor(
                    mContext,
                    year,
                    month,
                    if (i == 0) RECORD_TYPE_PURCHASE else RECORD_TYPE_SALE,
                    callBack
                )
            }.create().show()
    }

    private fun chooseBillFor(
        mContext: Context,
        year: Int,
        month: Int,
        recordType: String,
        callBack: CallBack
    ) {
        val reportFor = arrayOf("All Clients", "Single Client", "Manually Select Clients")
        AlertDialog.Builder(mContext)
            .setTitle("Generate Bill For")
            .setItems(reportFor) { _, i ->
                if (i == 2) {
                    manuallyChooseClient(mContext, year, month, recordType, callBack)
                } else {
                    isManualClient = false
                    if (isPrint) {
                        callBack.callBackListener(
                            month, year, recordType, i == 1, false
                        )
                    } else {
                        outPutType(mContext, year, month, recordType, i == 1, callBack)
                    }
                }
            }.create().show()
    }

    private fun chooseClient(
        recordType: String,
        room: MyDatabase,
        mContext: Context,
        callBack: ClientCallBack
    ) {

        val clients =
            room.clientDao().getAllClientsByClientType(recordType) as ArrayList<ClientsEntity>

        val ar = ArrayList<String>()
        for (c in clients) {
            ar.add(c.clientName)
        }

        AlertDialog.Builder(mContext)
            .setTitle("Choose Client")
            .setItems(ar.toTypedArray()) { _, i ->
                callBack.callBackListener(clients[i].id)
            }.create().show()

    }

    private fun manuallyChooseClient(
        mContext: Context,
        year: Int,
        month: Int,
        recordType: String,
        callBack: CallBack
    ) {

        val d = Dialog(mContext)
        val dBinding = DialogAllClientsBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        d.show()

        resultList.clear()

        val clients =
            room.clientDao().getAllClientsByClientType(recordType) as ArrayList<ClientsEntity>

        var adapter = AdapterChooseClients(clients, resultList)

        dBinding.rv.adapter = adapter

        dBinding.all.setOnCheckedChangeListener { _, b ->
            if (b) {
                resultList.addAll(clients)
            } else {
                resultList.clear()
            }
            adapter = AdapterChooseClients(clients, resultList, b)
            dBinding.rv.adapter = adapter
        }

        dBinding.done.setOnClickListener {
            if (resultList.isEmpty()) {
                dBinding.noSelectedError.visibility = View.VISIBLE
            } else {
                d.dismiss()
                isManualClient = true
                if (isPrint) {
                    callBack.callBackListener(
                        month, year, recordType, isSingleClient = false, isExcel = false
                    )
                } else {
                    outPutType(mContext, year, month, recordType, false, callBack)
                }
            }
        }

    }

    private fun outPutType(
        mContext: Context,
        year: Int,
        month: Int,
        recordType: String,
        isSingleClient: Boolean,
        callBack: CallBack
    ) {
        val fileTypes = arrayOf(FILE_FORMAT_PDF, FILE_FORMAT_XSL)
        AlertDialog.Builder(mContext)
            .setTitle("Choose File Type")
            .setItems(fileTypes) { _, i ->
                callBack.callBackListener(
                    month, year, recordType, isSingleClient, i == 1
                )
            }.create().show()
    }

    interface ClientCallBack {
        fun callBackListener(clientId: String)
    }

    interface CallBack {
        fun callBackListener(
            month: Int,
            year: Int,
            recordType: String,
            isSingleClient: Boolean,
            isExcel: Boolean
        )
    }

    inner class AdapterChooseClients(
        val list: ArrayList<ClientsEntity>,
        private val resultList: ArrayList<ClientsEntity>,
        private val setCheckAll: Boolean = false
    ) : RecyclerView.Adapter<AdapterChooseClients.ClientHolder>() {

        inner class ClientHolder(itemView: View, val dBinding: RowItemClientsDialogBinding) :
            RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterChooseClients.ClientHolder {
            val dBinding = RowItemClientsDialogBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ClientHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(holder: AdapterChooseClients.ClientHolder, position: Int) {

            holder.dBinding.clientName.text = list[position].clientName
            holder.dBinding.clientName.isChecked = setCheckAll

            holder.dBinding.clientName.setOnCheckedChangeListener { _, b ->
                if (!b) {
                    resultList.remove(list[position])
                } else {
                    resultList.add(list[position])
                }
            }

        }

        override fun getItemCount() = list.size

    }

}