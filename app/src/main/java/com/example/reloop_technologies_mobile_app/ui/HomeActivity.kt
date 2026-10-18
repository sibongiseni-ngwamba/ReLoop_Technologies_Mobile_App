package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.repositories.NotificationRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.RewardsRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.UserRepository
import com.example.reloop_technologies_mobile_app.database.UserData
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    private val userRepository = UserRepository()
    private val rewardsRepository = RewardsRepository()
    private val pickupRepository = PickupRepository()
    private val notificationRepository = NotificationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restrict authenticated screen: Ensure user is signed in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null && !UserManager.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: UserManager.getUserProfile(this).uid

        // Local profile fallback first for instant display
        val localProfile = UserManager.getUserProfile(this)
        val greetingView = findViewById<TextView>(R.id.greetingText)
        val avatarView = findViewById<TextView>(R.id.avatarText)
        val pointsBalanceView = findViewById<TextView>(R.id.pointsBalanceText)

        if (localProfile.name.isNotEmpty()) {
            val firstName = localProfile.name.split(" ").first()
            greetingView?.text = "Hi, $firstName!"
            avatarView?.text = localProfile.name.first().uppercase()
        }

        if (userId.isNotEmpty()) {
            // Fetch User Profile from Firestore
            userRepository.fetchUserProfile(userId) { result ->
                when (result) {
                    is ResultState.Success -> {
                        val user = result.data
                        if (user.firstName.isNotEmpty()) {
                            val firstName = user.firstName
                            greetingView?.text = "Hi, $firstName!"
                            avatarView?.text = firstName.first().uppercase()
                            UserManager.saveUserProfile(
                                this,
                                UserData(
                                    uid = user.userId,
                                    name = user.fullName,
                                    email = user.email,
                                    phone = user.phoneNumber,
                                    address = user.address
                                )
                            )
                        }
                    }
                    else -> {}
                }
            }

            // Fetch Rewards Balance
            rewardsRepository.fetchRewardAccount(userId) { result ->
                when (result) {
                    is ResultState.Success -> {
                        val account = result.data
                        pointsBalanceView?.text = "${account.pointsBalance} pts"
                    }
                    else -> {}
                }
            }

            // Fetch Pickups & Recent Activity
            pickupRepository.fetchPickupsForUser(userId) { result ->
                when (result) {
                    is ResultState.Success -> {
                        val pickups = result.data
                        // Update recent activity / pickup display if needed
                    }
                    else -> {}
                }
            }

            // Fetch Notifications for Unread Badge Indicator
            notificationRepository.fetchNotificationsForUser(userId) { result ->
                if (result is ResultState.Success) {
                    val hasUnread = result.data.any { !it.isRead }
                    findViewById<View>(R.id.notificationBadgeDot)?.visibility =
                        if (hasUnread) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupClickListeners() {
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
