package com.warbodi.smartalarmcalendar.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warbodi.smartalarmcalendar.R
import com.warbodi.smartalarmcalendar.alarm.AlarmScheduler
import com.warbodi.smartalarmcalendar.alarm.XiaomiPermissionHelper
import com.warbodi.smartalarmcalendar.data.model.PresetAlarm
import com.warbodi.smartalarmcalendar.data.model.PresetWithAlarms
import com.warbodi.smartalarmcalendar.ui.components.AlarmItemRow
import com.warbodi.smartalarmcalendar.ui.components.PresetCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    assignmentsMap: Map<String, List<Long>>,
    presetsList: List<PresetWithAlarms>,
    dismissedEarlySet: Set<String>,
    onToggleAlarm: (LocalDate, PresetAlarm, Boolean) -> Unit,
    onNavigateToCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val todayPresetIds = assignmentsMap[todayStr] ?: emptyList()
    val todayPresets = presetsList.filter { it.preset.id in todayPresetIds }
    val todayAlarms = todayPresets.flatMap { it.alarms }.sortedBy { it.hour * 60 + it.minute }

    val canScheduleExact = rememberCanScheduleExact(context)
    val canDrawOverlays = Settings.canDrawOverlays(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Card: ONLY shown if Settings.canDrawOverlays is FALSE
        if (!canDrawOverlays) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (XiaomiPermissionHelper.isXiaomiDevice())
                                stringResource(R.string.xiaomi_perm_desc)
                            else
                                stringResource(R.string.overlay_perm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (XiaomiPermissionHelper.isXiaomiDevice()) {
                                XiaomiPermissionHelper.openXiaomiPermissionSettings(context)
                            } else {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.configure_lockscreen_perm))
                    }
                }
            }
        }

        if (!canScheduleExact) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.exact_alarm_perm_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (todayPresets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.no_preset_assigned_today),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.no_preset_today_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = onNavigateToCalendar) {
                        Text(stringResource(R.string.open_calendar_button))
                    }
                }
            }
        } else {
            Text(
                text = stringResource(R.string.today_preset_label, todayPresets.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                todayPresets.forEach { preset ->
                    PresetCard(presetWithAlarms = preset)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.today_alarms_label, todayAlarms.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (todayAlarms.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_active_alarms_today),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                data class DisplayAlarmState(
                    val alarm: PresetAlarm,
                    val isEffectiveEnabled: Boolean,
                    val statusLabel: String?,
                    val isInteractive: Boolean
                )

                val alreadyRungLabel = stringResource(R.string.already_rung)
                val dismissedEarlyLabel = stringResource(R.string.dismissed_early)

                val now = remember { java.time.LocalDateTime.now() }
                val displayAlarmStates = remember(todayAlarms, todayStr, dismissedEarlySet, now) {
                    todayAlarms.map { alarm ->
                        val key = "${todayStr}_${alarm.id}"
                        val isDismissed = dismissedEarlySet.contains(key)
                        val alarmDateTime = java.time.LocalDateTime.of(today, java.time.LocalTime.of(alarm.hour, alarm.minute))
                        val isPassed = alarmDateTime.isBefore(now)

                        when {
                            isPassed -> DisplayAlarmState(
                                alarm = alarm,
                                isEffectiveEnabled = false,
                                statusLabel = alreadyRungLabel,
                                isInteractive = false
                            )
                            isDismissed || !alarm.isEnabled -> DisplayAlarmState(
                                alarm = alarm,
                                isEffectiveEnabled = false,
                                statusLabel = dismissedEarlyLabel,
                                isInteractive = true
                            )
                            else -> DisplayAlarmState(
                                alarm = alarm,
                                isEffectiveEnabled = true,
                                statusLabel = null,
                                isInteractive = true
                            )
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayAlarmStates) { state ->
                        AlarmItemRow(
                            alarm = state.alarm.copy(isEnabled = state.isEffectiveEnabled),
                            statusLabel = state.statusLabel,
                            isInteractive = state.isInteractive,
                            onToggle = { isChecked ->
                                onToggleAlarm(today, state.alarm, isChecked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberCanScheduleExact(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AlarmScheduler.canScheduleExactAlarms(context)
    } else {
        true
    }
}
