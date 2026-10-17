package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.database.UserManager

class SchedulePickupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_pickup)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        val addressInput = findViewById<EditText>(R.id.addressInput)
        val userProfile = UserManager.getUserProfile(this)
        if (userProfile.address.isNotEmpty()) {
            addressInput?.setText(userProfile.address)
        }

        findViewById<View>(R.id.submitButton)?.setOnClickListener {
            val address = addressInput?.text?.toString()?.trim()
            if (address.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a pickup address", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Pickup Request Submitted Successfully!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MyPickupsActivity::class.java))
                finish()
            }
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
