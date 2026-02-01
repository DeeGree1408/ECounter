package com.dg.electricitycounter.util

import android.content.Context
import android.util.Log
import com.dg.electricitycounter.data.local.dao.ReadingDao
import com.dg.electricitycounter.data.local.entity.ReadingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object DataMigration {
    
    suspend fun migrateFromSharedPreferences(
        context: Context,
        dao: ReadingDao
    ) = withContext(Dispatchers.IO) {
        try {
            // Проверяем, нужна ли миграция
            val count = dao.getCount()
            if (count > 0) {
                Log.d("DataMigration", "Database already has data, skipping migration")
                return@withContext
            }
            
            val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
            val historyJson = prefs.getString("history_data", "")
            
            if (historyJson.isNullOrEmpty()) {
                Log.d("DataMigration", "No old data to migrate")
                return@withContext
            }
            
            // Парсим старые данные
            val items = historyJson.split("|").mapNotNull { itemStr ->
                val parts = itemStr.split(",")
                if (parts.size == 9) {
                    try {
                        val dateStr = parts[1]
                        val timestamp = parseDate(dateStr)
                        
                        ReadingEntity(
                            id = parts[0].toInt(),
                            date = timestamp,
                            previousReading = parts[3].toDouble(),
                            currentReading = parts[4].toDouble(),
                            consumption = parts[5].toDouble(),
                            tariff = parts[6].toDouble(),
                            amount = parts[7].toDouble(),
                            address = parts[8]
                        )
                    } catch (e: Exception) {
                        Log.e("DataMigration", "Error parsing item: $itemStr", e)
                        null
                    }
                } else {
                    null
                }
            }
            
            if (items.isNotEmpty()) {
                dao.insertAll(items)
                Log.d("DataMigration", "Migrated ${items.size} items to Room")
            }
            
        } catch (e: Exception) {
            Log.e("DataMigration", "Error during migration", e)
        }
    }
    
    private fun parseDate(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
