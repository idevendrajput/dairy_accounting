package com.dr.dairyaccounting.records.summary

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract.AUTHORITY
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.databinding.FragmentSummaryChildMonthlyBinding
import com.dr.dairyaccounting.databinding.RowItemMonthlySummaryBinding
import com.dr.dairyaccounting.records.summary.Summary.Companion.excel
import com.dr.dairyaccounting.records.summary.Summary.Companion.pdf
import com.dr.dairyaccounting.records.summary.Summary.Companion.print
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.DATE
import com.dr.dairyaccounting.utils.AppConstants.MONTH
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.AppFunctions
import com.dr.dairyaccounting.utils.AppFunctions.Companion.createRecordExcel
import com.dr.dairyaccounting.utils.AppFunctions.Companion.createRecordPdf
import com.dr.dairyaccounting.utils.AppFunctions.Companion.getMonthInInt
import com.dr.dairyaccounting.utils.BillGenerateCallbacks
import com.dr.dairyaccounting.utils.Utils.Companion.getPurchaseByMonth
import com.dr.dairyaccounting.utils.Utils.Companion.getSaleByMonth
import com.google.android.material.snackbar.Snackbar
import java.io.File
import kotlin.collections.ArrayList

class SummaryChildMonthly : Fragment() {

    private lateinit var binding: FragmentSummaryChildMonthlyBinding
    private lateinit var mContext: Context
    private var recordType = AppConstants.RECORD_TYPE_PURCHASE
    private lateinit var room: MyDatabase
    private lateinit var monthYear: String

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryChildMonthlyBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        room = MyDatabase.getDatabase(mContext)
        monthYear = arguments?.getString(DATE).toString()
        recordType = arguments?.getString(AppConstants.RECORD_TYPE).toString()

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

        val list = if (recordType == AppConstants.RECORD_TYPE_PURCHASE) {
            getPurchaseByMonth(mContext, monthYear)
        } else {
            getSaleByMonth(mContext, monthYear)
        }

        if (list.isEmpty()) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterSummaryMonthly(
            list,
            findNavController(),
            recordType, getMonthInInt(monthYear.split(' ')[0]).toString().toInt(),
            monthYear.split(' ')[1].trim().toInt()
        )

        total(list)

        val my = monthYear

        excel.setOnMenuItemClickListener {
            createRecordExcel(
                mContext,
                recordType,
                getMonthInInt(my.split(' ')[0]),
                my.split(' ')[1].toInt(),
                object : BillGenerateCallbacks {
                    override fun onSuccessListener(fileName: String) {
                        Snackbar.make(binding.root, "Excel Downloaded", Snackbar.LENGTH_SHORT).show()
                    }
                    override fun onFailureListener(error: String) {
                        Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT).show()
                    }
                }
            )
            false
        }

        pdf.setOnMenuItemClickListener {
            createRecordPdf(
                mContext,
                recordType,
                 getMonthInInt(monthYear.split(' ')[0]),
                monthYear.split(' ')[1].toInt(),
                false,
                object: BillGenerateCallbacks {
                    override fun onSuccessListener(fileName: String) {
                        Snackbar.make(binding.root, "Download Successful", Snackbar.LENGTH_SHORT).show()
                    }
                    override fun onFailureListener(error: String) {
                        Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT).show()
                    }
                }
            )
            false
        }
        
        print.setOnMenuItemClickListener {
            createRecordPdf(
                mContext,
                recordType,
                getMonthInInt(monthYear.split(' ')[0]),
                monthYear.split(' ')[1].toInt(),
                true,
                object: BillGenerateCallbacks {
                    override fun onSuccessListener(fileName: String) { }
                    override fun onFailureListener(error: String) {
                        Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT).show()
                    }
                }
            )
            false
        }

        excel.isVisible = true
        pdf.isVisible = true
        print.isVisible = true

    }

    private fun total(list: ArrayList<MonthlySummaryModel>) {

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
        binding.lastBalance.text = String.format("%.2f", totalLastBalance)
        binding.advance.text = String.format("%.2f", totalAdvance)

    }
}

class AdapterSummaryMonthly(private val list: ArrayList<MonthlySummaryModel>, private val navController: NavController, val recordType: String, val month: Int?, val year: Int?) :
    RecyclerView.Adapter<AdapterSummaryMonthly.RecordHolder>() {

    class RecordHolder(itemView: View, val dBinding: RowItemMonthlySummaryBinding) :
        RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecordHolder {
        val dBinding =
            RowItemMonthlySummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

        if (month != null && year != null) {
            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(AppConstants.CLIENT_ID, list[position].clientId)
                args.putString(AppConstants.RECORD_TYPE, recordType)
                args.putInt(MONTH, month)
                args.putInt(YEAR, year)
                navController.navigate(R.id.summaryByPerson, args)
            }
        }

    }

    override fun getItemCount() = list.size

}

class MonthlySummaryModel(
    val clientName: String,
    val eveningQnt: Float,
    val morningQnt: Float,
    val amount: Float,
    val paid: Float,
    val lastBalance: Float,
    val balance: Float,
    val advance: Float,
    val clientId: String
)