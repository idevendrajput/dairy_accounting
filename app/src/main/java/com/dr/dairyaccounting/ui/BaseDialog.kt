package com.dr.dairyaccounting.ui

import android.app.Dialog
import android.content.Context
import com.dr.dairyaccounting.R

open class BaseDialog(context: Context) : Dialog(context, R.style.Theme_DairyAccounting) {

    private val loading = Loading(context).build()

    val mContext = context

    fun showLoading() = loading.showLoading()

    fun dismissLoading() = loading.dismissLoading()

    open fun showDialog() = this.show()

    fun dismissDialog() = this.dismiss()

    fun setCancelable() = loading.setCancelable(true)

}