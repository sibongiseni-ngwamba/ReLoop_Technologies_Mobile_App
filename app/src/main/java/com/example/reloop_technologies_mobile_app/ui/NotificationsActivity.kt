package com.example.reloop_technologies_mobile_app.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.repositories.NotificationRepository
import com.example.reloop_technologies_mobile_app.backend.services.SupabaseAuthService
import com.example.reloop_technologies_mobile_app.ui.adapters.NotificationAdapter

class NotificationsActivity : AppCompatActivity() {

    private val notificationRepository = NotificationRepository()
    private val authService = SupabaseAuthService()
    private lateinit var adapter: NotificationAdapter
    private var notificationList = mutableListOf<NotificationModel>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        findViewById<View>(R.id.backButton)?.setOnClickListener { finish() }

        recyclerView = findViewById(R.id.notificationsRecyclerView)
        progressBar = findViewById(R.id.loadingProgressBar)
        emptyStateView = findViewById(R.id.emptyState)

        setupRecyclerView()
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

    private fun loadNotifications() {
        val currentUid = authService.currentUser?.uid.orEmpty()

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
                        showEmptyState()
                    } else {
                        showNotificationsList()
                    }
                }
                is ResultState.Error -> {
                    progressBar.visibility = View.GONE
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
        val currentUid = authService.currentUser?.uid.orEmpty()

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
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
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

}
