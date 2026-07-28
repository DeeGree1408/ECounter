package com.dg.electricitycounter.domain.usecase

import android.content.Context
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportHistoryUseCase @Inject constructor(
    private val repository: ReadingRepository,
    private val preferencesHelper: PreferencesHelper,
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Flow<Result<String>> = flow {
        try {
            val ru = Locale("ru", "RU")

            // ✅ Получаем список показаний из Flow
            val readings: List<Reading> = repository.getAllReadings().first()

            // 1. Секция ЭЛЕКТРИЧЕСТВА
            val electricitySection = if (readings.isNotEmpty()) {
                "#ELECTRICITY\n" + readings.joinToString("\n") { r ->
                    val date = SimpleDateFormat("dd.MM.yyyy", ru).format(Date(r.date))
                    "$date ${r.currentReading.toInt()} ${r.consumption.toInt()} ${String.format(ru, "%.2f", r.tariff)} ${String.format(ru, "%.2f", r.amount)}"
                }
            } else "#ELECTRICITY\n"

            // 2. Секция ЧЛЕНСКОГО ВЗНОСА (из SharedPreferences)
            val prefs = context.getSharedPreferences("membership_fee_prefs_v2", Context.MODE_PRIVATE)
            val history = prefs.getString("membership_fee_history", "") ?: ""
            val membershipSection = if (history.isNotBlank()) {
                "\n#MEMBERSHIP_FEE\n$history"
            } else {
                // Fallback: если истории нет, генерируем текущие значения
                val area = prefs.getString("membership_area", "6,94") ?: "6,94"
                val tariff = prefs.getString("membership_tariff", "280") ?: "280"
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                val date = SimpleDateFormat("dd.MM.yyyy", ru).format(cal.time)
                val sum = (area.replace(",", ".").toFloatOrNull() ?: 6.94f) * (tariff.toFloatOrNull() ?: 280f)
                "\n#MEMBERSHIP_FEE\n$date $area $tariff ${String.format(ru, "%.2f", sum).replace(".", ",")}"
            }

            // 3. Объединяем и возвращаем
            val fullContent = electricitySection + membershipSection
            emit(Result.success(fullContent))

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}