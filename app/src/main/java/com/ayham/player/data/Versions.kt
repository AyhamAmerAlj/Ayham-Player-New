package com.ayham.player.data

/** تجميع نسخ نفس العنوان (مثل 4K-AR - FROM و 4K-DE - From) من قائمة المزوّد. */
object Versions {
    private val prefixRe = Regex("""^\s*([^-:|\[\]]{1,14}(?:-[^-:|\[\]\s]{1,10}){0,3})\s*[-:|]\s+(.+)$""")
    private val parenRe = Regex("""\s*[\(\[][^\)\]]*[\)\]]""")

    /** عنوان نظيف للمقارنة (بدون وسم الجودة/اللغة والسنة والبلد). */
    fun core(name: String): String {
        var n = name
        val m = prefixRe.find(n)
        if (m != null) n = m.groupValues[2]
        n = parenRe.replace(n, "")
        return n.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
    }

    /** وسم النسخة: الجزء قبل أول " - " مثل 4K-AR. */
    fun tag(name: String): String =
        prefixRe.find(name)?.groupValues?.get(1)?.trim()?.replace("-", " · ")?.uppercase().orEmpty()
}
