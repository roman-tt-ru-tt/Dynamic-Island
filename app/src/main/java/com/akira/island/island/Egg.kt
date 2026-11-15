package com.akira.island.island

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.island.R
import com.akira.island.core.IslandBus
import com.akira.island.core.Lyrics
import com.akira.island.core.MediaInfo
import com.akira.island.core.Notice
import com.akira.island.core.NoticeStyle
import com.akira.island.core.tr
import com.akira.island.data.IslandSettings
import com.akira.island.data.SettingsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Пасхалка: "다시 만나, 5번 출구" (Chae Yul).
 * Остров пульсирует в такт, а по всему экрану летают и кружатся звезды на фоне ночного неба.
 */
object Egg {
    /** Примерный BPM для этой песни */
    const val BPM = 80f
    const val BEAT_MS = 60_000f / BPM
    
    /** Удар: 1 в момент сильной доли, быстро гаснет */
    val pulse = mutableFloatStateOf(0f)
    val intensity = mutableFloatStateOf(0f)
    val hue = mutableFloatStateOf(0f)
    val flash = mutableFloatStateOf(1f)
    val frame = mutableLongStateOf(0L)

    internal var bounce = 0.03f
    internal val sparks = ArrayList<Spark>()

    private val LINE get() = tr("다시 만나, 5번 출구", "See you again, Exit 5")

    fun matches(m: MediaInfo): Boolean {
        val title = norm(m.title)
        // Ищем корейское название или Chae Yul
        val about = norm(m.artist) + " " + norm(m.album) + " " + title
        return ("다시 만나" in title || "5번 출구" in title || "chae yul" in about) && "chae yul" in about
    }

    private val nonWord = Regex("[^\\p{L}\\p{N}]+")

    private fun norm(s: String?): String = s.orEmpty().lowercase().replace(nonWord, " ").trim()

    fun bump(): Float = 1f + bounce * pulse.floatValue * intensity.floatValue

    internal fun scaled(b: Rect): Rect {
        val k = bump()
        val half = b.width / 2f * k
        return Rect(b.center.x - half, b.top, b.center.x + half, b.top + b.height * k)
    }

    fun watch(context: Context, scope: CoroutineScope) {
        var announced: String? = null
        scope.launch {
            combine(IslandBus.media, SettingsRepo.state, IslandBus.landscape) { m, s, land ->
                m?.takeIf { s.enabled && s.easterEggs && !(s.hideInLandscape && land) && matches(it) }
            }.collect { m ->
                IslandBus.egg.value = m != null
                if (m == null) {
                    announced = null
                    return@collect
                }
                val id = "${m.pkg}|${m.title}"
                if (m.playing && announced != id) {
                    announced = id
                    IslandBus.post(
                        Notice(
                            title = LINE,
                            caption = m.artist.ifBlank { "Chae Yul" },
                            state = m.title,
                            icon = Ios.load(context, R.drawable.ic_star), // Иконка звезды
                            tint = Color(0xFFA78BFA), // Фиолетовый
                            style = NoticeStyle.EGG,
                            extraMs = 2_000,
                            openPkg = m.pkg,
                        ),
                    )
                }
            }
        }
    }

    internal fun reset() {
        pulse.floatValue = 0f
        intensity.floatValue = 0f
        flash.floatValue = 1f
        sparks.clear()
    }
}

internal class Spark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val life: Float,
    val size: Float,
    val color: Color,
    var spin: Float,
    var age: Float = 0f,
)

private val StarGold = Color(0xFFFFD700)
private val StarPink = Color(0xFFFFB6C1)
private val StarPurple = Color(0xFFDDA0DD)
private val SoulPurple = Color(0xFF9370DB)

private fun rainbow(hue: Float): Color = Color.hsv(((hue % 360f) + 360f) % 360f, 0.6f, 1f)

