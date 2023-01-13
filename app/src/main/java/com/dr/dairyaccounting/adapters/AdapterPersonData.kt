package com.dr.dairyaccounting.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.databinding.RowItemPersonDataBinding
import com.dr.dairyaccounting.records.summary.ItemPersonData

class AdapterPersonData(val list: ArrayList<ItemPersonData>) :
        RecyclerView.Adapter<AdapterPersonData.DataHolder>() {

        inner class DataHolder(itemView: View, val dBinding: RowItemPersonDataBinding) :
            RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataHolder {
            val dBinding =
                RowItemPersonDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DataHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(holder: DataHolder, position: Int) {

            holder.dBinding.date.text = list[position].date
            holder.dBinding.quantityMorning.text = String.format("%.2f", list[position].mQnt)
            holder.dBinding.quantityEvening.text = String.format("%.2f", list[position].eQnt)
            holder.dBinding.rate.text = list[position].rate
            holder.dBinding.amount.text = String.format("%.2f", list[position].amount)
            holder.dBinding.amountPaid.text = String.format("%.2f", list[position].paid)

        }

        override fun getItemCount() = list.size


    }