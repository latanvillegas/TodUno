package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CalculationRecord
import com.example.ui.SeleniaUiState
import com.example.ui.components.SeleniaVectorIcons
import kotlinx.coroutines.delay

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

@Composable
fun CalculatorScreen(
    uiState: SeleniaUiState,
    onToggleRadiansMode: () -> Unit = {},
    onToggleInverseTrigMode: () -> Unit = {},
    onInputToken: (String) -> Unit,
    onInsertLastAnswer: () -> Unit = {},
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: (String?) -> Unit,
    onUseHistoryItem: (CalculationRecord) -> Unit,
    onClearHistory: () -> Unit,
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
            ScientificCalculatorContent(
                uiState = uiState,
                onToggleRadiansMode = onToggleRadiansMode,
                onToggleInverseTrigMode = onToggleInverseTrigMode,
                onInputToken = onInputToken,
                onInsertLastAnswer = onInsertLastAnswer,
                onClear = onClear,
                onBackspace = onBackspace,
                onEvaluate = onEvaluate,
                onUseHistoryItem = onUseHistoryItem,
                onClearHistory = onClearHistory
            )
        }
    }
}

@Composable
private fun ScientificCalculatorContent(
    uiState: SeleniaUiState,
    onToggleRadiansMode: () -> Unit,
    onToggleInverseTrigMode: () -> Unit,
    onInputToken: (String) -> Unit,
    onInsertLastAnswer: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEvaluate: (String?) -> Unit,
    onUseHistoryItem: (CalculationRecord) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(1600L)
            copiedFeedback = false
        }
    }

    fun copyResultToClipboard() {
        val valueToCopy = uiState.liveResultPreview
        if (valueToCopy.isBlank() || valueToCopy == "Error") return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("TodUno Resultado", valueToCopy))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        copiedFeedback = true
    }

    val isInv = uiState.isInverseTrigMode

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. Expression & Result Display Screen (tap result to copy)
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
                            text = buildString {
                                append(if (uiState.isRadiansMode) "Ángulo: RAD" else "Ángulo: DEG")
                                if (isInv) append(" • INV activo")
                            },
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
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { copyResultToClipboard() }
                            .testTag("calc_result_display")
                    )
                }
            }
        }

        // 2. DEG/RAD, INV, Ans & Copy Quick Action Strip
        item(key = "calc_quick_strip") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                FilterChip(
                    selected = isInv,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleInverseTrigMode()
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.calc_inv_label),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("calc_inv_chip")
                )
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInsertLastAnswer()
                    },
                    label = { Text("Ans") },
                    modifier = Modifier.testTag("calc_ans_chip")
                )
                Spacer(modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = { copyResultToClipboard() },
                    leadingIcon = {
                        Icon(
                            imageVector = if (copiedFeedback) {
                                Icons.Default.Check
                            } else {
                                SeleniaVectorIcons.ContentCopy
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (copiedFeedback) {
                                stringResource(R.string.calc_copied_label)
                            } else {
                                stringResource(R.string.calc_copy_label)
                            }
                        )
                    },
                    modifier = Modifier.testTag("calc_copy_chip")
                )
            }
        }

        // 3. Scientific & Standard Keypad (3D Tactile Keycaps, INV-aware)
        item(key = "calc_keypad") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sinLabel = if (isInv) "sin⁻¹" else "sin("
                    val sinToken = if (isInv) "asin(" else "sin("
                    CalcKeyButton(sinLabel, Modifier.weight(1f), isScientific = true, tag = "key_sin") {
                        onInputToken(sinToken)
                    }

                    val cosLabel = if (isInv) "cos⁻¹" else "cos("
                    val cosToken = if (isInv) "acos(" else "cos("
                    CalcKeyButton(cosLabel, Modifier.weight(1f), isScientific = true, tag = "key_cos") {
                        onInputToken(cosToken)
                    }

                    val tanLabel = if (isInv) "tan⁻¹" else "tan("
                    val tanToken = if (isInv) "atan(" else "tan("
                    CalcKeyButton(tanLabel, Modifier.weight(1f), isScientific = true, tag = "key_tan") {
                        onInputToken(tanToken)
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

                    val constLabel = if (isInv) "e" else "π"
                    CalcKeyButton(constLabel, Modifier.weight(1f), isScientific = true, tag = "key_pi") {
                        onInputToken(constLabel)
                    }

                    val logLabel = if (isInv) "log(" else "ln("
                    CalcKeyButton(logLabel, Modifier.weight(1f), isScientific = true, tag = "key_ln") {
                        onInputToken(logLabel)
                    }

                    val postfixLabel = if (isInv) "x!" else "%"
                    val postfixToken = if (isInv) "!" else "%"
                    CalcKeyButton(postfixLabel, Modifier.weight(1f), isScientific = true, tag = "key_percent") {
                        onInputToken(postfixToken)
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
