package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.RedemptionLog
import com.example.reloop_technologies_mobile_app.backend.models.RewardAccount
import com.example.reloop_technologies_mobile_app.backend.models.RewardItem
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class RewardsRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    fun fetchRewardAccount(userId: String, onResult: (ResultState<RewardAccount>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        onResult(ResultState.Loading)
        firestoreService.db.collection("rewards").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val account = doc.toObject(RewardAccount::class.java) ?: RewardAccount(userId = userId)
                    onResult(ResultState.Success(account))
                } else {
                    onResult(ResultState.Success(RewardAccount(userId = userId)))
                }
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun fetchAvailableRewardItems(onResult: (ResultState<List<RewardItem>>) -> Unit) {
        onResult(ResultState.Loading)
        firestoreService.db.collection("rewardItems")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.toObjects(RewardItem::class.java)
                onResult(ResultState.Success(items))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
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
