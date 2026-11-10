package com.example.reloop_technologies_mobile_app.backend.services

import android.content.Context
import com.example.reloop_technologies_mobile_app.backend.ResultState
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

data class ReLoopAuthUser(val uid: String, val email: String?, val displayName: String?)

class SupabaseAuthService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUser: ReLoopAuthUser?
        get() = if (!SupabaseClientProvider.isConfigured) null else runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.let { user ->
                val metadata = user.userMetadata
                ReLoopAuthUser(
                    uid = user.id,
                    email = user.email,
                    displayName = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                        ?: listOfNotNull(
                            metadata?.get("first_name")?.jsonPrimitive?.contentOrNull,
                            metadata?.get("last_name")?.jsonPrimitive?.contentOrNull
                        ).joinToString(" ").ifEmpty { null }
                )
            }
        }.getOrNull()

    fun isUserSignedIn(): Boolean = currentUser != null
    fun getUserId(): String = currentUser?.uid.orEmpty()

    fun signIn(email: String, password: String, onResult: (ResultState<ReLoopAuthUser>) -> Unit) = runAuth(onResult) {
        SupabaseClientProvider.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        currentUser ?: error("Sign in completed without an active session.")
    }

    fun signUp(email: String, password: String, displayName: String, phone: String, address: String, onResult: (ResultState<ReLoopAuthUser?>) -> Unit) = runAuth(onResult) {
        val names = displayName.trim().split(Regex("\\s+"), limit = 2)
        SupabaseClientProvider.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("first_name", names.firstOrNull().orEmpty())
                put("last_name", names.getOrElse(1) { "" })
                put("phone_number", phone)
                put("address", address)
                put("full_name", displayName)
            }
        }
        currentUser
    }

    fun signInWithGoogleIdToken(idToken: String, onResult: (ResultState<ReLoopAuthUser>) -> Unit) = runAuth(onResult) {
        SupabaseClientProvider.client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
        currentUser ?: error("Google sign-in completed without an active session.")
    }

    fun updatePassword(newPassword: String, onResult: (ResultState<Boolean>) -> Unit) = runAuth(onResult) {
        SupabaseClientProvider.client.auth.updateUser { password = newPassword }
        true
    }

    fun sendPasswordResetEmail(email: String, onResult: (ResultState<Boolean>) -> Unit) = runAuth(onResult) {
        SupabaseClientProvider.client.auth.resetPasswordForEmail(email)
        true
    }

    fun signOut(context: Context? = null, onResult: ((ResultState<Boolean>) -> Unit)? = null) {
        if (onResult == null) {
            scope.launch { runCatching { if (SupabaseClientProvider.isConfigured) SupabaseClientProvider.client.auth.signOut() } }
            return
        }
        runAuth(onResult) {
            SupabaseClientProvider.client.auth.signOut()
            true
        }
    }

    private fun <T> runAuth(onResult: (ResultState<T>) -> Unit, action: suspend () -> T) {
        if (!SupabaseClientProvider.isConfigured) {
            onResult(ResultState.Error(message = "Set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties first."))
            return
        }
        onResult(ResultState.Loading)
        scope.launch {
            val result = runCatching { action() }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onResult(ResultState.Success(it)) },
                    onFailure = { onResult(ResultState.Error(it as? Exception, it.localizedMessage ?: "Supabase authentication failed.")) }
                )
            }
        }
    }
}
