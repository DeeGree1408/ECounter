package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CheckDataExistsUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(): Flow<Boolean> = flow {
        try {
            val readings = repository.getAllReadings().first()
            emit(readings.isNotEmpty())
        } catch (e: Exception) {
            emit(false)
        }
    }
}
