package com.loh.camerax

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.loh.camerax.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        binding.btnTakePhoto.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.btnTakeVideo.setOnClickListener {
            startActivity(Intent(this, VideoRecordActivity::class.java))

        }
    }
}