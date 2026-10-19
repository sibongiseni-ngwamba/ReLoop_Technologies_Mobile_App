package com.example.reloop_technologies_mobile_app.backend.services

import android.util.Log
import com.example.reloop_technologies_mobile_app.backend.api.RestDataSeed
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreSeeder {
    private const val TAG = "FirestoreSeeder"
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun seedAllCollections(onComplete: ((Boolean) -> Unit)? = null) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        try {
            Log.d(TAG, "Starting Firebase Firestore database seeding...")

            // 1. Seed Users Collection
            RestDataSeed.seededUsers.forEach { user ->
                db.collection("users").document(user.userId).set(user, SetOptions.merge())
            }
            if (currentUid.isNotEmpty()) {
                val currentUserDoc = RestDataSeed.seededUsers.first().copy(
                    userId = currentUid,
                    email = FirebaseAuth.getInstance().currentUser?.email.orEmpty().ifEmpty { "user@reloop.app" },
                    firstName = FirebaseAuth.getInstance().currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Eco",
                    lastName = FirebaseAuth.getInstance().currentUser?.displayName?.split(" ")?.getOrNull(1) ?: "User"
                )
                db.collection("users").document(currentUid).set(currentUserDoc, SetOptions.merge())
            }

            // 2. Seed Pickups Collection
            RestDataSeed.seededPickups.forEach { pickup ->
                db.collection("pickups").document(pickup.pickupId).set(pickup, SetOptions.merge())
                if (currentUid.isNotEmpty() && pickup.userId == "user_001") {
                    val userPickup = pickup.copy(pickupId = "pickup_user_${pickup.pickupId}", userId = currentUid)
                    db.collection("pickups").document(userPickup.pickupId).set(userPickup, SetOptions.merge())
                }
            }

            // 3. Seed Reward Items Collection
            RestDataSeed.seededRewardItems.forEach { item ->
                db.collection("rewardItems").document(item.rewardItemId).set(item, SetOptions.merge())
            }

            // 4. Seed Points Transactions Collection
            RestDataSeed.seededTransactions.forEach { tx ->
                db.collection("pointsTransactions").document(tx.transactionId).set(tx, SetOptions.merge())
                if (currentUid.isNotEmpty() && tx.userId == "user_001") {
                    val userTx = tx.copy(transactionId = "tx_user_${tx.transactionId}", userId = currentUid)
                    db.collection("pointsTransactions").document(userTx.transactionId).set(userTx, SetOptions.merge())
                }
            }

            // 5. Seed Redemption Logs Collection
            RestDataSeed.seededRedemptionLogs.forEach { log ->
                db.collection("redemptionLogs").document(log.redemptionId).set(log, SetOptions.merge())
                if (currentUid.isNotEmpty() && log.userId == "user_001") {
                    val userLog = log.copy(redemptionId = "red_user_${log.redemptionId}", userId = currentUid)
                    db.collection("redemptionLogs").document(userLog.redemptionId).set(userLog, SetOptions.merge())
                }
            }

            // 6. Seed Waste Logs Collection
            RestDataSeed.seededWasteLogs.forEach { log ->
                db.collection("wasteLogs").document(log.wasteLogId).set(log, SetOptions.merge())
                if (currentUid.isNotEmpty() && log.userId == "user_001") {
                    val userLog = log.copy(wasteLogId = "log_user_${log.wasteLogId}", userId = currentUid)
                    db.collection("wasteLogs").document(userLog.wasteLogId).set(userLog, SetOptions.merge())
                }
            }

            // 7. Seed Waste Categories Collection
            RestDataSeed.seededWasteCategories.forEach { cat ->
                db.collection("wasteCategories").document(cat.wasteCategoryId).set(cat, SetOptions.merge())
            }

            // 8. Seed Notifications Collection
            RestDataSeed.seededNotifications.forEach { notif ->
                db.collection("notifications").document(notif.notificationId).set(notif, SetOptions.merge())
                if (currentUid.isNotEmpty() && notif.userId == "user_001") {
                    val userNotif = notif.copy(notificationId = "notif_user_${notif.notificationId}", userId = currentUid)
                    db.collection("notifications").document(userNotif.notificationId).set(userNotif, SetOptions.merge())
                }
            }

            // 9. Seed Guidance Content Collection
            RestDataSeed.seededGuidanceContent.forEach { guide ->
                db.collection("guidanceContent").document(guide.guidanceId).set(guide, SetOptions.merge())
            }

            // 10. Seed Rewards Accounts Collection
            RestDataSeed.seededAccounts.forEach { (userId, account) ->
                db.collection("rewards").document(userId).set(account, SetOptions.merge())
            }
            if (currentUid.isNotEmpty()) {
                val userAccount = RestDataSeed.seededAccounts["user_001"]?.copy(userId = currentUid)
                    ?: RewardAccount(userId = currentUid, pointsBalance = 800, totalEarned = 1300, totalRedeemed = 500)
                db.collection("rewards").document(currentUid).set(userAccount, SetOptions.merge())
            }

            Log.d(TAG, "Successfully seeded all 10 collections to Firebase Firestore!")
            onComplete?.invoke(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding Firestore data: ${e.message}", e)
            onComplete?.invoke(false)
        }
    }
}
