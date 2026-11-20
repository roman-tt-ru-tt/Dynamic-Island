package com.akira.island.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.service.BuiltInAlarmScheduler
import com.akira.island.ui.Page
import com.akira.island.ui.SectionHeader
import com.akira.island.ui.Ui
import java.util.Calendar

@Composable
fun AlarmScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    
    // Состояния для выбора времени
    val timeState = rememberTimePickerState(
        initialHour = s.builtInAlarmHour,
        initialMinute = s.builtInAlarmMinute,
        is24Hour = true
    )
    
    var selectedDays by remember { mutableStateOf(s.builtInAlarmDays) }

    Page(tr("Будильник", "Alarm")) {
        SectionHeader(tr("Встроенный будильник", "Built-in alarm"))

        // Включение/выключение
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                tr("Включить будильник", "Enable alarm"),
                fontSize = 17.sp,
                color = Ui.Text
            )
            Switch(
                checked = s.builtInAlarmEnabled,
                onCheckedChange = { enabled ->
                    SettingsRepo.update { copy(builtInAlarmEnabled = enabled) }
                    BuiltInAlarmScheduler.schedule(com.akira.island.ui.MainActivity.getInstance() ?: return@Switch)
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Выбор времени
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TimePicker(state = timeState)
        }

        Spacer(Modifier.height(20.dp))

        // Выбор дней недели
        SectionHeader(tr("Дни недели", "Days of the week"))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val days = listOf(
                1 to tr("Пн", "Mon"),
                2 to tr("Вт", "Tue"),
                3 to tr("Ср", "Wed"),
                4 to tr("Чт", "Thu"),
                5 to tr("Пт", "Fri"),
                6 to tr("Сб", "Sat"),
                7 to tr("Вс", "Sun")
            )
            days.forEach { (dayNum, label) ->
                val selected = selectedDays.contains(dayNum)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selected) Ui.Primary else Ui.Card)
                        .clickable {
                            selectedDays = if (selected) selectedDays - dayNum else selectedDays + dayNum
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else Ui.Sub,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // Кнопка сохранения
        Button(
            onClick = {
                SettingsRepo.update {
                    copy(
                        builtInAlarmHour = timeState.hour,
                        builtInAlarmMinute = timeState.minute,
                        builtInAlarmDays = selectedDays,
                        builtInAlarmNextAt = 0L // Пересчитаем
                    )
                }
                val ctx = com.akira.island.ui.MainActivity.getInstance()
                if (ctx != null) {
                    BuiltInAlarmScheduler.schedule(ctx)
                }
                onBack()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ui.Primary)
        ) {
            Text(tr("Сохранить", "Save"), fontSize = 17.sp)
        }

        Spacer(Modifier.height(20.dp))
    }
}
