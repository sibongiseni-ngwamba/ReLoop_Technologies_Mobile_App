package com.example.reloop_technologies_mobile_app.ui

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
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
    private var catalogueItems: List<RewardItem> = emptyList()
    private var currentFilter: String = "all" // "all", "earned", "redeemed"
    private var isProcessingRedemption: Boolean = false

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
        filterAllBtn.setOnClickListener { setFilter("all") }
        filterEarnedBtn.setOnClickListener { setFilter("earned") }
        filterRedeemedBtn.setOnClickListener { setFilter("redeemed") }

        // Voucher Redeem Actions
        redeemButton1.setOnClickListener {
            val item = getItemByPosition(0, "$5 Coffee Shop Voucher", 500)
            initiateRewardRedemption(item)
        }
        redeemButton2.setOnClickListener {
            val item = getItemByPosition(1, "10% Off Eco Grocery", 800)
            initiateRewardRedemption(item)
        }
        redeemButton3.setOnClickListener {
            val item = getItemByPosition(2, "Plant a Tree in Your Name", 1000)
            initiateRewardRedemption(item)
        }
        redeemButton4.setOnClickListener {
            val item = getItemByPosition(3, "$15 Eco Store Gift Card", 1500)
            initiateRewardRedemption(item)
        }

        setupBottomNav()
    }

    private fun getItemByPosition(index: Int, defaultName: String, defaultCost: Int): RewardItem {
        if (catalogueItems.size > index) {
            return catalogueItems[index]
        }
        return RewardItem(
            rewardItemId = "item_${index + 1}",
            itemName = defaultName,
            description = "Active reward item in catalogue",
            pointsCost = defaultCost,
            isActive = true,
            stockQuantity = 50
        )
    }

    private fun getUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid.orEmpty()
    }

    private fun loadRewardsData() {
        val userId = getUserId()
        if (userId.isEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        transactionsLoadingBar.visibility = View.VISIBLE

        // 1. Fetch Reward Catalogue Items
        rewardsRepository.fetchAvailableRewardItems(this) { result ->
            if (result is ResultState.Success) {
                catalogueItems = result.data
            }
        }

        // 2. Fetch Reward Account
        rewardsRepository.fetchRewardAccount(userId, this) { result ->
            when (result) {
                is ResultState.Success -> {
                    currentAccount = result.data
                    updateAccountUi(currentAccount)
                }
                else -> {}
            }
        }

        // 3. Fetch User Pickups & Sync Completed Pickups for Points
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
                    allTransactions = emptyList()
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

        // Update Catalogue Redeem Buttons state based on points balance & active/stock status
        val item1 = getItemByPosition(0, "$5 Coffee Shop Voucher", 500)
        val item2 = getItemByPosition(1, "10% Off Eco Grocery", 800)
        val item3 = getItemByPosition(2, "Plant a Tree in Your Name", 1000)
        val item4 = getItemByPosition(3, "$15 Eco Store Gift Card", 1500)

        updateButtonState(redeemButton1, item1, account.pointsBalance)
        updateButtonState(redeemButton2, item2, account.pointsBalance)
        updateButtonState(redeemButton3, item3, account.pointsBalance)
        updateButtonState(redeemButton4, item4, account.pointsBalance)
    }

    private fun updateButtonState(btn: Button, item: RewardItem, currentBalance: Int) {
        if (!item.isActive || (item.stockQuantity != null && item.stockQuantity <= 0)) {
            btn.isEnabled = false
            btn.text = "Out of Stock"
            btn.alpha = 0.5f
            return
        }

        if (currentBalance < item.pointsCost) {
            val needed = item.pointsCost - currentBalance
            btn.isEnabled = false
            btn.text = "Need $needed pts"
            btn.alpha = 0.6f
        } else {
            btn.isEnabled = !isProcessingRedemption
            btn.text = "Redeem"
            btn.alpha = if (isProcessingRedemption) 0.5f else 1.0f
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
            } catch (_: Exception) {
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

    private fun initiateRewardRedemption(item: RewardItem) {
        if (isProcessingRedemption) return

        // 1. Availability check
        if (!item.isActive || (item.stockQuantity != null && item.stockQuantity <= 0)) {
            Toast.makeText(this, "This reward is currently out of stock or unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Balance Validation
        if (currentAccount.pointsBalance < item.pointsCost) {
            val needed = item.pointsCost - currentAccount.pointsBalance
            AlertDialog.Builder(this)
                .setTitle("Insufficient Points")
                .setMessage("You need $needed more points to redeem ${item.itemName}.\n\nCurrent Balance: ${currentAccount.pointsBalance} pts\nRequired: ${item.pointsCost} pts")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // 3. Confirmation Dialog
        val numFormat = NumberFormat.getNumberInstance(Locale.US)
        val remainingAfter = currentAccount.pointsBalance - item.pointsCost

        AlertDialog.Builder(this)
            .setTitle("Confirm Reward Redemption")
            .setMessage("Are you sure you want to redeem \"${item.itemName}\" for ${item.pointsCost} pts?\n\n" +
                    "Current Balance: ${numFormat.format(currentAccount.pointsBalance)} pts\n" +
                    "Remaining Balance After: ${numFormat.format(remainingAfter)} pts")
            .setPositiveButton("Confirm & Redeem") { dialog, _ ->
                dialog.dismiss()
                executeRedemption(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeRedemption(item: RewardItem) {
        val userId = getUserId()
        isProcessingRedemption = true
        updateAccountUi(currentAccount)

        @Suppress("DEPRECATION")
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Processing redemption securely...")
            setCancelable(false)
            show()
        }

        rewardsRepository.processRewardRedemption(userId, item, this) { result ->
            progressDialog.dismiss()
            isProcessingRedemption = false

            when (result) {
                is ResultState.Success -> {
                    val updatedAccount = result.data.first
                    val log = result.data.second
                    currentAccount = updatedAccount
                    updateAccountUi(currentAccount)

                    // Reload transactions list
                    rewardsRepository.fetchPointsTransactions(userId, this) { txResult ->
                        if (txResult is ResultState.Success) {
                            allTransactions = txResult.data
                            renderTransactions(allTransactions)
                        }
                    }

                    // Display Voucher Confirmation Code Dialog
                    showVoucherConfirmationDialog(item, log)
                }
                is ResultState.Error -> {
                    updateAccountUi(currentAccount)
                    AlertDialog.Builder(this)
                        .setTitle("Redemption Failed")
                        .setMessage(result.message ?: "Failed to process reward redemption. Please try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                is ResultState.Loading -> {}
            }
        }
    }

    private fun showVoucherConfirmationDialog(item: RewardItem, log: RedemptionLog) {
        val voucherCode = log.voucherCode ?: "RELOOP-${(100000..999999).random()}"
        val numFormat = NumberFormat.getNumberInstance(Locale.US)

        val messageText = "Redemption Successful! 🎉\n\n" +
                "Voucher: ${item.itemName}\n" +
                "Voucher Code: $voucherCode\n\n" +
                "Remaining Balance: ${numFormat.format(currentAccount.pointsBalance)} pts\n\n" +
                "Use this voucher code at checkout to claim your reward."

        AlertDialog.Builder(this)
            .setTitle("Voucher Generated")
            .setMessage(messageText)
            .setPositiveButton("Copy Code") { dialog, _ ->
                copyToClipboard("Voucher Code", voucherCode)
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Voucher code copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNav() {
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
