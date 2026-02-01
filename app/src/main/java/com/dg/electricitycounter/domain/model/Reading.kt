package com.dg.electricitycounter.domain.model

data class Reading(
    val id: Int = 0,
    val date: Long,
    val previousReading: Double,
    val currentReading: Double,
    val consumption: Double,
    val tariff: Double,
    val amount: Double,
    val address: String = "уч.143а"
)
