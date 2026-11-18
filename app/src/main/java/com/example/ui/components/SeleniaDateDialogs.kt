package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.LunarAstronomer

private val MONTH_ROWS_CHUNKED = LunarAstronomer.SPANISH_MONTHS.chunked(3)

@Composable
fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirmYearMonth: (year: Int, month: Int) -> Unit
) {
    var draftYear by rememberSaveable { mutableIntStateOf(initialYear) }
    var draftMonth by rememberSaveable { mutableIntStateOf(initialMonth.coerceIn(1, 12)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ir a Mes y Año",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Year selector with -10, -1, +1, +10 controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { draftYear = (draftYear - 10).coerceIn(1900, 2150) }) {
                            Text("-10")
                        }
                        FilledTonalIconButton(
                            onClick = { draftYear = (draftYear - 1).coerceIn(1900, 2150) },
                            modifier = Modifier.testTag("picker_prev_year")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Año anterior"
                            )
                        }
                        Text(
                            text = draftYear.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("picker_year_label")
                        )
                        FilledTonalIconButton(
                            onClick = { draftYear = (draftYear + 1).coerceIn(1900, 2150) },
                            modifier = Modifier.testTag("picker_next_year")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Año siguiente"
                            )
                        }
                        TextButton(onClick = { draftYear = (draftYear + 10).coerceIn(1900, 2150) }) {
                            Text("+10")
                        }
                    }
                }

                // 12-Month Grid (4 rows x 3 columns, >= 48dp accessible touch targets)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MONTH_ROWS_CHUNKED.forEachIndexed { rowIdx, rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEachIndexed { colIdx, monthName ->
                                val monthNumber = rowIdx * 3 + colIdx + 1
                                val selected = draftMonth == monthNumber
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            }
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 0.5.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(role = Role.Button) { draftMonth = monthNumber }
                                        .testTag("picker_month_$monthNumber")
                                ) {
                                    Text(
                                        text = monthName.take(3),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmYearMonth(draftYear, draftMonth) },
                modifier = Modifier.testTag("confirm_month_year_button")
            ) {
                Text("Ir a ${LunarAstronomer.SPANISH_MONTHS[draftMonth - 1]} $draftYear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
