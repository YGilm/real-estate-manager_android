package com.example.real_estate_manager.ui

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

fun Throwable.toUiErrorMessage(): String =
    when (this) {
        is ConnectException -> "Сервер не запущен. Изменения сохранены локально, если операция поддерживает офлайн-режим."
        is SocketTimeoutException -> "Сервер недоступен. Изменения сохранены локально, если операция поддерживает офлайн-режим."
        is UnknownHostException -> "Неверный адрес сервера."
        is HttpException -> when (code()) {
            401 -> "Требуется повторный вход."
            403 -> "Доступ запрещен."
            in 500..599 -> "Ошибка сервера. Повторите позже."
            else -> "Не удалось выполнить запрос. Изменения сохранены локально, если операция поддерживает офлайн-режим."
        }
        is IOException -> "Сервер недоступен. Изменения сохранены локально, если операция поддерживает офлайн-режим."
        else -> "Не удалось выполнить операцию."
    }
