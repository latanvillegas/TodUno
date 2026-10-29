package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Today
import com.example.domain.LunarAlmanacCategory
import com.example.ui.components.MoonPhaseBadgeIcon
import com.example.ui.components.SeleniaIconBadge
import com.example.ui.components.accentColor
import com.example.ui.components.iconVector
import com.example.ui.components.recommendedCalendarLitColor
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CalendarDayCell
import com.example.domain.LunarAstronomer
import com.example.domain.MajorPhaseMilestone
import com.example.domain.SimpleDate
import com.example.ui.CalendarNote
import com.example.ui.ModuleThemeTarget
import com.example.ui.SeleniaUiState
import com.example.ui.components.ModuleSkinBox
import com.example.ui.components.RealisticMoonCanvas

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    uiState: SeleniaUiState,
    onSelectDate: (SimpleDate) -> Unit,
    onChangeMonth: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenMoonObservatory: () -> Unit,
    onSendAgeToCalculator: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (SimpleDate, Long) -> Unit,
    onOpenColorCustomizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddNoteForm by rememberSaveable { mutableStateOf(false) }
    var noteTitle by rememberSaveable { mutableStateOf("") }
    var noteCategory by rememberSaveable { mutableStateOf("Personal") }

    val skin = uiState.skinFor(ModuleThemeTarget.CALENDAR)
    val customMoonLitColor = if (skin.isCustomApplied) {
        Color(skin.primarySwatch.rgb.r, skin.primarySwatch.rgb.g, skin.primarySwatch.rgb.b)
    } else {
        Color(0xFFF7E7B4)
    }

    val selectedDayNotes = remember(uiState.notesByDate, uiState.selectedDate) {
        uiState.notesByDate[uiState.selectedDate.toStorageKey()].orEmpty()
    }
    val datesWithNotes = remember(uiState.notesByDate) {
        uiState.notesByDate.filterValues { it.isNotEmpty() }.keys
    }
    val weeks = remember(uiState.calendarCells) {
        uiState.calendarCells.chunked(7)
    }
    val monthTitle = "${LunarAstronomer.SPANISH_MONTHS[(uiState.displayMonth - 1).coerceIn(0, 11)]} ${uiState.displayYear}"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("calendar_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Month Header Bar + "Hoy" & "Color" buttons with custom ModuleSkinBox
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModuleSkinBox(
                        skin = skin,
                        alphaIntensity = 0.34f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (!skin.isCustomApplied) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onChangeMonth(-1) },
                                        modifier = Modifier.testTag("calendar_prev_month_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = stringResource(R.string.prev_month_desc),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = monthTitle,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("calendar_month_title")
                                        )
                                        Text(
                                            text = if (skin.isCustomApplied) {
                                                "Tema: ${skin.primarySwatch.label} (${skin.gradientStyle.badge})"
                                            } else {
                                                stringResource(R.string.astronomical_season_note)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onChangeMonth(1) },
                                        modifier = Modifier.testTag("calendar_next_month_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = stringResource(R.string.next_month_desc),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = onOpenColorCustomizer,
                                        modifier = Modifier.testTag("calendar_customize_color_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Personalizar color del calendario",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    AssistChip(
                                        onClick = onJumpToToday,
                                        label = { Text(stringResource(R.string.today_button)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Today,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("calendar_today_button")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Weekday headers (Lun..Dom)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LunarAstronomer.SPANISH_WEEKDAYS_SHORT.forEach { dayLabel ->
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 6-week (42 cells) Calendar Grid with miniature realistic moons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                weeks.forEach { weekCells ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        weekCells.forEach { cell ->
                                            val hasNotes = cell.date.storageKey in datesWithNotes
                                            CalendarDayCellItem(
                                                cell = cell,
                                                isSelected = cell.date == uiState.selectedDate,
                                                isNorthernHemisphere = uiState.isNorthernHemisphere,
                                                hasNotes = hasNotes,
                                                moonLitColor = customMoonLitColor,
                                                isCustomSkin = skin.isCustomApplied,
                                                onClick = { onSelectDate(cell.date) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 Phase Color Legend Strip for Calendar Moons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CalendarPhaseLegendBadge(
                        color = Color(0xFF4DD0E1),
                        label = "Nueva (Zafiro)"
                    )
                    CalendarPhaseLegendBadge(
                        color = Color(0xFFFFD54F),
                        label = "Creciente (Oro)"
                    )
                    CalendarPhaseLegendBadge(
                        color = Color(0xFFFFB300),
                        label = "Llena (Ámbar 3D)"
                    )
                    CalendarPhaseLegendBadge(
                        color = Color(0xFF80DEEA),
                        label = "Menguante (Plata-Cian)"
                    )
                }
            }

            // 2. Month Major Lunar Milestones Strip
            if (uiState.monthMilestones.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.month_lunar_milestones),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("month_milestones_row")
                        ) {
                            items(uiState.monthMilestones) { milestone ->
                                val phaseColor = if (skin.isCustomApplied) {
                                    customMoonLitColor
                                } else {
                                    milestone.phaseType.recommendedCalendarLitColor()
                                }
                                MilestoneChipCard(
                                    milestone = milestone,
                                    isSelected = milestone.date == uiState.selectedDate,
                                    moonLitColor = phaseColor,
                                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                                    onClick = { onSelectDate(milestone.date) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Selected Day Lunar & Almanac Inspector Card (with custom gradient skin)
            item {
                val info = uiState.selectedDayLunarInfo
                val selectedDayMoonColor = if (skin.isCustomApplied) {
                    customMoonLitColor
                } else {
                    info.phaseType.recommendedCalendarLitColor()
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_day_detail_card")
                ) {
                    ModuleSkinBox(
                        skin = skin,
                        alphaIntensity = 0.45f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(4.dp)
                                ) {
                                    RealisticMoonCanvas(
                                        phaseFraction = info.phaseFraction,
                                        isNorthernHemisphere = uiState.isNorthernHemisphere,
                                        showAmbientGlow = true,
                                        litColor = selectedDayMoonColor
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.selectedDate.formatSpanishLong().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MoonPhaseBadgeIcon(
                                            phaseType = info.phaseType,
                                            size = 22.dp,
                                            litColor = selectedDayMoonColor,
                                            southernHemisphere = !uiState.isNorthernHemisphere
                                        )
                                        Text(
                                            text = info.phaseType.spanishName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = selectedDayMoonColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Iluminación: ${"%.1f".format(info.illuminationPercent)}%  •  Edad: ${"%.1f".format(info.lunarAgeDays)} días",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Distancia: ${info.distanceKm} km  •  ${info.zodiacSign}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.58f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SeleniaIconBadge(
                                            icon = LunarAlmanacCategory.HAIR_CARE.iconVector(),
                                            tint = LunarAlmanacCategory.HAIR_CARE.accentColor(),
                                            size = 26.dp,
                                            iconSize = 14.dp
                                        )
                                        Text(
                                            text = "Cabello: ${info.phaseType.hairActionBadge}  •  Piel: ${info.phaseType.beautyActionBadge}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SeleniaIconBadge(
                                            icon = LunarAlmanacCategory.BEAUTY_SKIN.iconVector(),
                                            tint = LunarAlmanacCategory.BEAUTY_SKIN.accentColor(),
                                            size = 24.dp,
                                            iconSize = 13.dp
                                        )
                                        Text(
                                            text = info.phaseType.beautySkinTip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SeleniaIconBadge(
                                            icon = LunarAlmanacCategory.GARDENING.iconVector(),
                                            tint = LunarAlmanacCategory.GARDENING.accentColor(),
                                            size = 24.dp,
                                            iconSize = 13.dp
                                        )
                                        Text(
                                            text = info.phaseType.gardeningTip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SeleniaIconBadge(
                                            icon = LunarAlmanacCategory.ASTRONOMY.iconVector(),
                                            tint = LunarAlmanacCategory.ASTRONOMY.accentColor(),
                                            size = 24.dp,
                                            iconSize = 13.dp
                                        )
                                        Text(
                                            text = info.phaseType.stargazingTip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenMoonObservatory,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("inspect_moon_observatory_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NightlightRound,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.inspect_moon_phase))
                                }

                                FilledTonalButton(
                                    onClick = onSendAgeToCalculator,
                                    modifier = Modifier.testTag("send_day_to_calc_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.send_day_to_calc))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Personal Notes & Ephemeris Agenda for Selected Day
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
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
                                    text = "Notas y Eventos del Día",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = uiState.selectedDate.formatSpanishShort(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { showAddNoteForm = !showAddNoteForm },
                                modifier = Modifier.testTag("toggle_add_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showAddNoteForm) "Cerrar" else "Nueva Nota")
                            }
                        }

                        AnimatedVisibility(visible = showAddNoteForm) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = noteTitle,
                                    onValueChange = { noteTitle = it },
                                    label = { Text("Descripción del evento o nota lunar") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("calendar_note_input")
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Personal", "Astronomía", "Siembra", "Cálculo").forEach { cat ->
                                        FilterChip(
                                            selected = noteCategory == cat,
                                            onClick = { noteCategory = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (noteTitle.isNotBlank()) {
                                            onAddNote(noteTitle, noteCategory)
                                            noteTitle = ""
                                            showAddNoteForm = false
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .testTag("save_calendar_note_button")
                                ) {
                                    Text("Guardar en ${uiState.selectedDate.formatSpanishShort()}")
                                }
                            }
                        }

                        if (selectedDayNotes.isEmpty()) {
                            Text(
                                text = "Sin notas registradas para este día. Pulsa «Nueva Nota» para agendar recordatorios astronómicos o personales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            selectedDayNotes.forEach { note ->
                                NoteItemRow(
                                    note = note,
                                    onDelete = { onDeleteNote(uiState.selectedDate, note.id) }
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
private fun CalendarPhaseLegendBadge(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarDayCellItem(
    cell: CalendarDayCell,
    isSelected: Boolean,
    isNorthernHemisphere: Boolean,
    hasNotes: Boolean,
    moonLitColor: Color,
    isCustomSkin: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveCellMoonColor = if (isCustomSkin) {
        moonLitColor
    } else {
        cell.lunarInfo.phaseType.recommendedCalendarLitColor()
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        cell.isToday -> MaterialTheme.colorScheme.secondary
        cell.lunarInfo.isExactMajorPhaseDay && cell.isCurrentMonth ->
            effectiveCellMoonColor.copy(alpha = 0.65f)
        else -> Color.Transparent
    }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        cell.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        cell.lunarInfo.isExactMajorPhaseDay && cell.isCurrentMonth ->
            effectiveCellMoonColor.copy(alpha = 0.12f)
        cell.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.background.copy(alpha = 0.25f)
    }

    val textAlpha = if (cell.isCurrentMonth) 1f else 0.42f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = if (isSelected || cell.isToday) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("calendar_day_${cell.date.toStorageKey()}")
    ) {
        Text(
            text = cell.date.day.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected || cell.isToday || cell.lunarInfo.isExactMajorPhaseDay) FontWeight.Bold else FontWeight.Medium,
            color = (if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                .copy(alpha = textAlpha)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(modifier = Modifier.size(if (cell.lunarInfo.isExactMajorPhaseDay) 23.dp else 20.dp)) {
            RealisticMoonCanvas(
                phaseFraction = cell.lunarInfo.phaseFraction,
                isNorthernHemisphere = isNorthernHemisphere,
                showAmbientGlow = cell.lunarInfo.isExactMajorPhaseDay && cell.isCurrentMonth,
                litColor = effectiveCellMoonColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cell.lunarInfo.isExactMajorPhaseDay) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(effectiveCellMoonColor)
                )
            }
            if (hasNotes) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
private fun MilestoneChipCard(
    milestone: MajorPhaseMilestone,
    isSelected: Boolean,
    moonLitColor: Color,
    isNorthernHemisphere: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.testTag("milestone_${milestone.date.toStorageKey()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.example.ui.components.MoonPhaseBadgeIcon(
                phaseType = milestone.phaseType,
                size = 28.dp,
                litColor = moonLitColor,
                southernHemisphere = !isNorthernHemisphere
            )
            Column {
                Text(
                    text = milestone.phaseType.spanishName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${milestone.date.formatSpanishShort()} • ${milestone.illuminationPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoteItemRow(
    note: CalendarNote,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_note_${note.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar nota",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
