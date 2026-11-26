package com.example.shortlink.link

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Запрос на создание короткой ссылки.
 * Сообщения об ошибках совпадают с контрактом lab01 дословно.
 */
data class CreateLinkRequest(

    @field:NotBlank(message = "longUrl is required")
    @field:Pattern(
        regexp = "^https?://.*",
        message = "longUrl must start with http:// or https://",
    )
    @field:Size(
        max = 2048,
        message = "longUrl must be at most 2048 characters",
    )
    val longUrl: String? = null,
)
