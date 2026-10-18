package com.warbodi.smartalarmcalendar.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.warbodi.smartalarmcalendar.ui.screens.CalendarScreen
import com.warbodi.smartalarmcalendar.ui.screens.EditPresetScreen
import com.warbodi.smartalarmcalendar.ui.screens.PresetsScreen
import com.warbodi.smartalarmcalendar.ui.screens.SettingsScreen
import com.warbodi.smartalarmcalendar.ui.screens.TodayScreen
import com.warbodi.smartalarmcalendar.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    var isEditingPreset by remember { mutableStateOf(false) }

    val presetsList by viewModel.presetsWithAlarms.collectAsState()
    val assignmentsMap by viewModel.calendarAssignmentsMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val editingPreset by viewModel.editingPreset.collectAsState()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        viewModel.refreshAlarms()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    androidx.activity.compose.BackHandler(enabled = isEditingPreset || editingPreset != null) {
        viewModel.setEditingPreset(null)
        isEditingPreset = false
    }

    if (isEditingPreset || editingPreset != null) {
        EditPresetScreen(
            editingPreset = editingPreset,
            onSave = { preset, alarms ->
                viewModel.savePreset(preset, alarms)
                isEditingPreset = false
            },
            onBack = {
                viewModel.setEditingPreset(null)
                isEditingPreset = false
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_calendar)) },
                        label = { Text(androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_calendar)) }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_presets)) },
                        label = { Text(androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_presets)) }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        icon = { Icon(Icons.Default.Today, contentDescription = androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_today)) },
                        label = { Text(androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_today)) }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_settings)) },
                        label = { Text(androidx.compose.ui.res.stringResource(com.warbodi.smartalarmcalendar.R.string.tab_settings)) }
                    )
                }
            }
        ) { paddingValues ->
            val modifier = Modifier.padding(paddingValues)

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 3,
                modifier = modifier
            ) { page ->
                when (page) {
                    0 -> CalendarScreen(
                        selectedDate = selectedDate,
                        assignmentsMap = assignmentsMap,
                        presetsList = presetsList,
                        onDateSelected = { date -> viewModel.setSelectedDate(date) },
                        onAssignPresetToDate = { date, presetId -> viewModel.assignPresetToDate(date, presetId) },
                        onAssignPresetToDates = { dates, presetId -> viewModel.assignPresetToDates(dates, presetId) },
                        onRemovePresetFromDate = { date, presetId -> viewModel.removePresetFromDate(date, presetId) }
                    )
                    1 -> PresetsScreen(
                        presetsList = presetsList,
                        onCreateNewPreset = {
                            viewModel.setEditingPreset(null)
                            isEditingPreset = true
                        },
                        onTestPreset = {
                            viewModel.createAndAssignTestPreset()
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onEditPreset = { preset ->
                            viewModel.setEditingPreset(preset)
                            isEditingPreset = true
                        },
                        onDeletePreset = { preset ->
                            viewModel.deletePreset(preset.preset)
                        }
                    )
                    2 -> TodayScreen(
                        assignmentsMap = assignmentsMap,
                        presetsList = presetsList,
                        dismissedEarlySet = viewModel.dismissedEarlySet.collectAsState().value,
                        onToggleAlarm = { date, alarm, isEnabled ->
                            viewModel.toggleAlarmState(date, alarm, isEnabled)
                        },
                        onNavigateToCalendar = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                    3 -> SettingsScreen(
                        onThemeModeChanged = { mode -> viewModel.setAppThemeMode(mode) }
                    )
                }
            }
        }
    }
}
