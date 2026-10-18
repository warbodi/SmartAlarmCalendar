package com.example.smartalarmcalendar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartalarmcalendar.R
import com.example.smartalarmcalendar.data.model.Preset
import com.example.smartalarmcalendar.data.model.PresetAlarm
import com.example.smartalarmcalendar.data.model.PresetWithAlarms
import com.example.smartalarmcalendar.ui.components.AlarmItemRow
import com.example.smartalarmcalendar.ui.components.ColorPickerRow
import com.example.smartalarmcalendar.ui.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPresetScreen(
    editingPreset: PresetWithAlarms?,
    onSave: (Preset, List<PresetAlarm>) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(editingPreset?.preset?.name ?: "") }
    var description by remember { mutableStateOf(editingPreset?.preset?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(editingPreset?.preset?.colorHex ?: "#3F51B5") }

    val alarms = remember {
        mutableStateListOf<PresetAlarm>().apply {
            if (editingPreset != null) {
                addAll(editingPreset.alarms)
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlarmIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingPreset == null) stringResource(R.string.new_preset_title_screen) else stringResource(R.string.edit_preset_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                val preset = Preset(
                                    id = editingPreset?.preset?.id ?: 0L,
                                    name = name,
                                    colorHex = selectedColorHex,
                                    description = description
                                )
                                onSave(preset, alarms)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.preset_desc_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.color_label),
                style = MaterialTheme.typography.labelLarge
            )

            ColorPickerRow(
                selectedHex = selectedColorHex,
                onColorSelected = { selectedColorHex = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.alarms_in_preset),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        editingAlarmIndex = -1
                        showTimePicker = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_alarm_button))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (alarms.isEmpty()) {
                Text(
                    text = stringResource(R.string.alarms_in_preset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(alarms) { index, alarm ->
                        AlarmItemRow(
                            alarm = alarm,
                            onClick = {
                                editingAlarmIndex = index
                                showTimePicker = true
                            },
                            onDelete = {
                                alarms.removeAt(index)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val preset = Preset(
                            id = editingPreset?.preset?.id ?: 0L,
                            name = name,
                            colorHex = selectedColorHex,
                            description = description
                        )
                        onSave(preset, alarms)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_preset_button))
            }
        }
    }

    if (showTimePicker) {
        val initialHour = if (editingAlarmIndex in alarms.indices) alarms[editingAlarmIndex].hour else 7
        val initialMinute = if (editingAlarmIndex in alarms.indices) alarms[editingAlarmIndex].minute else 0

        TimePickerDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                showTimePicker = false
                if (editingAlarmIndex in alarms.indices) {
                    val current = alarms[editingAlarmIndex]
                    alarms[editingAlarmIndex] = current.copy(hour = hour, minute = minute)
                } else {
                    alarms.add(PresetAlarm(hour = hour, minute = minute))
                }
                alarms.sortBy { it.hour * 60 + it.minute }
            }
        )
    }
}
