package com.example.reloop_technologies_mobile_app.backend.api

import android.os.Handler
import android.os.Looper
import com.example.reloop_technologies_mobile_app.backend.BackendConfig
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.GuidanceContent
import com.example.reloop_technologies_mobile_app.backend.models.NotificationModel
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.models.UserModel
import com.example.reloop_technologies_mobile_app.backend.models.WasteCategory
import com.example.reloop_technologies_mobile_app.backend.models.WasteLog
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreSeeder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.Executors

class RestApiClient {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val baseUrl: String
        get() = BackendConfig.apiBaseUrl

    // Local Data Cache backed by Seeded Data (At least 10 records per table)
    private val usersMap = RestDataSeed.seededUsers.associateBy { it.userId }.toMutableMap()
    private val pickupsMap = RestDataSeed.seededPickups.associateBy { it.pickupId }.toMutableMap()
    private val rewardItemsMap = RestDataSeed.seededRewardItems.associateBy { it.rewardItemId }.toMutableMap()
    private val transactionsList = RestDataSeed.seededTransactions.toMutableList()
    private val redemptionLogsList = RestDataSeed.seededRedemptionLogs.toMutableList()
    private val wasteLogsList = RestDataSeed.seededWasteLogs.toMutableList()
    private val wasteCategoriesMap = RestDataSeed.seededWasteCategories.associateBy { it.wasteCategoryId }.toMutableMap()
    private val notificationsList = RestDataSeed.seededNotifications.toMutableList()
    private val guidanceList = RestDataSeed.seededGuidanceContent.toMutableList()
    private val rewardAccountsMap = RestDataSeed.seededAccounts.toMutableMap()

    init {
        // Automatically sync all seeded records to Firebase Firestore on initialization
        FirestoreSeeder.seedAllCollections()
    }

    private fun <T> executeAsync(block: () -> ResultState<T>, onResult: (ResultState<T>) -> Unit) {
        onResult(ResultState.Loading)
        executor.execute {
            try {
                Thread.sleep(15)
                val result = block()
                mainHandler.post { onResult(result) }
            } catch (e: Exception) {
                mainHandler.post { onResult(ResultState.Error(exception = e, message = e.localizedMessage ?: "REST API request failed")) }
            }
        }
    }

    // --- USER ENDPOINTS ---
    fun getUserProfile(userId: String, onResult: (ResultState<UserModel>) -> Unit) {
        executeAsync({
            val user = usersMap[userId] ?: UserModel(
                userId = userId,
                firstName = "Eco",
                lastName = "User",
                email = "user@reloop.app",
                role = "resident"
            )
            // Push to Firebase Firestore
            db.collection("users").document(userId).set(user, SetOptions.merge())
            ResultState.Success(user)
        }, onResult)
    }

    fun updateUserProfile(user: UserModel, onResult: (ResultState<Boolean>) -> Unit) {
        executeAsync({
            usersMap[user.userId] = user
            db.collection("users").document(user.userId).set(user, SetOptions.merge())
            ResultState.Success(true)
        }, onResult)
    }

    // --- PICKUP ENDPOINTS ---
    fun getPickupsForUser(userId: String, role: String = "resident", onResult: (ResultState<List<PickupModel>>) -> Unit) {
        executeAsync({
            val userPickups = if (role.equals("collector", ignoreCase = true) || role.equals("admin", ignoreCase = true)) {
                pickupsMap.values.toList()
            } else {
                pickupsMap.values.filter { it.userId == userId || it.userId == "user_001" }
            }
            ResultState.Success(userPickups.sortedByDescending { it.createdAt })
        }, onResult)
    }

    fun schedulePickup(pickup: PickupModel, onResult: (ResultState<String>) -> Unit) {
        executeAsync({
            val pickupId = if (pickup.pickupId.isNotEmpty()) pickup.pickupId else "pickup_${System.currentTimeMillis()}"
            val newPickup = pickup.copy(pickupId = pickupId)
            pickupsMap[pickupId] = newPickup
            db.collection("pickups").document(pickupId).set(newPickup, SetOptions.merge())
            ResultState.Success(pickupId)
        }, onResult)
    }

