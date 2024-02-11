package com.itstor.wifimapper.models

import kotlinx.serialization.Serializable

class SparseGrid<T>(private val initialValue: T) {
    private val map = HashMap<Position, T>()
    private var origin = Position(0, 0)

    private var minRow: Int? = null
    private var maxRow: Int? = null
    private var minCol: Int? = null
    private var maxCol: Int? = null

    private var minMaxDirty = false

    /**
     * Converts an internal position to an external position.
     *
     * The internal position is based on the grid's origin, while the external position is relative to the grid's origin.
     * This method is used when we want to expose a position to the outside.
     *
     * @param x the x-coordinate of the internal position
     * @param y the y-coordinate of the internal position
     * @return a Position object representing the external position
     */
    private fun toExternalPosition(x: Int, y: Int): Position {
        return Position(x - origin.x, y - origin.y)
    }

    /**
     * Converts an internal position to an external position.
     *
     * The internal position is based on the grid's origin, while the external position is relative to the grid's origin.
     * This method is used when we want to expose a position to the outside.
     *
     * @param position the internal position
     * @return a Position object representing the external position
     */
    private fun toExternalPosition(position: Position): Position {
        return position - origin
    }

    /**
     * Converts an external position to an internal position.
     *
     * The internal position is based on the grid's origin, while the external position is relative to the grid's origin.
     * This method is used when we want to convert an external position to an internal position.
     *
     * @param x the x-coordinate of the external position
     * @param y the y-coordinate of the external position
     * @return a Position object representing the internal position
     */
    private fun toInternalPosition(x: Int, y: Int): Position {
        return Position(x + origin.x, y + origin.y)
    }

    /**
     * Converts an external position to an internal position.
     *
     * The internal position is based on the grid's origin, while the external position is relative to the grid's origin.
     * This method is used when we want to convert an external position to an internal position.
     *
     * @param position the external position
     * @return a Position object representing the internal position
     */
    private fun toInternalPosition(position: Position): Position {
        return position + origin
    }


    /**
     * Updates the minimum and maximum row and column values.
     *
     * This method is used to recalculate the minimum and maximum row and column values based on the current positions in the map.
     */
    private fun updateMinMax() {
        minRow = null
        maxRow = null
        minCol = null
        maxCol = null

        map.keys.forEach { position ->
            minRow = minOf(minRow ?: position.x, position.x)
            maxRow = maxOf(maxRow ?: position.x, position.x)
            minCol = minOf(minCol ?: position.y, position.y)
            maxCol = maxOf(maxCol ?: position.y, position.y)
        }

        minMaxDirty = false
    }

    inner class Row(private val x: Int) {
        operator fun get(y: Int): T {
            return map[toInternalPosition(x, y)] ?: initialValue
        }

        operator fun set(y: Int, value: T) {
            val internalPos = toInternalPosition(x, y)
            map[internalPos] = value
            minMaxDirty = true
        }
    }

    operator fun get(x: Int): Row {
        return Row(x)
    }

    fun get(x: Int, y: Int): T? {
        return map[toInternalPosition(x, y)]
    }

    fun get(position: Position): T? {
        return map[toInternalPosition(position.x, position.y)]
    }

    fun remove(position: Position) {
        val internalPos = toInternalPosition(position)

        map.remove(internalPos)
        minMaxDirty = true
    }

    fun put(position: Position, value: T) {
        map[toInternalPosition(position)] = value
        minMaxDirty = true
    }

    fun shiftOrigin(newOriginX: Int, newOriginY: Int) {
        origin = toInternalPosition(newOriginX, newOriginY)
        minMaxDirty = true
    }

    fun shiftOrigin(newOrigin: Position) {
        origin = toInternalPosition(newOrigin)
        minMaxDirty = true
    }

    fun size(): Int {
        return map.size
    }

    fun clear() {
        map.clear()
        minRow = null
        maxRow = null
        minCol = null
        maxCol = null
    }

    fun forEachElement(action: (Position, T) -> Unit) {
        map.forEach { (position, value) -> action(toExternalPosition(position), value) }
    }

    fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    fun find(predicate: (Position, T) -> Boolean): Pair<Position, T>? {
        return map.entries.find { (position, value) ->
            predicate(
                toExternalPosition(position),
                value
            )
        }?.let { Pair(it.key, it.value) }
    }

    fun sumOf(predicate: (Position, T) -> Int): Int {
        return map.entries.sumOf { (position, value) ->
            predicate(
                toExternalPosition(position),
                value
            )
        }
    }

    /**
     * Calculates the center of the grid.
     *
     * This method is used to find the center position of the grid.
     *
     * @return a Position object representing the center of the grid, or null if the minimum and maximum row and column values are null
     */
    fun centerOfGrid(): Position? {
        if (minRow == null || minCol == null || maxRow == null || maxCol == null) {
            return null
        }
        val centerX = (minRow!! + maxRow!!) / 2
        val centerY = (minCol!! + maxCol!!) / 2

        return toExternalPosition(Position(centerX, centerY))
    }

    fun minOfRow(): Int {
        if (minMaxDirty) {
            updateMinMax()
        }
        return minRow ?: 0
    }

    fun maxOfRow(): Int {
        if (minMaxDirty) {
            updateMinMax()
        }
        return maxRow ?: 0
    }

    fun minOfCol(): Int {
        if (minMaxDirty) {
            updateMinMax()
        }
        return minCol ?: 0
    }

    fun maxOfCol(): Int {
        if (minMaxDirty) {
            updateMinMax()
        }
        return maxCol ?: 0
    }

    fun toExportFormat(): List<ExportFormat<T>> {
        val data = map.map { (position, value) ->
            val externalPosition = toExternalPosition(position)
            ExportFormat(externalPosition.x, externalPosition.y, value)
        }

        return data
    }

    @Serializable
    data class ExportFormat<T>(val x: Int, val y: Int, val value: T)
}
