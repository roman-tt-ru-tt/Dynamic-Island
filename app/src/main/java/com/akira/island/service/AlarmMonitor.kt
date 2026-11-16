package com.akira.island.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.util.Log
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import com.akira.island.R
import com.akira.island.core.AlarmInfo
import com.akira.island.core.IslandBus
import com.akira.island.core.Notice
import com.akira.island.core.NoticeStyle
import com.akira.island.core.Tints
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.island.Ios

/**
 * Ближайший будильник. Его знает сама система (AlarmManager.getNextAlarmClock) — уведомления «Часов»
 * для этого не нужны. Поставили новый будильник — остров показывает плашку со временем подъёма;
 * следующий по расписанию (прошлый прозвенел или отложен) — молча.
 * Будильником системы пользуются и календари, напоминалки, трекеры сна — их «будильник» человек не ставил,
 * поэтому берём только поставленные приложением-будильником («Часы», Alarmy и т. п.).
 */
class AlarmMonitor(private val context: Context) {
    companion object { private const val TAG = "DynamicIslandAlarm" }
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) = refresh(announce = true)
    }

    fun start() {
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter().apply {
                addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
            }, ContextCompat.RECEIVER_EXPORTED,
        )
        refresh(announce = false)
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
        IslandBus.nextAlarm.value = null
    }

    private fun refresh(announce: Boolean) {
        val info = runCatching { alarms?.nextAlarmClock }.onFailure {
            Log.e(TAG, "Failed to read AlarmManager.nextAlarmClock", it)
        }.getOrNull()
        val now = System.currentTimeMillis()
        val trigger = info?.triggerTime
        val pkg = info?.showIntent?.creatorPackage
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        Log.i(TAG, "nextAlarm package=$pkg triggerEpoch=$trigger " +
            "triggerLocal=${trigger?.let { formatter.format(Date(it)) }} " +
            "nowLocal=${formatter.format(Date(now))} timezone=${TimeZone.getDefault().id} " +
            "offsetMs=${TimeZone.getDefault().getOffset(trigger ?: now)}")
        val next = info?.takeIf { a -> a.showIntent?.creatorPackage?.let { ClockApps.isAlarmApp(context, it) } == true }
            ?.let { AlarmInfo(it.triggerTime, it.showIntent?.creatorPackage, it.showIntent) }
        val prev = IslandBus.nextAlarm.value
        IslandBus.nextAlarm.value = next
        if (!announce || next == null || prev?.triggerAt == next.triggerAt) return
        if (next.triggerAt <= now) return
        // Прошлый будильник уже прозвенел или его отложили — это не новый, а очередной по расписанию
        if (prev != null && prev.triggerAt <= now + 60_000) return
        val s = SettingsRepo.current
        if (!s.enabled || !s.evAlarmSet) return
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
