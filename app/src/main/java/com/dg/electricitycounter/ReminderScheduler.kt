package com.dg.electricitycounter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "ReminderScheduler"

    companion object {
        const val ACTION_REMINDER = "com.dg.electricitycounter.REMINDER"
        const val REQUEST_CODE_REMINDER = 1001
    }

    fun scheduleReminder(lastReadingDateMillis: Long? = null) {
        Log.d(TAG, "=== scheduleReminder START ===")
        Log.d(TAG, "🔍 lastReadingDateMillis=$lastReadingDateMillis")

        if (!canScheduleExactAlarms()) {
            Log.e(TAG, " Нет разрешения SCHEDULE_EXACT_ALARM")
            requestExactAlarmPermission()
            return
        }

        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMonth = now.get(Calendar.MONTH)
        val previousMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val triggerCalendar = Calendar.getInstance()

        val inGracePeriod = currentDay >= 1 && currentDay <= 3

        if (inGracePeriod) {
            // 🔹 Льготный период (1-3 число)
            if (lastReadingDateMillis != null) {
                val lastCal = Calendar.getInstance().apply { timeInMillis = lastReadingDateMillis }
                val lastMonth = lastCal.get(Calendar.MONTH)

                if (lastMonth == previousMonth) {
                    // ✅ За предыдущий месяц всё передано. Ждём следующего цикла.
                    triggerCalendar.set(now.get(Calendar.YEAR), currentMonth, 24, 12, 0, 0)
                    Log.d(TAG, "📅 Логика А1: льготный период, всё ок -> 24.${currentMonth + 1}")
                } else {
                    // ❌ Пропущен цикл (например, последний ввод был в июле, а сейчас сентябрь)
                    // Напоминаем срочно: сегодня в 12:00 или завтра, если уже прошли полдень
                    triggerCalendar.set(now.get(Calendar.YEAR), currentMonth, currentDay, 12, 0, 0)
                    triggerCalendar.set(Calendar.MILLISECOND, 0)
                    if (currentHour >= 12) triggerCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    Log.d(TAG, "📅 Логика А2: льготный период, пропуск цикла -> ${triggerCalendar.time}")
                }
            } else {
                // Нет данных вообще -> срочно
                triggerCalendar.set(now.get(Calendar.YEAR), currentMonth, currentDay, 12, 0, 0)
                triggerCalendar.set(Calendar.MILLISECOND, 0)
                if (currentHour >= 12) triggerCalendar.add(Calendar.DAY_OF_MONTH, 1)
                Log.d(TAG, "📅 Логика А3: льготный период, нет данных -> ${triggerCalendar.time}")
            }
        } else if (lastReadingDateMillis != null) {
            // 🔹 Не в льготном периоде
            val lastCal = Calendar.getInstance().apply { timeInMillis = lastReadingDateMillis }
            val lastMonth = lastCal.get(Calendar.MONTH)

            if (lastMonth != currentMonth) {
                // Ввод в прошлом цикле -> напоминаем СЕГОДНЯ
                triggerCalendar.set(now.get(Calendar.YEAR), currentMonth, currentDay, 12, 0, 0)
                triggerCalendar.set(Calendar.MILLISECOND, 0)
                if (currentHour >= 12) triggerCalendar.add(Calendar.DAY_OF_MONTH, 1)
                Log.d(TAG, "📅 Логика Б: прошлый цикл -> сегодня")
            } else {
                // Ввод в этом месяце -> ждём 24 числа след. месяца
                triggerCalendar.set(now.get(Calendar.YEAR), currentMonth, 24, 12, 0, 0)
                triggerCalendar.set(Calendar.MILLISECOND, 0)
                triggerCalendar.add(Calendar.MONTH, 1)
                Log.d(TAG, "📅 Логика В: этот месяц -> 24.${currentMonth + 2}")
            }
        } else {
            // 🔹 Fallback: нет даты из БД
            if (currentDay >= 24) {
                triggerCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 24, 12, 0, 0)
                triggerCalendar.set(Calendar.MILLISECOND, 0)
                triggerCalendar.add(Calendar.MONTH, 1)
                Log.d(TAG, "📅 Логика Г: нет данных, после 24-го -> след. месяц")
            } else {
                triggerCalendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 24, 12, 0, 0)
                triggerCalendar.set(Calendar.MILLISECOND, 0)
                Log.d(TAG, "📅 Логика Д: нет данных, 4-23 число -> 24.${now.get(Calendar.MONTH) + 1}")
            }
        }

        val triggerTime = triggerCalendar.timeInMillis
        val pendingIntent = createPendingIntent()

        if (isHuaweiDevice()) {
            Log.d(TAG, "📱 Обнаружен Huawei - используем setAlarmClock()")
            scheduleWithAlarmClock(triggerTime, pendingIntent)
        } else {
            Log.d(TAG, " Обычное устройство - используем setExactAndAllowWhileIdle()")
            scheduleWithExactAlarm(triggerTime, pendingIntent)
        }

        saveNextAlarmTime(triggerTime)
        Log.d(TAG, "✅ Напоминание на: ${triggerCalendar.time}")
        Log.d(TAG, "=== scheduleReminder END ===")
    }

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
            if (get(Calendar.HOUR_OF_DAY) >= 12) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val triggerTime = calendar.timeInMillis
        val pendingIntent = createPendingIntent()
        if (isHuaweiDevice()) {
            scheduleWithAlarmClock(triggerTime, pendingIntent)
        } else {
            scheduleWithExactAlarm(triggerTime, pendingIntent)
        }
        saveNextAlarmTime(triggerTime)
        Log.d(TAG, "✅ Ежедневное напоминание на: ${calendar.time}")
        Log.d(TAG, "=== scheduleDailyReminders END ===")
    }

    fun cancelReminders() {
        Log.d(TAG, "🛑 cancelReminders")
        val pendingIntent = createPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        clearNextAlarmTime()
        Log.d(TAG, "✅ Напоминания отменены")
    }

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

    private fun scheduleWithAlarmClock(triggerTime: Long, pendingIntent: PendingIntent) {
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "✅ setAlarmClock() успешно")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка setAlarmClock(): ${e.message}", e)
        }
    }

    private fun scheduleWithExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "✅ setExactAndAllowWhileIdle() успешно")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка setExact(): ${e.message}", e)
        }
    }

    fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val isHuawei = manufacturer.contains("huawei") || manufacturer.contains("honor")
        Log.d(TAG, "🔍 Производитель: $manufacturer, Huawei: $isHuawei")
        return isHuawei
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.d(TAG, "🔍 canScheduleExactAlarms: $canSchedule")
            canSchedule
        } else {
            true
        }
    }

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

    fun openHuaweiSettings() {
        try {
            val intent = Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                component = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            context.startActivity(intent)
            Log.d(TAG, "🚀 Открыты настройки Huawei")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Не удалось открыть настройки Huawei: ${e.message}")
            openAppSettings()
        }
    }

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

    // ✅ ИСПРАВЛЕНО: Сначала читаем из наших SharedPreferences (надёжно на Huawei)
    fun getNextAlarmTime(): Long? {
        val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
        val savedTime = prefs.getLong("next_alarm_time", 0L)
        if (savedTime > System.currentTimeMillis()) {
            Log.d(TAG, "💾 Будильник из памяти: ${Date(savedTime)}")
            return savedTime
        }

        // Fallback: системный API (может быть неточным)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val nextAlarmInfo = alarmManager.nextAlarmClock
            nextAlarmInfo?.triggerTime?.also {
                Log.d(TAG, "⏰ Будильник из системы: ${Date(it)}")
                return it
            }
        }

        Log.d(TAG, "❌ Будильник не найден")
        return null
    }

    private fun saveNextAlarmTime(time: Long) {
        val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
        prefs.edit().putLong("next_alarm_time", time).apply()
        Log.d(TAG, "💾 Сохранено время будильника: ${Date(time)}")
    }

    private fun clearNextAlarmTime() {
        val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
        prefs.edit().remove("next_alarm_time").apply()
        Log.d(TAG, "🗑️ Время будильника очищено")
    }

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
            scheduleWithAlarmClock(triggerTime, pendingIntent)
        } else {
            scheduleWithExactAlarm(triggerTime, pendingIntent)
        }
        saveNextAlarmTime(triggerTime)
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val message = "🧪 Тестовый будильник установлен на ${dateFormat.format(calendar.time)}"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        Log.d(TAG, "✅ Тестовый будильник на: ${calendar.time}")
        Log.d(TAG, "🧪=== scheduleTestAlarm END ===")
    }
}