package com.example.komunalka.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * "Изюминка" приложения: распознавание показаний счётчика по фотографии.
 *
 * Пользователь фотографирует и обрезает табло счётчика (после съёмки
 * открывается редактор обрезки — см. AddEditBillFragment), затем текст
 * распознаётся: сначала пробуем Tesseract с whitelist только на цифры
 * (если добавлены обученные данные — см. assets/tessdata/README.md),
 * а если его нет или он не справился — откатываемся на ML Kit (офлайн,
 * без интернета).
 *
 * Механические счётчики (барабан с цифрами) — сложный случай для любого
 * универсального OCR: шрифт нестандартный, при перелистывании барабана
 * цифра наполовину "смазана" соседней. Чтобы дать распознаванию максимум
 * шансов, перед ML Kit фото проходит предобработку:
 *  1) перевод в градации серого;
 *  2) бинаризация по методу Оцу (чёрно-белый, без полутонов и шума-муара);
 *  3) инверсия в чёрный текст на белом фоне, если исходно фон тёмный —
 *     большинство OCR-моделей обучены на печатных документах и куда
 *     увереннее читают именно такое сочетание;
 *  4) увеличение мелких обрезков — маленькое разрешение резко снижает
 *     точность распознавания.
 */
object MeterScanner {

    data class ScanResult(val recognizedValue: String?, val rawText: String)

    suspend fun recognizeReading(context: Context, imageUri: Uri): ScanResult {
        val original = loadAndRotateBitmap(context, imageUri)
        val processed = preprocessForOcr(original)

        // Tesseract с whitelist только на цифры обычно точнее на нестандартных
        // шрифтах счётчиков — пробуем его первым, если обученные данные на месте.
        if (TesseractScanner.isAvailable(context)) {
            val tesseractValue = TesseractScanner.recognizeReading(context, processed)
            if (!tesseractValue.isNullOrBlank() && tesseractValue.count(Char::isDigit) >= 2) {
                return ScanResult(tesseractValue, tesseractValue)
            }
        }

        val image = InputImage.fromBitmap(processed, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val visionText = suspendCancellableCoroutine<Text?> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }

        if (visionText == null || visionText.text.isBlank()) {
            // Предобработка иногда "перечищает" изображение — если результата нет,
            // пробуем ещё раз на исходном (просто повёрнутом) фото как запасной вариант.
            return recognizeOnBitmap(original)
        }

        val value = extractMostProminentDigitGroup(visionText)
        if (value != null) return ScanResult(value, visionText.text)
        return recognizeOnBitmap(original)
    }

    private suspend fun recognizeOnBitmap(bitmap: Bitmap): ScanResult {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val visionText = suspendCancellableCoroutine<Text?> { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        if (visionText == null) return ScanResult(null, "")
        return ScanResult(extractMostProminentDigitGroup(visionText), visionText.text)
    }

    /**
     * Проходит по всем распознанным "элементам" (словам) во всех строках,
     * извлекает из них числа и выбирает то, у которого самая высокая рамка
     * (bounding box) — крупный шрифт табло перебивает мелкие серийные номера.
     */
    private fun extractMostProminentDigitGroup(visionText: Text): String? {
        val regex = Regex("""\d[\d.,]*\d|\d""")
        var best: String? = null
        var bestScore = -1.0

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val height = element.boundingBox?.height()?.toDouble() ?: 0.0
                    regex.findAll(element.text).forEach { match ->
                        val digitsOnly = match.value.count(Char::isDigit)
                        if (digitsOnly < 2) return@forEach
                        val score = height * digitsOnly
                        if (score > bestScore) {
                            bestScore = score
                            best = match.value.replace(',', '.').trim('.', ',')
                        }
                    }
                }
            }
        }
        return best
    }

    // ===== Предобработка изображения для повышения точности OCR =====

    private fun preprocessForOcr(source: Bitmap): Bitmap {
        val upscaled = upscaleIfSmall(source)
        val gray = toGrayscale(upscaled)
        return binarizeOtsu(gray, upscaled.width, upscaled.height)
    }

    private fun upscaleIfSmall(bitmap: Bitmap, targetMinWidth: Int = 900): Bitmap {
        if (bitmap.width >= targetMinWidth) return bitmap
        val scale = targetMinWidth.toFloat() / bitmap.width
        val newWidth = targetMinWidth
        val newHeight = (bitmap.height * scale).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun toGrayscale(bitmap: Bitmap): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // Стандартные веса восприятия яркости (luma)
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt().coerceIn(0, 255)
            pixels[i] = gray
        }
        return pixels
    }

    /**
     * Бинаризация методом Оцу: автоматически находит оптимальный порог
     * чёрное/белое по гистограмме яркости, без ручной настройки под
     * конкретное освещение или тип счётчика.
     */
    private fun binarizeOtsu(grayPixels: IntArray, width: Int, height: Int): Bitmap {
        val histogram = IntArray(256)
        for (v in grayPixels) histogram[v]++
        val total = grayPixels.size

        var sum = 0.0
        for (t in 0 until 256) sum += t * histogram[t]

        var sumB = 0.0
        var wB = 0
        var maxVariance = 0.0
        var threshold = 127

        for (t in 0 until 256) {
            wB += histogram[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break

            sumB += t * histogram[t]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val variance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (variance > maxVariance) {
                maxVariance = variance
                threshold = t
            }
        }

        // Считаем, какая доля пикселей темнее порога — если фон преимущественно
        // тёмный (типично для табло счётчика), инвертируем в чёрный-по-белому,
        // как в обычном документе — так текстовые модели читают увереннее.
        var darkCount = 0
        for (v in grayPixels) if (v < threshold) darkCount++
        val invert = darkCount > grayPixels.size / 2

        val out = IntArray(grayPixels.size)
        for (i in grayPixels.indices) {
            val isDark = grayPixels[i] < threshold
            // Текст — всегда МЕНЬШИНСТВО пикселей на фото (табло почти целиком фон).
            // Если фон тёмный (invert=true), текст — это светлые пиксели, и наоборот.
            val isTextPixel = if (invert) !isDark else isDark
            out[i] = if (isTextPixel) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, width, 0, 0, width, height)
        return result
    }

    private fun loadAndRotateBitmap(context: Context, uri: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input)
        input?.close()

        val rotation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }

        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
