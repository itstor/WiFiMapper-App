package com.itstor.wifimapper.models

import kotlinx.serialization.Serializable

@Serializable
data class DataFormat(
    val projectName: String,
    val wifiRegex: String,
    val wifiScanInterval: Int,
    val distanceBetweenPoints: Int,
    val stopScanAt: Int,
    val createdAt: Long,
    val data: List<SparseGrid.ExportFormat<WiFiPoint>>
)