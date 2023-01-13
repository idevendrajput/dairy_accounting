package com.dr.dairyaccounting.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment

open class BaseFragment: Fragment() {

    lateinit var mContext: Context
    lateinit var loading: Loading

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loading = Loading(mContext).build()
        super.onCreate(savedInstanceState)
    }

    fun showLoading() {
        if (this::loading.isInitialized)
            loading.showLoading()
    }

    fun dismissLoading() {
        if (this::loading.isInitialized)
            loading.dismissLoading()
    }

    fun changeMessage(msg: String) {
        if (this::loading.isInitialized)
            loading.changeTitle(msg)
    }

}