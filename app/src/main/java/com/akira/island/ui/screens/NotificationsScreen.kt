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
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.akira.island.core.Tints
import com.akira.island.core.tr
import com.akira.island.data.AccentMode
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
import com.akira.island.ui.SectionHeader
import com.akira.island.ui.Tile

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var accentDialog by remember { mutableStateOf(false) }

    Page(tr("Уведомления", "Notifications"), onBack) {
        Group {
            GroupToggle(Pos.First, Icons.Rounded.ChatBubble, tr("Уведомления приложений", "App notifications"),
                tr("Сообщения из Telegram, WhatsApp, почты и других приложений — карточкой в острове, как на iPhone",
                    "Messages from Telegram, WhatsApp, email and other apps as a card in the island, like on iPhone"),
                s.cardMessages, color = Tile.Blue) { v -> SettingsRepo.update { copy(cardMessages = v) } }
            
            // Новый переключатель
            GroupToggle(Pos.Middle, Icons.Rounded.VisibilityOff, tr("Показывать в острове", "Show in island"),
                tr("Если выключено — уведомления приходят только в шторку, а остров их не показывает",
                    "If off, notifications go to the shade only, not to the island"),
                s.msgInIsland, color = Tile.Gray) { v -> SettingsRepo.update { copy(msgInIsland = v) } }
            
            GroupToggle(Pos.Middle, Icons.AutoMirrored.Rounded.Reply, tr("Кнопки в уведомлениях", "Notification buttons"),
                tr("«Ответить» с вводом текста прямо в острове и «Пометить прочитанным»", "Reply with typing right in the island, and Mark as read"),
                s.msgActions, color = Tile.Indigo) { v -> SettingsRepo.update { copy(msgActions = v) } }
            GroupToggle(Pos.Middle, Icons.Rounded.Smartphone, tr("Из открытого приложения", "From the open app"),
                tr("Показывать и уведомления приложения, которое сейчас на экране", "Also show notifications from the app that's on screen"),
                s.msgInApp, color = Tile.Teal) { v ->
                SettingsRepo.update { copy(msgInApp = v) }
            }
            GroupSlider(
                Pos.Last, Icons.Rounded.FormatLineSpacing, tr("Строк текста", "Text lines"),
                value = s.msgLines.toFloat(), range = 1f..6f, steps = 4, label = "${s.msgLines}", color = Tile.Green,
            ) { v -> SettingsRepo.update { copy(msgLines = Math.round(v)) } }
        }

        SectionHeader(tr("Цвет кнопок", "Button color"))
        AccentPreview(s)
        GroupItem(Pos.Single, Icons.Rounded.Palette, tr("Цвет кнопок", "Button color"), "${s.accentMode.title} — ${s.accentMode.hint}",
            onClick = { accentDialog = true }, color = Tile.Orange)
        if (s.accentMode == AccentMode.CUSTOM) {
            Swatches(accentColors, s.accentColor) { c -> SettingsRepo.update { copy(accentColor = c) } }
        }
    }

    if (accentDialog) {
        ChoiceDialog(tr("Цвет кнопок", "Button color"), AccentMode.entries, s.accentMode, { "${it.title} — ${it.hint}" },
            { v -> SettingsRepo.update { copy(accentMode = v) } }) { accentDialog = false }
    }
}

@Composable
private fun AccentPreview(s: com.akira.island.data.IslandSettings) {
    val context = LocalContext.current
    val accent = resolveAccent(context, s, Tints.Orange)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.Black).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewTile(tr("Ответить", "Reply"), accent, Modifier.weight(1f))
        PreviewTile(tr("Пометить прочитанным", "Mark as read"), accent, Modifier.weight(2f))
        Box(Modifier.size(40.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowUpward, null, tint = onAccent(accent), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PreviewTile(title: String, accent: Color, modifier: Modifier) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        FitText(title, accent, 14.sp, 10.sp, FontWeight.SemiBold)
    }
}

private val accentColors = listOf(
    0xFF0A84FF, 0xFFC6C3F7, 0xFF64D2FF, 0xFF34C759, 0xFFFFD60A, 0xFFFF9F0A,
    0xFFFF453A, 0xFFFF2D92, 0xFFBF5AF2, 0xFFFFFFFF,
)
