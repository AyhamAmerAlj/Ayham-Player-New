package com.ayham.player.ui

import java.util.Calendar
import java.util.TimeZone

/** المناسبات المؤقتة. تنتهي تلقائيًا بالتاريخ بدون تحديث جديد. */
object Season {
    /** هالوين 2026: من 31 أكتوبر 00:00 إلى 2 نوفمبر 23:59 بتوقيت بغداد (3 أيام). */
    val halloween: Boolean by lazy {
        val tz = TimeZone.getTimeZone("Asia/Baghdad")
        val now = Calendar.getInstance(tz).timeInMillis
        fun at(m: Int, d: Int, h: Int, mi: Int, s: Int) = Calendar.getInstance(tz).apply { set(2026, m, d, h, mi, s); set(Calendar.MILLISECOND, 0) }.timeInMillis
        now in at(Calendar.OCTOBER, 31, 0, 0, 0)..at(Calendar.NOVEMBER, 2, 23, 59, 59)
    }
}
