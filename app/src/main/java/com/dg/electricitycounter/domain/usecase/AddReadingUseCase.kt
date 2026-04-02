package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AddReadingUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(
        previous: Double,
        current: Double,
        tariff: Double
    ): Flow<Result<Reading>> = flow {
        try {
            val consumption = current - previous
            val amount = consumption * tariff

            // Определяем месяц показаний
            val today = java.util.Calendar.getInstance()
            val dayOfMonth = today.get(java.util.Calendar.DAY_OF_MONTH)

// Если день < 15, то показания за предыдущий месяц
            val readingMonth = if (dayOfMonth < 15) {
                today.apply { add(java.util.Calendar.MONTH, -1) }
            } else {
                today
            }

// Устанавливаем последний день месяца показаний
            readingMonth.set(java.util.Calendar.DAY_OF_MONTH, readingMonth.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            readingMonth.set(java.util.Calendar.HOUR_OF_DAY, 0)
            readingMonth.set(java.util.Calendar.MINUTE, 0)
            readingMonth.set(java.util.Calendar.SECOND, 0)
            readingMonth.set(java.util.Calendar.MILLISECOND, 0)

            val reading = Reading(
                date = readingMonth.timeInMillis,  // ← последний день месяца показаний
                previousReading = previous,
                currentReading = current,
                consumption = consumption,
                tariff = tariff,
                amount = amount
            )

            repository.addReading(reading)
            emit(Result.success(reading))
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
