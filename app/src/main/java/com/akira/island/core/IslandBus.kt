package com.akira.island.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IslandBus {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    
    val nextAlarm = MutableStateFlow<AlarmInfo?>(null)
    val builtInAlarm = MutableStateFlow<AlarmInfo?>(null) // <-- ДОБАВЛЕНО
    
    // Остальные поля (если они у вас есть, оставьте их здесь):
    // val media = MutableStateFlow<MediaInfo?>(null)
    // val calls = MutableStateFlow<Map<String, CallInfo>>(emptyMap())
    // val chrono = MutableStateFlow<Map<String, ChronoInfo>>(emptyMap())
    // val progress = MutableStateFlow<Map<String, ProgressInfo>>(emptyMap())
    // val live = MutableStateFlow<Map<String, LiveInfo>>(emptyMap())
    // val ringingAlarms = MutableStateFlow<Set<String>>(emptySet())
    // val screenOn = MutableStateFlow(true)

    fun post(notice: Notice) {
        // Ваша логика отправки событий в остров
    }
}
