package com.example.reloop_technologies_mobile_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Header Bell
        findViewById<View>(R.id.bellButton)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Total Points Card
        findViewById<View>(R.id.pointsCard)?.setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }

        // Quick Actions
        findViewById<View>(R.id.scanButton)?.setOnClickListener {
            startActivity(Intent(this, ScanWasteActivity::class.java))
        }
        findViewById<View>(R.id.scheduleButton)?.setOnClickListener {
            startActivity(Intent(this, SchedulePickupActivity::class.java))
        }
        findViewById<TextView>(R.id.pickupsLink)?.setOnClickListener {
            startActivity(Intent(this, MyPickupsActivity::class.java))
        }

        // Bottom Nav
        findViewById<View>(R.id.navHome)?.setOnClickListener { /* Already on Home */ }
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
