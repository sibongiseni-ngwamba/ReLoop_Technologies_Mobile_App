package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.repositories.PickupRepository
import com.example.reloop_technologies_mobile_app.backend.repositories.RewardsRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RewardsActivity : AppCompatActivity() {
    private val rewardsRepository = RewardsRepository()
    private val pickupRepository = PickupRepository()

    private var currentAccount: RewardAccount = RewardAccount()
    private var allTransactions: List<PointsTransaction> = emptyList()
    private var currentFilter: String = "all" // "all", "earned", "redeemed"

    private lateinit var pointsBalanceText: TextView
    private lateinit var levelBadgeText: TextView
    private lateinit var totalEarnedText: TextView
    private lateinit var totalRedeemedText: TextView
    private lateinit var levelProgressText: TextView
    private lateinit var levelProgressBar: ProgressBar

    private lateinit var filterAllBtn: TextView
    private lateinit var filterEarnedBtn: TextView
    private lateinit var filterRedeemedBtn: TextView

    private lateinit var transactionHistoryContainer: LinearLayout
    private lateinit var emptyTransactionText: TextView
    private lateinit var transactionsLoadingBar: ProgressBar

    private lateinit var redeemButton1: Button
    private lateinit var redeemButton2: Button
    private lateinit var redeemButton3: Button
    private lateinit var redeemButton4: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        bindViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadRewardsData()
    }

    private fun bindViews() {
        pointsBalanceText = findViewById(R.id.pointsBalanceText)
        levelBadgeText = findViewById(R.id.levelBadgeText)
        totalEarnedText = findViewById(R.id.totalEarnedText)
        totalRedeemedText = findViewById(R.id.totalRedeemedText)
        levelProgressText = findViewById(R.id.levelProgressText)
        levelProgressBar = findViewById(R.id.levelProgressBar)

        filterAllBtn = findViewById(R.id.filterAllBtn)
        filterEarnedBtn = findViewById(R.id.filterEarnedBtn)
        filterRedeemedBtn = findViewById(R.id.filterRedeemedBtn)

        transactionHistoryContainer = findViewById(R.id.transactionHistoryContainer)
        emptyTransactionText = findViewById(R.id.emptyTransactionText)
        transactionsLoadingBar = findViewById(R.id.transactionsLoadingBar)

        redeemButton1 = findViewById(R.id.redeemButton1)
        redeemButton2 = findViewById(R.id.redeemButton2)
        redeemButton3 = findViewById(R.id.redeemButton3)
        redeemButton4 = findViewById(R.id.redeemButton4)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        // Filter Tabs
        filterAllBtn.setOnClickListener {
            setFilter("all")
        }
        filterEarnedBtn.setOnClickListener {
            setFilter("earned")
        }
        filterRedeemedBtn.setOnClickListener {
            setFilter("redeemed")
        }

        // Voucher Redeem Actions
        redeemButton1.setOnClickListener {
            redeemVoucher(
                RewardItem(
                    rewardItemId = "item_1",
                    itemName = "$5 Coffee Shop Voucher",
                    description = "Valid at participating organic coffee outlets.",
                    pointsCost = 500
                )
            )
        }
        redeemButton2.setOnClickListener {
            redeemVoucher(
                RewardItem(
                    rewardItemId = "item_2",
                    itemName = "10% Off Eco Grocery",
                    description = "Discount code for sustainable food products.",
                    pointsCost = 800
                )
            )
        }
        redeemButton3.setOnClickListener {
            redeemVoucher(
                RewardItem(
                    rewardItemId = "item_3",
                    itemName = "Plant a Tree in Your Name",
                    description = "We will plant a native tree to offset carbon.",
                    pointsCost = 1000
                )
            )
        }
        redeemButton4.setOnClickListener {
            redeemVoucher(
                RewardItem(
                    rewardItemId = "item_4",
                    itemName = "$15 Eco Store Gift Card",
                    description = "Redeemable online at EcoMart.",
                    pointsCost = 1500
                )
            )
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
        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun getUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: UserManager.getUserProfile(this).uid
        return uid.ifEmpty { "default_user" }
    }

    private fun loadRewardsData() {
        val userId = getUserId()
        transactionsLoadingBar.visibility = View.VISIBLE

        // 1. Fetch Reward Account
        rewardsRepository.fetchRewardAccount(userId, this) { result ->
            when (result) {
                is ResultState.Success -> {
                    currentAccount = result.data
                    updateAccountUi(currentAccount)
                }
                else -> {}
            }
        }

        // 2. Fetch User Pickups & Sync Completed Pickups for Points
        pickupRepository.fetchPickupsForUser(userId) { result ->
            if (result is ResultState.Success) {
                val pickups = result.data
                rewardsRepository.syncCompletedPickupsAndAwardPoints(userId, pickups, this) { syncResult ->
                    if (syncResult is ResultState.Success) {
                        currentAccount = syncResult.data
                        updateAccountUi(currentAccount)
                        loadTransactions(userId)
                    } else {
                        loadTransactions(userId)
                    }
                }
            } else {
                loadTransactions(userId)
            }
        }
    }

    private fun loadTransactions(userId: String) {
        rewardsRepository.fetchPointsTransactions(userId, this) { result ->
            transactionsLoadingBar.visibility = View.GONE
            when (result) {
                is ResultState.Success -> {
                    allTransactions = result.data
                    renderTransactions(allTransactions)
                }
                is ResultState.Error -> {
                    allTransactions = rewardsRepository.createInitialDefaultTransactions(userId)
                    renderTransactions(allTransactions)
                }
                is ResultState.Loading -> {
                    transactionsLoadingBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateAccountUi(account: RewardAccount) {
        val numFormat = NumberFormat.getNumberInstance(Locale.US)
        pointsBalanceText.text = "${numFormat.format(account.pointsBalance)} pts"
        totalEarnedText.text = "${numFormat.format(account.totalEarned)} pts"
        totalRedeemedText.text = "${numFormat.format(account.totalRedeemed)} pts"

        // Calculate Eco Level
        val points = account.totalEarned
        val levelName: String
        val maxProgress: Int
        val currentProgress: Int

        when {
            points >= 3000 -> {
                levelName = "Level 4 Eco Legend"
                maxProgress = 5000
                currentProgress = points.coerceAtMost(5000)
            }
            points >= 2000 -> {
                levelName = "Level 3 Eco Master"
                maxProgress = 3000
                currentProgress = points
            }
            points >= 800 -> {
                levelName = "Level 2 Eco Champion"
                maxProgress = 2000
                currentProgress = points
            }
            else -> {
                levelName = "Level 1 Eco Starter"
                maxProgress = 800
                currentProgress = points
            }
        }

        levelBadgeText.text = levelName
        levelProgressText.text = "Progress ($currentProgress / $maxProgress pts)"
        levelProgressBar.max = maxProgress
        levelProgressBar.progress = currentProgress

        // Update button states based on points balance
        updateButtonState(redeemButton1, account.pointsBalance >= 500)
        updateButtonState(redeemButton2, account.pointsBalance >= 800)
        updateButtonState(redeemButton3, account.pointsBalance >= 1000)
        updateButtonState(redeemButton4, account.pointsBalance >= 1500)
    }

    private fun updateButtonState(btn: Button, isEnabled: Boolean) {
        btn.isEnabled = isEnabled
        if (isEnabled) {
            btn.alpha = 1.0f
        } else {
            btn.alpha = 0.5f
        }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter

        val activeBg = R.drawable.bg_green_button
        val inactiveBg = R.drawable.bg_input_box
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val grayColor = ContextCompat.getColor(this, R.color.text_secondary)

        filterAllBtn.setBackgroundResource(if (filter == "all") activeBg else inactiveBg)
        filterAllBtn.setTextColor(if (filter == "all") whiteColor else grayColor)
        filterAllBtn.setTypeface(null, if (filter == "all") Typeface.BOLD else Typeface.NORMAL)

        filterEarnedBtn.setBackgroundResource(if (filter == "earned") activeBg else inactiveBg)
        filterEarnedBtn.setTextColor(if (filter == "earned") whiteColor else grayColor)
        filterEarnedBtn.setTypeface(null, if (filter == "earned") Typeface.BOLD else Typeface.NORMAL)

        filterRedeemedBtn.setBackgroundResource(if (filter == "redeemed") activeBg else inactiveBg)
        filterRedeemedBtn.setTextColor(if (filter == "redeemed") whiteColor else grayColor)
        filterRedeemedBtn.setTypeface(null, if (filter == "redeemed") Typeface.BOLD else Typeface.NORMAL)

        renderTransactions(allTransactions)
    }

    private fun renderTransactions(transactions: List<PointsTransaction>) {
        transactionHistoryContainer.removeAllViews()

        val filtered = when (currentFilter) {
            "earned" -> transactions.filter { it.points > 0 || it.transactionType.equals("earn", ignoreCase = true) }
            "redeemed" -> transactions.filter { it.points < 0 || it.transactionType.equals("redeem", ignoreCase = true) }
            else -> transactions
        }

        if (filtered.isEmpty()) {
            emptyTransactionText.visibility = View.VISIBLE
            return
        } else {
            emptyTransactionText.visibility = View.GONE
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        for (tx in filtered) {
            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(10))
                }
                setBackgroundResource(R.drawable.bg_card)
                setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                gravity = Gravity.CENTER_VERTICAL
            }

            val isEarn = tx.points > 0 || tx.transactionType.equals("earn", ignoreCase = true)

            // Icon Frame
            val iconFrame = FrameLayout(this).apply {
                val size = dpToPx(40)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setBackgroundResource(if (isEarn) R.drawable.bg_circle_icon_green else R.drawable.bg_badge_cancelled)
            }

            val iconImage = ImageView(this).apply {
                val imgSize = dpToPx(20)
                layoutParams = FrameLayout.LayoutParams(imgSize, imgSize).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(if (isEarn) R.drawable.ic_leaf else R.drawable.ic_gift)
                contentDescription = tx.transactionType
            }
            iconFrame.addView(iconImage)
            itemView.addView(iconFrame)

            // Description and Date Column
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                ).apply {
                    setMargins(dpToPx(12), 0, dpToPx(8), 0)
                }
            }

            val descText = TextView(this).apply {
                text = tx.description.ifEmpty { "Points Activity" }
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                typeface = Typeface.DEFAULT_BOLD
            }

            val dateStr = try {
                dateFormat.format(Date(tx.createdAt))
            } catch (e: Exception) {
                ""
            }

            val dateText = TextView(this).apply {
                text = dateStr
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            }

            textCol.addView(descText)
            textCol.addView(dateText)
            itemView.addView(textCol)

            // Points Badge
            val pointsText = TextView(this).apply {
                val ptsFormatted = if (tx.points > 0) "+${tx.points}" else "${tx.points}"
                text = "$ptsFormatted pts"
                setTextColor(
                    if (isEarn)
                        ContextCompat.getColor(context, R.color.primary_green)
                    else
                        Color.parseColor("#DC2626")
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT_BOLD
            }
            itemView.addView(pointsText)

            transactionHistoryContainer.addView(itemView)
        }
    }

    private fun redeemVoucher(item: RewardItem) {
        val userId = getUserId()

        if (currentAccount.pointsBalance < item.pointsCost) {
            val missing = item.pointsCost - currentAccount.pointsBalance
            Toast.makeText(this, "Insufficient points! Need $missing more points.", Toast.LENGTH_SHORT).show()
            return
        }

        rewardsRepository.redeemReward(userId, item, this) { result ->
            when (result) {
                is ResultState.Success -> {
                    currentAccount = result.data
                    updateAccountUi(currentAccount)
                    Toast.makeText(this, "Successfully redeemed ${item.itemName}!", Toast.LENGTH_LONG).show()

                    // Reload transactions to include the new redemption activity
                    rewardsRepository.fetchPointsTransactions(userId, this) { txResult ->
                        if (txResult is ResultState.Success) {
                            allTransactions = txResult.data
                            renderTransactions(allTransactions)
                        }
                    }
                }
                is ResultState.Error -> {
                    Toast.makeText(this, result.message ?: "Redemption failed.", Toast.LENGTH_SHORT).show()
                }
                is ResultState.Loading -> {}
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
