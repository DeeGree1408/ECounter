package com.dg.electricitycounter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dg.electricitycounter.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "electricity_reminder_channel"
        const val CHANNEL_NAME = "Напоминания электросчётчика"
        const val NOTIFICATION_ID = 1001
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
        scope.launch {
            try {
                // Получаем данные из Room
                val database = AppDatabase::class.java
                    .getDeclaredMethod("invoke", Context::class.java)
                    .invoke(null, context) as? AppDatabase
                
                val dao = database?.readingDao()
                val latestReading = dao?.getLatest()
                
                val latestReadingValue = latestReading?.currentReading?.toInt()?.toString() ?: "нет данных"
                val latestDate = latestReading?.date?.let {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                } ?: "нет данных"
                
                // Склонение месяца
                val monthNames = arrayOf(
                    "январь", "февраль", "март", "апрель", "май", "июнь",
                    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
                )
                val calendar = Calendar.getInstance()
                val currentMonth = monthNames[calendar.get(Calendar.MONTH)]
                
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle("⚡ Пора передать показания!")
                    .setContentText("За $currentMonth - последние: $latestReadingValue ($latestDate)")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Не забудьте передать показания электросчётчика за $currentMonth месяц.\n\nПоследние переданные показания: $latestReadingValue кВт·ч\nДата: $latestDate"))
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
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback уведомление
                showFallbackNotification()
            }
        }
    }
    
    private fun showFallbackNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("⚡ Пора передать показания!")
            .setContentText("Не забудьте передать показания счётчика")
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
    
    fun cancelAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}
