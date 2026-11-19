package com.akira.island.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.akira.island.core.AlarmInfo
import com.akira.island.core.IslandBus
import com.akira.island.data.SettingsRepo
import java.util.Calendar
import java.util.TimeZone

object BuiltInAlarmScheduler {
    private const val REQUEST_CODE = 4242
    const val ACTION_BUILT_IN_ALARM = "com.akira.island.BUILT_IN_ALARM"

    fun schedule(context: Context) {
        val s = SettingsRepo.current
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = getPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)

        if (!s.builtInAlarmEnabled) {
            am.cancel(pi)
            updateBus(s.builtInAlarmNextAt, false)
            return
        }

        val nextAt = calculateNextAlarm(s.builtInAlarmHour, s.builtInAlarmMinute, s.builtInAlarmDays)
        
        // Сохраняем время в настройки, чтобы остров его видел
        SettingsRepo.update { copy(builtInAlarmNextAt = nextAt) }
        updateBus(nextAt, true)

        // Используем setAlarmClock — он самый надежный и показывает иконку будильника в статус-баре
        val info = AlarmManager.AlarmClockInfo(nextAt, null)
        am.setAlarmClock(info, pi)
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = getPendingIntent(context, PendingIntent.FLAG_NO_CREATE) ?: return
        am.cancel(pi)
    }

    private fun getPendingIntent(context: Context, flags: Int): PendingIntent {
        val intent = Intent(context, BuiltInAlarmReceiver::class.java).apply {
            action = ACTION_BUILT_IN_ALARM
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateBus(triggerAt: Long, enabled: Boolean) {
        // Если будильник выключен, шлем null, иначе — время
        IslandBus.builtInAlarm.value = if (enabled && triggerAt > 0) {
            AlarmInfo(triggerAt, null, null, isBuiltIn = true)
        } else null
    }

    private fun calculateNextAlarm(hour: Int, minute: Int, days: List<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Если дни не выбраны — однократно
        if (days.isEmpty()) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Если выбраны дни — ищем ближайший
        for (i in 0..7) {
            val dayOfWeek = target.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            val isoDay = if (dayOfWeek == 1) 7 else dayOfWeek - 1 // 1=Mon, 7=Sun
            if (days.contains(isoDay) && target.timeInMillis > now.timeInMillis) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
