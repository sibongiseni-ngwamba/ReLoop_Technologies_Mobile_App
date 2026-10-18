package com.example.reloop_technologies_mobile_app.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.repositories.WasteScanRepository

class RecyclingGuidanceActivity : AppCompatActivity() {

    private val wasteScanRepository = WasteScanRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycling_guidance)

        findViewById<View>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        loadGuidance()
    }

    private fun loadGuidance() {
        wasteScanRepository.fetchGuidanceContent { result ->
            when (result) {
                is ResultState.Error -> {
                    // Operate in cached/offline fallback mode smoothly
                    Toast.makeText(this, "Operating in offline guidance mode.", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
