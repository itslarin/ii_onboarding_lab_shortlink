package com.example.shortlink.link

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

/**
 * In-memory хранилище коротких ссылок: ключ — shortCode.
 * Живёт в памяти процесса, при рестарте очищается.
 */
@Repository
class LinkRepository {

    private val store = ConcurrentHashMap<String, Link>()

    fun save(link: Link): Link {
        store[link.shortCode] = link
        return link
    }

    fun findByCode(shortCode: String): Link? = store[shortCode]
}
