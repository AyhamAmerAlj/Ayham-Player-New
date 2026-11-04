package com.ayham.player.data

/** ما الجديد في التحديث: يُحدَّث النص مع كل إصدار. يظهر مرة واحدة لكل إصدار ويمكن إغلاقه. */
object WhatsNew {
    const val id = "223"
    val ar = listOf(
        "مقدمة سينمائية عند تشغيل فيلم أو حلقة: الاسم والتقييم ورقم الحلقة",
        "عبارة \"تابع من حيث توقفت\" مع الوقت الذي وصلت إليه",
        "صورة الفيلم المموّهة أثناء التحميل بدل الشاشة السوداء",
        "أنماط ترجمة: سينما، أصفر، خلفية، مع معاينة حية في الإعدادات",
        "بحث أذكى: آخر عمليات البحث، الأكثر مشاهدة، فلاتر النوع والتقييم، وأفضل نتيجة بشكل كبير",
        "شريط التقدم: توهج حول المقبض وعلامات لبداية المقدمة ونهايتها (للمسلسلات التي حُفظت مقدمتها)",
        "سجل التحديثات في الإعدادات",
    )
    val en = listOf(
        "Cinematic intro when a movie or episode starts: title, rating and episode number",
        "\"Resuming at\" message with the time you reached",
        "Blurred poster backdrop while loading instead of a black screen",
        "Subtitle styles: Cinema, Yellow, Box, with a live preview in settings",
        "Smarter search: recent searches, most watched, type and rating filters, and a big top result",
        "Seek bar: glow around the thumb and marks for the intro start and end (for series with a saved intro)",
        "Update history in settings",
    )
}

/** سجل التحديثات: يُضاف إصدار جديد في أول القائمة مع كل تحديث. */
data class Release(val id: String, val ar: List<String>, val en: List<String>)

val releaseHistory = listOf(
    Release("223", WhatsNew.ar, WhatsNew.en),
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