@Composable
fun EggDriver(body: () -> Rect, s: IslandSettings) {
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp * density

    val settings by rememberUpdatedState(s)
    LaunchedEffect(Unit) {
        var lastBeat = Long.MIN_VALUE
        var last = 0L
        try {
            while (isActive) {
                if (IslandBus.media.value?.playing != true && Egg.intensity.floatValue < 0.01f && Egg.sparks.isEmpty()) {
                    Egg.reset()
                    IslandBus.media.first { it?.playing == true }
                    last = 0L
                    continue
                }
                withFrameMillis { }
                val now = SystemClock.elapsedRealtime()
                val dt = if (last == 0L) 16f else (now - last).coerceIn(1L, 100L).toFloat()
                last = now
                val reduce = settings.reduceEffects
                Egg.bounce = if (reduce) 0f else 0.05f // Чуть сильнее прыгает

                val m = IslandBus.media.value
                val playing = m?.playing == true
                val pos = m?.positionAt(now) ?: 0L
                val beatF = pos / Egg.BEAT_MS
                val beat = floor(beatF).toLong()
                val phase = beatF - beat
                val left = if (m != null && m.durationMs > 0) m.durationMs - pos else Long.MAX_VALUE
                val target = when {
                    !playing -> 0f
                    left < 4_000 -> left / 4_000f
                    beat < 8 -> 0.6f
                    else -> 1f
                }
                Egg.intensity.floatValue += (target - Egg.intensity.floatValue) * min(1f, dt / 250f)
                Egg.pulse.floatValue = if (playing) {
                    exp(-phase * 6f) * (if (beat % 4 == 0L) 1f else 0.6f)
                } else {
                    Egg.pulse.floatValue * exp(-dt / 120f)
                }
                Egg.hue.floatValue = (Egg.hue.floatValue + dt * 0.03f) % 360f
                Egg.flash.floatValue = min(1f, Egg.flash.floatValue + dt / 700f)

                if (playing && beat != lastBeat && beat >= 0) {
                    // На каждый бит разлетаются звезды по всему экрану
                    if (!reduce) {
                        val b = Egg.scaled(body())
                        spawn(b, density, screenHeight, if (beat % 4 == 0L) 25 else 10)
                    }
                }
                lastBeat = beat
                step(dt, density)
                Egg.frame.longValue = now
            }
        } finally {
            Egg.reset()
        }
    }
}

/** Звезды вылетают из случайных мест экрана или из острова */
private fun spawn(b: Rect, px: Float, screenHeight: Float, n: Int) {
    if (b.isEmpty) return
    repeat(n) {
        val fromIsland = Random.nextFloat() < 0.3f
        val x: Float
        val y: Float
        var nx: Float
        var ny: Float
        
        if (fromIsland) {
            x = b.left + b.width * Random.nextFloat()
            y = b.bottom
            nx = (x - b.center.x) / b.width
            ny = 1f
        } else {
            x = Random.nextFloat() * px * 400f
            y = Random.nextFloat() * screenHeight
            nx = Random.nextFloat() * 2f - 1f
            ny = Random.nextFloat() * 2f - 1f
        }
        
        val len = sqrt(nx * nx + ny * ny).coerceAtLeast(0.01f)
        nx /= len
        ny /= len
        val turn = (Random.nextFloat() - 0.5f) * 2f // Больше разброса
        val speed = (0.05f + Random.nextFloat() * 0.1f) * px
        val color = listOf(StarGold, StarPink, StarPurple, SoulPurple).random()
        
        Egg.sparks += Spark(
            x, y,
            vx = (nx * cos(turn) - ny * sin(turn)) * speed,
            vy = (nx * sin(turn) + ny * cos(turn)) * speed,
            life = 1500f + Random.nextFloat() * 1000f, // Живут дольше
            size = (4f + Random.nextFloat() * 6f) * px,
            color = color,
            spin = Random.nextFloat() * 6.28f,
        )
    }
    while (Egg.sparks.size > 150) Egg.sparks.removeAt(0)
}

private fun step(dt: Float, px: Float) {
    val drag = exp(-dt / 1200f)
    val it = Egg.sparks.iterator()
    while (it.hasNext()) {
        val p = it.next()
        p.age += dt
        if (p.age >= p.life) {
            it.remove()
            continue
        }
        p.x += p.vx * dt
        p.y += p.vy * dt
        p.vx *= drag
        p.vy = p.vy * drag + 0.00003f * px * dt // Медленно падают
        p.spin += dt * 0.003f
    }
}

@Composable
fun EggGlow(body: () -> Rect) {
    Canvas(Modifier.fillMaxSize()) {
        Egg.frame.longValue
        val raw = body()
        val i = Egg.intensity.floatValue
        if (raw.isEmpty || i < 0.01f) return@Canvas
        val b = Egg.scaled(raw)
        val e = Egg.pulse.floatValue
        val hue = Egg.hue.floatValue
        val corner = min(b.height / 2f, 44.dp.toPx())
        
        val gap = 2.5.dp.toPx() * (1f + 0.5f * e)
        val width = gap * 2f
        for (k in 1..8) {
            val grow = k * gap
            val fade = 1f - k / 9f
            drawIslandRect(
                rainbow(hue + k * 15f).copy(alpha = (i * (0.1f + 0.3f * e) * fade).coerceIn(0f, 1f)),
                topLeft = Offset(b.left - grow, b.top - grow),
                size = Size(b.width + grow * 2, b.height + grow * 2),
                corner = corner + grow,
                notch = SettingsRepo.current.notch,
                style = Stroke(width),
            )
        }
    }
}

