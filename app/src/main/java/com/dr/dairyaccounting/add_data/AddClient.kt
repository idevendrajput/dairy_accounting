package com.dr.dairyaccounting.add_data

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE_PURCHASE
import com.dr.dairyaccounting.utils.AppConstants.ACCOUNT_TYPE_SALE
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_ID
import com.dr.dairyaccounting.utils.AppConstants.CLIENT_NAME
import com.dr.dairyaccounting.utils.AppConstants.COLLECTION_CLIENTS
import com.dr.dairyaccounting.utils.AppConstants.EVENING_QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.MORNING_QUANTITY
import com.dr.dairyaccounting.utils.AppConstants.ORDER_TIMESTAMP
import com.dr.dairyaccounting.utils.AppConstants.PHONE
import com.dr.dairyaccounting.utils.AppConstants.RATE
import com.dr.dairyaccounting.utils.AppConstants.TIMESTAMP
import com.dr.dairyaccounting.R
import com.dr.dairyaccounting.database.ClientsEntity
import com.dr.dairyaccounting.database.MyDatabase
import com.dr.dairyaccounting.databinding.FragmentAddClientBinding
import com.dr.dairyaccounting.ui.BaseFragment
import com.dr.dairyaccounting.utils.AppConstants.EVENING_SHIFT
import com.dr.dairyaccounting.utils.AppConstants.IS_CLIENT_ACTIVE
import com.dr.dairyaccounting.utils.AppConstants.MORNING_SHIFT
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

class AddClient: BaseFragment() {

    private lateinit var binding: FragmentAddClientBinding
    private lateinit var clientId: String
    private lateinit var accountType: String
    private val db = FirebaseFirestore.getInstance()
    private lateinit var room: MyDatabase
    private var isUserExist = false
    private var client: ClientsEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddClientBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clientId = arguments?.getString(CLIENT_ID) ?: UUID.randomUUID().toString()

        room = MyDatabase.getDatabase(mContext)

        val clients = room.clientDao().getClientById(clientId)

        if (clients.isNotEmpty()) {
            isUserExist = true
            client = clients[0]
            loadPreviousData(clients[0])
        }

