package com.ayham.player.data

import android.net.Uri
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

/**
 * سيرفر صغير يشتغل على الشاشة داخل الشبكة المنزلية فقط.
 * الهاتف يفتح الصفحة (من QR) ويرسل بيانات الاشتراك للشاشة مباشرة.
 */
class LoginServer(
    private val onCreds: ((server: String, user: String, pass: String) -> Unit)? = null,
    private val onUpload: ((name: String, bytes: ByteArray) -> Unit)? = null,
    private val photoMode: Boolean = false,
) {

    @Volatile private var socket: ServerSocket? = null
    @Volatile private var running = false
    var port: Int = 0
        private set

    fun start(): Boolean {
        for (p in 8080..8095) {
            try {
                socket = ServerSocket(p)
                port = p
                break
            } catch (_: Exception) {
            }
        }
        val ss = socket ?: return false
        running = true
        thread(isDaemon = true, name = "ayham-login") {
            while (running) {
                try {
                    val c = ss.accept()
                    thread(isDaemon = true) { handle(c) }
                } catch (_: Exception) {
                    break
                }
            }
        }
        return true
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
        socket = null
    }

    private fun handle(c: Socket) {
        try {
            c.soTimeout = 8000
            val input = c.getInputStream()
            val head = ByteArrayOutputStream()
            var last4 = 0
            // نقرأ الهيدر لحد السطر الفارغ
            while (head.size() < 16_384) {
                val b = input.read()
                if (b < 0) break
                head.write(b)
                last4 = (last4 shl 8) or b
                if (last4 == 0x0D0A0D0A) break
            }
            val headStr = head.toString("ISO-8859-1")
            val first = headStr.lineSequence().firstOrNull().orEmpty()
            val len = Regex("(?i)content-length:\\s*(\\d+)").find(headStr)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val out = c.getOutputStream()

            if (first.startsWith("POST /sub") && onUpload != null && len in 1..6_000_000) {
                val buf = ByteArray(len)
                var read = 0
                while (read < len) {
                    val n = input.read(buf, read, len - read)
                    if (n < 0) break
                    read += n
                }
                val q = first.substringAfter("name=", "").substringBefore(" ").substringBefore("&")
                val name = runCatching { URLDecoder.decode(q, "UTF-8") }.getOrDefault("sub.srt")
                respond(out, "ok")
                onUpload.invoke(name, buf.copyOf(read))
            } else if (first.startsWith("POST") && onCreds != null && len in 1..8192) {
                val buf = ByteArray(len)
                var read = 0
                while (read < len) {
                    val n = input.read(buf, read, len - read)
                    if (n < 0) break
                    read += n
                }
                val fields = String(buf, 0, read, Charsets.UTF_8).split("&").associate {
                    val kv = it.split("=", limit = 2)
                    URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
                }
                var server = fields["server"].orEmpty().trim()
                var user = fields["user"].orEmpty().trim()
                var pass = fields["pass"].orEmpty().trim()
                val link = fields["link"].orEmpty().trim()
                if (link.isNotEmpty()) {
                    runCatching {
                        val u = Uri.parse(link)
                        if (!u.scheme.isNullOrEmpty() && !u.host.isNullOrEmpty()) {
                            server = u.scheme + "://" + u.host + (if (u.port > 0) ":" + u.port else "")
                        }
                        u.getQueryParameter("username")?.let { user = it }
                        u.getQueryParameter("password")?.let { pass = it }
                    }
                }
                if (server.isNotEmpty() && user.isNotEmpty() && pass.isNotEmpty()) {
                    respond(out, DONE_PAGE)
                    onCreds.invoke(server, user, pass)
                } else {
                    respond(out, FORM_PAGE.replace("<!--ERR-->", "<p class=e>عبّي كل الحقول أو الصق الرابط الكامل</p>"))
                }
            } else {
                respond(out, if (onUpload != null && onCreds == null) (if (photoMode) PHOTO_PAGE else SUB_PAGE) else FORM_PAGE)
            }
            out.flush()
        } catch (_: Exception) {
        } finally {
            runCatching { c.close() }
        }
    }

    private fun respond(out: java.io.OutputStream, html: String) {
        val body = html.toByteArray(Charsets.UTF_8)
        out.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n")
                .toByteArray(Charsets.ISO_8859_1),
        )
        out.write(body)
    }

    companion object {
        fun localIp(): String? {
            return runCatching {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { it.isSiteLocalAddress }
                    ?.hostAddress
            }.getOrNull()
        }

        private const val STYLE = """
<meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1">
<style>
body{margin:0;font-family:sans-serif;background:#0B0614;color:#F5F0FF;display:flex;justify-content:center}
.c{width:100%;max-width:440px;padding:24px}
h1{color:#C4A1FF;margin:8px 0 18px}
input{width:100%;box-sizing:border-box;padding:14px;margin:6px 0 14px;border-radius:12px;border:1px solid #5b3aa6;background:#1C1033;color:#fff;font-size:16px}
label{font-size:14px;color:#A99BC4}
button{width:100%;padding:15px;border:0;border-radius:24px;background:#8B5CF6;color:#fff;font-size:18px;font-weight:bold}
.e{color:#FF6B8B}.s{color:#A99BC4;font-size:13px;text-align:center;margin:14px 0}
</style>"""

        private const val FORM_PAGE = """<!doctype html><html dir=rtl lang=ar><head>$STYLE<title>Ayham Player</title></head><body><div class=c>
<h1>Ayham Player</h1><!--ERR-->
<form method=post action=/login>
<label>الصق رابط الاشتراك الكامل (اختياري)</label>
<input name=link placeholder="http://server:port/get.php?username=...&password=..." dir=ltr>
<p class=s>— أو عبّي الحقول —</p>
<label>عنوان السيرفر</label><input name=server placeholder="http://example.com:8080" dir=ltr autocapitalize=off>
<label>اسم المستخدم</label><input name=user dir=ltr autocapitalize=off>
<label>كلمة المرور</label><input name=pass dir=ltr autocapitalize=off>
<button type=submit>إرسال للشاشة</button>
</form></div></body></html>"""

        private const val SUB_PAGE = """<!doctype html><html dir=rtl lang=ar><head>$STYLE<title>Ayham Player</title></head><body><div class=c>
<h1>إرسال ترجمة للشاشة</h1>
<p class=s>1) حمّل ملف الترجمة من الموقع ثم 2) اخترها وأرسلها</p>
<a href="https://subdl.com/" target=_blank style="display:block;text-align:center;padding:14px;border-radius:24px;border:1px solid #8B5CF6;color:#C4A1FF;text-decoration:none;margin-bottom:22px">فتح موقع SubDL</a>
<label>ملف الترجمة (srt / vtt / ass / zip)</label>
<input id=f type=file accept=".srt,.vtt,.ass,.ssa,.zip">
<button onclick="send()">إرسال للشاشة</button>
<p id=m class=s></p>
<script>
async function send(){
 var f=document.getElementById('f').files[0]; var m=document.getElementById('m');
 if(!f){m.textContent='اختر ملفًا أولًا';return}
 m.textContent='جارٍ الإرسال...';
 try{var r=await fetch('/sub?name='+encodeURIComponent(f.name),{method:'POST',body:f});
 m.textContent=r.ok?'تم الإرسال ✓ ارجع للشاشة':'فشل الإرسال';}catch(e){m.textContent='فشل الإرسال'}
}
</script></div></body></html>"""

        private const val PHOTO_PAGE = """<!doctype html><html dir=rtl lang=ar><head>$STYLE<title>Ayham Player</title></head><body><div class=c>
<h1>صورة الملف الشخصي</h1>
<p class=s>اختر صورة من هاتفك وترسل للشاشة</p>
<input id=f type=file accept="image/*">
<button onclick="send()">إرسال للشاشة</button>
<p id=m class=s></p>
<script>
function send(){
 var f=document.getElementById('f').files[0]; var m=document.getElementById('m');
 if(!f){m.textContent='اختر صورة أولًا';return}
 m.textContent='جارٍ الإرسال...';
 var img=new Image(); img.onload=function(){
  var s=Math.min(1,480/Math.max(img.width,img.height));
  var c=document.createElement('canvas'); c.width=Math.round(img.width*s); c.height=Math.round(img.height*s);
  c.getContext('2d').drawImage(img,0,0,c.width,c.height);
  c.toBlob(async function(b){
   try{var r=await fetch('/sub?name=photo.jpg',{method:'POST',body:b});
   m.textContent=r.ok?'تم الإرسال ✓ ارجع للشاشة':'فشل الإرسال';}catch(e){m.textContent='فشل الإرسال'}
  },'image/jpeg',0.85);
 };
 img.onerror=function(){m.textContent='تعذّر قراءة الصورة'};
 img.src=URL.createObjectURL(f);
}
</script></div></body></html>"""

        private const val DONE_PAGE = """<!doctype html><html dir=rtl lang=ar><head>$STYLE<title>Ayham Player</title></head><body><div class=c>
<h1>تم الإرسال ✓</h1><p>ارجع للشاشة، التطبيق يحمّل القائمة الآن.</p></div></body></html>"""
    }
}
