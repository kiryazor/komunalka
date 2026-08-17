package com.example.komunalka.utils

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Tesseract OCR, настроенный только на распознавание цифр (whitelist 0-9
 * + разделитель дробной части), с режимом "одна строка текста". Для
 * счётчиков это обычно заметно точнее, чем ML Kit — тот заточен под
 * обычный печатный текст документов и куда чаще путает похожие символы.
 *
 * Требует файл eng.traineddata в assets/tessdata (см. README.md рядом) —
 * его нельзя было скачать автоматически при генерации проекта. Если файла
 * нет, [isAvailable] вернёт false и вызывающий код (MeterScanner) сам
 * откатится на ML Kit — приложение не падает.
 */
object TesseractScanner {
    private const val LANG = "eng"
    private const val WHITELIST = "0123456789.,"

    fun isAvailable(context: Context): Boolean = try {
        context.assets.list("tessdata")?.any { it == "$LANG.traineddata" } == true
    } catch (e: Exception) {
        false
    }

    suspend fun recognizeReading(context: Context, bitmap: Bitmap): String? =
        withContext(Dispatchers.Default) {
            try {
                val dataPath = ensureTrainedDataCopied(context)
                val api = TessBaseAPI()
                val initialized = api.init(dataPath, LANG)
                if (!initialized) {
                    api.recycle()
                    return@withContext null
                }
                api.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
                api.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, WHITELIST)
                api.setImage(bitmap)
                val rawText = api.utF8Text
                api.recycle()

                rawText?.trim()?.replace(',', '.')?.trim('.')?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Tesseract умеет читать обученные данные только с диска (не из assets
     * напрямую), поэтому при первом запуске копируем файл во внутреннее
     * хранилище приложения — дальше используем уже скопированную копию.
     */
    private fun ensureTrainedDataCopied(context: Context): String {
        val tessRoot = File(context.filesDir, "tesseract")
        val tessDataDir = File(tessRoot, "tessdata")
        if (!tessDataDir.exists()) tessDataDir.mkdirs()

        val dest = File(tessDataDir, "$LANG.traineddata")
        if (!dest.exists() || dest.length() == 0L) {
            context.assets.open("tessdata/$LANG.traineddata").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return tessRoot.absolutePath
    }
}
