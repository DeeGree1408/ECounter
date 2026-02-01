package com.dg.electricitycounter.presentation.reminders

data class RemindersUiState(
    val isReminderEnabled: Boolean = false,
    val latestReading: String = "",
    val latestDate: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
