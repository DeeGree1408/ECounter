package com.dg.electricitycounter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "electricity_reminder_channel"
        const val CHANNEL_NAME = "Напоминания электросчётчика"
        const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о передаче показаний"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showReminderNotification() {
        val latestReading = getLatestReadingFromHistory()
        val latestDate = getLatestDateFromHistory()
        
        // Правильное склонение месяца
        val monthNames = arrayOf(
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
        )
        val calendar = Calendar.getInstance()
        val currentMonth = monthNames[calendar.get(Calendar.MONTH)]
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("⚡ Пора передать показания!")
            .setContentText("За $currentMonth - последние: $latestReading ($latestDate)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Не забудьте передать показания электросчётчика за $currentMonth месяц.\n\nПоследние переданные показания: $latestReading кВт·ч\nДата: $latestDate"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun getLatestReadingFromHistory(): String {
        return try {
            // Получаем AppState через reflection
            val appStateClass = Class.forName("com.dg.electricitycounter.AppState")
            
            // Получаем поле historyItems
            val historyItemsField = appStateClass.getDeclaredField("historyItems")
            historyItemsField.isAccessible = true
            
            // Получаем список истории
            val historyItems = historyItemsField.get(null) as? kotlin.collections.List<*>
            
            if (historyItems.isNullOrEmpty()) {
                return "нет данных"
            }
            
            // Берем первый элемент (последнюю запись)
            val firstItem = historyItems.first()
            val itemClass = firstItem!!.javaClass
            
            // Получаем поле current
            val currentField = itemClass.getDeclaredField("current")
            currentField.isAccessible = true
            val currentValue = currentField.get(firstItem) as Double
            
            return currentValue.toInt().toString()
            
        } catch (e: Exception) {
            // Логируем ошибку для отладки
            e.printStackTrace()
            return "ошибка"
        }
    }
    
    private fun getLatestDateFromHistory(): String {
        return try {
            // Получаем AppState через reflection
            val appStateClass = Class.forName("com.dg.electricitycounter.AppState")
            
            // Получаем поле historyItems
            val historyItemsField = appStateClass.getDeclaredField("historyItems")
            historyItemsField.isAccessible = true
            
            // Получаем список истории
            val historyItems = historyItemsField.get(null) as? kotlin.collections.List<*>
            
            if (historyItems.isNullOrEmpty()) {
                return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            }
            
            // Берем первый элемент (последнюю запись)
            val firstItem = historyItems.first()
            val itemClass = firstItem!!.javaClass
            
            // Получаем поле date
            val dateField = itemClass.getDeclaredField("date")
            dateField.isAccessible = true
            val dateValue = dateField.get(firstItem) as String
            
            return dateValue
            
        } catch (e: Exception) {
            // Логируем ошибку для отладки
            e.printStackTrace()
            return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        }
    }
    
    fun cancelAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}