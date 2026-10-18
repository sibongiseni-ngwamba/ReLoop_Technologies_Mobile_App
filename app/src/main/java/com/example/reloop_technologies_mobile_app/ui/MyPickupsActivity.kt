package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.RewardsRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPickupsActivity : AppCompatActivity() {

    private val pickupRepository = PickupRepository()
    private val rewardsRepository = RewardsRepository()

    private lateinit var upcomingContainer: LinearLayout
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_pickups)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        upcomingContainer = findViewById(R.id.upcomingContainer)
        historyContainer = findViewById(R.id.historyContainer)

        setupTabs()
        setupBottomNav()
        loadPickups()
    }

    private fun setupTabs() {
        val tabUpcoming = findViewById<View>(R.id.tabUpcoming)
        val tabHistory = findViewById<View>(R.id.tabHistory)
        val textUpcoming = findViewById<TextView>(R.id.textUpcoming)
        val textHistory = findViewById<TextView>(R.id.textHistory)
        val indicatorUpcoming = findViewById<View>(R.id.indicatorUpcoming)
        val indicatorHistory = findViewById<View>(R.id.indicatorHistory)

        val greenColor = Color.parseColor("#237A3B")
        val grayColor = Color.parseColor("#6B7280")

        tabUpcoming?.setOnClickListener {
            upcomingContainer.visibility = View.VISIBLE
            historyContainer.visibility = View.GONE

            textUpcoming?.setTextColor(greenColor)
            textUpcoming?.setTypeface(null, Typeface.BOLD)
            indicatorUpcoming?.setBackgroundColor(greenColor)

            textHistory?.setTextColor(grayColor)
            textHistory?.setTypeface(null, Typeface.NORMAL)
            indicatorHistory?.setBackgroundColor(Color.TRANSPARENT)
        }

        tabHistory?.setOnClickListener {
            upcomingContainer.visibility = View.GONE
            historyContainer.visibility = View.VISIBLE

            textHistory?.setTextColor(greenColor)
            textHistory?.setTypeface(null, Typeface.BOLD)
            indicatorHistory?.setBackgroundColor(greenColor)

            textUpcoming?.setTextColor(grayColor)
            textUpcoming?.setTypeface(null, Typeface.NORMAL)
            indicatorUpcoming?.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun loadPickups() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(this).uid

        if (currentUid.isEmpty()) return

        pickupRepository.fetchPickupsForUser(currentUid) { result ->
            when (result) {
                is ResultState.Loading -> {
                    // Show subtle loading or retain current cache view
                }
                is ResultState.Success -> {
                    val pickups = result.data
                    if (pickups.isNotEmpty()) {
                        renderPickupsList(pickups)
                    }
                }
                is ResultState.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderPickupsList(pickups: List<PickupModel>) {
        val upcomingList = pickups.filter { it.status == "Scheduled" || it.status == "In Progress" }
            .sortedBy { it.scheduledDate }
        val historyList = pickups.filter { it.status == "Completed" || it.status == "Cancelled" }
            .sortedByDescending { it.scheduledDate }

        if (upcomingList.isNotEmpty()) {
            upcomingContainer.removeAllViews()
            upcomingList.forEach { pickup ->
                val cardView = createPickupCardView(pickup)
                upcomingContainer.addView(cardView)
            }
        }

        if (historyList.isNotEmpty()) {
            historyContainer.removeAllViews()
            historyList.forEach { pickup ->
                val cardView = createPickupCardView(pickup)
                historyContainer.addView(cardView)
            }
        }
    }

    private fun createPickupCardView(pickup: PickupModel): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            val paddingPx = (16 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }

        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateStr = sdfDate.format(Date(pickup.scheduledDate))

        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = sdfTime.format(Date(pickup.scheduledDate))

        // Row 1: Date & Status Badge
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val dateTv = TextView(this).apply {
            text = dateStr
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val badgeTv = TextView(this).apply {
            text = pickup.status
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            val hPadding = (10 * resources.displayMetrics.density).toInt()
            val vPadding = (4 * resources.displayMetrics.density).toInt()
            setPadding(hPadding, vPadding, hPadding, vPadding)

            when (pickup.status) {
                "Scheduled" -> {
                    setBackgroundResource(R.drawable.bg_badge_scheduled)
                    setTextColor(ContextCompat.getColor(context, R.color.badge_scheduled_text))
                }
                "In Progress" -> {
                    setBackgroundResource(R.drawable.bg_badge_in_progress)
                    setTextColor(ContextCompat.getColor(context, R.color.badge_in_progress_text))
                }
                "Completed" -> {
                    setBackgroundResource(R.drawable.bg_badge_completed)
                    setTextColor(ContextCompat.getColor(context, R.color.badge_completed_text))
                }
                else -> {
                    setBackgroundResource(R.drawable.bg_badge_cancelled)
                    setTextColor(ContextCompat.getColor(context, R.color.badge_cancelled_text))
                }
            }
        }

        row1.addView(dateTv)
        row1.addView(badgeTv)

        // Row 2: Time
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = (10 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }

        val timeTv = TextView(this).apply {
            text = "Scheduled: $timeStr"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        row2.addView(timeTv)

        // Row 3: Waste Type & Address
        val row3 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = (6 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }

        val wasteTv = TextView(this).apply {
            text = "Waste: ${pickup.wasteType.ifEmpty { "Recyclables" }}"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        val addressTv = TextView(this).apply {
            text = "Address: ${pickup.address}"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = (2 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }

        row3.addView(wasteTv)
        row3.addView(addressTv)

        cardLayout.addView(row1)
        cardLayout.addView(row2)
        cardLayout.addView(row3)

        return cardLayout
    }

    private fun setupBottomNav() {
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
