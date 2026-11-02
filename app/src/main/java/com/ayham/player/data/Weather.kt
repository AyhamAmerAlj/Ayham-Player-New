package com.ayham.player.data

import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

data class City(val ar: String, val en: String, val lat: Double, val lon: Double)

/** الطقس الحالي من Open-Meteo (بدون مفتاح). أي فشل يُرجع null فتُخفى الخانة، ولا تُعرض قيمة وهمية. */
object Weather {
    data class W(val temp: Int, val code: Int, val day: Boolean)

    val cities = listOf(
        City("بغداد", "Baghdad", 33.31, 44.36), City("الرمادي (الأنبار)", "Ramadi (Anbar)", 33.42, 43.30),
        City("الفلوجة", "Fallujah", 33.35, 43.78), City("الموصل", "Mosul", 36.34, 43.13),
        City("البصرة", "Basra", 30.51, 47.78), City("أربيل", "Erbil", 36.19, 44.01),
        City("النجف", "Najaf", 32.00, 44.34), City("كربلاء", "Karbala", 32.62, 44.02),
        City("السليمانية", "Sulaymaniyah", 35.56, 45.43), City("كركوك", "Kirkuk", 35.47, 44.39),
        City("دهوك", "Duhok", 36.87, 42.99), City("الحلة", "Hilla", 32.48, 44.43),
        City("الناصرية", "Nasiriyah", 31.04, 46.26), City("بعقوبة (ديالى)", "Baqubah (Diyala)", 33.75, 44.64),
        City("تكريت", "Tikrit", 34.61, 43.68), City("الكوت", "Kut", 32.51, 45.82),
        City("العمارة", "Amarah", 31.84, 47.14), City("السماوة", "Samawah", 31.31, 45.28),
        City("الديوانية", "Diwaniyah", 31.99, 44.92), City("دمشق", "Damascus", 33.51, 36.29),
        City("عمّان", "Amman", 31.95, 35.93), City("الرياض", "Riyadh", 24.71, 46.68),
        City("الكويت", "Kuwait City", 29.38, 47.99), City("دبي", "Dubai", 25.20, 55.27),
        City("القاهرة", "Cairo", 30.04, 31.24), City("بيروت", "Beirut", 33.89, 35.50),
    )

    private const val TTL = 30L * 60 * 1000
    private val client = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
    private val cache = HashMap<Int, Pair<Long, W>>()

    @Synchronized
    fun fetch(cityIdx: Int): W? {
        val i = cityIdx.coerceIn(0, cities.lastIndex)
        val now = System.currentTimeMillis()
        cache[i]?.let { if (now - it.first < TTL) return it.second }
        val c = cities[i]
        return runCatching {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lon}&current=temperature_2m,weather_code,is_day&timezone=auto"
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val cur = JsonParser.parseString(r.body?.string() ?: return@use null).asJsonObject.getAsJsonObject("current")
                W(Math.round(cur.get("temperature_2m").asDouble).toInt(), cur.get("weather_code").asInt, cur.get("is_day").asInt == 1)
            }
        }.getOrNull()?.also { cache[i] = now to it } ?: cache[i]?.second
    }
}
