@Composable
private fun IdleExpanded(s: IslandSettings, battery: BatteryInfo) {
    val context = LocalContext.current
    val now = rememberWallNow(1000)
    val time = DateFormat.getTimeFormat(context).format(Date(now))
    val date = SimpleDateFormat(if (Lang.en) "EEEE, MMMM d" else "EEEE, d MMMM", Lang.locale).format(Date(now))
        .replaceFirstChar { it.titlecase(Lang.locale) }
    val alarm by IslandBus.nextAlarm.collectAsState()
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(clockText(time, 36.sp), color = Color.White, fontSize = 36.sp, style = Digits)
                Text(date, color = Dim, fontSize = 14.sp)
                alarm?.takeIf { it.triggerAt - now in 1..86_400_000L }?.let { a ->
                    Row(
                        Modifier.padding(top = 3.dp).clickable { openAlarm(context, a) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Ios.alarm, null, tint = AlarmTint, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(tr("${wakeTime(context, a.triggerAt)} · через ${durationWords(a.triggerAt - now)}",
                            "${wakeTime(context, a.triggerAt)} · in ${durationWords(a.triggerAt - now)}"),
                            color = AlarmTint, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
            if (battery.level >= 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (battery.charging) Ios.batteryCharging else Ios.batteryFull, null,
                        tint = when {
                            battery.charging -> Tints.Green
                            battery.level <= 15 -> Tints.Red
                            else -> Color.White
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${battery.level}%", color = Color.White, fontSize = 16.sp, style = Digits)
                }
            }
        }
        val actions = s.quickActions
        if (actions.isEmpty() && s.customQuickApps.isEmpty()) return@Column
        Spacer(Modifier.height(16.dp))
        
        // Объединяем стандартные действия и кастомные приложения для сетки
        val allActions = mutableListOf<Any>()
        if (QuickAction.TIMER in actions) {
            // Таймер рендерим отдельно, он занимает всю ширину
        }
        actions.filter { it != QuickAction.TIMER }.forEach { allActions.add(it) }
        s.customQuickApps.forEach { allActions.add(it) }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Таймер от 1 до 99 минут
            if (QuickAction.TIMER in actions) Row(
                Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton(Ios.remove, size = 38.dp, iconScale = 0.44f) {
                    SettingsRepo.update { copy(timerMinutes = (timerMinutes - 1).coerceAtLeast(1)) }
                }
                Text(
                    tr("${s.timerMinutes} мин", "${s.timerMinutes} min"), color = Color.White, fontSize = 16.sp, style = Digits,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                RoundButton(Ios.add, size = 38.dp, iconScale = 0.44f) {
                    SettingsRepo.update { copy(timerMinutes = (timerMinutes + 1).coerceAtMost(99)) }
                }
                Spacer(Modifier.width(8.dp))
                PillButton(Ios.play, tr("Таймер", "Timer"), bg = Tints.Orange, fg = Color.Black) {
                    Controls.startTimer(s.timerMinutes)
                    Controls.expand("timer")
                }
            }
            
            // Остальные кнопки — по две в ряд
            allActions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { item ->
                        if (item is QuickAction) {
                            QuickButton(item, context, Modifier.weight(1f))
                        } else if (item is String) {
                            AppQuickButton(item, context, Modifier.weight(1f))
                        }
                    }
                    // Если в ряду один элемент, добавляем пустое место для выравнивания
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppQuickButton(pkg: String, context: Context, modifier: Modifier) {
    val appName = remember(pkg) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault("App")
    }
    val icon = remember(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg) }.getOrNull()
    }

    val haptic = rememberHaptic()
    Row(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable { 
                haptic()
                Controls.collapse()
                Controls.openApp(context, pkg) 
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            DrawableImage(icon, Modifier.size(20.dp).clip(CircleShape))
        } else {
            Icon(Ios.apps, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(appName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
