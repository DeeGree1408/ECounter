package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import com.dg.electricitycounter.presentation.statistics.Period
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(period: Period): Flow<List<Reading>> = flow {
        try {
            val allReadings = repository.getAllReadings().first()
            val filteredReadings = filterByPeriod(allReadings, period)
            emit(filteredReadings)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    private fun filterByPeriod(readings: List<Reading>, period: Period): List<Reading> {
        if (period == Period.ALL) return readings
        
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        val periodStart = when (period) {
            Period.THREE_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.timeInMillis
            }
            Period.SIX_MONTHS -> {
                calendar.add(Calendar.MONTH, -6)
                calendar.timeInMillis
            }
            Period.TWELVE_MONTHS -> {
                calendar.add(Calendar.MONTH, -12)
                calendar.timeInMillis
            }
            Period.LAST_YEAR -> {
                val lastYear = Calendar.getInstance().get(Calendar.YEAR) - 1
                calendar.set(Calendar.YEAR, lastYear)
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val end = calendar.timeInMillis
                
                return readings.filter { it.date in start..end }
            }
            Period.ALL -> return readings
        }
        
        return readings.filter { it.date >= periodStart }
    }
}
