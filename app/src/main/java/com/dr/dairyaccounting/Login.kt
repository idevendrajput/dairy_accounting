package com.dr.dairyaccounting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.dr.dairyaccounting.databinding.FragmentLoginBinding
import com.dr.dairyaccounting.ui.MainActivity
import com.dr.dairyaccounting.utils.AppConstants
import com.dr.dairyaccounting.utils.AppConstants.AUTH_STATUS_DONE
import com.dr.dairyaccounting.utils.AppConstants.COLLECTION_USERS
import com.dr.dairyaccounting.utils.AppConstants.PASSWORD
import com.dr.dairyaccounting.utils.Utils
import com.dr.dairyaccounting.utils.Utils.Companion.getAuthStatus
import com.dr.dairyaccounting.utils.Utils.Companion.getPassword
import com.dr.dairyaccounting.utils.Utils.Companion.getPhone
import com.dr.dairyaccounting.utils.Utils.Companion.setAuthStatus
import com.dr.dairyaccounting.utils.Utils.Companion.setPassword
import com.dr.dairyaccounting.utils.Utils.Companion.setPhone
import com.dr.dairyaccounting.utils.Utils.Companion.setUserId
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.jvm.Throws

class Login: AppCompatActivity() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        navigate()

        Handler(Looper.getMainLooper())
            .post {
                updateUi()
                actions()
            }

    }

    private fun navigate() {

//        if (getAuthStatus(mContext) == AUTH_STATUS_DONE) {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }

    }

    private fun updateUi() {

        if (getAuthStatus(mContext) == AUTH_STATUS_DONE) {
            binding.phone.visibility = View.GONE
        }

    }

    private fun actions() {

        binding.login.setOnClickListener {

            if (binding.phone.text.toString().trim().isEmpty() && getAuthStatus(mContext) != AUTH_STATUS_DONE) {
                binding.phone.error = "Enter phone number"
                return@setOnClickListener
            }

            if (binding.password.text.toString().trim().isEmpty()) {
                binding.password.error = "Enter password"
                return@setOnClickListener
            }

            if (getAuthStatus(mContext) == AUTH_STATUS_DONE) {
                loginAfterAuth()
            } else {
                login()
            }

        }

    }

    private fun login() {

        showLoading(true)

        db.collection(COLLECTION_USERS)
            .document(binding.phone.text.toString().trim())
            .get().addOnSuccessListener {
                if (it.exists()) {

                    val password = it[PASSWORD].toString()

                    if (password == binding.password.text.toString().trim()) {

                        showLoading(false)

                        CoroutineScope(IO)
                            .launch {
                                setAuthStatus(mContext, AUTH_STATUS_DONE)
                                setPhone(mContext, binding.phone.text.toString())
                                setUserId(mContext, it.id)
                                setPassword(mContext, password)
                            }

                        startActivity(Intent(mContext, MainActivity::class.java))
                        finish()

                    } else {
                        showLoading(false)
                        Snackbar.make(binding.root, "Incorrect Password", Snackbar.LENGTH_SHORT).show()
                    }

                } else {
                    showLoading(false)
                    Snackbar.make(binding.root, "User not exist", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    @Throws(Exception::class)
    private fun loginAfterAuth() {

        showLoading(true)

        if (binding.password.text.toString().trim() == getPassword(mContext)) {
            startActivity(Intent(mContext, MainActivity::class.java))
            finish()
        } else {
            binding.password.error = "Incorrect Password"
            showLoading(false)
        }

    }

    private fun showLoading(isShow : Boolean) {
        if (isShow) {
            binding.pb.visibility = View.VISIBLE
            binding.login.text = ""
        } else {
            binding.pb.visibility = View.GONE
            binding.login.text = "Securely Login"
        }
    }

}