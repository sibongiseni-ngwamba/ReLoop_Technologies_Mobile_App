package com.example.reloop_technologies_mobile_app.backend.api

import com.example.reloop_technologies_mobile_app.backend.models.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ReLoopRestApi {
    @GET("users/{userId}") fun getUser(@Path("userId") userId: String): Call<UserModel>
    @PUT("users/{userId}") fun updateUser(@Path("userId") userId: String, @Body user: UserModel): Call<UserModel>

    @GET("pickups") fun getPickups(): Call<List<PickupModel>>
    @POST("pickups") fun createPickup(@Body pickup: PickupModel): Call<PickupModel>
    @PATCH("pickups/{pickupId}") fun updatePickup(@Path("pickupId") pickupId: String, @Body updates: Map<String, Any?>): Call<PickupModel>

    @GET("rewards/account") fun getRewardAccount(): Call<RewardAccount>
    @GET("rewards/items") fun getRewardItems(): Call<List<RewardItem>>
    @GET("rewards/transactions") fun getTransactions(): Call<List<PointsTransaction>>
    @POST("rewards/transactions") fun awardPoints(@Body body: Map<String, Any?>): Call<Map<String, Any?>>
    @POST("rewards/redemptions") fun redeemReward(@Body body: Map<String, String>): Call<RedemptionResponse>

    @GET("waste-categories") fun getWasteCategories(): Call<List<WasteCategory>>
    @GET("waste-logs") fun getWasteLogs(): Call<List<WasteLog>>
    @POST("waste-logs") fun createWasteLog(@Body log: WasteLog): Call<WasteLog>

    @GET("notifications") fun getNotifications(): Call<List<NotificationModel>>
    @POST("notifications") fun createNotification(@Body notification: NotificationModel): Call<NotificationModel>
    @PATCH("notifications/{notificationId}") fun updateNotification(@Path("notificationId") id: String, @Body updates: Map<String, Any?>): Call<NotificationModel>
    @DELETE("notifications/{notificationId}") fun deleteNotification(@Path("notificationId") id: String): Call<Map<String, Boolean>>
    @DELETE("notifications") fun clearNotifications(): Call<Map<String, Boolean>>

    @GET("guidance") fun getGuidance(): Call<List<GuidanceContent>>
}

data class RedemptionResponse(
    val account: RewardAccount = RewardAccount(),
    val redemption: RedemptionLog = RedemptionLog(),
    val transaction: PointsTransaction = PointsTransaction()
)
