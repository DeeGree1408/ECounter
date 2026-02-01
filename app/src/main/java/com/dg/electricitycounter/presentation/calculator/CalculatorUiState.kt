package com.dg.electricitycounter.presentation.calculator

data class CalculatorUiState(
    val currentReading: String = "",
    val previousReading: String = "",
    val tariff: String = "6.84",
    val tariffChangeDate: String = "", // 🆕 ДОБАВЛЕНО
    val lastReadingDate: String = "",
    val isTariffLocked: Boolean = true,
    val isPreviousLocked: Boolean = true,
    val resultText: String = "",
    val showResult: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false
)
