package com.example.shortlink.link

import java.security.SecureRandom
import org.springframework.stereotype.Component

/**
 * Генератор коротких кодов: случайная строка длиной 6 из алфавита Base62
 * ([0-9a-zA-Z], 62^6 ≈ 5.6·10^10 комбинаций).
 */
@Component
class ShortCodeGenerator {

    private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val random = SecureRandom()

    fun generate(): String = buildString {
        repeat(CODE_LENGTH) { append(alphabet[random.nextInt(alphabet.length)]) }
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}
