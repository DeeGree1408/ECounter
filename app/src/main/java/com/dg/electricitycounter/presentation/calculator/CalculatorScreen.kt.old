package com.dg.electricitycounter.presentation.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun CalculatorScreen(
    onNavigateToReminders: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 🔧 ПЕРЕЗАГРУЖАЕМ ДАННЫЕ ПРИ КАЖДОМ ПОЯВЛЕНИИ ЭКРАНА
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    // Показываем ошибки через Toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
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

                // НАВИГАЦИОННЫЕ КНОПКИ
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
                                // ДАТА ИЗМЕНЕНИЯ ТАРИФА
                                if (uiState.tariffChangeDate.isNotEmpty()) {
                                    Text(
                                        text = "действует с ${uiState.tariffChangeDate}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = uiState.tariff,
                                onValueChange = viewModel::onTariffChange,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isTariffLocked,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = viewModel::toggleTariffLock,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            if (uiState.isTariffLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Защита",
                                            tint = if (uiState.isTariffLocked) Color.Gray else Color(0xFF28A745),
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
                                    text = uiState.lastReadingDate,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            OutlinedTextField(
                                value = uiState.previousReading,
                                onValueChange = viewModel::onPreviousReadingChange,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isPreviousLocked,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = viewModel::togglePreviousLock,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            if (uiState.isPreviousLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Защита",
                                            tint = if (uiState.isPreviousLocked) Color.Gray else Color(0xFF28A745),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // ТЕКУЩИЕ ПОКАЗАНИЯ
                        Column {
                            Text(
                                text = "НОВЫЕ ПОКАЗАНИЯ, кВт",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.currentReading,
                                onValueChange = viewModel::onCurrentReadingChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Введите показания", fontSize = 14.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)  // ← ДОБАВЬ ЭТУ СТРОКУ
                            )
                        }
                    }
                }

                // КНОПКА "ПЕРЕДАТЬ ПОКАЗАНИЯ"
                Button(
                    onClick = viewModel::submitReading,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8C00)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("ПЕРЕДАТЬ ПОКАЗАНИЯ", fontSize = 14.sp)
                }

                // РЕЗУЛЬТАТ РАСЧЁТА
                if (uiState.showResult && uiState.resultText.isNotEmpty()) {
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
                                        val clip = ClipData.newPlainText("Расчёт", uiState.resultText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Результат скопирован!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CopyAll,
                                        contentDescription = "Копировать",
                                        tint = Color(0xFF155724),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.resultText,
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
            fontSize = 14.sp,
            maxLines = 1,
            letterSpacing = (-0.5).sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}
