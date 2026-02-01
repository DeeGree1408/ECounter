package com.dg.electricitycounter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long, // Timestamp
    val previousReading: Double,
    val currentReading: Double,
    val consumption: Double,
    val tariff: Double,
    val amount: Double,
    val address: String = "уч.143а"
)
