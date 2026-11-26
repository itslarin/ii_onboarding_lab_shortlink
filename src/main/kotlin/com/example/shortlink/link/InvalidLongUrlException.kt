package com.example.shortlink.link

/**
 * Нарушение контракта longUrl: 400 {"error": "<message>"} через advice.
 * Сообщения задаются дословно по контракту lab01.
 */
class InvalidLongUrlException(message: String) : RuntimeException(message)
