package dev.amenokizele.tervyn.data.local.repository

import java.util.UUID

interface LocalIdGenerator {
    fun newId(): String
}

class UuidLocalIdGenerator : LocalIdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
