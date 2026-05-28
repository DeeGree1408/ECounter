package com.dg.electricitycounter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dg.electricitycounter.data.local.entity.MembershipSettingsEntity

@Dao
interface MembershipSettingsDao {
    @Query("SELECT * FROM membership_settings WHERE id = 1")
    suspend fun getSettings(): MembershipSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: MembershipSettingsEntity)
}