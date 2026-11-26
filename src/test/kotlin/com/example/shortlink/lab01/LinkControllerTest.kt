package com.example.shortlink.lab01

import com.example.shortlink.Application
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@Tag("lab01")
class LinkControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("C1: Валидный longUrl → 201, shortCode (6 символов Base62) и shortUrl")
    fun should_return_201_and_short_code_when_valid_url() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "https://example.com/some/path"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.shortCode") { value(matchesPattern("[0-9a-zA-Z]{6}")) }
            jsonPath("$.shortUrl") { value(matchesPattern("http://localhost:8080/[0-9a-zA-Z]{6}")) }
        }
    }

    @Test
    @DisplayName("C2: longUrl отсутствует в JSON → 400")
    fun should_return_400_when_long_url_missing() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("longUrl is required") }
        }
    }

    @Test
    @DisplayName("C3+C4: longUrl пустой или только пробелы → 400")
    fun should_return_400_when_long_url_blank() {
        for (blank in listOf("", "   ")) {
            mockMvc.post("/links") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"longUrl": "$blank"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("longUrl is required") }
            }
        }
    }

    @Test
    @DisplayName("C5: longUrl не начинается с http:// или https:// → 400")
    fun should_return_400_when_long_url_has_invalid_scheme() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "ftp://example.com"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("longUrl must start with http:// or https://") }
        }
    }

    @Test
    @DisplayName("C6: longUrl длиннее 2048 символов → 400")
    fun should_return_400_when_long_url_too_long() {
        val tooLong = "https://example.com/" + "a".repeat(2049 - "https://example.com/".length)
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "$tooLong"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("longUrl must be at most 2048 characters") }
        }
    }

    @Test
    @DisplayName("C7: Тело невалидный JSON → 400")
    fun should_return_400_when_body_malformed() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = "{"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("malformed request body") }
        }
    }

    @Test
    @DisplayName("C8: Content-Type не application/json → 415")
    fun should_return_415_when_content_type_not_json() {
        mockMvc.post("/links") {
            contentType = MediaType.TEXT_PLAIN
            content = "https://example.com"
        }.andExpect {
            status { isUnsupportedMediaType() }
            jsonPath("$.error") { value("unsupported media type") }
        }
    }

    @Test
    @DisplayName("C13: Повторный POST с тем же longUrl → 201, shortCode отличается (дедупликации нет)")
    fun should_create_new_short_code_when_long_url_already_exists() {
        val body = """{"longUrl": "https://example.com/duplicate"}"""

        val first = mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect { status { isCreated() } }.andReturn()

        val second = mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect { status { isCreated() } }.andReturn()

        val firstCode = objectMapper.readTree(first.response.contentAsString)["shortCode"].asText()
        val secondCode = objectMapper.readTree(second.response.contentAsString)["shortCode"].asText()

        assertThat(firstCode).isNotEqualTo(secondCode)
    }
}
