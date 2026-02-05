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
                val startCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, lastYear)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = startCalendar.timeInMillis

                val endCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, lastYear)
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = endCalendar.timeInMillis

                return readings.filter { it.date >= start && it.date <= end }
            }

            Period.ALL -> return readings
        }
        
        return readings.filter { it.date >= periodStart }
    }
}
