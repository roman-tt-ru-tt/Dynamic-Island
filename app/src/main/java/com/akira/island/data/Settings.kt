package com.akira.island.data

import android.content.Context
import android.content.SharedPreferences
import com.akira.island.core.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class FxRate(val fps: Int, private val ru: String, private val en: String) {
    ECO(30, "30 кадров — экономнее", "30 fps — saves power"),
    SMOOTH(60, "60 кадров — плавно", "60 fps — smooth"),
    MAX(0, "Как у экрана — до 120", "Screen rate — up to 120"),
    ;
    val title: String get() = tr(ru, en)
}

@Serializable
enum class ExpandStyle(private val ru: String, private val en: String) {
    FRUIT("Как Фрукт", "Like a Fruit"),
    SPRING("Пружина", "Spring"),
    SMOOTH("Плавно", "Smooth"),
    INSTANT("Без анимации", "No animation"),
    ;
    val title: String get() = tr(ru, en)
}

@Serializable
enum class Elasticity(private val ru: String, private val en: String, val damping: Float) {
    NONE("Без упругости", "No bounce", 1f),
    LOW("Слабая упругость", "Light bounce", 0.8f),
    MEDIUM("Средняя упругость", "Medium bounce", 0.62f),
    HIGH("Сильная упругость", "Strong bounce", 0.48f),
    ;
    val title: String get() = tr(ru, en)
}

@Serializable
enum class TapAction(private val ru: String, private val en: String) {
    EXPAND("Раскрыть капсулу", "Expand the capsule"),
    OPEN_APP("Открыть приложение", "Open the app"),
    PLAY_PAUSE("Пауза / воспроизведение", "Play / pause"),
    NEXT_TRACK("Следующий трек", "Next track"),
    SCREENSHOT("Скриншот без острова", "Screenshot without the island"),
    NOTHING("Ничего", "Nothing"),
    ;
    val title: String get() = tr(ru, en)
}

@Serializable
enum class QuickAction(
    private val ru: String,
    private val en: String,
    private val shortRu: String,
    private val shortEn: String,
) {
    TIMER("Таймер", "Timer", "Таймер", "Timer"),
    STOPWATCH("Секундомер", "Stopwatch", "Секундомер", "Stopwatch"),
    FLASHLIGHT("Фонарик", "Flashlight", "Фонарик", "Flashlight"),
    SCREENSHOT("Скриншот без острова", "Screenshot without the island", "Скриншот", "Screenshot"),
    CAMERA("Камера", "Camera", "Камера", "Camera"),
    LOCK("Заблокировать экран", "Lock the screen", "Блокировка", "Lock"),
    SILENT("Беззвучный режим", "Silent mode", "Без звука", "Silent"),
    ;

    val title: String get() = tr(ru, en)
    val short: String get() = tr(shortRu, shortEn)
}

@Serializable
enum class AccentMode(private val ru: String, private val en: String, private val hintRu: String, private val hintEn: String) {
    DEFAULT("Сиреневый", "Lavender", "Цвет приложения", "The app's color"),
    SYSTEM("Системный", "System", "Цвет из обоев Android 12+ (Material You)", "From your Android 12+ wallpaper (Material You)"),
    ADAPTIVE("Из аватара", "From the avatar", "Подбирается по аватару или значку приложения", "Matched to the avatar or app icon"),
    CUSTOM("Свой", "Custom", "Любой цвет из набора", "Any color from the palette"),
    ;
    val title: String get() = tr(ru, en)
    val hint: String get() = tr(hintRu, hintEn)
}

@Serializable
enum class AppLanguage {
    SYSTEM, RU, EN;
    val title: String
        get() = when (this) {
            SYSTEM -> tr("Как в системе", "System default")
            RU -> "Русский"
            EN -> "English"
        }
}

