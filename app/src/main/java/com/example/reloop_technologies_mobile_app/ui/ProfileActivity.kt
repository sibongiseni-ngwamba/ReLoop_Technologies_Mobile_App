package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.utils.setSingleClickListener
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val userProfile = UserManager.getUserProfile(this)

        val avatarText = findViewById<TextView>(R.id.avatarText)
        val nameText = findViewById<TextView>(R.id.nameText)
        val emailText = findViewById<TextView>(R.id.emailText)

        if (userProfile.name.isNotEmpty()) {
            nameText?.text = userProfile.name
            avatarText?.text = userProfile.name.first().uppercase()
        }
        if (userProfile.email.isNotEmpty()) {
            emailText?.text = userProfile.email
        }

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        findViewById<View>(R.id.guidanceOption)?.setOnClickListener {
            startActivity(Intent(this, RecyclingGuidanceActivity::class.java))
        }

        findViewById<Button>(R.id.logoutButton)?.setSingleClickListener {
            FirebaseAuth.getInstance().signOut()
            UserManager.logout(this)
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
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
    }
}
