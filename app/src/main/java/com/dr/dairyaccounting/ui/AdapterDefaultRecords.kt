package com.dr.dairyaccounting.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.RowItemMorningRecordsBinding
import com.itextpdf.xmp.impl.Utils

class AdapterDefaultRecords(items: ArrayList<RecordsEntity>, val recordsView: RecordView, mContext: Context): RecyclerView.Adapter<AdapterDefaultRecords.RecordHolder>() {

    val room = MyDatabase.getDatabase(mContext)

    var list = items

    class RecordHolder(itemView: View, val dBinding: RowItemMorningRecordsBinding) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecordHolder {
        val dBinding = RowItemMorningRecordsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordHolder(dBinding.root, dBinding)
    }
    
    override fun onBindViewHolder(
        holder: RecordHolder,
        position: Int
    ) {

        val item = list[position]

//        val it = room.tempRecordDao().getRecordsById(list[position].id)
//        if (it.isNotEmpty()) {
//            item = com.dr.dairyaccounting.utils.Utils.tempToRecordItem(it[0])
//        }

        val srNo = position + 1
        holder.dBinding.srNo.text = srNo.toString()
        holder.dBinding.name.text = item.clientName
        holder.dBinding.quantity.text = item.quantity.toString()
        holder.dBinding.rate.text = item.rate.toString()
        holder.dBinding.amount.text = item.amount.toString()
        holder.dBinding.amountPaid.text = item.amountPaid.toString()

        recordsView.recordViw(holder.itemView, list[position].clientId)

    }

    override fun getItemCount(): Int {
        return list.size
    }

}

interface RecordView {
    fun recordViw(itemView: View, clientId: String)
}

