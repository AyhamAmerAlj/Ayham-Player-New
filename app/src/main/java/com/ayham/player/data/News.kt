package com.ayham.player.data

import com.ayham.player.BuildConfig
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** ميزة واحدة داخل تحديث (أيقونة، عنوان، وصف، صورة اختيارية). */
data class NewsFeature(
    val icon: String, val title: String, val desc: String,
    val titleEn: String, val descEn: String, val image: String,
)

/** تحديث "ما الجديد؟" كما يرسله الخادم (يُدار من لوحة الإدارة). */
data class NewsEntry(
    val id: String, val version: String, val build: Int, val date: String,
    val title: String, val desc: String, val titleEn: String, val descEn: String,
    val items: List<NewsFeature>,
)

object NewsApi {
    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()

    /** null = تعذّر الجلب (بدون إنترنت مثلًا). لا يؤثر على سرعة التطبيق: يُستدعى بخيط خلفي مرة واحدة. */
    fun fetch(): List<NewsEntry>? {
        if (BuildConfig.LICENSE_URL.isBlank()) return null
        return try {
            val req = Request.Builder().url(BuildConfig.LICENSE_URL.trimEnd('/') + "/v1/news")
                .post("{}".toRequestBody("application/json".toMediaType())).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return null
                val o = JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
                if (o.get("ok")?.asBoolean != true) return null
                o.getAsJsonArray("items").map { e ->
                    val n = e.asJsonObject
                    fun s(k: String) = n.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
                    NewsEntry(
                        s("id"), s("version"), n.get("build")?.takeIf { !it.isJsonNull }?.asInt ?: 0, s("date"),
                        s("title"), s("desc"), s("title_en"), s("desc_en"),
                        n.getAsJsonArray("items")?.map { x ->
                            val i = x.asJsonObject
                            fun f(k: String) = i.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
                            NewsFeature(f("icon"), f("title"), f("desc"), f("title_en"), f("desc_en"), f("image"))
                        } ?: emptyList(),
                    )
                }
            }
        } catch (e: Exception) { null }
    }
}
