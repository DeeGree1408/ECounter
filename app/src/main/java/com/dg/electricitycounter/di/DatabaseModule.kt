package com.dg.electricitycounter.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dg.electricitycounter.data.local.AppDatabase
import com.dg.electricitycounter.data.local.dao.ReadingDao
import com.dg.electricitycounter.util.DataMigration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "electricity_counter_db"
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Выполняем миграцию при первом создании БД
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val database = Room.databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            "electricity_counter_db"
                        ).build()
                        
                        DataMigration.migrateFromSharedPreferences(
                            context,
                            database.readingDao()
                        )
                    }
                }
            })
            .build()
    }
    
    @Provides
    fun provideReadingDao(database: AppDatabase): ReadingDao {
        return database.readingDao()
    }
}
