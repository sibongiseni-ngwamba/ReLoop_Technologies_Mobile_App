package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class RewardsRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    private val prefsName = "reloop_rewards_cache"

    private fun getSharedPreferences(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getLocalAccount(context: Context, userId: String): RewardAccount? {
        val prefs = getSharedPreferences(context)
        if (!prefs.contains("points_$userId")) return null
        val pts = prefs.getInt("points_$userId", 0)
        val earned = prefs.getInt("earned_$userId", 0)
        val redeemed = prefs.getInt("redeemed_$userId", 0)
        return RewardAccount(userId = userId, pointsBalance = pts, totalEarned = earned, totalRedeemed = redeemed)
    }

    fun saveAccountLocally(context: Context, account: RewardAccount) {
        getSharedPreferences(context).edit()
            .putInt("points_${account.userId}", account.pointsBalance)
            .putInt("earned_${account.userId}", account.totalEarned)
            .putInt("redeemed_${account.userId}", account.totalRedeemed)
            .apply()
    }

    fun getTransactionsLocally(context: Context, userId: String): List<PointsTransaction> {
        return emptyList()
    }

    fun saveTransactionsLocally(context: Context, userId: String, list: List<PointsTransaction>) {}

    fun getRedemptionLogsLocally(context: Context, userId: String): List<RedemptionLog> {
        return emptyList()
    }

    fun saveRedemptionLogsLocally(context: Context, userId: String, list: List<RedemptionLog>) {}

    fun fetchRewardAccount(
        userId: String,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        firestoreService.db.collection("rewards").document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val account = try { snapshot.toObject(RewardAccount::class.java) } catch (_: RuntimeException) { null }
                val finalAccount = if (!snapshot.exists()) RewardAccount(userId = userId)
                    else (account ?: context?.let { getLocalAccount(it, userId) } ?: RewardAccount(userId = userId)).copy(userId = userId)

                context?.let { ctx -> saveAccountLocally(ctx, finalAccount) }
                onResult(ResultState.Success(finalAccount))
            }
            .addOnFailureListener {
                val cached = context?.let { ctx -> getLocalAccount(ctx, userId) }
                    ?: RewardAccount(userId = userId)
                onResult(ResultState.Success(cached))
            }
    }

    fun saveRewardAccount(
        account: RewardAccount,
        context: Context? = null,
        onResult: (ResultState<Boolean>) -> Unit = {}
    ) {
        if (account.userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        firestoreService.db.collection("rewards").document(account.userId)
            .set(account)
            .addOnSuccessListener {
                context?.let { ctx -> saveAccountLocally(ctx, account) }
                onResult(ResultState.Success(true))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun addPointsTransaction(
        tx: PointsTransaction,
        context: Context? = null,
        onResult: (ResultState<String>) -> Unit = {}
    ) {
        if (tx.userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        val docRef = if (tx.transactionId.isNotEmpty()) {
            firestoreService.db.collection("pointsTransactions").document(tx.transactionId)
        } else {
            firestoreService.db.collection("pointsTransactions").document()
        }
        val newTx = tx.copy(transactionId = docRef.id)

        docRef.set(newTx)
            .addOnSuccessListener {
                onResult(ResultState.Success(docRef.id))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun fetchPointsTransactions(
        userId: String,
        context: Context? = null,
        onResult: (ResultState<List<PointsTransaction>>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        firestoreService.db.collection("pointsTransactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val sorted = snapshot.toObjects(PointsTransaction::class.java).sortedByDescending { it.createdAt }
                    onResult(ResultState.Success(sorted))
                } catch (exception: RuntimeException) {
                    onResult(ResultState.Error(exception = exception, message = "Could not read points history."))
                }
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun createInitialDefaultTransactions(userId: String): List<PointsTransaction> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        return listOf(
            PointsTransaction(
                transactionId = "tx_001",
                userId = userId,
                transactionType = "earn",
                points = 1000,
                description = "Welcome Eco Signup Bonus",
                createdAt = now - (10 * dayMs)
            ),
            PointsTransaction(
                transactionId = "tx_002",
                userId = userId,
                transactionType = "earn",
                points = 150,
                description = "Completed Pickup: Plastic Bottles & Metal Cans",
                createdAt = now - (5 * dayMs)
            ),
            PointsTransaction(
                transactionId = "tx_003",
                userId = userId,
                transactionType = "earn",
                points = 50,
                description = "AI Scan Verification Bonus",
                createdAt = now - (2 * dayMs)
            )
        )
    }

    fun syncCompletedPickupsAndAwardPoints(
        userId: String,
        pickups: List<PickupModel>,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        fetchPointsTransactions(userId, context) { txResult ->
            if (txResult !is ResultState.Success) {
                if (txResult is ResultState.Error) onResult(txResult)
                return@fetchPointsTransactions
            }
            val existingTxList = txResult.data
            val existingPickupIds = existingTxList.mapNotNull { it.relatedId }.toSet()

            var pointsToAdd = 0
            val newTransactions = mutableListOf<PointsTransaction>()

            val completedPickups = pickups.filter { it.status == "Completed" }

            for (pickup in completedPickups) {
                if (pickup.pickupId.isNotEmpty() && !existingPickupIds.contains(pickup.pickupId)) {
                    val awardAmount = 150
                    pointsToAdd += awardAmount

                    val wasteLabel = pickup.wasteType.ifEmpty { "Recyclables" }
                    val tx = PointsTransaction(
                        transactionId = "tx_pickup_${pickup.pickupId}",
                        userId = userId,
                        transactionType = "earn",
                        points = awardAmount,
                        relatedId = pickup.pickupId,
                        description = "Points Earned: Completed Pickup ($wasteLabel)",
                        createdAt = if (pickup.completedAt != null && pickup.completedAt > 0) pickup.completedAt else pickup.scheduledDate
                    )
                    newTransactions.add(tx)
                }
            }

            fetchRewardAccount(userId, context) { accountResult ->
                if (accountResult !is ResultState.Success) return@fetchRewardAccount
                val currentAccount = accountResult.data

                if (pointsToAdd > 0) {
                    val updatedAccount = currentAccount.copy(
                        pointsBalance = currentAccount.pointsBalance + pointsToAdd,
                        totalEarned = currentAccount.totalEarned + pointsToAdd,
                        lastUpdated = System.currentTimeMillis()
                    )
                    saveRewardAccount(updatedAccount, context)

                    for (tx in newTransactions) {
                        addPointsTransaction(tx, context) {}
                    }
                    onResult(ResultState.Success(updatedAccount))
                } else {
                    onResult(ResultState.Success(currentAccount))
                }
            }
        }
    }

    fun fetchAvailableRewardItems(
        context: Context? = null,
        onResult: (ResultState<List<RewardItem>>) -> Unit
    ) {
        onResult(ResultState.Loading)
        firestoreService.db.collection("rewardItems")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.toObjects(RewardItem::class.java)
                if (items.isEmpty()) {
                    onResult(ResultState.Success(getDefaultRewardItems()))
                } else {
                    onResult(ResultState.Success(items))
                }
            }
            .addOnFailureListener {
                onResult(ResultState.Success(getDefaultRewardItems()))
            }
    }

    fun getDefaultRewardItems(): List<RewardItem> {
        return listOf(
            RewardItem(
                rewardItemId = "item_1",
                itemName = "$5 Coffee Shop Voucher",
                description = "Valid at participating organic coffee outlets.",
                pointsCost = 500,
                isActive = true,
                stockQuantity = 50
            ),
            RewardItem(
                rewardItemId = "item_2",
                itemName = "10% Off Eco Grocery",
                description = "Discount code for sustainable food products.",
                pointsCost = 800,
                isActive = true,
                stockQuantity = 30
            ),
            RewardItem(
                rewardItemId = "item_3",
                itemName = "Plant a Tree in Your Name",
                description = "We will plant a native tree to offset carbon.",
                pointsCost = 1000,
                isActive = true,
                stockQuantity = 100
            ),
            RewardItem(
                rewardItemId = "item_4",
                itemName = "$15 Eco Store Gift Card",
                description = "Redeemable online at EcoMart.",
                pointsCost = 1500,
                isActive = true,
                stockQuantity = 20
            )
        )
    }

    fun processRewardRedemption(
        userId: String,
        rewardItem: RewardItem,
        context: Context? = null,
        onResult: (ResultState<Pair<RewardAccount, RedemptionLog>>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        fetchRewardAccount(userId, context) { accountResult ->
            if (accountResult !is ResultState.Success) {
                onResult(ResultState.Error(message = "Failed to retrieve user reward balance."))
                return@fetchRewardAccount
            }

            val account = accountResult.data

            if (!rewardItem.isActive) {
                onResult(ResultState.Error(message = "This reward item is currently inactive or unavailable."))
                return@fetchRewardAccount
            }

            if (rewardItem.stockQuantity != null && rewardItem.stockQuantity <= 0) {
                onResult(ResultState.Error(message = "This reward item is currently out of stock."))
                return@fetchRewardAccount
            }

            if (account.pointsBalance < rewardItem.pointsCost) {
                val needed = rewardItem.pointsCost - account.pointsBalance
                onResult(ResultState.Error(message = "Insufficient points balance! You need $needed more points."))
                return@fetchRewardAccount
            }

            val now = System.currentTimeMillis()
            val updatedAccount = account.copy(
                pointsBalance = account.pointsBalance - rewardItem.pointsCost,
                totalRedeemed = account.totalRedeemed + rewardItem.pointsCost,
                lastUpdated = now
            )

            val voucherCode = "RELOOP-" + (100000..999999).random()
            val logRef = firestoreService.db.collection("redemptionLogs").document()
            val redemptionLog = RedemptionLog(
                redemptionId = logRef.id,
                userId = userId,
                rewardItemId = rewardItem.rewardItemId,
                pointsUsed = rewardItem.pointsCost,
                voucherCode = voucherCode,
                status = "Redeemed",
                redeemedAt = now,
                createdAt = now
            )

            val txRef = firestoreService.db.collection("pointsTransactions").document()
            val pointsTx = PointsTransaction(
                transactionId = txRef.id,
                userId = userId,
                transactionType = "redeem",
                points = -rewardItem.pointsCost,
                relatedId = redemptionLog.redemptionId,
                description = "Redeemed: ${rewardItem.itemName}",
                createdAt = now
            )

            val batch = firestoreService.db.batch()
            batch.set(firestoreService.db.collection("rewards").document(userId), updatedAccount)
            batch.set(logRef, redemptionLog)
            batch.set(txRef, pointsTx)

            if (rewardItem.rewardItemId.isNotEmpty() && rewardItem.stockQuantity != null) {
                val itemRef = firestoreService.db.collection("rewardItems").document(rewardItem.rewardItemId)
                batch.update(itemRef, "stockQuantity", rewardItem.stockQuantity - 1)
            }

            batch.commit()
                .addOnSuccessListener {
                    context?.let { ctx -> saveAccountLocally(ctx, updatedAccount) }
                    onResult(ResultState.Success(Pair(updatedAccount, redemptionLog)))
                }
                .addOnFailureListener { exception ->
                    onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
                }
        }
    }

    fun redeemReward(
        userId: String,
        rewardItem: RewardItem,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        processRewardRedemption(userId, rewardItem, context) { res ->
            when (res) {
                is ResultState.Success -> onResult(ResultState.Success(res.data.first))
                is ResultState.Error -> onResult(ResultState.Error(message = res.message))
                is ResultState.Loading -> onResult(ResultState.Loading)
            }
        }
    }

    fun redeemReward(redemptionLog: RedemptionLog, onResult: (ResultState<String>) -> Unit) {
        onResult(ResultState.Loading)
        val docRef = firestoreService.db.collection("redemptionLogs").document()
        val log = redemptionLog.copy(redemptionId = docRef.id)

        docRef.set(log)
            .addOnSuccessListener {
                onResult(ResultState.Success(docRef.id))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
