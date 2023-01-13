package com.dr.dairyaccounting.ui

import android.app.Dialog
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.dr.dairyaccounting.R

class Loading(val mContext: Context) {

    private lateinit var d: Dialog

    fun build() = run {
        d = Dialog(mContext)
        d.setContentView(R.layout.loading)
        d.window?.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        setCancelable()
        this
    }

    fun changeTitle(title: String) {
        if (this::d.isInitialized){
            val mTitle = d.findViewById<TextView>(R.id.title)
            mTitle.text = title
        }
    }

    fun showLoading() {
        if (this::d.isInitialized)
            d.show()
    }

    fun dismissLoading() {
        if (this::d.isInitialized)
            d.dismiss()
    }

    fun setCancelable(isCancelable: Boolean = false) {
        if (this::d.isInitialized)
            d.setCancelable(isCancelable)
    }

}