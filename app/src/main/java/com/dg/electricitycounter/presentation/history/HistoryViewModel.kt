package com.dg.electricitycounter.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.domain.usecase.DeleteLatestReadingUseCase
import com.dg.electricitycounter.domain.usecase.GetAllReadingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllReadingsUseCase: GetAllReadingsUseCase,
    private val deleteLatestReadingUseCase: DeleteLatestReadingUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getAllReadingsUseCase()
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
                .collect { readings ->
                    _uiState.update {
                        it.copy(
                            readings = readings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }
    
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }
    
    fun deleteLatestReading() {
        viewModelScope.launch {
            deleteLatestReadingUseCase()
                .collect { result ->
                    result.onSuccess {
                        _uiState.update { it.copy(showDeleteDialog = false) }
                    }
                    result.onFailure { e ->
                        _uiState.update { 
                            it.copy(
                                error = e.message,
                                showDeleteDialog = false
                            )
                        }
                    }
                }
        }
    }
}
