package com.example.reloop_technologies_mobile_app.backend.api

import com.example.reloop_technologies_mobile_app.BuildConfig
import com.example.reloop_technologies_mobile_app.backend.BackendConfig
import com.example.reloop_technologies_mobile_app.backend.ResultState
import com.example.reloop_technologies_mobile_app.backend.models.*
import com.example.reloop_technologies_mobile_app.backend.services.SupabaseClientProvider
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The app's business-data gateway. All reads and writes go through the ReLoop REST Edge Function. */
class RestApiClient private constructor() {
    private val api: ReLoopRestApi by lazy {
        val configuredUrl = BuildConfig.RELOOP_API_URL.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?: "https://unconfigured.supabase.co/functions/v1/reloop-api/"
        val authHeaders = Interceptor { chain ->
            if (BuildConfig.SUPABASE_ANON_KEY.isBlank()) throw IOException("Set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties.")
            val token = SupabaseClientProvider.accessToken()
                ?: throw IOException("Please sign in again to access ReLoop data.")
            val request = chain.request().newBuilder()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        val http = OkHttpClient.Builder()
            .addInterceptor(authHeaders)
            .connectTimeout(BackendConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(BackendConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(configuredUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(ReLoopRestApi::class.java)
    }

    private fun <T, R> send(call: Call<T>, onResult: (ResultState<R>) -> Unit, transform: (T) -> R) {
        onResult(ResultState.Loading)
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
                if (!response.isSuccessful) {
                    val detail = response.errorBody()?.string()?.take(300)
                    onResult(ResultState.Error(message = detail?.takeIf { it.isNotBlank() } ?: "Request failed (${response.code()})."))
                    return
                }
                val body = response.body()
                if (body == null) {
                    onResult(ResultState.Error(message = "The server returned an empty response."))
                    return
                }
                runCatching { transform(body) }
                    .onSuccess { onResult(ResultState.Success(it)) }
                    .onFailure { onResult(ResultState.Error(it as? Exception, it.localizedMessage ?: "Could not read the server response.")) }
            }

            override fun onFailure(call: Call<T>, error: Throwable) {
                onResult(ResultState.Error(error as? Exception, error.localizedMessage ?: "Could not connect to the ReLoop API."))
            }
        })
    }

    val baseUrl: String get() = BuildConfig.RELOOP_API_URL

    fun getUserProfile(userId: String, onResult: (ResultState<UserModel>) -> Unit) =
        send(api.getUser(userId), onResult) { it }

    fun updateUserProfile(user: UserModel, onResult: (ResultState<Boolean>) -> Unit) =
        send(api.updateUser(user.userId, user), onResult) { true }

    fun getPickupsForUser(userId: String, role: String = "resident", onResult: (ResultState<List<PickupModel>>) -> Unit) =
        send(api.getPickups(), onResult) { pickups ->
            if (role.equals("collector", true) || role.equals("admin", true)) pickups
            else pickups.filter { it.userId == userId }
        }

    fun schedulePickup(pickup: PickupModel, onResult: (ResultState<String>) -> Unit) =
        send(api.createPickup(pickup), onResult) { it.pickupId }

    fun completePickup(pickupId: String, userId: String = "", onResult: (ResultState<Boolean>) -> Unit) =
        send(api.updatePickup(pickupId, mapOf("status" to "Completed", "completedAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis())), onResult) { true }

    fun cancelPickup(pickupId: String, cancellationReason: String, onResult: (ResultState<Boolean>) -> Unit) =
        send(api.updatePickup(pickupId, mapOf("status" to "Cancelled", "cancelledAt" to System.currentTimeMillis(), "cancellationReason" to cancellationReason, "updatedAt" to System.currentTimeMillis())), onResult) { true }

    fun setPickupStatus(pickupId: String, status: String, onResult: (ResultState<Boolean>) -> Unit): Unit {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>("status" to status, "updatedAt" to now)
        if (status.equals("Completed", true)) updates["completedAt"] = now
        send(api.updatePickup(pickupId, updates), onResult) { true }
    }

    fun getRewardAccount(userId: String, onResult: (ResultState<RewardAccount>) -> Unit) =
        send(api.getRewardAccount(), onResult) { it }

    /** Reward balances are server-owned and change only through the atomic points RPCs. */
    fun saveRewardAccount(account: RewardAccount, onResult: (ResultState<Boolean>) -> Unit) {
        onResult(ResultState.Error(message = "Reward balances can only be changed through an approved points transaction."))
    }

    fun getRewardItems(onResult: (ResultState<List<RewardItem>>) -> Unit) =
        send(api.getRewardItems(), onResult) { it }

    fun getPointsTransactions(userId: String, onResult: (ResultState<List<PointsTransaction>>) -> Unit) =
        send(api.getTransactions(), onResult) { it }

    fun addPointsTransaction(tx: PointsTransaction, onResult: (ResultState<String>) -> Unit) =
        send(api.awardPoints(mapOf("points" to tx.points, "relatedId" to tx.relatedId, "description" to tx.description)), onResult) { result ->
            result["transactionId"]?.toString() ?: ""
        }

    fun processRewardRedemption(userId: String, item: RewardItem, onResult: (ResultState<Pair<RewardAccount, RedemptionLog>>) -> Unit) =
        send(api.redeemReward(mapOf("rewardItemId" to item.rewardItemId)), onResult) { it.account to it.redemption }

    fun getWasteCategories(onResult: (ResultState<List<WasteCategory>>) -> Unit) =
        send(api.getWasteCategories(), onResult) { it }

    fun getWasteLogsForUser(userId: String, onResult: (ResultState<List<WasteLog>>) -> Unit) =
        send(api.getWasteLogs(), onResult) { it }

    fun saveWasteLog(wasteLog: WasteLog, onResult: (ResultState<String>) -> Unit) =
        send(api.createWasteLog(wasteLog), onResult) { it.wasteLogId }

    fun getNotificationsForUser(userId: String, onResult: (ResultState<List<NotificationModel>>) -> Unit) =
        send(api.getNotifications(), onResult) { it }

    fun addNotification(notification: NotificationModel, onResult: ((ResultState<String>) -> Unit)? = null) {
        send(api.createNotification(notification), { result -> onResult?.invoke(result) }) { it.notificationId }
    }

    fun markNotificationAsRead(notificationId: String, onResult: (ResultState<Boolean>) -> Unit) =
        send(api.updateNotification(notificationId, mapOf("isRead" to true)), onResult) { true }

    fun clearNotificationsForUser(userId: String, onResult: (ResultState<Boolean>) -> Unit) =
        send(api.clearNotifications(), onResult) { true }

    fun getGuidanceContent(onResult: (ResultState<List<GuidanceContent>>) -> Unit) =
        send(api.getGuidance(), onResult) { it }

    companion object {
        val instance: RestApiClient by lazy { RestApiClient() }
    }
}
