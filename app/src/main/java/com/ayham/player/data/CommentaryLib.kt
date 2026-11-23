package com.ayham.player.data

import android.content.Context
import java.io.File
import java.io.InputStream

/** مكتبة ملفات التعليق الصوتية المحفوظة داخل التطبيق (تُضاف من الهاتف عبر QR وتبقى محفوظة). */
object CommentaryLib {
    const val MAX_BYTES = 400L * 1024 * 1024
    private val EXTS = setOf("mp3", "m4a", "aac", "wav", "ogg", "oga", "opus", "flac", "mka", "mp4", "m4b", "webm", "ts", "3gp", "amr")

    fun dir(ctx: Context): File = File(ctx.filesDir, "commentary").also { it.mkdirs() }

    fun list(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.isFile && !f.name.endsWith(".part") }?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** أي صيغة مقبولة (تشمل cfg وغيرها). */
    fun isAudioName(@Suppress("UNUSED_PARAMETER") name: String) = true

    private fun clean(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        val c = base.replace(Regex("[^\\p{L}\\p{N}._ \\-()]"), "_").trim().take(80)
        return c.ifBlank { "commentary.bin" }.let { if (it.contains('.')) it else "$it.bin" }
    }

    /** يحفظ الملف من مجرى الاتصال مباشرة على القرص. يرجع الملف أو null عند الفشل. */
    fun save(ctx: Context, rawName: String, len: Long, input: InputStream): File? {
        if (len <= 0 || len > MAX_BYTES) return null
        var name = clean(rawName)
        if (!isAudioName(name)) return null
        val d = dir(ctx)
        var target = File(d, name)
        var n = 1
        while (target.exists()) {
            val stem = name.substringBeforeLast('.'); val ext = name.substringAfterLast('.')
            target = File(d, "$stem ($n).$ext"); n++
        }
        val tmp = File(d, target.name + ".part")
        return try {
            tmp.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var left = len
                while (left > 0) {
                    val r = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                    if (r < 0) break
                    o.write(buf, 0, r); left -= r
                }
                if (left > 0) throw java.io.IOException("short")
            }
            if (tmp.renameTo(target)) target else null
        } catch (_: Exception) { tmp.delete(); null }
    }

    fun delete(f: File) { runCatching { f.delete() } }

    fun sizeText(f: File): String {
        val mb = f.length() / 1048576.0
        return if (mb >= 1) String.format(java.util.Locale.US, "%.1f MB", mb) else (f.length() / 1024).toString() + " KB"
    }
}
