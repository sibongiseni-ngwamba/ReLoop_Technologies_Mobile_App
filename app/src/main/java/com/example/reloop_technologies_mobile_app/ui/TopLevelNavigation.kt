package com.example.reloop_technologies_mobile_app.ui

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.reloop_technologies_mobile_app.R

private const val EXTRA_TOP_LEVEL_DESTINATION = "top_level_destination"

/** Keep Home beneath one active tab, even when switching from a detail screen. */
internal fun AppCompatActivity.bindTopLevelNavigation() {
    findViewById<View>(R.id.navHome)?.setOnClickListener { switchToTab(HomeActivity::class.java) }
    findViewById<View>(R.id.navScan)?.setOnClickListener { switchToTab(ScanWasteActivity::class.java) }
    findViewById<View>(R.id.navPickups)?.setOnClickListener { switchToTab(MyPickupsActivity::class.java) }
    findViewById<View>(R.id.navRewards)?.setOnClickListener { switchToTab(RewardsActivity::class.java) }
    findViewById<View>(R.id.navProfile)?.setOnClickListener { switchToTab(ProfileActivity::class.java) }
}

@Suppress("DEPRECATION")
internal fun AppCompatActivity.switchToTab(destination: Class<out AppCompatActivity>) {
    if (javaClass == destination || isFinishing) return

    if (this is HomeActivity) {
        startActivity(Intent(this, destination))
    } else {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_TOP_LEVEL_DESTINATION, destination.name)
        }
        startActivity(intent)
        finish()
    }
    overridePendingTransition(0, 0)
}

@Suppress("DEPRECATION")
internal fun HomeActivity.openRequestedTab(intent: Intent?) {
    val destination = when (intent?.getStringExtra(EXTRA_TOP_LEVEL_DESTINATION)) {
        ScanWasteActivity::class.java.name -> ScanWasteActivity::class.java
        MyPickupsActivity::class.java.name -> MyPickupsActivity::class.java
        RewardsActivity::class.java.name -> RewardsActivity::class.java
        ProfileActivity::class.java.name -> ProfileActivity::class.java
        else -> null
    } ?: return
    intent?.removeExtra(EXTRA_TOP_LEVEL_DESTINATION)
    startActivity(Intent(this, destination))
    overridePendingTransition(0, 0)
}
