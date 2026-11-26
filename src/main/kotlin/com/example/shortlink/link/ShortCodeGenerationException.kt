package com.example.shortlink.link

/**
 * Коллизия короткого кода: не удалось сгенерировать уникальный код
 * за отведённое число попыток (500 через advice).
 */
class ShortCodeGenerationException(message: String) : RuntimeException(message)
