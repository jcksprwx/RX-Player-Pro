package com.rxplayer.pro.core.common

sealed interface NovaResult<out T> {
    data class Success<T>(val data: T) : NovaResult<T>
    data class Error(val exception: Throwable, val message: String? = exception.localizedMessage) : NovaResult<Nothing>
    data object Loading : NovaResult<Nothing>
}
