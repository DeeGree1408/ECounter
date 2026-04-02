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
        if (period == Period.ALL || period == Period.SPECIFIC_YEAR) return readings

        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH)  // 0-11
        val currentYear = currentCalendar.get(Calendar.YEAR)

        // Вычисляем начальный месяц для фильтрации
        val monthsToSubtract = when (period) {
            Period.THREE_MONTHS -> 2  // Текущий + 2 предыдущих = 3 месяца
            Period.SIX_MONTHS -> 5    // Текущий + 5 предыдущих = 6 месяцев
            Period.TWELVE_MONTHS -> 11 // Текущий + 11 предыдущих = 12 месяцев
            else -> 0
        }

        val startCalendar = Calendar.getInstance()
        startCalendar.set(Calendar.YEAR, currentYear)
        startCalendar.set(Calendar.MONTH, currentMonth)
        startCalendar.add(Calendar.MONTH, -monthsToSubtract)

        val startMonth = startCalendar.get(Calendar.MONTH)
        val startYear = startCalendar.get(Calendar.YEAR)

        return readings.filter { reading ->
            val readingCalendar = Calendar.getInstance()
            readingCalendar.timeInMillis = reading.date

            val readingMonth = readingCalendar.get(Calendar.MONTH)
            val readingYear = readingCalendar.get(Calendar.YEAR)

            // Фильтр: запись входит в диапазон?
            when {
                readingYear > currentYear -> false  // Будущее - исключаем
                readingYear < startYear -> false    // Слишком старое - исключаем
                readingYear == currentYear && readingMonth > currentMonth -> false  // Будущий месяц текущего года
                readingYear == startYear && readingMonth < startMonth -> false  // До начального месяца
                else -> true  // Попадает в диапазон
            }
        }
    }
}