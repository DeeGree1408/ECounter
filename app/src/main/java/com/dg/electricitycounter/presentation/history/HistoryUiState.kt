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
    val minConsumption: Double,
    val maxConsumption: Double,
    val recordsCount: Int,
    val firstDate: String,
    val lastDate: String,
    val monthsCount: Int
)

fun List<Reading>.toStats(): HistoryStats {
    if (isEmpty()) {
        return HistoryStats(
            totalPaid = 0.0,
            totalConsumption = 0.0,
            averageConsumption = 0.0,
            averagePerYear = 0.0,
            minConsumption = 0.0,
            maxConsumption = 0.0,
            recordsCount = 0,
            firstDate = "",
            lastDate = "",
            monthsCount = 0
        )
    }

    val totalPaid = sumOf { it.amount }
    val totalConsumption = sumOf { it.consumption }
    val averageConsumption = totalConsumption / size
    val averagePerYear = averageConsumption * 12
    val minConsumption = minOf { it.consumption }
    val maxConsumption = maxOf { it.consumption }

    // Первая запись (самая старая) - в конце списка
    val firstReading = last()
    val firstCalendar = java.util.Calendar.getInstance()
    firstCalendar.timeInMillis = firstReading.date
    val firstMonth = java.text.SimpleDateFormat("LLLL yyyy", java.util.Locale("ru")).format(firstCalendar.time)
        .replaceFirstChar { it.uppercase() }

    // Последняя запись (самая новая) - в начале списка
    val lastReading = first()
    val lastCalendar = java.util.Calendar.getInstance()
    lastCalendar.timeInMillis = lastReading.date
    val lastMonth = java.text.SimpleDateFormat("LLLL yyyy", java.util.Locale("ru")).format(lastCalendar.time)
        .replaceFirstChar { it.uppercase() }

    return HistoryStats(
        totalPaid = totalPaid,
        totalConsumption = totalConsumption,
        averageConsumption = averageConsumption,
        averagePerYear = averagePerYear,
        minConsumption = minConsumption,
        maxConsumption = maxConsumption,
        recordsCount = size,
        firstDate = firstMonth,
        lastDate = lastMonth,
        monthsCount = size
    )
}
