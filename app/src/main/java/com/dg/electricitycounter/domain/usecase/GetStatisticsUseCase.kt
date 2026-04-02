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
    operator fun invoke(period: Period, selectedYear: Int? = null): Flow<List<Reading>> = flow {
        try {
            val allReadings = repository.getAllReadings().first()
            val filteredReadings = filterByPeriod(allReadings, period, selectedYear)
            emit(filteredReadings)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    private fun filterByPeriod(
        readings: List<Reading>,
        period: Period,
        selectedYear: Int?
    ): List<Reading> {
        // Для "ВСЕ" возвращаем все записи
        if (period == Period.ALL) return readings

        // Для конкретного года фильтруем по году
        if (period == Period.SPECIFIC_YEAR && selectedYear != null) {
            return readings.filter { reading ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = reading.date
                calendar.get(Calendar.YEAR) == selectedYear
            }
        }

        // Для периодов 3/6/12 месяцев - берём N последних записей
        val count = when (period) {
            Period.THREE_MONTHS -> 3
            Period.SIX_MONTHS -> 6
            Period.TWELVE_MONTHS -> 12
            else -> readings.size
        }

        return readings.take(count)
    }
}
