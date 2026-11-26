package com.example.shortlink.link

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Единый формат ошибок {"error": "..."} для всех неуспешных ответов.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Ошибки валидации DTO: сообщение — из первой field-ошибки,
     * текст задан в аннотациях CreateLinkRequest дословно по контракту.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: "validation failed"
        return badRequest(message)
    }

    /** Тело запроса не является валидным JSON. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.debug("Malformed request body: {}", e.message)
        return badRequest("malformed request body")
    }

    /** Content-Type не поддерживается эндпоинтом. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaType(e: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported media type")

    /** Коллизия короткого кода: не удалось сгенерировать уникальный код за отведённые попытки. */
    @ExceptionHandler(ShortCodeGenerationException::class)
    fun handleCodeGeneration(e: ShortCodeGenerationException): ResponseEntity<ErrorResponse> {
        log.error("Short code generation failed", e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "internal error")
    }

    /**
     * Fallback: путь вне эндпоинтов сервиса (например, многосегментный /a/b)
     * отвечает в едином формате, а не стандартным Spring-телом ошибки.
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException): ResponseEntity<ErrorResponse> =
        error(HttpStatus.NOT_FOUND, "not found")

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        error(HttpStatus.BAD_REQUEST, message)

    private fun error(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(error = message))
}
