package com.dr.dairyaccounting.services

import com.dr.dairyaccounting.utils.AppConstants.SOMETHING_WENT_WRONG

interface DataUpdateCallBack {

    suspend fun onSuccess()

    fun onFailure(msg: String = SOMETHING_WENT_WRONG)

}