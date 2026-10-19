package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.UserRole
import com.example.reloop_technologies_mobile_app.backend.repositories.NotificationRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.RewardsRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.UserRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private val userRepository = UserRepository()
    private val rewardsRepository = RewardsRepository()
    private val pickupRepository = PickupRepository()
    private val notificationRepository = NotificationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restrict authenticated screen: Ensure user is signed in
        if (FirebaseAuth.getInstance().currentUser == null) {
            UserManager.logout(this)
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
        if (currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        val userId = currentUser.uid

        // Local profile fallback first for instant display
        val localProfile = UserManager.getUserProfile(this, userId)
        val greetingView = findViewById<TextView>(R.id.greetingText)
        val avatarView = findViewById<TextView>(R.id.avatarText)
        val pointsBalanceView = findViewById<TextView>(R.id.pointsBalanceText)

        val displayName = localProfile.name.ifEmpty { currentUser.displayName.orEmpty() }
        val firstName = displayName.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "there" }
        val localRole = UserRole.fromString(localProfile.role)
        greetingView.text = if (localRole.isResident) "Hi, $firstName!" else "Hi, $firstName (${localRole.displayName})"
        avatarView.text = displayName.firstOrNull()?.uppercase() ?: "?"
        pointsBalanceView.text = "${rewardsRepository.getLocalAccount(this, userId)?.pointsBalance ?: 0} pts"

        if (userId.isNotEmpty()) {
            // Fetch User Profile from Firestore
            userRepository.fetchUserProfile(userId, this) { result ->
                if (FirebaseAuth.getInstance().currentUser?.uid != userId || isFinishing) return@fetchUserProfile
                when (result) {
                    is ResultState.Success -> {
                        val user = result.data
                        if (user.firstName.isNotEmpty()) {
                            val firstName = user.firstName
                            greetingView?.text = if (user.userRole.isResident) "Hi, $firstName!" else "Hi, $firstName (${user.userRole.displayName})"
                            avatarView?.text = firstName.first().uppercase()
                        }
                    }
                    else -> {}
                }
            }

            // Fetch Rewards Balance
            rewardsRepository.fetchRewardAccount(userId, this) { result ->
                if (FirebaseAuth.getInstance().currentUser?.uid != userId || isFinishing) return@fetchRewardAccount
                when (result) {
                    is ResultState.Success -> {
                        val account = result.data
                        pointsBalanceView?.text = "${account.pointsBalance} pts"
                    }
                    else -> {}
                }
            }

            // Fetch Pickups & Recent Activity
            pickupRepository.fetchPickupsForUser(userId, localProfile.role) { result ->
                when (result) {
                    is ResultState.Success -> {
                        val pickups = result.data
                        if (FirebaseAuth.getInstance().currentUser?.uid == userId && !isFinishing) {
                            val container = findViewById<LinearLayout>(R.id.recentActivityContainer)
                            container.removeAllViews()
                            pickups.sortedByDescending { it.createdAt }.take(3).forEach { pickup ->
                                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(pickup.scheduledDate))
                                container.addView(TextView(this).apply {
                                    text = "${pickup.wasteType.ifEmpty { "Recycling pickup" }} · ${pickup.status} · $date"
                                    setTextColor(getColor(R.color.text_primary))
                                    textSize = 14f
                                    setPadding(16, 16, 16, 16)
                                    setBackgroundResource(R.drawable.bg_card)
                                    val margin = (8 * resources.displayMetrics.density).toInt()
                                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = margin }
                                })
                            }
                            if (pickups.isEmpty()) {
                                container.addView(TextView(this).apply { text = "No recent activity yet" })
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Fetch Notifications for Unread Badge Indicator
            notificationRepository.fetchNotificationsForUser(userId) { result ->
                if (FirebaseAuth.getInstance().currentUser?.uid != userId || isFinishing) return@fetchNotificationsForUser
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
