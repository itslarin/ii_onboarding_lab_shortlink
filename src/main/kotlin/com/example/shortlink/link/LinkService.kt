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
     * Валидация longUrl в порядке контракта (детерминированные сообщения):
     * null/blank -> схема -> длина.
     */
    fun create(longUrl: String?): LinkResponse {
        validate(longUrl)
        val link = generateUniqueLink(longUrl!!)
        repository.save(link)
        log.info("Created short link: {} -> {}", link.shortCode, link.longUrl)
        return LinkResponse(
            shortCode = link.shortCode,
            shortUrl = "$BASE_URL/${link.shortCode}",
        )
    }

    /**
     * Lookup по коду; регистрозависимый (обычный map-lookup).
     * null — код не найден (редирект вернёт 404).
     */
    fun resolve(shortCode: String): Link? = repository.findByCode(shortCode)

    private fun validate(longUrl: String?) {
        if (longUrl == null || longUrl.isBlank()) {
            throw InvalidLongUrlException("longUrl is required")
        }
        if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
            throw InvalidLongUrlException("longUrl must start with http:// or https://")
        }
        if (longUrl.length > MAX_URL_LENGTH) {
            throw InvalidLongUrlException("longUrl must be at most 2048 characters")
        }
    }

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
        private const val MAX_URL_LENGTH = 2048
    }
}
