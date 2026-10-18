package com.example.reloop_technologies_mobile_app.backend.repositories

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.PickupModel
import com.example.reloop_technologies_mobile_app.backend.models.PointsTransaction
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService
import org.json.JSONArray
import org.json.JSONObject

class RewardsRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    private companion object {
        const val PREF_NAME = "reloop_rewards_prefs"
        const val KEY_ACCOUNT_PREFIX = "reward_account_"
        const val KEY_TRANSACTIONS_PREFIX = "reward_transactions_"
        const val KEY_REDEMPTIONS_PREFIX = "reward_redemptions_"
    }

    private fun getSharedPreferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Local SharedPreferences Persistence Helpers
    fun saveAccountLocally(context: Context, account: RewardAccount) {
        if (account.userId.isEmpty()) return
        val obj = JSONObject().apply {
            put("userId", account.userId)
            put("pointsBalance", account.pointsBalance)
            put("totalEarned", account.totalEarned)
            put("totalRedeemed", account.totalRedeemed)
            put("lastUpdated", account.lastUpdated)
        }
        getSharedPreferences(context).edit()
            .putString(KEY_ACCOUNT_PREFIX + account.userId, obj.toString())
            .apply()
    }

    fun getAccountLocally(context: Context, userId: String): RewardAccount? {
        if (userId.isEmpty()) return null
        val jsonStr = getSharedPreferences(context).getString(KEY_ACCOUNT_PREFIX + userId, null)
            ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            RewardAccount(
                userId = obj.optString("userId", userId),
                pointsBalance = obj.optInt("pointsBalance", 1200),
                totalEarned = obj.optInt("totalEarned", 1200),
                totalRedeemed = obj.optInt("totalRedeemed", 0),
                lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveTransactionsLocally(context: Context, userId: String, transactions: List<PointsTransaction>) {
        if (userId.isEmpty()) return
        val array = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("transactionId", tx.transactionId)
                put("userId", tx.userId)
                put("transactionType", tx.transactionType)
                put("points", tx.points)
                put("relatedId", tx.relatedId ?: "")
                put("description", tx.description)
                put("createdAt", tx.createdAt)
            }
            array.put(obj)
        }
        getSharedPreferences(context).edit()
            .putString(KEY_TRANSACTIONS_PREFIX + userId, array.toString())
            .apply()
    }

    fun getTransactionsLocally(context: Context, userId: String): List<PointsTransaction> {
        if (userId.isEmpty()) return emptyList()
        val jsonStr = getSharedPreferences(context).getString(KEY_TRANSACTIONS_PREFIX + userId, null)
            ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<PointsTransaction>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PointsTransaction(
                        transactionId = obj.optString("transactionId"),
                        userId = obj.optString("userId", userId),
                        transactionType = obj.optString("transactionType", "earn"),
                        points = obj.optInt("points", 0),
                        relatedId = obj.optString("relatedId").ifEmpty { null },
                        description = obj.optString("description"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRedemptionLogsLocally(context: Context, userId: String, logs: List<RedemptionLog>) {
        if (userId.isEmpty()) return
        val array = JSONArray()
        for (log in logs) {
            val obj = JSONObject().apply {
                put("redemptionId", log.redemptionId)
                put("userId", log.userId)
                put("rewardItemId", log.rewardItemId)
                put("pointsUsed", log.pointsUsed)
                put("voucherCode", log.voucherCode ?: "")
                put("status", log.status)
                put("redeemedAt", log.redeemedAt)
                put("createdAt", log.createdAt)
            }
            array.put(obj)
        }
        getSharedPreferences(context).edit()
            .putString(KEY_REDEMPTIONS_PREFIX + userId, array.toString())
            .apply()
    }

    fun getRedemptionLogsLocally(context: Context, userId: String): List<RedemptionLog> {
        if (userId.isEmpty()) return emptyList()
        val jsonStr = getSharedPreferences(context).getString(KEY_REDEMPTIONS_PREFIX + userId, null)
            ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<RedemptionLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RedemptionLog(
                        redemptionId = obj.optString("redemptionId"),
                        userId = obj.optString("userId", userId),
                        rewardItemId = obj.optString("rewardItemId"),
                        pointsUsed = obj.optInt("pointsUsed", 0),
                        voucherCode = obj.optString("voucherCode").ifEmpty { null },
                        status = obj.optString("status", "Redeemed"),
                        redeemedAt = obj.optLong("redeemedAt", System.currentTimeMillis()),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
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

        val localAccount = context?.let { getAccountLocally(it, userId) }
        if (localAccount != null) {
            onResult(ResultState.Success(localAccount))
        } else {
            onResult(ResultState.Loading)
        }

        firestoreService.db.collection("rewards").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val account = doc.toObject(RewardAccount::class.java) ?: RewardAccount(userId = userId)
                    context?.let { saveAccountLocally(it, account) }
                    onResult(ResultState.Success(account))
                } else {
                    val defaultAccount = localAccount ?: RewardAccount(
                        userId = userId,
                        pointsBalance = 1200,
                        totalEarned = 1200,
                        totalRedeemed = 0
                    )
                    saveRewardAccount(defaultAccount, context) {}
                    onResult(ResultState.Success(defaultAccount))
                }
            }
            .addOnFailureListener {
                val account = localAccount ?: RewardAccount(userId = userId)
                onResult(ResultState.Success(account))
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
        context?.let { saveAccountLocally(it, account) }

        firestoreService.db.collection("rewards").document(account.userId).set(account)
            .addOnSuccessListener {
                onResult(ResultState.Success(true))
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

        val localTxList = context?.let { getTransactionsLocally(it, userId) } ?: emptyList()
        if (localTxList.isNotEmpty()) {
            onResult(ResultState.Success(localTxList.sortedByDescending { it.createdAt }))
        } else {
            onResult(ResultState.Loading)
        }

        firestoreService.db.collection("pointsTransactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                var transactions = snapshot.toObjects(PointsTransaction::class.java)
                if (transactions.isEmpty()) {
                    transactions = createInitialDefaultTransactions(userId)
                    transactions.forEach { tx ->
                        firestoreService.db.collection("pointsTransactions").document(tx.transactionId).set(tx)
                    }
                }
                val sorted = transactions.sortedByDescending { it.createdAt }
                context?.let { saveTransactionsLocally(it, userId, sorted) }
                onResult(ResultState.Success(sorted))
            }
            .addOnFailureListener {
                val fallback = if (localTxList.isNotEmpty()) localTxList else createInitialDefaultTransactions(userId)
                val sorted = fallback.sortedByDescending { it.createdAt }
                context?.let { saveTransactionsLocally(it, userId, sorted) }
                onResult(ResultState.Success(sorted))
            }
    }

    fun createInitialDefaultTransactions(userId: String): List<PointsTransaction> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        return listOf(
            PointsTransaction(
                transactionId = "tx_pickup_1",
                userId = userId,
                transactionType = "earn",
                points = 150,
                relatedId = "pickup_completed_1",
                description = "Points Earned: Completed Pickup - Glass Bottles, Metals",
                createdAt = now - (2 * dayMs)
            ),
            PointsTransaction(
                transactionId = "tx_scan_1",
                userId = userId,
                transactionType = "earn",
                points = 50,
                relatedId = "scan_1",
                description = "AI Waste Scan Bonus: Plastic Bottle",
                createdAt = now - (5 * dayMs)
            ),
            PointsTransaction(
                transactionId = "tx_welcome_1",
                userId = userId,
                transactionType = "earn",
                points = 1000,
                relatedId = "signup",
                description = "Welcome Eco Champion Bonus",
                createdAt = now - (10 * dayMs)
            )
        )
    }

    fun addPointsTransaction(
        transaction: PointsTransaction,
        context: Context? = null,
        onResult: (ResultState<String>) -> Unit = {}
    ) {
        val docRef = if (transaction.transactionId.isNotEmpty()) {
            firestoreService.db.collection("pointsTransactions").document(transaction.transactionId)
        } else {
            firestoreService.db.collection("pointsTransactions").document()
        }
        val newTx = transaction.copy(transactionId = docRef.id)

        context?.let {
            val currentList = getTransactionsLocally(it, newTx.userId).toMutableList()
            currentList.removeAll { item -> item.transactionId == newTx.transactionId }
            currentList.add(0, newTx)
            saveTransactionsLocally(it, newTx.userId, currentList)
        }

        docRef.set(newTx)
            .addOnSuccessListener {
                onResult(ResultState.Success(docRef.id))
            }
            .addOnFailureListener {
                onResult(ResultState.Success(docRef.id))
            }
    }

    fun syncCompletedPickupsAndAwardPoints(
        userId: String,
        pickups: List<PickupModel>,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        if (userId.isEmpty()) return

        fetchPointsTransactions(userId, context) { txResult ->
            val existingTxList = if (txResult is ResultState.Success) txResult.data else emptyList()
            val existingRelatedIds = existingTxList.mapNotNull { it.relatedId }.toSet()

            val completedPickups = pickups.filter {
                it.status.equals("Completed", ignoreCase = true)
            }

            var pointsToAdd = 0
            val newTransactions = mutableListOf<PointsTransaction>()

            for (pickup in completedPickups) {
                if (pickup.pickupId.isNotEmpty() && !existingRelatedIds.contains(pickup.pickupId)) {
                    val awardAmount = 150
                    pointsToAdd += awardAmount
                    val wasteLabel = if (pickup.wasteTypes.isNotEmpty()) {
                        pickup.wasteTypes.joinToString(", ")
                    } else if (pickup.wasteType.isNotEmpty()) {
                        pickup.wasteType
                    } else {
                        "Recyclables"
                    }
                    val tx = PointsTransaction(
                        transactionId = "tx_pickup_" + pickup.pickupId,
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
                val currentAccount = if (accountResult is ResultState.Success) {
                    accountResult.data
                } else {
                    RewardAccount(userId = userId)
                }

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
    fun fetchAvailableRewardItems(onResult: (ResultState<List<RewardItem>>) -> Unit) {
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
                isActive = true
            ),
            RewardItem(
                rewardItemId = "item_2",
                itemName = "10% Off Eco Grocery",
                description = "Discount code for sustainable food products.",
                pointsCost = 800,
                isActive = true
            ),
            RewardItem(
                rewardItemId = "item_3",
                itemName = "Plant a Tree in Your Name",
                description = "We will plant a native tree to offset carbon.",
                pointsCost = 1000,
                isActive = true
            ),
            RewardItem(
                rewardItemId = "item_4",
                itemName = "$15 Eco Store Gift Card",
                description = "Redeemable online at EcoMart.",
                pointsCost = 1500,
                isActive = true
            )
        )
    }

    fun redeemReward(
        userId: String,
        rewardItem: RewardItem,
        context: Context? = null,
        onResult: (ResultState<RewardAccount>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }

        fetchRewardAccount(userId, context) { accountResult ->
            if (accountResult !is ResultState.Success) {
                onResult(ResultState.Error(message = "Failed to retrieve reward account."))
                return@fetchRewardAccount
            }

            val account = accountResult.data
            if (account.pointsBalance < rewardItem.pointsCost) {
                onResult(ResultState.Error(message = "Insufficient points balance!"))
                return@fetchRewardAccount
            }

            val updatedAccount = account.copy(
                pointsBalance = account.pointsBalance - rewardItem.pointsCost,
                totalRedeemed = account.totalRedeemed + rewardItem.pointsCost,
                lastUpdated = System.currentTimeMillis()
            )

            // Save updated account
            saveRewardAccount(updatedAccount, context)

            // Create RedemptionLog
            val logRef = firestoreService.db.collection("redemptionLogs").document()
            val redemptionLog = RedemptionLog(
                redemptionId = logRef.id,
                userId = userId,
                rewardItemId = rewardItem.rewardItemId,
                pointsUsed = rewardItem.pointsCost,
                voucherCode = "RELOOP-" + (100000..999999).random(),
                status = "Redeemed",
                redeemedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            logRef.set(redemptionLog)

            // Create PointsTransaction
            val tx = PointsTransaction(
                userId = userId,
                transactionType = "redeem",
                points = -rewardItem.pointsCost,
                relatedId = redemptionLog.redemptionId,
                description = "Redeemed: ${rewardItem.itemName}",
                createdAt = System.currentTimeMillis()
            )
            addPointsTransaction(tx, context) {}

            onResult(ResultState.Success(updatedAccount))
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

        // 1. Fetch current account
        fetchRewardAccount(userId, context) { accountResult ->
            if (accountResult !is ResultState.Success) {
                onResult(ResultState.Error(message = "Failed to retrieve user reward balance."))
                return@fetchRewardAccount
            }

            val account = accountResult.data

            // 2. Validate availability & stock
            if (!rewardItem.isActive) {
                onResult(ResultState.Error(message = "This reward item is currently inactive or unavailable."))
                return@fetchRewardAccount
            }

            if (rewardItem.stockQuantity != null && rewardItem.stockQuantity <= 0) {
                onResult(ResultState.Error(message = "This reward item is currently out of stock."))
                return@fetchRewardAccount
            }

            // 3. Validate user points balance
            if (account.pointsBalance < rewardItem.pointsCost) {
                val needed = rewardItem.pointsCost - account.pointsBalance
                onResult(ResultState.Error(message = "Insufficient points balance! You need $needed more points."))
                return@fetchRewardAccount
            }

            // 4. Perform atomic deduction & creation
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

            // Local cache updates first for immediate consistency
            context?.let { ctx ->
                saveAccountLocally(ctx, updatedAccount)

                val currentTxList = getTransactionsLocally(ctx, userId).toMutableList()
                currentTxList.removeAll { item -> item.transactionId == pointsTx.transactionId }
                currentTxList.add(0, pointsTx)
                saveTransactionsLocally(ctx, userId, currentTxList)

                val currentLogs = getRedemptionLogsLocally(ctx, userId).toMutableList()
                currentLogs.removeAll { item -> item.redemptionId == redemptionLog.redemptionId }
                currentLogs.add(0, redemptionLog)
                saveRedemptionLogsLocally(ctx, userId, currentLogs)
            }

            // Firestore batch write
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
                    onResult(ResultState.Success(Pair(updatedAccount, redemptionLog)))
                }
                .addOnFailureListener {
                    // Local cache is already saved, so return success for offline capability
                    onResult(ResultState.Success(Pair(updatedAccount, redemptionLog)))
                }
        }
    }

    // Legacy overload for backward compatibility
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

    // Overload for backward compatibility
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
