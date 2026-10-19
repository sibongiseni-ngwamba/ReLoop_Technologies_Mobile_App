package com.example.reloop_technologies_mobile_app.ui.adapters

import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reloop_technologies_mobile_app.R
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel

class NotificationAdapter(
    private var notifications: List<NotificationModel> = emptyList(),
    private val onItemClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    fun updateNotifications(newNotifications: List<NotificationModel>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: View = itemView.findViewById(R.id.notificationCard)
        private val iconBg: FrameLayout = itemView.findViewById(R.id.iconBackground)
        private val icon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val titleView: TextView = itemView.findViewById(R.id.notificationTitle)
        private val messageView: TextView = itemView.findViewById(R.id.notificationMessage)
        private val timeView: TextView = itemView.findViewById(R.id.notificationTime)
        private val unreadDot: View = itemView.findViewById(R.id.unreadDot)

        fun bind(item: NotificationModel) {
            // Title logic
            val displayTitle = if (item.title.isNotEmpty()) {
                item.title
            } else {
                when (item.type) {
                    NotificationModel.TYPE_PICKUP_CONFIRMATION -> "Pickup Scheduled"
                    NotificationModel.TYPE_PICKUP_REMINDER -> "Pickup Reminder"
                    NotificationModel.TYPE_STATUS_CHANGE -> "Status Update"
                    NotificationModel.TYPE_REWARD -> "Points Earned!"
                    else -> "Notification"
                }
            }
            titleView.text = displayTitle
            messageView.text = item.message

            // Icon & Background styling
            when (item.type) {
                NotificationModel.TYPE_PICKUP_CONFIRMATION -> {
                    iconBg.setBackgroundResource(R.drawable.bg_circle_icon_blue)
                    icon.setImageResource(R.drawable.ic_calendar)
                }
                NotificationModel.TYPE_PICKUP_REMINDER -> {
                    iconBg.setBackgroundResource(R.drawable.bg_circle_icon_blue)
                    icon.setImageResource(R.drawable.ic_clock)
                }
                NotificationModel.TYPE_STATUS_CHANGE -> {
                    iconBg.setBackgroundResource(R.drawable.bg_circle_icon_green)
                    icon.setImageResource(R.drawable.ic_leaf)
                }
                NotificationModel.TYPE_REWARD -> {
                    iconBg.setBackgroundResource(R.drawable.bg_circle_icon_green)
                    icon.setImageResource(R.drawable.ic_star)
                }
                else -> {
                    iconBg.setBackgroundResource(R.drawable.bg_circle_icon_blue)
                    icon.setImageResource(R.drawable.ic_bell)
                }
            }

            // Unread state styling
            if (item.isRead) {
                unreadDot.visibility = View.GONE
                titleView.setTypeface(null, Typeface.NORMAL)
            } else {
                unreadDot.visibility = View.VISIBLE
                titleView.setTypeface(null, Typeface.BOLD)
            }

            // Relative Time calculation
            val timeMillis = if (item.createdAt > 0) item.createdAt else item.sentAt
            val relativeTime = if (timeMillis > 0) {
                DateUtils.getRelativeTimeSpanString(
                    timeMillis,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
            } else {
                "Just now"
            }
            timeView.text = relativeTime

            card.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
