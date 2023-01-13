package com.dr.dairyaccounting.ui

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.dairyaccounting.Login
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.database.RecordsEntity
import com.dr.dairyaccounting.databinding.*
import com.dr.dairyaccounting.services.DataUpdateCallBack
import com.dr.dairyaccounting.utils.*
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_ADVANCE
import com.dr.dairyaccounting.utils.AppConstants.AMOUNT_PAID
import com.dr.dairyaccounting.utils.AppConstants.AUTH_STATUS_DONE
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_NAME
import com.dr.dairyaccounting.utils.AppConstants.COLLECTIONS_RECORDS
import com.dr.dairyaccounting.utils.AppConstants.COLLECTION_CLIENTS
import com.dr.dairyaccounting.utils.AppConstants.COLLECTION_CONTROLS
import com.dr.dairyaccounting.utils.AppConstants.DISPLAY_NAME
import com.dr.dairyaccounting.utils.AppConstants.DISPLAY_PHONE
import com.dr.dairyaccounting.utils.AppConstants.EVENING_QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.FILE_FORMAT_PDF
import com.dr.dairyaccounting.utils.AppConstants.FILE_FORMAT_XSL
import com.dr.dairyaccounting.utils.AppConstants.IS_CLIENT_ACTIVE
import com.dr.dairyaccounting.utils.AppConstants.MORNING_QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.PHONE
import com.dr.dairyaccounting.utils.AppConstants.QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.RATE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.RECORD_TYPE_SALE
import com.dr.dairyaccounting.utils.AppConstants.SHIFT
import com.dr.dairyaccounting.utils.AppConstants.TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.UTILS
import com.dr.dairyaccounting.utils.AppFunctions.Companion.checkPermission
import com.dr.dairyaccounting.utils.Utils.Companion.getAuthStatus
import com.dr.dairyaccounting.utils.Utils.Companion.getCalendarTime
import com.dr.dairyaccounting.utils.Utils.Companion.getDisplayName
import com.dr.dairyaccounting.utils.Utils.Companion.getDisplayPhone
import com.dr.dairyaccounting.utils.Utils.Companion.getPhone
import com.dr.dairyaccounting.utils.Utils.Companion.getTodayDate
import com.dr.dairyaccounting.utils.Utils.Companion.setCalendarTime
import com.dr.dairyaccounting.utils.Utils.Companion.setDisplayName
import com.dr.dairyaccounting.utils.Utils.Companion.setDisplayPhone
import com.dr.dairyaccounting.utils.Utils.Companion.storeExcelInStorage
import com.dr.dairyaccounting.utils.Utils.Companion.todayTimeRange
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.util.function.LongFunction
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Home : BaseFragment() {

    private lateinit var binding: FragmentHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var room: MyDatabase
    private var isPrintReport = false

    companion object {
        lateinit var homePageBinding: View
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        homePageBinding = binding.root
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (getAuthStatus(mContext) != AUTH_STATUS_DONE) {
            startActivity(Intent(mContext, Login::class.java))
            return
        } else {
            if (!checkPermission(mContext)) {
                requestPermission(mContext)
            }
        }


//
        room = MyDatabase.getDatabase(mContext)

        actions()
        updateUi()
        toolBar()

    }

    private fun actions() {

        val args = Bundle()
        binding.morningPurchase.setOnClickListener {
            args.putString(SHIFT, MORNING_SHIFT)
            args.putString(RECORD_TYPE, RECORD_TYPE_PURCHASE)
            findNavController().navigate(R.id.purchaseEvening, args)
        }

        binding.eveningPurchase.setOnClickListener {
            args.putString(SHIFT, EVENING_SHIFT)
            args.putString(RECORD_TYPE, RECORD_TYPE_PURCHASE)
            findNavController().navigate(R.id.purchaseEvening, args)
        }

        binding.morningSale.setOnClickListener {
            args.putString(SHIFT, MORNING_SHIFT)
            args.putString(RECORD_TYPE, RECORD_TYPE_SALE)
            findNavController().navigate(R.id.purchaseEvening, args)
        }

        binding.eveningSale.setOnClickListener {
            args.putString(SHIFT, EVENING_SHIFT)
            args.putString(RECORD_TYPE, RECORD_TYPE_SALE)
            findNavController().navigate(R.id.purchaseEvening, args)
        }

        binding.addClient.setOnClickListener {
            findNavController().navigate(R.id.clientsContainer)
        }

        binding.summary.setOnClickListener {
            findNavController().navigate(R.id.summary)
        }

        binding.date.setOnClickListener {
            calendarDialog()
        }

        binding.generateReport.setOnClickListener {
            isPrintReport = false
            generateReport()
        }

        binding.printReport.setOnClickListener {
            isPrintReport = true
            generateReport()
        }

        binding.downloadOnline.setOnClickListener {

            showLoading()
            binding.downloadOnline.isEnabled = false
            Handler(Looper.getMainLooper())
                .post {
                    HomeUtils.downloadOnlineData(room, mContext, object : DownloadDataCallbacks {

                        override fun onRecordDownloadStart() {
                             changeMessage("Downloading records...")
                        }

                        override fun onRecordDownloaded() {
                           changeMessage("Records downloaded")
                        }

                        override fun onRecordDownloadFailed() {
                            changeMessage("Record download failed")
                        }

                        override fun onClientDownloadStart() {
                            changeMessage("Downloading clients...")
                        }

                        override fun onClientDownloaded() {
                            changeMessage("Initializing...")
                        }

                        override fun onClientDownloadFailed() {
                            changeMessage("Client download failed")
                        }

                        override fun onSuccess() {
                            Toast.makeText(
                                mContext,
                                "Data downloaded successful",
                                Toast.LENGTH_SHORT
                            ).show()
                            dismissLoading()
                        }

                        override fun onFailed(error: String) {
                            Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show()
                            dismissLoading()
                        }

                    })
                }

        }

    }

    fun requestPermission(mContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestPermissionManageStorage()
        } else {
            Dexter.withContext(mContext)
                .withPermissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        if (p0?.areAllPermissionsGranted() == true) {

                        } else {
                            Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                    }
                }).check()

        }
    }

    private fun requestPermissionManageStorage() {

        val uri = Uri.parse("package:com.dr.dairyaccounting")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }

    }

    private fun toolBar() {

        val menu = binding.tb.menu
        val calendar = menu[0]

        calendar.setOnMenuItemClickListener {
            calendarDialog()
            true
        }

        binding.tb.setNavigationOnClickListener {
            accountDialog()
        }

    }

    private fun accountDialog() {

        val d = Dialog(mContext)
        val dBinding = DialogAccountInfoBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        d.show()

        dBinding.phone.text = getPhone(mContext)

        dBinding.displayPhone.setText(getDisplayPhone(mContext))
        dBinding.displayName.setText(getDisplayName(mContext))

        dBinding.save.setOnClickListener {
            if (dBinding.displayName.text.toString().trim().isEmpty()) {
                dBinding.displayName.error = "Enter display name"
                return@setOnClickListener
            }
            if (dBinding.displayPhone.text.toString().trim().isEmpty()) {
                dBinding.displayPhone.error = "Enter display phone"
                return@setOnClickListener
            }
            setDisplayPhone(mContext, dBinding.displayPhone.text.toString())
            setDisplayName(mContext, dBinding.displayName.text.toString())
            val map = HashMap<String, Any>()
            map[DISPLAY_NAME] = dBinding.displayName.text.toString()
            map[DISPLAY_PHONE] = dBinding.displayPhone.text.toString()
            db.collection(COLLECTION_CONTROLS)
                .document(UTILS)[map] = SetOptions.merge()
            d.dismiss()
            Toast.makeText(mContext, "Saved", Toast.LENGTH_SHORT).show()
        }

        dBinding.logOut.setOnClickListener {
            AlertDialog.Builder(mContext)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    d.dismiss()
                    SharedPref.logOut(mContext)
                    findNavController().navigate(R.id.login)
                }
                .setNegativeButton("No", null).create().show()
        }

        val t = Temp.allTempDataSize(room)

        if (t > 0) {
            dBinding.unsavedChanges.text = "$t Changes unsaved"
            dBinding.unsavedChanges.visibility = View.VISIBLE
        } else {
            dBinding.unsavedChanges.visibility = View.GONE
        }

        dBinding.deleteData.setOnClickListener {

            d.dismiss()

            if (t > 0) {
                AlertDialog.Builder(mContext)
                    .setTitle("Warning")
                    .setMessage("There are $t records not backed up to server. If you delete offline data changes will be lost.")
                    .setPositiveButton("Delete Anyway") {_,_->
                        showLoading()
                        CoroutineScope(IO).launch {
                            eraseOfflineData()
                        }
                    }.setNeutralButton("Cancel", null).create().show()
            } else {
                showLoading()
                CoroutineScope(IO).launch {
                    eraseOfflineData()
                }
            }


        }

    }

    private suspend fun eraseOfflineData() {
        HomeUtils.deleteOffline(room, object : DataUpdateCallBack {
            override suspend fun onSuccess() {
                withContext(Main) {
                    dismissLoading()
                    Toast.makeText(mContext, "Data erased", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(msg: String) {
                //
            }
        })
    }

    private fun calendarDialog() {

        val d = Dialog(mContext)
        val dBinding = DialogCalendarBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        d.show()

        dBinding.calendar.maxDate = Date().time

        dBinding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val ft = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            val dx = ft.parse("$year/${month + 1}/$dayOfMonth").time
            dBinding.calendar.date = dx
        }

        dBinding.select.setOnClickListener {
            d.dismiss()
            setCalendarTime(mContext, dBinding.calendar.date + 1000)
            updateUi()
        }

    }

    private fun updateUi() {

        binding.date.text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(
            Date(
                getCalendarTime(mContext)
            )
        )
        binding.tb.title = getPhone(mContext)

    }


    private fun generateReport() {

        val optionsRecordType = arrayOf("Bill", "Summary")

        AlertDialog.Builder(mContext)
            .setTitle("Report Type")
            .setItems(optionsRecordType) { _, i ->
                when (i) {
                    0 -> Bills().generateBill(mContext, isPrintReport)
                    1 -> GenerateSummary().generateSummary(mContext, isPrintReport)
                }
            }.create().show()

    }

//    private fun requestPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            try {
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                intent.addCategory("android.intent.category.DEFAULT")
//                intent.data = Uri.parse(
//                    String.format(
//                        "package:%s",
//                        ApplicationProvider.getApplicationContext<Context>().packageName
//                    )
//                )
//                mContext.startActivityForResult(intent, 2296)
//            } catch (e: java.lang.Exception) {
//                val intent = Intent()
//                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
//                ActivityCompat.startActivityForResult(intent, 2296)
//            }
//        } else {
//            //below android 11
//            ActivityCompat.requestPermissions(
//                this@PermissionActivity,
//                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                PERMISSION_REQUEST_CODE
//            )
//        }
//    }

    private fun durationDialog(recordType: String) {


        val d = Dialog(mContext)
        val dBinding = DialogFilterByDurationBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        d.show()

        dBinding.close.setOnClickListener { d.dismiss() }

        val calendar = GregorianCalendar.getInstance()

        var timeRange = todayTimeRange(mContext)

        dBinding.today.text =
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(getCalendarTime(mContext)))
        dBinding.currentMonth.text = getCurrentMonth(calendar)
        dBinding.lastMonth.text = getLastMonth(calendar)
        dBinding.startingDate.text = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.ENGLISH
        ).format(Date(last15dayTimeMillis(calendar)))
        dBinding.endDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())

        dBinding.today.setOnClickListener {
            timeRange = todayTimeRange(mContext)
            refreshButtonState(dBinding, 0)
        }

        dBinding.currentMonth.setOnClickListener {
            timeRange = getMonthTimeRange(0)
            refreshButtonState(dBinding, 1)
        }

        dBinding.lastMonth.setOnClickListener {
            timeRange = getMonthTimeRange(-1)
            refreshButtonState(dBinding, 2)
        }

        dBinding.customDateLayout.setOnClickListener {
            timeRange = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.ENGLISH
            ).parse(dBinding.startingDate.text.toString()).time..SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.ENGLISH
            ).parse(dBinding.endDate.text.toString()).time
            refreshButtonState(dBinding, 3)
        }

        dBinding.startingDate.setOnClickListener {
            calendarDialogForReports(dBinding, true)
        }

        dBinding.endDate.setOnClickListener {
            calendarDialogForReports(dBinding, false)
        }

        dBinding.generateReport.setOnClickListener {

            d.dismiss()
            fileFormatOptions(recordType, timeRange)

        }

    }

    private fun getMonthTimeRange(month: Int) = run {

        val calStart: Calendar = GregorianCalendar()
        calStart.set(Calendar.DAY_OF_MONTH, 1)
        calStart.set(Calendar.HOUR_OF_DAY, 5)
        calStart.set(Calendar.MINUTE, 30)
        calStart.set(Calendar.SECOND, 0)
        calStart.set(Calendar.MILLISECOND, 0)
        calStart.add(Calendar.MONTH, month)
        val startOfMonth = calStart.time.time

        calStart.set(Calendar.DAY_OF_MONTH, calStart.getActualMaximum(Calendar.DAY_OF_MONTH))
        calStart.set(Calendar.HOUR_OF_DAY, calStart.getActualMaximum(Calendar.HOUR_OF_DAY))
        calStart.set(Calendar.MINUTE, calStart.getActualMaximum(Calendar.MINUTE))
        calStart.set(Calendar.SECOND, calStart.getActualMaximum(Calendar.SECOND))
        calStart.set(Calendar.MILLISECOND, calStart.getActualMaximum(Calendar.MILLISECOND))
        calStart.add(Calendar.HOUR_OF_DAY, 5)
        calStart.add(Calendar.MINUTE, 30)
        val endOfMonth = calStart.time.time

        startOfMonth..endOfMonth
    }

    private fun calendarDialogForReports(
        db: DialogFilterByDurationBinding,
        isStartingDate: Boolean
    ) {

        val d = Dialog(mContext)
        val dBinding = DialogCalendarBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        d.show()

        dBinding.calendar.maxDate = Date().time

        dBinding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val ft = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            val dx = ft.parse("$year/${month + 1}/$dayOfMonth").time
            dBinding.calendar.date = dx
        }

        dBinding.select.setOnClickListener {
            d.dismiss()
            if (isStartingDate) {
                db.startingDate.text =
                    SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(dBinding.calendar.date)
            } else {
                db.endDate.text =
                    SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(dBinding.calendar.date)
            }
        }

    }

    private fun refreshButtonState(dBinding: DialogFilterByDurationBinding, selectedButton: Int) {
        for (i in 0..3) {
            val d = dBinding.optionsContainer.getChildAt(i)

            if (i == selectedButton) {
                d.background = ContextCompat.getDrawable(mContext, R.drawable.bg_rounded_5dp)
            } else {
                d.background = null
            }
        }
    }

    private fun last15dayTimeMillis(calendar: Calendar) = run {

        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.HOUR_OF_DAY, 5)
        calendar.add(Calendar.DAY_OF_MONTH, -15)

        calendar.time.time

    }

    private fun getLastMonth(calendar: Calendar) = run {
        calendar.add(Calendar.MONTH, -1)
        val lastMonthDate: Date = calendar.time
        val lastMonthName: String = SimpleDateFormat("MMMM", Locale.ENGLISH).format(lastMonthDate)
        lastMonthName
    }

    private fun getCurrentMonth(calendar: Calendar) = run {
        val cm = SimpleDateFormat("MMMM", Locale.ENGLISH).format(calendar.time)
        cm
    }

    private fun fileFormatOptions(recordType: String, timeRange: LongRange) {

        if (isPrintReport) {
            generateReportOptions(recordType, timeRange, FILE_FORMAT_PDF)
            return
        }

        val options = arrayOf(FILE_FORMAT_XSL, FILE_FORMAT_PDF)

        AlertDialog.Builder(mContext)
            .setTitle("File Format")
            .setItems(options) { _, i ->
                val fileFormat = when (i) {
                    0 -> FILE_FORMAT_PDF
                    1 -> FILE_FORMAT_PDF
                    else -> ""
                }
                generateReportOptions(recordType, timeRange, fileFormat)
            }.create().show()

    }

    private fun generateReportOptions(
        recordType: String,
        timeRange: LongRange,
        fileFormat: String
    ) {

        val options =
            arrayOf("Generate report for all clients", "Generate report for a individual client")

        AlertDialog.Builder(mContext)
            .setTitle("Options")
            .setItems(options) { _, i ->
                when (i) {
                    0 -> allClients(recordType, timeRange, fileFormat)
                    1 -> allClientsDialog(recordType, timeRange, fileFormat)
                }

            }.create().show()

    }

    private fun allClientsDialog(recordType: String, timeRange: LongRange, fileFormat: String) {

        val clients =
            room.clientDao().getAllClientsByClientType(recordType) as ArrayList<ClientsEntity>

        val d = Dialog(mContext)
        val dBinding = DialogAllClientsBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        d.show()

        dBinding.rv.adapter = AdapterAllClientsDialog(clients, recordType, timeRange, fileFormat, d)

    }

    private fun allClients(recordType: String, timeRange: LongRange, fileFormat: String) {

        val record = room.recordDao().getAllRecords() as ArrayList<RecordsEntity>

        if (isPrintReport) {

            val newList = ArrayList<RecordsEntity>()

            record.removeIf {
                (it.timestamp !in timeRange) || (it.amount == 0f && it.quantity == 0f)
            }

            for (c in room.clientDao().getAllClientsByClientType(recordType)) {
                for (r in 0..record.lastIndex) {
                    if (record[r].clientId == c.id) {
                        newList.add(record[r])
                    }
                }
            }

            val fileName = "All Client $recordType report " + SimpleDateFormat(
                "dd MMM",
                Locale.ENGLISH
            ).format(Date(timeRange.first)) + " To " + SimpleDateFormat(
                "dd MMM",
                Locale.ENGLISH
            ).format(Date(timeRange.last))

            generatePDF(newList, fileName)

            return
        }

        val client =
            room.clientDao().getAllClientsByClientType(recordType) as ArrayList<ClientsEntity>

        for (i in client) {
            clientsRecord(i.id, timeRange, fileFormat, i.clientName, recordType)
        }

        Snackbar.make(binding.root, "Report Generated", Snackbar.LENGTH_SHORT).show()
    }

    private fun clientsRecord(
        clientId: String,
        timeRange: LongRange,
        fileFormat: String,
        clientName: String,
        recordType: String
    ) {

        val record = room.recordDao().getAllRecords() as ArrayList<RecordsEntity>

        val fileName = clientName + "'s $recordType report " + SimpleDateFormat(
            "dd MMM",
            Locale.ENGLISH
        ).format(Date(timeRange.first)) + " To " + SimpleDateFormat(
            "dd MMM",
            Locale.ENGLISH
        ).format(Date(timeRange.last))

        record.removeIf {
            (it.timestamp !in timeRange) || it.clientId != clientId || (it.amountPaid == 0f && it.quantity == 0f)
        }

        if (isPrintReport) {
            generatePDF(record, fileName)
            return
        }

        if (fileFormat == FILE_FORMAT_PDF) {
            generatePDF(record, fileName)
        } else {
            createExcel(mContext, record, fileName)
        }

    }

    fun createExcel(mContext: Context, list: ArrayList<RecordsEntity>, fileName: String) {

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
                ) {
                }
            }).check()

    }

    private fun excelOutPut(mContext: Context, list: ArrayList<RecordsEntity>, fileName: String) {

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

        //  Create row
        val headingRow = sheet.createRow(0)

        // cell style
        val cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = HSSFColor.YELLOW.index
        cellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle.alignment = CellStyle.ALIGN_LEFT

        for (i in 0..6) {
            val cell = headingRow.createCell(i)
            cell.setCellValue(
                when (i) {
                    0 -> "Date"
                    1 -> "Client Name"
                    2 -> "Quantity"
                    3 -> "Rate"
                    4 -> "Amount"
                    5 -> "Paid"
                    6 -> "Shift"
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
            if (r.shift > EVENING_SHIFT) {
                row.createCell(6)
                    .setCellValue("Evening")
            } else {
                row.createCell(6)
                    .setCellValue("Morning")
            }
        }

        storeExcelInStorage(workbook, fileName)
        Toast.makeText(mContext, "File Saved Successfully", Toast.LENGTH_SHORT).show()

    }

    private fun generatePDF(list: ArrayList<RecordsEntity>, fileName: String) {

        Dexter.withContext(mContext)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    pdfOutPut(list, fileName)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                }
            }).check()

    }

    fun pdfOutPut(list: ArrayList<RecordsEntity>, fileName: String) {

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

        //  Create row
        val headingRow = sheet.createRow(0)

        // cell style
        val cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = HSSFColor.YELLOW.index
        cellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle.alignment = CellStyle.ALIGN_LEFT

        for (i in 0..6) {
            val cell = headingRow.createCell(i)
            cell.setCellValue(
                when (i) {
                    0 -> "Date"
                    1 -> "Client Name"
                    2 -> "Quantity"
                    3 -> "Rate"
                    4 -> "Amount"
                    5 -> "Paid"
                    6 -> "Shift"
                    else -> ""
                }
            )
            cell.setCellStyle(cellStyle)
        }

        for ((i, r) in list.withIndex()) {

            /* row starts with +1 because row number 1 is heading and instruction row */

            val row = sheet.createRow(i + 1)

            if (r.clientId.isEmpty()) {
                Toast.makeText(mContext, "$r", Toast.LENGTH_SHORT).show()
                for (c in 0..6) {
                    row.createCell(c).setCellValue("♦")
                }
            } else {
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
                if (r.shift > EVENING_SHIFT) {
                    row.createCell(6)
                        .setCellValue("Evening")
                } else {
                    row.createCell(6)
                        .setCellValue("Morning")
                }
            }
        }

        val my_worksheet = workbook.getSheetAt(0)

        val rowIterator: Iterator<Row> = my_worksheet.iterator()

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(dir, "/$fileName.pdf")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (!filePath.exists()) {
            filePath.createNewFile()
        }

        val iText_xls_2_pdf = com.itextpdf.text.Document()
        PdfWriter.getInstance(iText_xls_2_pdf, FileOutputStream(filePath))
        iText_xls_2_pdf.open()

        val my_table = PdfPTable(7)
        var table_cell: PdfPCell?
        var i = 0
        while (rowIterator.hasNext()) {
            val row: Row = rowIterator.next()
            val cellIterator: Iterator<Cell> = row.cellIterator()
            while (cellIterator.hasNext()) {
                val cell: Cell = cellIterator.next()
                when (cell.cellType) {
                    Cell.CELL_TYPE_STRING -> {
                        table_cell = PdfPCell(Phrase(cell.stringCellValue))
                        if (i == 0) {
                            table_cell.backgroundColor = BaseColor.YELLOW
                        }
                        my_table.addCell(table_cell)
                    }
                }
            }
            i++
        }
        iText_xls_2_pdf.add(my_table)
        iText_xls_2_pdf.close()

        if (isPrintReport) {
            Utils.print(activity as AppCompatActivity, filePath.path)
        }
    }

    private inner class AdapterAllClientsDialog(
        val list: ArrayList<ClientsEntity>,
        val recordType: String,
        val timeRange: LongRange,
        val fileFormat: String,
        val d: Dialog
    ) : RecyclerView.Adapter<AdapterAllClientsDialog.ClientHolder>() {

        inner class ClientHolder(itemView: View, val dBinding: RowItemClientsDialogBinding) :
            RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientHolder {
            val dBinding = RowItemClientsDialogBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ClientHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(holder: ClientHolder, position: Int) {

            holder.dBinding.clientName.text = list[position].clientName

            holder.itemView.setOnClickListener {

                d.dismiss()

                clientsRecord(
                    list[position].id,
                    timeRange,
                    fileFormat,
                    list[position].clientName,
                    recordType
                )

                if (!isPrintReport) {
                    Snackbar.make(binding.root, "Report Generated", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }


}