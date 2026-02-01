package com.dg.electricitycounter.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.usecase.ImportHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val importHistoryUseCase: ImportHistoryUseCase,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()
    
    fun importHistory(content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            importHistoryUseCase(content)
                .collect { result ->
                    result.onSuccess { count ->
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess()
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
    
    fun startFromScratch(onSuccess: () -> Unit) {
        // Устанавливаем дефолтный тариф
        preferencesHelper.saveTariff("6.84")
        preferencesHelper.saveTariffChangeDate(
            java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                .format(java.util.Date())
        )
        onSuccess()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
