/* ملف إعدادات الباقات: عدّل هنا الأسماء والأسعار والمميزات فقط، والصفحة تتحدث تلقائيًا.
   كل باقة:
   id       : معرّف قصير بالإنجليزية
   name     : اسم الباقة
   desc     : وصف قصير
   price    : السعر بالأرقام فقط (دينار عراقي)
   old      : السعر قبل الخصم (يظهر مشطوبًا)
   period   : "once" = دفعة واحدة، "month" = شهري
   badges   : شارات صغيرة فوق السعر
   features : قائمة مميزات. اكتب نصًا عاديًا، أو {t:"النص", soon:true} لإظهار شارة "قريبًا"
   style    : "simple" | "pro" | "elite"  (شكل البطاقة)
   ribbon   : نص شريط ذهبي أعلى البطاقة (اختياري)
   اكتب فقط مميزات موجودة فعلًا أو علّمها soon. */
window.WA_NUMBER = '9647828244399';
window.PLANS = [
  {
    id: 'basic', name: 'Ayham Basic', style: 'simple',
    desc: 'كل ما تحتاجه لمشغّل وسائط متكامل بواجهة عصرية.',
    price: 3000, old: 5000, period: 'once', badges: ['شراء مرة واحدة','مدى الحياة'],
    features: [
      'مشغل وسائط متكامل',
      'دعم تشغيل البث المباشر والأفلام والمسلسلات',
      'أدوات التحكم بالتشغيل والتقديم والتأخير',
      'واجهة استخدام سهلة وعصرية',
      'تحديثات التطبيق الأساسية',
      'دعم فني للمستخدمين',
      'جهاز واحد'
    ]
  },
  {
    id: 'pro', name: 'Ayham Pro', style: 'pro',
    desc: 'مرونة أكثر وتجربة أفضل، مع أولوية في الدعم.',
    price: 6000, old: 8000, period: 'month', badges: ['اشتراك شهري'],
    features: [
      'جميع مميزات الباقة الأساسية',
      'الاستخدام على جهازين (جهاز واحد يعمل بالوقت نفسه)',
      'تجربة تشغيل محسّنة لمختلف أنواع المحتوى',
      'دعم تشغيل البث المباشر والأفلام والمسلسلات ومختلف صيغ الفيديو',
      {t:'واجهات وثيمات إضافية', soon:true},
      {t:'مركز المباريات والنتائج', soon:true},
      {t:'خيارات تخصيص إضافية', soon:true},
      'أولوية في الدعم الفني',
      'الحصول على المميزات والتحديثات الجديدة'
    ]
  },
  {
    id: 'elite', name: 'Ayham Elite', style: 'elite', ribbon: '★ الأكثر تميزًا',
    desc: 'أقصى مرونة وأعلى أولوية، مع وصول مبكر لكل جديد.',
    price: 10000, old: 12000, period: 'month', badges: ['اشتراك شهري'],
    features: [
      'جميع مميزات باقة Pro',
      'تسجيل حتى 3 أجهزة (جهاز واحد يعمل بالوقت نفسه)',
      {t:'خيارات تخصيص متقدمة', soon:true},
      {t:'جميع الثيمات والواجهات الإضافية', soon:true},
      'دعم تشغيل البث المباشر والأفلام والمسلسلات',
      'دعم متقدم وأوسع للتشغيل وميزات داخلية أكثر',
      {t:'مركز المباريات والنتائج', soon:true},
      'أولوية قصوى في الدعم الفني',
      'الوصول المبكر للمميزات الجديدة',
      'الاستفادة من التحديثات والمزايا الحصرية'
    ]
  }
];

/* رسم بطاقات الباقات (تستخدمه الصفحة الرئيسية وصفحة الباقات) */
window.renderPlans = function (el) {
  var CHECK = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 13l4 4L19 7"></path></svg>';
  function esc(s) { return String(s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function fmt(n) { return Number(n).toLocaleString('en-US'); }
  el.innerHTML = window.PLANS.map(function (p) {
    var per = p.period === 'month' ? 'شهريًا' : 'دفعة واحدة';
    var bd = (p.badges || []).map(function (b, i) { return '<span class="bd' + (i === 0 && p.period !== 'month' ? ' g' : '') + '">' + esc(b) + '</span>'; }).join('');
    var msg = 'مرحبًا، أريد الاشتراك في ' + p.name + ' بسعر ' + fmt(p.price) + ' دينار عراقي (' + per + ')';
    var href = 'https://wa.me/' + window.WA_NUMBER + '?text=' + encodeURIComponent(msg);
    var feats = (p.features || []).map(function (f) {
      var t = typeof f === 'string' ? f : f.t, soon = typeof f !== 'string' && f.soon;
      return '<li>' + CHECK + '<span>' + esc(t) + (soon ? '<span class="soon">قريبًا</span>' : '') + '</span></li>';
    }).join('');
    var btn = p.style === 'elite' ? 'btn gold' : (p.style === 'pro' ? 'btn' : 'btn ghost');
    return '<article class="plan ' + (p.style || 'simple') + '">' + (p.ribbon ? '<div class="ribbon">' + esc(p.ribbon) + '</div>' : '') + '<div class="badges">' + bd + '</div><h2>' + esc(p.name) + '</h2><p class="d">' + esc(p.desc || '') + '</p>' + (p.old ? '<div class="was"><s>' + fmt(p.old) + '</s><span>خصم</span></div>' : '') + '<div class="p">' + fmt(p.price) + '<small>دينار عراقي</small></div><div class="per">' + per + '</div><ul>' + feats + '</ul><a class="' + btn + '" href="' + href + '" target="_blank" rel="noopener">اشترك الآن</a></article>';
  }).join('');
};
