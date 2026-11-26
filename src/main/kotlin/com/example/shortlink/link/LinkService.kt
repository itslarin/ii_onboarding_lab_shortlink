package com.example.shortlink.link

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Бизнес-логика: создание короткой ссылки и lookup для редиректа.
 */
@Service
class LinkService(
    private val repository: LinkRepository,
    private val codeGenerator: ShortCodeGenerator,
) {

    private val log = LoggerFactory.getLogger(LinkService::class.java)

    /**
     * Генерирует Base62-код, при коллизии — до [MAX_GENERATION_ATTEMPTS]
     * повторных попыток, затем исключение (500 через GlobalExceptionHandler).
     */
    fun create(longUrl: String): LinkResponse {
        val link = generateUniqueLink(longUrl)
        repository.save(link)
        log.info("Created short link: {} -> {}", link.shortCode, link.longUrl)
        return LinkResponse(
            shortCode = link.shortCode,
            shortUrl = "$BASE_URL/${link.shortCode}",
        )
    }

    /**
     * Lookup по коду; регистрозависимый (обычный map-lookup).
     * null — код не найден / не существует (редирект вернёт 404).
     */
    fun resolve(shortCode: String): Link? = repository.findByCode(shortCode)

    private fun generateUniqueLink(longUrl: String): Link {
        repeat(MAX_GENERATION_ATTEMPTS) {
            val code = codeGenerator.generate()
            val link = Link(shortCode = code, longUrl = longUrl)
            if (repository.findByCode(code) == null) {
                return link
            }
            log.warn("Short code collision: {}", code)
        }
        throw ShortCodeGenerationException(
            "failed to generate unique short code after $MAX_GENERATION_ATTEMPTS attempts",
        )
    }

    companion object {
        const val BASE_URL = "http://localhost:8080"
        private const val MAX_GENERATION_ATTEMPTS = 3
    }
}

class ShortCodeGenerationException(message: String) : RuntimeException(message)
