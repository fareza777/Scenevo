package com.scenevo.core.common

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data
