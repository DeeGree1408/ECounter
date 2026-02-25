package com.dg.electricitycounter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.*
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale


class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "ReminderScheduler"

    companion object {
        const val ACTION_REMINDER = "com.dg.electricitycounter.REMINDER"
        const val REQUEST_CODE_REMINDER = 1001
    }

    /**
     * Планирует напоминание на 24 число текущего или следующего месяца в 12:00
     */
    fun scheduleReminder() {
        Log.d(TAG, "=== scheduleReminder START ===")
        
        // Проверяем разрешения
        if (!canScheduleExactAlarms()) {
            Log.e(TAG, "❌ Нет разрешения SCHEDULE_EXACT_ALARM")
            requestExactAlarmPermission()
            return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Если сегодня >= 24, то планируем на следующий месяц
            if (get(Calendar.DAY_OF_MONTH) >= 24) {
                add(Calendar.MONTH, 1)
            }
            set(Calendar.DAY_OF_MONTH, 24)
        }

        val triggerTime = calendar.timeInMillis
        val pendingIntent = createPendingIntent()

        // ГЛАВНОЕ ИЗМЕНЕНИЕ: используем setAlarmClock() для Huawei
        if (isHuaweiDevice()) {
            Log.d(TAG, "📱 Обнаружен Huawei - используем setAlarmClock()")
            scheduleWithAlarmClock(triggerTime, pendingIntent)
        } else {
            Log.d(TAG, "📱 Обычное устройство - используем setExactAndAllowWhileIdle()")
            scheduleWithExactAlarm(triggerTime, pendingIntent)
        }

        Log.d(TAG, "✅ Напоминание запланировано на: ${calendar.time}")
        Log.d(TAG, "⏰ Timestamp: $triggerTime (через ${(triggerTime - System.currentTimeMillis()) / 1000 / 60} минут)")
        Log.d(TAG, "=== scheduleReminder END ===")
    }

    /**
     * Планирует ежедневные напоминания с 25 числа
     */
    fun scheduleDailyReminders() {
        Log.d(TAG, "=== scheduleDailyReminders START ===")
        
        if (!canScheduleExactAlarms()) {
            Log.e(TAG, "❌ Нет разрешения SCHEDULE_EXACT_ALARM")
            return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Если сегодня до 12:00, то сегодня в 12:00, иначе завтра
            if (get(Calendar.HOUR_OF_DAY) >= 12) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val triggerTime = calendar.timeInMillis
        val pendingIntent = createPendingIntent()

        if (isHuaweiDevice()) {
            Log.d(TAG, "📱 Huawei - ежедневное с setAlarmClock()")
            scheduleWithAlarmClock(triggerTime, pendingIntent)
        } else {
            Log.d(TAG, "📱 Ежедневное с setExactAndAllowWhileIdle()")
            scheduleWithExactAlarm(triggerTime, pendingIntent)
        }

        Log.d(TAG, "✅ Ежедневное напоминание на: ${calendar.time}")
        Log.d(TAG, "=== scheduleDailyReminders END ===")
    }

    /**
     * Отменяет все напоминания
     */
    fun cancelReminders() {
        Log.d(TAG, "🛑 cancelReminders")
        val pendingIntent = createPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "✅ Напоминания отменены")
    }

    // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

    /**
     * Создает PendingIntent для ReminderReceiver
     */
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE_REMINDER, intent, flags)
    }

    /**
     * Планирует с setAlarmClock() - для Huawei
     * Показывает иконку в статус-баре, наивысший приоритет
     */
    private fun scheduleWithAlarmClock(triggerTime: Long, pendingIntent: PendingIntent) {
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            triggerTime,
            pendingIntent // showIntent - открывает приложение при нажатии на иконку
        )
        
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "✅ setAlarmClock() успешно")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка setAlarmClock(): ${e.message}", e)
        }
    }

    /**
     * Планирует с setExactAndAllowWhileIdle() - для обычных устройств
     */
    private fun scheduleWithExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "✅ setExactAndAllowWhileIdle() успешно")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка setExact(): ${e.message}", e)
        }
    }

    /**
     * Проверяет, Huawei ли это устройство
     */
    fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val isHuawei = manufacturer.contains("huawei") || manufacturer.contains("honor")
        Log.d(TAG, "🔍 Производитель: $manufacturer, Huawei: $isHuawei")
        return isHuawei
    }

    /**
     * Проверяет разрешение SCHEDULE_EXACT_ALARM (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.d(TAG, "🔍 canScheduleExactAlarms: $canSchedule")
            canSchedule
        } else {
            true
        }
    }

    /**
     * Открывает настройки разрешения SCHEDULE_EXACT_ALARM
     */
    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "🚀 Открыты настройки SCHEDULE_EXACT_ALARM")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка открытия настроек: ${e.message}", e)
            }
        }
    }

    /**
     * Проверяет, находится ли приложение в исключениях батареи
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            Log.d(TAG, "🔋 Battery optimization ignored: $isIgnoring")
            isIgnoring
        } else {
            true
        }
    }

    /**
     * Открывает настройки оптимизации батареи
     */
    fun requestIgnoreBatteryOptimizations() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "🚀 Открыты настройки оптимизации батареи")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка открытия настроек батареи: ${e.message}", e)
        }
    }

    /**
     * Для Huawei - открывает настройки автозапуска (не стандартный Android)
     */
    fun openHuaweiSettings() {
        try {
            val intent = Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                // Попытка 1: Настройки запуска
                component = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            context.startActivity(intent)
            Log.d(TAG, "🚀 Открыты настройки Huawei")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Не удалось открыть настройки Huawei: ${e.message}")
            // Fallback - открываем общие настройки приложения
            openAppSettings()
        }
    }

    /**
     * Открывает настройки приложения
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "🚀 Открыты настройки приложения")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка открытия настроек: ${e.message}", e)
        }
    }

    /**
     * Получает время следующего срабатывания (для отображения)
     */
    fun getNextAlarmTime(): Long? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val nextAlarmInfo = alarmManager.nextAlarmClock
            nextAlarmInfo?.triggerTime?.also {
                Log.d(TAG, "⏰ Следующий будильник: ${Date(it)}")
            }
        } else {
            null
        }
    }
    /**
     * 🧪 ТЕСТОВЫЙ МЕТОД: Установить будильник через N минут
     * Для проверки работы уведомлений
     */
    fun scheduleTestAlarm(minutesFromNow: Int) {
        Log.d(TAG, "🧪=== scheduleTestAlarm START ===")
        Log.d(TAG, "🧪 Будильник через $minutesFromNow минут")

        if (!canScheduleExactAlarms()) {
            Log.e(TAG, "❌ Нет разрешения SCHEDULE_EXACT_ALARM")
            Toast.makeText(context, "❌ Нет разрешения на точные будильники", Toast.LENGTH_LONG).show()
            return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, minutesFromNow)
        }

        val triggerTime = calendar.timeInMillis
        val pendingIntent = createPendingIntent()

        if (isHuaweiDevice()) {
            Log.d(TAG, "🧪 Huawei - используем setAlarmClock()")
            scheduleWithAlarmClock(triggerTime, pendingIntent)
        } else {
            Log.d(TAG, "🧪 Обычное устройство - используем setExactAndAllowWhileIdle()")
            scheduleWithExactAlarm(triggerTime, pendingIntent)
        }

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val message = "🧪 Тестовый будильник установлен на ${dateFormat.format(calendar.time)}"

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        Log.d(TAG, "✅ Тестовый будильник на: ${calendar.time}")
        Log.d(TAG, "🧪=== scheduleTestAlarm END ===")
    }

}
