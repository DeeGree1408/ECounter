package com.dg.electricitycounter.data.repository

import com.dg.electricitycounter.data.local.dao.ReadingDao
import com.dg.electricitycounter.data.mapper.toDomain
import com.dg.electricitycounter.data.mapper.toEntity
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReadingRepositoryImpl @Inject constructor(
    private val dao: ReadingDao
) : ReadingRepository {
    
    override fun getAllReadings(): Flow<List<Reading>> {
        return dao.getAllReadingsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getLatestReading(): Flow<Reading?> {
        return dao.getLatestFlow().map { it?.toDomain() }
    }
    
    override suspend fun addReading(reading: Reading) {
        dao.insert(reading.toEntity())
    }
    
    override suspend fun deleteLatestReading() {
        dao.deleteLatest()
    }
    
    override suspend fun importReadings(readings: List<Reading>) {
        dao.deleteAll()
        dao.insertAll(readings.map { it.toEntity() })
    }
}
