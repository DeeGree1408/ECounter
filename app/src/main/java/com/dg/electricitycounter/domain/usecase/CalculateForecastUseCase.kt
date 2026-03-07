package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import com.dg.electricitycounter.presentation.statistics.Forecast
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

/**
 * UseCase для расчета прогноза расхода на текущий месяц
 * на основе исторических данных за этот же месяц из предыдущих лет
 */
class CalculateForecastUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    /**
     * Рассчитывает прогноз расхода на текущий месяц
     *
     * Логика:
     * - Определяем текущий месяц (например, март)
     * - Находим все записи за март из всех предыдущих лет
     * - Вычисляем среднее значение расхода
     * - Это и будет прогноз на текущий март
     *
     * @param allReadings - все записи (не используется, оставлено для совместимости)
     * @param totalConsumption - общий расход (не используется, оставлено для совместимости)
     * @return Прогноз на текущий месяц (или null если данных нет)
     */
    suspend operator fun invoke(
        @Suppress("UNUSED_PARAMETER") allReadings: List<Reading>,
        @Suppress("UNUSED_PARAMETER") totalConsumption: Double
    ): Forecast? {
        // Используем все записи из репозитория
        val readings = repository.getAllReadings().first()
        if (readings.isEmpty()) return null

        // Определяем текущий месяц и год
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1-12 (Calendar.MONTH = 0-11)
        val currentYear = calendar.get(Calendar.YEAR)

        // Находим все записи за текущий месяц из ПРЕДЫДУЩИХ лет
        val historicalReadingsForThisMonth = readings.filter { reading ->
            val readingCalendar = Calendar.getInstance()
            readingCalendar.timeInMillis = reading.date
            val readingMonth = readingCalendar.get(Calendar.MONTH) + 1
            val readingYear = readingCalendar.get(Calendar.YEAR)

            // Берем тот же месяц, но из предыдущих лет (не текущий год!)
            readingMonth == currentMonth && readingYear < currentYear
        }

        // Если нет исторических данных за этот месяц, возвращаем null
        if (historicalReadingsForThisMonth.isEmpty()) return null

        // Вычисляем средний расход за этот месяц по всем предыдущим годам
        val averageConsumption = historicalReadingsForThisMonth
            .map { it.consumption }
            .average()
            .toInt()

        // Получаем текущий тариф из последней записи
        val currentTariff = readings.firstOrNull()?.tariff ?: 0.0

        // Рассчитываем ожидаемую сумму
        val expectedAmount = averageConsumption * currentTariff

        // Название текущего месяца
        val monthName = getMonthName(currentMonth)

        return Forecast(
            nextMonth = monthName,
            expectedConsumption = averageConsumption,
            expectedAmount = expectedAmount
        )
    }

    /**
     * Получить название месяца по его номеру (1-12)
     */
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Январь"
            2 -> "Февраль"
            3 -> "Март"
            4 -> "Апрель"
            5 -> "Май"
            6 -> "Июнь"
            7 -> "Июль"
            8 -> "Август"
            9 -> "Сентябрь"
            10 -> "Октябрь"
            11 -> "Ноябрь"
            12 -> "Декабрь"
            else -> "Неизвестно"
        }
    }
}
