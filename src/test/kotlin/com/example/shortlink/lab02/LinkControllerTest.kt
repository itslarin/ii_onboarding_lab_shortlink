package lab02

import com.example.shortlink.Application
import com.fasterxml.jackson.databind.JsonNode
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@Tag("lab02")
class LinkControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun postLink(longUrl: String): Pair<Int, JsonNode> {
        val result = mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "$longUrl"}"""
        }.andReturn()
        val status = result.response.status
        val body = objectMapper.readTree(result.response.contentAsString)
        return status to body
    }

    @Test
    @DisplayName("C1: Первый POST с новым longUrl → 201, shortCode (6 символов Base62) и shortUrl")
    fun `should_return_201_and_short_code_when_new_url`() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "https://example.com/lab02/c1"}"""
        }.andExpect {
            status { isCreated }
            jsonPath("$.shortCode") { value(matchesPattern("[0-9a-zA-Z]{6}")) }
            jsonPath("$.shortUrl") { value(matchesPattern("http://localhost:8080/[0-9a-zA-Z]{6}")) }
        }
    }

    @Test
    @DisplayName("C2: Повторный POST с тем же longUrl → 200, тот же shortCode и shortUrl")
    fun `should_return_200_and_same_code_when_duplicate_url`() {
        val longUrl = "https://example.com/lab02/c2"

        val (firstStatus, firstBody) = postLink(longUrl)
        assertThat(firstStatus).isEqualTo(201)

        val (secondStatus, secondBody) = postLink(longUrl)
        assertThat(secondStatus).isEqualTo(200)

        assertThat(secondBody["shortCode"].asText())
            .isEqualTo(firstBody["shortCode"].asText())
        assertThat(secondBody["shortUrl"].asText())
            .isEqualTo(firstBody["shortUrl"].asText())
    }

    @Test
    @DisplayName("C3: Два разных longUrl → два разных shortCode, оба 201")
    fun `should_return_different_codes_when_different_urls`() {
        val (statusA, bodyA) = postLink("https://example.com/lab02/c3-a")
        val (statusB, bodyB) = postLink("https://example.com/lab02/c3-b")

        assertThat(statusA).isEqualTo(201)
        assertThat(statusB).isEqualTo(201)
        assertThat(bodyA["shortCode"].asText())
            .isNotEqualTo(bodyB["shortCode"].asText())
    }

    @Test
    @DisplayName("C4: longUrl отсутствует в JSON → 400, longUrl is required")
    fun `should_return_400_when_long_url_missing`() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest }
            jsonPath("$.error") { value("longUrl is required") }
        }
    }

    @Test
    @DisplayName("C5+C6: longUrl пустой или только пробелы → 400, longUrl must not be blank")
    fun `should_return_400_when_long_url_blank`() {
        for (blank in listOf("", "   ")) {
            mockMvc.post("/links") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"longUrl": "$blank"}"""
            }.andExpect {
                status { isBadRequest }
                jsonPath("$.error") { value("longUrl must not be blank") }
            }
        }
    }

    @Test
    @DisplayName("C7: longUrl не начинается с http:// или https:// → 400")
    fun `should_return_400_when_long_url_has_invalid_scheme`() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "ftp://example.com"}"""
        }.andExpect {
            status { isBadRequest }
            jsonPath("$.error") { value("longUrl must start with http:// or https://") }
        }
    }

    @Test
    @DisplayName("C8: longUrl длиннее 2048 символов → 400")
    fun `should_return_400_when_long_url_too_long`() {
        val tooLong = "https://example.com/" + "a".repeat(2049 - "https://example.com/".length)
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"longUrl": "$tooLong"}"""
        }.andExpect {
            status { isBadRequest }
            jsonPath("$.error") { value("longUrl must be at most 2048 characters") }
        }
    }

    @Test
    @DisplayName("C9: Тело невалидный JSON → 400, malformed request body")
    fun `should_return_400_when_body_malformed`() {
        mockMvc.post("/links") {
            contentType = MediaType.APPLICATION_JSON
            content = "{"
        }.andExpect {
            status { isBadRequest }
            jsonPath("$.error") { value("malformed request body") }
        }
    }

    @Test
    @DisplayName("C10: Content-Type не application/json → 415")
    fun `should_return_415_when_content_type_not_json`() {
        mockMvc.post("/links") {
            contentType = MediaType.TEXT_PLAIN
            content = "https://example.com"
        }.andExpect {
            status { isUnsupportedMediaType }
            jsonPath("$.error") { value("unsupported media type") }
        }
    }

    @Test
    @DisplayName("C11: Повторный POST с невалидным longUrl → 400 дважды, дедупликация не срабатывает")
    fun `should_return_400_when_duplicate_url_is_invalid`() {
        val invalid = "ftp://example.com/lab02/c11"

        val (firstStatus, firstBody) = postLink(invalid)
        assertThat(firstStatus).isEqualTo(400)
        assertThat(firstBody["error"].asText())
            .isEqualTo("longUrl must start with http:// or https://")

        val (secondStatus, secondBody) = postLink(invalid)
        assertThat(secondStatus).isEqualTo(400)
        assertThat(secondBody["error"].asText())
            .isEqualTo("longUrl must start with http:// or https://")
    }
}
