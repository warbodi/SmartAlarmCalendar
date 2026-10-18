package com.example.smartalarmcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartalarmcalendar.R
import com.example.smartalarmcalendar.data.model.PresetWithAlarms
import com.example.smartalarmcalendar.ui.components.PresetCard
import com.example.smartalarmcalendar.ui.components.parseHexColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    selectedDate: LocalDate,
    assignmentsMap: Map<String, List<Long>>, // YYYY-MM-DD -> List of PresetIDs
    presetsList: List<PresetWithAlarms>,
    onDateSelected: (LocalDate) -> Unit,
    onAssignPresetToDate: (LocalDate, Long) -> Unit,
    onAssignPresetToDates: (List<LocalDate>, Long) -> Unit,
    onRemovePresetFromDate: (LocalDate, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseMonth = remember { YearMonth.from(selectedDate) }
    val initialPage = 1000
    val coroutineScope = rememberCoroutineScope()

    val monthPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2000 }
    )

    val currentMonth = remember(monthPagerState.currentPage) {
        baseMonth.plusMonths((monthPagerState.currentPage - initialPage).toLong())
    }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedMultiDates = remember { mutableStateOf(setOf<LocalDate>()) }

    var showPresetSelectorSheet by remember { mutableStateOf(false) }

    val selectedDateString = remember(selectedDate) { selectedDate.toString() }
    val presetsByDateMap = remember(assignmentsMap, presetsList) {
        val presetsById = presetsList.associateBy { it.preset.id }
        assignmentsMap.mapValues { (_, presetIds) ->
            presetIds.mapNotNull { presetsById[it] }
        }
    }
    val assignedPresetsForSelectedDate = remember(selectedDateString, presetsByDateMap) {
        presetsByDateMap[selectedDateString] ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header: Month Navigation + Multi-Select Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            monthPagerState.animateScrollToPage(monthPagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.prev_month))
                }

                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            monthPagerState.animateScrollToPage(monthPagerState.currentPage + 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next_month))
                }
            }

            FilterChip(
                selected = isMultiSelectMode,
                onClick = {
                    isMultiSelectMode = !isMultiSelectMode
                    if (!isMultiSelectMode) {
                        selectedMultiDates.value = emptySet()
                    }
                },
                label = {
                    Text(if (isMultiSelectMode) stringResource(R.string.selection_active) else stringResource(R.string.multi_days))
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isMultiSelectMode) Icons.Default.Check else Icons.Default.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekday Labels (MON, TUE... or LUN, MAR...)
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = remember {
                (1..7).map { day ->
                    java.time.DayOfWeek.of(day)
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        .uppercase()
                }
            }
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pre-loaded Month Pager (Previous & Next Month Pre-rendered in Memory)
        HorizontalPager(
            state = monthPagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageMonth = baseMonth.plusMonths((page - initialPage).toLong())
            val daysInMonth = remember(pageMonth) {
                val firstDay = pageMonth.atDay(1)
                val dayOfWeek = firstDay.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                val padding = dayOfWeek - 1

                val list = mutableListOf<LocalDate?>()
                repeat(padding) { list.add(null) }
                for (day in 1..pageMonth.lengthOfMonth()) {
                    list.add(pageMonth.atDay(day))
                }
                list
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = daysInMonth.size,
                    key = { index -> daysInMonth[index]?.toString() ?: "empty_${page}_$index" }
                ) { index ->
                    val date = daysInMonth[index]
                    if (date != null) {
                        val dateStr = remember(date) { date.toString() }
                        val presetsForDate = remember(dateStr, presetsByDateMap) { presetsByDateMap[dateStr] ?: emptyList() }

                        val isSelectedInSingle = !isMultiSelectMode && date == selectedDate
                        val isSelectedInMulti = isMultiSelectMode && selectedMultiDates.value.contains(date)
                        val isToday = date == LocalDate.now()

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelectedInMulti -> MaterialTheme.colorScheme.tertiaryContainer
                                        isSelectedInSingle -> MaterialTheme.colorScheme.primaryContainer
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = when {
                                        isSelectedInMulti -> 2.dp
                                        isToday && !isSelectedInSingle -> 1.dp
                                        else -> 0.dp
                                    },
                                    color = if (isSelectedInMulti) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isMultiSelectMode) {
                                        val current = selectedMultiDates.value.toMutableSet()
                                        if (current.contains(date)) {
                                            current.remove(date)
                                        } else {
                                            current.add(date)
                                        }
                                        selectedMultiDates.value = current
                                    } else {
                                        onDateSelected(date)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelectedInSingle || isSelectedInMulti || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelectedInMulti -> MaterialTheme.colorScheme.onTertiaryContainer
                                        isSelectedInSingle -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                // Render colored dots for assigned presets
                                if (presetsForDate.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        presetsForDate.take(3).forEach { preset ->
                                            val dotColor = parseHexColor(preset.preset.colorHex)
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Action Panel: Multi-Selection Bar OR Selected Date Details Panel
        if (isMultiSelectMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.selected_days, selectedMultiDates.value.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPresetSelectorSheet = true },
                            enabled = selectedMultiDates.value.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.assign_preset))
                        }

                        OutlinedButton(
                            onClick = { selectedMultiDates.value = emptySet() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }
        } else {
            // Selected Date Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(onClick = { showPresetSelectorSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add_set), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (assignedPresetsForSelectedDate.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_preset_assigned_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(assignedPresetsForSelectedDate) { preset ->
                                PresetCard(
                                    presetWithAlarms = preset,
                                    onDelete = {
                                        onRemovePresetFromDate(selectedDate, preset.preset.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet to Select Preset
    if (showPresetSelectorSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPresetSelectorSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isMultiSelectMode)
                        stringResource(R.string.choose_preset_for_days, selectedMultiDates.value.size)
                    else
                        stringResource(R.string.choose_preset_for_date, selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (presetsList.isEmpty()) {
                    Text(stringResource(R.string.no_presets_available))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(presetsList) { preset ->
                            PresetCard(
                                presetWithAlarms = preset,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        onAssignPresetToDates(selectedMultiDates.value.toList(), preset.preset.id)
                                        isMultiSelectMode = false
                                        selectedMultiDates.value = emptySet()
                                    } else {
                                        onAssignPresetToDate(selectedDate, preset.preset.id)
                                    }
                                    showPresetSelectorSheet = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
