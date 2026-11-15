package com.akira.island.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.akira.island.core.tr
import com.akira.island.data.IslandSettings
import com.akira.island.data.QuickAction
import com.akira.island.data.SettingsRepo
import com.akira.island.ui.ChoiceDialog
import com.akira.island.ui.Group
import com.akira.island.ui.GroupItem
import com.akira.island.ui.GroupSlider
import com.akira.island.ui.GroupToggle
import com.akira.island.ui.Page
import com.akira.island.ui.Pos
import com.akira.island.ui.SectionHeader
import com.akira.island.ui.Tile

@Composable
fun BehaviorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s by SettingsRepo.state.collectAsState()
    var collapseDialog by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    Page(tr("Поведение", "Behavior"), onBack) {
        SectionHeader(tr("Когда показывать", "When to show"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.VisibilityOff, tr("Скрывать пустую капсулу", "Hide the empty capsule"),
                tr("Показывать капсулу, только когда в ней что-то есть", "Show the capsule only when it has something in it"),
                s.hideIdle, color = Tile.Gray) { v -> SettingsRepo.update { copy(hideIdle = v) } }
            GroupToggle(Pos.Middle, Icons.Rounded.ScreenRotation, tr("Скрывать в альбомной ориентации", "Hide in landscape"),
                tr("Не мешает играм и видео", "Stays out of games and videos"),
                s.hideInLandscape, color = Tile.Gray) { v -> SettingsRepo.update { copy(hideInLandscape = v) } }
            GroupToggle(Pos.Middle, Icons.Rounded.ScreenLockRotation, tr("Не переворачивать остров", "Keep island upright"),
                tr("Остров останется сверху, даже если экран повернулся горизонтально",
                    "The island stays at the top even if the screen rotates horizontally"),
                s.keepUpright, color = Tile.Indigo) { v -> SettingsRepo.update { copy(keepUpright = v) } }
            GroupToggle(Pos.Last, Icons.Rounded.Apps, tr("Прятать плеер в его приложении", "Hide the player in its own app"),
                tr("Карточка музыки не показывается, пока открыт сам плеер", "The music card hides while the player itself is open"),
                s.hideWhenPlayerOpen, color = Tile.Blue) { v -> SettingsRepo.update { copy(hideWhenPlayerOpen = v) } }
        }

        SectionHeader(tr("Расписание", "Schedule"))
        Group {
            GroupToggle(if (s.scheduleOn) Pos.First else Pos.Single, Icons.Rounded.Schedule, tr("По расписанию", "On a schedule"),
                tr("Остров виден только с ${hourLabel(s.scheduleFrom)} до ${hourLabel(s.scheduleTo)}",
                    "The island shows only from ${hourLabel(s.scheduleFrom)} to ${hourLabel(s.scheduleTo)}"),
                s.scheduleOn, color = Tile.Indigo) { v -> SettingsRepo.update { copy(scheduleOn = v) } }
            if (s.scheduleOn) {
                GroupSlider(Pos.Middle, Icons.Rounded.WbSunny, tr("С", "From"), s.scheduleFrom.toFloat(), 0f..23f,
                    hourLabel(s.scheduleFrom), steps = 22, color = Tile.Orange) { v -> SettingsRepo.update { copy(scheduleFrom = v.toInt()) } }
                GroupSlider(Pos.Last, Icons.Rounded.Bedtime, tr("До", "To"), s.scheduleTo.toFloat(), 0f..23f,
                    hourLabel(s.scheduleTo), steps = 22, color = Tile.Purple) { v -> SettingsRepo.update { copy(scheduleTo = v.toInt()) } }
            }
        }

        SectionHeader(tr("Раскрытый остров", "Expanded island"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.UnfoldLess, tr("Сворачивать раскрытый остров", "Collapse the expanded island"), collapseLabel(s.autoCollapseSec),
                color = Tile.Indigo, onClick = { collapseDialog = true })
            GroupToggle(Pos.Last, Icons.Rounded.Circle, tr("Вторая активность рядом", "Second activity alongside"),
                tr("Когда активностей две, вторая — кружком рядом с капсулой, как на iPhone",
                    "With two activities, the second one is a small circle next to the capsule, like on iPhone"),
                s.bubble, color = Tile.Purple) { v ->
                SettingsRepo.update { copy(bubble = v) }
            }
        }

        SectionHeader(tr("Пустой остров по нажатию", "Tapping the empty island"))
        Group {
            GroupToggle(if (s.idleExpand) Pos.First else Pos.Single, Icons.Rounded.UnfoldMore, tr("Раскрывать", "Expand"),
                tr("Часы, заряд и кнопки ниже. Выключено — нажатие ничего не делает",
                    "Clock, battery and the buttons below. Off — tapping does nothing"),
                s.idleExpand, color = Tile.Blue) { v -> SettingsRepo.update { copy(idleExpand = v) } }
            if (s.idleExpand) {
                // Стандартные действия
                QuickAction.entries.forEachIndexed { i, a ->
                    GroupToggle(if (i == QuickAction.entries.lastIndex && s.customQuickApps.isEmpty()) Pos.Last else Pos.Middle, a.settingsIcon, a.title,
                        a.hint, a in s.quickActions, color = Tile.Orange) { v ->
                        SettingsRepo.update { copy(quickActions = QuickAction.entries.filter { if (it == a) v else it in quickActions }) }
                    }
                }
                
                // Пользовательские приложения
                if (s.customQuickApps.isNotEmpty()) {
                    s.customQuickApps.forEachIndexed { i, pkg ->
                        val appName = remember(pkg) {
                            runCatching {
                                val pm = context.packageManager
                                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                            }.getOrDefault(pkg)
                        }
                        
                        GroupItem(
                            if (i == s.customQuickApps.lastIndex) Pos.Last else Pos.Middle,
                            Icons.Rounded.Apps,
                            appName,
                            tr("Открыть приложение из острова", "Open app from the island"),
                            color = Tile.Blue,
                            onClick = { SettingsRepo.update { copy(customQuickApps = customQuickApps - pkg) } }
                        ) {
                            Icon(Icons.Rounded.Close, null, tint = Tile.Red)
                        }
                    }
                }

                // Кнопка добавления
                GroupItem(
                    if (s.customQuickApps.isEmpty()) Pos.Last else Pos.Middle,
                    Icons.Rounded.Add,
                    tr("Добавить приложение", "Add app"),
                    tr("TikTok, YouTube и другие", "TikTok, YouTube, etc."),
                    color = Tile.Green,
                    onClick = { showAppPicker = true }
                )
            }
        }

        SectionHeader(tr("Отклик", "Feedback"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Vibration, tr("Виброотклик", "Haptics"),
                tr("При открытии, закрытии и нажатиях на капсулу", "When opening, closing and tapping the capsule"),
                s.haptics, color = Tile.Pink) { v -> SettingsRepo.update { copy(haptics = v) } }
            GroupToggle(Pos.Last, Icons.Rounded.LightMode, tr("Включать экран", "Turn on the screen"),
                tr("Когда звонит таймер, будильник или входящий вызов — экран включается и не гаснет, пока звонит",
                    "When a timer, alarm or incoming call rings, the screen turns on and stays on while it rings"),
                s.wakeForAlerts, color = Tile.Yellow) { v -> SettingsRepo.update { copy(wakeForAlerts = v) } }
        }
    }

    if (collapseDialog) {
        ChoiceDialog(tr("Сворачивать раскрытый остров", "Collapse the expanded island"), listOf(0, 5, 10, 20, 30), s.autoCollapseSec, ::collapseLabel,
            { v -> SettingsRepo.update { copy(autoCollapseSec = v) } }) { collapseDialog = false }
    }

    if (showAppPicker) {
        AppPickerDialog(
            installed = s.customQuickApps,
            onPick = { pkg ->
                if (!s.customQuickApps.contains(pkg)) {
                    SettingsRepo.update { copy(customQuickApps = customQuickApps + pkg) }
                }
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
private fun AppPickerDialog(
    installed: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    
    // Получаем список всех приложений с launcher-активностью
    val apps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName } // Исключаем само приложение
            .sortedBy { pkg ->
                runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString().lowercase()
                }.getOrDefault(pkg)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Выберите приложение", "Choose an app")) },
        text = {
            if (apps.isEmpty()) {
                Text(tr("Нет установленных приложений", "No apps installed"))
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(apps) { pkg ->
                        val label = remember(pkg) {
                            runCatching {
                                val pm = context.packageManager
                                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                            }.getOrDefault(pkg)
                        }
                        val icon = remember(pkg) {
                            runCatching {
                                context.packageManager.getApplicationIcon(pkg).toBitmap(48, 48).asImageBitmap()
                            }.getOrNull()
                        }
                        val added = pkg in installed
                        
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !added) { onPick(pkg) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (icon != null) {
                                Image(icon, null, Modifier.size(36.dp).clip(CircleShape))
                            } else {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(Tile.Blue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                label,
                                color = if (added) Color.Gray else Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                            )
                            if (added) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Rounded.Close, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Закрыть", "Close"))
            }
        }
    )
}

private fun hourLabel(h: Int) = "%d:00".format(h)

private fun collapseLabel(sec: Int) = if (sec <= 0) tr("Только касанием мимо или свайпом вверх", "Only by tapping outside or swiping up")
    else tr("Через $sec секунд без касаний", "After $sec seconds without a touch")

private val QuickAction.settingsIcon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        QuickAction.TIMER -> Icons.Rounded.HourglassBottom
        QuickAction.STOPWATCH -> Icons.Rounded.Timer
        QuickAction.FLASHLIGHT -> Icons.Rounded.FlashlightOn
        QuickAction.SCREENSHOT -> Icons.Rounded.Screenshot
        QuickAction.CAMERA -> Icons.Rounded.PhotoCamera
        QuickAction.LOCK -> Icons.Rounded.Lock
    }

private val QuickAction.hint: String?
    get() = when (this) {
        QuickAction.SCREENSHOT -> tr("Остров на миг прячется — на снимке его нет (Android 9+)",
            "The island hides for a moment, so it's not in the shot (Android 9+)")
        QuickAction.LOCK -> tr("Гасит экран, как кнопка питания (Android 9+)", "Turns the screen off like the power button (Android 9+)")
        else -> null
    }
