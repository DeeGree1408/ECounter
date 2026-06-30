package com.dg.electricitycounter.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dg.electricitycounter.data.local.dao.MembershipSettingsDao
import com.dg.electricitycounter.data.local.dao.ReadingDao
import com.dg.electricitycounter.data.local.entity.MembershipSettingsEntity
import com.dg.electricitycounter.data.local.entity.ReadingEntity

@Database(
    entities = [
        ReadingEntity::class,
        MembershipSettingsEntity::class  // ← добавлено
    ],
    version = 2,                         // ← было 1, увеличено!
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun readingDao(): ReadingDao
    abstract fun membershipSettingsDao(): MembershipSettingsDao  // ← добавлено

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "electricity_counter_db" // ← ИСПРАВЛЕНО! Было "ecounter_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}