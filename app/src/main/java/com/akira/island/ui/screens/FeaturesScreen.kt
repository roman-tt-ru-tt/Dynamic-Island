package com.akira.island.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.island.core.Lang
import com.akira.island.core.Tints
import com.akira.island.core.tr
import com.akira.island.data.AccentMode
import com.akira.island.data.AppLanguage
import com.akira.island.data.IslandSettings
import com.akira.island.data.SettingsRepo
import com.akira.island.island.FitText
import com.akira.island.island.onAccent
import com.akira.island.island.resolveAccent
import com.akira.island.ui.ChoiceDialog
import com.akira.island.ui.Group
import com.akira.island.ui.GroupItem
import com.akira.island.ui.GroupSlider
import com.akira.island.ui.GroupToggle
import com.akira.island.ui.Page
import com.akira.island.ui.Pos
import com.akira.island.ui.Screen
import com.akira.island.ui.SectionHeader
import com.akira.island.ui.Tile
import com.akira.island.ui.Ui

@Composable
fun FeaturesScreen(open: (Screen) -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }

    Page(tr("Функции", "Features")) {
        SectionHeader(tr("Что показывает остров", "What the island shows"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.Dashboard, tr("Динамические карточки", "Dynamic cards"),
                tr("Музыка, звонки, таймеры, фонарик, навигация", "Music, calls, timers, flashlight, navigation"),
                color = Tile.Purple, onClick = { open(Screen.CARDS) }) { Arrow() }
            GroupItem(Pos.Middle, Icons.Rounded.ChatBubble, tr("Уведомления", "Notifications"),
                tr("Сообщения приложений с ответом прямо в острове", "App messages with replies right in the island"),
                color = Tile.Blue, onClick = { open(Screen.NOTIFICATIONS) }) { Arrow() }
            GroupItem(Pos.Middle, Icons.Rounded.NotificationsActive, tr("События", "Events"),
                tr("Зарядка, Wi-Fi, наушники, оплата, будильник", "Charging, Wi-Fi, headphones, payments, alarm"),
                color = Tile.Orange, onClick = { open(Screen.EVENTS) }) { Arrow() }
            GroupItem(Pos.Last, Icons.Rounded.LibraryMusic, tr("Текст песен", "Lyrics"),
                tr("Синхронный текст в раскрытом плеере", "Synced lyrics in the expanded player"),
                color = Tile.Pink, onClick = { open(Screen.LYRICS) }) { Arrow() }
        }

        SectionHeader(tr("Управление", "Controls"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.Gesture, tr("Жесты", "Gestures"), tr("Нажатия и свайпы по капсуле", "Taps and swipes on the capsule"),
                color = Tile.Teal, onClick = { open(Screen.GESTURES) }) { Arrow() }
            GroupItem(Pos.Middle, Icons.Rounded.Tune, tr("Поведение", "Behavior"),
                tr("Когда показывать и сворачивать остров, виброотклик", "When to show and collapse the island, haptics"),
                color = Tile.Indigo, onClick = { open(Screen.BEHAVIOR) }) { Arrow() }
            GroupItem(Pos.Last, Icons.Rounded.NotificationsOff, tr("Системные плашки", "System banners"),
                tr("Чтобы звонок, таймер и батарею показывал только остров", "So only the island shows calls, timers and battery"),
                color = Tile.Red, onClick = { open(Screen.BANNERS) }) { Arrow() }
        }

        SectionHeader(tr("Общие", "General"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.Language, if (Lang.en) "Language" else "Язык · Language", s.language.title,
                color = Tile.Blue, onClick = { languageDialog = true }) { Arrow() }
            GroupItem(Pos.Last, Icons.Rounded.Restore, tr("Сбросить все настройки", "Reset all settings"),
                tr("Вернуть всё как было после установки", "Back to how it was after installing"),
                onClick = { confirmReset = true }, color = Tile.Gray)
        }
    }

    if (languageDialog) {
        ChoiceDialog(if (Lang.en) "Language" else "Язык · Language", AppLanguage.entries, s.language, { it.title },
            { v -> SettingsRepo.update { copy(language = v) } }) { languageDialog = false }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Ui.Card,
            title = { Text(tr("Сбросить настройки?", "Reset settings?")) },
            text = {
                Text(tr("Размер, цвета, анимации, карточки и события вернутся к значениям по умолчанию.",
                    "Size, colors, animations, cards and events go back to their defaults."))
            },
            confirmButton = {
                TextButton(onClick = {
                    SettingsRepo.reset()
                    confirmReset = false
                }) { Text(tr("Сбросить", "Reset"), color = Ui.Primary) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(tr("Отмена", "Cancel"), color = Ui.Sub) } },
        )
    }
}
