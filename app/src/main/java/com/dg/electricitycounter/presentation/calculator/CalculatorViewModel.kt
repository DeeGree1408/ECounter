package com.dg.electricitycounter.presentation.calculator

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.ReminderScheduler
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.usecase.AddReadingUseCase
import com.dg.electricitycounter.domain.usecase.GetLatestReadingUseCase
import com.dg.electricitycounter.domain.usecase.GetAllReadingsUseCase
import com.dg.electricitycounter.util.formatToDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addReadingUseCase: AddReadingUseCase,
    private val getLatestReadingUseCase: GetLatestReadingUseCase,
    private val getAllReadingsUseCase: GetAllReadingsUseCase,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
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
                    
                    // АВТОМАТИЧЕСКИЙ ЭКСПОРТ И ОТПРАВКА НА ПОЧТУ
                    exportAndSendHistory()
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
            scheduler.cancelReminders()
            scheduler.scheduleReminder()
        }
    }
    
    private fun exportAndSendHistory() {
        viewModelScope.launch {
            try {
                // Получаем всю историю
                val readings = getAllReadingsUseCase().first()
                
                if (readings.isEmpty()) return@launch
                
                // Формируем текст истории
                val historyText = readings.joinToString("\n") { reading ->
                    val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .format(Date(reading.date))
                    "$date ${reading.currentReading.toInt()} ${reading.consumption.toInt()} ${String.format("%.2f", reading.tariff)} ${String.format("%.2f", reading.amount)}"
                }
                
                // Сохраняем в файл
                val fileName = "history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(historyText, Charsets.UTF_8)
                
                val currentDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                
                // СОЗДАЕМ URI ЧЕРЕЗ FileProvider
                val uri = try {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    return@launch
                }
                
                // Создаем Intent для отправки email С ВЛОЖЕНИЕМ
                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("lbvsx@mail.ru"))
                    putExtra(Intent.EXTRA_SUBJECT, "показания счётчика $currentDate")
                    putExtra(Intent.EXTRA_TEXT, "История показаний во вложении.\n\nОтправлено из приложения Электросчётчик")
                    
                    // ПРИКРЕПЛЯЕМ ФАЙЛ
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Пытаемся открыть почтовое приложение
                try {
                    context.startActivity(Intent.createChooser(emailIntent, "Отправить историю").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    // Если нет почтового приложения - игнорируем
                }
                
            } catch (e: Exception) {
                // Игнорируем ошибки экспорта - не критично
            }
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
            📧 История отправлена на почту
            ${if (preferencesHelper.isReminderEnabled()) "\n🔕 Напоминания остановлены до следующего месяца" else ""}
        """.trimIndent()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
