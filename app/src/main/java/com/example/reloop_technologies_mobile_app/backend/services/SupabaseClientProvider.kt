package com.example.reloop_technologies_mobile_app.backend.services

import com.example.reloop_technologies_mobile_app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth

object SupabaseClientProvider {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client by lazy {
        check(isConfigured) { "Set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties before using Supabase." }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Auth)
        }
    }

    fun accessToken(): String? = if (isConfigured) runCatching {
        client.auth.currentSessionOrNull()?.accessToken
    }.getOrNull() else null
}
