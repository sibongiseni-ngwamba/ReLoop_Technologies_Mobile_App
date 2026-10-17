package com.example.reloop_technologies_mobile_app.backend

sealed class ResultState<out T> {
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val exception: Exception? = null, val message: String) : ResultState<Nothing>()
    object Loading : ResultState<Nothing>()
}
