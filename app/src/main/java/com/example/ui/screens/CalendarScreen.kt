package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BackupImportValidator
import com.example.domain.CalendarDayCell
import com.example.domain.LunarAstronomer
import com.example.domain.MajorPhaseMilestone
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import com.example.notifications.SeleniaAlertManager
import com.example.ui.CalendarNote
import com.example.ui.SeleniaUiState
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.MoonPhaseBadgeIcon
import com.example.ui.components.RealisticMoonCanvas
import com.example.ui.components.SeleniaVectorIcons
import com.example.ui.components.recommendedCalendarLitColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val NOTE_CATEGORIES = listOf("Personal", "Astronomía", "Siembra", "Cálculo")
private val QUICK_REMINDER_TIMES = listOf(
    8 to 0,
    9 to 0,
    12 to 0,
    18 to 0,
    20 to 0
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    uiState: SeleniaUiState,
    onSelectDate: (SimpleDate) -> Unit,
    onChangeMonth: (Int) -> Unit,
    onJumpToYearMonth: (Int, Int) -> Unit = { _, _ -> },
    onJumpToToday: () -> Unit,
    onAddNote: (String, String, Boolean, Int, Int) -> Unit,
    onUpdateNote: (SimpleDate, Long, String, String, Boolean, Int, Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteNote: (SimpleDate, Long) -> Unit,
    onExportBackupJson: ((String) -> Unit) -> Unit = {},
    onImportBackupJson: (String) -> Unit = {},
    onDismissBackupMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ioScope = rememberCoroutineScope()

    var showMonthYearPicker by rememberSaveable { mutableStateOf(false) }
    var showAllNotesDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderTimePicker by rememberSaveable { mutableStateOf(false) }

    var showAddNoteForm by rememberSaveable { mutableStateOf(false) }
    var editingNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var noteTitle by rememberSaveable { mutableStateOf("") }
    var noteCategory by rememberSaveable { mutableStateOf("Personal") }
    var noteReminder by rememberSaveable { mutableStateOf(false) }
    var noteReminderHour by rememberSaveable { mutableIntStateOf(9) }
    var noteReminderMinute by rememberSaveable { mutableIntStateOf(0) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        noteReminder = granted
    }

    val createBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val jsonPayload = pendingExportJson
        pendingExportJson = null
        if (uri != null && jsonPayload != null) {
            ioScope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val openBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ioScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("No se puede abrir el respaldo")
                    BackupImportValidator.readBounded(stream)
                }
                result.onSuccess { onImportBackupJson(it) }
                result.onFailure {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "No se pudo leer el JSON. Comprueba el archivo y el límite de 8 MiB.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun resetNoteForm() {
        editingNoteId = null
        noteTitle = ""
        noteCategory = "Personal"
        noteReminder = false
        noteReminderHour = 9
        noteReminderMinute = 0
        showAddNoteForm = false
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            initialYear = uiState.displayYear,
            initialMonth = uiState.displayMonth,
            onDismiss = { showMonthYearPicker = false },
            onConfirmYearMonth = { year, month ->
                showMonthYearPicker = false
                onJumpToYearMonth(year, month)
            }
        )
    }

    if (showReminderTimePicker) {
        ReminderTimePickerDialog(
            initialHour = noteReminderHour,
            initialMinute = noteReminderMinute,
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { h, m ->
                noteReminderHour = h
                noteReminderMinute = m
                showReminderTimePicker = false
            }
        )
    }

    val allNotesChronological = remember(uiState.notesByDate) {
        uiState.notesByDate.values
            .flatten()
            .sortedWith(
                compareBy<CalendarNote>(
                    { it.date.toPackedKey() },
                    { it.reminderHour },
                    { it.reminderMinute },
                    { it.id }
                )
            )
    }

    if (showAllNotesDialog) {
        AllNotesSearchDialog(
            allNotes = allNotesChronological,
            onJumpToNoteDate = { targetDate ->
                showAllNotesDialog = false
                onSelectDate(targetDate)
            },
            onDeleteNote = onDeleteNote,
            onDismiss = { showAllNotesDialog = false }
        )
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
            // 1. Month Header Bar + "Hoy" button
            item(key = "month_header_grid") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(uiState.displayYear, uiState.displayMonth) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulatedX = 0f },
                                onDragEnd = {
                                    if (dragAccumulatedX > 90f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onChangeMonth(-1)
                                    } else if (dragAccumulatedX < -90f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onChangeMonth(1)
                                    }
                                    dragAccumulatedX = 0f
                                }
                            ) { _, dragAmount ->
                                dragAccumulatedX += dragAmount
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
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
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Button) { showMonthYearPicker = true }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("calendar_month_year_picker_trigger")
                                ) {
                                    Text(
                                        text = "$monthTitle ▾",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("calendar_month_title")
                                    )
                                    Text(
                                        text = "Toca para ir a Mes/Año • Desliza ← →",
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

                            AssistChip(
                                onClick = onJumpToToday,
                                label = { Text(stringResource(R.string.today_button)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = SeleniaVectorIcons.Today,
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

            // 1.5 Phase Legend Strip with Colored Mini-Moons
            item(key = "phase_legend") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CalendarPhaseLegendBadge(
                        phaseType = MoonPhaseType.NEW_MOON,
                        color = MoonPhaseType.NEW_MOON.recommendedCalendarLitColor(),
                        label = "Nueva",
                        isNorthernHemisphere = uiState.isNorthernHemisphere
                    )
                    CalendarPhaseLegendBadge(
                        phaseType = MoonPhaseType.FIRST_QUARTER,
                        color = MoonPhaseType.FIRST_QUARTER.recommendedCalendarLitColor(),
                        label = "Creciente",
                        isNorthernHemisphere = uiState.isNorthernHemisphere
                    )
                    CalendarPhaseLegendBadge(
                        phaseType = MoonPhaseType.FULL_MOON,
                        color = MoonPhaseType.FULL_MOON.recommendedCalendarLitColor(),
                        label = "Llena",
                        isNorthernHemisphere = uiState.isNorthernHemisphere
                    )
                    CalendarPhaseLegendBadge(
                        phaseType = MoonPhaseType.LAST_QUARTER,
                        color = MoonPhaseType.LAST_QUARTER.recommendedCalendarLitColor(),
                        label = "Menguante",
                        isNorthernHemisphere = uiState.isNorthernHemisphere
                    )
                }
            }

            // 2. Month Major Lunar Milestones Strip
            if (uiState.monthMilestones.isNotEmpty()) {
                item(key = "month_milestones") {
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
                            items(
                                items = uiState.monthMilestones,
                                key = { it.date.toPackedKey() }
                            ) { milestone ->
                                MilestoneChipCard(
                                    milestone = milestone,
                                    isSelected = milestone.date == uiState.selectedDate,
                                    moonLitColor = milestone.phaseType.recommendedCalendarLitColor(),
                                    isNorthernHemisphere = uiState.isNorthernHemisphere,
                                    onClick = { onSelectDate(milestone.date) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Personal Notes & Agenda for Selected Day (Create, Edit, Custom HH:mm Reminder & Search All)
            item(key = "notes_card") {
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
                            Column(modifier = Modifier.weight(1f)) {
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = { showAllNotesDialog = true },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = SeleniaVectorIcons.EventNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        val countSuffix = if (allNotesChronological.isNotEmpty()) {
                                            " (${allNotesChronological.size})"
                                        } else {
                                            ""
                                        }
                                        Text("${stringResource(R.string.all_notes_button)}$countSuffix")
                                    },
                                    modifier = Modifier.testTag("open_all_notes_button")
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (showAddNoteForm) {
                                            resetNoteForm()
                                        } else {
                                            showAddNoteForm = true
                                        }
                                    },
                                    modifier = Modifier.testTag("toggle_add_note_button")
                                ) {
                                    Icon(
                                        imageVector = if (showAddNoteForm) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showAddNoteForm) "Cerrar" else "Nueva")
                                }
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
                                    label = {
                                        Text(
                                            if (editingNoteId != null) {
                                                "Editar nota seleccionada"
                                            } else {
                                                "Descripción del evento o nota lunar"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("calendar_note_input")
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    NOTE_CATEGORIES.forEach { cat ->
                                        FilterChip(
                                            selected = noteCategory == cat,
                                            onClick = { noteCategory = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                    FilterChip(
                                        selected = noteReminder,
                                        onClick = {
                                            if (!noteReminder) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    !SeleniaAlertManager.hasNotificationPermission(context)
                                                ) {
                                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    noteReminder = true
                                                }
                                            } else {
                                                noteReminder = false
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            val formattedTime = "%02d:%02d".format(
                                                Locale.US,
                                                noteReminderHour,
                                                noteReminderMinute
                                            )
                                            Text(
                                                if (noteReminder) {
                                                    "Aviso $formattedTime"
                                                } else {
                                                    stringResource(R.string.note_reminder_toggle)
                                                }
                                            )
                                        },
                                        modifier = Modifier.testTag("note_reminder_chip")
                                    )
                                    if (noteReminder) {
                                        AssistChip(
                                            onClick = { showReminderTimePicker = true },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = SeleniaVectorIcons.AccessTime,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    "%02d:%02d ▾".format(
                                                        Locale.US,
                                                        noteReminderHour,
                                                        noteReminderMinute
                                                    )
                                                )
                                            },
                                            modifier = Modifier.testTag("note_reminder_time_chip")
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (editingNoteId != null) {
                                        TextButton(onClick = { resetNoteForm() }) {
                                            Text(stringResource(R.string.dialog_cancel_button))
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (noteTitle.isNotBlank()) {
                                                val targetId = editingNoteId
                                                if (targetId != null) {
                                                    onUpdateNote(
                                                        uiState.selectedDate,
                                                        targetId,
                                                        noteTitle,
                                                        noteCategory,
                                                        noteReminder,
                                                        noteReminderHour,
                                                        noteReminderMinute
                                                    )
                                                } else {
                                                    onAddNote(
                                                        noteTitle,
                                                        noteCategory,
                                                        noteReminder,
                                                        noteReminderHour,
                                                        noteReminderMinute
                                                    )
                                                }
                                                resetNoteForm()
                                            }
                                        },
                                        modifier = Modifier.testTag("save_calendar_note_button")
                                    ) {
                                        Text(
                                            if (editingNoteId != null) {
                                                "Guardar cambios"
                                            } else {
                                                "Guardar en ${uiState.selectedDate.formatSpanishShort()}"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedDayNotes.isEmpty()) {
                            Text(
                                text = "Sin notas registradas para este día. Pulsa «Nueva» para agendar recordatorios astronómicos o personales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            selectedDayNotes.forEach { note ->
                                NoteItemRow(
                                    note = note,
                                    onEdit = {
                                        editingNoteId = note.id
                                        noteTitle = note.title
                                        noteCategory = note.category
                                        noteReminder = note.reminderEnabled
                                        noteReminderHour = note.reminderHour
                                        noteReminderMinute = note.reminderMinute
                                        showAddNoteForm = true
                                    },
                                    onDelete = { onDeleteNote(uiState.selectedDate, note.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Zero-Permission Local JSON Backup & Restore (Storage Access Framework)
            item(key = "local_backup_card") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_backup_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.backup_card_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.backup_card_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        uiState.backupStatusMessage?.let { statusMsg ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onDismissBackupMessage) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar aviso",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onExportBackupJson { json ->
                                        pendingExportJson = json
                                        val stamp = uiState.today.toStorageKey()
                                        createBackupDocumentLauncher.launch("TodUno-respaldo-$stamp.json")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(
                                    imageVector = SeleniaVectorIcons.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.backup_export_button))
                            }

                            FilledTonalButton(
                                onClick = {
                                    openBackupDocumentLauncher.launch(
                                        arrayOf("application/json", "text/plain", "*/*")
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_button")
                            ) {
                                Icon(
                                    imageVector = SeleniaVectorIcons.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.backup_import_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hourText by rememberSaveable { mutableStateOf("%02d".format(Locale.US, initialHour.coerceIn(0, 23))) }
    var minuteText by rememberSaveable { mutableStateOf("%02d".format(Locale.US, initialMinute.coerceIn(0, 59))) }

    val parsedHour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val parsedMinute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.note_reminder_time_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(2)
                            hourText = digits
                        },
                        label = { Text("Hora (0-23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reminder_hour_input")
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(2)
                            minuteText = digits
                        },
                        label = { Text("Min (0-59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reminder_minute_input")
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_REMINDER_TIMES.forEach { (h, m) ->
                        val label = "%02d:%02d".format(Locale.US, h, m)
                        FilterChip(
                            selected = parsedHour == h && parsedMinute == m,
                            onClick = {
                                hourText = "%02d".format(Locale.US, h)
                                minuteText = "%02d".format(Locale.US, m)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel_button))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(parsedHour, parsedMinute) },
                modifier = Modifier.testTag("confirm_reminder_time_button")
            ) {
                Text(stringResource(R.string.dialog_done_button))
            }
        }
    )
}

@Composable
private fun AllNotesSearchDialog(
    allNotes: List<CalendarNote>,
    onJumpToNoteDate: (SimpleDate) -> Unit,
    onDeleteNote: (SimpleDate, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredNotes = remember(allNotes, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) {
            allNotes
        } else {
            allNotes.filter { note ->
                note.title.lowercase(Locale.ROOT).contains(q) ||
                    note.category.lowercase(Locale.ROOT).contains(q) ||
                    note.date.formatSpanishShort().lowercase(Locale.ROOT).contains(q) ||
                    note.date.storageKey.contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.all_notes_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    placeholder = {
                        Text(stringResource(R.string.search_notes_placeholder))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_notes_input")
                )

                if (filteredNotes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.all_notes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .testTag("all_notes_dialog_list")
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Surface(
                                onClick = { onJumpToNoteDate(note.date) },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("all_notes_item_${note.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = note.date.formatSpanishShort(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "• ${note.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            if (note.reminderEnabled) {
                                                Text(
                                                    text = "🔔 ${note.formattedReminderTime()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteNote(note.date, note.id) }
                                    ) {
                                        Icon(
                                            imageVector = SeleniaVectorIcons.DeleteOutline,
                                            contentDescription = stringResource(R.string.delete_note_desc),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("all_notes_dialog_done")
            ) {
                Text(stringResource(R.string.dialog_done_button))
            }
        }
    )
}

@Composable
private fun CalendarPhaseLegendBadge(
    phaseType: MoonPhaseType,
    color: Color,
    label: String,
    isNorthernHemisphere: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MoonPhaseBadgeIcon(
            phaseType = phaseType,
            size = 16.dp,
            litColor = color,
            southernHemisphere = !isNorthernHemisphere
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveCellMoonColor = cell.lunarInfo.phaseType.recommendedCalendarLitColor()

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
            .clickable(role = Role.Button, onClick = onClick)
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
            MoonPhaseBadgeIcon(
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
    onEdit: () -> Unit,
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
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = note.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (note.reminderEnabled) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Recordatorio activo",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = note.formattedReminderTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_note_${note.id}")
                ) {
                    Icon(
                        imageVector = SeleniaVectorIcons.Edit,
                        contentDescription = stringResource(R.string.edit_note_desc),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_note_${note.id}")
                ) {
                    Icon(
                        imageVector = SeleniaVectorIcons.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_note_desc),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
