package com.dg.electricitycounter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dg.electricitycounter.data.local.entity.ReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    
    @Query("SELECT * FROM readings ORDER BY date DESC")
    fun getAllReadingsFlow(): Flow<List<ReadingEntity>>
    
    @Query("SELECT * FROM readings ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): ReadingEntity?
    
    @Query("SELECT * FROM readings ORDER BY date DESC LIMIT 1")
    fun getLatestFlow(): Flow<ReadingEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: ReadingEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<ReadingEntity>)
    
    @Query("DELETE FROM readings WHERE id = (SELECT id FROM readings ORDER BY date DESC LIMIT 1)")
    suspend fun deleteLatest()
    
    @Query("DELETE FROM readings")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM readings")
    suspend fun getCount(): Int
}
