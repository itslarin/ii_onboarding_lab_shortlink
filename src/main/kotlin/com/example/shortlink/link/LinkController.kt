package com.example.shortlink.link

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LinkController(
    private val service: LinkService,
) {

    @PostMapping("/links")
    fun create(@RequestBody request: CreateLinkRequest): ResponseEntity<LinkResponse> {
        val response = service.create(request.longUrl)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header(HttpHeaders.LOCATION, "/${response.shortCode}")
            .body(response)
    }

    @GetMapping("/{code}")
    fun redirect(@PathVariable code: String): ResponseEntity<Any> {
        val link = service.resolve(code)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(error = "short code not found"))
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, link.longUrl)
            .build()
    }
}
