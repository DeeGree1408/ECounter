package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AddReadingUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(
        previous: Double,
        current: Double,
        tariff: Double
    ): Flow<Result<Reading>> = flow {
        try {
            val consumption = current - previous
            val amount = consumption * tariff
            
            val reading = Reading(
                date = System.currentTimeMillis(),
                previousReading = previous,
                currentReading = current,
                consumption = consumption,
                tariff = tariff,
                amount = amount
            )
            
            repository.addReading(reading)
            emit(Result.success(reading))
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
