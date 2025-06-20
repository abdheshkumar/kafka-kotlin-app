package com.abtech.consumer

import arrow.atomic.update
import java.util.concurrent.atomic.AtomicReference

class InMemoryCache {
    private val data: AtomicReference<List<String>> = AtomicReference(emptyList())

    fun add(value: String): Unit = data.update { it + value }

    fun getAll(): List<String> = data.get()
}
