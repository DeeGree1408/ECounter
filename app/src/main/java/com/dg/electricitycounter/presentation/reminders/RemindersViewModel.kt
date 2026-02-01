package com.dg.electricitycounter.presentation.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.usecase.ExportHistoryUseCase
import com.dg.electricitycounter.domain.usecase.GetLatestReadingUseCase
import com.dg.electricitycounter.domain.usecase.ImportHistoryUseCase
import com.dg.electricitycounter.util.formatToDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesHelper: PreferencesHelper,
    private val getLatestReadingUseCase: GetLatestReadingUseCase,
    private val exportHistoryUseCase: ExportHistoryUseCase,
    private val importHistoryUseCase: ImportHistoryUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Загружаем настройки
            val isEnabled = preferencesHelper.isReminderEnabled()
            _uiState.update { it.copy(isReminderEnabled = isEnabled) }
            
            // Загружаем последнее показание
            getLatestReadingUseCase()
                .catch { }
                .collect { reading ->
                    _uiState.update {
                        it.copy(
                            latestReading = reading?.currentReading?.toInt()?.toString() ?: "нет данных",
                            latestDate = reading?.date?.formatToDisplay() ?: ""
                        )
                    }
                }
        }
    }
    
    fun toggleReminder(enabled: Boolean) {
        _uiState.update { it.copy(isReminderEnabled = enabled) }
        preferencesHelper.setReminderEnabled(enabled)
    }
    
    fun exportHistory(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            exportHistoryUseCase()
                .collect { result ->
                    result.onSuccess { exportText ->
                        onSuccess(exportText)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "История экспортирована"
                            )
                        }
                    }
                    result.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message
                            )
                        }
                    }
                }
        }
    }
    
    fun importHistory(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            importHistoryUseCase(content)
                .collect { result ->
                    result.onSuccess { count ->
                        loadData() // Перезагружаем данные
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "✅ Импортировано $count записей"
                            )
                        }
                    }
                    result.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "❌ ${e.message}"
                            )
                        }
                    }
                }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
