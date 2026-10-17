package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R

class ScanWasteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_waste)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        findViewById<View>(R.id.captureButton)?.setOnClickListener {
            Toast.makeText(this, "Scanning item...", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, ResultActivity::class.java))
            }, 600)
        }

        findViewById<View>(R.id.tipsCard)?.setOnClickListener {
            startActivity(Intent(this, RecyclingGuidanceActivity::class.java))
        }

        // Bottom Nav
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.navScan)?.setOnClickListener { /* Already on Scan */ }
        findViewById<View>(R.id.navPickups)?.setOnClickListener {
            startActivity(Intent(this, MyPickupsActivity::class.java))
        }
        findViewById<View>(R.id.navRewards)?.setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }
        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
