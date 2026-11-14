@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.akira.island.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.akira.island.core.AppMessage
import com.akira.island.core.CallInfo
import com.akira.island.core.Controls
import com.akira.island.core.IslandBus
import com.akira.island.core.LiveInfo
import com.akira.island.core.LiveKind
import com.akira.island.core.MsgAction
import com.akira.island.core.ReplyState
import com.akira.island.core.MediaInfo
import com.akira.island.core.Notice
import com.akira.island.core.NoticeStyle
import com.akira.island.core.Tints
import com.akira.island.core.formatDuration
import com.akira.island.data.IslandSettings
import com.akira.island.data.SettingsRepo
import com.akira.island.service.Messages
import androidx.compose.runtime.collectAsState
import com.akira.island.core.tr
import com.akira.island.ui.Inter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val Digits = TextStyle(fontFamily = Inter, fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

fun activeHeight(s: IslandSettings): Dp = (s.height + s.activeExtra).dp
fun compactGap(s: IslandSettings): Dp = s.cameraGap.dp.coerceAtLeast(8.dp)
fun sideMax(s: IslandSettings): Dp = activeHeight(s) * 2.6f
fun noticeHeight(s: IslandSettings): Dp = (s.height.dp * 1.68f).coerceAtLeast(56.dp)
fun callHeight(s: IslandSettings): Dp = (s.height.dp * 2.27f).coerceAtLeast(76.dp)
fun cameraInset(s: IslandSettings): Dp = s.height.dp * 0.5f + 11.5.dp

@Composable
private fun DrawableImage(drawable: Drawable, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
        drawable.draw(drawContext.canvas.nativeCanvas)
    }
}

@Composable
fun CompactContent(act: Act, s: IslandSettings, wideWidth: Dp) {
    val h = activeHeight(s)
    val gap = compactGap(s)
    val side = sideMax(s)
    val icon = h * 0.5f
    when (act) {
        is Act.Idle -> Box(Modifier.size(s.width.dp, s.height.dp), contentAlignment = Alignment.CenterStart) {
            val keyguard by IslandBus.keyguard.collectAsState()
            AnimatedVisibility(s.evLock && keyguard, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Icon(Ios.lock, null, tint = Color.White, modifier = Modifier.padding(start = s.height.dp * 0.38f).size(s.height.dp * 0.46f))
            }
        }

        // Увеличенная иконка запущенного приложения
        is Act.AppA -> Box(
            Modifier.size(s.width.dp, s.height.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            DrawableImage(
                drawable = act.icon,
                modifier = Modifier
                    .padding(start = s.height.dp * 0.28f)
                    .size(h * 0.75f) // Увеличили с 0.55f до 0.75f
                    .clip(RoundedCornerShape(h * 0.22f)),
            )
        }

        is Act.MediaA -> MediaCompact(act.m, s)

        is Act.TimerA -> {
            val now = rememberNow(250)
            if (act.t.ringing) {
                val wobble = rememberInfiniteTransition(label = "bell")
                val angle = if (s.reduceEffects || !s.fxShake) null
                else wobble.animateFloat(-18f, 18f, infiniteRepeatable(tween(120), RepeatMode.Reverse), label = "a")
                CompactRow(h, gap, side * 1.4f,
                    left = {
                        Icon(Ios.bell, null, tint = Tints.Orange, modifier = Modifier.size(icon).graphicsLayer {
                            rotationZ = angle?.value ?: 0f
                            transformOrigin = TransformOrigin(0.5f, 0.1f)
                        })
                    },
                    right = { Text(tr("Таймер", "Timer"), color = Tints.Orange, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                )
            } else {
                val left = act.t.left(now)
                val color = if (left <= 10_000 && !act.t.paused) Tints.Red else Tints.Orange
                CompactRow(h, gap, side * 1.4f,
                    left = { Icon(if (act.t.paused) Ios.pause else Ios.timer, null, tint = color, modifier = Modifier.size(icon)) },
                    right = { TimeText(formatDuration(left + 999), color, 18, FontWeight.Bold) },
                )
            }
        }

        is Act.StopwatchA -> {
            val now = rememberNow(250, active = act.w.running)
            CompactRow(h, gap, side * 1.4f,
                left = { Icon(Ios.stopwatch, null, tint = Tints.Orange, modifier = Modifier.size(icon)) },
                right = { TimeText(formatDuration(act.w.elapsed(now)), Tints.Orange, 18, FontWeight.Bold) },
            )
        }

        is Act.ChronoA -> {
            val now = rememberWallNow(500)
            val recording = act.c.isRecording
            val tint = if (recording) Tints.Red else Tints.Orange
            CompactRow(h, gap, side * 1.4f,
                left = {
                    when {
                        recording -> Equalizer(tint, true, s.liveEqualizer, Modifier.size(h * 0.62f, h * 0.42f), bars = 5)
                        act.c.pausedMs != null -> Icon(Ios.pause, null, tint = tint, modifier = Modifier.size(icon))
                        else -> AppIcon(act.c.icon, h * 0.56f, Ios.timer, tint)
                    }
                },
                right = { TimeText(formatDuration(act.c.elapsed(now)), tint, 18, FontWeight.Bold) },
            )
        }

        is Act.CallA -> if (act.c.incoming) {
            IncomingCall(act.c, s, wideWidth)
        } else {
            val now = rememberWallNow(500)
            CompactRow(h, gap, side * 2f,
                left = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ios.call, null, tint = Tints.Green, modifier = Modifier.size(h * 0.42f))
                        Spacer(Modifier.width(6.dp))
                        TimeText(formatDuration(now - act.c.base), Tints.Green, 15)
                    }
                },
                right = { Equalizer(Tints.Green, true, s.liveEqualizer, Modifier.size(h * 0.7f, h * 0.42f), bars = 6) },
            )
        }

        is Act.TorchA -> CompactRow(h, gap, side,
            left = { Icon(Ios.flashlight, null, tint = Tints.Yellow, modifier = Modifier.size(icon)) },
            right = { GlowDot(Tints.Yellow, h * 0.26f, !s.reduceEffects) },
        )

        is Act.ProgressA -> CompactRow(h, gap, side,
            left = { AppIcon(act.p.icon, h * 0.56f, Ios.download, Tints.Blue) },
            right = {
                if (act.p.indeterminate) {
                    Icon(Ios.download, null, tint = Tints.Blue, modifier = Modifier.size(icon))
                } else {
                    val fraction by animateFloatAsState(act.p.fraction, tween(450, easing = FastOutSlowInEasing), label = "progress")
                    ProgressRing(fraction, Tints.Blue, Modifier.size(h * 0.52f), stroke = 3.dp)
                }
            },
        )

        is Act.ChargingA -> CompactRow(h, gap, side * 1.6f,
            left = { Icon(Ios.flash, null, tint = Tints.Green, modifier = Modifier.size(icon)) },
            right = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeText("${act.b.level}%", Tints.Green, 15)
                    Spacer(Modifier.width(5.dp))
                    BatteryGlyph(act.b.level, Tints.Green, Modifier.size(h * 0.6f, h * 0.3f))
                }
            },
        )

        is Act.LiveA -> LiveCompact(act.l, s)

        is Act.AlarmA -> AlarmCompact(act.a, s)

        is Act.NoticeA -> NoticeCompact(act.n, s, wideWidth)

        is Act.PayA -> PayReady(tr("Оплата", "Pay"), tr("К терминалу", "Hold near reader"), act.p.icon, s, wideWidth)

    }
}

