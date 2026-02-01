package com.dg.electricitycounter.domain.repository

import com.dg.electricitycounter.domain.model.Reading
import kotlinx.coroutines.flow.Flow

interface ReadingRepository {
    fun getAllReadings(): Flow<List<Reading>>
    fun getLatestReading(): Flow<Reading?>
    suspend fun addReading(reading: Reading)
    suspend fun deleteLatestReading()
    suspend fun importReadings(readings: List<Reading>)
}
