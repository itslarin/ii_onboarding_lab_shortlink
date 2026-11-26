package com.example.shortlink.link

/**
 * Единый формат тела ошибки: {"error": "<сообщение>"}.
 */
data class ErrorResponse(
    val error: String,
)
