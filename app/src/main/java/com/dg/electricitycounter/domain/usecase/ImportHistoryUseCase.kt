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
            
            // Проверяем первую строку на метаданные
            var startIndex = 0
            if (lines.isNotEmpty() && lines[0].startsWith("META|")) {
                parseMetadata(lines[0])
                startIndex = 1
            }
            
            // Парсим записи
            for (i in startIndex until lines.size) {
                val line = lines[i]
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue
                
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
                
                // 🔧 ИЩЕМ ПЕРВОЕ ИЗМЕНЕНИЕ ТАРИФА
                val latestTariff = readings.first().tariff
                
                // Находим последнюю (самую раннюю по дате) запись с этим тарифом
                val firstTariffChange = readings.lastOrNull { it.tariff == latestTariff }
                
                if (firstTariffChange != null) {
                    val tariffValue = String.format("%.2f", firstTariffChange.tariff).replace(',', '.')
                    val tariffDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .format(Date(firstTariffChange.date))
                    
                    // Сохраняем тариф и дату первого изменения
                    preferencesHelper.saveTariff(tariffValue)
                    preferencesHelper.saveTariffChangeDate(tariffDate)
                }
                
                emit(Result.success(readings.size))
            } else {
                emit(Result.failure(Exception("Не найдено корректных записей (ошибок: $errorCount)")))
            }
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    private fun parseMetadata(metaLine: String) {
        try {
            // Формат: META|6.95|25.01.2026
            val parts = metaLine.split("|")
            if (parts.size >= 3) {
                val tariff = parts[1]
                val tariffDate = parts[2]
                
                // Сохраняем тариф и дату
                preferencesHelper.saveTariff(tariff)
                preferencesHelper.saveTariffChangeDate(tariffDate)
            }
        } catch (e: Exception) {
            // Игнорируем ошибки парсинга метаданных
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
