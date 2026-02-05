package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.presentation.statistics.TariffChange
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class GetTariffHistoryUseCase @Inject constructor() {

    operator fun invoke(readings: List<Reading>, currentTariff: Double): List<TariffChange> {
        if (readings.isEmpty()) return emptyList()

        // Группируем по тарифу и находим ПЕРВОЕ (самое раннее) появление каждого
        val tariffMap = mutableMapOf<Double, Reading>()

        for (reading in readings) {
            val tariff = reading.tariff

            if (!tariffMap.containsKey(tariff)) {
                tariffMap[tariff] = reading
            } else {
                // Если нашли более раннюю дату с этим тарифом - обновляем
                if (reading.date < tariffMap[tariff]!!.date) {
                    tariffMap[tariff] = reading
                }
            }
        }

        // Преобразуем в список и сортируем по дате (от новых к старым)
        val tariffChanges = tariffMap.map { (tariff, reading) ->
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(Date(reading.date))

            val isCurrent = (Math.abs(tariff - currentTariff) < 0.01)

            TariffChange(
                tariff = tariff,
                date = dateStr,
                isCurrent = isCurrent
            )
        }.sortedByDescending { change ->
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(change.date)?.time ?: 0
        }

        return tariffChanges
    }
}
