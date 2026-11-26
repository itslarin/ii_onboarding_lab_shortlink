package com.example.shortlink.link

/**
 * Доменная модель короткой ссылки.
 * Не возвращается из контроллеров наружу (только DTO).
 */
data class Link(
    val shortCode: String,
    val longUrl: String,
)
