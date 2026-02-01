package com.dg.electricitycounter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// КЛАСС ДЛЯ СОХРАНЕНИЯ ВСЕХ ДАННЫХ
object DataStorage {
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
        loadAllData()
    }
    
    // СОХРАНЕНИЕ ДАННЫХ КАЛЬКУЛЯТОРА
    fun saveCalculatorData(
        currentReading: String = "",
        tariff: String = "6.84",
        previousReading: String = "",
        isTariffLocked: Boolean = true,
        isPreviousLocked: Boolean = true,
        lastReadingDate: String = "",
        tariffChangeDate: String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
    ) {
        // Автоматически обновляем previousReading из истории, если не указан
        val actualPreviousReading = if (previousReading.isEmpty() && AppState.historyItems.isNotEmpty()) {
            AppState.historyItems.first().current.toInt().toString()
        } else if (previousReading.isEmpty()) {
            "180237"
        } else {
            previousReading
        }
        
        // Автоматически обновляем дату из истории, если не указана
        val actualLastDate = if (lastReadingDate.isEmpty() && AppState.historyItems.isNotEmpty()) {
            AppState.historyItems.first().date
        } else if (lastReadingDate.isEmpty()) {
            "01.12.2025"
        } else {
            lastReadingDate
        }
        
        prefs.edit().apply {
            putString("current_reading", currentReading)
            putString("tariff", tariff)
            putString("previous_reading", actualPreviousReading)
            putBoolean("tariff_locked", isTariffLocked)
            putBoolean("previous_locked", isPreviousLocked)
            putString("last_reading_date", actualLastDate)
            putString("tariff_change_date", tariffChangeDate) // ← СОХРАНЯЕМ ДАТУ ИЗМЕНЕНИЯ ТАРИФА
            apply()
        }
    }
    
    // ЗАГРУЗКА ДАННЫХ КАЛЬКУЛЯТОРА
    fun loadCalculatorData(): Map<String, Any> {
        // Получаем данные из истории
        val latestReadingFromHistory = AppState.getLatestReading()
        val latestDateFromHistory = AppState.getLatestReadingDate()
        
        return mapOf(
            "current_reading" to (prefs.getString("current_reading", "") ?: ""),
            "tariff" to (prefs.getString("tariff", "6.84") ?: "6.84"),
            "previous_reading" to (prefs.getString("previous_reading", latestReadingFromHistory) ?: latestReadingFromHistory),
            "last_reading_date" to (prefs.getString("last_reading_date", latestDateFromHistory) ?: latestDateFromHistory),
            "tariff_locked" to prefs.getBoolean("tariff_locked", true),
            "previous_locked" to prefs.getBoolean("previous_locked", true),
            "tariff_change_date" to (prefs.getString("tariff_change_date", "01.07.2025") ?: "01.07.2025") // ← ЗАГРУЖАЕМ ДАТУ ИЗМЕНЕНИЯ ТАРИФА
        )
    }
    
    // ОБНОВИТЬ ТОЛЬКО ПРЕДЫДУЩИЕ ПОКАЗАНИЯ И ДАТУ
    fun updatePreviousFromHistory() {
        val latestReading = AppState.getLatestReading()
        val latestDate = AppState.getLatestReadingDate()
        
        prefs.edit().apply {
            putString("previous_reading", latestReading)
            putString("last_reading_date", latestDate)
            apply()
        }
    }
    
    // СОХРАНЕНИЕ НАСТРОЕК НАПОМИНАНИЙ
    fun saveReminderData(
        isEnabled: Boolean = false
    ) {
        prefs.edit().putBoolean("reminder_enabled", isEnabled).apply()
        
        // Обновляем глобальное состояние
        AppState.isReminderEnabled = isEnabled
    }
    
    // СОХРАНЕНИЕ ВСЕЙ ИСТОРИИ
    fun saveHistory() {
        val historyJson = AppState.historyItems.joinToString("|") { item ->
            "${item.id},${item.date},${item.readingDate},${item.previous},${item.current},${item.consumption},${item.tariff},${item.amount},${item.address}"
        }
        prefs.edit().putString("history_data", historyJson).apply()
        
        // После сохранения истории обновляем предыдущие показания
        updatePreviousFromHistory()
    }
    
    // ЗАГРУЗКА ИСТОРИИ
    fun loadHistory() {
        val historyJson = prefs.getString("history_data", "")
        if (!historyJson.isNullOrEmpty()) {
            AppState.historyItems.clear()
            val items = historyJson.split("|").mapNotNull { itemStr ->
                val parts = itemStr.split(",")
                if (parts.size == 9) {
                    try {
                        HistoryItem(
                            id = parts[0].toInt(),
                            date = parts[1],
                            readingDate = parts[2],
                            previous = parts[3].toDouble(),
                            current = parts[4].toDouble(),
                            consumption = parts[5].toDouble(),
                            tariff = parts[6].toDouble(),
                            amount = parts[7].toDouble(),
                            address = parts[8]
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
            AppState.historyItems.addAll(items)
            
            // Обновляем nextId
            AppState.updateNextId()
        } else {
            // Если истории нет, загружаем начальные данные
            loadInitialHistory()
        }
    }

    // ЗАГРУЗКА НАЧАЛЬНОЙ ИСТОРИИ
    private fun loadInitialHistory() {
        if (AppState.historyItems.isEmpty()) {
            AppState.historyItems.addAll(listOf(
                HistoryItem(
                    id = 6,
                    date = "01.01.2026",
                    readingDate = "01/01/2026",
                    previous = 184097.0,
                    current = 185702.0,
                    consumption = 1605.0,
                    tariff = 6.84,
                    amount = 10978.20,
                    address = "уч.143а"
                ),
                HistoryItem(
                    id = 5,
                    date = "01.12.2025",
                    readingDate = "01/12/2025",
                    previous = 182404.0,
                    current = 184097.0,
                    consumption = 1693.0,
                    tariff = 6.84,
                    amount = 11580.12,
                    address = "уч.143а"
                ),
                HistoryItem(
                    id = 4,
                    date = "01.11.2025",
                    readingDate = "01/11/2025",
                    previous = 181043.0,
                    current = 182404.0,
                    consumption = 1361.0,
                    tariff = 6.84,
                    amount = 9309.24,
                    address = "уч.143а"
                ),
                HistoryItem(
                    id = 3,
                    date = "01.10.2025",
                    readingDate = "01/10/2025",
                    previous = 180543.0,
                    current = 181043.0,
                    consumption = 500.0,
                    tariff = 6.84,
                    amount = 3420.00,
                    address = "уч.143а"
                ),
                HistoryItem(
                    id = 2,
                    date = "01.09.2025",
                    readingDate = "01/09/2025",
                    previous = 180435.0,
                    current = 180543.0,
                    consumption = 108.0,
                    tariff = 6.84,
                    amount = 738.72,
                    address = "уч.143а"
                ),
                HistoryItem(
                    id = 1,
                    date = "01.08.2025",
                    readingDate = "01/08/2025",
                    previous = 180237.0,
                    current = 180435.0,
                    consumption = 198.0,
                    tariff = 6.84,
                    amount = 1354.32,
                    address = "уч.143а"
                )
            ))
            AppState.updateNextId()
        }
    }
    
    // ЗАГРУЗКА ВСЕХ ДАННЫХ
    private fun loadAllData() {
        // Напоминания
        AppState.isReminderEnabled = prefs.getBoolean("reminder_enabled", false)
        
        // История
        loadHistory()
    }
}

// МОДЕЛЬ ДАННЫХ ДЛЯ ИСТОРИИ
data class HistoryItem(
    val id: Int,
    val date: String,
    val readingDate: String,
    val previous: Double,
    val current: Double,
    val consumption: Double,
    val tariff: Double,
    val amount: Double,
    val address: String
)

// ГЛОБАЛЬНЫЕ СОСТОЯНИЯ ДЛЯ ВСЕГО ПРИЛОЖЕНИЯ
object AppState {
    // Для напоминаний
    var isReminderEnabled by mutableStateOf(false)

    // Счетчик для ID новых записей
    private var nextId = 7

    // Для истории
    val historyItems = mutableStateListOf<HistoryItem>()

    init {
        // Инициализируем при запуске
        if (historyItems.isEmpty()) {
            // История загружается через DataStorage.loadHistory()
        }
    }

    // Функция обновления nextId
    fun updateNextId() {
        nextId = if (historyItems.isNotEmpty()) {
            historyItems.maxOf { it.id } + 1
        } else {
            1
        }
    }

    // Функция для добавления новой записи в историю
    fun addNewReading(
        date: String,
        previous: Double,
        current: Double,
        tariff: Double,
        address: String = "уч.143а"
    ) {
        val consumption = current - previous
        val amount = consumption * tariff

        val newItem = HistoryItem(
            id = nextId++,
            date = formatDisplayDate(date),
            readingDate = date,
            previous = previous,
            current = current,
            consumption = consumption,
            tariff = tariff,
            amount = amount,
            address = address
        )

        historyItems.add(0, newItem)

        // Сохраняем историю
        DataStorage.saveHistory()
    }

    // Форматируем дату для отображения
    private fun formatDisplayDate(inputDate: String): String {
        return try {
            val parts = inputDate.split("/")
            if (parts.size == 3) {
                "${parts[0]}.${parts[1]}.${parts[2]}"
            } else {
                inputDate
            }
        } catch (e: Exception) {
            inputDate
        }
    }

    // Для калькулятора
    fun getLatestReading(): String {
        return if (historyItems.isNotEmpty()) {
            historyItems.first().current.toInt().toString()
        } else {
            "180237"
        }
    }

    fun getLatestReadingDate(): String {
        return if (historyItems.isNotEmpty()) {
            historyItems.first().date
        } else {
            "01.12.2025"
        }
    }

    // Функция для удаления последней записи
    fun deleteLastReading() {
        if (historyItems.isNotEmpty()) {
            historyItems.removeAt(0)

            // Сохраняем историю и обновляем предыдущие показания
            DataStorage.saveHistory()
        }
    }
}

// Основной композабл для всего приложения
@Composable
fun SimpleApp() {
    var currentScreen by remember { mutableStateOf("calculator") }
    
    when (currentScreen) {
        "calculator" -> CalculatorScreen(
            onNavigateToReminders = { currentScreen = "reminders" },
            onNavigateToHistory = { currentScreen = "history" }
        )
        "reminders" -> RemindersScreen(
            onBack = { currentScreen = "calculator" }
        )
        "history" -> HistoryScreen(
            onBack = { currentScreen = "calculator" }
        )
    }
}

// КОМПАКТНАЯ КНОПКА НАВИГАЦИИ
@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
    ) {
        Text(
            text = text, 
            fontSize = 10.sp,
            maxLines = 1, 
            letterSpacing = (-0.5).sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ЭКРАН КАЛЬКУЛЯТОРА
@Composable
fun CalculatorScreen(
    onNavigateToReminders: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current

    // Всегда берем свежие данные из истории при запуске экрана
    val latestFromHistory = AppState.getLatestReading()
    val latestDateFromHistory = AppState.getLatestReadingDate()

    val savedData = remember {
        DataStorage.loadCalculatorData()
    }

    var currentReading by remember {
        mutableStateOf("") // Всегда пустое при запуске
    }
    var tariff by remember {
        mutableStateOf(savedData["tariff"] as String)
    }
    
    // 🔧 ДАТА ИЗМЕНЕНИЯ ТАРИФА
    var tariffChangeDate by remember {
        mutableStateOf(savedData["tariff_change_date"] as String)
    }

    // ВАЖНО: Берем previous_reading из сохраненных данных, 
    // которые автоматически обновляются из истории
    var previousReading by remember {
        mutableStateOf(savedData["previous_reading"] as String)
    }

    var lastReadingDate by remember {
        mutableStateOf(savedData["last_reading_date"] as String)
    }

    var isTariffLocked by remember {
        mutableStateOf(savedData["tariff_locked"] as Boolean)
    }
    var isPreviousLocked by remember {
        mutableStateOf(savedData["previous_locked"] as Boolean)
    }

    var resultText by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }

    // ФУНКЦИЯ ДЛЯ СОХРАНЕНИЯ ДАННЫХ
    fun saveData(isTariffChanged: Boolean = false) {
        DataStorage.saveCalculatorData(
            currentReading = currentReading,
            tariff = tariff,
            previousReading = previousReading,
            lastReadingDate = lastReadingDate,
            isTariffLocked = isTariffLocked,
            isPreviousLocked = isPreviousLocked,
            tariffChangeDate = if (isTariffChanged) {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            } else {
                tariffChangeDate
            }
        )
        
        // Обновляем локальную переменную, если тариф изменился
        if (isTariffChanged) {
            tariffChangeDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        }
    }

    // Обновляем данные из истории при каждом отображении экрана
    LaunchedEffect(Unit) {
        // Обновляем previousReading из истории
        previousReading = AppState.getLatestReading()
        lastReadingDate = AppState.getLatestReadingDate()
        saveData()
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F2027),
                            Color(0xFF203A43),
                            Color(0xFF2C5364)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ЗАГОЛОВОК
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 36.sp,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = "ЭЛЕКТРОСЧЁТЧИК",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Учёт и расчёт электроэнергии",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

                // НАВИГАЦИОННЫЕ КНОПКИ (ТОЛЬКО 2 КНОПКИ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    NavigationButton(
                        text = "НАПОМИНАНИЯ",
                        onClick = onNavigateToReminders,
                        color = Color(0xFF2A5298),
                        modifier = Modifier.weight(1f)
                    )

                    NavigationButton(
                        text = "ИСТОРИЯ",
                        onClick = onNavigateToHistory,
                        color = Color(0xFFFF8C00),
                        modifier = Modifier.weight(1f)
                    )
                }

                // КАРТОЧКА С ПОЛЯМИ ВВОДА
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ТАРИФ
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ТАРИФ (руб/кВт·ч)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                // 🔧 ИЗМЕНЕНО: показываем дату изменения тарифа
                                Text(
                                    text = "действует с $tariffChangeDate",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            OutlinedTextField(
                                value = tariff,
                                onValueChange = {
                                    val oldTariff = tariff
                                    tariff = it
                                    
                                    // Проверяем, действительно ли изменилось значение тарифа
                                    if (oldTariff != it && !isTariffLocked) {
                                        saveData(isTariffChanged = true)
                                    } else {
                                        saveData()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isTariffLocked,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            isTariffLocked = !isTariffLocked
                                            saveData()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            if (isTariffLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Защита",
                                            tint = if (isTariffLocked) Color.Gray else Color(0xFF28A745),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // ПРЕДЫДУЩИЕ ПОКАЗАНИЯ
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "СТАРЫЕ ПОКАЗАНИЯ, кВт",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = lastReadingDate,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            OutlinedTextField(
                                value = previousReading,
                                onValueChange = {
                                    previousReading = it
                                    saveData()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isPreviousLocked,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            isPreviousLocked = !isPreviousLocked
                                            saveData()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            if (isPreviousLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Защита",
                                            tint = if (isPreviousLocked) Color.Gray else Color(0xFF28A745),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // ТЕКУЩИЕ ПОКАЗАНИЯ
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "НОВЫЕ ПОКАЗАНИЯ, кВт",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = currentReading,
                                onValueChange = {
                                    currentReading = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Введите показания", fontSize = 14.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                        }
                    }
                }

                // КНОПКА "ПЕРЕДАТЬ ПОКАЗАНИЯ" (ЗАНИМАЕТ ВСЮ ШИРИНУ)
                Button(
                    onClick = {
                        val current = currentReading.toDoubleOrNull()
                        val prev = previousReading.toDoubleOrNull()
                        val tar = tariff.toDoubleOrNull()

                        if (current != null && prev != null && tar != null) {
                            if (current < prev) {
                                resultText = "⚠️ ВНИМАНИЕ!\nТекущие показания меньше предыдущих.\nВозможно, был сброс счётчика."
                                showResult = true
                            } else {
                                val consumption = current - prev
                                val amount = consumption * tar
                                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                val displayDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

                                resultText = """
                                    📊 ПОКАЗАНИЯ ПЕРЕДАНЫ
                                    
                                    📈 ИЗРАСХОДОВАНО: ${String.format("%.1f", consumption)} кВт·ч
                                    💰 ТАРИФ: ${String.format("%.2f", tar)} ₽/кВт·ч
                                    🏦 СУММА К ОПЛАТЕ: ${String.format("%.2f", amount)} ₽
                                    
                                    📅 Дата передачи: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}
                                    🔄 Показания: ${prev.toInt()} → ${current.toInt()}
                                    
                                    ✅ Предыдущие показания обновлены
                                    ✅ Запись добавлена в историю
                                """.trimIndent()

                                AppState.addNewReading(
                                    date = date,
                                    previous = prev,
                                    current = current,
                                    tariff = tar
                                )

                                // 🔧 АВТОМАТИЧЕСКАЯ ОТПРАВКА ИСТОРИИ НА ПОЧТУ 🔧
                                EmailSender.exportAndSendHistory(context)

                                // ЕСЛИ НАПОМИНАНИЯ БЫЛИ ВКЛЮЧЕНЫ - ОСТАНАВЛИВАЕМ ИХ
                                if (AppState.isReminderEnabled) {
                                    val scheduler = ReminderScheduler(context)
                                    scheduler.cancelAllReminders()

                                    Toast.makeText(
                                        context,
                                        "✅ Показания переданы! Напоминания остановлены до 24 числа следующего месяца",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Показания переданы! Сумма: ${String.format("%.2f", amount)} ₽\nЗапись добавлена в историю",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                // Обновляем данные из истории (после добавления записи)
                                previousReading = AppState.getLatestReading()
                                lastReadingDate = displayDate
                                currentReading = "" // ОЧИЩАЕМ ПОЛЕ ТЕКУЩИХ ПОКАЗАНИЙ
                                isPreviousLocked = true
                                saveData()
                                showResult = true
                            }
                        } else {
                            resultText = "❌ ОШИБКА!\nПожалуйста, заполните все поля\nкорректными числами!"
                            showResult = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8C00)
                    )
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("ПЕРЕДАТЬ ПОКАЗАНИЯ", fontSize = 12.sp)
                }

                // РЕЗУЛЬТАТ РАСЧЁТА
                if (showResult && resultText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD4EDDA)
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 РЕЗУЛЬТАТ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF155724),
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Расчёт", resultText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Результат скопирован!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.CopyAll, contentDescription = "Копировать",
                                        tint = Color(0xFF155724), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = resultText,
                                color = Color(0xFF155724),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ИНФОРМАЦИОННАЯ КАРТОЧКА
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 КАК ПОЛЬЗОВАТЬСЯ",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C757D),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Введите ТЕКУЩИЕ показания\n" +
                                    "2. Нажмите 'ПЕРЕДАТЬ ПОКАЗАНИЯ'\n" +
                                    "3. Результат появится ниже\n" +
                                    "4. Для изменения тарифа или\n   предыдущих показаний нажмите\n   на замок 🔒 рядом с полем",
                            color = Color(0xFF6C757D),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ЭКРАН НАПОМИНАНИЙ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isReminderEnabled by remember { mutableStateOf(AppState.isReminderEnabled) }
    
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
                            Text(
                                text = if (isReminderEnabled) "🔔 ВКЛЮЧЕНО" else "🔕 ВЫКЛЮЧЕНО",
                                fontSize = 12.sp,
                                color = if (isReminderEnabled) Color(0xFF28A745) else Color.Gray
                            )
                        }
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { newState ->
                                isReminderEnabled = newState
                                AppState.isReminderEnabled = newState

                                // Сохраняем в SharedPreferences
                                val prefs = context.getSharedPreferences("electricity_counter", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("reminder_enabled", newState).apply()

                                if (newState) {
                                    // ВКЛЮЧАЕМ НАПОМИНАНИЯ
                                    val scheduler = ReminderScheduler(context)

                                    // Проверяем разрешения
                                    if (PermissionHelper.hasNotificationPermission(context)) {
                                        scheduler.scheduleMonthlyReminder()
                                        Toast.makeText(
                                            context,
                                            "✅ Напоминания включены!\nНачнутся 24 числа в 12:00",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        // Запрашиваем разрешение
                                        PermissionHelper.requestNotificationPermissionIfNeeded(context)
                                        Toast.makeText(
                                            context,
                                            "📱 Разрешите уведомления в настройках приложения",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        // Сбрасываем переключатель, если нет разрешений
                                        isReminderEnabled = false
                                        AppState.isReminderEnabled = false
                                    }
                                } else {
                                    // ВЫКЛЮЧАЕМ НАПОМИНАНИЯ
                                    val scheduler = ReminderScheduler(context)
                                    scheduler.cancelAllReminders()
                                    Toast.makeText(
                                        context,
                                        "🔕 Напоминания выключены",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                    
                    // 🔧 КАРТОЧКА УПРАВЛЕНИЯ ИСТОРИЕЙ И НАПОМИНАНИЯМИ 🔧
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
                            // ЗАГОЛОВОК
                            Text(
                                text = "📁 УПРАВЛЕНИЕ ИСТОРИЕЙ",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72),
                                fontSize = 16.sp
                            )
                            
                            // КНОПКА ЭКСПОРТА
                            Button(
                                onClick = {
                                    EmailSender.exportAndSendHistory(context)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF28A745)
                                )
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("📤 ЭКСПОРТ И ОТПРАВКА ИСТОРИИ")
                            }
                            
                            // КНОПКА ИМПОРТА ИСТОРИИ
                            val filePickerLauncher = FileHelper.rememberFilePicker { uri ->
                                if (uri != null) {
                                    FileHelper.importHistoryFromFile(context, uri)
                                }
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch("text/plain")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF17A2B8)
                                )
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("📥 ИМПОРТ ИСТОРИИ ИЗ ФАЙЛА")
                            }
                            
                            // ОТСТУП
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // СТАТУС НАПОМИНАНИЙ
                            Column {
                                Text(
                                    text = "📊 СТАТУС НАПОМИНАНИЙ:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3C72),
                                    fontSize = 14.sp
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🔢",
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text(
                                        text = "Последние показания: ${AppState.getLatestReading()}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF333333)
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "📋",
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text(
                                        text = "Дата последних показаний: ${AppState.getLatestReadingDate()}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF333333)
                                    )
                                }
                            }
                            
                            // КНОПКА ТЕСТИРОВАНИЯ УВЕДОМЛЕНИЙ
                            Button(
                                onClick = {
                                    // Проверяем разрешения
                                    if (PermissionHelper.hasNotificationPermission(context)) {
                                        try {
                                            NotificationHelper(context).showReminderNotification()
                                            Toast.makeText(
                                                context,
                                                "🔔 Тестовое уведомление отправлено!\nПроверь верхнюю шторку",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "❌ Ошибка при отправке: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        // Запрашиваем разрешение
                                        PermissionHelper.requestNotificationPermissionIfNeeded(context)
                                        Toast.makeText(
                                            context,
                                            "📱 Разрешите уведомления в настройках приложения",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF28A745)
                                )
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("🔔 ТЕСТ УВЕДОМЛЕНИЯ")
                            }
                            
                            // ИНФОРМАЦИЯ О ФАЙЛАХ
                            Text(
                                text = "💡 Файл истории сохраняется в папке Downloads\nи отправляется на почту lbvsx@mail.ru",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    // ИНФОРМАЦИОННАЯ КАРТОЧКА
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 ВАЖНАЯ ИНФОРМАЦИЯ",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6C757D),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Напоминания работают в фоновом режиме\n" +
                                     "• Уведомления появляются в верхней шторке\n" +
                                     "• Начинаются с 24 числа каждого месяца\n" +
                                     "• Приходят ежедневно в 12:00\n" +
                                     "• Автоматически останавливаются после ввода\n   новых показаний\n" +
                                     "• Для работы нужны разрешения на уведомления",
                                color = Color(0xFF6C757D),
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    // КНОПКА НАЗАД
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3C72)
                        )
                    ) {
                        Text("← ВЕРНУТЬСЯ", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ЭКРАН ИСТОРИИ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val historyItems = AppState.historyItems
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // ПРАВИЛЬНАЯ СТАТИСТИКА
    val totalPaid = historyItems.sumOf { it.amount }
    val totalConsumption = historyItems.sumOf { it.consumption }
    val averageConsumption = if (historyItems.isNotEmpty()) totalConsumption / historyItems.size else 0.0
    val averagePerYear = averageConsumption * 12
    
    val context = LocalContext.current
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("🗑️ УДАЛИТЬ ПОСЛЕДНЮЮ ЗАПИСЬ?", fontSize = 16.sp)
            },
            text = {
                Column {
                    Text("Вы уверены, что хотите удалить последнюю запись из истории?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (historyItems.isNotEmpty()) {
                        val lastItem = historyItems.first()
                        Text(
                            text = "${lastItem.date}: ${lastItem.previous.toInt()} → ${lastItem.current.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Расход: ${String.format("%.0f", lastItem.consumption)} кВт·ч",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Сумма: ${String.format("%.2f", lastItem.amount)} ₽",
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Эта операция необратима!",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppState.deleteLastReading()
                        showDeleteDialog = false
                        Toast.makeText(context, "Последняя запись удалена", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("УДАЛИТЬ", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("ОТМЕНА", fontSize = 12.sp)
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("📊 ИСТОРИЯ РАСЧЁТОВ", fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // КНОПКА УДАЛЕНИЯ ТОЛЬКО ДЛЯ ПОСЛЕДНЕЙ ЗАПИСИ
                    if (historyItems.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = historyItems.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить последнюю запись",
                                tint = if (historyItems.isNotEmpty()) Color.Red else Color.Gray
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // КОМПАКТНАЯ СТАТИСТИКА
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE7F3FF)
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📈 СТАТИСТИКА",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3C72),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "6,84 ₽/кВт·ч",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3C72),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Первая строка статистики
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Оплачено
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Оплачено",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.2f", totalPaid) + " ₽",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72)
                            )
                        }

                        // Всего кВт·ч
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Всего кВт·ч",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.0f", totalConsumption),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72)
                            )
                        }

                        // Количество расчётов
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Расчётов",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = historyItems.size.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72)
                            )
                        }
                    }

                    // Вторая строка статистики
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Средний расход в месяц
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "В среднем в месяц",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.0f", averageConsumption) + " кВт·ч",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3C72)
                            )
                        }

                        // Средний расход в год
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "В среднем в год",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.0f", averagePerYear) + " кВт·ч",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3C72)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // КНОПКА НАЗАД
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3C72)
                )
            ) {
                Text("← ВЕРНУТЬСЯ", fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // СПИСОК ИСТОРИИ
            if (historyItems.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📭",
                            fontSize = 36.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ИСТОРИЯ ПУСТА",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Выполните расчёт на главном экране,\nчтобы добавить запись",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(historyItems) { index, item ->
                        HistoryCard(
                            item = item,
                            isLatest = index == 0
                        )
                    }
                }
            }
        }
    }
}

// КАРТОЧКА ЗАПИСИ ИСТОРИИ
@Composable
fun HistoryCard(item: HistoryItem, isLatest: Boolean) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // ЗАГОЛОВОК
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLatest) "📅 ${item.date} ⭐" else "📅 ${item.date}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isLatest) Color(0xFFDC3545) else Color(0xFF1E3C72)
                )
                
                if (isLatest) {
                    Text(
                        text = "ПОСЛЕДНЯЯ",
                        fontSize = 10.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // ОСНОВНАЯ ИНФОРМАЦИЯ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ПОКАЗАНИЯ",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${item.previous.toInt()} → ${item.current.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "РАСХОД",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.0f", item.consumption)} кВт·ч",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF28A745)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // ДЕТАЛИ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ТАРИФ",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", item.tariff)} ₽",
                        fontSize = 12.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "СУММА",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%.2f", item.amount)} ₽",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC3545)
                    )
                }
            }
            
            // СТРОКА ДЛЯ БАНКА (ТОЛЬКО ДЛЯ ПОСЛЕДНЕЙ ЗАПИСИ)
            if (isLatest) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Преобразуем дату из формата "dd.MM.yyyy" в "dd.MM.yyyy"
                val bankDate = item.date
                
                // Формируем строку для банка
                val bankString = "Эл-во уч.143а - расход ${item.consumption.toInt()} кВт, показания ${item.current.toInt()} на $bankDate"
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // Заголовок строки для банка
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 ДЛЯ БАНКА",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3C72)
                            )
                            
                            // Кнопка копирования
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Для банка", bankString)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Скопировано для банка!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.CopyAll,
                                    contentDescription = "Копировать для банка",
                                    tint = Color(0xFF1E3C72),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Сама строка для банка
                        Text(
                            text = bankString,
                            fontSize = 12.sp,
                            color = Color(0xFF333333),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = "Нажмите на кнопку справа для копирования",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// КОМПОНЕНТ ДЛЯ СТАТИСТИКИ
@Composable
fun StatItem(
    value: String,
    label: String,
    fontSizeValue: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontSizeLabel: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = fontSizeValue,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3C72)
        )
        Text(
            text = label,
            fontSize = fontSizeLabel,
            color = Color.Gray
        )
    }
}