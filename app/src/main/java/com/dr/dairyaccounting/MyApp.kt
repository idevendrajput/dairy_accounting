package com.dr.dairyaccounting

import android.app.Application
import android.content.Context
import com.dr.dairyaccounting.MyApp

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        var appContext: Context? = null
    }

}