package com.dg.electricitycounter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "membership_settings")
data class MembershipSettingsEntity(
    @PrimaryKey val id: Int = 1, // Храним одну запись
    val plotNumber: String = "143а",
    val plotArea: Float = 0f,        // площадь в сотках
    val tariffPerSotka: Float = 0f,  // руб/сотка
    val lastUpdated: Long = System.currentTimeMillis()
)