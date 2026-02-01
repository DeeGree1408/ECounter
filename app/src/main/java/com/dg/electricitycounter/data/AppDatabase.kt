package com.dg.electricitycounter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dg.electricitycounter.data.local.dao.ReadingDao
import com.dg.electricitycounter.data.local.entity.ReadingEntity

@Database(
    entities = [ReadingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
}
