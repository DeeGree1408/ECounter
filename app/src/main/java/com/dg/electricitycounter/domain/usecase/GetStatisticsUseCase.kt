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

                // Для 2025 года нужны записи:
                // - с датой в 2025 (кроме начала января - это показания за декабрь 2024)
                // - с датой начала 2026 (показания за декабрь 2025)

                return readings.filter { reading ->
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = reading.date
                    }
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    when {
                        // Записи с датой в нужном году (кроме начала января)
                        year == lastYear && !(month == Calendar.JANUARY && day < 15) -> true
                        // Начало следующего года (показания за декабрь нужного года)
                        year == lastYear + 1 && month == Calendar.JANUARY && day < 15 -> true
                        else -> false
                    }
                }
            }
            Period.ALL -> return readings
        }

        return readings.filter { it.date >= periodStart }
    }
}
