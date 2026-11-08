package com.ayham.player.data

/** ما الجديد في التحديث: يُحدَّث النص مع كل إصدار. يظهر مرة واحدة لكل إصدار ويمكن إغلاقه. */
object WhatsNew {
    const val id = "253"
    val ar = listOf(
        "الملفات الشخصية: شاشة «من يشاهد الآن؟» بتصميم سينمائي، لكل ملف مفضلته وسجل مشاهدته وآخر قناة وتفضيلاته وإعداداته المستقلة",
        "تخصيص الملف: الاسم واللقب والصورة (48 رمزاً أو صورة من جهازك) واللون والخلفية والإطار و8 أنماط جاهزة مع معاينة مباشرة",
        "ملف أطفال بتقييد حسب العمر وبدون إعدادات، وملف ضيف مؤقت، ورمز PIN من 4 أرقام لأي ملف، وإخفاء الملفات",
        "إدارة الملفات: إضافة وتعديل وحذف وترتيب ونسخ الإعدادات وتحديد الرئيسي والافتراضي، وترحيب سينمائي عند الدخول",
        "شاشة توقف سينمائية: لو التطبيق ساكن تظهر بوسترات أفلامك ومسلسلاتك بحركة هادئة وساعة (من الإعدادات تقدر تغيّر المدة أو تطفيها)",
        "شريط التقديم بفصول مثل أبل تي في، ولونه يتغير حسب بوستر كل فيلم",
        "كبسولة التقديم تعرض الوقت الجديد والفرق والمتبقي ووقت انتهاء الفيلم بالساعة",
        "لقطة عنوان سينمائية لكل حلقة: الموسم والحلقة واسمها",
        "بطاقة نهاية الفيلم: قيّمه بـ 👍/👎 وشاهد أفلاماً مشابهة من مكتبتك",
        "تشغيل آخر قناة مباشرة تلقائياً عند فتح التطبيق (اختياري من الإعدادات)",
        "فتح الأفلام والمسلسلات أسرع: تُحمَّل التفاصيل والخلفية والتقييمات مسبقاً عند الوقوف على البوستر",
        "القنوات تفضّل الاستمرارية: مخزن أكبر وبدء بهامش أمان وإعادة محاولة تلقائية صامتة بدل التوقف",
        "البث المباشر أكثر ثباتاً: تقليل التوقف وإعادة التحميل التلقائية",
        "شريط التقديم والتأخير على القنوات صار اختيارياً: فعّله من الإعدادات إن أردت",
        "الليلة في السينما: اختر مزاجك فيقترح لك فيلماً من مكتبتك (زر الليلة في الرئيسية)",
        "شريطا سينما أسودان يدخلان عند بدء الفيلم ثم ينسحبان",
        "بطاقة الحلقة التالية بصورة الحلقة وعدّاد أوضح",
        "اضغط اسم ممثل في صفحة الفيلم لترى أعماله الموجودة في مكتبتك",
        "تقييمات أوسع: Rotten Tomatoes (نقاد وجمهور) وLetterboxd وMetacritic إلى جانب IMDb وTMDB وTrakt",
        "خط جديد للترجمة (Cairo) أوضح وأجمل على الشاشة",
        "صفحة تفاصيل الفيلم والمسلسل بتصميم جديد: خلفية عريضة وبوستر وتفاصيل وأزرار مرتبة",
        "افتتاحية سينمائية جديدة بشعار Ayham وألوانه عند فتح التطبيق",
        "تقييمات متعددة وبحث ذكي بالجملة ووصف القصة",
    )
    val en = listOf(
        "Profiles: a cinematic \"Who's watching?\" screen; each profile keeps its own favorites, history, last channel, preferences and settings",
        "Customize a profile: name, tagline, avatar (48 icons or your own photo), color, background, frame and 8 ready styles with live preview",
        "Kids profile with age restriction and no settings, a temporary Guest profile, a 4-digit PIN per profile, and hidden profiles",
        "Manage profiles: add, edit, delete, reorder, copy settings, set main and default, plus a cinematic welcome on entry",
        "Cinematic screensaver: when the app is idle your posters drift by with a gentle zoom and a clock (change the delay or turn it off in Settings)",
        "Chaptered seek bar like Apple TV, tinted from each poster",
        "Seek capsule shows the new time, the jump, time left and the clock time the movie ends",
        "Cinematic title card for every episode: season, episode and name",
        "End-of-movie card: rate it 👍/👎 and pick a similar title from your library",
        "Resume the last live channel when the app opens (optional in Settings)",
        "Faster movie and series opening: details, backdrop and ratings preload while you hover a poster",
        "Channels favor continuity: bigger buffer, safety margin from live and silent auto-retry instead of stopping",
        "More stable live streams: fewer stops and automatic reloads",
        "Live rewind/forward bar is now optional: enable it in Settings",
        "Tonight at the cinema: pick a mood and get a movie from your library (Tonight tile on Home)",
        "Cinematic letterbox bars slide in when a movie starts, then retract",
        "Next-episode card with a thumbnail and a clearer countdown",
        "Tap an actor on the details page to see their works in your library",
        "More ratings: Rotten Tomatoes (critics and audience), Letterboxd and Metacritic alongside IMDb, TMDB and Trakt",
        "New subtitle font (Cairo): clearer and nicer on screen",
        "New movie and series details page: wide backdrop, poster, details and tidy buttons",
        "New cinematic opening with the Ayham logo and colors when the app starts",
        "Multiple ratings and smart natural-language and plot search",
    )
}

/** سجل التحديثات: يُضاف إصدار جديد في أول القائمة مع كل تحديث. */
data class Release(val id: String, val ar: List<String>, val en: List<String>)

val releaseHistory = listOf(
    Release("253", WhatsNew.ar, WhatsNew.en),
    Release("237", listOf("افتتاحية سينمائية جديدة بشعار Ayham"), listOf("New cinematic opening with the Ayham logo")),
    Release("227", listOf(
        "تقييمات متعددة (IMDb وRotten Tomatoes وMetacritic وTMDB وTrakt)",
        "بحث ذكي بالجملة الطبيعية ووصف القصة، وأعمال مشابهة أدق",
    ), listOf(
        "Multiple ratings (IMDb, Rotten Tomatoes, Metacritic, TMDB, Trakt)",
        "Smart natural-language and plot search, more accurate similar titles",
    )),
    Release("223", listOf(
        "مقدمة سينمائية، خلفية تحميل مموّهة، أنماط ترجمة، بحث أذكى، توهج شريط التقدم، سجل التحديثات",
    ), listOf(
        "Cinematic intro, blurred loading backdrop, subtitle styles, smarter search, seek-bar glow, update history",
    )),
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
