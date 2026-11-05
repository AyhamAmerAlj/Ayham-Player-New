package com.ayham.player.data

/** ما الجديد في التحديث: يُحدَّث النص مع كل إصدار. يظهر مرة واحدة لكل إصدار ويمكن إغلاقه. */
object WhatsNew {
    const val id = "227"
    val ar = listOf(
        "تقييمات متعددة في صفحة الفيلم والمسلسل: IMDb وRotten Tomatoes وMetacritic وTMDB وTrakt (تظهر \"غير متوفر\" عند غياب البيانات)",
        "بحث ذكي بالجملة الطبيعية: \"أفلام رعب بتقييم أعلى من 8\"، \"مسلسلات تشبه ...\"، \"خيال علمي من 2020 إلى 2026\"",
        "البحث بوصف القصة لمن نسي اسم الفيلم",
        "أعمال مشابهة أدق بحسب القصة والأجواء",
    )
    val en = listOf(
        "Multiple ratings on movie and series pages: IMDb, Rotten Tomatoes, Metacritic, TMDB and Trakt (\"N/A\" when unavailable)",
        "Smart natural-language search: \"horror movies rated above 8\", \"series like ...\", \"sci-fi 2020 to 2026\"",
        "Search by describing the plot when you forgot the title",
        "More accurate similar titles by story and mood",
    )
}

/** سجل التحديثات: يُضاف إصدار جديد في أول القائمة مع كل تحديث. */
data class Release(val id: String, val ar: List<String>, val en: List<String>)

val releaseHistory = listOf(
    Release("227", WhatsNew.ar, WhatsNew.en),
    Release("223", listOf(
        "مقدمة سينمائية، خلفية تحميل مموّهة، أنماط ترجمة، بحث أذكى، توهج شريط التقدم، سجل التحديثات",
    ), listOf(
        "Cinematic intro, blurred loading backdrop, subtitle styles, smarter search, seek-bar glow, update history",
    )),
    Release("222", listOf(
        "مقدمة سينمائية عند تشغيل فيلم أو حلقة مع \"تابع من حيث توقفت\"",
        "صورة الفيلم المموّهة أثناء التحميل",
    ), listOf(
        "Cinematic intro when a movie or episode starts, with resume info",
        "Blurred poster backdrop while loading",
    )),
    Release("221", listOf(
        "أنماط ترجمة مع معاينة حية في الإعدادات",
        "بحث أذكى مع اقتراحات وفلاتر وأفضل نتيجة",
        "توهج وعلامات مقدمة على شريط التقدم",
    ), listOf(
        "Subtitle styles with a live preview in settings",
        "Smarter search with suggestions, filters and a top result",
        "Glow and intro marks on the seek bar",
    )),
    Release("206", listOf(
        "شاشة قنوات جديدة للتلفاز: تصنيفات جانبية ولوحة معلومات",
        "القفز لأي قناة برقمها من الريموت",
        "شارات الجودة و LIVE على القنوات",
        "معاينة مصغّرة سينمائية",
        "تذكير انتهاء الاشتراك وإحصائية المشاهدة الأسبوعية",
    ), listOf(
        "New TV channels screen with side categories and info panel",
        "Jump to any channel by number",
        "Quality and LIVE badges",
        "Cinematic mini preview",
        "Expiry reminder and weekly watch stats",
    )),
)

data class WatchWeek(val total: Long, val topName: String, val topSec: Long)
