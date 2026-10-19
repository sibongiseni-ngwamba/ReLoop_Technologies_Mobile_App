package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.api.RestDataSeed
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class RewardsRepository(
    private val firestoreService: FirestoreService = FirestoreService(),
    private val restApiClient: RestApiClient = RestApiClient.instance
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

    fun fetchRewardAccount(
        userId: String,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        restApiClient.getRewardAccount(userId) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    val account = restResult.data
                    context?.let { saveAccountLocally(it, account) }
                    onResult(ResultState.Success(account))
                }
                else -> {
                    firestoreService.db.collection("rewards").document(userId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val account = try { snapshot.toObject(RewardAccount::class.java) } catch (_: RuntimeException) { null }
                            val finalAccount = if (!snapshot.exists()) RewardAccount(userId = userId, pointsBalance = 800, totalEarned = 1300, totalRedeemed = 500)
                            else (account ?: context?.let { getLocalAccount(it, userId) } ?: RewardAccount(userId = userId, pointsBalance = 800, totalEarned = 1300, totalRedeemed = 500)).copy(userId = userId)

                            context?.let { ctx -> saveAccountLocally(ctx, finalAccount) }
                            onResult(ResultState.Success(finalAccount))
                        }
                        .addOnFailureListener {
                            val cached = context?.let { ctx -> getLocalAccount(ctx, userId) }
                                ?: RewardAccount(userId = userId, pointsBalance = 800, totalEarned = 1300, totalRedeemed = 500)
                            onResult(ResultState.Success(cached))
                        }
                }
            }
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

        restApiClient.saveRewardAccount(account) { _ ->
            context?.let { ctx -> saveAccountLocally(ctx, account) }
            firestoreService.db.collection("rewards").document(account.userId).set(account)
            onResult(ResultState.Success(true))
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

        restApiClient.addPointsTransaction(tx) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    val docRef = if (tx.transactionId.isNotEmpty()) {
                        firestoreService.db.collection("pointsTransactions").document(tx.transactionId)
                    } else {
                        firestoreService.db.collection("pointsTransactions").document()
                    }
                    docRef.set(tx.copy(transactionId = docRef.id))
                    onResult(ResultState.Success(docRef.id))
                }
            }
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

        restApiClient.getPointsTransactions(userId) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    firestoreService.db.collection("pointsTransactions")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            try {
                                val list = snapshot.toObjects(PointsTransaction::class.java).sortedByDescending { it.createdAt }
                                onResult(ResultState.Success(list.ifEmpty { createInitialDefaultTransactions(userId) }))
                            } catch (_: RuntimeException) {
                                onResult(ResultState.Success(createInitialDefaultTransactions(userId)))
                            }
                        }
                        .addOnFailureListener {
                            onResult(ResultState.Success(createInitialDefaultTransactions(userId)))
                        }
                }
            }
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
            val existingTxList = if (txResult is ResultState.Success) txResult.data else emptyList()
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
                val currentAccount = if (accountResult is ResultState.Success) accountResult.data else RewardAccount(userId = userId, pointsBalance = 800, totalEarned = 1300, totalRedeemed = 500)

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
        restApiClient.getRewardItems { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    onResult(ResultState.Success(getDefaultRewardItems()))
                }
            }
        }
    }

    fun getDefaultRewardItems(): List<RewardItem> = RestDataSeed.seededRewardItems

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

        restApiClient.processRewardRedemption(userId, rewardItem) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    val (updatedAcc, log) = restResult.data
                    context?.let { saveAccountLocally(it, updatedAcc) }
                    onResult(ResultState.Success(Pair(updatedAcc, log)))
                }
                is ResultState.Error -> {
                    onResult(ResultState.Error(message = restResult.message))
                }
                else -> {}
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
}
