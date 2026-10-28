# Ayham Player Web (قيد التطوير)

موقع خاص لتشغيل المحتوى من متصفح الهاتف أو الكمبيوتر، بخادم وسيط مدمج وبكلمة سر.

## الحالة
- ✅ الخادم `server.js` (كلمة سر، وسيط Xtream، بث HLS، ملفات VOD مع Range، صور، حماية SSRF، تشفير الروابط). مُختبر محليًا بسيرفر تجريبي (`test/mock-xtream.js`).
- ✅ قالب الصفحة والتنسيق (`public/index.html`, `public/app.css`).
- ⏳ واجهة المتصفح `public/app.js` (القوائم، التفاصيل، المشغّل) — لم تُكتب بعد.
- ⏳ ملفات النشر (Dockerfile / render.yaml) وتعليمات النشر.

## متغيرات البيئة المطلوبة
`ACCESS_PASSWORD` (12+ حرفًا)، `XTREAM_URL`، `XTREAM_USER`، `XTREAM_PASS`. لا تضعها داخل المستودع.

## تشغيل محلي
```
npm install
ACCESS_PASSWORD=... XTREAM_URL=http://host:port XTREAM_USER=... XTREAM_PASS=... npm start
```
