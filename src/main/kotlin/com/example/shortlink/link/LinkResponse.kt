package com.example.shortlink.link

/**
 * Ответ на создание короткой ссылки.
 */
data class LinkResponse(
    val shortCode: String,
    val shortUrl: String,
)