        actions()
        toolBar()

    }

    private fun actions() {

        binding.tb.setNavigationOnClickListener {
            findNavController().popBackStack(R.id.addClient, true)
        }

        binding.save.setOnClickListener {
            if (isVerified()) {
                update()
            }
        }

        binding.active.setOnCheckedChangeListener { _, _ ->
            binding.inactive.isChecked = !binding.active.isChecked
        }

        binding.inactive.setOnCheckedChangeListener { _, _ ->
            binding.active.isChecked = !binding.inactive.isChecked
        }

        binding.accountTypeSale.setOnCheckedChangeListener { _, _ ->
            binding.accountTypePurchase.isChecked = !binding.accountTypeSale.isChecked
        }

        binding.accountTypePurchase.setOnCheckedChangeListener { _, _ ->
            binding.accountTypeSale.isChecked = !binding.accountTypePurchase.isChecked
        }

        binding.morning.setOnCheckedChangeListener { _, _ ->
            if (!binding.morning.isChecked) {
                binding.quantityMorning.visibility = View.GONE
                binding.quantityMorning.setText("0")
                if (!binding.evening.isChecked) {
                    binding.evening.isChecked = true
                    binding.quantityEvening.visibility = View.VISIBLE
                }
                return@setOnCheckedChangeListener
            }
            binding.quantityMorning.visibility = View.VISIBLE
        }

        binding.evening.setOnCheckedChangeListener { _, _ ->
            if (!binding.evening.isChecked) {
                binding.quantityEvening.visibility = View.GONE
                binding.quantityEvening.setText("0")
                if (!binding.morning.isChecked) {
                    binding.morning.isChecked = true
                    binding.quantityMorning.visibility = View.VISIBLE
                }
                return@setOnCheckedChangeListener
            }
            binding.quantityEvening.visibility = View.VISIBLE
        }

    }

    private fun toolBar() {

        val menu = binding.tb.menu
        val save = menu[0]

        save.setOnMenuItemClickListener {
            if (isVerified()) {
                update()
            }
            false
        }

    }

    private fun isVerified() = run {

        var verify = true

        val clients = room.clientDao().getAllClients()

        if (binding.clientName.text.toString().trim().isEmpty()) {
            binding.clientName.error = "Enter client name"
            verify = false
        }

        if (binding.morning.isChecked && binding.quantityMorning.text.toString().trim().isEmpty()) {
            binding.quantityMorning.error = "This field is required"
            verify = false
        }

        if (binding.evening.isChecked && binding.quantityEvening.text.toString().trim().isEmpty()) {
            binding.quantityMorning.error = "This field is required"
            verify = false
        }

        if (binding.rate.text.toString().trim().isEmpty()) {
            binding.rate.error = "This field is required"
            verify = false
        }

        if (!isUserExist) {
            for (c in clients) {
                if (binding.clientName.text.toString().lowercase() == c.clientName.lowercase()) {
                    binding.clientName.error = "A client already exist with this name"
                    verify = false
                }
            }
        }

        verify
    }

    private fun loadPreviousData(client: ClientsEntity) {

        binding.active.isChecked = client.isActive
        binding.clientName.setText(client.clientName)
        binding.phoneNumber.setText(client.phone)
        binding.accountTypePurchase.isChecked = client.accountType == ACCOUNT_TYPE_PURCHASE
        binding.accountTypeSale.isChecked = client.accountType == ACCOUNT_TYPE_SALE
        binding.morning.isChecked = client.morningShift
        binding.evening.isChecked = client.eveningShift
        binding.quantityMorning.setText(client.morningQuantity.toString())
        binding.quantityEvening.setText(client.eveningQuantity.toString())
        binding.rate.setText(client.rate.toString())

    }

    private fun update() {

        accountType = if (binding.accountTypePurchase.isChecked) {
            ACCOUNT_TYPE_PURCHASE
        } else {
            ACCOUNT_TYPE_SALE
        }

        val morningQuantity = if (binding.quantityMorning.text.toString().trim()
                .isEmpty()
        ) 0f else binding.quantityMorning.text.toString().trim().toFloat()
        val eveningQuantity = if (binding.quantityEvening.text.toString().trim()
                .isEmpty()
        ) 0f else binding.quantityEvening.text.toString().trim().toFloat()

        val map = HashMap<String, Any>()
        map[CLIENT_NAME] = binding.clientName.text.toString()
        map[CLIENT_ID] = clientId
        map[PHONE] = binding.phoneNumber.text.toString()
        map[ACCOUNT_TYPE] = accountType
        map[MORNING_QUANTITY] = morningQuantity
        map[EVENING_QUANTITY] = eveningQuantity
        map[MORNING_SHIFT] = binding.morning.isChecked
        map[EVENING_SHIFT] = binding.evening.isChecked
        map[IS_CLIENT_ACTIVE] = binding.active.isChecked
        map[RATE] = binding.rate.text.toString().trim().toFloat()
        if (!isUserExist) {
            map[TIMESTAMP] = System.currentTimeMillis()
            map[ORDER_TIMESTAMP] = System.currentTimeMillis()
        }

        showLoading()

        db.collection(COLLECTION_CLIENTS)
            .document(clientId)
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                CoroutineScope(IO)
                    .launch {
                        try {
                            room.clientDao().insertClient(
                                ClientsEntity(
                                    id = clientId,
                                    clientName = binding.clientName.text.toString(),
                                    phone = binding.phoneNumber.text.toString(),
                                    accountType = accountType,
                                    morningQuantity = morningQuantity,
                                    rate = binding.rate.text.toString().trim().toFloat(),
                                    eveningQuantity = eveningQuantity,
                                    timestamp = if (isUserExist) client?.timestamp
                                        ?: System.currentTimeMillis() else System.currentTimeMillis(),
                                    orderTimeStamp = if (isUserExist) client?.orderTimeStamp
                                        ?: System.currentTimeMillis() else System.currentTimeMillis(),
                                    isActive = binding.active.isChecked,
                                    morningShift = binding.morning.isChecked,
                                    eveningShift = binding.evening.isChecked
                                )
                            )
                        } catch (e: Exception) {
                            room.clientDao().updateClient(
                                ClientsEntity(
                                    id = clientId,
                                    clientName = binding.clientName.text.toString(),
                                    phone = binding.phoneNumber.text.toString(),
                                    accountType = accountType,
                                    morningQuantity = morningQuantity,
                                    rate = binding.rate.text.toString().trim().toFloat(),
                                    eveningQuantity = eveningQuantity,
                                    timestamp = if (isUserExist) client?.timestamp!! else System.currentTimeMillis(),
                                    orderTimeStamp = if (isUserExist) client?.orderTimeStamp!! else System.currentTimeMillis(),
                                    isActive = binding.active.isChecked,
                                    morningShift = binding.morning.isChecked,
                                    eveningShift = binding.evening.isChecked
                                )
                            )
                        }
                    }
                Handler(Looper.getMainLooper())
                    .postDelayed({
                        dismissLoading()
                        Snackbar.make(binding.root, "Successful", Snackbar.LENGTH_SHORT).show()
                    }, 500)
            }
            .addOnFailureListener {
                dismissLoading()
                Toast.makeText(mContext, it.message.toString(), Toast.LENGTH_SHORT).show()
            }

    }

}