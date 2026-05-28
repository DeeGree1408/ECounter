package com.dg.electricitycounter.presentation.calculator

import com.dg.electricitycounter.domain.model.Reading

data class CalculatorUiState(
    val currentReading: String = "",
    val previousReading: String = "",
    val tariff: String = "6.84",
    val tariffChangeDate: String = "",
    val lastReadingDate: String = "",
    val isTariffLocked: Boolean = true,
    val isPreviousLocked: Boolean = true,
    val resultText: String = "",
    val showResult: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
    val showReplaceDialog: Boolean = false,
    val existingReading: Reading? = null,
    val newReading: Reading? = null,
    val showPeriodErrorDialog: Boolean = false,
    val periodErrorDay: Int = 0,

    // 💰 Членский взнос
    val membershipPlotNumber: String = "143а",
    val membershipPlotArea: String = "",
    val membershipTariff: String = "",
    val isMembershipNumberLocked: Boolean = true,
    val isMembershipAreaLocked: Boolean = true,
    val isMembershipTariffLocked: Boolean = true,
    val membershipFeeTotal: String = "0.00 ₽",
    val membershipChangeDate: String = ""
)