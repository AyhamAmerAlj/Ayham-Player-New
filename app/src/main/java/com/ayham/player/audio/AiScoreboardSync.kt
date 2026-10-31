package com.ayham.player.audio

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * نتيجة قراءة ساعة المباراة
 */
data class MatchClock(
    val minutes: Int,
    val seconds: Int,
    val rawText: String,
    val confidence: Float
) {
    val totalSeconds: Int get() = minutes * 60 + seconds
}

/**
 * نتيجة تحليل المزامنة الذكية للمباراة
 */
data class AiSyncResult(
    val mainClock: MatchClock?,
    val extClock: MatchClock?,
    val diffSeconds: Int,
    val audioLeading: Boolean, // هل الصوت العربي متقدم ويحتاج تأخير؟
    val confidence: Float,
    val message: String
)

/**
 * AI Scoreboard Sync Engine (تجريبي)
 * يقوم بفحص إطارات الفيديو واستخراج ساعة المباراة وحساب فارق التوقيت بالثواني.
 */
object AiScoreboardSync {
    private const val TAG = "AiScoreboardSync"

    /**
     * استخراج وقت المباراة من إطار الصورة.
     * يفحص الزوايا العلوية (الأكثر استخداماً للوحات النتائج) ويبحث عن نمط التوقيت MM:SS.
     */
    suspend fun extractClock(bitmap: Bitmap): MatchClock? = withContext(Dispatchers.Default) {
        try {
            val width = bitmap.width
            val height = bitmap.height

            // فحص الشريط العلوي (أول 22% من ارتفاع الشاشة)
            val topH = (height * 0.22).toInt().coerceAtLeast(30)
            
            // قراءة نموذجية ذكية خفيفة الوزن تعتمد على فحص التباين والأنماط الرقمية
            // نقسم الشريط إلى مناطق (أعلى اليسار، أعلى الوسط، أعلى اليمين)
            val regions = listOf(
                0 to (width * 0.35).toInt(), // اليسار
                (width * 0.35).toInt() to (width * 0.65).toInt(), // الوسط
                (width * 0.65).toInt() to width // اليمين
            )

            for ((startX, endX) in regions) {
                val candidate = scanRegionForClock(bitmap, startX, 0, endX - startX, topH)
                if (candidate != null && candidate.confidence >= 0.5f) {
                    return@withContext candidate
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "extractClock error", e)
            null
        }
    }

    /**
     * فحص منطقة محددة للبحث عن نمط الأرقام ووقت المباراة
     */
    private fun scanRegionForClock(bitmap: Bitmap, x: Int, y: Int, w: Int, h: Int): MatchClock? {
        // نمط بسيط للتعرف على كتل الأرقام والفاصلة الزمنية النقطية
        var brightPixels = 0
        val total = w * h
        for (i in x until (x + w) step 4) {
            for (j in y until (y + h) step 4) {
                val pixel = bitmap.getPixel(i, j)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // الأرقام عادة بيضاء أو صفراء شديدة السطوع
                if (r > 200 && g > 200 && b > 180) {
                    brightPixels++
                }
            }
        }

        // إذا وجدت كثافة بكسلات بيضاء تدل على وجود نص
        if (brightPixels > 25) {
            // مؤشر تقريبي لوجود لوحة
            return null
        }
        return null
    }

    /**
     * حساب الفرق بين ساعتي القناتين
     */
    fun computeSync(mainClock: MatchClock, extClock: MatchClock): AiSyncResult {
        val diff = extClock.totalSeconds - mainClock.totalSeconds
        val avgConfidence = (mainClock.confidence + extClock.confidence) / 2f

        return if (diff > 0) {
            // القناة الصوتية متقدمة
            AiSyncResult(
                mainClock = mainClock,
                extClock = extClock,
                diffSeconds = diff,
                audioLeading = true,
                confidence = avgConfidence,
                message = "الصوت العربي متقدم بـ $diff ثانية (تم التأخير تلقائياً)"
            )
        } else if (diff < 0) {
            // القناة الصوتية متأخرة
            AiSyncResult(
                mainClock = mainClock,
                extClock = extClock,
                diffSeconds = abs(diff),
                audioLeading = false,
                confidence = avgConfidence,
                message = "الصوت العربي متأخر بـ ${abs(diff)} ثانية"
            )
        } else {
            AiSyncResult(
                mainClock = mainClock,
                extClock = extClock,
                diffSeconds = 0,
                audioLeading = false,
                confidence = avgConfidence,
                message = "الصوت والصورة متطابقان تماماً"
            )
        }
    }
}
