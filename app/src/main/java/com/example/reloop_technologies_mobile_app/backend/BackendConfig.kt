package com.example.reloop_technologies_mobile_app.backend

object BackendConfig {
    enum class Environment {
        DEVELOPMENT,
        STAGING,
        PRODUCTION
    }

    var currentEnvironment: Environment = Environment.DEVELOPMENT

    val apiBaseUrl: String
        get() = when (currentEnvironment) {
            Environment.DEVELOPMENT -> "https://dev-api.reloop.app/v1/"
            Environment.STAGING -> "https://staging-api.reloop.app/v1/"
            Environment.PRODUCTION -> "https://api.reloop.app/v1/"
        }

    const val NETWORK_TIMEOUT_SECONDS = 15L
    const val MAX_RETRY_COUNT = 3
}
