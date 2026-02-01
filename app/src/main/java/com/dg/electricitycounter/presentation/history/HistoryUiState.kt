package com.dg.electricitycounter.presentation.history

import com.dg.electricitycounter.domain.model.Reading

data class HistoryUiState(
    val readings: List<Reading> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false
)

data class HistoryStats(
    val totalPaid: Double,
    val totalConsumption: Double,
    val averageConsumption: Double,
    val averagePerYear: Double,
    val recordsCount: Int
)

fun List<Reading>.toStats(): HistoryStats {
    val totalPaid = sumOf { it.amount }
    val totalConsumption = sumOf { it.consumption }
    val averageConsumption = if (isNotEmpty()) totalConsumption / size else 0.0
    val averagePerYear = averageConsumption * 12
    
    return HistoryStats(
        totalPaid = totalPaid,
        totalConsumption = totalConsumption,
        averageConsumption = averageConsumption,
        averagePerYear = averagePerYear,
        recordsCount = size
    )
}
