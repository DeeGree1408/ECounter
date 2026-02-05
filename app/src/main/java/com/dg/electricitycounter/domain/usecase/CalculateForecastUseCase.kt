package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.presentation.statistics.Forecast
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CalculateForecastUseCase @Inject constructor() {
    
    operator fun invoke(readings: List<Reading>, currentTariff: Double): Forecast? {
        if (readings.size < 3) return null
        
        // Берём последние 3 месяца для прогноза
        val lastThreeMonths = readings.take(3)
        
        // Средний расход за 3 месяца
        val averageConsumption = lastThreeMonths.sumOf { it.consumption } / 3
        
        // Ожидаемая сумма по текущему тарифу
        val expectedAmount = averageConsumption * currentTariff
        
        // Следующий месяц
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        val nextMonth = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(calendar.time)
            .replaceFirstChar { it.uppercase() }


        return Forecast(
            nextMonth = nextMonth,
            expectedConsumption = averageConsumption.toInt(),
            expectedAmount = expectedAmount
        )
    }
}
