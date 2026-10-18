package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.repositories.RewardsRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.firebase.auth.FirebaseAuth

class RewardsActivity : AppCompatActivity() {

    private val rewardsRepository = RewardsRepository()
    private var points = 1200
    private lateinit var pointsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        pointsText = findViewById(R.id.pointsBalanceText)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        val redeemBtn1 = findViewById<Button>(R.id.redeemButton1)
        val redeemBtn2 = findViewById<Button>(R.id.redeemButton2)
        val redeemBtn3 = findViewById<Button>(R.id.redeemButton3)

        redeemBtn1?.setSingleClickListener {
            redeem(500, "$5 Coffee Shop Voucher", redeemBtn1)
        }
        redeemBtn2?.setSingleClickListener {
            redeem(800, "10% Off Eco Grocery", redeemBtn2)
        }
        redeemBtn3?.setSingleClickListener {
            redeem(1000, "Plant a Tree", redeemBtn3)
        }

        loadRewardAccount()
        setupBottomNav()
    }

    private fun loadRewardAccount() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(this).uid

        if (currentUid.isEmpty()) return

        rewardsRepository.fetchRewardAccount(currentUid) { result ->
            when (result) {
                is ResultState.Success -> {
                    val account = result.data
                    if (account.pointsBalance > 0) {
                        points = account.pointsBalance
                        pointsText.text = "$points pts"
                    }
                }
                else -> {}
            }
        }
    }

    private fun redeem(cost: Int, voucherName: String, btn: Button) {
        if (points < cost) {
            Toast.makeText(this, "Insufficient points balance!", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(this).uid

        // Lock button to prevent double redemption
        btn.isEnabled = false
        val originalText = btn.text.toString()
        btn.text = "Redeeming..."

        val log = RedemptionLog(
            userId = currentUid,
            pointsUsed = cost,
            voucherCode = voucherName,
            status = "Redeemed",
            redeemedAt = System.currentTimeMillis()
        )

        rewardsRepository.redeemReward(log) { result ->
            when (result) {
                is ResultState.Success -> {
                    points -= cost
                    pointsText.text = "$points pts"
                    btn.isEnabled = false
                    btn.text = "Redeemed"
                    Toast.makeText(this, "Successfully redeemed $voucherName!", Toast.LENGTH_SHORT).show()
                }
                is ResultState.Error -> {
                    // Unlock button on failure
                    btn.isEnabled = true
                    btn.text = originalText
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                is ResultState.Loading -> {
                    btn.isEnabled = false
                }
            }
        }
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
}