@Serializable
data class IslandSettings(
    val enabled: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,

    // Динамические карточки
    val cardMedia: Boolean = true,
    val cardTorch: Boolean = true,
    val cardProgress: Boolean = true,
    val cardTimer: Boolean = true,
    val cardCharging: Boolean = true,
    val cardCall: Boolean = true,
    val cardMessages: Boolean = true,
    val cardAppIcon: Boolean = true,
    val msgInIsland: Boolean = true,

    val msgActions: Boolean = true,
    val cardLive: Boolean = true,
    val cardAlarm: Boolean = true,
    val clockAutoTap: Boolean = true,
    val timerFromAssistant: Boolean = true,
    val wakeForAlerts: Boolean = true,
    val easterEggs: Boolean = true,

    // События
    val evChargeStart: Boolean = true,
    val evChargeStop: Boolean = true,
    val evLowBattery: Boolean = true,
    val evWifiOn: Boolean = true,
    val evWifiOff: Boolean = true,
    val evHeadsetOn: Boolean = true,
    val evHeadsetOff: Boolean = true,
    val evAirplaneOn: Boolean = true,
    val evAirplaneOff: Boolean = true,
    val evRinger: Boolean = true,
    val evBrightness: Boolean = true,
    val evUsbOn: Boolean = true,
    val evUsbOff: Boolean = true,
    val evUnlock: Boolean = false,
    val evLock: Boolean = true,
    val evDnd: Boolean = true,
    val evPayReady: Boolean = true,
    val payHintOnOpen: Boolean = false,
    val evPaySuccess: Boolean = true,
    val evAlarmSet: Boolean = true,

    // Размер и положение
    val width: Int = 104,
    val height: Int = 31,
    val offsetX: Int = 0,
    val offsetY: Int = 11,
    val notch: Boolean = false,
    val cameraGap: Int = 34,
    val activeExtra: Int = 6,
    val activeWidth: Int = 40,
    val layoutVersion: Int = 1,

    // Цвета
    val color: Long = 0xFF000000,
    val outline: Boolean = false,
    val outlineColor: Long = 0xFF3A3A40,
    val outlineWidth: Float = 1f,
    val outlineFromArt: Boolean = true,
    val shadow: Boolean = false,

    val accentMode: AccentMode = AccentMode.DEFAULT,
    val accentColor: Long = 0xFF0A84FF,

    // Обновления
    val autoUpdateCheck: Boolean = true,
    val skippedVersion: String = "",
    val lastSeenVersion: String = "",
    val lastUpdateCheck: Long = 0,
    val islandVersion: String = "",

    // Внешний вид
    val liquidGlass: Boolean = false,
    val glassOpacity: Int = 68,

    // Анимации
    val expandStyle: ExpandStyle = ExpandStyle.FRUIT,
    val durationPercent: Int = 35,
    val liquidExpand: Boolean = false,
    val elasticity: Elasticity = Elasticity.LOW,
    val reduceEffects: Boolean = false,
    val haptics: Boolean = true,

    // Эффекты
    val fxBlur: Boolean = true,
    val fxEqualizer: Boolean = true,
    val fxRate: FxRate = FxRate.SMOOTH,
    val fxGlow: Boolean = false,
    val fxPay: Boolean = true,
    val fxShake: Boolean = true,
    val batterySaver: Boolean = false,

    // Жесты
    val tapAction: TapAction = TapAction.EXPAND,
    val doubleTapAction: TapAction = TapAction.NOTHING,
    val longPressAction: TapAction = TapAction.OPEN_APP,
    val swipeTracks: Boolean = true,
    val swipeExpand: Boolean = true,

    val lyricsEnabled: Boolean = true,

    // Поведение
    val hideIdle: Boolean = false,
    val hideInLandscape: Boolean = false,
    val hideWhenPlayerOpen: Boolean = true,
    val scheduleOn: Boolean = false,
    val scheduleFrom: Int = 10,
    val scheduleTo: Int = 23,
    val noticeDurationMs: Int = 2500,
    val timerMinutes: Int = 5,
    val autoCollapseSec: Int = 0,
    val bubble: Boolean = true,
    val idleExpand: Boolean = true,
    val quickActions: List<QuickAction> = listOf(QuickAction.TIMER, QuickAction.STOPWATCH, QuickAction.FLASHLIGHT),
    val customQuickApps: List<String> = emptyList(),
    /** Порядок кнопок и приложений в раскрытом острове: "action:SILENT", "app:com.tiktok" */
    val quickOrder: List<String> = emptyList(),
    val keepAlive: Boolean = false,
    val msgLines: Int = 3,
    val msgInApp: Boolean = false,
    val msgChannels: Map<String, String> = emptyMap(),
    val bannerHintHidden: Boolean = false,
) {
    fun forPowerSave(on: Boolean): IslandSettings = if (on && batterySaver && !reduceEffects) copy(reduceEffects = true) else this
    fun inSchedule(hour: Int): Boolean = when {
        !scheduleOn || scheduleFrom == scheduleTo -> true
        scheduleFrom < scheduleTo -> hour in scheduleFrom until scheduleTo
        else -> hour >= scheduleFrom || hour < scheduleTo
    }
    val topOffset: Int get() = if (notch) 0 else offsetY
    val liveEqualizer: Boolean get() = fxEqualizer && !reduceEffects
}

object SettingsRepo {
    private const val KEY = "settings_v1"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private var prefs: SharedPreferences? = null
    var hadSavedSettings = false
        private set
    private val _state = MutableStateFlow(IslandSettings())
    val state: StateFlow<IslandSettings> = _state.asStateFlow()
    val current: IslandSettings get() = _state.value

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = p
        hadSavedSettings = p.contains(KEY)
        p.getString(KEY, null)?.let { raw ->
            runCatching { json.decodeFromString<IslandSettings>(raw) }.onSuccess { _state.value = it }
        }
        val cur = _state.value
        if (cur.layoutVersion in 2..2) update { copy(cameraGap = IslandSettings().cameraGap, layoutVersion = 3) }
        if (cur.layoutVersion < 2) {
            update {
                copy(
                    width = if (width == 96) 126 else width,
                    height = if (height == 34) 37 else height,
                    offsetY = if (offsetY == 8) 11 else offsetY,
                    layoutVersion = 3,
                )
            }
        }
        if (current.layoutVersion < 4) {
            val d = IslandSettings()
            update {
                val old = width == 126 && height == 37
                copy(
                    width = if (old) d.width else width,
                    height = if (old) d.height else height,
                    activeWidth = if (old && activeWidth == 48) d.activeWidth else activeWidth,
                    elasticity = if (elasticity == Elasticity.HIGH) d.elasticity else elasticity,
                    layoutVersion = 4,
                )
            }
        }
    }

    fun update(block: IslandSettings.() -> IslandSettings) {
        val next = _state.value.block()
        if (next == _state.value) return
        _state.value = next
        prefs?.edit()?.putString(KEY, json.encodeToString(next))?.apply()
    }

    fun reset() = update { IslandSettings() }

    fun resetAnimations() = update {
        val d = IslandSettings()
        copy(
            expandStyle = d.expandStyle,
            durationPercent = d.durationPercent,
            liquidExpand = d.liquidExpand,
            elasticity = d.elasticity,
            reduceEffects = d.reduceEffects,
            noticeDurationMs = d.noticeDurationMs,
        )
    }

    fun resetEffects() = update {
        val d = IslandSettings()
        copy(
            fxBlur = d.fxBlur, fxEqualizer = d.fxEqualizer, fxRate = d.fxRate, fxGlow = d.fxGlow, fxPay = d.fxPay,
            fxShake = d.fxShake, batterySaver = d.batterySaver,
        )
    }
}
