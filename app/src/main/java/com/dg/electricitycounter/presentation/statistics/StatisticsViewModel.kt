package com.dg.electricitycounter.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
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
        loadAvailableYears()
    }

    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period, selectedYear = null) }
        loadStatistics(period)
    }

    fun selectYear(year: Int) {
        _uiState.update {
            it.copy(
                selectedPeriod = Period.SPECIFIC_YEAR,
                selectedYear = year
            )
        }
        loadStatistics(Period.SPECIFIC_YEAR, year)
    }

    private fun loadAvailableYears() {
        viewModelScope.launch {
            getStatisticsUseCase(Period.ALL)
                .catch { }
                .collect { allReadings ->
                    val years = getAvailableYears(allReadings)
                    _uiState.update { it.copy(availableYears = years) }
                }
        }
    }

    private fun getAvailableYears(readings: List<Reading>): List<Int> {
        return readings.map { reading ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = reading.date
            calendar.get(Calendar.YEAR)
        }.distinct().sortedDescending()
    }

    private fun loadStatistics(period: Period, specificYear: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Для конкретного года загружаем все данные, а потом фильтруем
            val periodToLoad = if (period == Period.SPECIFIC_YEAR) Period.ALL else period

            getStatisticsUseCase(periodToLoad)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
                .collect { readings ->
                    // Фильтруем по выбранному году, если нужно
                    val filteredReadings = if (period == Period.SPECIFIC_YEAR && specificYear != null) {
                        readings.filter { reading ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = reading.date
                            val readingYear = calendar.get(Calendar.YEAR)
                            readingYear == specificYear
                        }
                    } else {
                        readings
                    }

                    val stats = calculateStats(filteredReadings, period)
                    val currentTariff = preferencesHelper.getTariff().toDoubleOrNull() ?: 6.84

                    val forecast = if (period == Period.THREE_MONTHS || period == Period.SIX_MONTHS || period == Period.TWELVE_MONTHS) {
                        calculateForecastUseCase(filteredReadings, currentTariff)
                    } else {
                        null
                    }

                    _uiState.update {
                        it.copy(
                            readings = filteredReadings,
                            stats = stats,
                            forecast = forecast,
                            isLoading = false,
                            error = null
                        )
                    }

                    // История тарифов - загружаем отдельно из ВСЕХ данных
                    loadTariffHistory(currentTariff)
                }
        }
    }

    private fun loadTariffHistory(currentTariff: Double) {
        viewModelScope.launch {
            getStatisticsUseCase(Period.ALL)
                .catch { }
                .collect { allReadings ->
                    val tariffHistory = getTariffHistoryUseCase(allReadings, currentTariff)
                    _uiState.update { it.copy(tariffHistory = tariffHistory) }
                }
        }
    }

    private fun calculateStats(readings: List<Reading>, period: Period): PeriodStats {
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
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = reading.date

            // Для периода ALL - показываем только год
            val monthName = if (period == Period.ALL) {
                calendar.get(Calendar.YEAR).toString()
            } else {
                SimpleDateFormat("LLL", Locale("ru")).format(calendar.time)
                    .replaceFirstChar { it.uppercase() }
                    .take(3)
            }

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
