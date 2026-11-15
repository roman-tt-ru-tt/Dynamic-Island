package com.akira.island.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.ui.screens.restrictedHelp

class OrientationTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Принудительно горизонтальная ориентация для теста
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Не гасим экран
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            IslandTheme {
                OrientationTestScreen(onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun OrientationTestScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) } // 0 - вопрос, 1 - да, 2 - нет
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0D16)),
        contentAlignment = Alignment.Center,
    ) {
        when (step) {
            0 -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    tr("Проверка острова", "Island check"),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    tr("Сейчас экран повернётся горизонтально. Посмотрите наверх — видите ли вы остров около камеры?",
                        "The screen will now rotate horizontally. Look at the top — do you see the island near the camera?"),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { step = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(tr("Да", "Yes"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { step = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(tr("Нет", "No"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            1 -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    tr("Отлично! 🎉", "Great! 🎉"),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    tr("Остров работает в горизонтальном режиме. Сохранена настройка «Не переворачивать».",
                        "The island works in landscape mode. The \"Keep upright\" setting is saved."),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        SettingsRepo.update { copy(keepUpright = true, hideInLandscape = false) }
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(tr("Готово", "Done"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            2 -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    tr("Остров не виден", "Island not visible"),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    tr("Скорее всего, ваша прошивка скрывает окна службы спец. возможностей в горизонтальном режиме.\n\n" +
                        "Попробуйте:\n" +
                        "1. Отключить оптимизацию батареи для острова\n" +
                        "2. Разрешить острову работать в фоне\n" +
                        "3. Закрепить остров в недавних приложениях\n\n" +
                        "Если не помогло — эта функция может не работать на вашем телефоне.",
                        "Your firmware likely hides accessibility service windows in landscape mode.\n\n" +
                            "Try:\n" +
                            "1. Disable battery optimization for the island\n" +
                            "2. Allow the island to run in the background\n" +
                            "3. Lock the island in recent apps\n\n" +
                            "If that doesn't help — this feature may not work on your phone."),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            Perms.openBattery(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(tr("Батарея", "Battery"), color = Color.White, fontSize = 16.sp)
                    }
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E8E93)),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(tr("Закрыть", "Close"), color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
