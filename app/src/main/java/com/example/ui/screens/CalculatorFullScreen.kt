package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculatorHistoryRecord
import com.example.service.ChronosAudioSynthesizer
import com.example.service.ConversionCategory
import com.example.service.SmartCalculatorEngine
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBlueNeon
import com.example.ui.components.ChronosCardNavy
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosOrangeNeon
import com.example.ui.components.ChronosPurpleNeon
import com.example.ui.components.ChronosRoseNeon
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.theme.JetBrainsMonoFontFamily

private data class CalcCategoryItem(
    val index: Int,
    val title: String,
    val shortBadge: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun CalculatorFullScreen(
    appLanguage: String,
    historyRecords: List<CalculatorHistoryRecord>,
    onBack: () -> Unit,
    onOpenFeatureHub: () -> Unit,
    onAddHistory: (categoryName: String, expression: String, result: String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val isId = appLanguage != "en"

    val categories = remember {
        listOf(
            CalcCategoryItem(0, "Kalkulator Dasar", "DASAR", Icons.Default.Calculate, ChronosCyanNeon),
            CalcCategoryItem(1, "Kalkulator Ilmiah", "ILMIAH", Icons.Default.Functions, ChronosPurpleNeon),
            CalcCategoryItem(2, "Kalkulator Persentase", "PERSEN", Icons.Default.Percent, ChronosAmberNeon),
            CalcCategoryItem(3, "Kalkulator Keuangan", "KEUANGAN", Icons.Default.AccountBalance, ChronosGreenNeon),
            CalcCategoryItem(4, "Kalkulator Konversi", "KONVERSI", Icons.Default.SwapHoriz, ChronosBlueNeon),
            CalcCategoryItem(5, "Kalkulator Tanggal dan Waktu", "WAKTU", Icons.Default.CalendarMonth, ChronosOrangeNeon),
            CalcCategoryItem(6, "Kalkulator Statistik", "STATISTIK", Icons.Default.Analytics, ChronosCyanNeon),
            CalcCategoryItem(7, "Kalkulator Programmer", "PROGRAMMER", Icons.Default.Code, ChronosRoseNeon),
            CalcCategoryItem(8, "Riwayat Perhitungan", "RIWAYAT", Icons.Default.History, ChronosAmberNeon),
            CalcCategoryItem(9, "Pengaturan Kalkulator", "PENGATURAN", Icons.Default.Settings, ChronosCyanNeon)
        )
    }

    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var showCategoryGridMenu by rememberSaveable { mutableStateOf(false) }

    // Persistent expression & settings state across Basic & Scientific tabs
    var expression by rememberSaveable { mutableStateOf("") }
    var lastEvaluatedResult by rememberSaveable { mutableStateOf("0") }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    var lastRepeatOpTail by rememberSaveable { mutableStateOf("") }
    var isDegreeMode by rememberSaveable { mutableStateOf(true) }
    var memoryRegister by rememberSaveable { mutableDoubleStateOf(0.0) }
    var hasMemoryValue by rememberSaveable { mutableStateOf(false) }
    var copiedBannerText by remember { mutableStateOf<String?>(null) }

    // Calculator Preferences (Category 9: Pengaturan Kalkulator)
    var calcDecimalPrecision by rememberSaveable { mutableIntStateOf(8) }
    var useIndonesianNumberFormat by rememberSaveable { mutableStateOf(false) }
    var autoSaveHistoryOnEquals by rememberSaveable { mutableStateOf(true) }
    var hapticKeypressEnabled by rememberSaveable { mutableStateOf(true) }

    val liveEvaluation = remember(expression, isDegreeMode, memoryRegister, calcDecimalPrecision, useIndonesianNumberFormat) {
        val raw = SmartCalculatorEngine.evaluateExpression(
            rawExpr = expression,
            isDegreeMode = isDegreeMode,
            memoryValue = memoryRegister
        )
        if (raw.isValid && expression.isNotBlank()) {
            raw.copy(
                formattedResult = SmartCalculatorEngine.formatNumber(
                    raw.numericValue,
                    useIndonesianFormat = useIndonesianNumberFormat,
                    maxDecimals = calcDecimalPrecision
                )
            )
        } else {
            raw
        }
    }

    fun triggerCalcHaptic(level: Int = 0) {
        if (hapticKeypressEnabled) {
            ChronosAudioSynthesizer.triggerHaptic(context, level)
        }
    }

    fun copyToClipboard(label: String, text: String) {
        triggerCalcHaptic(1)
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        copiedBannerText = if (isId) "Disalin: $text" else "Copied: $text"
    }

    fun appendSmartParenthesis() {
        triggerCalcHaptic(0)
        if (justEvaluated) {
            expression = "("
            justEvaluated = false
            return
        }
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val lastCh = expression.lastOrNull()
        val canClose = openCount > closeCount &&
            lastCh != null &&
            (lastCh.isDigit() || lastCh == ')' || lastCh == '!' || lastCh == '%' || lastCh == 'π' || lastCh == 'e')
        expression += if (canClose) ")" else "("
    }

    fun appendTokenToExpression(token: String) {
        triggerCalcHaptic(0)
        val binaryOps = listOf("+", "-", "×", "÷", "^", " mod ")
        val postfixOps = listOf("%", "!", "^2", "^3")

        if (justEvaluated) {
            justEvaluated = false
            if (token !in binaryOps && token !in postfixOps) {
                // Starting a new number/function after '=' replaces previous result cleanly
                expression = if (token == ".") "0." else token
                return
            }
        }

        if (token in binaryOps) {
            if (expression.isEmpty()) {
                if (token == "-") {
                    expression = "-"
                } else if (lastEvaluatedResult != "0" && !lastEvaluatedResult.contains("Tidak")) {
                    val cleanPrev = SmartCalculatorEngine.parseSmartNumber(lastEvaluatedResult)?.let { v ->
                        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
                    } ?: "0"
                    expression = cleanPrev + token
                }
                return
            }
            val trimmed = expression.trimEnd()
            if (trimmed.endsWith("mod")) {
                expression = trimmed.removeSuffix("mod").trimEnd() + token
                return
            }
            val lastChar = trimmed.lastOrNull()?.toString().orEmpty()
            if (lastChar in listOf("+", "-", "×", "÷", "^", "*", "/")) {
                expression = trimmed.dropLast(1) + token
                return
            }
        }
        if (token == ".") {
            // Prevent duplicate decimal point in the current number segment
            val lastNumberSegment = expression.takeLastWhile { it.isDigit() || it == '.' }
            if (lastNumberSegment.contains('.')) return
            if (lastNumberSegment.isEmpty()) {
                expression += "0."
                return
            }
        }
        expression += token
    }

    fun toggleNegateCurrentExpression() {
        triggerCalcHaptic(0)
        justEvaluated = false
        if (expression.isEmpty()) {
            expression = "-"
            return
        }
        if (expression.startsWith("-(") && expression.endsWith(")")) {
            expression = expression.substring(2, expression.length - 1)
        } else if (expression.startsWith("-")) {
            expression = expression.removePrefix("-")
        } else {
            expression = "-($expression)"
        }
    }

    fun executeEqualsEvaluation(catTitle: String) {
        triggerCalcHaptic(1)
        if (expression.isBlank()) return
        val exprToEval = if (justEvaluated && lastRepeatOpTail.isNotBlank()) {
            expression + lastRepeatOpTail
        } else {
            // Capture trailing binary operator + operand for repeat '=' calculation
            val match = Regex("([+\\-×÷^]|\\s+mod\\s+)([0-9.]+%?)$").find(expression.trim())
            if (match != null) {
                lastRepeatOpTail = match.value
            }
            expression
        }

        val outcome = SmartCalculatorEngine.evaluateExpression(
            rawExpr = exprToEval,
            isDegreeMode = isDegreeMode,
            memoryValue = memoryRegister
        )
        if (outcome.isValid) {
            val formatted = SmartCalculatorEngine.formatNumber(
                outcome.numericValue,
                useIndonesianFormat = useIndonesianNumberFormat,
                maxDecimals = calcDecimalPrecision
            )
            if (autoSaveHistoryOnEquals) {
                onAddHistory(catTitle, exprToEval, formatted)
            }
            lastEvaluatedResult = formatted
            expression = outcome.numericValue.let { v ->
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            justEvaluated = true
        } else {
            lastEvaluatedResult = outcome.errorMessage ?: "Ekspresi tidak valid"
            justEvaluated = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
            .testTag("calculator_smart_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // =====================================================================
        // A. HEADER: Kalkulator Smart + Back + Menu + History Shortcut
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ChronosCyanNeon.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onBack() }
                        .testTag("calculator_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ChronosCyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kalkulator Smart",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("calculator_header_title")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ChronosAmberNeon.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "PRO SUITE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = ChronosAmberNeon,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isId) "Matematika • Keuangan • Konversi • Statistik • Programmer"
                        else "Math • Finance • Conversion • Statistics • Programmer",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Quick History Shortcut Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCategoryIndex == 8) ChronosAmberNeon.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedCategoryIndex == 8) ChronosAmberNeon else Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            ChronosAudioSynthesizer.triggerHaptic(context, 0)
                            selectedCategoryIndex = 8
                        }
                        .testTag("calculator_history_shortcut")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Riwayat Perhitungan",
                            tint = ChronosAmberNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        if (historyRecords.isNotEmpty()) {
                            Text(
                                text = "${historyRecords.size}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosAmberNeon
                            )
                        }
                    }
                }

                // Feature Hub Drawer Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenFeatureHub() }
                        .testTag("calculator_open_hub_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Feature Hub",
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Copied feedback toast pill
        if (copiedBannerText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChronosGreenNeon.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(16.dp))
                        Text(
                            text = copiedBannerText.orEmpty(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosGreenNeon
                        )
                    }
                    Text(
                        text = "OK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.clickable { copiedBannerText = null }
                    )
                }
            }
        }

        // =====================================================================
        // C. ORGANIZED 10-CATEGORY MENU BAR + COMPACT GRID DRAWER TOGGLE
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (showCategoryGridMenu) ChronosCyanNeon.copy(alpha = 0.22f) else ChronosSurfaceDark.copy(alpha = 0.9f),
                border = BorderStroke(
                    1.dp,
                    if (showCategoryGridMenu) ChronosCyanNeon else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        triggerCalcHaptic(0)
                        showCategoryGridMenu = !showCategoryGridMenu
                    }
                    .testTag("calculator_toggle_category_grid")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Semua Kategori",
                        tint = ChronosCyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isId) "10 Menu" else "10 Modes",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosCyanNeon
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .testTag("calculator_category_bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(categories) { idx, cat ->
                    val isSelected = selectedCategoryIndex == idx
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) cat.color.copy(alpha = 0.22f) else ChronosSurfaceDark.copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) cat.color else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                triggerCalcHaptic(0)
                                selectedCategoryIndex = idx
                                showCategoryGridMenu = false
                            }
                            .testTag("calc_tab_${idx}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.title,
                                tint = if (isSelected) cat.color else Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = cat.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }
        }

        // Collapsible 2-Column 10-Category Selector Grid for Fast Discovery
        if (showCategoryGridMenu) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ChronosCardNavy,
                border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_category_grid_panel")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "PILIH KATEGORI KALKULATOR SMART (10 MODE)" else "SELECT SMART CALCULATOR CATEGORY (10 MODES)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = if (isId) "Tutup ✕" else "Close ✕",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.clickable { showCategoryGridMenu = false }
                        )
                    }
                    categories.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { cat ->
                                val active = selectedCategoryIndex == cat.index
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (active) cat.color.copy(alpha = 0.2f) else ChronosSurfaceDark,
                                    border = BorderStroke(1.dp, if (active) cat.color else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            triggerCalcHaptic(0)
                                            selectedCategoryIndex = cat.index
                                            showCategoryGridMenu = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = cat.title,
                                            tint = if (active) cat.color else Color(0xFF9CA3AF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = cat.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = if (active) Color.White else Color(0xFFD1D5DB),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // B. CALCULATION DISPLAY & WORKSPACE BY CATEGORY
        // =====================================================================
        when (selectedCategoryIndex) {
            0, 1 -> {
                // Shared Calculation Display for Kalkulator Dasar (0) & Kalkulator Ilmiah (1)
                CalculationDisplayCard(
                    expression = expression,
                    liveOutcome = liveEvaluation,
                    lastResult = lastEvaluatedResult,
                    isScientific = selectedCategoryIndex == 1,
                    isDegreeMode = isDegreeMode,
                    hasMemoryValue = hasMemoryValue,
                    memoryValue = memoryRegister,
                    onExpressionEdited = {
                        justEvaluated = false
                        expression = it
                    },
                    onToggleDegRad = { isDegreeMode = !isDegreeMode },
                    onClearAll = {
                        triggerCalcHaptic(1)
                        expression = ""
                        lastEvaluatedResult = "0"
                        justEvaluated = false
                        lastRepeatOpTail = ""
                    },
                    onBackspace = {
                        triggerCalcHaptic(0)
                        justEvaluated = false
                        if (expression.isNotEmpty()) {
                            val fnTokens = listOf("asin(", "acos(", "atan(", "sqrt(", "cbrt(", "sinh(", "cosh(", "tanh(", "log2(", "sin(", "cos(", "tan(", "log(", "abs(", "inv(", "ln(", " mod ")
                            val matchedFn = fnTokens.firstOrNull { expression.endsWith(it) }
                            expression = if (matchedFn != null) {
                                expression.dropLast(matchedFn.length)
                            } else {
                                expression.dropLast(1)
                            }
                        }
                    },
                    onCopyResult = { resStr -> copyToClipboard("Calculation Result", resStr) }
                )

                if (selectedCategoryIndex == 1) {
                    ScientificKeypadSection(
                        isDegreeMode = isDegreeMode,
                        onToggleDegRad = { isDegreeMode = !isDegreeMode },
                        onToken = { appendTokenToExpression(it) },
                        onWrapSquare = { appendTokenToExpression("^2") },
                        onWrapCube = { appendTokenToExpression("^3") },
                        onReciprocal = { appendTokenToExpression("inv(") }
                    )
                }

                MemoryAndBasicKeypadSection(
                    isScientificMode = selectedCategoryIndex == 1,
                    hasMemory = hasMemoryValue,
                    memoryValue = memoryRegister,
                    onMemoryClear = {
                        triggerCalcHaptic(0)
                        memoryRegister = 0.0
                        hasMemoryValue = false
                    },
                    onMemoryRecall = {
                        triggerCalcHaptic(0)
                        val memStr = if (memoryRegister == memoryRegister.toLong().toDouble()) {
                            memoryRegister.toLong().toString()
                        } else {
                            memoryRegister.toString()
                        }
                        appendTokenToExpression(memStr)
                    },
                    onMemoryAdd = {
                        triggerCalcHaptic(0)
                        val v = if (liveEvaluation.isValid) liveEvaluation.numericValue else 0.0
                        memoryRegister += v
                        hasMemoryValue = true
                    },
                    onMemorySub = {
                        triggerCalcHaptic(0)
                        val v = if (liveEvaluation.isValid) liveEvaluation.numericValue else 0.0
                        memoryRegister -= v
                        hasMemoryValue = true
                    },
                    onSmartParen = { appendSmartParenthesis() },
                    onClearEntry = {
                        triggerCalcHaptic(0)
                        justEvaluated = false
                        val trimmed = expression.trimEnd()
                        val newExpr = trimmed.dropLastWhile { it.isDigit() || it == '.' }
                        expression = if (newExpr.length < trimmed.length) newExpr else ""
                    },
                    onClearAll = {
                        triggerCalcHaptic(1)
                        expression = ""
                        lastEvaluatedResult = "0"
                        justEvaluated = false
                        lastRepeatOpTail = ""
                    },
                    onBackspace = {
                        triggerCalcHaptic(0)
                        justEvaluated = false
                        if (expression.isNotEmpty()) {
                            val fnTokens = listOf("asin(", "acos(", "atan(", "sqrt(", "cbrt(", "sinh(", "cosh(", "tanh(", "log2(", "sin(", "cos(", "tan(", "log(", "abs(", "inv(", "ln(", " mod ")
                            val matchedFn = fnTokens.firstOrNull { expression.endsWith(it) }
                            expression = if (matchedFn != null) {
                                expression.dropLast(matchedFn.length)
                            } else {
                                expression.dropLast(1)
                            }
                        }
                    },
                    onNegate = { toggleNegateCurrentExpression() },
                    onToken = { appendTokenToExpression(it) },
                    onEquals = {
                        val catTitle = categories[selectedCategoryIndex].title
                        executeEqualsEvaluation(catTitle)
                    }
                )
            }

            2 -> {
                PercentageCalculatorSection(
                    isId = isId,
                    onSaveToHistory = { expr, res ->
                        onAddHistory("Kalkulator Persentase", expr, res)
                    },
                    onCopy = { copyToClipboard("Persentase", it) }
                )
            }

            3 -> {
                FinancialCalculatorSection(
                    isId = isId,
                    onSaveToHistory = { expr, res ->
                        onAddHistory("Kalkulator Keuangan", expr, res)
                    },
                    onCopy = { copyToClipboard("Keuangan", it) }
                )
            }

            4 -> {
                UnitConversionCalculatorSection(
                    isId = isId,
                    onSaveToHistory = { expr, res ->
                        onAddHistory("Kalkulator Konversi", expr, res)
                    },
                    onCopy = { copyToClipboard("Konversi", it) }
                )
            }

            5 -> {
                DateAndTimeCalculatorSection(
                    isId = isId,
                    onSaveToHistory = { expr, res ->
                        onAddHistory("Kalkulator Tanggal dan Waktu", expr, res)
                    },
                    onCopy = { copyToClipboard("Tanggal & Waktu", it) }
                )
            }

            6 -> {
                StatisticsCalculatorSection(
                    isId = isId,
                    onSaveToHistory = { expr, res ->
                        onAddHistory("Kalkulator Statistik", expr, res)
                    },
                    onCopy = { copyToClipboard("Statistik", it) }
                )
            }

            7 -> {
                ProgrammerCalculatorSection(
                    isId = isId,
                    onSaveToHistory = { expr, res ->
                        onAddHistory("Kalkulator Programmer", expr, res)
                    },
                    onCopy = { copyToClipboard("Programmer", it) }
                )
            }

            8 -> {
                HistoryCalculatorSection(
                    isId = isId,
                    records = historyRecords,
                    onReuseExpression = { rec ->
                        triggerCalcHaptic(1)
                        val parsed = SmartCalculatorEngine.parseSmartNumber(rec.result)
                        expression = if (parsed != null) {
                            if (parsed == parsed.toLong().toDouble()) parsed.toLong().toString() else parsed.toString()
                        } else {
                            rec.result.replace("Rp ", "").replace(",", "")
                        }
                        justEvaluated = false
                        selectedCategoryIndex = 0
                    },
                    onCopyRecord = { rec ->
                        copyToClipboard("Riwayat", "${rec.expression} = ${rec.result}")
                    },
                    onDeleteRecord = onDeleteHistoryItem,
                    onClearAllHistory = onClearHistory
                )
            }

            9 -> {
                CalculatorSettingsSection(
                    isId = isId,
                    decimalPrecision = calcDecimalPrecision,
                    onChangePrecision = { calcDecimalPrecision = it },
                    isDegreeMode = isDegreeMode,
                    onChangeDegreeMode = { isDegreeMode = it },
                    useIndonesianFormat = useIndonesianNumberFormat,
                    onChangeIndonesianFormat = { useIndonesianNumberFormat = it },
                    autoSaveHistory = autoSaveHistoryOnEquals,
                    onChangeAutoSaveHistory = { autoSaveHistoryOnEquals = it },
                    hapticEnabled = hapticKeypressEnabled,
                    onChangeHaptic = { hapticKeypressEnabled = it },
                    onResetWorkspace = {
                        triggerCalcHaptic(1)
                        expression = ""
                        lastEvaluatedResult = "0"
                        justEvaluated = false
                        lastRepeatOpTail = ""
                        memoryRegister = 0.0
                        hasMemoryValue = false
                        calcDecimalPrecision = 8
                        isDegreeMode = true
                        useIndonesianNumberFormat = false
                        autoSaveHistoryOnEquals = true
                    }
                )
            }
        }
    }
}

// =============================================================================
// B. SHARED CALCULATION DISPLAY CARD (Basic & Scientific)
// =============================================================================
@Composable
private fun CalculationDisplayCard(
    expression: String,
    liveOutcome: com.example.service.CalcEvalOutcome,
    lastResult: String,
    isScientific: Boolean,
    isDegreeMode: Boolean,
    hasMemoryValue: Boolean,
    memoryValue: Double,
    onExpressionEdited: (String) -> Unit,
    onToggleDegRad: () -> Unit,
    onClearAll: () -> Unit,
    onBackspace: () -> Unit,
    onCopyResult: (String) -> Unit
) {
    val displayResultText = when {
        expression.isEmpty() -> lastResult
        liveOutcome.isValid -> liveOutcome.formattedResult
        else -> liveOutcome.errorMessage ?: "Ekspresi tidak valid"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.5.dp, ChronosCyanNeon.copy(alpha = 0.38f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calculator_display_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Status Row: Mode Badge, DEG/RAD toggle, Memory Indicator, Copy & Clear Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isScientific) ChronosPurpleNeon.copy(alpha = 0.18f) else ChronosCyanNeon.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (isScientific) ChronosPurpleNeon else ChronosCyanNeon)
                    ) {
                        Text(
                            text = if (isScientific) "MODE ILMIAH" else "MODE DASAR",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isScientific) ChronosPurpleNeon else ChronosCyanNeon,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChronosAmberNeon.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleDegRad() }
                            .testTag("calc_deg_rad_toggle")
                    ) {
                        Text(
                            text = if (isDegreeMode) "DEG (°)" else "RAD (π)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosAmberNeon,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (hasMemoryValue) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ChronosGreenNeon.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "M: ${SmartCalculatorEngine.formatNumber(memoryValue, maxDecimals = 4)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosGreenNeon,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onCopyResult(displayResultText) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("calc_copy_result_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Hasil",
                            tint = ChronosCyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onBackspace,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("calc_display_backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Hapus Karakter Terakhir",
                            tint = ChronosAmberNeon,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("calc_display_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Bersihkan Semua",
                            tint = ChronosRoseNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Editable & Scrollable Expression Field
            OutlinedTextField(
                value = expression,
                onValueChange = onExpressionEdited,
                placeholder = {
                    Text(
                        text = "0",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 22.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.End
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChronosCyanNeon.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
                    cursorColor = ChronosCyanNeon
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_expression_input")
            )

            // Formatted Live Result Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "= ",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = ChronosCyanNeon
                )
                Text(
                    text = displayResultText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (displayResultText.length > 16) 22.sp else 30.sp,
                    fontWeight = FontWeight.Black,
                    color = if (expression.isNotEmpty() && !liveOutcome.isValid) ChronosRoseNeon else ChronosCyanNeon,
                    maxLines = 1,
                    modifier = Modifier.testTag("calculator_result_text")
                )
            }
        }
    }
}

// =============================================================================
// 1 & 2. SCIENTIFIC & BASIC KEYPADS
// =============================================================================
@Composable
private fun ScientificKeypadSection(
    isDegreeMode: Boolean,
    onToggleDegRad: () -> Unit,
    onToken: (String) -> Unit,
    onWrapSquare: () -> Unit,
    onWrapCube: () -> Unit,
    onReciprocal: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ChronosSurfaceDark.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, ChronosPurpleNeon.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scientific_keypad_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf(
                    Triple(if (isDegreeMode) "DEG" else "RAD", "deg_rad") { onToggleDegRad() },
                    Triple("sin", "sci_sin") { onToken("sin(") },
                    Triple("cos", "sci_cos") { onToken("cos(") },
                    Triple("tan", "sci_tan") { onToken("tan(") },
                    Triple("π", "sci_pi") { onToken("π") }
                ),
                listOf(
                    Triple("asin", "sci_asin") { onToken("asin(") },
                    Triple("acos", "sci_acos") { onToken("acos(") },
                    Triple("atan", "sci_atan") { onToken("atan(") },
                    Triple("ln", "sci_ln") { onToken("ln(") },
                    Triple("log", "sci_log") { onToken("log(") }
                ),
                listOf(
                    Triple("x²", "sci_sq") { onWrapSquare() },
                    Triple("x³", "sci_cube") { onWrapCube() },
                    Triple("xʸ", "sci_pow") { onToken("^") },
                    Triple("√x", "sci_sqrt") { onToken("√(") },
                    Triple("∛x", "sci_cbrt") { onToken("∛(") }
                ),
                listOf(
                    Triple("(", "sci_lpar") { onToken("(") },
                    Triple(")", "sci_rpar") { onToken(")") },
                    Triple("n!", "sci_fact") { onToken("!") },
                    Triple("1/x", "sci_inv") { onReciprocal() },
                    Triple("e", "sci_e") { onToken("e") }
                ),
                listOf(
                    Triple("mod", "sci_mod") { onToken(" mod ") },
                    Triple("|x|", "sci_abs") { onToken("abs(") },
                    Triple("log₂", "sci_log2") { onToken("log2(") },
                    Triple("sinh", "sci_sinh") { onToken("sinh(") },
                    Triple("cosh", "sci_cosh") { onToken("cosh(") }
                )
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (label, tag, action) ->
                        val isModeKey = tag == "deg_rad"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isModeKey) ChronosAmberNeon.copy(alpha = 0.18f) else ChronosPurpleNeon.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (isModeKey) ChronosAmberNeon.copy(alpha = 0.45f) else ChronosPurpleNeon.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { action() }
                                .testTag("calc_btn_$tag")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isModeKey) ChronosAmberNeon else Color(0xFFE9D5FF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryAndBasicKeypadSection(
    isScientificMode: Boolean = false,
    hasMemory: Boolean,
    memoryValue: Double,
    onMemoryClear: () -> Unit,
    onMemoryRecall: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySub: () -> Unit,
    onSmartParen: () -> Unit,
    onClearEntry: () -> Unit,
    onClearAll: () -> Unit,
    onBackspace: () -> Unit,
    onNegate: () -> Unit,
    onToken: (String) -> Unit,
    onEquals: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Memory Bar: MC, MR, M+, M-, ( )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val memButtons = listOf(
                Triple("MC", "mc", onMemoryClear),
                Triple("MR", "mr", onMemoryRecall),
                Triple("M+", "m_plus", onMemoryAdd),
                Triple("M-", "m_minus", onMemorySub),
                Triple("( )", "parens", onSmartParen)
            )
            memButtons.forEach { (lbl, tag, act) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChronosSurfaceDark,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { act() }
                        .testTag("calc_btn_$tag")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = lbl,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lbl.startsWith("M") && hasMemory) ChronosGreenNeon else if (tag == "parens") ChronosCyanNeon else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        // Quick Math Function Row in Basic Mode: %, √x, x², 1/x, π
        if (!isScientificMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickBasicOps = listOf(
                    Triple("%", "percent") { onToken("%") },
                    Triple("√x", "sqrt_basic") { onToken("√(") },
                    Triple("x²", "sq_basic") { onToken("^2") },
                    Triple("1/x", "inv_basic") { onToken("inv(") },
                    Triple("π", "pi_basic") { onToken("π") }
                )
                quickBasicOps.forEach { (lbl, tag, act) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ChronosCyanNeon.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { act() }
                            .testTag("calc_btn_$tag")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = lbl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosCyanNeon
                            )
                        }
                    }
                }
            }
        } else {
            // In Scientific mode, expose % directly on a compact utility row if needed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sciExtraOps = listOf(
                    Triple("%", "percent") { onToken("%") },
                    Triple("tanh", "sci_tanh") { onToken("tanh(") },
                    Triple("φ", "sci_phi") { onToken("phi") }
                )
                sciExtraOps.forEach { (lbl, tag, act) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ChronosCyanNeon.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { act() }
                            .testTag("calc_btn_$tag")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = lbl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosCyanNeon
                            )
                        }
                    }
                }
            }
        }

        // Standard 5x4 Keypad Grid
        val keypadRows = listOf(
            listOf(
                CalcKeySpec("C", "clear", ChronosRoseNeon.copy(alpha = 0.18f), ChronosRoseNeon, onClearAll),
                CalcKeySpec("CE", "ce", ChronosAmberNeon.copy(alpha = 0.15f), ChronosAmberNeon, onClearEntry),
                CalcKeySpec("⌫", "backspace", ChronosAmberNeon.copy(alpha = 0.15f), ChronosAmberNeon, onBackspace),
                CalcKeySpec("÷", "div", ChronosCyanNeon.copy(alpha = 0.18f), ChronosCyanNeon) { onToken("÷") }
            ),
            listOf(
                CalcKeySpec("7", "7", ChronosSurfaceDark, Color.White) { onToken("7") },
                CalcKeySpec("8", "8", ChronosSurfaceDark, Color.White) { onToken("8") },
                CalcKeySpec("9", "9", ChronosSurfaceDark, Color.White) { onToken("9") },
                CalcKeySpec("×", "mul", ChronosCyanNeon.copy(alpha = 0.18f), ChronosCyanNeon) { onToken("×") }
            ),
            listOf(
                CalcKeySpec("4", "4", ChronosSurfaceDark, Color.White) { onToken("4") },
                CalcKeySpec("5", "5", ChronosSurfaceDark, Color.White) { onToken("5") },
                CalcKeySpec("6", "6", ChronosSurfaceDark, Color.White) { onToken("6") },
                CalcKeySpec("-", "sub", ChronosCyanNeon.copy(alpha = 0.18f), ChronosCyanNeon) { onToken("-") }
            ),
            listOf(
                CalcKeySpec("1", "1", ChronosSurfaceDark, Color.White) { onToken("1") },
                CalcKeySpec("2", "2", ChronosSurfaceDark, Color.White) { onToken("2") },
                CalcKeySpec("3", "3", ChronosSurfaceDark, Color.White) { onToken("3") },
                CalcKeySpec("+", "add", ChronosCyanNeon.copy(alpha = 0.18f), ChronosCyanNeon) { onToken("+") }
            ),
            listOf(
                CalcKeySpec("±", "negate", ChronosSurfaceDark, ChronosAmberNeon, onNegate),
                CalcKeySpec("0", "0", ChronosSurfaceDark, Color.White) { onToken("0") },
                CalcKeySpec(".", "dot", ChronosSurfaceDark, Color.White) { onToken(".") },
                CalcKeySpec("=", "equals", ChronosCyanNeon, Color.Black, onEquals)
            )
        )

        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = key.bgColor,
                        border = BorderStroke(
                            1.dp,
                            if (key.tag == "equals") ChronosCyanNeon else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { key.onClick() }
                            .testTag("calc_btn_${key.tag}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key.label,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = key.textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CalcKeySpec(
    val label: String,
    val tag: String,
    val bgColor: Color,
    val textColor: Color,
    val onClick: () -> Unit
)

// =============================================================================
// 3. KALKULATOR PERSENTASE
// =============================================================================
@Composable
private fun PercentageCalculatorSection(
    isId: Boolean,
    onSaveToHistory: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var inputAStr by rememberSaveable { mutableStateOf("15") }
    var inputBStr by rememberSaveable { mutableStateOf("250000") }

    val modes = listOf(
        "X% dari Y",
        "X berapa % dari Y",
        "Perubahan % (A → B)",
        "Tambah / Diskon X%"
    )

    val valA = SmartCalculatorEngine.parseSmartNumber(inputAStr) ?: 0.0
    val valB = SmartCalculatorEngine.parseSmartNumber(inputBStr) ?: 0.0
    val outcome = remember(modeIndex, valA, valB, isId) {
        SmartCalculatorEngine.calculatePercentage(modeIndex, valA, valB, isId)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("percentage_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "KALKULATOR PERSENTASE PINTAR",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ChronosAmberNeon
            )

            // Mode selector chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(modes) { idx, title ->
                    val active = modeIndex == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ChronosAmberNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (active) ChronosAmberNeon else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { modeIndex = idx }
                            .testTag("pct_mode_$idx")
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ChronosAmberNeon else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            val labelA = when (modeIndex) {
                0 -> "Persentase X (%)"
                1 -> "Nilai Bagian (X)"
                2 -> "Nilai Awal (A)"
                else -> "Nilai Awal / Harga (Rp)"
            }
            val labelB = when (modeIndex) {
                0 -> "Nilai Total (Y)"
                1 -> "Nilai Total Pembanding (Y)"
                2 -> "Nilai Akhir (B)"
                else -> "Persentase Tambah / Diskon (%)"
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalcNumberInputField(
                    label = labelA,
                    value = inputAStr,
                    onValueChange = { inputAStr = it },
                    accentColor = ChronosAmberNeon,
                    testTag = "pct_input_a",
                    modifier = Modifier.weight(1f)
                )
                CalcNumberInputField(
                    label = labelB,
                    value = inputBStr,
                    onValueChange = { inputBStr = it },
                    accentColor = ChronosAmberNeon,
                    testTag = "pct_input_b",
                    modifier = Modifier.weight(1f)
                )
            }

            // Result Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChronosSurfaceDark,
                border = BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = outcome.summaryLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1D5DB)
                    )
                    Text(
                        text = if (modeIndex == 1 || modeIndex == 2) {
                            "${SmartCalculatorEngine.formatNumber(outcome.mainResult, maxDecimals = 4)}%"
                        } else {
                            SmartCalculatorEngine.formatNumber(outcome.mainResult, maxDecimals = 4)
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = ChronosAmberNeon,
                        modifier = Modifier.testTag("pct_result_value")
                    )
                    Text(
                        text = "Rumus: ${outcome.formulaExplanation}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CalcActionChip(
                            label = "Salin",
                            color = ChronosCyanNeon,
                            onClick = { onCopy(outcome.summaryLabel) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CalcActionChip(
                            label = "Simpan ke Riwayat",
                            color = ChronosAmberNeon,
                            onClick = {
                                onSaveToHistory(
                                    outcome.formulaExplanation,
                                    SmartCalculatorEngine.formatNumber(outcome.mainResult, maxDecimals = 4)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. KALKULATOR KEUANGAN (Pinjaman, Bunga Majemuk, Diskon+Pajak, Margin Laba)
// =============================================================================
@Composable
private fun FinancialCalculatorSection(
    isId: Boolean,
    onSaveToHistory: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    var finTab by rememberSaveable { mutableIntStateOf(0) }
    val finModes = listOf(
        "Cicilan Pinjaman",
        "Bunga & Tabungan",
        "Diskon & Pajak PPN",
        "Margin & Markup Laba"
    )

    // State for Loan
    var loanPrincipal by rememberSaveable { mutableStateOf("100000000") }
    var loanRate by rememberSaveable { mutableStateOf("8.5") }
    var loanMonths by rememberSaveable { mutableStateOf("36") }

    // State for Compound Interest / Savings
    var initSavings by rememberSaveable { mutableStateOf("10000000") }
    var monthlySave by rememberSaveable { mutableStateOf("1000000") }
    var savingsRate by rememberSaveable { mutableStateOf("6.5") }
    var savingsYears by rememberSaveable { mutableStateOf("5") }

    // State for Discount & Tax
    var origPrice by rememberSaveable { mutableStateOf("500000") }
    var discPct by rememberSaveable { mutableStateOf("20") }
    var extraDiscPct by rememberSaveable { mutableStateOf("0") }
    var taxPct by rememberSaveable { mutableStateOf("11") }

    // State for Margin & Markup
    var costPrice by rememberSaveable { mutableStateOf("120000") }
    var sellPrice by rememberSaveable { mutableStateOf("180000") }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("financial_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "KALKULATOR KEUANGAN & BISNIS",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ChronosGreenNeon
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(finModes) { idx, title ->
                    val active = finTab == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ChronosGreenNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (active) ChronosGreenNeon else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { finTab = idx }
                            .testTag("fin_mode_$idx")
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ChronosGreenNeon else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            when (finTab) {
                0 -> {
                    CalcNumberInputField(
                        label = "Pokok Pinjaman (Rp)",
                        value = loanPrincipal,
                        onValueChange = { loanPrincipal = it },
                        accentColor = ChronosGreenNeon,
                        testTag = "fin_loan_principal"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Bunga Tahunan (%)",
                            value = loanRate,
                            onValueChange = { loanRate = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_loan_rate",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Tenor (Bulan)",
                            value = loanMonths,
                            onValueChange = { loanMonths = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_loan_months",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val outcome = SmartCalculatorEngine.calculateLoanInstallment(
                        principal = SmartCalculatorEngine.parseSmartNumber(loanPrincipal) ?: 0.0,
                        annualRatePercent = SmartCalculatorEngine.parseSmartNumber(loanRate) ?: 0.0,
                        tenureMonths = loanMonths.trim().toIntOrNull() ?: 0
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Estimasi Cicilan per Bulan", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = SmartCalculatorEngine.formatCurrencyIdr(outcome.monthlyPayment),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = ChronosGreenNeon,
                                modifier = Modifier.testTag("fin_loan_monthly_result")
                            )
                            Text(
                                text = "Total Bayar: ${SmartCalculatorEngine.formatCurrencyIdr(outcome.totalPayment)}",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Total Bunga: ${SmartCalculatorEngine.formatCurrencyIdr(outcome.totalInterest)}",
                                fontSize = 12.sp,
                                color = ChronosAmberNeon
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CalcActionChip("Salin", ChronosCyanNeon) {
                                    onCopy("Cicilan: ${SmartCalculatorEngine.formatCurrencyIdr(outcome.monthlyPayment)}/bln")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                CalcActionChip("Simpan Riwayat", ChronosGreenNeon) {
                                    onSaveToHistory(
                                        "Pinjaman ${SmartCalculatorEngine.formatCurrencyIdr(outcome.principalAmount)} (${loanMonths} bln @ ${loanRate}%)",
                                        "${SmartCalculatorEngine.formatCurrencyIdr(outcome.monthlyPayment)}/bln"
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Saldo Awal (Rp)",
                            value = initSavings,
                            onValueChange = { initSavings = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_save_initial",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Setoran / Bulan (Rp)",
                            value = monthlySave,
                            onValueChange = { monthlySave = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_save_monthly",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Imbal Hasil / Tahun (%)",
                            value = savingsRate,
                            onValueChange = { savingsRate = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_save_rate",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Durasi (Tahun)",
                            value = savingsYears,
                            onValueChange = { savingsYears = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_save_years",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val outcome = SmartCalculatorEngine.calculateCompoundInterest(
                        initialDeposit = SmartCalculatorEngine.parseSmartNumber(initSavings) ?: 0.0,
                        monthlyContribution = SmartCalculatorEngine.parseSmartNumber(monthlySave) ?: 0.0,
                        annualRatePercent = SmartCalculatorEngine.parseSmartNumber(savingsRate) ?: 0.0,
                        years = SmartCalculatorEngine.parseSmartNumber(savingsYears) ?: 0.0
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nilai Akhir Investasi / Tabungan", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = SmartCalculatorEngine.formatCurrencyIdr(outcome.finalBalance),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = ChronosGreenNeon
                            )
                            Text("Total Pokok Setoran: ${SmartCalculatorEngine.formatCurrencyIdr(outcome.totalContributions)}", fontSize = 12.sp, color = Color.White)
                            Text("Total Hasil Bunga: ${SmartCalculatorEngine.formatCurrencyIdr(outcome.totalInterestEarned)}", fontSize = 12.sp, color = ChronosAmberNeon)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CalcActionChip("Simpan Riwayat", ChronosGreenNeon) {
                                    onSaveToHistory(
                                        "Tabungan $savingsYears thn @ $savingsRate%",
                                        SmartCalculatorEngine.formatCurrencyIdr(outcome.finalBalance)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Harga Awal (Rp)",
                            value = origPrice,
                            onValueChange = { origPrice = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_disc_price",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Diskon Utama (%)",
                            value = discPct,
                            onValueChange = { discPct = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_disc_pct",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Diskon Tambahan (%)",
                            value = extraDiscPct,
                            onValueChange = { extraDiscPct = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_disc_extra",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Pajak PPN (%)",
                            value = taxPct,
                            onValueChange = { taxPct = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_disc_tax",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val outcome = SmartCalculatorEngine.calculateDiscountAndTax(
                        originalPrice = SmartCalculatorEngine.parseSmartNumber(origPrice) ?: 0.0,
                        discountPercent = SmartCalculatorEngine.parseSmartNumber(discPct) ?: 0.0,
                        extraDiscountPercent = SmartCalculatorEngine.parseSmartNumber(extraDiscPct) ?: 0.0,
                        taxPercent = SmartCalculatorEngine.parseSmartNumber(taxPct) ?: 0.0
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Harga Akhir Setelah Diskon & Pajak", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = SmartCalculatorEngine.formatCurrencyIdr(outcome.finalPayablePrice),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = ChronosGreenNeon,
                                modifier = Modifier.testTag("fin_disc_final_result")
                            )
                            Text("Hemat Diskon: ${SmartCalculatorEngine.formatCurrencyIdr(outcome.totalSavings)}", fontSize = 12.sp, color = ChronosAmberNeon)
                            Text("Pajak (+${taxPct}%): ${SmartCalculatorEngine.formatCurrencyIdr(outcome.taxAmount)}", fontSize = 12.sp, color = Color.White)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CalcActionChip("Simpan Riwayat", ChronosGreenNeon) {
                                    onSaveToHistory(
                                        "Diskon $discPct% + PPN $taxPct% dari $origPrice",
                                        SmartCalculatorEngine.formatCurrencyIdr(outcome.finalPayablePrice)
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Harga Modal / HPP (Rp)",
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_margin_cost",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Harga Jual (Rp)",
                            value = sellPrice,
                            onValueChange = { sellPrice = it },
                            accentColor = ChronosGreenNeon,
                            testTag = "fin_margin_sell",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val outcome = SmartCalculatorEngine.calculateProfitMargin(
                        costPrice = SmartCalculatorEngine.parseSmartNumber(costPrice) ?: 0.0,
                        sellingPrice = SmartCalculatorEngine.parseSmartNumber(sellPrice) ?: 0.0
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Laba Bersih per Unit", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = SmartCalculatorEngine.formatCurrencyIdr(outcome.netProfit),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (outcome.isProfit) ChronosGreenNeon else ChronosRoseNeon
                            )
                            Text(
                                text = "Gross Margin: ${SmartCalculatorEngine.formatNumber(outcome.grossMarginPercent, maxDecimals = 2)}% • Markup: ${SmartCalculatorEngine.formatNumber(outcome.markupPercent, maxDecimals = 2)}%",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = ChronosAmberNeon
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CalcActionChip("Simpan Riwayat", ChronosGreenNeon) {
                                    onSaveToHistory(
                                        "Margin Modal $costPrice → Jual $sellPrice",
                                        "${SmartCalculatorEngine.formatCurrencyIdr(outcome.netProfit)} (${SmartCalculatorEngine.formatNumber(outcome.grossMarginPercent, maxDecimals = 1)}%)"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. KALKULATOR KONVERSI UNIT (Panjang, Berat, Suhu, Waktu, Data, Luas, Volume, Kecepatan)
// =============================================================================
@Composable
private fun UnitConversionCalculatorSection(
    isId: Boolean,
    onSaveToHistory: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    var selectedCatIndex by rememberSaveable { mutableIntStateOf(0) }
    val allCategories = ConversionCategory.entries
    val currentCategory = allCategories[selectedCatIndex.coerceIn(0, allCategories.lastIndex)]
    val units = remember(currentCategory) {
        SmartCalculatorEngine.getUnitsForCategory(currentCategory)
    }

    var fromUnitIdx by rememberSaveable(selectedCatIndex) { mutableIntStateOf(0) }
    var toUnitIdx by rememberSaveable(selectedCatIndex) { mutableIntStateOf((1).coerceAtMost(units.lastIndex)) }
    var inputValueStr by rememberSaveable { mutableStateOf("1000") }

    val fromUnit = units[fromUnitIdx.coerceIn(0, units.lastIndex)]
    val toUnit = units[toUnitIdx.coerceIn(0, units.lastIndex)]
    val inputDouble = SmartCalculatorEngine.parseSmartNumber(inputValueStr) ?: 0.0
    val convertedValue = remember(currentCategory, inputDouble, fromUnit.code, toUnit.code) {
        SmartCalculatorEngine.convertUnit(currentCategory, inputDouble, fromUnit.code, toUnit.code)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosBlueNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversion_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "KONVERSI SATUAN MULTI-DIMENSI",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ChronosBlueNeon
            )

            // Category chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(allCategories) { idx, cat ->
                    val active = selectedCatIndex == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ChronosBlueNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (active) ChronosBlueNeon else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedCatIndex = idx }
                            .testTag("conv_cat_${cat.id}")
                    ) {
                        Text(
                            text = if (isId) cat.labelId else cat.labelEn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ChronosCyanNeon else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            CalcNumberInputField(
                label = "Nilai Input (${fromUnit.code})",
                value = inputValueStr,
                onValueChange = { inputValueStr = it },
                accentColor = ChronosBlueNeon,
                testTag = "conv_input_value"
            )

            // From Unit Selector
            Text("Dari Satuan:", fontSize = 11.sp, color = Color(0xFF9CA3AF))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(units) { idx, u ->
                    val sel = fromUnitIdx == idx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) ChronosCyanNeon.copy(alpha = 0.2f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (sel) ChronosCyanNeon else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { fromUnitIdx = idx }
                            .testTag("conv_from_${u.code}")
                    ) {
                        Text(
                            text = if (isId) u.nameId else u.nameEn,
                            fontSize = 11.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) ChronosCyanNeon else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Swap Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ke Satuan:", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ChronosAmberNeon.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            val tmp = fromUnitIdx
                            fromUnitIdx = toUnitIdx
                            toUnitIdx = tmp
                        }
                        .testTag("conv_swap_units_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Tukar Satuan", tint = ChronosAmberNeon, modifier = Modifier.size(14.dp))
                        Text("Tukar Satuan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChronosAmberNeon)
                    }
                }
            }

            // To Unit Selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(units) { idx, u ->
                    val sel = toUnitIdx == idx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) ChronosBlueNeon.copy(alpha = 0.25f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (sel) ChronosBlueNeon else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { toUnitIdx = idx }
                            .testTag("conv_to_${u.code}")
                    ) {
                        Text(
                            text = if (isId) u.nameId else u.nameEn,
                            fontSize = 11.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) Color.White else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            val formattedConverted = SmartCalculatorEngine.formatNumber(convertedValue, maxDecimals = 6)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChronosSurfaceDark,
                border = BorderStroke(1.dp, ChronosBlueNeon.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${SmartCalculatorEngine.formatNumber(inputDouble)} ${fromUnit.code} =",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "$formattedConverted ${toUnit.code}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = ChronosCyanNeon,
                        modifier = Modifier.testTag("conv_result_text")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        CalcActionChip("Salin", ChronosCyanNeon) {
                            onCopy("$formattedConverted ${toUnit.code}")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        CalcActionChip("Simpan Riwayat", ChronosBlueNeon) {
                            onSaveToHistory(
                                "${SmartCalculatorEngine.formatNumber(inputDouble)} ${fromUnit.code} → ${toUnit.code}",
                                "$formattedConverted ${toUnit.code}"
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 6. KALKULATOR TANGGAL DAN WAKTU
// =============================================================================
@Composable
private fun DateAndTimeCalculatorSection(
    isId: Boolean,
    onSaveToHistory: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    var dateTab by rememberSaveable { mutableIntStateOf(0) }
    val dateModes = listOf(
        "Selisih Dua Tanggal",
        "Tambah / Kurang Hari",
        "Durasi Waktu (Jam:Menit)"
    )

    var startDateIso by rememberSaveable { mutableStateOf("2026-01-01") }
    var endDateIso by rememberSaveable { mutableStateOf("2026-10-19") }

    var baseDateIso by rememberSaveable { mutableStateOf("2026-10-19") }
    var deltaDaysStr by rememberSaveable { mutableStateOf("30") }
    var deltaWeeksStr by rememberSaveable { mutableStateOf("0") }
    var deltaMonthsStr by rememberSaveable { mutableStateOf("0") }

    var startTimeHm by rememberSaveable { mutableStateOf("08:30") }
    var endTimeHm by rememberSaveable { mutableStateOf("17:15") }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosOrangeNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("datetime_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "KALKULATOR TANGGAL, USIA & DURASI WAKTU",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ChronosOrangeNeon
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(dateModes) { idx, title ->
                    val active = dateTab == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ChronosOrangeNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (active) ChronosOrangeNeon else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { dateTab = idx }
                            .testTag("dt_mode_$idx")
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ChronosOrangeNeon else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            when (dateTab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Tanggal Mulai (YYYY-MM-DD)",
                            value = startDateIso,
                            onValueChange = { startDateIso = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_start_date",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Tanggal Akhir (YYYY-MM-DD)",
                            value = endDateIso,
                            onValueChange = { endDateIso = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_end_date",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val outcome = SmartCalculatorEngine.calculateDateDifference(startDateIso, endDateIso)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosOrangeNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (outcome.isValid) {
                                Text("Total Selisih Hari", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                                Text(
                                    text = "${outcome.totalDays} Hari",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ChronosOrangeNeon,
                                    modifier = Modifier.testTag("dt_diff_result")
                                )
                                Text(
                                    text = "${outcome.years} tahun, ${outcome.months} bulan, ${outcome.days} hari",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Setara: ${outcome.totalWeeks} minggu ${outcome.remainingWeekDays} hari • ${outcome.totalHours} jam",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    CalcActionChip("Simpan Riwayat", ChronosOrangeNeon) {
                                        onSaveToHistory("$startDateIso s/d $endDateIso", "${outcome.totalDays} Hari")
                                    }
                                }
                            } else {
                                Text(
                                    text = outcome.errorMessage ?: "Format tanggal salah",
                                    fontSize = 12.sp,
                                    color = ChronosRoseNeon
                                )
                            }
                        }
                    }
                }

                1 -> {
                    CalcNumberInputField(
                        label = "Tanggal Acuan (YYYY-MM-DD)",
                        value = baseDateIso,
                        onValueChange = { baseDateIso = it },
                        accentColor = ChronosOrangeNeon,
                        testTag = "dt_base_date",
                        keyboardType = KeyboardType.Text
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "± Hari",
                            value = deltaDaysStr,
                            onValueChange = { deltaDaysStr = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_delta_days",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "± Minggu",
                            value = deltaWeeksStr,
                            onValueChange = { deltaWeeksStr = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_delta_weeks",
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "± Bulan",
                            value = deltaMonthsStr,
                            onValueChange = { deltaMonthsStr = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_delta_months",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val targetDateStr = SmartCalculatorEngine.addOrSubtractDaysFromDate(
                        baseIso = baseDateIso,
                        daysDelta = deltaDaysStr.toLongOrNull() ?: 0L,
                        weeksDelta = deltaWeeksStr.toLongOrNull() ?: 0L,
                        monthsDelta = deltaMonthsStr.toLongOrNull() ?: 0L
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosOrangeNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tanggal Hasil Perhitungan", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = targetDateStr,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = ChronosOrangeNeon,
                                modifier = Modifier.testTag("dt_add_result")
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CalcActionChip("Simpan Riwayat", ChronosOrangeNeon) {
                                    onSaveToHistory("$baseDateIso + (${deltaDaysStr}h ${deltaWeeksStr}mgg ${deltaMonthsStr}bln)", targetDateStr)
                                }
                            }
                        }
                    }
                }

                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcNumberInputField(
                            label = "Waktu Mulai (HH:MM)",
                            value = startTimeHm,
                            onValueChange = { startTimeHm = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_start_time",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                        CalcNumberInputField(
                            label = "Waktu Selesai (HH:MM)",
                            value = endTimeHm,
                            onValueChange = { endTimeHm = it },
                            accentColor = ChronosOrangeNeon,
                            testTag = "dt_end_time",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val durationRes = SmartCalculatorEngine.calculateTimeDuration(startTimeHm, endTimeHm)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, ChronosOrangeNeon.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Durasi Waktu", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = durationRes,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosOrangeNeon,
                                modifier = Modifier.testTag("dt_duration_result")
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CalcActionChip("Simpan Riwayat", ChronosOrangeNeon) {
                                    onSaveToHistory("Durasi $startTimeHm → $endTimeHm", durationRes)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 7. KALKULATOR STATISTIK
// =============================================================================
@Composable
private fun StatisticsCalculatorSection(
    isId: Boolean,
    onSaveToHistory: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    var datasetInput by rememberSaveable { mutableStateOf("10, 20, 20, 35, 40, 55") }
    val stats = remember(datasetInput) {
        SmartCalculatorEngine.calculateStatistics(datasetInput)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "KALKULATOR STATISTIK & ANALISIS DATA",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ChronosCyanNeon
            )

            CalcNumberInputField(
                label = "Daftar Angka (pisahkan dengan koma atau spasi)",
                value = datasetInput,
                onValueChange = { datasetInput = it },
                accentColor = ChronosCyanNeon,
                testTag = "stats_dataset_input",
                keyboardType = KeyboardType.Text
            )

            if (!stats.isValid) {
                Text(
                    text = stats.errorMessage ?: "Data tidak valid",
                    fontSize = 12.sp,
                    color = ChronosRoseNeon
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ChronosSurfaceDark,
                    border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val modeStr = if (stats.modes.isEmpty()) {
                            "Tidak ada modus unik"
                        } else {
                            stats.modes.joinToString(", ") { SmartCalculatorEngine.formatNumber(it) }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatMetricBox("Jumlah Data (N)", "${stats.count}", ChronosCyanNeon, Modifier.weight(1f))
                            StatMetricBox("Total (Σx)", SmartCalculatorEngine.formatNumber(stats.sum), ChronosAmberNeon, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatMetricBox("Rata-rata (Mean)", SmartCalculatorEngine.formatNumber(stats.mean, maxDecimals = 4), ChronosGreenNeon, Modifier.weight(1f), "stats_mean_value")
                            StatMetricBox("Nilai Tengah (Median)", SmartCalculatorEngine.formatNumber(stats.median, maxDecimals = 4), ChronosCyanNeon, Modifier.weight(1f), "stats_median_value")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatMetricBox("Minimum", SmartCalculatorEngine.formatNumber(stats.min), Color.White, Modifier.weight(1f))
                            StatMetricBox("Maksimum", SmartCalculatorEngine.formatNumber(stats.max), Color.White, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatMetricBox("Rentang (Range)", SmartCalculatorEngine.formatNumber(stats.range), ChronosOrangeNeon, Modifier.weight(1f))
                            StatMetricBox("Modus (Mode)", modeStr, ChronosPurpleNeon, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatMetricBox("Simpangan Baku (σ Pop)", SmartCalculatorEngine.formatNumber(stats.stdDevPop, maxDecimals = 4), ChronosCyanNeon, Modifier.weight(1f))
                            StatMetricBox("Simpangan Baku (s Sampel)", SmartCalculatorEngine.formatNumber(stats.stdDevSample, maxDecimals = 4), ChronosAmberNeon, Modifier.weight(1f))
                        }
                        Text(
                            text = "Urutan Data: ${stats.sortedValues.joinToString(", ") { SmartCalculatorEngine.formatNumber(it) }}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            CalcActionChip("Salin Ringkasan", ChronosCyanNeon) {
                                onCopy("N=${stats.count}, Mean=${SmartCalculatorEngine.formatNumber(stats.mean)}, Median=${SmartCalculatorEngine.formatNumber(stats.median)}")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            CalcActionChip("Simpan Riwayat", ChronosGreenNeon) {
                                onSaveToHistory(
                                    "Statistik [${datasetInput.take(28)}]",
                                    "Mean=${SmartCalculatorEngine.formatNumber(stats.mean, maxDecimals = 2)}, Med=${SmartCalculatorEngine.formatNumber(stats.median, maxDecimals = 2)}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Column(modifier = modifier.padding(4.dp)) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF9CA3AF))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

// =============================================================================
// 8. KALKULATOR PROGRAMMER (DEC, BIN, OCT, HEX & Bitwise)
// =============================================================================
@Composable
private fun ProgrammerCalculatorSection(
    isId: Boolean,
    onSaveToHistory: (String, String) -> Unit,
    onCopy: (String) -> Unit
) {
    var inputRadix by rememberSaveable { mutableIntStateOf(10) }
    var bitWidth by rememberSaveable { mutableIntStateOf(32) }
    var selectedOp by rememberSaveable { mutableStateOf("AND") }
    var inputAStr by rememberSaveable { mutableStateOf("255") }
    var inputBStr by rememberSaveable { mutableStateOf("15") }
    var activeHexTargetB by rememberSaveable { mutableStateOf(false) }

    val bases = listOf(
        10 to "DEC (10)",
        16 to "HEX (16)",
        2 to "BIN (2)",
        8 to "OCT (8)"
    )
    val ops = listOf("AND", "OR", "XOR", "NOT", "SHL", "SHR", "ADD", "SUB")
    val outcome = remember(inputAStr, inputBStr, inputRadix, selectedOp, bitWidth) {
        SmartCalculatorEngine.evaluateProgrammerOperation(
            inputA = inputAStr,
            inputB = inputBStr,
            inputRadix = inputRadix,
            operation = selectedOp,
            bitWidth = bitWidth
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosRoseNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("programmer_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KALKULATOR PROGRAMMER & BITWISE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = ChronosRoseNeon
                )

                // Bit width selector (8, 16, 32, 64-bit)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(8, 16, 32, 64).forEach { bw ->
                        val active = bitWidth == bw
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) ChronosRoseNeon.copy(alpha = 0.25f) else ChronosSurfaceDark,
                            border = BorderStroke(1.dp, if (active) ChronosRoseNeon else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { bitWidth = bw }
                                .testTag("prog_bw_$bw")
                        ) {
                            Text(
                                text = "${bw}b",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) ChronosRoseNeon else Color(0xFF9CA3AF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Input Base Selector (Automatically converts inputA and inputB to the newly selected radix!)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                bases.forEach { (radix, label) ->
                    val active = inputRadix == radix
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) ChronosRoseNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (active) ChronosRoseNeon else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (inputRadix != radix) {
                                    inputAStr = SmartCalculatorEngine.convertProgrammerBaseInput(inputAStr, inputRadix, radix, bitWidth)
                                    inputBStr = SmartCalculatorEngine.convertProgrammerBaseInput(inputBStr, inputRadix, radix, bitWidth)
                                    inputRadix = radix
                                }
                            }
                            .testTag("prog_base_$radix")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) ChronosRoseNeon else Color(0xFFD1D5DB)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalcNumberInputField(
                    label = "Operand A (Basis $inputRadix)",
                    value = inputAStr,
                    onValueChange = {
                        activeHexTargetB = false
                        inputAStr = it
                    },
                    accentColor = ChronosRoseNeon,
                    testTag = "prog_input_a",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f)
                )
                if (selectedOp != "NOT") {
                    CalcNumberInputField(
                        label = "Operand B (Basis $inputRadix)",
                        value = inputBStr,
                        onValueChange = {
                            activeHexTargetB = true
                            inputBStr = it
                        },
                        accentColor = ChronosRoseNeon,
                        testTag = "prog_input_b",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Hexadecimal Quick Digit Pad (A-F) when in HEX mode
            if (inputRadix == 16) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Target Keypad HEX:" else "HEX Keypad Target:",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(false to "Operand A", true to "Operand B").forEach { (isB, lbl) ->
                            val sel = activeHexTargetB == isB
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (sel) ChronosRoseNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                                border = BorderStroke(1.dp, if (sel) ChronosRoseNeon else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { activeHexTargetB = isB }
                            ) {
                                Text(
                                    text = lbl,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) ChronosRoseNeon else Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("A", "B", "C", "D", "E", "F").forEach { hexChar ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ChronosSurfaceDark,
                            border = BorderStroke(1.dp, ChronosRoseNeon.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (activeHexTargetB && selectedOp != "NOT") {
                                        inputBStr = if (inputBStr == "0") hexChar else inputBStr + hexChar
                                    } else {
                                        inputAStr = if (inputAStr == "0") hexChar else inputAStr + hexChar
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(hexChar, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = ChronosRoseNeon)
                            }
                        }
                    }
                }
            }

            // Interactive 8-bit Quick Bit Toggle Bar for Operand A (LSB bits 7..0)
            val parsedAVal = SmartCalculatorEngine.parseProgrammerValue(inputAStr, inputRadix) ?: 0L
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isId) "Sakelar Bit Interaktif Operand A (Bit 7 → 0):" else "Interactive Bit Toggle Operand A (Bits 7 → 0):",
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    for (bitIdx in 7 downTo 0) {
                        val bitOn = ((parsedAVal ushr bitIdx) and 1L) == 1L
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (bitOn) ChronosRoseNeon.copy(alpha = 0.25f) else ChronosSurfaceDark,
                            border = BorderStroke(1.dp, if (bitOn) ChronosRoseNeon else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    inputAStr = SmartCalculatorEngine.toggleBitInBase(inputAStr, inputRadix, bitIdx, bitWidth)
                                }
                                .testTag("prog_bit_$bitIdx")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (bitOn) "1" else "0",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (bitOn) ChronosRoseNeon else Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = "b$bitIdx",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 8.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            // Bitwise Operation Selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(ops) { _, op ->
                    val active = selectedOp == op
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) ChronosCyanNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                        border = BorderStroke(1.dp, if (active) ChronosCyanNeon else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedOp = op }
                            .testTag("prog_op_$op")
                    ) {
                        Text(
                            text = op,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ChronosCyanNeon else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Live Multi-Base Output Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChronosSurfaceDark,
                border = BorderStroke(1.dp, ChronosRoseNeon.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!outcome.isValid) {
                        Text(
                            text = outcome.errorMessage ?: "Format input tidak sesuai basis bilangan",
                            fontSize = 12.sp,
                            color = ChronosRoseNeon
                        )
                    } else {
                        ProgrammerBaseRow("HEX", "0x${outcome.hexString}", ChronosAmberNeon, "prog_res_hex") {
                            onCopy("0x${outcome.hexString}")
                        }
                        ProgrammerBaseRow("DEC", outcome.decString, ChronosCyanNeon, "prog_res_dec") {
                            onCopy(outcome.decString)
                        }
                        ProgrammerBaseRow("OCT", "0o${outcome.octString}", ChronosGreenNeon, "prog_res_oct") {
                            onCopy(outcome.octString)
                        }
                        ProgrammerBaseRow("BIN", outcome.binString, ChronosPurpleNeon, "prog_res_bin") {
                            onCopy(outcome.binString.replace(" ", ""))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            CalcActionChip("Simpan Riwayat", ChronosRoseNeon) {
                                onSaveToHistory(
                                    "$inputAStr $selectedOp $inputBStr (Base $inputRadix)",
                                    "DEC ${outcome.decString} • HEX 0x${outcome.hexString}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgrammerBaseRow(
    baseLabel: String,
    valueText: String,
    accent: Color,
    testTag: String,
    onCopyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable { onCopyClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = baseLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = accent,
            modifier = Modifier.width(42.dp)
        )
        Text(
            text = valueText,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
    }
}

// =============================================================================
// 9. RIWAYAT PERHITUNGAN (Calculation History)
// =============================================================================
@Composable
private fun HistoryCalculatorSection(
    isId: Boolean,
    records: List<CalculatorHistoryRecord>,
    onReuseExpression: (CalculatorHistoryRecord) -> Unit,
    onCopyRecord: (CalculatorHistoryRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onClearAllHistory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RIWAYAT PERHITUNGAN",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = ChronosAmberNeon
                    )
                    Text(
                        text = "${records.size} catatan tersimpan di perangkat",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                if (records.isNotEmpty()) {
                    CalcActionChip(
                        label = "Hapus Semua",
                        color = ChronosRoseNeon,
                        onClick = onClearAllHistory
                    )
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Belum ada riwayat perhitungan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9CA3AF)
                        )
                        Text(
                            text = "Setiap perhitungan yang Anda evaluasi atau simpan akan muncul di sini.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                records.forEach { rec ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ChronosSurfaceDark,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ChronosCyanNeon.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = rec.categoryName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = rec.timestampStr,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }

                            Text(
                                text = rec.expression,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                color = Color(0xFFD1D5DB)
                            )
                            Text(
                                text = "= ${rec.result}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = ChronosAmberNeon
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CalcActionChip("Gunakan Hasil", ChronosGreenNeon) {
                                    onReuseExpression(rec)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                CalcActionChip("Salin", ChronosCyanNeon) {
                                    onCopyRecord(rec)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteRecord(rec.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = ChronosRoseNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 10. PENGATURAN KALKULATOR (Calculator Settings & Precision Control)
// =============================================================================
@Composable
private fun CalculatorSettingsSection(
    isId: Boolean,
    decimalPrecision: Int,
    onChangePrecision: (Int) -> Unit,
    isDegreeMode: Boolean,
    onChangeDegreeMode: (Boolean) -> Unit,
    useIndonesianFormat: Boolean,
    onChangeIndonesianFormat: (Boolean) -> Unit,
    autoSaveHistory: Boolean,
    onChangeAutoSaveHistory: (Boolean) -> Unit,
    hapticEnabled: Boolean,
    onChangeHaptic: (Boolean) -> Unit,
    onResetWorkspace: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChronosCardNavy,
        border = BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calculator_settings_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isId) "PENGATURAN KALKULATOR SMART" else "SMART CALCULATOR SETTINGS",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ChronosCyanNeon
            )

            // 1. Decimal Precision Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isId) "Presisi Angka Desimal (Pembulatan Hasil)" else "Decimal Precision (Result Rounding)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 4, 6, 8, 10).forEach { prec ->
                        val active = decimalPrecision == prec
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ChronosCyanNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                            border = BorderStroke(1.dp, if (active) ChronosCyanNeon else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onChangePrecision(prec) }
                                .testTag("calc_setting_prec_$prec")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                                Text(
                                    text = "$prec Des",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) ChronosCyanNeon else Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Default Angle Unit (DEG vs RAD)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isId) "Satuan Sudut Trigonometri" else "Trigonometric Angle Unit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        true to (if (isId) "Derajat (DEG °)" else "Degrees (DEG °)"),
                        false to (if (isId) "Radian (RAD π)" else "Radians (RAD π)")
                    ).forEach { (deg, label) ->
                        val active = isDegreeMode == deg
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ChronosAmberNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                            border = BorderStroke(1.dp, if (active) ChronosAmberNeon else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onChangeDegreeMode(deg) }
                                .testTag("calc_setting_angle_${if (deg) "deg" else "rad"}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) ChronosAmberNeon else Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Number Formatting Style
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isId) "Format Pemisah Ribuan & Desimal" else "Thousand & Decimal Separator Format",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        false to "1,250,000.50 (Standar)",
                        true to "1.250.000,50 (Indonesia)"
                    ).forEach { (idFmt, label) ->
                        val active = useIndonesianFormat == idFmt
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ChronosGreenNeon.copy(alpha = 0.22f) else ChronosSurfaceDark,
                            border = BorderStroke(1.dp, if (active) ChronosGreenNeon else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onChangeIndonesianFormat(idFmt) }
                                .testTag("calc_setting_fmt_${if (idFmt) "id" else "us"}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) ChronosGreenNeon else Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Auto-Save History & Haptic Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (autoSaveHistory) ChronosCyanNeon.copy(alpha = 0.16f) else ChronosSurfaceDark,
                    border = BorderStroke(1.dp, if (autoSaveHistory) ChronosCyanNeon else Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onChangeAutoSaveHistory(!autoSaveHistory) }
                        .testTag("calc_setting_autosave_toggle")
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isId) "Simpan Riwayat Otomatis" else "Auto-Save History",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (autoSaveHistory) ChronosCyanNeon else Color.White
                        )
                        Text(
                            text = if (autoSaveHistory) "AKTIF (Saat tekan =)" else "NONAKTIF",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hapticEnabled) ChronosPurpleNeon.copy(alpha = 0.16f) else ChronosSurfaceDark,
                    border = BorderStroke(1.dp, if (hapticEnabled) ChronosPurpleNeon else Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onChangeHaptic(!hapticEnabled) }
                        .testTag("calc_setting_haptic_toggle")
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isId) "Getar Tombol (Haptic)" else "Keypress Haptics",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hapticEnabled) ChronosPurpleNeon else Color.White
                        )
                        Text(
                            text = if (hapticEnabled) "AKTIF" else "NONAKTIF",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            // 5. Reset Calculator Workspace & Memory
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CalcActionChip(
                    label = if (isId) "Reset Workspace & Memori Kalkulator" else "Reset Calculator Workspace & Memory",
                    color = ChronosRoseNeon,
                    onClick = onResetWorkspace
                )
            }
        }
    }
}

// =============================================================================
// REUSABLE INPUT & CHIP COMPONENTS
// =============================================================================
@Composable
private fun CalcNumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9CA3AF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                focusedContainerColor = ChronosSurfaceDark,
                unfocusedContainerColor = ChronosSurfaceDark,
                cursorColor = accentColor
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
private fun CalcActionChip(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
