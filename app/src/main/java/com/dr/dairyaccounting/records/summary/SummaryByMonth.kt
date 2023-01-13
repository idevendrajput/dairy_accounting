package com.dr.dairyaccounting.records.summary

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.iterator
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.databinding.FragmentSummaryMonthlyBinding
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.AppFunctions
import com.dr.dairyaccounting.utils.BillGenerateCallbacks
import com.dr.dairyaccounting.utils.Utils
import com.google.android.material.snackbar.Snackbar

class SummaryByMonth : Fragment() {

    private lateinit var binding : FragmentSummaryMonthlyBinding
    private lateinit var mContext: Context
    private var recordType = AppConstants.RECORD_TYPE_PURCHASE
    private lateinit var room: MyDatabase
    private lateinit var month: String
    private lateinit var year: String

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryMonthlyBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        room = MyDatabase.getDatabase(mContext)
        month = arguments?.getString(AppConstants.MONTH).toString()
        year = arguments?.getString(YEAR).toString()
        recordType = arguments?.getString(AppConstants.RECORD_TYPE).toString()

        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }

        toolBar()
        actions()

    }

    private fun actions() {
        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateUi() {

        binding.tb.title = "$month $year"
        binding.tb.subtitle = recordType

        initData()

    }

    private fun toolBar() {

        val menu = binding.tb.menu
        val changeYear = menu[0]
        val changeMonth = menu[1]
        val excel = menu[2]
        val pdf = menu[3]
        val print = menu[4]

        for (sub in changeYear.subMenu) {
            sub.setOnMenuItemClickListener {
                changeYear(sub.title.toString())
                false
            }
        }

        for (sub in changeMonth.subMenu) {
            sub.setOnMenuItemClickListener {
                changeMonth(sub.title.toString())
                false
            }
        }

        val my = "$month $year"

         excel.setOnMenuItemClickListener {
            AppFunctions.createRecordExcel(
                mContext,
                recordType,
                AppFunctions.getMonthInInt(my.split(' ')[0]),
                my.split(' ')[1].toInt(),
                object : BillGenerateCallbacks {
                    override fun onSuccessListener(fileName: String) {
                        Snackbar.make(binding.root, "Excel Downloaded", Snackbar.LENGTH_SHORT)
                            .show()
                    }

                    override fun onFailureListener(error: String) {
                        Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            )
            false
        }

        pdf.setOnMenuItemClickListener {
            AppFunctions.createRecordPdf(
                mContext,
                recordType,
                AppFunctions.getMonthInInt(my.split(' ')[0]),
                my.split(' ')[1].toInt(),
                false,
                object : BillGenerateCallbacks {
                    override fun onSuccessListener(fileName: String) {
                        Snackbar.make(binding.root, "Download Successful", Snackbar.LENGTH_SHORT)
                            .show()
                    }

                    override fun onFailureListener(error: String) {
                        Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            )
            false
        }

        print.setOnMenuItemClickListener {
            AppFunctions.createRecordPdf(
                mContext,
                recordType,
                AppFunctions.getMonthInInt(my.split(' ')[0]),
                my.split(' ')[1].toInt(),
                true,
                object : BillGenerateCallbacks {
                    override fun onSuccessListener(fileName: String) {}
                    override fun onFailureListener(error: String) {
                        Snackbar.make(binding.root, "Something went wrong", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            )
            false
        }

    }

    private fun changeYear(mYear: String) {
        year = mYear
        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }
    }

    private fun changeMonth(mMonth: String) {
        month = mMonth
        Handler(Looper.getMainLooper())
            .post {
                updateUi()
            }
    }


    private fun initData() {

        binding.noResult.visibility = View.GONE

        val list = if (recordType == AppConstants.RECORD_TYPE_PURCHASE) {
            Utils.getPurchaseByMonth(mContext, "$month $year")
        } else {
            Utils.getSaleByMonth(mContext, "$month $year")
        }

        if (list.isEmpty()) {
            binding.noResult.visibility = View.VISIBLE
            return
        }

        binding.rv.adapter = AdapterSummaryMonthly(
            list,
            findNavController(),
            recordType, AppFunctions.getMonthInInt(month),
            year.toInt()
        )

        total(list)

        var fileName = "Records ($recordType) $month $year"

    }


    private fun total(list: ArrayList<MonthlySummaryModel>)  {

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