    fun completePickup(pickupId: String, userId: String = "", onResult: (ResultState<Boolean>) -> Unit) {
        executeAsync({
            val existing = pickupsMap[pickupId]
            if (existing != null) {
                val now = System.currentTimeMillis()
                val updated = existing.copy(
                    status = "Completed",
                    completedAt = now,
                    updatedAt = now,
                    userId = if (userId.isNotEmpty()) userId else existing.userId
                )
                pickupsMap[pickupId] = updated
                db.collection("pickups").document(pickupId).set(updated, SetOptions.merge())
                ResultState.Success(true)
            } else {
                ResultState.Error(message = "Pickup record not found on REST API server.")
            }
        }, onResult)
    }

    fun cancelPickup(pickupId: String, cancellationReason: String, onResult: (ResultState<Boolean>) -> Unit) {
        executeAsync({
            val existing = pickupsMap[pickupId]
            if (existing != null) {
                val now = System.currentTimeMillis()
                val updated = existing.copy(
                    status = "Cancelled",
                    cancelledAt = now,
                    cancellationReason = cancellationReason,
                    updatedAt = now
                )
                pickupsMap[pickupId] = updated
                db.collection("pickups").document(pickupId).set(updated, SetOptions.merge())
                ResultState.Success(true)
            } else {
                ResultState.Error(message = "Pickup record not found on REST API server.")
            }
        }, onResult)
    }

    // --- REWARDS ENDPOINTS ---
    fun getRewardAccount(userId: String, onResult: (ResultState<RewardAccount>) -> Unit) {
        executeAsync({
            val account = rewardAccountsMap[userId] ?: RewardAccount(
                userId = userId,
                pointsBalance = 800,
                totalEarned = 1300,
                totalRedeemed = 500
            )
            rewardAccountsMap[userId] = account
            db.collection("rewards").document(userId).set(account, SetOptions.merge())
            ResultState.Success(account)
        }, onResult)
    }

    fun saveRewardAccount(account: RewardAccount, onResult: (ResultState<Boolean>) -> Unit) {
        executeAsync({
            rewardAccountsMap[account.userId] = account
            db.collection("rewards").document(account.userId).set(account, SetOptions.merge())
            ResultState.Success(true)
        }, onResult)
    }

    fun getRewardItems(onResult: (ResultState<List<RewardItem>>) -> Unit) {
        executeAsync({
            ResultState.Success(rewardItemsMap.values.filter { it.isActive })
        }, onResult)
    }

    fun getPointsTransactions(userId: String, onResult: (ResultState<List<PointsTransaction>>) -> Unit) {
        executeAsync({
            val txList = transactionsList.filter { it.userId == userId || it.userId == "user_001" }
                .sortedByDescending { it.createdAt }
            ResultState.Success(txList)
        }, onResult)
    }

    fun addPointsTransaction(tx: PointsTransaction, onResult: (ResultState<String>) -> Unit) {
        executeAsync({
            val txId = if (tx.transactionId.isNotEmpty()) tx.transactionId else "tx_${System.currentTimeMillis()}"
            val newTx = tx.copy(transactionId = txId)
            transactionsList.add(0, newTx)
            db.collection("pointsTransactions").document(txId).set(newTx, SetOptions.merge())
            ResultState.Success(txId)
        }, onResult)
    }

