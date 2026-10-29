# استعادة المشروع على حساب GitHub جديد

## ما الموجود في هذا المشروع
- `app/` تطبيق أندرويد (Ayham Player). يُبنى تلقائيًا بـ GitHub Actions.
- `web/` موقع المشاهدة (خادم Node.js، يُنشر على Docker/Render).
- `license/` خادم أكواد التفعيل (Cloudflare Worker + Durable Object).
- `site/` موقع البيع (ملفات ثابتة على Cloudflare).
- `app/ayham-release.jks` مفتاح توقيع التطبيق. **بدونه لا يمكن تحديث التطبيق فوق النسخة المثبتة.** احتفظ بنسخة منه (كلمة سره داخل `app/build.gradle.kts`).

## خطوات الاستعادة
1. أنشئ حساب GitHub جديد ومستودعًا جديدًا (فارغ).
2. ارفع الملفات: فك الأرشيف ثم
   `git init && git add -A && git commit -m restore && git branch -M main && git remote add origin <رابط المستودع> && git push -u origin main`
   (أو من ملف bundle: `git clone project.bundle مجلد` ثم غيّر remote وادفع).
3. عدّل رابط التحديث الداخلي في `app/src/main/java/com/ayham/player/data/Updater.kt` ليشير للمستودع الجديد.
4. في المستودع: Settings > Secrets and variables > Actions
   - Secrets: `CLOUDFLARE_API_TOKEN` و`CLOUDFLARE_ACCOUNT_ID` و`ADMIN_KEY`
   - Variable: `LICENSE_URL` = رابط خادم الأكواد (بدون `/` بالنهاية)
5. شغّل من تبويب Actions: Deploy license server، Deploy sales site، Build Ayham Player APK.
6. رقم النسخة (versionCode) = رقم تشغيل البناء + 100. إذا بدأ المستودع الجديد من 1 فقد لا تتثبت النسخ فوق القديمة. ارفع الإزاحة في `.github/workflows/build.yml` (القيمة 100) لأكبر من آخر نسخة مثبتة.

## أشياء ليست داخل المستودع
- أكواد التفعيل المباعة محفوظة في Cloudflare فقط (مرتبطة بالخادم). احتفظ بقائمة بها (الكود واسم الزبون) خارج المستودع.
- كلمات السر والـ Token: لا تُحفظ في الملفات. خزّنها بمكان آمن.
- مفاتيح اشتراك الـ IPTV وروابطه: ليست في المستودع ويجب ألا توضع فيه.