@Composable
fun EggSparks(body: () -> Rect) {
    Canvas(Modifier.fillMaxSize()) {
        Egg.frame.longValue
        val raw = body()
        val i = Egg.intensity.floatValue
        if (raw.isEmpty || i < 0.01f) return@Canvas
        
        // Вспышка при смене фраз
        val f = Egg.flash.floatValue
        if (f < 1f) {
            val b = Egg.scaled(raw)
            val corner = min(b.height / 2f, 44.dp.toPx())
            val width = 4.dp.toPx() * (1f - f) + 1.5f
            val wave = Brush.sweepGradient(List(7) { rainbow(Egg.hue.floatValue + 180f + it * 60f) }, b.center)
            for (k in 0..2) {
                val grow = f * 60.dp.toPx() + k * width
                drawIslandRect(
                    wave,
                    topLeft = Offset(b.left - grow, b.top - grow),
                    size = Size(b.width + grow * 2, b.height + grow * 2),
                    corner = corner + grow,
                    notch = SettingsRepo.current.notch,
                    style = Stroke(width),
                    alpha = ((1f - f) * 0.9f * i / (k + 1)).coerceIn(0f, 1f),
                )
            }
        }
        
        // Звезды
        for (p in Egg.sparks) {
            val t = p.age / p.life
            star(Offset(p.x, p.y), p.size * (1f - 0.3f * t), p.spin, p.color.copy(alpha = ((1f - t) * i).coerceIn(0f, 1f)))
        }
    }
}

fun DrawScope.eggRim(corner: Float, notch: Boolean) {
    val i = Egg.intensity.floatValue
    if (i < 0.01f) return
    val e = Egg.pulse.floatValue
    val w = (2f + 2f * e).dp.toPx()
    val hue = Egg.hue.floatValue
    drawIslandRect(
        Brush.sweepGradient(List(7) { rainbow(hue + it * 60f) }, center),
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        corner = corner - w / 2,
        notch = notch,
        style = Stroke(w),
        alpha = (i * (0.6f + 0.4f * e)).coerceIn(0f, 1f),
    )
}

private fun DrawScope.star(c: Offset, r: Float, rotation: Float, color: Color) {
    val path = Path()
    for (k in 0 until 8) {
        val angle = rotation + k * PI.toFloat() / 4f
        val rr = if (k % 2 == 0) r else r * 0.36f
        val x = c.x + cos(angle) * rr
        val y = c.y + sin(angle) * rr
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun SoulHeart(modifier: Modifier = Modifier) {
    // Вместо сердца — фиолетовая звезда
    Icon(
        ImageVector.vectorResource(R.drawable.ic_star), null, tint = SoulPurple,
        modifier = modifier.graphicsLayer {
            val k = 1f + 0.25f * Egg.pulse.floatValue
            scaleX = k
            scaleY = k
        },
    )
}

@Composable
fun EggBanner(n: Notice, s: IslandSettings) {
    val haptic = rememberHaptic()
    LaunchedEffect(n.id) { haptic() }
    val dim = Color.White.copy(alpha = 0.55f)
    
    // Если есть текст песни, покажем его, иначе — фразу из игры
    val lyrics by IslandBus.lyrics.collectAsState()
    val media by IslandBus.media.collectAsState()
    val pos = rememberWallNow(100) { media?.positionAt(it) ?: 0L }
    
    val lineText = remember(lyrics, pos) {
        val l = lyrics
        val m = media
        if (l != null && m != null && l.key == m.key && l.lines.isNotEmpty()) {
            val idx = l.indexAt(pos)
            if (idx >= 0) l.lines[idx].text else "• • •"
        } else {
            Egg.LINE // fallback
        }
    }

    var typed by remember(n.id) { mutableIntStateOf(if (s.reduceEffects) lineText.length else 0) }
    LaunchedEffect(n.id, lineText) {
        typed = if (s.reduceEffects) lineText.length else 0
        while (typed < lineText.length) {
            delay(if (lineText[typed] in ".,!?" || lineText[typed] == ' ') 150L else 50L)
            typed++
        }
    }
    
    Column(Modifier.width(messageWidth()).padding(bottom = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().height(s.height.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                SoulHeart(Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(n.caption.orEmpty(), color = dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(compactGap(s)))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(n.state.orEmpty(), color = dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height((messageTop(s) - s.height.dp).coerceAtLeast(4.dp)))
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .border(1.dp, SoulPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append(lineText.take(typed)) }
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0f))) { append(lineText.drop(typed)) }
                },
                color = Color.White, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
