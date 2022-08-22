package com.udacity.main

import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivityViewModelProvider(
    private val application: Application,
    private val downloadManager: DownloadManager,
    private val notificationManager: NotificationManager
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel(application, downloadManager, notificationManager) as T
        }
        throw IllegalArgumentException("Unable to construct viewmodel")
    }
}