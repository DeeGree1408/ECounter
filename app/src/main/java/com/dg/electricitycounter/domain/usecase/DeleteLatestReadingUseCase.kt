package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DeleteLatestReadingUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(): Flow<Result<Unit>> = flow {
        try {
            repository.deleteLatestReading()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
