package com.example.shortlink.lab01

import com.example.shortlink.Application
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@Tag("lab01")
class RedirectControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun createLinkAndGetCode(longUrl: String): String {
        val response = mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "$longUrl"}"""
        }.andExpect { status { isCreated() } }.andReturn()
        return objectMapper.readTree(response.response.contentAsString)["shortCode"].asText()
    }

    @Test
    @DisplayName("C9: GET /{code} для существующего кода → 302, Location = исходный longUrl")
    fun should_return_302_when_code_exists() {
        val longUrl = "https://example.com/target"
        val code = createLinkAndGetCode(longUrl)

        mockMvc.get("/$code").andExpect {
            status { isFound() }
            header { string("Location", longUrl) }
        }
    }

    @Test
    @DisplayName("C10: GET /{code} для несуществующего кода → 404")
    fun should_return_404_when_code_not_found() {
        mockMvc.get("/nonexistent").andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("short code not found") }
        }
    }

    @Test
    @DisplayName("C11: GET /{code} с кодом в другом регистре → 404")
    fun should_return_404_when_code_case_mismatch() {
        var code: String
        var toggled: String
        var attempts = 0
        do {
            code = createLinkAndGetCode("https://example.com/case")
            toggled = code.map {
                when {
                    it.isUpperCase() -> it.lowercaseChar()
                    it.isLowerCase() -> it.uppercaseChar()
                    else -> it
                }
            }.joinToString("")
            attempts++
        } while (code == toggled && attempts < 10)

        assertThat(code)
            .describedAs("Generated code must contain a letter to test case sensitivity")
            .isNotEqualTo(toggled)

        mockMvc.get("/$toggled").andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("short code not found") }
        }
    }

    @Test
    @DisplayName("C12: GET /{code} с не-Base62 символом → 404")
    fun should_return_404_when_code_has_invalid_chars() {
        mockMvc.get("/ab_cd").andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("short code not found") }
        }
    }
}