    fun processRewardRedemption(
        userId: String,
        item: RewardItem,
        onResult: (ResultState<Pair<RewardAccount, RedemptionLog>>) -> Unit
    ) {
        executeAsync({
            val currentAcc = rewardAccountsMap[userId] ?: RewardAccount(userId = userId, pointsBalance = 800, totalEarned = 1300, totalRedeemed = 500)
            if (currentAcc.pointsBalance < item.pointsCost) {
                return@executeAsync ResultState.Error(message = "Insufficient points! You need ${item.pointsCost - currentAcc.pointsBalance} more points.")
            }

            val now = System.currentTimeMillis()
            val updatedAcc = currentAcc.copy(
                pointsBalance = currentAcc.pointsBalance - item.pointsCost,
                totalRedeemed = currentAcc.totalRedeemed + item.pointsCost,
                lastUpdated = now
            )
            rewardAccountsMap[userId] = updatedAcc
            db.collection("rewards").document(userId).set(updatedAcc, SetOptions.merge())

            val voucherCode = "RELOOP-" + (100000..999999).random()
            val redemptionLog = RedemptionLog(
                redemptionId = "red_${now}",
                userId = userId,
                rewardItemId = item.rewardItemId,
                pointsUsed = item.pointsCost,
                voucherCode = voucherCode,
                status = "Redeemed",
                redeemedAt = now,
                createdAt = now
            )
            redemptionLogsList.add(0, redemptionLog)
            db.collection("redemptionLogs").document(redemptionLog.redemptionId).set(redemptionLog, SetOptions.merge())

            val pointsTx = PointsTransaction(
                transactionId = "tx_red_${now}",
                userId = userId,
                transactionType = "redeem",
                points = -item.pointsCost,
                relatedId = redemptionLog.redemptionId,
                description = "Redeemed: ${item.itemName}",
                createdAt = now
            )
            transactionsList.add(0, pointsTx)
            db.collection("pointsTransactions").document(pointsTx.transactionId).set(pointsTx, SetOptions.merge())

            ResultState.Success(Pair(updatedAcc, redemptionLog))
        }, onResult)
    }

    // --- WASTE SCAN & LOG ENDPOINTS ---
    fun getWasteCategories(onResult: (ResultState<List<WasteCategory>>) -> Unit) {
        executeAsync({
            ResultState.Success(wasteCategoriesMap.values.toList())
        }, onResult)
    }

    fun getWasteLogsForUser(userId: String, onResult: (ResultState<List<WasteLog>>) -> Unit) {
        executeAsync({
            val logs = wasteLogsList.filter { it.userId == userId || it.userId == "user_001" }
                .sortedByDescending { it.loggedAt }
            ResultState.Success(logs)
        }, onResult)
    }

    fun saveWasteLog(wasteLog: WasteLog, onResult: (ResultState<String>) -> Unit) {
        executeAsync({
            val logId = if (wasteLog.wasteLogId.isNotEmpty()) wasteLog.wasteLogId else "log_${System.currentTimeMillis()}"
            val newLog = wasteLog.copy(wasteLogId = logId)
            wasteLogsList.add(0, newLog)
            db.collection("wasteLogs").document(logId).set(newLog, SetOptions.merge())
            ResultState.Success(logId)
        }, onResult)
    }

    // --- NOTIFICATION ENDPOINTS ---
    fun getNotificationsForUser(userId: String, onResult: (ResultState<List<NotificationModel>>) -> Unit) {
        executeAsync({
            val userNotifs = notificationsList.filter { it.userId == userId || it.userId == "user_001" }
                .sortedByDescending { it.sentAt }
            ResultState.Success(userNotifs)
        }, onResult)
    }

    fun markNotificationAsRead(notificationId: String, onResult: (ResultState<Boolean>) -> Unit) {
        executeAsync({
            val idx = notificationsList.indexOfFirst { it.notificationId == notificationId }
            if (idx != -1) {
                val updated = notificationsList[idx].copy(isRead = true)
                notificationsList[idx] = updated
                db.collection("notifications").document(notificationId).set(updated, SetOptions.merge())
            }
            ResultState.Success(true)
        }, onResult)
    }

    fun clearNotificationsForUser(userId: String, onResult: (ResultState<Boolean>) -> Unit) {
        executeAsync({
            notificationsList.removeAll { it.userId == userId || it.userId == "user_001" }
            ResultState.Success(true)
        }, onResult)
    }

    // --- GUIDANCE ENDPOINTS ---
    fun getGuidanceContent(onResult: (ResultState<List<GuidanceContent>>) -> Unit) {
        executeAsync({
            ResultState.Success(guidanceList)
        }, onResult)
    }

    companion object {
        val instance: RestApiClient by lazy { RestApiClient() }
    }
}
