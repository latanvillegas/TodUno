package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CalculationRecord
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.ui.CalculatorMode
import com.example.ui.SeleniaUiState
import com.example.ui.components.MoonPhaseBadgeIcon
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.components.SeleniaCalendarDatePickerModal
import com.example.ui.components.SeleniaVectorIcons
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
private data class CalcKeyStyle(
    val bgBrush: Brush,
    val borderBrush: Brush,
    val contentColor: Color
)

private val KEY_SHAPE = RoundedCornerShape(16.dp)
private val DARK_KEY_BORDER_BOTTOM = Color(0xFF0A0F1F).copy(alpha = 0.8f)

private val STYLE_PRIMARY_EQUAL = CalcKeyStyle(
    bgBrush = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA000))),
    borderBrush = Brush.verticalGradient(listOf(Color(0xFFFFF8E1).copy(alpha = 0.85f), DARK_KEY_BORDER_BOTTOM)),
    contentColor = Color(0xFF101626)
)

private val STYLE_DANGER = CalcKeyStyle(
    bgBrush = Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFF8E0000))),
    borderBrush = Brush.verticalGradient(listOf(Color(0xFFFF8A80).copy(alpha = 0.70f), DARK_KEY_BORDER_BOTTOM)),
    contentColor = Color(0xFFFFEBEE)
)

private val STYLE_OPERATOR = CalcKeyStyle(
    bgBrush = Brush.verticalGradient(listOf(Color(0xFF00ACC1), Color(0xFF006064))),
    borderBrush = Brush.verticalGradient(listOf(Color(0xFF84FFFF).copy(alpha = 0.70f), DARK_KEY_BORDER_BOTTOM)),
    contentColor = Color(0xFFE0F7FA)
)

private val STYLE_SCIENTIFIC = CalcKeyStyle(
    bgBrush = Brush.verticalGradient(listOf(Color(0xFF39237E), Color(0xFF1F1347))),
    borderBrush = Brush.verticalGradient(listOf(Color(0xFFB388FF).copy(alpha = 0.60f), DARK_KEY_BORDER_BOTTOM)),
    contentColor = Color(0xFFEDE7F6)
)

private val STYLE_STANDARD = CalcKeyStyle(
    bgBrush = Brush.verticalGradient(listOf(Color(0xFF223052), Color(0xFF141C33))),
    borderBrush = Brush.verticalGradient(listOf(Color(0xFF4DD0E1).copy(alpha = 0.35f), DARK_KEY_BORDER_BOTTOM)),
    contentColor = Color(0xFFF4F7FF)
)

private val STYLE_BACKSPACE = CalcKeyStyle(
    bgBrush = Brush.verticalGradient(listOf(Color(0xFF283558), Color(0xFF17203A))),
    borderBrush = Brush.verticalGradient(listOf(Color(0xFF80DEEA).copy(alpha = 0.55f), Color(0xFF10172B))),
    contentColor = Color(0xFF80DEEA)
)

private val DISPLAY_BORDER_BRUSH = Brush.linearGradient(
    listOf(
        Color(0xFFFFD54F).copy(alpha = 0.85f),
        Color(0xFF4DD0E1).copy(alpha = 0.75f)
    )
)

private val DAY_OFFSET_PRESETS = listOf(-30, -7, 7, 14, 29, 90)

