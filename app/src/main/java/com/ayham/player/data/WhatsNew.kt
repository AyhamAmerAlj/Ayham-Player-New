package com.ayham.player.data

/** ما الجديد في التحديث: يُحدَّث النص مع كل إصدار. يظهر مرة واحدة لكل إصدار ويمكن إغلاقه. */
object WhatsNew {
    const val id = "228"
    val ar = listOf(
        "Ayham Vision: أوضاع صورة (سينما، مسلسلات، رياضة، كرة القدم، HDR، مخصص) من قائمة المشغّل",
        "تأثير حقيقي على الصورة: سطوع، تباين، تشبّع، صبغة، حرارة اللون",
        "تفعيل تلقائي حسب نوع المحتوى، وإنشاء أوضاع باسمك وحفظها",
        "محتوى HDR يبقى بصورته الأصلية بلا فلاتر",
        "تقييمات متعددة وبحث ذكي بالجملة ووصف القصة",
    )
    val en = listOf(
        "Ayham Vision: picture modes (Cinema, Series, Sports, Football, HDR, Custom) from the player menu",
        "Real picture effect: brightness, contrast, saturation, hue, color temperature",
        "Auto mode by content type, plus your own named modes",
        "HDR content keeps its native picture, no filters",
        "Multiple ratings and smart natural-language and plot search",
    )
}

/** سجل التحديثات: يُضاف إصدار جديد في أول القائمة مع كل تحديث. */
data class Release(val id: String, val ar: List<String>, val en: List<String>)

val releaseHistory = listOf(
    Release("228", WhatsNew.ar, WhatsNew.en),
    Release("227", listOf(
        "تقييمات متعددة (IMDb وRotten Tomatoes وMetacritic وTMDB وTrakt)",
        "بحث ذكي بالجملة الطبيعية ووصف القصة، وأعمال مشابهة أدق",
    ), listOf(
        "Multiple ratings (IMDb, Rotten Tomatoes, Metacritic, TMDB, Trakt)",
        "Smart natural-language and plot search, more accurate similar titles",
    )),
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
