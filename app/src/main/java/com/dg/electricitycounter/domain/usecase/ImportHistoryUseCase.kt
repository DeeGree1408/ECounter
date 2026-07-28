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
            val membershipLines = mutableListOf<String>()
            var mode = Mode.AUTO
            var importedCount = 0

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                when {
                    trimmed == "#ELECTRICITY" -> mode = Mode.ELECTRICITY
                    trimmed == "#MEMBERSHIP_FEE" -> mode = Mode.MEMBERSHIP_FEE
                    trimmed.startsWith("META|") -> parseMetadata(trimmed) // Старый формат
                    else -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        when (mode) {
                            Mode.AUTO, Mode.ELECTRICITY -> {
                                if (parts.size >= 5) {
                                    try {
                                        val dateStr = parts[0]
                                        val current = parts[1].toDouble()
                                        val consumption = parts[2].toDouble()
                                        val tariff = parts[3].replace(',', '.').toDouble()
                                        val amount = parts[4].replace(',', '.').toDouble()
                                        readings.add(Reading(
                                            date = parseDate(dateStr), previousReading = current - consumption,
                                            currentReading = current, consumption = consumption,
                                            tariff = tariff, amount = amount, address = "уч.143а"
                                        ))
                                        mode = Mode.ELECTRICITY
                                    } catch (e: Exception) { /* игнор */ }
                                } else if (mode == Mode.AUTO && parts.size >= 4) {
                                    // Попытка распознать ЧВ без заголовка (fallback)
                                    membershipLines.add(trimmed)
                                    mode = Mode.MEMBERSHIP_FEE
                                }
                            }
                            Mode.MEMBERSHIP_FEE -> {
                                if (parts.size >= 4) membershipLines.add(trimmed)
                            }
                        }
                    }
                }
            }

            // 1. Импорт ЭЭ
            if (readings.isNotEmpty()) {
                repository.importReadings(readings)
                importedCount = readings.size
                val latestTariff = readings.first().tariff
                val firstChange = readings.lastOrNull { it.tariff == latestTariff }
                if (firstChange != null) {
                    preferencesHelper.saveTariff(String.format("%.2f", firstChange.tariff).replace(',', '.'))
                    preferencesHelper.saveTariffChangeDate(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(firstChange.date)))
                }
            }

            // 2. Восстановление истории ЧВ
            if (membershipLines.isNotEmpty()) {
                val prefs = context.getSharedPreferences("membership_fee_prefs_v2", Context.MODE_PRIVATE)
                prefs.edit().putString("membership_fee_history", membershipLines.joinToString("\n")).apply()

                // Применяем текущие настройки из самой свежей записи (первая строка)
                val latest = membershipLines.first().split("\\s+".toRegex())
                if (latest.size >= 4) {
                    prefs.edit()
                        .putString("membership_area", latest[1].replace(',', '.'))
                        .putString("membership_tariff", latest[2].replace(',', '.'))
                        .apply()
                }
            }

            emit(Result.success(importedCount))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun parseMetadata(metaLine: String) {
        try {
            val parts = metaLine.split("|")
            if (parts.size >= 3) {
                preferencesHelper.saveTariff(parts[1])
                preferencesHelper.saveTariffChangeDate(parts[2])
            }
        } catch (e: Exception) { /* игнор */ }
    }

    private fun parseDate(dateStr: String): Long {
        return try { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: System.currentTimeMillis() }
        catch (e: Exception) { System.currentTimeMillis() }
    }

    enum class Mode { AUTO, ELECTRICITY, MEMBERSHIP_FEE }
}