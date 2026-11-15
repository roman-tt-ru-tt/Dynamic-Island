package com.akira.island.island

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp,
    content: @Composable (T, Boolean) -> Unit,
) {
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var heights by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { index, item ->
            val k = key(item)
            val isDragging = draggingKey == k
            
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        heights = heights + (k to coords.size.height.toFloat())
                    }
                    .pointerInput(items) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingKey = k; dragOffset = 0f },
                            onDragEnd = { draggingKey = null; dragOffset = 0f },
                            onDragCancel = { draggingKey = null; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val h = heights[k] ?: 0f
                                if (h > 0f) {
                                    val step = h + spacing.toPx()
                                    val dir = if (dragOffset > step / 2) 1 else if (dragOffset < -step / 2) -1 else 0
                                    if (dir != 0) {
                                        val newIndex = (index + dir).coerceIn(0, items.lastIndex)
                                        if (newIndex != index) {
                                            onMove(index, newIndex)
                                            dragOffset -= dir * step
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        alpha = if (isDragging) 0.9f else 1f
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                    }
            ) {
                content(item, isDragging)
            }
        }
    }
}
