package com.dg.electricitycounter.presentation.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dg.electricitycounter.ReminderScheduler
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.domain.model.Reading
import com.dg.electricitycounter.domain.repository.ReadingRepository
import com.dg.electricitycounter.domain.usecase.AddReadingResult
import com.dg.electricitycounter.domain.usecase.AddReadingUseCase
import com.dg.electricitycounter.domain.usecase.DeleteLatestReadingUseCase
import com.dg.electricitycounter.domain.usecase.GetAllReadingsUseCase
import com.dg.electricitycounter.domain.usecase.GetLatestReadingUseCase
import com.dg.electricitycounter.util.formatToDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addReadingUseCase: AddReadingUseCase,
    private val getLatestReadingUseCase: GetLatestReadingUseCase,
    private val getAllReadingsUseCase: GetAllReadingsUseCase,
    private val deleteLatestReadingUseCase: DeleteLatestReadingUseCase,
    private val preferencesHelper: PreferencesHelper,
    private val repository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    //  Членский взнос: ключи
    private val memPrefsName = "membership_fee_prefs_v2"
    private val keyNum = "membership_number"
    private val keyArea = "membership_area"
    private val keyTariff = "membership_tariff"
    private val keyHistory = "membership_fee_history"

    private var prevMembershipNumber: String = ""
    private var prevMembershipArea: String = ""
    private var prevMembershipTariff: String = ""

    init {
        loadData()
        loadMembershipSettings()
    }

    // ==========================================
    // 🔌 ЭЛЕКТРИЧЕСТВО
    // ==========================================
    fun loadData() {
        viewModelScope.launch {
            val tariff = preferencesHelper.getTariff()
            val tariffChangeDate = preferencesHelper.getTariffChangeDate()
            val isTariffLocked = preferencesHelper.isTariffLocked()
            val isPreviousLocked = preferencesHelper.isPreviousLocked()

            _uiState.update {
                it.copy(
                    tariff = tariff,
                    tariffChangeDate = tariffChangeDate,
                    isTariffLocked = isTariffLocked,
                    isPreviousLocked = isPreviousLocked
                )
            }

            getLatestReadingUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { reading ->
                    _uiState.update { state ->
                        state.copy(
                            previousReading = reading?.currentReading?.toInt()?.toString() ?: "",
                            lastReadingDate = reading?.date?.formatToDisplay() ?: "",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onCurrentReadingChange(value: String) {
        _uiState.update { it.copy(currentReading = value, error = null) }
    }

    fun onTariffChange(value: String) {
        val oldTariff = _uiState.value.tariff
        _uiState.update { it.copy(tariff = value, error = null) }
        preferencesHelper.saveTariff(value)
        if (oldTariff != value && value.isNotEmpty()) {
            val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            preferencesHelper.saveTariffChangeDate(currentDate)
            _uiState.update { it.copy(tariffChangeDate = currentDate) }
        }
    }

    fun onPreviousReadingChange(value: String) {
        _uiState.update { it.copy(previousReading = value, error = null) }
    }

    fun toggleTariffLock() {
        val newState = !_uiState.value.isTariffLocked
        _uiState.update { it.copy(isTariffLocked = newState) }
        preferencesHelper.setTariffLocked(newState)
    }

    fun togglePreviousLock() {
        val newState = !_uiState.value.isPreviousLocked
        _uiState.update { it.copy(isPreviousLocked = newState) }
        preferencesHelper.setPreviousLocked(newState)
    }

    fun submitReading() {
        viewModelScope.launch {
            val state = _uiState.value
            val current = state.currentReading.toDoubleOrNull()
            val previous = state.previousReading.toDoubleOrNull()
            val tariff = state.tariff.toDoubleOrNull()

            if (current == null || previous == null || tariff == null) {
                _uiState.update { it.copy(error = " Заполните все поля корректными числами!") }
                return@launch
            }
            if (current < previous) {
                _uiState.update {
                    it.copy(
                        error = "️ ВНИМАНИЕ!\nТекущие показания меньше предыдущих.\nВозможно, был сброс счётчика.",
                        showResult = true
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            addReadingUseCase(previous = previous, current = current, tariff = tariff)
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { result ->
                    when (result) {
                        is AddReadingResult.Success -> handleSuccess(result.reading, current)
                        is AddReadingResult.NeedReplacement -> _uiState.update {
                            it.copy(
                                showReplaceDialog = true,
                                existingReading = result.existingReading,
                                newReading = result.newReading,
                                isLoading = false
                            )
                        }
                        is AddReadingResult.OutsidePeriod -> _uiState.update {
                            it.copy(
                                showPeriodErrorDialog = true,
                                periodErrorDay = result.currentDay,
                                isLoading = false
                            )
                        }
                        is AddReadingResult.Error -> _uiState.update {
                            it.copy(error = result.message, isLoading = false)
                        }
                    }
                }
        }
    }

    fun confirmReplacement() {
        viewModelScope.launch {
            val newReading = _uiState.value.newReading ?: return@launch
            _uiState.update { it.copy(isLoading = true, showReplaceDialog = false) }
            try {
                deleteLatestReadingUseCase()
                repository.addReading(newReading)
                handleSuccess(newReading, newReading.currentReading)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun dismissReplaceDialog() {
        _uiState.update {
            it.copy(
                showReplaceDialog = false,
                existingReading = null,
                newReading = null,
                isLoading = false
            )
        }
    }

    fun dismissPeriodErrorDialog() {
        _uiState.update { it.copy(showPeriodErrorDialog = false, periodErrorDay = 0) }
    }

    private suspend fun handleSuccess(reading: Reading, current: Double) {
        _uiState.update {
            it.copy(
                currentReading = "",
                previousReading = current.toInt().toString(),
                lastReadingDate = reading.date.formatToDisplay(),
                resultText = formatResult(reading),
                showResult = true,
                error = null,
                isLoading = false,
                isPreviousLocked = true,
                showReplaceDialog = false,
                existingReading = null,
                newReading = null
            )
        }
        preferencesHelper.setPreviousLocked(true)
        stopRemindersIfEnabled()
        createUnifiedBackup()
    }

    private fun stopRemindersIfEnabled() {
        if (preferencesHelper.isReminderEnabled()) {
            val scheduler = ReminderScheduler(context)
            scheduler.cancelReminders()
            scheduler.scheduleReminder()
        }
    }

    // ✅ ОБЪЕДИНЁННЫЙ БЭКАП: ЭЭ + История ЧВ
    private fun createUnifiedBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ru = Locale("ru", "RU")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", ru).format(Date())
                val fileName = "backup_full_$timestamp.txt"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                // 1. Секция ЭЛЕКТРИЧЕСТВА
                val readings = getAllReadingsUseCase().first()
                val electricitySection = if (readings.isNotEmpty()) {
                    "#ELECTRICITY\n" + readings.joinToString("\n") { r ->
                        val date = SimpleDateFormat("dd.MM.yyyy", ru).format(Date(r.date))
                        "$date ${r.currentReading.toInt()} ${r.consumption.toInt()} ${String.format(ru, "%.2f", r.tariff)} ${String.format(ru, "%.2f", r.amount)}"
                    }
                } else "#ELECTRICITY\n"

                // 2. Секция ЧЛЕНСКОГО ВЗНОСА (берём из сохранённой истории)
                val history = getMembershipHistory()
                val membershipSection = if (history.isNotBlank()) {
                    "\n#MEMBERSHIP_FEE\n$history"
                } else {
                    // Fallback: если истории нет, генерируем текущие значения
                    val s = _uiState.value
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    val date = SimpleDateFormat("dd.MM.yyyy", ru).format(cal.time)
                    val area = s.membershipPlotArea.replace(".", ",")
                    val tariff = s.membershipTariff.replace(".", "").replace(",", "").trim()
                    val sum = s.membershipFeeTotal.replace("₽", "").replace(" ", "").replace(".", ",")
                    "\n#MEMBERSHIP_FEE\n$date $area $tariff $sum"
                }

                file.writeText(electricitySection + membershipSection, Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("lbvsx@mail.ru"))
                        putExtra(Intent.EXTRA_SUBJECT, "Бэкап: ЭЭ + ЧВ $timestamp")
                        putExtra(Intent.EXTRA_TEXT, "История показаний во вложении.\n\nОтправлено из приложения Электросчётчик")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Отправить бэкап").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    Toast.makeText(context, "✅ Файл сохранён: $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show() }
                e.printStackTrace()
            }
        }
    }

    private fun formatResult(reading: Reading): String {
        val ru = Locale("ru", "RU")
        return """
        📊 ПОКАЗАНИЯ ПЕРЕДАНЫ
        
        📈 ИЗРАСХОДОВАНО: ${String.format(ru, "%.1f", reading.consumption)} кВт·ч
        💰 ТАРИФ: ${String.format(ru, "%.2f", reading.tariff)} ₽/кВт·ч
        🏦 СУММА К ОПЛАТЕ: ${String.format(ru, "%.2f", reading.amount)} ₽
        
         Дата передачи: ${reading.date.formatToDisplay()}
        🔄 Показания: ${reading.previousReading.toInt()} → ${reading.currentReading.toInt()}
        
        ✅ Предыдущие показания обновлены | ✅ Запись в истории | 📧 Отправлено
        ${if (preferencesHelper.isReminderEnabled()) "\n🔕 Напоминания остановлены" else ""}
        """.trimIndent()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ==========================================
    // 💰 ЧЛЕНСКИЙ ВЗНОС (ЧВ)
    // ==========================================
    fun loadMembershipSettings() {
        val prefs = context.getSharedPreferences(memPrefsName, Context.MODE_PRIVATE)
        val number = prefs.getString(keyNum, "143а") ?: "143а"
        val area = prefs.getString(keyArea, "") ?: ""
        val tariff = prefs.getString(keyTariff, "") ?: ""

        _uiState.update {
            it.copy(
                membershipPlotNumber = number,
                membershipPlotArea = area,
                membershipTariff = tariff
            )
        }
        prevMembershipNumber = number
        prevMembershipArea = area
        prevMembershipTariff = tariff
        calculateMembershipFee()
    }

    fun onMembershipNumberChange(value: String) {
        _uiState.update { it.copy(membershipPlotNumber = value) }
    }

    fun onMembershipAreaChange(value: String) {
        _uiState.update { it.copy(membershipPlotArea = value) }
        calculateMembershipFee()
    }

    fun onMembershipTariffChange(value: String) {
        _uiState.update { it.copy(membershipTariff = value) }
        calculateMembershipFee()
    }

    // ✅ ЗАМКИ: сохраняем ТОЛЬКО при изменении Тарифа
    fun toggleMembershipNumberLock() {
        val s = _uiState.value
        _uiState.update { it.copy(isMembershipNumberLocked = !s.isMembershipNumberLocked) }
    }

    fun toggleMembershipAreaLock() {
        val s = _uiState.value
        _uiState.update { it.copy(isMembershipAreaLocked = !s.isMembershipAreaLocked) }
    }

    fun toggleMembershipTariffLock() {
        val s = _uiState.value
        val locked = !s.isMembershipTariffLocked
        if (locked && s.membershipTariff != prevMembershipTariff) {
            saveMembershipData()
        }
        prevMembershipTariff = s.membershipTariff
        _uiState.update { it.copy(isMembershipTariffLocked = locked) }
    }

    // ✅ СОХРАНЕНИЕ С ЗАПИСЬЮ В ИСТОРИЮ
    private fun saveMembershipData() {
        val s = _uiState.value
        val ru = Locale("ru", "RU")
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val dateStr = SimpleDateFormat("dd.MM.yyyy", ru).format(cal.time)
        val area = s.membershipPlotArea.replace(".", ",")
        val tariff = s.membershipTariff.replace(".", "").replace(",", "").trim()
        val sum = s.membershipFeeTotal.replace("₽", "").replace(" ", "").replace(".", ",")
        val newLine = "$dateStr $area $tariff $sum"

        val oldHistory = getMembershipHistory()
        val firstLine = oldHistory.lines().firstOrNull()?.trim()

        // Добавляем только если изменилось
        val updatedHistory = if (firstLine != newLine) {
            if (oldHistory.isBlank()) newLine else "$newLine\n$oldHistory"
        } else oldHistory

        saveMembershipHistory(updatedHistory)

        val prefs = context.getSharedPreferences(memPrefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(keyNum, s.membershipPlotNumber)
            .putString(keyArea, s.membershipPlotArea)
            .putString(keyTariff, s.membershipTariff)
            .apply()

        createUnifiedBackup()
    }

    private fun getMembershipHistory(): String {
        return context.getSharedPreferences(memPrefsName, Context.MODE_PRIVATE).getString(keyHistory, "") ?: ""
    }

    private fun saveMembershipHistory(history: String) {
        context.getSharedPreferences(memPrefsName, Context.MODE_PRIVATE).edit().putString(keyHistory, history).apply()
    }

    fun copyMembershipToClipboard() {
        val s = _uiState.value
        val monthName = getPreviousMonthName()
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val text = "Членский взнос уч.${s.membershipPlotNumber} за $monthName $year"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("membership", text))
        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
    }

    private fun calculateMembershipFee() {
        val s = _uiState.value
        val area = s.membershipPlotArea.toFloatOrNull() ?: 0f
        val tariff = s.membershipTariff.toFloatOrNull() ?: 0f
        _uiState.update {
            it.copy(membershipFeeTotal = String.format(Locale("ru", "RU"), "%.2f ₽", area * tariff))
        }
    }

    private fun getPreviousMonthName(): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        return listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")[calendar.get(Calendar.MONTH)]
    }
}