package com.dg.electricitycounter.domain.usecase

import android.content.Context
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ImportHistoryUseCase @Inject constructor(
    private val repository: ReadingRepository,
    private val preferencesHelper: PreferencesHelper,
    @ApplicationContext private val context: Context
) {
    operator fun invoke(content: String): Flow<Result<Int>> = flow {
        try {
            val lines = content.trim().split("\n")
            val readings = mutableListOf<Reading>()
            var mode = Mode.ELECTRICITY
            var importedCount = 0

            // Временные переменные для ЧВ
            var feeArea: String? = null
            var feeTariff: String? = null

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                when {
                    trimmed == "#ELECTRICITY" -> mode = Mode.ELECTRICITY
                    trimmed == "#MEMBERSHIP_FEE" -> mode = Mode.MEMBERSHIP_FEE
                    else -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        when (mode) {
                            Mode.ELECTRICITY -> {
                                if (parts.size >= 5) {
                                    try {
                                        val dateStr = parts[0]
                                        val current = parts[1].toDouble()
                                        val consumption = parts[2].toDouble()
                                        val tariff = parts[3].replace(',', '.').toDouble()
                                        val amount = parts[4].replace(',', '.').toDouble()
                                        val previous = current - consumption
                                        val timestamp = parseDate(dateStr)
                                        readings.add(Reading(
                                            date = timestamp, previousReading = previous, currentReading = current,
                                            consumption = consumption, tariff = tariff, amount = amount, address = "уч.143а"
                                        ))
                                    } catch (e: Exception) { /* пропускаем битые строки */ }
                                }
                            }
                            Mode.MEMBERSHIP_FEE -> {
                                if (parts.size >= 4) {
                                    try {
                                        // Формат: 31.07.2026 6,94 280 1943,20
                                        feeArea = parts[1].replace(',', '.')
                                        feeTariff = parts[2].replace(',', '.')
                                    } catch (e: Exception) { /* пропускаем */ }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Сохраняем ЭЭ в базу
            if (readings.isNotEmpty()) {
                repository.importReadings(readings)
                importedCount = readings.size

                val latestTariff = readings.first().tariff
                val firstTariffChange = readings.lastOrNull { it.tariff == latestTariff }
                if (firstTariffChange != null) {
                    preferencesHelper.saveTariff(String.format("%.2f", firstTariffChange.tariff).replace(',', '.'))
                    preferencesHelper.saveTariffChangeDate(
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(firstTariffChange.date))
                    )
                }
            }

            // 2. Сохраняем ЧВ в SharedPreferences
            if (feeArea != null && feeTariff != null) {
                val prefs = context.getSharedPreferences("membership_fee_prefs_v2", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("membership_area", feeArea)
                    .putString("membership_tariff", feeTariff)
                    .apply()
            }

            emit(Result.success(importedCount))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun parseDate(dateStr: String): Long {
        return try { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: System.currentTimeMillis() }
        catch (e: Exception) { System.currentTimeMillis() }
    }

    enum class Mode { ELECTRICITY, MEMBERSHIP_FEE }
}