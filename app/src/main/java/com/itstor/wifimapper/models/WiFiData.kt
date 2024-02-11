package com.itstor.wifimapper.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
data class WiFiData(
    @SerialName("ssid")
    val ssid: String,

    @SerialName("bssid")
    val bssid: String,

    @SerialName("rssi")
    val rssi: Int,

    @SerialName("frequency")
    val frequency: Int,

    @SerialName("row")
    val row: Int = 0,
)
