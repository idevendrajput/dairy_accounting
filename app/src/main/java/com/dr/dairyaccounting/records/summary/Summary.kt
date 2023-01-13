package com.dr.dairyaccounting.records.summary

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.databinding.DialogChangeYearBinding
import com.dr.dairyaccounting.databinding.FragmentSummaryBinding
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.DATE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_SALE
import com.dr.dairyaccounting.utils.AppConstants.TAB
import com.dr.dairyaccounting.utils.AppConstants.YEAR
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.storeExcelInStorage
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.CellStyle
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Summary : Fragment() {

    private lateinit var binding : FragmentSummaryBinding
    private lateinit var mContext: Context
    private var year = 0
    private var recordType = RECORD_TYPE_PURCHASE

    companion object {
        lateinit var excel: MenuItem
        lateinit var pdf: MenuItem
        lateinit var print: MenuItem
    }

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolBar()
        setTabLayout()
        actions()

    }

    private fun actions() {
        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setTabLayout() {

        val calendar: Calendar = GregorianCalendar.getInstance()
        calendar.add(Calendar.YEAR, -year)

        val tabs = ArrayList<ItemTabs>()

        tabs.apply {
            add(ItemTabs("Today", SimpleDateFormat("dd MM y", Locale.ENGLISH).format(Date())))
            add(ItemTabs(getCurrentMonth(calendar), getCurrentMonthAndYear(calendar)))
            add(ItemTabs(getLastMonth(calendar), getLastMonthAndYear(calendar)))
            add(ItemTabs(getCurrentYear(calendar), ""))
        }

        val adapter = AdapterPurchaseSummary(
            requireActivity().supportFragmentManager,
            lifecycle,
            tabs
        )

        binding.vp.adapter = adapter
        binding.vp.isUserInputEnabled = false

        TabLayoutMediator(
            binding.tablayout, binding.vp
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = tabs[position].title
        }.attach()

    }

    inner class ItemTabs(val title: String, val date: String)

    private fun toolBar() {

        val menu = binding.tb.menu

        val sale = menu[0]
        val purchase = menu[1]
        val customSummary = menu[2]
        val changeYear = menu[3]
        excel = menu[4]
        pdf = menu[5]
        print = menu[6]

        sale.setOnMenuItemClickListener {
            recordType = RECORD_TYPE_SALE
            setTabLayout()
            false
        }

        purchase.setOnMenuItemClickListener {
            recordType = RECORD_TYPE_PURCHASE
            setTabLayout()
            false
        }

        customSummary.setOnMenuItemClickListener {
            val args = Bundle()
            args.putString(RECORD_TYPE, recordType)
            findNavController().navigate(R.id.summaryCustom, args)
            false
        }

        changeYear.setOnMenuItemClickListener {
            changeYearDialog()
            false
        }

    }

    private fun getLastMonth(calendar: Calendar) = run {
        calendar.add(Calendar.MONTH, -1)
        val lastMonthDate: Date = calendar.time
        val lastMonthName: String = SimpleDateFormat("MMMM", Locale.ENGLISH).format(lastMonthDate)
        lastMonthName
    }

    private fun getCurrentMonth(calendar: Calendar) = run {
        val cm =  SimpleDateFormat("MMMM", Locale.ENGLISH).format(calendar.time)
        cm
    }

    private fun getCurrentMonthAndYear(calendar: Calendar) = SimpleDateFormat("MMMM y", Locale.ENGLISH).format(calendar.time)

    private fun getLastMonthAndYear(calendar: Calendar) = run {
        val lastMonthDate: Date = calendar.time
        val lastMonthName: String = SimpleDateFormat("MMMM y", Locale.ENGLISH).format(lastMonthDate)
        lastMonthName
    }

    private fun getCurrentYear(calendar: Calendar) = run {
        val currentYear: String = SimpleDateFormat("y", Locale.ENGLISH).format(calendar.time)
        currentYear
    }

    private fun changeYearDialog() {

        val d = Dialog(mContext)
        val dBinding = DialogChangeYearBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        d.show()

        val years = ArrayList<Int>()

        for (i in 0..9) {
            years.add(i)
        }

        val adapter = AdapterYears(years, d)

        dBinding.rvYears.adapter = adapter

    }

    fun createExcel(mContext: Context, list: ArrayList<SummaryItems>, fileName: String) {

        Dexter.withContext(mContext)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    excelOutPut(mContext, list, fileName)
                }
                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {}
            }).check()

    }

    private fun excelOutPut(mContext: Context, list: ArrayList<SummaryItems>, fileName: String) {

        if (list.isEmpty()) {
            Toast.makeText(mContext, "No Record Found", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Utils.isExternalStorageAvailable() || Utils.isExternalStorageReadOnly()) {
            Toast.makeText(mContext, AppConstants.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
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

        //  Crete row
        val headingRow = sheet.createRow(0)

        // cell style
        val cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = HSSFColor.YELLOW.index
        cellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle.alignment = CellStyle.ALIGN_LEFT

        for (i in 0..6) {
            val cell = headingRow.createCell(i)
            cell.setCellValue(when(i) {
                0-> "Sr. No."
                1-> "Client Name"
                2-> "Morning Quantity"
                3-> "Evening Quantity"
                4-> "Rate"
                5-> "Amount"
                6-> "Paid"
                else-> ""
            })
            cell.setCellStyle(cellStyle)
        }

        for ((i, r) in list.withIndex()) {

            /* row starts with +1 because row number 1 is heading and instruction row */

            val row = sheet.createRow(i + 1)
            row.createCell(0)
                .setCellValue((i + 1).toString())
            row.createCell(1)
                .setCellValue(r.recordsEntity.clientName)
            row.createCell(2)
                .setCellValue(r.morningQuantity.toString())
            row.createCell(3)
                .setCellValue(r.eveningQuantity.toString())
            row.createCell(4)
                .setCellValue(r.recordsEntity.rate.toString())
            row.createCell(5)
                .setCellValue(r.recordsEntity.amount.toString())
            row.createCell(6)
                .setCellValue(r.recordsEntity.amountPaid.toString())

        }

        storeExcelInStorage(workbook, fileName)
        Toast.makeText(mContext, "File Saved Successfully", Toast.LENGTH_SHORT).show()

    }

    inner class AdapterPurchaseSummary(fragmentManager: FragmentManager, lifecycle: Lifecycle, val tabs: ArrayList<ItemTabs>) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun createFragment(position: Int): Fragment {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, -year)
            val bundle = Bundle()
            bundle.putInt(TAB, position)
            bundle.putString(YEAR, SimpleDateFormat("y", Locale.ENGLISH).format(cal.time).toString())
            bundle.putString(RECORD_TYPE, recordType)
            bundle.putString(DATE, tabs[position].date)
            val fragment = if (position == 3) SummaryYearly() else if (position == 0) SummaryChild() else SummaryChildMonthly()
            fragment.arguments = bundle
            return fragment
        }

        override fun getItemCount(): Int {
            return 4
        }
    }

    inner class AdapterYears(val list: ArrayList<Int>, val d: Dialog): RecyclerView.Adapter<AdapterYears.YearHolder>() {

        inner class YearHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return YearHolder(view)
        }

        override fun onBindViewHolder(holder: YearHolder, position: Int) {

            val text = holder.itemView.findViewById<TextView>(android.R.id.text1)

            val calendar = GregorianCalendar.getInstance()
            calendar.add(Calendar.YEAR, -position)

            text.text = SimpleDateFormat("yyyy", Locale.ENGLISH).format(calendar.time)

            val p = position

            holder.itemView.setOnClickListener {
                year = p
                setTabLayout()
                d.dismiss()
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

}
