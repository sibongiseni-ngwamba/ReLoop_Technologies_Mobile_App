package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.api.RestDataSeed
import com.example.reloop_technologies_mobile_app.backend.models.GuidanceContent
import com.example.reloop_technologies_mobile_app.backend.models.WasteLog
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class WasteScanRepository(
    private val firestoreService: FirestoreService = FirestoreService(),
    private val restApiClient: RestApiClient = RestApiClient.instance
) {
    fun fetchWasteScansForUser(userId: String, onResult: (ResultState<List<WasteLog>>) -> Unit) {
        if (userId.isBlank()) {
            onResult(ResultState.Error(message = "User ID is empty."))
            return
        }
        restApiClient.getWasteLogsForUser(userId) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    firestoreService.db.collection("wasteLogs").whereEqualTo("userId", userId).get()
                        .addOnSuccessListener { snapshot ->
                            try {
                                val list = snapshot.toObjects(WasteLog::class.java)
                                onResult(ResultState.Success(list.ifEmpty { RestDataSeed.seededWasteLogs }))
                            } catch (_: RuntimeException) {
                                onResult(ResultState.Success(RestDataSeed.seededWasteLogs))
                            }
                        }
                        .addOnFailureListener {
                            onResult(ResultState.Success(RestDataSeed.seededWasteLogs))
                        }
                }
            }
        }
    }

    fun logWasteScan(wasteLog: WasteLog, onResult: (ResultState<String>) -> Unit) {
        onResult(ResultState.Loading)
        restApiClient.saveWasteLog(wasteLog) { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    val docRef = firestoreService.db.collection("wasteLogs").document(restResult.data)
                    docRef.set(wasteLog.copy(wasteLogId = restResult.data))
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    val docRef = firestoreService.db.collection("wasteLogs").document()
                    docRef.set(wasteLog.copy(wasteLogId = docRef.id))
                        .addOnSuccessListener { onResult(ResultState.Success(docRef.id)) }
                        .addOnFailureListener { onResult(ResultState.Success(docRef.id)) }
                }
            }
        }
    }

    fun fetchGuidanceContent(onResult: (ResultState<List<GuidanceContent>>) -> Unit) {
        onResult(ResultState.Loading)
        restApiClient.getGuidanceContent { restResult ->
            when (restResult) {
                is ResultState.Success -> {
                    onResult(ResultState.Success(restResult.data))
                }
                else -> {
                    onResult(ResultState.Success(RestDataSeed.seededGuidanceContent))
                }
            }
        }
    }
}