@Composable
fun CalculatorScreen(
    uiState: SeleniaUiState,
    onSetMode: (CalculatorMode) -> Unit,
    onToggleRadiansMode: () -> Unit = {},
    onInputToken: (String) -> Unit,
    onInsertLastAnswer: () -> Unit = {},
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: (String?) -> Unit,
    onConvertDaysToSynodic: () -> Unit,
    onMultiplyBySynodic: () -> Unit,
    onUseHistoryItem: (CalculationRecord) -> Unit,
    onClearHistory: () -> Unit,
    onSelectBaseDate: (SimpleDate) -> Unit = {},
    onSetUseLunarCycleOffset: (Boolean) -> Unit,
    onUpdateOffsetDays: (Int) -> Unit,
    onUpdateOffsetLunarCycles: (Double) -> Unit,
    onStepComparisonDate: (Int) -> Unit,
    onSetComparisonStartDate: (SimpleDate) -> Unit = {},
    onSetComparisonEndDate: (SimpleDate) -> Unit = {},
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.calculatorMode == CalculatorMode.SCIENTIFIC,
                    onClick = { onSetMode(CalculatorMode.SCIENTIFIC) },
                    label = { Text(stringResource(R.string.calc_mode_standard)) },
                    leadingIcon = {
                        Icon(
                            imageVector = SeleniaVectorIcons.Calculate,
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
                    onToggleRadiansMode = onToggleRadiansMode,
                    onInputToken = onInputToken,
                    onInsertLastAnswer = onInsertLastAnswer,
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
                    onSelectBaseDate = onSelectBaseDate,
                    onSetUseLunarCycleOffset = onSetUseLunarCycleOffset,
                    onUpdateOffsetDays = onUpdateOffsetDays,
                    onUpdateOffsetLunarCycles = onUpdateOffsetLunarCycles,
                    onStepComparisonDate = onStepComparisonDate,
                    onSetComparisonStartDate = onSetComparisonStartDate,
                    onSetComparisonEndDate = onSetComparisonEndDate,
                    onJumpToDateInCalendar = onJumpToDateInCalendar
                )
            }
        }
    }
}

