package com.dr.dairyaccounting.records.filters

import android.content.Context
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import kotlin.collections.ArrayList

class Filter(val mContext: Context) {

    val room = MyDatabase.getDatabase(mContext)
    val allClient = room.clientDao().getAllClients() as ArrayList<ClientsEntity>
    private val saleClients = room.clientDao().getAllSalesClients()
    private val purchaseClient = room.clientDao().getAllPurchaseClients()
    val allRecords = room.recordDao().getAllRecords() as ArrayList<RecordsEntity>

}


public class ReportTypeNotFoundException(private val msg: String? = null): Exception() {
    override val message: String? get() = msg ?: super.message
}