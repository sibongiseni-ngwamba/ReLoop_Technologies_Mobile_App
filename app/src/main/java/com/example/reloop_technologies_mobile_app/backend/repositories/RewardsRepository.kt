package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.models.*

class RewardsRepository(private val restApiClient: RestApiClient = RestApiClient.instance) {
    private val prefsName = "reloop_rewards_cache"
    private fun preferences(context: Context) = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getLocalAccount(context: Context, userId: String): RewardAccount? {
        val prefs = preferences(context)
        if (!prefs.contains("points_$userId")) return null
        return RewardAccount(
            userId = userId,
            pointsBalance = prefs.getInt("points_$userId", 0),
            totalEarned = prefs.getInt("earned_$userId", 0),
            totalRedeemed = prefs.getInt("redeemed_$userId", 0)
        )
    }

    fun saveAccountLocally(context: Context, account: RewardAccount) {
        preferences(context).edit()
            .putInt("points_${account.userId}", account.pointsBalance)
            .putInt("earned_${account.userId}", account.totalEarned)
            .putInt("redeemed_${account.userId}", account.totalRedeemed)
            .apply()
    }

    fun fetchRewardAccount(userId: String, context: Context? = null, onResult: (ResultState<RewardAccount>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.getRewardAccount(userId) { result ->
            if (result is ResultState.Success) context?.let { saveAccountLocally(it, result.data) }
            if (result is ResultState.Error && context != null) {
                val cached = getLocalAccount(context, userId)
                if (cached != null) return@getRewardAccount onResult(ResultState.Success(cached))
            }
            onResult(result)
        }
    }

    fun saveRewardAccount(account: RewardAccount, context: Context? = null, onResult: (ResultState<Boolean>) -> Unit = {}) {
        onResult(ResultState.Error(message = "Reward balances are updated only by verified points transactions."))
    }

    fun addPointsTransaction(tx: PointsTransaction, context: Context? = null, onResult: (ResultState<String>) -> Unit = {}) {
        if (tx.userId.isBlank() || tx.relatedId.isNullOrBlank()) return onResult(ResultState.Error(message = "A verified related record is required to award points."))
        restApiClient.addPointsTransaction(tx) { result ->
            if (result is ResultState.Success) fetchRewardAccount(tx.userId, context) { }
            onResult(result)
        }
    }

    fun fetchPointsTransactions(userId: String, context: Context? = null, onResult: (ResultState<List<PointsTransaction>>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.getPointsTransactions(userId, onResult)
    }

    fun createInitialDefaultTransactions(userId: String): List<PointsTransaction> = emptyList()

    fun syncCompletedPickupsAndAwardPoints(
        userId: String,
        pickups: List<PickupModel>,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        val completed = pickups.filter { it.status.equals("Completed", true) && it.pickupId.isNotBlank() }
        fun awardAt(index: Int) {
            if (index >= completed.size) {
                fetchRewardAccount(userId, context, onResult)
                return
            }
            val pickup = completed[index]
            addPointsTransaction(
                PointsTransaction(userId = userId, transactionType = "earn", points = 150, relatedId = pickup.pickupId, description = "Points Earned: Completed Pickup (${pickup.wasteType.ifEmpty { "Recyclables" }})")
            ) { result ->
                if (result is ResultState.Error) onResult(ResultState.Error(result.exception, result.message)) else awardAt(index + 1)
            }
        }
        if (userId.isBlank()) onResult(ResultState.Error(message = "User ID is empty.")) else awardAt(0)
    }

    fun fetchAvailableRewardItems(context: Context? = null, onResult: (ResultState<List<RewardItem>>) -> Unit) =
        restApiClient.getRewardItems(onResult)

    fun getDefaultRewardItems(): List<RewardItem> = emptyList()

    fun processRewardRedemption(
        userId: String,
        rewardItem: RewardItem,
        context: Context? = null,
        onResult: (ResultState<Pair<RewardAccount, RedemptionLog>>) -> Unit
    ) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.processRewardRedemption(userId, rewardItem) { result ->
            if (result is ResultState.Success) context?.let { saveAccountLocally(it, result.data.first) }
            onResult(result)
        }
    }

    fun redeemReward(userId: String, rewardItem: RewardItem, context: Context? = null, onResult: (ResultState<RewardAccount>) -> Unit) =
        processRewardRedemption(userId, rewardItem, context) { result ->
            when (result) {
                is ResultState.Success -> onResult(ResultState.Success(result.data.first))
                is ResultState.Error -> onResult(ResultState.Error(result.exception, result.message))
                ResultState.Loading -> onResult(ResultState.Loading)
            }
        }
}
