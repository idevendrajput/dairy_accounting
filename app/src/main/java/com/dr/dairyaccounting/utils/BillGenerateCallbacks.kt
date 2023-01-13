package com.dr.dairyaccounting.utils

interface BillGenerateCallbacks {

    fun onSuccessListener(fileName: String)

    fun onFailureListener(error: String = "Unknown Error")

}