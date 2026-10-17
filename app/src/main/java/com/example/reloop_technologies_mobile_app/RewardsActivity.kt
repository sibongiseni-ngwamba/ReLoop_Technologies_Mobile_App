package com.example.reloop_technologies_mobile_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RewardsActivity : AppCompatActivity() {
    private var points = 1200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        val pointsText = findViewById<TextView>(R.id.pointsBalanceText)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        fun redeem(cost: Int, voucherName: String, btn: Button?) {
            if (points >= cost) {
                points -= cost
                pointsText?.text = "$points pts"
                btn?.isEnabled = false
                btn?.text = "Redeemed"
                Toast.makeText(this, "Successfully redeemed $voucherName!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Insufficient points balance!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.redeemButton1)?.setOnClickListener { btn ->
            redeem(500, "$5 Coffee Shop Voucher", btn as Button)
        }
        findViewById<Button>(R.id.redeemButton2)?.setOnClickListener { btn ->
            redeem(800, "10% Off Eco Grocery", btn as Button)
        }
        findViewById<Button>(R.id.redeemButton3)?.setOnClickListener { btn ->
            redeem(1000, "Plant a Tree", btn as Button)
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
}
