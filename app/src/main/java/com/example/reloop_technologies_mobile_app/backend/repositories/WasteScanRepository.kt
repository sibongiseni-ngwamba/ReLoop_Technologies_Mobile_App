package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.GuidanceContent
import com.example.reloop_technologies_mobile_app.backend.models.WasteLog
import com.example.reloop_technologies_mobile_app.backend.services.FirestoreService

class WasteScanRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {
    fun logWasteScan(wasteLog: WasteLog, onResult: (ResultState<String>) -> Unit) {
        onResult(ResultState.Loading)
        val docRef = firestoreService.db.collection("wasteLogs").document()
        val log = wasteLog.copy(wasteLogId = docRef.id)

        docRef.set(log)
            .addOnSuccessListener {
                onResult(ResultState.Success(docRef.id))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }

    fun fetchGuidanceContent(onResult: (ResultState<List<GuidanceContent>>) -> Unit) {
        onResult(ResultState.Loading)
        firestoreService.db.collection("guidanceContent")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(GuidanceContent::class.java)
                onResult(ResultState.Success(list))
            }
            .addOnFailureListener { exception ->
                onResult(ResultState.Error(exception = exception, message = firestoreService.mapError(exception)))
            }
    }
}
