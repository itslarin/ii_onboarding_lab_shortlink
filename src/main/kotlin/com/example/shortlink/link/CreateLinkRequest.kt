package com.example.shortlink.link

/**
 * Запрос на создание короткой ссылки.
 */
data class CreateLinkRequest(
    val longUrl: String? = null,
)
