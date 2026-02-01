package com.dg.electricitycounter.domain.usecase

import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllReadingsUseCase @Inject constructor(
    private val repository: ReadingRepository
) {
    operator fun invoke(): Flow<List<Reading>> {
        return repository.getAllReadings()
    }
}
