package com.ayham.player.data

/** ما الجديد في التحديث: يُحدَّث النص مع كل إصدار. يظهر مرة واحدة لكل إصدار ويمكن إغلاقه. */
object WhatsNew {
    const val id = "206"
    val ar = listOf(
        "شاشة قنوات جديدة للتلفاز: تصنيفات جانبية ولوحة معلومات",
        "القفز لأي قناة برقمها من الريموت",
        "شارات الجودة و LIVE على القنوات",
        "معاينة مصغّرة سينمائية، ويرجع التركيز لنفس القناة بعد المشاهدة",
        "تذكير انتهاء الاشتراك وإحصائية المشاهدة الأسبوعية",
    )
    val en = listOf(
        "New TV channels screen: side categories and an info panel",
        "Jump to any channel by typing its number on the remote",
        "Quality and LIVE badges on channels",
        "Cinematic mini preview, and focus returns to the same channel",
        "Subscription expiry reminder and weekly watch stats",
    )
}

data class WatchWeek(val total: Long, val topName: String, val topSec: Long)