@Composable
private fun TimeText(text: String, color: Color, size: Int = 16, weight: FontWeight = FontWeight.SemiBold) {
    Text(text, color = color, fontSize = size.sp, style = Digits.copy(fontWeight = weight), maxLines = 1, softWrap = false)
}

@Composable
private fun GlowDot(color: Color, size: Dp, animate: Boolean) {
    val time = rememberFxClock(animate)
    Canvas(Modifier.size(size * 1.8f)) {
        val k = if (animate) 0.55f + 0.45f * pingPong(time.value, 1100) else 1f
        val r = this.size.minDimension / 2
        drawCircle(color.copy(alpha = 0.25f * k), radius = r * k)
        drawCircle(color, radius = r / 1.8f)
    }
}

@Composable
fun CompactRow(
    h: Dp,
    gap: Dp,
    side: Dp,
    left: @Composable BoxScope.() -> Unit,
    right: @Composable BoxScope.() -> Unit,
    shrink: Boolean = true,
) {
    val st = SettingsRepo.state.collectAsState().value
    val baseWidth = st.width.dp + st.activeWidth.dp
    val density = LocalDensity.current
    SubcomposeLayout { _ ->
        val hPx = h.roundToPx()
        val pad = (h * 0.3f).roundToPx()
        val gapPx = gap.roundToPx()
        val loose = Constraints(maxWidth = side.roundToPx().coerceAtLeast(1), maxHeight = hPx)
        var l = subcompose("l") { Box(contentAlignment = Alignment.Center, content = left) }.first().measure(loose)
        var r = subcompose("r") { Box(contentAlignment = Alignment.Center, content = right) }.first().measure(loose)
        val base = baseWidth.roundToPx()
        val sideNeed = maxOf(l.width, r.width)
        val sideRoom = (base - gapPx) / 2 - pad
        val k = if (shrink && sideNeed > sideRoom && sideRoom > 0) (sideRoom.toFloat() / sideNeed).coerceIn(0.7f, 1f) else 1f
        if (k < 1f) {
            val small = Density(density.density * k, density.fontScale)
            l = subcompose("ls") {
                CompositionLocalProvider(LocalDensity provides small) { Box(contentAlignment = Alignment.Center, content = left) }
            }.first().measure(loose)
            r = subcompose("rs") {
                CompositionLocalProvider(LocalDensity provides small) { Box(contentAlignment = Alignment.Center, content = right) }
            }.first().measure(loose)
        }
        val needed = 2 * (pad + maxOf(l.width, r.width)) + gapPx
        val w = maxOf(needed, base)
        layout(w, hPx) {
            l.place(pad, (hPx - l.height) / 2)
            r.place(w - pad - r.width, (hPx - r.height) / 2)
        }
    }
}

// ... Функции MediaCompact, NoticeCompact, WideNotice, PillNotice, MessageCard, PayReady, PayDone, LiveCompact и др. остаются без изменений ...
