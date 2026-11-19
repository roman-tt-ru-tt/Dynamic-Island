package com.akira.island.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.akira.island.core.AppLog
import com.akira.island.R
import com.akira.island.core.AlarmInfo
import com.akira.island.core.IslandBus
import com.akira.island.core.Notice
import com.akira.island.core.NoticeStyle
import com.akira.island.core.Tints
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.island.Ios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Монитор будильников.
 * Если встроенный будильник включен — показывает ТОЛЬКО его (игнорирует системный).
 * Если выключен — работает со системным AlarmManager (с компенсацией сдвига Xiaomi).
 */
class AlarmMonitor(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var pollJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = refresh(announce = true, fromPolling = false)
    }

    fun start() {
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), ContextCompat.RECEIVER_EXPORTED,
        )
        refresh(announce = false, fromPolling = false)
        
        // Опрос каждые 10 секунд для системного будильника (если встроенный выключен)
        pollJob = scope.launch {
            while (isActive) {
                delay(10_000)
                if (!SettingsRepo.current.builtInAlarmEnabled) {
                    refresh(announce = false, fromPolling = true)
                }
            }
        }
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
        pollJob?.cancel()
        IslandBus.nextAlarm.value = null
        IslandBus.builtInAlarm.value = null
    }

    private fun refresh(announce: Boolean, fromPolling: Boolean) {
        val settings = SettingsRepo.current
        
        // Если встроенный будильник включен — игнорируем системный.
        // Данные для острова приходят из BuiltInAlarmScheduler.
        if (settings.builtInAlarmEnabled) {
            return
        }

        val info = runCatching { alarms?.nextAlarmClock }
            .onFailure { AppLog.logException("AlarmTrace", it) }
            .getOrNull()

        val now = System.currentTimeMillis()
        val rawTrigger = info?.triggerTime ?: 0L
        val pkg = info?.showIntent?.creatorPackage
        val zone = TimeZone.getDefault()

        fun local(value: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
            .apply { timeZone = zone }.format(Date(value))

        fun utc(value: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(value))

        val acceptedByClockApps = pkg?.let {
            runCatching { ClockApps.isAlarmApp(context, it) }.getOrDefault(false)
        } ?: false

        // Компенсация сдвига Xiaomi/HyperOS
        val isXiaomiShift = shouldCompensateXiaomi(pkg)
        val shiftMs = if (isXiaomiShift) HOUR_MS else 0L
        var triggerAt = if (rawTrigger > 0L) rawTrigger + shiftMs else 0L

        // Защита от прошлого
        if (triggerAt > 0L && triggerAt <= now) {
            triggerAt += DAY_MS
        }

        if (!fromPolling || triggerAt != IslandBus.nextAlarm.value?.triggerAt) {
            AppLog.log(
                "AlarmTrace",
                "REFRESH announce=$announce fromPolling=$fromPolling; " +
                    "creatorPackage=$pkg; isAlarmApp=$acceptedByClockApps; " +
                    "rawTriggerLocal=${if (rawTrigger > 0L) local(rawTrigger) else "NONE"}; " +
                    "isXiaomiShift=$isXiaomiShift; shiftMs=$shiftMs; " +
                    "triggerLocal=${if (triggerAt > 0L) local(triggerAt) else "NONE"}; " +
                    "deltaMinutes=${if (triggerAt > 0L) (triggerAt - now) / 60_000 else -1}"
            )
        }

        val next = info?.takeIf { alarm ->
            alarm.showIntent?.creatorPackage?.let {
                ClockApps.isAlarmApp(context, it)
            } == true
        }?.let {
            AlarmInfo(
                triggerAt = triggerAt,
                pkg = it.showIntent?.creatorPackage,
                show = it.showIntent,
            )
        }

        val prev = IslandBus.nextAlarm.value
        IslandBus.nextAlarm.value = next

        if (next == null) return
        if (next.triggerAt <= now) return
        if (prev != null && prev.triggerAt == next.triggerAt) return

        if (!fromPolling || prev?.triggerAt != next.triggerAt) {
            if (!settings.enabled || !settings.evAlarmSet) return

            IslandBus.post(
                Notice(
                    title = tr("Будильник", "Alarm"),
                    icon = Ios.load(context, R.drawable.ios_alarm),
                    tint = Tints.Orange,
                    style = NoticeStyle.ALARM,
                    alarm = next,
                    extraMs = 2_000,
                ),
            )
        }
    }

    private fun shouldCompensateXiaomi(pkg: String?): Boolean {
        if (pkg == null) return false
        if (!SettingsRepo.current.alarmXiaomiShift) return false
        if (!Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
            && !Build.BRAND.equals("Redmi", ignoreCase = true)
            && !Build.BRAND.equals("POCO", ignoreCase = true)
        ) return false
        return pkg in XIAOMI_CLOCK_PACKAGES
    }

    companion object {
        private const val HOUR_MS = 3_600_000L
        private const val DAY_MS = 86_400_000L
        private val XIAOMI_CLOCK_PACKAGES = setOf(
            "com.android.deskclock",
            "com.miui.clock",
        )
    }
}
