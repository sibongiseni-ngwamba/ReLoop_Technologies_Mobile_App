package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.RewardsRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth

class MyPickupsActivity : AppCompatActivity() {
    private val pickupRepository = PickupRepository()
    private val rewardsRepository = RewardsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_pickups)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        val tabUpcoming = findViewById<View>(R.id.tabUpcoming)
        val tabHistory = findViewById<View>(R.id.tabHistory)
        val textUpcoming = findViewById<TextView>(R.id.textUpcoming)
        val textHistory = findViewById<TextView>(R.id.textHistory)
        val indicatorUpcoming = findViewById<View>(R.id.indicatorUpcoming)
        val indicatorHistory = findViewById<View>(R.id.indicatorHistory)

        val upcomingContainer = findViewById<View>(R.id.upcomingContainer)
        val historyContainer = findViewById<View>(R.id.historyContainer)

        val greenColor = Color.parseColor("#237A3B")
        val grayColor = Color.parseColor("#6B7280")

        tabUpcoming?.setOnClickListener {
            upcomingContainer?.visibility = View.VISIBLE
            historyContainer?.visibility = View.GONE

            textUpcoming?.setTextColor(greenColor)
            textUpcoming?.setTypeface(null, Typeface.BOLD)
            indicatorUpcoming?.setBackgroundColor(greenColor)

            textHistory?.setTextColor(grayColor)
            textHistory?.setTypeface(null, Typeface.NORMAL)
            indicatorHistory?.setBackgroundColor(Color.TRANSPARENT)
        }

        tabHistory?.setOnClickListener {
            upcomingContainer?.visibility = View.GONE
            historyContainer?.visibility = View.VISIBLE

            textHistory?.setTextColor(greenColor)
            textHistory?.setTypeface(null, Typeface.BOLD)
            indicatorHistory?.setBackgroundColor(greenColor)

            textUpcoming?.setTextColor(grayColor)
            textUpcoming?.setTypeface(null, Typeface.NORMAL)
            indicatorUpcoming?.setBackgroundColor(Color.TRANSPARENT)
        }

        // Bottom Nav
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.navScan)?.setOnClickListener {
            startActivity(Intent(this, ScanWasteActivity::class.java))
        }
        findViewById<View>(R.id.navPickups)?.setOnClickListener { /* Already on Pickups */ }
        findViewById<View>(R.id.navRewards)?.setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }
        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        syncUserPickupsAndPoints()
    }

    private fun syncUserPickupsAndPoints() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: UserManager.getUserProfile(this).uid
        if (userId.isEmpty()) return

        pickupRepository.fetchPickupsForUser(userId) { result ->
            if (result is ResultState.Success) {
                val pickups = result.data
                rewardsRepository.syncCompletedPickupsAndAwardPoints(userId, pickups, this) {}
            }
        }
    }
}
