package com.itstor.wifimapper.models

import com.google.gson.Gson

data class Position(val x: Int, val y: Int) {
    operator fun times(scale: Int) = Position((x * scale), (y * scale))
    operator fun plus(other: Position) = Position(x + other.x, y + other.y)
    operator fun minus(other: Position) = Position(x - other.x, y - other.y)
    operator fun div(other: Position) = Position(x / other.x, y / other.y)
    operator fun div(other: Int) = Position(x / other, y / other)

    fun toJson(): String {
        val gson = Gson()

        return gson.toJson(this)
    }
}
