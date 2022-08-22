package com.udacity.details

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.R
import com.udacity.databinding.ActivityDetailBinding
import com.udacity.main.MainActivity
import com.udacity.models.DownloadStatus
import com.udacity.util.DOWNLOAD_STATUS_INTENT_EXTRA_KEY
import com.udacity.util.FILE_NAME_INTENT_EXTRA_KEY

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        setSupportActionBar(binding.toolbar)

        if (intent?.extras != null) {
            binding.contentDetailLayout.fileNameValueTv.text =
                intent.getStringExtra(FILE_NAME_INTENT_EXTRA_KEY)
            binding.contentDetailLayout.downloadStatusValueTv.text =
                (intent.getSerializableExtra(DOWNLOAD_STATUS_INTENT_EXTRA_KEY) as DownloadStatus).name
        }

        binding.contentDetailLayout.okButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

}
