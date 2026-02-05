package com.dg.electricitycounter.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.usecase.CalculateForecastUseCase
import com.dg.electricitycounter.domain.usecase.GetStatisticsUseCase
import com.dg.electricitycounter.domain.usecase.GetTariffHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.dg.electricitycounter.domain.model.Reading


@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val calculateForecastUseCase: CalculateForecastUseCase,
    private val getTariffHistoryUseCase: GetTariffHistoryUseCase,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
    
    init {
        loadStatistics(Period.SIX_MONTHS)
    }
    
    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStatistics(period)
    }
    
    private fun loadStatistics(period: Period) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getStatisticsUseCase(period)
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
                .collect { readings ->
                    val stats = calculateStats(readings)
                    val currentTariff = preferencesHelper.getTariff().toDoubleOrNull() ?: 6.84
                    val forecast = if (period == Period.THREE_MONTHS || period == Period.SIX_MONTHS || period == Period.TWELVE_MONTHS) {
                        calculateForecastUseCase(readings, currentTariff)
                    } else {
                        null
                    }

                    val tariffHistory = getTariffHistoryUseCase(readings, currentTariff)
                    
                    _uiState.update {
                        it.copy(
                            readings = readings,
                            stats = stats,
                            forecast = forecast,
                            tariffHistory = tariffHistory,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun calculateStats(readings: List<Reading>): PeriodStats {
        if (readings.isEmpty()) {
            return PeriodStats(
                totalPaid = 0.0,
                totalConsumption = 0.0,
                averageConsumption = 0.0,
                minConsumption = 0.0,
                maxConsumption = 0.0,
                monthlyData = emptyList()
            )
        }

        val totalPaid: Double = readings.sumOf { it.amount }
        val totalConsumption: Double = readings.sumOf { it.consumption }
        val averageConsumption: Double = totalConsumption / readings.size
        val minConsumption: Double = readings.minOf { it.consumption }
        val maxConsumption: Double = readings.maxOf { it.consumption }

        val monthlyData = readings.map { reading ->
            val monthName = SimpleDateFormat("LLL", Locale("ru")).format(Date(reading.date))
                .replaceFirstChar { it.uppercase() }
                .take(3)


            MonthData(
                month = monthName,
                consumption = reading.consumption,
                isAboveAverage = reading.consumption > averageConsumption
            )
        }.reversed()

        return PeriodStats(
            totalPaid = totalPaid,
            totalConsumption = totalConsumption,
            averageConsumption = averageConsumption,
            minConsumption = minConsumption,
            maxConsumption = maxConsumption,
            monthlyData = monthlyData
        )
    }

}
