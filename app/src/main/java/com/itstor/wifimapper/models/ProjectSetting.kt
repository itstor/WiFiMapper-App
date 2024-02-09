package com.itstor.wifimapper.models

import kotlinx.serialization.Serializable

@Serializable
data class ProjectSetting(
    var projectName: String,
    var wifiRegex: String,
    var wifiScanInterval: Int,
    var distanceBetweenPoints: Int,
    var stopScanAt: Int,
    var createdAt: Long
)
