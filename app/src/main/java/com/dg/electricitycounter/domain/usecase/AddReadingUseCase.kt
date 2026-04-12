package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

// 🔥 РЕЗУЛЬТАТЫ ОПЕРАЦИИ
sealed class AddReadingResult {
    data class Success(val reading: Reading) : AddReadingResult()
    data class NeedReplacement(val existingReading: Reading, val newReading: Reading) : AddReadingResult()
    data class OutsidePeriod(val currentDay: Int) : AddReadingResult()
    data class Error(val message: String) : AddReadingResult()
}

class AddReadingUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(
        previous: Double,
        current: Double,
        tariff: Double
    ): Flow<AddReadingResult> = flow {
        try {
            val today = Calendar.getInstance()
            val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)

            // 🔥 ПРОВЕРКА ПЕРИОДА ВВОДА (24 - 3 число)
            val isValidPeriod = dayOfMonth >= 24 || dayOfMonth <= 3

            if (!isValidPeriod) {
                emit(AddReadingResult.OutsidePeriod(dayOfMonth))
                return@flow
            }

            val consumption = current - previous
            val amount = consumption * tariff

            // Определяем месяц показаний
            val readingMonth = if (dayOfMonth <= 3) {
                // Если 1-3 число - показания за предыдущий месяц
                today.apply { add(Calendar.MONTH, -1) }
            } else {
                // Если 24-31 число - показания за текущий месяц
                today
            }

            // Устанавливаем последний день месяца показаний
            readingMonth.set(Calendar.DAY_OF_MONTH, readingMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
            readingMonth.set(Calendar.HOUR_OF_DAY, 0)
            readingMonth.set(Calendar.MINUTE, 0)
            readingMonth.set(Calendar.SECOND, 0)
            readingMonth.set(Calendar.MILLISECOND, 0)

            val targetDate = readingMonth.timeInMillis

            val newReading = Reading(
                date = targetDate,
                previousReading = previous,
                currentReading = current,
                consumption = consumption,
                tariff = tariff,
                amount = amount
            )

            // 🔥 ПРОВЕРКА ДУБЛИКАТА: есть ли уже запись за этот месяц?
            val allReadings = repository.getAllReadings().first()
            val existingReading = allReadings.find { it.date == targetDate }

            if (existingReading != null) {
                // Запись уже существует - нужно подтверждение замены
                emit(AddReadingResult.NeedReplacement(existingReading, newReading))
            } else {
                // Записи нет - сохраняем новую
                repository.addReading(newReading)
                emit(AddReadingResult.Success(newReading))
            }

        } catch (e: Exception) {
            emit(AddReadingResult.Error(e.message ?: "Неизвестная ошибка"))
        }
    }
}
