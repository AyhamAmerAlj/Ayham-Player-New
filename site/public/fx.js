/* حركات وتفاعل الموقع: لا تؤثر على المحتوى إذا تعطّل JavaScript أو إذا المستخدم يفضّل تقليل الحركة */
(function () {
  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var d = document;

  // 1) ظهور تدريجي عند التمرير
  var sel = '.hero .tag,.hero h1,.hero p,.hero .cta,.hero .chips,h2.t,p.sub,.f,.plan,.cmp .box,.st,.pm,.log>div,details,.notice,.why,.pg';
  var els = [].slice.call(d.querySelectorAll(sel));
  if (!reduce && 'IntersectionObserver' in window) {
    d.documentElement.classList.add('fx');
    els.forEach(function (e, i) { e.classList.add('rv'); e.style.transitionDelay = (i % 3) * 70 + 'ms'; });
    var io = new IntersectionObserver(function (es) {
      es.forEach(function (x) { if (x.isIntersecting) { x.target.classList.add('in'); io.unobserve(x.target); } });
    }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });
    els.forEach(function (e) { io.observe(e); });
  }

  // 2) شريط علوي يلتصق ويتغير عند التمرير
  var nav = d.querySelector('header .nav');
  if (nav) {
    var h = nav.closest('header');
    h.classList.add('stick');
    var onS = function () { h.classList.toggle('scrolled', window.scrollY > 20); };
    window.addEventListener('scroll', onS, { passive: true }); onS();
  }

  // 3) شريط تقدم التمرير
  var bar = d.createElement('div'); bar.className = 'prog'; d.body.appendChild(bar);
  window.addEventListener('scroll', function () {
    var m = d.documentElement.scrollHeight - innerHeight;
    bar.style.transform = 'scaleX(' + (m > 0 ? Math.min(1, scrollY / m) : 0) + ')';
  }, { passive: true });

  // 4) زر واتساب عائم
  var wa = d.createElement('a');
  wa.className = 'wafab'; wa.target = '_blank'; wa.rel = 'noopener';
  wa.setAttribute('aria-label', 'تواصل عبر واتساب');
  wa.href = 'https://wa.me/' + (window.WA_NUMBER || '9647828244399') + '?text=' + encodeURIComponent('مرحبًا، لدي استفسار');
  wa.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21 11.5a8.4 8.4 0 01-12.4 7.4L3 20l1.2-5.3A8.4 8.4 0 1121 11.5z"></path></svg>';
  d.body.appendChild(wa);

  // 5) معرض اللقطات: أزرار تنقل
  var sh = d.querySelector('.shots');
  if (sh) {
    var wrap = sh.parentNode;
    var mk = function (cls, txt, dir) {
      var b = d.createElement('button'); b.type = 'button'; b.className = 'sbtn ' + cls; b.textContent = txt;
      b.setAttribute('aria-label', dir > 0 ? 'التالي' : 'السابق');
      b.onclick = function () { sh.scrollBy({ left: dir * Math.max(280, sh.clientWidth * 0.8), behavior: reduce ? 'auto' : 'smooth' }); };
      wrap.appendChild(b);
    };
    wrap.style.position = 'relative';
    mk('l', '‹', -1); mk('r', '›', 1);
    // سحب بالماوس
    var down = false, sx = 0, sl = 0;
    sh.addEventListener('mousedown', function (e) { down = true; sx = e.pageX; sl = sh.scrollLeft; sh.classList.add('drag'); });
    window.addEventListener('mouseup', function () { down = false; sh.classList.remove('drag'); });
    sh.addEventListener('mousemove', function (e) { if (!down) return; e.preventDefault(); sh.scrollLeft = sl - (e.pageX - sx); });
  }

  // 6) توهج يتبع المؤشر على البطاقات (حاسوب فقط)
  if (!reduce && window.matchMedia('(hover:hover)').matches) {
    d.addEventListener('mousemove', function (e) {
      var c = e.target.closest && e.target.closest('.plan,.f,.box,.pm');
      if (!c) return;
      var r = c.getBoundingClientRect();
      c.style.setProperty('--mx', (e.clientX - r.left) + 'px');
      c.style.setProperty('--my', (e.clientY - r.top) + 'px');
    }, { passive: true });
  }

  // 7) عدّاد الأسعار عند الظهور
  if (!reduce && 'IntersectionObserver' in window) {
    var pio = new IntersectionObserver(function (es) {
      es.forEach(function (x) {
        if (!x.isIntersecting) return; pio.unobserve(x.target);
        var el = x.target, txt = el.firstChild && el.firstChild.nodeType === 3 ? el.firstChild : null;
        if (!txt) return;
        var to = parseInt(txt.nodeValue.replace(/,/g, ''), 10); if (!to) return;
        var t0 = performance.now(), dur = 900;
        (function step(t) {
          var k = Math.min(1, (t - t0) / dur); k = 1 - Math.pow(1 - k, 3);
          txt.nodeValue = Math.round(to * k).toLocaleString('en-US');
          if (k < 1) requestAnimationFrame(step); else txt.nodeValue = to.toLocaleString('en-US');
        })(t0);
      });
    }, { threshold: 0.6 });
    [].slice.call(d.querySelectorAll('.plan .p')).forEach(function (p) { pio.observe(p); });
  }
})();
