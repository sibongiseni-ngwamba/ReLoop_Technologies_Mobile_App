package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        findViewById<View>(R.id.saveResultButton)?.setOnClickListener {
            Toast.makeText(this, "Result saved successfully! +10 pts added.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }

        // Bottom Nav
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.navScan)?.setOnClickListener {
            startActivity(Intent(this, ScanWasteActivity::class.java))
        }
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
