package com.dg.electricitycounter.presentation.calculator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.ReminderScheduler
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.usecase.AddReadingUseCase
import com.dg.electricitycounter.domain.usecase.GetLatestReadingUseCase
import com.dg.electricitycounter.util.formatToDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class CalculatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addReadingUseCase: AddReadingUseCase,
    private val getLatestReadingUseCase: GetLatestReadingUseCase,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    // 🔧 СДЕЛАЛИ ПУБЛИЧНЫМ - ТЕПЕРЬ МОЖНО ВЫЗЫВАТЬ ИЗ SCREEN
    fun loadData() {
        viewModelScope.launch {
            // Загружаем настройки
            val tariff = preferencesHelper.getTariff()
            val tariffChangeDate = preferencesHelper.getTariffChangeDate()
            val isTariffLocked = preferencesHelper.isTariffLocked()
            val isPreviousLocked = preferencesHelper.isPreviousLocked()
            
            _uiState.update {
                it.copy(
                    tariff = tariff,
                    tariffChangeDate = tariffChangeDate,
                    isTariffLocked = isTariffLocked,
                    isPreviousLocked = isPreviousLocked
                )
            }
            
            // Загружаем последнее показание
            getLatestReadingUseCase()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { reading ->
                    _uiState.update { state ->
                        state.copy(
                            previousReading = reading?.currentReading?.toInt()?.toString() ?: "",
                            lastReadingDate = reading?.date?.formatToDisplay() ?: "",
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun onCurrentReadingChange(value: String) {
        _uiState.update { it.copy(currentReading = value, error = null) }
    }
    
    fun onTariffChange(value: String) {
        val oldTariff = _uiState.value.tariff
        _uiState.update { it.copy(tariff = value, error = null) }
        
        // Сохраняем тариф
        preferencesHelper.saveTariff(value)
        
        // Если тариф изменился - обновляем дату
        if (oldTariff != value && value.isNotEmpty()) {
            val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            preferencesHelper.saveTariffChangeDate(currentDate)
            _uiState.update { it.copy(tariffChangeDate = currentDate) }
        }
    }
    
    fun onPreviousReadingChange(value: String) {
        _uiState.update { it.copy(previousReading = value, error = null) }
    }
    
    fun toggleTariffLock() {
        val newState = !_uiState.value.isTariffLocked
        _uiState.update { it.copy(isTariffLocked = newState) }
        preferencesHelper.setTariffLocked(newState)
    }
    
    fun togglePreviousLock() {
        val newState = !_uiState.value.isPreviousLocked
        _uiState.update { it.copy(isPreviousLocked = newState) }
        preferencesHelper.setPreviousLocked(newState)
    }
    
    fun submitReading() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Validation
            val current = state.currentReading.toDoubleOrNull()
            val previous = state.previousReading.toDoubleOrNull()
            val tariff = state.tariff.toDoubleOrNull()
            
            if (current == null || previous == null || tariff == null) {
                _uiState.update {
                    it.copy(error = "❌ Заполните все поля корректными числами!")
                }
                return@launch
            }
            
            if (current < previous) {
                _uiState.update {
                    it.copy(
                        error = "⚠️ ВНИМАНИЕ!\nТекущие показания меньше предыдущих.\nВозможно, был сброс счётчика.",
                        showResult = true
                    )
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true) }
            
            addReadingUseCase(
                previous = previous,
                current = current,
                tariff = tariff
            ).collect { result ->
                result.onSuccess { reading ->
                    _uiState.update {
                        it.copy(
                            currentReading = "",
                            previousReading = current.toInt().toString(),
                            lastReadingDate = reading.date.formatToDisplay(),
                            resultText = formatResult(reading),
                            showResult = true,
                            error = null,
                            isLoading = false,
                            isPreviousLocked = true
                        )
                    }
                    // Обновляем блокировку
                    preferencesHelper.setPreviousLocked(true)
                    
                    // ОСТАНАВЛИВАЕМ НАПОМИНАНИЯ ПОСЛЕ ВВОДА ПОКАЗАНИЙ
                    stopRemindersIfEnabled()
                }
                result.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
    
    private fun stopRemindersIfEnabled() {
        if (preferencesHelper.isReminderEnabled()) {
            val scheduler = ReminderScheduler(context)
            scheduler.cancelAllReminders()
            scheduler.scheduleMonthlyReminder()
        }
    }
    
    private fun formatResult(reading: Reading): String {
        return """
            📊 ПОКАЗАНИЯ ПЕРЕДАНЫ
            
            📈 ИЗРАСХОДОВАНО: ${String.format("%.1f", reading.consumption)} кВт·ч
            💰 ТАРИФ: ${String.format("%.2f", reading.tariff)} ₽/кВт·ч
            🏦 СУММА К ОПЛАТЕ: ${String.format("%.2f", reading.amount)} ₽
            
            📅 Дата передачи: ${reading.date.formatToDisplay()}
            🔄 Показания: ${reading.previousReading.toInt()} → ${reading.currentReading.toInt()}
            
            ✅ Предыдущие показания обновлены
            ✅ Запись добавлена в историю
            ${if (preferencesHelper.isReminderEnabled()) "\n🔕 Напоминания остановлены до следующего месяца" else ""}
        """.trimIndent()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
