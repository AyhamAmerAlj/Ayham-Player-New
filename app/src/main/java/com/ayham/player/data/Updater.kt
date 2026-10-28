package com.ayham.player.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(val build: Int, val name: String, val url: String, val notes: String)

/** تحديث التطبيق من صفحة الإصدارات على GitHub (نفس مفتاح التوقيع فيثبّت فوق القديم). */
object Updater {
    private const val API = "https://api.github.com/repos/AyhamAmerAlj/Ayham-Player-New/releases/latest"

    @Suppress("DEPRECATION")
    fun currentBuild(ctx: Context): Int {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else pi.versionCode
    }

    fun check(client: OkHttpClient): UpdateInfo? {
        val req = Request.Builder().url(API).header("Accept", "application/vnd.github+json").header("User-Agent", "AyhamPlayer").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val o = JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
            val tag = o.get("tag_name")?.asString.orEmpty()
            val n = tag.substringAfterLast('-').toIntOrNull() ?: return null
            val asset = o.getAsJsonArray("assets")?.map { it.asJsonObject }
                ?.firstOrNull { it.get("name")?.asString.orEmpty().endsWith(".apk") } ?: return null
            return UpdateInfo(
                n,
                o.get("name")?.asString.orEmpty(),
                asset.get("browser_download_url").asString,
                o.get("body")?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
            )
        }
    }

    fun download(client: OkHttpClient, url: String, dest: File, onProgress: (Float) -> Unit) {
        dest.parentFile?.mkdirs()
        client.newCall(Request.Builder().url(url).header("User-Agent", "AyhamPlayer").build()).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
            val body = r.body ?: throw IllegalStateException("empty")
            val total = body.contentLength().coerceAtLeast(1L)
            var done = 0L
            body.byteStream().use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var last = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - last > 250) { last = now; onProgress((done.toFloat() / total).coerceIn(0f, 1f)) }
                    }
                }
            }
            onProgress(1f)
        }
    }

    /** يفتح شاشة التثبيت. لو التطبيق غير مسموح له بالتثبيت يفتح صفحة السماح أولًا. */
    fun install(ctx: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}
