package com.dg.electricitycounter.presentation.statistics

import com.dg.electricitycounter.domain.model.Reading

data class StatisticsUiState(
    val selectedPeriod: Period = Period.SIX_MONTHS,
    val readings: List<Reading> = emptyList(),
    val stats: PeriodStats? = null,
    val forecast: Forecast? = null,
    val tariffHistory: List<TariffChange> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class Period {
    THREE_MONTHS,
    SIX_MONTHS,
    TWELVE_MONTHS,
    LAST_YEAR,
    ALL
}

data class PeriodStats(
    val totalPaid: Double,
    val totalConsumption: Double,
    val averageConsumption: Double,
    val minConsumption: Double,
    val maxConsumption: Double,
    val monthlyData: List<MonthData>
)

data class MonthData(
    val month: String,
    val consumption: Double,
    val isAboveAverage: Boolean
)

data class Forecast(
    val nextMonth: String,
    val expectedConsumption: Int,
    val expectedAmount: Double
)

data class TariffChange(
    val tariff: Double,
    val date: String,
    val isCurrent: Boolean
)
