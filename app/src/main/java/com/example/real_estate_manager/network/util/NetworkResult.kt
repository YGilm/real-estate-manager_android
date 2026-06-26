package com.example.real_estate_manager.network.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

sealed class NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>()
}

fun Throwable.toUserMessage(): String =
    when (this) {
        is SocketTimeoutException -> "Сервер недоступен"
        is UnknownHostException -> "Неверный адрес сервера"
        is ConnectException -> "Сервер не запущен"
        is HttpException -> when (code()) {
            401 -> "Требуется повторный вход"
            403 -> "Доступ запрещен"
            in 500..599 -> "Ошибка сервера"
            else -> "Ошибка запроса: ${code()}"
        }
        is IOException -> "Сервер недоступен"
        else -> message ?: "Не удалось выполнить запрос."
    }