@Composable
private fun ScientificCalculatorContent(
    uiState: SeleniaUiState,
    onToggleRadiansMode: () -> Unit,
    onInputToken: (String) -> Unit,
    onInsertLastAnswer: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: (String?) -> Unit,
    onConvertDaysToSynodic: () -> Unit,
    onMultiplyBySynodic: () -> Unit,
    onUseHistoryItem: (CalculationRecord) -> Unit,
    onClearHistory: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. Expression & Result Display Screen
        item(key = "calc_display") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.8.dp,
                        brush = DISPLAY_BORDER_BRUSH,
                        shape = MaterialTheme.shapes.large
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isRadiansMode) "Ángulo: RAD (Radianes)" else "Ángulo: DEG (Grados)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Ans = ${uiState.lastAnswer}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

        // 2. DEG/RAD, Ans & Lunar Constant Quick Action Strip
        item(key = "calc_quick_strip") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "chip_deg_rad") {
                    FilterChip(
                        selected = uiState.isRadiansMode,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleRadiansMode()
                        },
                        label = {
                            Text(
                                text = if (uiState.isRadiansMode) "RAD" else "DEG",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("calc_deg_rad_chip")
                    )
                }
                item(key = "chip_ans") {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onInsertLastAnswer()
                        },
                        label = { Text("Ans") },
                        modifier = Modifier.testTag("calc_ans_chip")
                    )
                }
                item(key = "chip_mul_synodic") {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMultiplyBySynodic()
                        },
                        label = { Text("× Luna (29.53d)") },
                        modifier = Modifier.testTag("calc_multiply_synodic_chip")
                    )
                }
                item(key = "chip_div_synodic") {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onConvertDaysToSynodic()
                        },
                        label = { Text("Días → Lunas") },
                        modifier = Modifier.testTag("calc_days_to_lunas_chip")
                    )
                }
                item(key = "chip_const_synodic") {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onInputToken("29.5306")
                        },
                        leadingIcon = {
                            MoonPhaseBadgeIcon(
                                phaseType = MoonPhaseType.WAXING_CRESCENT,
                                size = 16.dp
                            )
                        },
                        label = { Text("Constante Sinódica") },
                        modifier = Modifier.testTag("calc_insert_synodic_constant")
                    )
                }
            }
        }

        // 3. Scientific & Standard Keypad (3D Tactile Keycaps)
        item(key = "calc_keypad") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKeyButton("AC", Modifier.weight(1f), isDanger = true, tag = "key_clear") {
                        onClear()
                    }
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackspace()
                        },
                        shape = KEY_SHAPE,
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(KEY_SHAPE)
                            .background(STYLE_BACKSPACE.bgBrush)
                            .border(
                                width = 1.dp,
                                brush = STYLE_BACKSPACE.borderBrush,
                                shape = KEY_SHAPE
                            )
                            .testTag("key_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = SeleniaVectorIcons.Backspace,
                                contentDescription = "Borrar",
                                tint = STYLE_BACKSPACE.contentColor
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
        item(key = "calc_history") {
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
                                imageVector = SeleniaVectorIcons.History,
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
                                    .clickable(role = Role.Button) { onUseHistoryItem(item) }
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
    val style = when {
        isPrimaryEqual -> STYLE_PRIMARY_EQUAL
        isDanger -> STYLE_DANGER
        isOperator -> STYLE_OPERATOR
        isScientific -> STYLE_SCIENTIFIC
        else -> STYLE_STANDARD
    }

    val haptic = LocalHapticFeedback.current
    val height = if (isScientific) 48.dp else 54.dp

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = KEY_SHAPE,
        color = Color.Transparent,
        modifier = modifier
            .height(height)
            .clip(KEY_SHAPE)
            .background(style.bgBrush)
            .border(
                width = 1.dp,
                brush = style.borderBrush,
                shape = KEY_SHAPE
            )
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = if (isScientific) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = style.contentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateAndLunarCalculatorContent(
    uiState: SeleniaUiState,
    onSelectBaseDate: (SimpleDate) -> Unit,
    onSetUseLunarCycleOffset: (Boolean) -> Unit,
    onUpdateOffsetDays: (Int) -> Unit,
    onUpdateOffsetLunarCycles: (Double) -> Unit,
    onStepComparisonDate: (Int) -> Unit,
    onSetComparisonStartDate: (SimpleDate) -> Unit,
    onSetComparisonEndDate: (SimpleDate) -> Unit,
    onJumpToDateInCalendar: (SimpleDate) -> Unit
) {
    var showBaseDatePicker by rememberSaveable { mutableStateOf(false) }
    var showComparisonStartPicker by rememberSaveable { mutableStateOf(false) }
    var showComparisonDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showBaseDatePicker) {
        SeleniaCalendarDatePickerModal(
            initialDate = uiState.selectedDate,
            onDismiss = { showBaseDatePicker = false },
            onDateSelected = { picked ->
                showBaseDatePicker = false
                onSelectBaseDate(picked)
            }
        )
    }

    if (showComparisonStartPicker) {
        SeleniaCalendarDatePickerModal(
            initialDate = uiState.comparisonStartDate,
            onDismiss = { showComparisonStartPicker = false },
            onDateSelected = { picked ->
                showComparisonStartPicker = false
                onSetComparisonStartDate(picked)
            }
        )
    }

    if (showComparisonDatePicker) {
        SeleniaCalendarDatePickerModal(
            initialDate = uiState.comparisonEndDate,
            onDismiss = { showComparisonDatePicker = false },
            onDateSelected = { picked ->
                showComparisonDatePicker = false
                onSetComparisonEndDate(picked)
            }
        )
    }

    val targetDate = uiState.computedTargetDate
    val targetLunar = uiState.computedTargetLunarInfo

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("date_lunar_calculator_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "date_offset_card") {
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

                        Surface(
                            onClick = { showBaseDatePicker = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_base_date_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MoonPhaseBadgeIcon(
                                        phaseType = uiState.selectedDayLunarInfo.phaseType,
                                        size = 20.dp
                                    )
                                    Column {
                                        Text(
                                            text = "Fecha base (toca para elegir)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "${uiState.selectedDate.formatSpanishLong().replaceFirstChar { it.uppercase() }} (${uiState.selectedDayLunarInfo.phaseType.spanishName})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = SeleniaVectorIcons.CalendarMonth,
                                    contentDescription = "Elegir fecha base",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

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
                                DAY_OFFSET_PRESETS.forEach { preset ->
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

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MoonPhaseBadgeIcon(
                                            phaseType = targetLunar.phaseType,
                                            size = 15.dp
                                        )
                                        Text(
                                            text = "${targetLunar.phaseType.spanishName} • ${"%.1f".format(targetLunar.illuminationPercent)}% iluminada",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
                                imageVector = SeleniaVectorIcons.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.apply_to_calendar))
                        }
                }
            }
        }

        // 2. Interval Comparison Card
        item(key = "interval_comparison_card") {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { showComparisonStartPicker = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_comparison_start_date_button")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Desde (toca)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = uiState.comparisonStartDate.formatSpanishShort(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            onClick = { showComparisonDatePicker = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_comparison_end_date_button")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Hasta (toca)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = uiState.comparisonEndDate.formatSpanishShort(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MoonPhaseBadgeIcon(
                                    phaseType = MoonPhaseType.FULL_MOON,
                                    size = 24.dp
                                )
                                Text(
                                    text = "×${"%.1f".format(diffCycles)}",
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
}
