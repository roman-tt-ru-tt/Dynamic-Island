package com.akira.island.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Простой логгер: пишет всё в память (для показа в UI) и в файл (для отправки).
 * Файл лежит в cacheDir/logs/island.log. При превышении размера старые записи удаляются.
 */
object AppLog {
    private const val MAX_LINES = 500
    private const val FILE_NAME = "island.log"
    private const val TAG = "IslandLog"

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private var file: File? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        val dir = File(context.cacheDir, "logs").apply { mkdirs() }
        file = File(dir, FILE_NAME)
        // Загружаем старые записи из файла
        runCatching {
            if (file?.exists() == true) {
                val saved = file!!.readLines().takeLast(MAX_LINES)
                _lines.value = saved
            }
        }
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val now = Date()
        val line = "${timeFormat.format(now)} [$tag] $message" +
            if (throwable != null) "\n    ${throwable.javaClass.simpleName}: ${throwable.message}" else ""
        
        android.util.Log.d(TAG, line)
        
        val current = _lines.value.toMutableList().apply {
            add(line)
            while (size > MAX_LINES) removeAt(0)
        }
        _lines.value = current

        // Пишем в файл (не блокируя UI)
        runCatching {
            file?.appendText("${dateFormat.format(now)} $line\n")
            // Обрезаем файл, если он стал слишком большим
            if (file?.length() ?: 0 > 512_000) {
                val lines = file!!.readLines().takeLast(MAX_LINES)
                file!!.writeText(lines.joinToString("\n") + "\n")
            }
        }
    }

    fun logException(tag: String, throwable: Throwable) {
        log(tag, "⚠ ${throwable.javaClass.simpleName}: ${throwable.message}", throwable)
    }

    fun clear() {
        _lines.value = emptyList()
        runCatching { file?.writeText("") }
    }

    fun file(): File? = file
}
