package com.example.reloop_technologies_mobile_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        findViewById<View>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.clearAllButton)?.setOnClickListener {
            findViewById<View>(R.id.notificationsList)?.visibility = View.GONE
            findViewById<View>(R.id.emptyState)?.visibility = View.VISIBLE
        }
    }
}
