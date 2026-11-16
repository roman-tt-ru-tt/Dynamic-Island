package com.akira.island.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.akira.island.R
import com.akira.island.core.AlarmInfo
import com.akira.island.core.IslandBus
import com.akira.island.core.Notice
import com.akira.island.core.NoticeStyle
import com.akira.island.core.Tints
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.island.Ios
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AlarmMonitor(private val context: Context) {

    companion object {
        private const val TAG = "DynamicIslandAlarm"
        private const val XIAOMI_CLOCK = "com.miui.clock"
    }

    private val alarms =
        context.getSystemService(AlarmManager::class.java)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            Log.i(TAG, "Received system event: ${intent.action}")
            refresh(announce = true)
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        refresh(announce = false)
    }

    fun stop() {
        runCatching {
            context.unregisterReceiver(receiver)
        }

        IslandBus.nextAlarm.value = null
    }

    private fun refresh(announce: Boolean) {
        val info = runCatching {
            alarms?.nextAlarmClock
        }.onFailure {
            Log.e(TAG, "Cannot read next alarm", it)
        }.getOrNull()

        val now = System.currentTimeMillis()
        val triggerAt = info?.triggerTime ?: 0L
        val packageName = info?.showIntent?.creatorPackage
        val timezone = TimeZone.getDefault()

        val localFormat = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss Z",
            Locale.getDefault()
        ).apply {
            timeZone = timezone
        }

        val utcFormat = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss 'UTC'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        Log.i(TAG, "========== ALARM DEBUG ==========")
        Log.i(TAG, "Package: $packageName")
        Log.i(TAG, "Timezone: ${timezone.id}")
        Log.i(TAG, "UTC offset: ${timezone.getOffset(now) / 60000} minutes")
        Log.i(TAG, "Now LOCAL: ${localFormat.format(Date(now))}")
        Log.i(TAG, "Now UTC: ${utcFormat.format(Date(now))}")
        Log.i(TAG, "Trigger timestamp: $triggerAt")

        if (triggerAt > 0L) {
            Log.i(
                TAG,
                "Alarm LOCAL: ${localFormat.format(Date(triggerAt))}"
            )
            Log.i(
                TAG,
                "Alarm UTC: ${utcFormat.format(Date(triggerAt))}"
            )
            Log.i(
                TAG,
                "Time until alarm: ${(triggerAt - now) / 60000} minutes"
            )
        } else {
            Log.i(TAG, "No next system alarm")
        }

        val next = info
            ?.takeIf { alarm ->
                val pkg = alarm.showIntent?.creatorPackage

                pkg != null &&
                    (
                        pkg == XIAOMI_CLOCK ||
                        ClockApps.isAlarmApp(context, pkg)
                    ) &&
                    alarm.triggerTime > now
            }
            ?.let { alarm ->
                AlarmInfo(
                    alarm.triggerTime,
                    alarm.showIntent?.creatorPackage,
                    alarm.showIntent
                )
            }

        Log.i(TAG, "Accepted alarm: ${next != null}")
        Log.i(TAG, "Accepted trigger: ${next?.triggerAt}")

        val previous = IslandBus.nextAlarm.value

        IslandBus.nextAlarm.value = next

        Log.i(TAG, "=================================")

        if (!announce || next == null) return

        if (previous?.triggerAt == next.triggerAt) return

        if (previous != null &&
            previous.triggerAt <= now + 60_000
        ) {
            return
        }

        val settings = SettingsRepo.current

        if (!settings.enabled || !settings.evAlarmSet) return

        IslandBus.post(
            Notice(
                title = tr("Будильник", "Alarm"),
                icon = Ios.load(
                    context,
                    R.drawable.ios_alarm
                ),
                tint = Tints.Orange,
                style = NoticeStyle.ALARM,
                alarm = next,
                extraMs = 2_000
            )
        )
    }
}
