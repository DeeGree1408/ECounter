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

        return when (period) {
            Period.THREE_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                readings.filter { it.date >= calendar.timeInMillis }
            }
            Period.SIX_MONTHS -> {
                calendar.add(Calendar.MONTH, -6)
                readings.filter { it.date >= calendar.timeInMillis }
            }
            Period.TWELVE_MONTHS -> {
                calendar.add(Calendar.MONTH, -12)
                readings.filter { it.date >= calendar.timeInMillis }
            }
            Period.LAST_YEAR -> {
                val lastYear = Calendar.getInstance().get(Calendar.YEAR) - 1

                readings.filter { reading ->
                    val readingCalendar = Calendar.getInstance().apply {
                        timeInMillis = reading.date
                    }
                    val year = readingCalendar.get(Calendar.YEAR)
                    val month = readingCalendar.get(Calendar.MONTH)
                    val day = readingCalendar.get(Calendar.DAY_OF_MONTH)

                    when {
                        // Записи в нужном году (кроме начала января)
                        year == lastYear && !(month == Calendar.JANUARY && day < 15) -> true
                        // Начало следующего года (показания за декабрь)
                        year == lastYear + 1 && month == Calendar.JANUARY && day < 15 -> true
                        else -> false
                    }
                }
            }
            Period.ALL -> readings
        }
    }
}
