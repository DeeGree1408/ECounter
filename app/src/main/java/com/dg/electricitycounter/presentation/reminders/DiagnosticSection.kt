package com.dg.electricitycounter.presentation.reminders

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dg.electricitycounter.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.*

/**
 * Секция диагностики для экрана напоминаний
 */
@Composable
fun DiagnosticSection(scheduler: ReminderScheduler) {
    // Кешируем значения
    val isHuawei = remember(scheduler) { scheduler.isHuaweiDevice() }
    val canSchedule = remember(scheduler) { scheduler.canScheduleExactAlarms() }
    val batteryOptimized = remember(scheduler) { scheduler.isIgnoringBatteryOptimizations() }
    val nextAlarm = remember(scheduler) { scheduler.getNextAlarmTime() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHuawei) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🔍 Диагностика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Производитель
            DiagnosticRow(
                label = "Производитель",
                value = Build.MANUFACTURER,
                isWarning = isHuawei
            )

            // Разрешение SCHEDULE_EXACT_ALARM
            DiagnosticRow(
                label = "Точные будильники",
                value = if (canSchedule) "✅ Разрешено" else "❌ Запрещено",
                isWarning = !canSchedule
            )

            // Оптимизация батареи
            DiagnosticRow(
                label = "Оптимизация батареи",
                value = if (batteryOptimized) "✅ Отключена" else "⚠️ Включена",
                isWarning = !batteryOptimized
            )

            // Следующее срабатывание
            if (nextAlarm != null) {
                DiagnosticRow(
                    label = "Следующий будильник",
                    value = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(nextAlarm)),
                    isWarning = false
                )
            }

            // ========================================
            // 🧪 КНОПКА ТЕСТОВОГО БУДИЛЬНИКА
            // ========================================
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🧪 ТЕСТИРОВАНИЕ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Проверьте, работают ли напоминания прямо сейчас",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scheduler.scheduleTestAlarm(2) // 2 минуты
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("🧪 Тест через 2 минуты")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "💡 Через 2 минуты должно прийти уведомление",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Кнопки настроек (если Huawei или есть проблемы)
            val hasIssues = isHuawei || !canSchedule || !batteryOptimized

            if (hasIssues) {
                Spacer(modifier = Modifier.height(16.dp))

                if (isHuawei) {
                    HuaweiWarningCard(scheduler)
                }

                if (!canSchedule) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { scheduler.requestExactAlarmPermission() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("⚠️ Разрешить точные будильники")
                    }
                }

                if (!batteryOptimized) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { scheduler.requestIgnoreBatteryOptimizations() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("🔋 Отключить оптимизацию батареи")
                    }
                }
            }
        }
    }
}

/**
 * Предупреждение для Huawei с инструкцией
 */
@Composable
private fun HuaweiWarningCard(scheduler: ReminderScheduler) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "⚠️ ВАЖНО ДЛЯ HUAWEI",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Для надежной работы напоминаний:\n\n" +
                        "1. Нажмите кнопку ниже\n" +
                        "2. Найдите ECounter → включите «Автозапуск»\n" +
                        "3. Настройки → Батарея → Запуск приложений → ECounter → «Управление вручную» → ВСЕ галочки ✅",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { scheduler.openHuaweiSettings() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("⚙️ Открыть настройки Huawei")
            }
        }
    }
}

/**
 * Строка диагностической информации
 */
@Composable
private fun DiagnosticRow(label: String, value: String, isWarning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}
