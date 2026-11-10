package com.example.reloop_technologies_mobile_app.backend.repositories

import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.api.RestApiClient
import com.example.reloop_technologies_mobile_app.backend.models.GuidanceContent
import com.example.reloop_technologies_mobile_app.backend.models.WasteLog

class WasteScanRepository(private val restApiClient: RestApiClient = RestApiClient.instance) {
    fun fetchWasteScansForUser(userId: String, onResult: (ResultState<List<WasteLog>>) -> Unit) {
        if (userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.getWasteLogsForUser(userId, onResult)
    }

    fun logWasteScan(wasteLog: WasteLog, onResult: (ResultState<String>) -> Unit) {
        if (wasteLog.userId.isBlank()) return onResult(ResultState.Error(message = "User ID is empty."))
        restApiClient.saveWasteLog(wasteLog, onResult)
    }

    fun fetchGuidanceContent(onResult: (ResultState<List<GuidanceContent>>) -> Unit) =
        restApiClient.getGuidanceContent(onResult)
}
