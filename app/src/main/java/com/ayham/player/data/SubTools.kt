package com.ayham.player.data

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipInputStream

/** أدوات ملفات الترجمة: فك الضغط وتحويل الترميز (العربي القديم windows-1256 إلى UTF-8). */
object SubTools {
    private val exts = listOf("srt", "vtt", "ass", "ssa")

    private fun extOf(name: String): String =
        name.substringBefore('?').substringAfterLast('.', "").lowercase()

    fun toUtf8(bytes: ByteArray): ByteArray {
        var b = bytes
        if (b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte()) {
            return b.copyOfRange(3, b.size)
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(b))
            b
        } catch (_: CharacterCodingException) {
            val cs = runCatching { Charset.forName("windows-1256") }.getOrDefault(Charsets.ISO_8859_1)
            String(b, cs).toByteArray(Charsets.UTF_8)
        }
    }

    /** يكتب الملف في الكاش ويرجعه، أو null لو النوع غير مدعوم. */
    fun fromUpload(dir: File, name: String, bytes: ByteArray, clean: Boolean = true): File? {
        return runCatching {
            var fileBytes = bytes
            var ext = extOf(name)
            if (ext == "zip") {
                var found: Pair<String, ByteArray>? = null
                ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                    var e = z.nextEntry
                    while (e != null && found == null) {
                        val en = extOf(e.name)
                        if (!e.isDirectory && en in exts) found = en to z.readBytes()
                        e = z.nextEntry
                    }
                }
                val f = found ?: return null
                ext = f.first
                fileBytes = f.second
            }
            if (ext !in exts) return null
            dir.mkdirs()
            if (clean) dir.listFiles { x -> x.name.startsWith("sub_") }?.forEach { it.delete() }
            val out = File(dir, "sub_" + System.currentTimeMillis() + "." + ext)
            out.writeBytes(toUtf8(fileBytes))
            out
        }.getOrNull()
    }
}
