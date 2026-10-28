package com.ayham.player.data

enum class Kind { LIVE, MOVIE, SERIES }

data class Item(
    val kind: Kind,
    val id: String,
    val name: String,
    val image: String = "",
    val categoryId: String = "",
    val rating: String = "",
    val ext: String = "",
    val added: Long = 0L,
    val rid: Long = 0L,
) {
    val key: String get() = "${kind.name}:$id"
}

data class Category(val id: String, val name: String, val count: Int = 0)

data class Details(
    val plot: String = "",
    val cast: String = "",
    val genre: String = "",
    val year: String = "",
    val duration: String = "",
    val rating: String = "",
    val backdrop: String = "",
    val trailer: String = "",
)

data class Episode(
    val id: String,
    val season: Int,
    val num: Int,
    val title: String,
    val ext: String,
    val image: String,
    val plot: String,
    val duration: String,
) {
    val key: String get() = "EP:$id"
}

data class SeriesInfo(
    val details: Details,
    val seasons: Map<Int, List<Episode>>,
)

/** ترجمة أضافها المستخدم لعمل معيّن (ملف محفوظ داخل التطبيق أو رابط). */
data class SavedSub(val label: String, val uri: String)

data class EpgEntry(val title: String, val desc: String, val start: Long, val end: Long)

data class Account(val server: String, val user: String, val pass: String)

data class AccountInfo(
    val status: String = "",
    val expDate: String = "",
    val maxConnections: String = "",
)

/** مصدر قائمة القنوات: قسم معيّن (أو الكل)، أو قائمة جاهزة (مثل المفضلة ونتائج البحث). */
data class LiveSource(val cat: String? = null, val favs: List<Item>? = null)

/** عنصر واحد في قائمة التشغيل (قناة أو حلقة أو فيلم). */
data class QueueEntry(
    val title: String,
    val url: String,
    val key: String,
    val image: String = "",
    val isLive: Boolean = false,
)

data class PlayRequest(
    val queue: List<QueueEntry>,
    val index: Int,
    val item: Item? = null,
)
