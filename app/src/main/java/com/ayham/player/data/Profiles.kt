package com.ayham.player.data

/** شاشات نظام الملفات الشخصية (تُدار بمكدّس صغير داخل AppViewModel). */
enum class ProfileUi { PICK, MANAGE, EDIT }

/**
 * ملف شخصي. type: MAIN / PERSONAL / KIDS / GUEST. كل الحقول لها قيم افتراضية حتى يقرأ Gson
 * أي نسخة قديمة محفوظة بدون انهيار.
 */
data class Profile(
    val id: String = "",
    val name: String = "",
    val type: String = "PERSONAL",
    /** رقم الإيموجي من مكتبة الصور، أو -1 لصورة من الجهاز. */
    val avatar: Int = 0,
    val style: Int = 1,
    /** -1 = لون النمط، وإلا رقم اللون من لوحة الألوان. */
    val color: Int = -1,
    val bg: Int = 1,
    val frame: Int = 1,
    val tagline: String = "",
    /** SHA-256 للرمز أو فاضي لو بدون حماية. */
    val pin: String = "",
    val hidden: Boolean = false,
    /** 7 = أطفال فقط، 12 = معتدل، 16 = حجب فئات الكبار فقط. */
    val kidsLevel: Int = 7,
    /** معرّف مساحة المزامنة على الخادم (للرئيسي: اسمه القديم حتى لا تنقطع مزامنته). */
    val syncId: String = "",
)

object Pins {
    fun hash(id: String, pin: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest("ayham:$id:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }
}
