package com.dr.dairyaccounting.ui

interface DownloadDataCallbacks {

    fun onRecordDownloadStart()

    fun onRecordDownloaded()

    fun onRecordDownloadFailed()

    fun onClientDownloadStart()

    fun onClientDownloaded()

    fun onClientDownloadFailed()

    fun onSuccess()

    fun onFailed(error: String)

}