package com.itstor.wifimapper.models

import kotlinx.serialization.Serializable

@Serializable
data class WiFiPoint(
    val status: PointStatus = PointStatus.UNRECORDED,
    val data: List<WiFiData> = listOf(),
    val scannedAt: Long = 0,
    val scanDuration: Long = 0,
    val recordedDataCount: Int = 0
)
