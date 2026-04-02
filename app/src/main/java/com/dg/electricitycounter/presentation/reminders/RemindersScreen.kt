package com.dg.electricitycounter.presentation.reminders

import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dg.electricitycounter.NotificationHelper
import com.dg.electricitycounter.PermissionHelper
import com.dg.electricitycounter.ReminderScheduler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ✅ СОЗДАЕМ SCHEDULER ОДИН РАЗ
    val scheduler = remember { ReminderScheduler(context) }

    // ✅ ОТДЕЛЬНОЕ СОСТОЯНИЕ ДЛЯ ВРЕМЕНИ БУДИЛЬНИКА
    var nextAlarmTime by remember { mutableStateOf<Long?>(null) }

    // ✅ ПЕРИОДИЧЕСКИЙ ОПРОС ПЛАНИРОВЩИКА (каждые 2 секунды)
    LaunchedEffect(uiState.isReminderEnabled, scheduler) {
        if (uiState.isReminderEnabled) {
            while (true) {
                nextAlarmTime = scheduler.getNextAlarmTime()
                delay(2000)
            }
        } else {
            nextAlarmTime = null
        }
    }

    // Показываем сообщения
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    // Лончер для импорта файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText()
                inputStream?.close()

                if (!content.isNullOrEmpty()) {
                    viewModel.importHistory(content)
                } else {
                    Toast.makeText(context, "❌ Файл пуст", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Ошибка чтения файла: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🔔 НАПОМИНАНИЯ", fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ЗАГОЛОВОК И ПЕРЕКЛЮЧАТЕЛЬ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "НАПОМИНАНИЯ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72)
                            )

                            // ✅ СТАТУС: реальное время из планировщика (обновляется!)
                            Text(
                                text = if (uiState.isReminderEnabled) {
                                    nextAlarmTime?.let { time ->
                                        "⏰ ${SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(time))}"
                                    } ?: "🔔 Включено"
                                } else {
                                    "🔕 Выключено"
                                },
                                fontSize = 12.sp,
                                color = if (uiState.isReminderEnabled) Color(0xFF28A745) else Color.Gray
                            )
                        }
                        Switch(
                            checked = uiState.isReminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleReminder(enabled)

                                if (enabled) {
                                    if (PermissionHelper.hasNotificationPermission(context)) {
                                        scheduler.scheduleReminder()
                                        Toast.makeText(
                                            context,
                                            "✅ Напоминания включены!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        PermissionHelper.requestNotificationPermissionIfNeeded(context)
                                        Toast.makeText(
                                            context,
                                            "📱 Разрешите уведомления в настройках",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        viewModel.toggleReminder(false)
                                    }
                                } else {
                                    scheduler.cancelReminders()
                                    Toast.makeText(
                                        context,
                                        "🔕 Напоминания выключены",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    // УПРАВЛЕНИЕ ИСТОРИЕЙ
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE7F3FF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📁 УПРАВЛЕНИЕ ИСТОРИЕЙ",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72),
                                fontSize = 16.sp
                            )

                            Button(
                                onClick = {
                                    viewModel.exportHistory { exportText ->
                                        try {
                                            val fileName = "history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
                                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            val file = File(downloadsDir, fileName)
                                            file.writeText(exportText, Charsets.UTF_8)

                                            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "message/rfc822"
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf("lbvsx@mail.ru"))
                                                putExtra(Intent.EXTRA_SUBJECT, "показания счётчика ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
                                                putExtra(Intent.EXTRA_TEXT, "История показаний во вложении.\n\nОтправлено из приложения Электросчётчик")

                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }

                                            try {
                                                context.startActivity(Intent.createChooser(emailIntent, "Отправить историю"))
                                                Toast.makeText(context, "✅ Файл сохранен: $fileName", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "❌ Нет почтового приложения", Toast.LENGTH_LONG).show()
                                            }

                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF28A745)
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("📤 ЭКСПОРТ И ОТПРАВКА ИСТОРИИ")
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch("text/plain")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF17A2B8)
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("📥 ИМПОРТ ИСТОРИИ ИЗ ФАЙЛА")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column {
                                Text(
                                    text = "📊 СТАТУС НАПОМИНАНИЙ:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3C72),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "🔢", fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text(text = "Последние показания: ${uiState.latestReading}", fontSize = 12.sp, color = Color(0xFF333333))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "📋", fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text(text = "Дата последних показаний: ${uiState.latestDate}", fontSize = 12.sp, color = Color(0xFF333333))
                                }
                            }

                            Button(
                                onClick = {
                                    if (PermissionHelper.hasNotificationPermission(context)) {
                                        try {
                                            NotificationHelper(context).showReminderNotification()
                                            Toast.makeText(context, "🔔 Тестовое уведомление отправлено!", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        PermissionHelper.requestNotificationPermissionIfNeeded(context)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("🔔 ТЕСТ УВЕДОМЛЕНИЯ")
                            }

                            Text(
                                text = "💡 Файл сохраняется в Downloads и отправляется на lbvsx@mail.ru",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "💡 ВАЖНАЯ ИНФОРМАЦИЯ", fontWeight = FontWeight.Bold, color = Color(0xFF6C757D), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Работают в фоне\n• Начинаются с 24 числа\n• Ежедневно в 12:00\n• Останавливаются после ввода показаний",
                                color = Color(0xFF6C757D),
                                fontSize = 11.sp
                            )
                        }
                    }

                    DiagnosticSection(scheduler = scheduler)

                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3C72))
                    ) {
                        Text("← ВЕРНУТЬСЯ", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}