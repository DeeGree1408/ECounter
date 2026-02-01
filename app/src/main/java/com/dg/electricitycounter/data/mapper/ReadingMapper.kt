package com.dg.electricitycounter.data.mapper

import com.dg.electricitycounter.data.local.entity.ReadingEntity
import com.dg.electricitycounter.domain.model.Reading

fun ReadingEntity.toDomain(): Reading {
    return Reading(
        id = id,
        date = date,
        previousReading = previousReading,
        currentReading = currentReading,
        consumption = consumption,
        tariff = tariff,
        amount = amount,
        address = address
    )
}

fun Reading.toEntity(): ReadingEntity {
    return ReadingEntity(
        id = id,
        date = date,
        previousReading = previousReading,
        currentReading = currentReading,
        consumption = consumption,
        tariff = tariff,
        amount = amount,
        address = address
    )
}
