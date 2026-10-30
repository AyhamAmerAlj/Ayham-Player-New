package com.ayham.player.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ayham.player.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class LicState { CHECKING, OK, NEED_CODE }

/**
 * تفعيل التطبيق بكود. إذا LICENSE_URL فارغ (لم يُضبط وقت البناء) يعمل التطبيق بدون تفعيل.
 * الكود يسمح بجهازين، وجهاز واحد فقط بنفس الوقت (يتحقق منه الخادم).
 */
object LicenseManager {
    val enabled: Boolean get() = BuildConfig.LICENSE_URL.isNotBlank()
    var state by mutableStateOf(if (BuildConfig.LICENSE_URL.isBlank()) LicState.OK else LicState.CHECKING)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    private const val GRACE_MS = 72L * 3600 * 1000 // يسمح بالعمل بدون إنترنت 72 ساعة بعد آخر تحقق ناجح
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
    private val JSON = "application/json".toMediaType()
    private lateinit var appCtx: Context
    private val sp get() = appCtx.getSharedPreferences("ayham_license", Context.MODE_PRIVATE)

    fun deviceId(): String {
        @Suppress("HardwareIds")
        val a = Settings.Secure.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val d = MessageDigest.getInstance("SHA-256").digest((a + "|" + appCtx.packageName).toByteArray())
        return d.take(16).joinToString("") { "%02x".format(it) }
    }

    private sealed class R {
        data object Ok : R()
        data class Denied(val err: String) : R()
        data object Offline : R()
    }

    private suspend fun call(path: String, code: String): R = withContext(Dispatchers.IO) {
        try {
            val body = Gson().toJson(mapOf("code" to code, "device" to deviceId(), "name" to "${Build.MANUFACTURER} ${Build.MODEL}".take(40)))
            val req = Request.Builder().url(BuildConfig.LICENSE_URL.trimEnd('/') + path).post(body.toRequestBody(JSON)).build()
            client.newCall(req).execute().use { r ->
                if (r.code >= 500) return@use R.Offline
                val o = JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
                if (o.get("ok")?.asBoolean == true) R.Ok else R.Denied(o.get("err")?.asString ?: "invalid")
            }
        } catch (e: Exception) {
            R.Offline
        }
    }

    private fun msg(err: String) = when (err) {
        "invalid" -> "الكود غير صحيح"
        "revoked" -> "هذا الكود تم إيقافه"
        "devices_full" -> "هذا الكود مستخدم على الحد الأقصى من الأجهزة"
        "busy" -> "هذا الكود يعمل الآن على جهاز آخر. أغلقه هناك وجرّب بعد دقائق"
        else -> "تعذّر التحقق"
    }

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    /** الكود المحفوظ (للمزامنة). */
    fun savedCode(): String? = if (::appCtx.isInitialized) sp.getString("code", null) else null

    val isOk: Boolean get() = state == LicState.OK

    val hasSaved: Boolean get() = ::appCtx.isInitialized && sp.getString("code", null) != null

    /** إعادة محاولة تلقائية بالكود المحفوظ (مثلًا لما يكون الكود شغّال على جهاز آخر). */
    suspend fun retry() {
        val c = sp.getString("code", null) ?: return
        if (!busy) verify(c, manual = true)
    }

    /** عند فتح التطبيق. */
    suspend fun start() {
        if (!enabled) { state = LicState.OK; return }
        val code = sp.getString("code", null)
        if (code == null) { state = LicState.NEED_CODE; return }
        verify(code, manual = false)
    }

    suspend fun activate(input: String) {
        if (!enabled) return
        val code = input.uppercase().filter { it.isLetterOrDigit() }
        if (code.length != 12) { error = "اكتب الكود كاملًا (12 حرفًا)"; return }
        busy = true
        verify(code, manual = true)
        busy = false
    }

    private suspend fun verify(code: String, manual: Boolean) {
        when (val r = call("/v1/check", code)) {
            is R.Ok -> {
                sp.edit().putString("code", code).putLong("last_ok", System.currentTimeMillis()).apply()
                error = null
                state = LicState.OK
            }
            is R.Denied -> {
                if (r.err == "invalid" || r.err == "revoked") sp.edit().remove("code").apply()
                error = msg(r.err)
                state = LicState.NEED_CODE
            }
            is R.Offline -> {
                val within = System.currentTimeMillis() - sp.getLong("last_ok", 0L) < GRACE_MS
                if (!manual && within) { state = LicState.OK } else {
                    error = if (manual) "تعذّر الاتصال بالخادم، تأكد من الإنترنت" else "يلزم اتصال بالإنترنت للتحقق من الكود"
                    state = LicState.NEED_CODE
                }
            }
        }
    }

    /** نبضة دورية أثناء التشغيل. */
    suspend fun heartbeat() {
        if (!enabled) return
        while (true) {
            delay(4 * 60 * 1000L)
            val code = sp.getString("code", null) ?: continue
            if (state != LicState.OK) continue
            verify(code, manual = false)
        }
    }

    /** عند إغلاق التطبيق نحرّر الجلسة حتى يشتغل على جهاز آخر فورًا. */
    fun release() {
        if (!enabled) return
        val code = sp.getString("code", null) ?: return
        Thread { runCatching { kotlinx.coroutines.runBlocking { call("/v1/release", code) } } }.start()
    }

    fun forget() { sp.edit().remove("code").apply(); state = LicState.NEED_CODE; error = null }
}
