package com.itstor.wifimapper.models

import kotlinx.serialization.Serializable

@Serializable
enum class PointStatus {
    UNRECORDED,
    RECORDED
}