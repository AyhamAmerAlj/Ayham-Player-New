package com.ayham.player.data

/** ما الجديد في التحديث: يُحدَّث النص مع كل إصدار. يظهر مرة واحدة لكل إصدار ويمكن إغلاقه. */
object WhatsNew {
    const val id = "222"
    val ar = listOf(
        "مقدمة سينمائية عند تشغيل الفيلم أو الحلقة، مع \"تابع من حيث توقفت\"",
        "خلفية الفيلم المموّهة أثناء التحميل بدل الشاشة السوداء",
        "أنماط ترجمة (سينما/أصفر/خلفية) مع معاينة حية في الإعدادات",
        "بحث أذكى: اقتراحات وفلاتر وأفضل نتيجة بشكل كبير",
        "توهج وعلامات مقدمة على شريط التقدم",
    )
    val en = listOf(
        "Cinematic intro when a movie or episode starts, with resume info",
        "Blurred poster backdrop while loading instead of a black screen",
        "Subtitle styles (Cinema/Yellow/Box) with live preview in settings",
        "Smarter search: suggestions, filters and a big top result",
        "Glow and intro marks on the seek bar",
    )
}

data class WatchWeek(val total: Long, val topName: String, val topSec: Long)
