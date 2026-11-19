package com.akira.island.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.akira.island.R

class BuiltInAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BuiltInAlarmScheduler.ACTION_BUILT_IN_ALARM) return

        // 1. Вибрация
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0)) // Ждем 0, вибрируем 1000мс, пауза 500мс, повтор

        // 2. Звук (пока просто играем, потом прикрутим MediaPlayer)
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmSound)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Уведомление
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "built_in_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Встроенный будильник",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Звонок встроенного будильника"
                enableVibration(true)
                setSound(null, null) // Звук играет сам через Ringtone
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ios_alarm)
            .setContentTitle("Пора вставать!")
            .setContentText("Нажмите, чтобы выключить")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        nm.notify(4242, notification)
        
        // В реальном приложении здесь нужно запускать сервис для остановки звука по кнопке
        // Пока просто покажем тост для проверки
        Toast.makeText(context, "БУДИЛЬНИК ЗВОНИТ!", Toast.LENGTH_LONG).show()
    }
}
