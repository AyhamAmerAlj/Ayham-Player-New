package com.ayham.player.data

/** اكتشاف أجزاء السلسلة الواحدة من الاسم (Part 2 / II / 3 / الجزء الثاني ...). */
object Franchise {
    class Info(val title: String, val key: String, val n: Int)

    private val roman = mapOf("ii" to 2, "iii" to 3, "iv" to 4, "v" to 5, "vi" to 6, "vii" to 7, "viii" to 8, "ix" to 9, "x" to 10)
    private val ordAr = mapOf("الاول" to 1, "الأول" to 1, "الثاني" to 2, "الثالث" to 3, "الرابع" to 4, "الخامس" to 5, "السادس" to 6, "السابع" to 7, "الثامن" to 8, "التاسع" to 9, "العاشر" to 10)
    private val tailNum = Regex("""^(.*?)[\s:\-–]*(?:part|pt\.?|chapter|vol\.?|volume|الجزء|جزء)?\s*(\d{1,2}|ii|iii|iv|v|vi|vii|viii|ix|x)\s*$""", RegexOption.IGNORE_CASE)
    private val tailAr = Regex("""^(.*?)[\s:\-–]*(?:الجزء|جزء)\s*(${ordAr.keys.joinToString("|")})\s*$""")

    private fun keyOf(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    fun parse(name: String): Info? {
        val s = Imdb.clean(name).trim()
        if (s.length < 2) return null
        tailAr.find(s)?.let { m ->
            val t = m.groupValues[1].trim()
            if (keyOf(t).length >= 2) return Info(t, keyOf(t), ordAr[m.groupValues[2]] ?: 1)
        }
        tailNum.find(s)?.let { m ->
            val t = m.groupValues[1].trim()
            val w = m.groupValues[2].lowercase()
            val n = w.toIntOrNull() ?: roman[w]
            if (n != null && n in 1..30 && keyOf(t).length >= 2) return Info(t, keyOf(t), n)
        }
        return if (keyOf(s).length >= 2) Info(s, keyOf(s), 1) else null
    }
}
