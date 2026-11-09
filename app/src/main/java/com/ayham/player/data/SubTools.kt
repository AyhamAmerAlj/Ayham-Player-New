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
    /** صيغ يقرؤها المشغّل مباشرة. dfxp/xml تُعامل كـ ttml. */
    private val native = setOf("srt", "vtt", "ass", "ssa", "ttml", "dfxp", "xml", "sup")
    /** صيغ نحوّلها إلى srt بأنفسنا. */
    private val convert = setOf("sub", "sbv", "smi", "sami", "lrc", "txt", "stl", "ssf")
    private val exts = native + convert

    private fun extOf(name: String): String =
        name.substringBefore('?').substringAfterLast('.', "").lowercase()

    fun toUtf8(bytes: ByteArray): ByteArray {
        var b = bytes
        // UTF-16 (شائع في ملفات smi/ass القديمة)
        if (b.size >= 2 && ((b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte()) || (b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte()))) {
            return String(b, Charsets.UTF_16).toByteArray(Charsets.UTF_8)
        }
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
            if (ext == "gz") {
                fileBytes = java.util.zip.GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                ext = extOf(name.removeSuffix(".gz").removeSuffix(".GZ"))
            }
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
            // VobSub الثنائي (.sub مع .idx) صور وليس نصاً: غير مدعوم
            if (fileBytes.size > 4 && fileBytes[0] == 0.toByte() && fileBytes[1] == 0.toByte() && fileBytes[2] == 1.toByte() && fileBytes[3] == 0xBA.toByte()) return null
            val outBytes: ByteArray
            if (ext in convert) {
                val text = String(toUtf8(fileBytes), Charsets.UTF_8)
                val srt = convertToSrt(text, ext) ?: return null
                outBytes = srt.toByteArray(Charsets.UTF_8)
                ext = "srt"
            } else if (ext == "sup") {
                outBytes = fileBytes
            } else {
                outBytes = toUtf8(fileBytes)
                if (ext == "dfxp" || ext == "xml") ext = "ttml"
            }
            dir.mkdirs()
            if (clean) dir.listFiles { x -> x.name.startsWith("sub_") }?.forEach { it.delete() }
            val out = File(dir, "sub_" + System.currentTimeMillis() + "." + ext)
            out.writeBytes(outBytes)
            out
        }.getOrNull()
    }

    // ================= تحويل الصيغ غير المدعومة إلى SRT =================
    private class Cue(val s: Long, val e: Long, val t: String)

    private fun stamp(ms: Long): String =
        String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)

    private fun build(cues: List<Cue>): String? {
        val ok = cues.filter { it.t.isNotBlank() && it.e > it.s }.sortedBy { it.s }
        if (ok.isEmpty()) return null
        return ok.mapIndexed { i, c -> "${i + 1}\n${stamp(c.s)} --> ${stamp(c.e)}\n${c.t.trim()}\n" }.joinToString("\n")
    }

    private fun clean(t: String): String = t.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace(Regex("\\{[^}]*\\}"), "").lines().joinToString("\n") { it.trim() }.trim()

    private val timeRange = Regex("(\\d+):(\\d{2}):(\\d{2})[.,](\\d{1,3})\\s*,\\s*(\\d+):(\\d{2}):(\\d{2})[.,](\\d{1,3})")
    private fun tms(h: String, m: String, s: String, f: String): Long =
        h.toLong() * 3_600_000 + m.toLong() * 60_000 + s.toLong() * 1000 + f.padEnd(3, '0').take(3).toLong()

    /** يتعرّف على الصيغة من المحتوى (مفيد لملفات txt) ثم يحوّلها. */
    private fun convertToSrt(text: String, ext: String): String? {
        val kind = when {
            Regex("(?im)^\\s*\\{\\d+\\}\\{\\d+\\}").containsMatchIn(text) -> "microdvd"
            Regex("(?i)<sami").containsMatchIn(text) || Regex("(?i)<sync\\s+start").containsMatchIn(text) -> "sami"
            text.contains("-->") -> "srt"
            timeRange.containsMatchIn(text) -> "sbv"
            Regex("(?m)^\\[\\d+:\\d+([.:]\\d+)?]").containsMatchIn(text) -> "lrc"
            ext == "smi" || ext == "sami" -> "sami"
            else -> return null
        }
        return when (kind) {
            "srt" -> text
            "microdvd" -> microDvd(text)
            "sami" -> sami(text)
            "sbv" -> subViewer(text)
            else -> lrc(text)
        }
    }

    private fun microDvd(text: String): String? {
        var fps = 23.976
        val cues = ArrayList<Cue>()
        val re = Regex("^\\{(\\d+)\\}\\{(\\d+)\\}(.*)$")
        for (line in text.lines()) {
            val m = re.find(line.trim()) ?: continue
            val a = m.groupValues[1].toLong(); val b = m.groupValues[2].toLong(); val t = m.groupValues[3]
            if (a == 1L && b == 1L) { t.trim().toDoubleOrNull()?.takeIf { it in 5.0..120.0 }?.let { fps = it }; continue }
            cues.add(Cue((a * 1000 / fps).toLong(), (b * 1000 / fps).toLong(), clean(t.replace("|", "\n"))))
        }
        return build(cues)
    }

    private fun subViewer(text: String): String? {
        val cues = ArrayList<Cue>()
        val lines = text.lines()
        var i = 0
        while (i < lines.size) {
            val m = timeRange.find(lines[i])
            if (m != null) {
                val g = m.groupValues
                val sb = StringBuilder()
                i++
                while (i < lines.size && lines[i].isNotBlank()) { sb.append(lines[i].replace("[br]", "\n")).append('\n'); i++ }
                cues.add(Cue(tms(g[1], g[2], g[3], g[4]), tms(g[5], g[6], g[7], g[8]), clean(sb.toString())))
            } else i++
        }
        return build(cues)
    }

    private fun sami(text: String): String? {
        val syncs = Regex("(?is)<sync\\s+start\\s*=\\s*\"?(\\d+)\"?[^>]*>(.*?)(?=<sync\\s|</body|\\z)").findAll(text).toList()
        if (syncs.isEmpty()) return null
        // لغات متعددة: نفضّل الصنف الذي فيه عربي، وإلا الأول
        fun cls(block: String) = Regex("(?is)<p[^>]*class\\s*=\\s*\"?([A-Za-z0-9_-]+)\"?[^>]*>(.*?)(?=<p\\b|\\z)").findAll(block).map { it.groupValues[1] to it.groupValues[2] }.toList()
        val all = syncs.flatMap { cls(it.groupValues[2]) }
        val arCls = all.firstOrNull { p -> clean(p.second).any { it in '\u0600'..'\u06FF' } }?.first ?: all.firstOrNull()?.first
        val cues = ArrayList<Cue>()
        for ((i, m) in syncs.withIndex()) {
            val start = m.groupValues[1].toLong()
            val block = m.groupValues[2]
            val paras = cls(block)
            val raw = if (paras.isEmpty()) block else paras.firstOrNull { it.first == arCls }?.second ?: ""
            val t = clean(raw.replace(Regex("(?i)<br\\s*/?>"), "\n"))
            val end = syncs.getOrNull(i + 1)?.groupValues?.get(1)?.toLong() ?: (start + 3000)
            if (t.isNotBlank()) cues.add(Cue(start, end, t))
        }
        return build(cues)
    }

    private fun lrc(text: String): String? {
        val re = Regex("^\\[(\\d+):(\\d+)(?:[.:](\\d+))?](.*)$")
        val rows = text.lines().mapNotNull { re.find(it.trim()) }.map {
            val g = it.groupValues
            (g[1].toLong() * 60_000 + g[2].toLong() * 1000 + (g[3].ifEmpty { "0" }.padEnd(3, '0').take(3).toLong())) to clean(g[4])
        }
        val cues = rows.mapIndexed { i, r -> Cue(r.first, rows.getOrNull(i + 1)?.first ?: (r.first + 4000), r.second) }
        return build(cues)
    }
}
