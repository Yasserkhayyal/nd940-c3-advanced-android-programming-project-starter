package com.udacity.main

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.*
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.udacity.R
import com.udacity.databinding.ActivityMainBinding
import com.udacity.models.ButtonState
import com.udacity.models.DownloadStatus
import com.udacity.models.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 100

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0
    private lateinit var selectedDownloadUri: URL
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var viewModelProvider: MainActivityViewModelProvider
    private val downloadManager by lazy {
        getSystemService(DownloadManager::class.java)
    }
    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }
    private var downloadStatus = DownloadStatus.FAIL
    private var isNetworkConnected = false

    private val connectivityCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onUnavailable() {
                checkNetworkConnection(::updateUI)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                checkNetworkConnection(::updateUI)
            }

            override fun onLost(network: Network) {
                checkNetworkConnection(::updateUI)
            }

            override fun onAvailable(network: Network) {
                checkNetworkConnection(::updateUI)
            }
        }
    }

    private val downloadCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                val cursor = queryResultCursor(id)
                if (cursor.moveToFirst()) {
                    when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadStatus = DownloadStatus.SUCCESS
                            binding.contentMainLayout.customButton.buttonState =
                                ButtonState.Completed
                        }
                        else -> {
                            downloadStatus = DownloadStatus.FAIL
                            binding.contentMainLayout.customButton.buttonState = ButtonState.Failed
                            //this is added to prevent race condition between this receiver and the network callback when the internet is back
                            //the delay is added to clearly show the previous state before the network is back
                            //strangely this doesn't fire till the network is back and hence comes the problem
                            lifecycleScope.launch(Dispatchers.Main) {
                                delay(3000)
                                checkNetworkConnection(this@MainActivity::updateUI)
                            }
                        }
                    }
                }
                viewModel.createNotification(selectedDownloadUri, downloadStatus)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)
        setupViewModel()
        viewModel.createNotificationChannel()

        binding.contentMainLayout.downloadRadioGroup.setOnCheckedChangeListener { _, index ->
            selectedDownloadUri = when (index) {
                R.id.retrofit_radio_button -> URL.RETROFIT_URI
                R.id.udacity_radio_button -> URL.UDACITY_URI
                else -> URL.GLIDE_URI
            }
        }

        binding.contentMainLayout.customButton.setOnClickListener {
            if (::selectedDownloadUri.isInitialized) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    initiateDownloadRequest()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE
                    )
                }
            } else {
                Toast.makeText(this, getString(R.string.select_option_toast), Toast.LENGTH_SHORT)
                    .show()
            }

        }
        checkNetworkConnection(::updateUI)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateDownloadRequest()
            }
        }
    }

    private fun initiateDownloadRequest() {
        downloadID = viewModel.download(selectedDownloadUri)
        getDownloadState(downloadID)
    }

    private fun getDownloadState(downloadId: Long) {
        val cursor = queryResultCursor(downloadId)
        if (cursor.moveToFirst()) {
            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_RUNNING -> {
                    binding.contentMainLayout.customButton.buttonState = ButtonState.Loading
                }
                DownloadManager.STATUS_PENDING -> {
                    getDownloadState(downloadId)
                }
            }
        }
    }

    private fun queryResultCursor(downloadId: Long): Cursor =
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId))

    override fun onStart() {
        super.onStart()
        registerReceiver(
            downloadCompletedReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .addCapability(NET_CAPABILITY_VALIDATED)
            .addCapability(NET_CAPABILITY_TRUSTED)
            .addTransportType(TRANSPORT_CELLULAR)
            .addTransportType(TRANSPORT_WIFI)
            .addTransportType(TRANSPORT_ETHERNET)
            .addTransportType(TRANSPORT_VPN)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, connectivityCallback)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(downloadCompletedReceiver)
        connectivityManager.unregisterNetworkCallback(connectivityCallback)
    }

    private fun setupViewModel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val downloadManager = getSystemService(DownloadManager::class.java)
        viewModelProvider =
            MainActivityViewModelProvider(application, downloadManager, notificationManager)
        viewModel = ViewModelProvider(this, viewModelProvider)[MainActivityViewModel::class.java]
    }

    private fun checkNetworkConnection(updateUI: () -> Unit) {
        isNetworkConnected = connectivityManager.activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)?.let { networkCapabilities ->
                networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET) ||
                        networkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED) ||
                        networkCapabilities.hasCapability(NET_CAPABILITY_TRUSTED)
            } ?: false
        } ?: false
        updateUI()
    }

    private fun updateUI() = lifecycleScope.launch(Dispatchers.Main) {
        binding.contentMainLayout.customButton.buttonState = if (isNetworkConnected) {
            ButtonState.Completed
        } else {
            ButtonState.NetworkUnavailable
        }
    }

}
