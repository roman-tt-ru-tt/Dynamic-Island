package com.akira.island.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.akira.island.core.AppLog
import com.akira.island.core.tr
import com.akira.island.ui.Page
import com.akira.island.ui.Tile
import com.akira.island.ui.Ui

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lines by AppLog.lines.collectAsState()

    Page(tr("Логи", "Logs"), onBack,
        subtitle = tr("Последние ${lines.size} записей. Нажмите «Скачать», чтобы отправить разработчику.",
            "Last ${lines.size} entries. Tap “Download” to send to the developer."),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(tr("Скачать", "Download"), Icons.Rounded.Download, Tile.Green) {
                val file = AppLog.file() ?: return@ActionButton
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, tr("Поделиться логами", "Share logs")))
                }
            }
            ActionButton(tr("Очистить", "Clear"), Icons.Rounded.Delete, Tile.Red) {
                AppLog.clear()
            }
        }

        Spacer(Modifier.height(12.dp))

        if (lines.isEmpty()) {
            Text(
                tr("Логов пока нет. Сделайте что-нибудь в приложении — записи появятся здесь.",
                    "No logs yet. Do something in the app — entries will appear here."),
                color = Ui.Sub, fontSize = 14.sp, modifier = Modifier.padding(16.dp),
            )
        } else {
            // Оборачиваем в Column с verticalScroll, чтобы LazyColumn не падал
            Column(Modifier.verticalScroll(rememberScrollState())) {
                lines.forEach { line ->
                    Text(
                        line,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ui.Card)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
