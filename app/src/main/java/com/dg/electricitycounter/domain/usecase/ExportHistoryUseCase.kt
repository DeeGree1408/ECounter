package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExportHistoryUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(): Flow<Result<String>> = flow {
        try {
            val readings = repository.getAllReadings().first()
            val exportText = formatReadingsForExport(readings)
            emit(Result.success(exportText))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    private fun formatReadingsForExport(readings: List<Reading>): String {
        return readings.joinToString("\n") { reading ->
            val date = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(reading.date))
            "$date ${reading.currentReading.toInt()} ${reading.consumption.toInt()} ${String.format("%.2f", reading.tariff)} ${String.format("%.2f", reading.amount)}"
        }
    }
}
