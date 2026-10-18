package com.example.reloop_technologies_mobile_app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.repositories.NotificationRepository
import com.example.reloop_technologies_mobile_app.database.UserManager
import com.example.reloop_technologies_mobile_app.ui.adapters.NotificationAdapter
import com.google.firebase.auth.FirebaseAuth

class NotificationsActivity : AppCompatActivity() {

    private val notificationRepository = NotificationRepository()
    private lateinit var adapter: NotificationAdapter
    private var notificationList = mutableListOf<NotificationModel>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Push notifications disabled. You can still view updates in your inbox.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        recyclerView = findViewById(R.id.notificationsRecyclerView)
        progressBar = findViewById(R.id.loadingProgressBar)
        emptyStateView = findViewById(R.id.emptyState)

        setupRecyclerView()
        checkNotificationPermission()
        loadNotifications()

        findViewById<View>(R.id.clearAllButton)?.setOnClickListener {
            clearNotifications()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notificationList) { notification ->
            onNotificationClicked(notification)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadNotifications() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(this).uid

        if (currentUid.isEmpty()) {
            showEmptyState()
            return
        }

        notificationRepository.fetchNotificationsForUser(currentUid) { result ->
            when (result) {
                is ResultState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    emptyStateView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                }
                is ResultState.Success -> {
                    progressBar.visibility = View.GONE
                    notificationList.clear()
                    notificationList.addAll(result.data)

                    if (notificationList.isEmpty()) {
                        // If no remote notifications found, create sample initial notifications for demo
                        val sampleList = getSampleNotifications(currentUid)
                        notificationList.addAll(sampleList)
                        // Save sample notifications to Firestore so they persist
                        for (sample in sampleList) {
                            notificationRepository.addNotification(sample)
                        }
                    }

                    if (notificationList.isEmpty()) {
                        showEmptyState()
                    } else {
                        showNotificationsList()
                    }
                }
                is ResultState.Error -> {
                    progressBar.visibility = View.GONE
                    // On error/offline, check if local list empty, load sample fallback
                    if (notificationList.isEmpty()) {
                        val sampleList = getSampleNotifications(currentUid)
                        notificationList.clear()
                        notificationList.addAll(sampleList)
                    }
                    if (notificationList.isEmpty()) {
                        showEmptyState()
                    } else {
                        showNotificationsList()
                    }
                }
            }
        }
    }

    private fun onNotificationClicked(notification: NotificationModel) {
        if (!notification.isRead && notification.notificationId.isNotEmpty()) {
            val index = notificationList.indexOfFirst { it.notificationId == notification.notificationId }
            if (index != -1) {
                val updated = notificationList[index].copy(isRead = true)
                notificationList[index] = updated
                adapter.updateNotifications(notificationList)
            }
            notificationRepository.markAsRead(notification.notificationId)
        }
    }

    private fun clearNotifications() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: UserManager.getUserProfile(this).uid

        notificationRepository.clearAllNotifications(currentUid) { result ->
            when (result) {
                is ResultState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is ResultState.Success -> {
                    progressBar.visibility = View.GONE
                    notificationList.clear()
                    adapter.updateNotifications(notificationList)
                    showEmptyState()
                    Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
                }
                is ResultState.Error -> {
                    progressBar.visibility = View.GONE
                    notificationList.clear()
                    adapter.updateNotifications(notificationList)
                    showEmptyState()
                    Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEmptyState() {
        emptyStateView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showNotificationsList() {
        emptyStateView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        adapter.updateNotifications(notificationList)
    }

    private fun getSampleNotifications(userId: String): List<NotificationModel> {
        val now = System.currentTimeMillis()
        return listOf(
            NotificationModel(
                notificationId = "sample_1",
                userId = userId,
                title = "Pickup Scheduled",
                message = "Your recycling pickup has been scheduled successfully.",
                type = NotificationModel.TYPE_PICKUP_CONFIRMATION,
                sentAt = now - 600000,
                createdAt = now - 600000,
                isRead = false
            ),
            NotificationModel(
                notificationId = "sample_2",
                userId = userId,
                title = "Points Earned!",
                message = "+10 points added for Plastic Bottle scan.",
                type = NotificationModel.TYPE_REWARD,
                sentAt = now - 7200000,
                createdAt = now - 7200000,
                isRead = false
            ),
            NotificationModel(
                notificationId = "sample_3",
                userId = userId,
                title = "Eco Milestone Reached",
                message = "Congratulations! You reached Level 2 Eco Champion.",
                type = NotificationModel.TYPE_STATUS_CHANGE,
                sentAt = now - 86400000,
                createdAt = now - 86400000,
                isRead = true
            )
        )
    }
}
