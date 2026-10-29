package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CalculationRecord
import com.example.domain.SimpleDate
import com.example.ui.CalculatorMode
import com.example.ui.SeleniaUiState
import com.example.ui.components.RealisticMoonCanvas
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CalculatorScreen(
    uiState: SeleniaUiState,
    onSetMode: (CalculatorMode) -> Unit,
    onInputToken: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: (String?) -> Unit,
    onConvertDaysToSynodic: () -> Unit,
    onMultiplyBySynodic: () -> Unit,
    onUseHistoryItem: (CalculationRecord) -> Unit,
    onClearHistory: () -> Unit,
    onSetUseLunarCycleOffset: (Boolean) -> Unit,
    onUpdateOffsetDays: (Int) -> Unit,
    onUpdateOffsetLunarCycles: (Double) -> Unit,
    onStepComparisonDate: (Int) -> Unit,
    onJumpToDateInCalendar: (SimpleDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode Switcher: Científica vs Fechas y Ciclos Lunares
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = uiState.calculatorMode == CalculatorMode.SCIENTIFIC,
                    onClick = { onSetMode(CalculatorMode.SCIENTIFIC) },
                    label = { Text(stringResource(R.string.calc_mode_standard)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_mode_scientific_tab")
                )
                FilterChip(
                    selected = uiState.calculatorMode == CalculatorMode.DATE_AND_LUNAR,
                    onClick = { onSetMode(CalculatorMode.DATE_AND_LUNAR) },
                    label = { Text(stringResource(R.string.calc_mode_dates)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_mode_dates_tab")
                )
            }

            if (uiState.calculatorMode == CalculatorMode.SCIENTIFIC) {
                ScientificCalculatorContent(
                    uiState = uiState,
                    onInputToken = onInputToken,
                    onClear = onClear,
                    onBackspace = onBackspace,
                    onEvaluate = onEvaluate,
                    onConvertDaysToSynodic = onConvertDaysToSynodic,
                    onMultiplyBySynodic = onMultiplyBySynodic,
                    onUseHistoryItem = onUseHistoryItem,
                    onClearHistory = onClearHistory
                )
            } else {
                DateAndLunarCalculatorContent(
                    uiState = uiState,
                    onSetUseLunarCycleOffset = onSetUseLunarCycleOffset,
                    onUpdateOffsetDays = onUpdateOffsetDays,
                    onUpdateOffsetLunarCycles = onUpdateOffsetLunarCycles,
                    onStepComparisonDate = onStepComparisonDate,
                    onJumpToDateInCalendar = onJumpToDateInCalendar
                )
            }
        }
    }
}

@Composable
private fun ScientificCalculatorContent(
    uiState: SeleniaUiState,
    onInputToken: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: (String?) -> Unit,
    onConvertDaysToSynodic: () -> Unit,
    onMultiplyBySynodic: () -> Unit,
    onUseHistoryItem: (CalculationRecord) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. Expression & Result Display Screen
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.large
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = uiState.expression.ifEmpty { "0" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_expression_display")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "= ${uiState.liveResultPreview}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_result_display")
                    )
                }
            }
        }

        // 2. Lunar Constant & Conversion Quick Action Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onMultiplyBySynodic,
                    label = { Text("× Luna (29.53d)") },
                    modifier = Modifier.testTag("calc_multiply_synodic_chip")
                )
                AssistChip(
                    onClick = onConvertDaysToSynodic,
                    label = { Text("Días → Lunas") },
                    modifier = Modifier.testTag("calc_days_to_lunas_chip")
                )
                AssistChip(
                    onClick = { onInputToken("29.5306") },
                    label = { Text("🌙 29.53") },
                    modifier = Modifier.testTag("calc_insert_synodic_constant")
                )
            }
        }

        // 3. Scientific & Standard Keypad
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Scientific row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("sin(", Modifier.weight(1f), isScientific = true, tag = "key_sin") {
                        onInputToken("sin(")
                    }
                    CalcKeyButton("cos(", Modifier.weight(1f), isScientific = true, tag = "key_cos") {
                        onInputToken("cos(")
                    }
                    CalcKeyButton("tan(", Modifier.weight(1f), isScientific = true, tag = "key_tan") {
                        onInputToken("tan(")
                    }
                    CalcKeyButton("√(", Modifier.weight(1f), isScientific = true, tag = "key_sqrt") {
                        onInputToken("√(")
                    }
                    CalcKeyButton("^", Modifier.weight(1f), isScientific = true, tag = "key_pow") {
                        onInputToken("^")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("(", Modifier.weight(1f), isScientific = true, tag = "key_open_paren") {
                        onInputToken("(")
                    }
                    CalcKeyButton(")", Modifier.weight(1f), isScientific = true, tag = "key_close_paren") {
                        onInputToken(")")
                    }
                    CalcKeyButton("π", Modifier.weight(1f), isScientific = true, tag = "key_pi") {
                        onInputToken("π")
                    }
                    CalcKeyButton("ln(", Modifier.weight(1f), isScientific = true, tag = "key_ln") {
                        onInputToken("ln(")
                    }
                    CalcKeyButton("%", Modifier.weight(1f), isScientific = true, tag = "key_percent") {
                        onInputToken("%")
                    }
                }

                // Main numeric & operator rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("AC", Modifier.weight(1f), isDanger = true, tag = "key_clear") {
                        onClear()
                    }
                    Surface(
                        onClick = onBackspace,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("key_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Borrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    CalcKeyButton("÷", Modifier.weight(1f), isOperator = true, tag = "key_divide") {
                        onInputToken("÷")
                    }
                    CalcKeyButton("×", Modifier.weight(1f), isOperator = true, tag = "key_multiply") {
                        onInputToken("×")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("7", Modifier.weight(1f), tag = "key_7") { onInputToken("7") }
                    CalcKeyButton("8", Modifier.weight(1f), tag = "key_8") { onInputToken("8") }
                    CalcKeyButton("9", Modifier.weight(1f), tag = "key_9") { onInputToken("9") }
                    CalcKeyButton("−", Modifier.weight(1f), isOperator = true, tag = "key_minus") {
                        onInputToken("-")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("4", Modifier.weight(1f), tag = "key_4") { onInputToken("4") }
                    CalcKeyButton("5", Modifier.weight(1f), tag = "key_5") { onInputToken("5") }
                    CalcKeyButton("6", Modifier.weight(1f), tag = "key_6") { onInputToken("6") }
                    CalcKeyButton("+", Modifier.weight(1f), isOperator = true, tag = "key_plus") {
                        onInputToken("+")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("1", Modifier.weight(1f), tag = "key_1") { onInputToken("1") }
                    CalcKeyButton("2", Modifier.weight(1f), tag = "key_2") { onInputToken("2") }
                    CalcKeyButton("3", Modifier.weight(1f), tag = "key_3") { onInputToken("3") }
                    CalcKeyButton("=", Modifier.weight(1f), isPrimaryEqual = true, tag = "key_equals") {
                        onEvaluate(null)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("0", Modifier.weight(2f), tag = "key_0") { onInputToken("0") }
                    CalcKeyButton(".", Modifier.weight(1f), tag = "key_decimal") { onInputToken(".") }
                    CalcKeyButton("00", Modifier.weight(1f), tag = "key_double_zero") { onInputToken("00") }
                }
            }
        }

        // 4. Calculation History Tape
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.calc_history_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (uiState.calculationHistory.isNotEmpty()) {
                            TextButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_calc_history_button")
                            ) {
                                Text(stringResource(R.string.calc_clear_history))
                            }
                        }
                    }

                    if (uiState.calculationHistory.isEmpty()) {
                        Text(
                            text = stringResource(R.string.calc_history_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.calculationHistory.take(6).forEach { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUseHistoryItem(item) }
                                    .testTag("history_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        item.badgeLabel?.let { badge ->
                                            Text(
                                                text = badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = item.expression,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "= ${item.result}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
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

@Composable
private fun CalcKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isScientific: Boolean = false,
    isDanger: Boolean = false,
    isPrimaryEqual: Boolean = false,
    tag: String,
    onClick: () -> Unit
) {
    val containerColor = when {
        isPrimaryEqual -> MaterialTheme.colorScheme.primary
        isDanger -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        isScientific -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isPrimaryEqual -> MaterialTheme.colorScheme.onPrimary
        isDanger -> MaterialTheme.colorScheme.error
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        isScientific -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val height = if (isScientific) 44.dp else 54.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = modifier
            .height(height)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = if (isScientific) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DateAndLunarCalculatorContent(
    uiState: SeleniaUiState,
    onSetUseLunarCycleOffset: (Boolean) -> Unit,
    onUpdateOffsetDays: (Int) -> Unit,
    onUpdateOffsetLunarCycles: (Double) -> Unit,
    onStepComparisonDate: (Int) -> Unit,
    onJumpToDateInCalendar: (SimpleDate) -> Unit
) {
    val targetDate = uiState.computedTargetDate
    val targetLunar = uiState.computedTargetLunarInfo

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("date_lunar_calculator_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Add/Subtract Days or Synodic Cycles from Selected Calendar Date
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.date_calc_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Fecha base: ${uiState.selectedDate.formatSpanishLong().replaceFirstChar { it.uppercase() }} (${uiState.selectedDayLunarInfo.phaseType.emoji} ${uiState.selectedDayLunarInfo.phaseType.spanishName})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !uiState.useLunarCycleOffset,
                            onClick = { onSetUseLunarCycleOffset(false) },
                            label = { Text("Por Días") },
                            modifier = Modifier.testTag("offset_by_days_chip")
                        )
                        FilterChip(
                            selected = uiState.useLunarCycleOffset,
                            onClick = { onSetUseLunarCycleOffset(true) },
                            label = { Text("Por Ciclos Lunares (29.53d)") },
                            modifier = Modifier.testTag("offset_by_cycles_chip")
                        )
                    }

                    if (!uiState.useLunarCycleOffset) {
                        Text(
                            text = "Desplazamiento: ${if (uiState.dateOffsetDays >= 0) "+${uiState.dateOffsetDays}" else "${uiState.dateOffsetDays}"} días",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = uiState.dateOffsetDays.toFloat(),
                            onValueChange = { onUpdateOffsetDays(it.roundToInt()) },
                            valueRange = -180f..180f,
                            modifier = Modifier.testTag("date_offset_days_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(-30, -7, 7, 14, 29, 90).forEach { preset ->
                                AssistChip(
                                    onClick = { onUpdateOffsetDays(preset) },
                                    label = { Text(if (preset > 0) "+${preset}d" else "${preset}d") }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Ciclos sinódicos: ${"%.1f".format(uiState.dateOffsetLunarCycles)} lunas (${(uiState.dateOffsetLunarCycles * 29.5306).roundToInt()} días)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = uiState.dateOffsetLunarCycles.toFloat(),
                            onValueChange = {
                                val stepped = (it * 2f).roundToInt() / 2.0
                                onUpdateOffsetLunarCycles(stepped)
                            },
                            valueRange = -12f..12f,
                            modifier = Modifier.testTag("date_offset_cycles_slider")
                        )
                    }

                    // Resulting Date & Moon Phase Preview Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(4.dp)
                            ) {
                                RealisticMoonCanvas(
                                    phaseFraction = targetLunar.phaseFraction,
                                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                                    showAmbientGlow = false
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.date_calc_result_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = targetDate.formatSpanishLong().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("computed_target_date_text")
                                )
                                Text(
                                    text = "${targetLunar.phaseType.emoji} ${targetLunar.phaseType.spanishName} • ${"%.1f".format(targetLunar.illuminationPercent)}% iluminada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onJumpToDateInCalendar(targetDate) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_to_computed_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.apply_to_calendar))
                    }
                }
            }
        }

        // 2. Interval Comparison Card (Days & Lunar Cycles between two dates)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Intervalo entre Dos Fechas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Desde: ${uiState.selectedDate.formatSpanishShort()}  →  Hasta: ${uiState.comparisonEndDate.formatSpanishShort()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { onStepComparisonDate(-30) },
                            label = { Text("-30d") }
                        )
                        AssistChip(
                            onClick = { onStepComparisonDate(-7) },
                            label = { Text("-7d") }
                        )
                        AssistChip(
                            onClick = { onStepComparisonDate(7) },
                            label = { Text("+7d") }
                        )
                        AssistChip(
                            onClick = { onStepComparisonDate(30) },
                            label = { Text("+30d") }
                        )
                    }

                    val diffDays = abs(uiState.daysBetweenSelectedAndComparison)
                    val diffCycles = abs(uiState.synodicCyclesBetweenSelectedAndComparison)

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$diffDays días naturales",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${"%.2f".format(diffCycles)} meses sinódicos lunares",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "🌕×${"%.1f".format(diffCycles)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
