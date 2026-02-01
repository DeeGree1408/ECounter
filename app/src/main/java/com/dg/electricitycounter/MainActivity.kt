package com.dg.electricitycounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.domain.usecase.CheckDataExistsUseCase
import com.dg.electricitycounter.presentation.calculator.CalculatorScreen
import com.dg.electricitycounter.presentation.history.HistoryScreen
import com.dg.electricitycounter.presentation.reminders.RemindersScreen
import com.dg.electricitycounter.presentation.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val checkDataExistsUseCase: CheckDataExistsUseCase
) : ViewModel() {
    
    private val _hasData = MutableStateFlow<Boolean?>(null)
    val hasData: StateFlow<Boolean?> = _hasData.asStateFlow()
    
    init {
        checkData()
    }
    
    private fun checkData() {
        viewModelScope.launch {
            checkDataExistsUseCase()
                .collect { exists ->
                    _hasData.value = exists
                }
        }
    }
    
    fun onDataImported() {
        _hasData.value = true
    }
}

@Composable
fun AppRoot(
    viewModel: AppViewModel = hiltViewModel()
) {
    val hasData by viewModel.hasData.collectAsStateWithLifecycle()
    
    when (hasData) {
        null -> {
            // Загрузка
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Можно показать Splash Screen
            }
        }
        false -> {
            // Первый запуск - показываем Welcome Screen
            WelcomeScreen(
                onComplete = {
                    viewModel.onDataImported()
                }
            )
        }
        true -> {
            // Данные есть - показываем обычное приложение
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
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
