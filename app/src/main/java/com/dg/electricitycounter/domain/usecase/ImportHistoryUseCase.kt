package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ImportHistoryUseCase @Inject constructor(
    private val repository: ReadingRepository,
    private val preferencesHelper: PreferencesHelper
) {
    operator fun invoke(content: String): Flow<Result<Int>> = flow {
        try {
            val lines = content.trim().split("\n")
            val readings = mutableListOf<Reading>()
            var errorCount = 0
            
            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue
                
                // Пропускаем строки с META (для обратной совместимости)
                if (trimmedLine.startsWith("META|")) continue
                
                try {
                    val parts = trimmedLine.split("\\s+".toRegex())
                    
                    if (parts.size >= 5) {
                        val dateStr = parts[0] // dd.MM.yyyy
                        val current = parts[1].toDouble()
                        val consumption = parts[2].toDouble()
                        val tariff = parts[3].replace(',', '.').toDouble()
                        val amount = parts[4].replace(',', '.').toDouble()
                        val previous = current - consumption
                        
                        val timestamp = parseDate(dateStr)
                        
                        readings.add(
                            Reading(
                                date = timestamp,
                                previousReading = previous,
                                currentReading = current,
                                consumption = consumption,
                                tariff = tariff,
                                amount = amount,
                                address = "уч.143а"
                            )
                        )
                    } else {
                        errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }
            
            if (readings.isNotEmpty()) {
                // Заменяем всю историю новыми данными
                repository.importReadings(readings)
                
                // 🔧 БЕРЁМ ТАРИФ ИЗ ПЕРВОЙ (САМОЙ СВЕЖЕЙ) ЗАПИСИ
                val latestReading = readings.first()
                val latestTariff = String.format("%.2f", latestReading.tariff).replace(',', '.')
                val latestDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(Date(latestReading.date))
                
                // Сохраняем тариф и дату в настройки
                preferencesHelper.saveTariff(latestTariff)
                preferencesHelper.saveTariffChangeDate(latestDate)
                
                emit(Result.success(readings.size))
            } else {
                emit(Result.failure(Exception("Не найдено корректных записей (ошибок: $errorCount)")))
            }
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    private fun parseDate(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
