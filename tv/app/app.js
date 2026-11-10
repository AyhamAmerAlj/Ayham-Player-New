/* Ayham Player TV (LG webOS / Samsung Tizen) — ES5 للتوافق مع الشاشات القديمة */
(function () {
'use strict';
var LIC = 'https://ayham-license.aalalab178.workers.dev';
var GRACE = 3 * 24 * 3600 * 1000;
var UA = navigator.userAgent;
var IS_TIZEN = /Tizen/i.test(UA);
var IS_WEBOS = /web0s|webos|netcast/i.test(UA) || !!window.PalmServiceBridge;

function $(s, r) { return (r || document).querySelector(s); }
function $$(s, r) { return [].slice.call((r || document).querySelectorAll(s)); }
function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
function ls(k, v) {
  try {
    if (v === undefined) { var r = localStorage.getItem('ap_' + k); return r == null ? null : JSON.parse(r); }
    if (v === null) localStorage.removeItem('ap_' + k); else localStorage.setItem('ap_' + k, JSON.stringify(v));
  } catch (e) {}
  return null;
}
function fmt(sec) {
  sec = Math.max(0, Math.floor(sec || 0));
  var h = Math.floor(sec / 3600), m = Math.floor(sec % 3600 / 60), s = sec % 60;
  return (h ? h + ':' + (m < 10 ? '0' : '') : '') + m + ':' + (s < 10 ? '0' : '') + s;
}
function norm(s) {
  return String(s || '').toLowerCase().replace(/[ً-ٰٟ]/g, '').replace(/[أإآ]/g, 'ا').replace(/ى/g, 'ي').replace(/ة/g, 'ه');
}

/* ---------- scale ---------- */
var stage = $('#stage');
function fit() {
  var s = Math.min(window.innerWidth / 1920, window.innerHeight / 1080);
  stage.style.webkitTransform = stage.style.transform = 'scale(' + s + ')';
  stage._s = s;
}
window.addEventListener('resize', fit); fit();

/* ---------- network ---------- */
function xhr(method, url, body, cb) {
  var x = new XMLHttpRequest(), done = false;
  function fin(e, st, t) { if (done) return; done = true; cb(e, st, t); }
  try { x.open(method, url, true); } catch (e) { return fin('net'); }
  x.timeout = 30000;
  x.onload = function () { fin(null, x.status, x.responseText); };
  x.onerror = function () { fin('net'); };
  x.ontimeout = function () { fin('net'); };
  try { if (body != null) x.setRequestHeader('Content-Type', 'text/plain'); x.send(body == null ? null : body); } catch (e2) { fin('net'); }
}
/* احتياطي لـ LG: خدمة داخلية في الشاشة تجلب الرابط بدون قيود CORS */
function luna(url, cb) {
  if (!window.PalmServiceBridge) return cb('net');
  try {
    var b = new window.PalmServiceBridge();
    b.onservicecallback = function (m) {
      var r; try { r = JSON.parse(m); } catch (e) { return cb('net'); }
      if (r.returnValue === false) return cb('net');
      cb(null, r.status, r.body);
    };
    b.call('luna://com.ayham.player.tv.service/fetch', JSON.stringify({ url: url }));
  } catch (e) { cb('net'); }
}
function getText(url) {
  return new Promise(function (res, rej) {
    xhr('GET', url, null, function (e, st, t) {
      if (!e && st >= 200 && st < 400) return res(t);
      if (!e && st >= 400) return rej('http' + st);
      luna(url, function (e2, st2, t2) { if (!e2 && st2 >= 200 && st2 < 400) res(t2); else rej(e2 || ('http' + st2)); });
    });
  });
}
function getJSON(url) { return getText(url).then(function (t) { try { return JSON.parse(t); } catch (e) { throw 'bad'; } }); }
function post(path, obj) {
  return new Promise(function (res, rej) {
    xhr('POST', LIC + path, JSON.stringify(obj), function (e, st, t) {
      if (e) return rej('net');
      try { res(JSON.parse(t)); } catch (x) { rej('net'); }
    });
  });
}

/* ---------- license ---------- */
var dev = ls('dev');
if (!dev) { dev = 'tv-' + Math.random().toString(36).slice(2) + Date.now().toString(36); ls('dev', dev); }
var devName = IS_TIZEN ? 'Samsung TV' : (IS_WEBOS ? 'LG TV' : 'TV Web');
var LMSG = { invalid: 'الكود غير صحيح', revoked: 'هذا الكود تم إيقافه', devices_full: 'هذا الكود مستخدم على الحد الأقصى من الأجهزة', busy: 'هذا الكود يعمل الآن على جهاز آخر. أغلقه هناك وجرّب بعد دقائق' };
function licCheck(code) { return post('/v1/check', { code: code, device: dev, name: devName }); }
function normCode(s) { return String(s || '').toUpperCase().replace(/[^A-Z0-9]/g, ''); }

/* ---------- layers & focus ---------- */
var layers = [];
function top() { return layers[layers.length - 1]; }
function push(cls, html) {
  var d = document.createElement('div');
  d.className = 'layer ' + (cls || '');
  d.style.zIndex = 10 + layers.length;
  d.innerHTML = html;
  $('#layers').appendChild(d);
  var L = { el: d, last: null, cls: cls };
  layers.push(L);
  syncBody();
  focusFirst(L);
  return L;
}
function pop() {
  var L = layers.pop();
  if (L) L.el.parentNode.removeChild(L.el);
  syncBody();
  var t = top(); if (t) { if (t.last && document.body.contains(t.last)) setFocus(t.last); else focusFirst(t); }
}
function syncBody() { document.body.className = (top() && top().cls === 'player') ? 'playing' : ''; }
function focusFirst(L) { var f = $('.f.first', L.el) || $('.f', L.el); if (f) setFocus(f); }
function reveal(el) {
  var sc = el.parentNode; while (sc && sc !== document.body && !(sc.className && /\bscr\b/.test(sc.className))) sc = sc.parentNode;
  if (!sc || sc === document.body) return;
  var s = stage._s || 1, sr = sc.getBoundingClientRect(), r = el.getBoundingClientRect(), pad = 24 * s;
  if (r.bottom > sr.bottom - pad) sc.scrollTop += (r.bottom - sr.bottom + pad * 2) / s;
  else if (r.top < sr.top + pad) sc.scrollTop -= (sr.top - r.top + pad * 2) / s;
}
function setFocus(el) {
  var L = top(); if (!L || !el) return;
  $$('.foc', L.el).forEach(function (e) { e.className = e.className.replace(/\s*\bfoc\b/g, ''); });
  el.className += ' foc';
  L.last = el;
  reveal(el);
  if (el.getAttribute('data-i') != null && L.onFocusItem) L.onFocusItem(+el.getAttribute('data-i'));
}
function move(dir) {
  var L = top(); if (!L) return;
  var cur = L.last && document.body.contains(L.last) ? L.last : null;
  var list = $$('.f', L.el);
  if (!cur) { if (list[0]) setFocus(list[0]); return; }
  var r = cur.getBoundingClientRect(), cx = r.left + r.width / 2, cy = r.top + r.height / 2, best = null, bs = 1e12;
  list.forEach(function (e) {
    if (e === cur) return;
    var q = e.getBoundingClientRect(); if (!q.width) return;
    var dx = q.left + q.width / 2 - cx, dy = q.top + q.height / 2 - cy, main, cross, ok;
    if (dir === 'L') { ok = dx < -4; main = -dx; cross = Math.abs(dy); }
    else if (dir === 'R') { ok = dx > 4; main = dx; cross = Math.abs(dy); }
    else if (dir === 'U') { ok = dy < -4; main = -dy; cross = Math.abs(dx); }
    else { ok = dy > 4; main = dy; cross = Math.abs(dx); }
    if (!ok) return;
    var sc = main + cross * 3 + (cross > main * 2.5 ? 5000 : 0);
    if (sc < bs) { bs = sc; best = e; }
  });
  if (best) setFocus(best);
}
function toast(msg, ms) {
  var t = $('#toast'); t.textContent = msg; t.style.display = 'block';
  clearTimeout(toast._t); toast._t = setTimeout(function () { t.style.display = 'none'; }, ms || 3500);
}

/* ---------- account / api ---------- */
var A = null; // {base,u,p}
var INFO = null;
function normBase(s) {
  s = String(s || '').replace(/\s+/g, '');
  if (!/^https?:\/\//i.test(s)) s = 'http://' + s;
  return s.replace(/\/+$/, '').replace(/\/(player_api|get)\.php.*$/i, '').replace(/\/c$/i, '');
}
function apiUrl(action, extra) {
  return A.base + '/player_api.php?username=' + encodeURIComponent(A.u) + '&password=' + encodeURIComponent(A.p) + (action ? '&action=' + action : '') + (extra || '');
}
function api(action, extra) { return getJSON(apiUrl(action, extra)); }
function urlLive(id, ext) { return A.base + '/live/' + encodeURIComponent(A.u) + '/' + encodeURIComponent(A.p) + '/' + id + '.' + ext; }
function urlVod(id, ext) { return A.base + '/movie/' + encodeURIComponent(A.u) + '/' + encodeURIComponent(A.p) + '/' + id + '.' + (ext || 'mp4'); }
function urlEp(id, ext) { return A.base + '/series/' + encodeURIComponent(A.u) + '/' + encodeURIComponent(A.p) + '/' + id + '.' + (ext || 'mp4'); }

/* ---------- screens: license / login ---------- */
function screenLicense(msg) {
  closePlayer(true);
  while (layers.length) pop();
  var L = push('', '<div class="center"><img class="logo" src="logo.png"><h1>Ayham Player</h1><div class="sub">اكتب كود التفعيل (12 حرفاً)</div>' +
    '<input id="code" class="inp f first" maxlength="14" placeholder="XXXX-XXXX-XXXX" autocomplete="off">' +
    '<div class="err" id="e">' + esc(msg || '') + '</div><div><span class="btn pri f" id="go">تفعيل</span></div></div>');
  var inp = $('#code', L.el), go = $('#go', L.el), busy = false;
  function run() {
    var c = normCode(inp.value);
    if (c.length !== 12) { $('#e', L.el).textContent = 'اكتب الكود كاملاً (12 حرفاً)'; return; }
    if (busy) return; busy = true; $('#e', L.el).textContent = 'جارٍ التحقق...';
    licCheck(c).then(function (r) {
      busy = false;
      if (r.ok) { ls('code', c); ls('lastok', Date.now()); afterLicense(); }
      else $('#e', L.el).textContent = LMSG[r.err] || 'تعذّر التحقق';
    }, function () { busy = false; $('#e', L.el).textContent = 'تعذّر الاتصال بالخادم، تأكد من الإنترنت'; });
  }
  L.act = function (el) { if (el === inp) inp.focus(); else if (el === go) run(); };
  inp.addEventListener('change', function () { });
}

function screenLogin(msg) {
  while (layers.length) pop();
  var a = ls('acct') || {};
  var L = push('', '<div class="center"><img class="logo" src="logo.png"><h1>تسجيل الدخول</h1><div class="sub">بيانات اشتراكك (Xtream)</div>' +
    '<input id="sv" class="inp f first" placeholder="رابط السيرفر  http://example.com:8080" value="' + esc(a.base || '') + '">' +
    '<input id="us" class="inp f" placeholder="اسم المستخدم" value="' + esc(a.u || '') + '">' +
    '<input id="pw" class="inp f" placeholder="كلمة المرور" value="' + esc(a.p || '') + '">' +
    '<div class="err" id="e">' + esc(msg || '') + '</div><div><span class="btn pri f" id="go">دخول</span></div></div>');
  var busy = false;
  function run() {
    var base = normBase($('#sv', L.el).value), u = $('#us', L.el).value.trim(), p = $('#pw', L.el).value.trim();
    if (!$('#sv', L.el).value.trim() || !u || !p) { $('#e', L.el).textContent = 'املأ الخانات الثلاث'; return; }
    if (busy) return; busy = true; $('#e', L.el).textContent = 'جارٍ الدخول...';
    A = { base: base, u: u, p: p };
    api('').then(function (j) {
      busy = false;
      if (!j || !j.user_info || +j.user_info.auth !== 1) { $('#e', L.el).textContent = 'بيانات الدخول غير صحيحة'; return; }
      INFO = j; ls('acct', A); screenHome();
    }, function (e) {
      busy = false;
      $('#e', L.el).textContent = e === 'bad' ? 'ردّ السيرفر غير مفهوم، تأكد من الرابط' : (/^http/.test(e) ? 'السيرفر رفض الطلب (' + e + ')' : 'تعذّر الوصول للسيرفر، تأكد من الرابط والإنترنت');
    });
  }
  L.act = function (el) { if (el.tagName === 'INPUT') el.focus(); else run(); };
}

/* ---------- home ---------- */
var S = { tab: 'live', cats: {}, items: [], shown: 0, catId: null, cache: {}, all: null };
var TABS = [['live', 'القنوات'], ['vod', 'الأفلام'], ['series', 'المسلسلات'], ['search', 'بحث'], ['set', 'الحساب']];
function screenHome() {
  while (layers.length) pop();
  var tabs = TABS.map(function (t) { return '<span class="tab f" data-tab="' + t[0] + '">' + t[1] + '</span>'; }).join('');
  var L = push('home', '<div class="top"><img src="logo.png">' + tabs + '</div><div class="body"><div class="side scr" id="side"></div><div class="main scr" id="main"></div></div>');
  L.onFocusItem = function (i) { if (S.shown < S.items.length && i >= S.shown - 12) addChunk(L); };
  L.act = function (el) {
    var t = el.getAttribute('data-tab'); if (t) return showTab(L, t);
    var c = el.getAttribute('data-cat'); if (c != null) return loadCat(L, c, el);
    var i = el.getAttribute('data-i'); if (i != null) return openItem(S.items[+i], +i);
    var a = el.getAttribute('data-do'); if (a === 'logout') { ls('acct', null); A = null; screenLogin(); }
    if (a === 'code') { ls('code', null); screenLicense(); }
  };
  showTab(L, S.tab === 'set' || S.tab === 'search' ? 'live' : S.tab);
  var first = $('.tab.on', L.el); if (first) setFocus(first);
}
function markTab(L, t) { $$('.tab', L.el).forEach(function (e) { e.className = e.className.replace(/\s*\bon\b/g, '') + (e.getAttribute('data-tab') === t ? ' on' : ''); }); }
function showTab(L, t) {
  S.tab = t; markTab(L, t); S.items = []; S.shown = 0;
  var side = $('#side', L.el), main = $('#main', L.el);
  side.innerHTML = ''; side.scrollTop = 0; main.innerHTML = ''; main.scrollTop = 0;
  if (t === 'set') return renderSettings(main);
  if (t === 'search') return renderSearch(L, side, main);
  var kind = t;
  main.innerHTML = '<div class="msg">اختر تصنيفاً من القائمة</div>';
  var go = function (cats) {
    S.cats[kind] = cats;
    if (S.tab !== kind) return;
    side.innerHTML = cats.map(function (c) { return '<div class="cat f" data-cat="' + esc(c.category_id) + '">' + esc(c.category_name) + '</div>'; }).join('') || '<div class="msg">لا توجد تصنيفات</div>';
  };
  if (S.cats[kind]) return go(S.cats[kind]);
  side.innerHTML = '<div class="msg">جارٍ التحميل...</div>';
  api(kind === 'live' ? 'get_live_categories' : kind === 'vod' ? 'get_vod_categories' : 'get_series_categories').then(go, function () {
    side.innerHTML = '<div class="msg">تعذّر تحميل التصنيفات</div>';
  });
}
function loadCat(L, id, el) {
  var kind = S.tab, main = $('#main', L.el);
  $$('.cat', L.el).forEach(function (e) { e.className = e.className.replace(/\s*\bon\b/g, ''); });
  el.className += ' on';
  S.catId = id; main.scrollTop = 0;
  var act = kind === 'live' ? 'get_live_streams' : kind === 'vod' ? 'get_vod_streams' : 'get_series';
  var key = act + id;
  var done = function (arr) {
    if (S.catId !== id || S.tab !== kind) return;
    S.items = (arr || []).map(function (x) { x.t = kind; return x; }); S.shown = 0; main.innerHTML = '';
    if (!S.items.length) main.innerHTML = '<div class="msg">لا يوجد محتوى في هذا التصنيف</div>'; else addChunk(L);
  };
  if (S.cache[key]) return done(S.cache[key]);
  main.innerHTML = '<div class="msg">جارٍ التحميل...</div>';
  api(act, '&category_id=' + encodeURIComponent(id)).then(function (a) { if (a && a.length > 3000) a = a.slice(0, 3000); S.cache[key] = a; done(a); }, function () {
    if (S.catId === id) main.innerHTML = '<div class="msg">تعذّر تحميل القائمة</div>';
  });
}
function itemHtml(x, i) {
  if (x.t === 'live') return '<div class="ch f" data-i="' + i + '"><div class="lg" style="background-image:url(\'' + esc(x.stream_icon || '') + '\')"></div><div class="n">' + esc(x.name) + '</div></div>';
  var img = x.t === 'vod' ? x.stream_icon : (x.cover || x.stream_icon);
  return '<div class="card f" data-i="' + i + '"><div class="im" style="background-image:url(\'' + esc(img || '') + '\')"></div><div class="t">' + esc(x.name) + '</div></div>';
}
function addChunk(L) {
  var main = $('#main', L.el), n = Math.min(S.items.length, S.shown + 48), h = '';
  for (var i = S.shown; i < n; i++) h += itemHtml(S.items[i], i);
  main.insertAdjacentHTML('beforeend', h); S.shown = n;
}
function openItem(x, i) {
  if (!x) return;
  if (x.t === 'live') return startPlayer({ kind: 'live', title: x.name, list: S.items, idx: i });
  if (x.t === 'vod') return screenVod(x);
  screenSeries(x);
}

/* ---------- settings / search ---------- */
function renderSettings(main) {
  var u = (INFO && INFO.user_info) || {}, exp = u.exp_date ? new Date(+u.exp_date * 1000) : null;
  var expS = exp ? exp.getFullYear() + '-' + ('0' + (exp.getMonth() + 1)).slice(-2) + '-' + ('0' + exp.getDate()).slice(-2) : 'غير محدود';
  main.innerHTML = '<div class="set" style="padding:30px 60px"><div class="row">الحالة: ' + esc(u.status || '-') + '</div><div class="row">ينتهي الاشتراك: ' + esc(expS) + '</div>' +
    '<div class="row">الاتصالات: ' + esc(u.active_cons || 0) + ' / ' + esc(u.max_connections || '-') + '</div><div class="row">السيرفر: ' + esc(A ? A.base : '') + '</div>' +
    '<div class="row">الجهاز: ' + esc(devName) + ' — الإصدار 1.0</div><div style="margin-top:40px"><span class="btn f" data-do="logout">تسجيل الخروج من الاشتراك</span><span class="btn f" data-do="code">تغيير كود التفعيل</span></div></div>';
}
function renderSearch(L, side, main) {
  side.innerHTML = '<input id="q" class="inp f" style="width:380px;font-size:28px" placeholder="اكتب للبحث">';
  main.innerHTML = '<div class="msg">اكتب اسم قناة أو فيلم أو مسلسل</div>';
  var q = $('#q', L.el);
  q.addEventListener('change', function () { doSearch(L, q.value); });
  var orig = L.act; L.act = function (el) { if (el.id === 'q') { el.focus(); return; } orig(el); };
}
function doSearch(L, text) {
  var main = $('#main', L.el), n = norm(text); if (n.length < 2) return;
  main.innerHTML = '<div class="msg">جارٍ البحث...</div>';
  var load = S.all ? Promise.resolve(S.all) : Promise.all([api('get_live_streams'), api('get_vod_streams'), api('get_series')]).then(function (r) {
    var all = [];
    ['live', 'vod', 'series'].forEach(function (k, i) { (r[i] || []).forEach(function (x) { x.t = k; x._n = norm(x.name); all.push(x); }); });
    S.all = all; return all;
  });
  load.then(function (all) {
    if (S.tab !== 'search') return;
    var res = []; for (var i = 0; i < all.length && res.length < 120; i++) if (all[i]._n.indexOf(n) !== -1) res.push(all[i]);
    S.items = res; S.shown = 0; main.innerHTML = '';
    if (!res.length) main.innerHTML = '<div class="msg">لا توجد نتائج</div>'; else addChunk(L);
  }, function () { main.innerHTML = '<div class="msg">تعذّر البحث، حاول مرة ثانية</div>'; });
}

/* ---------- details ---------- */
function posOf(key) { var p = ls('pos') || {}; return p[key] || 0; }
function savePos(key, sec, dur) {
  var p = ls('pos') || {};
  if (dur && sec > dur - 40) delete p[key]; else if (sec > 20) p[key] = Math.floor(sec);
  var ks = Object.keys(p); if (ks.length > 300) delete p[ks[0]];
  ls('pos', p);
}
function screenVod(x) {
  var id = x.stream_id, L = push('over', '<div class="det"><div class="pos" style="background-image:url(\'' + esc(x.stream_icon || '') + '\')"></div><h1>' + esc(x.name) + '</h1><div class="meta" id="mt"></div><div class="plot" id="pl">جارٍ التحميل...</div><div id="bt"></div></div>');
  var ext = x.container_extension || 'mp4';
  function buttons() {
    var p = posOf('v' + id), h = '';
    if (p) h += '<span class="btn pri f first" data-p="' + p + '">متابعة من ' + fmt(p) + '</span><span class="btn f" data-p="0">من البداية</span>';
    else h += '<span class="btn pri f first" data-p="0">تشغيل</span>';
    $('#bt', L.el).innerHTML = h; focusFirst(L);
  }
  buttons();
  L.act = function (el) { var p = el.getAttribute('data-p'); if (p != null) startPlayer({ kind: 'vod', title: x.name, url: urlVod(id, ext), key: 'v' + id, start: +p }); };
  api('get_vod_info', '&vod_id=' + id).then(function (j) {
    if (!document.body.contains(L.el)) return;
    var i = (j && j.info) || {}, md = (j && j.movie_data) || {};
    if (md.container_extension) ext = md.container_extension;
    var meta = [i.releasedate || i.release_date, i.genre, i.duration, i.rating && i.rating !== '0' ? '★ ' + i.rating : ''].filter(Boolean).join('  •  ');
    $('#mt', L.el).textContent = meta; $('#pl', L.el).textContent = i.plot || i.description || '';
    var bg = i.movie_image || i.cover_big; if (bg) $('.pos', L.el).style.backgroundImage = 'url(\'' + bg + '\')';
  }, function () { $('#pl', L.el).textContent = ''; });
}
function screenSeries(x) {
  var id = x.series_id, L = push('over', '<div class="det"><div class="pos" style="background-image:url(\'' + esc(x.cover || '') + '\')"></div><h1>' + esc(x.name) + '</h1><div class="meta" id="mt"></div><div class="plot" id="pl">جارٍ التحميل...</div></div><div class="eps"><div class="seas" id="se"></div><div class="epl scr" id="ep"></div></div>');
  var eps = {}, seasons = [], cur = null;
  function showSeason(s) {
    cur = s; $$('#se .tab', L.el).forEach(function (e) { e.className = e.className.replace(/\s*\bon\b/g, '') + (e.getAttribute('data-s') === s ? ' on' : ''); });
    var h = (eps[s] || []).map(function (e, k) {
      var pc = posOf('e' + e.id);
      return '<span class="ep f" data-k="' + k + '">' + esc((e.episode_num || (k + 1)) + '. ' + (e.title || 'الحلقة ' + (k + 1))) + (pc ? '  —  ▶ ' + fmt(pc) : '') + '</span>';
    }).join('');
    $('#ep', L.el).innerHTML = h; $('#ep', L.el).scrollTop = 0;
  }
  L.act = function (el) {
    var s = el.getAttribute('data-s'); if (s != null) return showSeason(s);
    var k = el.getAttribute('data-k'); if (k == null) return;
    var list = eps[cur] || [], e = list[+k]; if (!e) return;
    startPlayer({ kind: 'ep', title: x.name + ' — ' + (e.title || ''), list: list, idx: +k, key: 'e' + e.id, start: posOf('e' + e.id) });
  };
  api('get_series_info', '&series_id=' + id).then(function (j) {
    if (!document.body.contains(L.el)) return;
    var i = (j && j.info) || {}; eps = (j && j.episodes) || {};
    if (Object.prototype.toString.call(eps) === '[object Array]') { var m = {}; eps.forEach(function (arr, n) { m[n + 1] = arr; }); eps = m; }
    seasons = Object.keys(eps).sort(function (a, b) { return a - b; });
    $('#mt', L.el).textContent = [i.releaseDate || i.release_date, i.genre, i.rating && i.rating !== '0' ? '★ ' + i.rating : ''].filter(Boolean).join('  •  ');
    $('#pl', L.el).textContent = i.plot || '';
    if (!seasons.length) { $('#pl', L.el).textContent = 'لا توجد حلقات متاحة'; return; }
    $('#se', L.el).innerHTML = seasons.map(function (s) { return '<span class="tab f" data-s="' + esc(s) + '">الموسم ' + esc(s) + '</span>'; }).join('');
    showSeason(seasons[0]); var f = $('.ep', L.el) || $('.tab', L.el); if (f) setFocus(f);
  }, function () { $('#pl', L.el).textContent = 'تعذّر تحميل الحلقات'; });
}

/* ---------- player ---------- */
var P = null;
var hasAV = function () { return IS_TIZEN && window.webapis && window.webapis.avplay; };
function startPlayer(o) {
  closePlayer(true);
  P = { o: o, t: 0, dur: 0, osdT: 0, tried: {}, lastSave: 0 };
  var useAV = hasAV();
  var html = (useAV ? '<object id="av" type="application/avplayer" style="position:absolute;left:0;top:0;width:1920px;height:1080px"></object>' : '<video id="vd" autoplay></video>') +
    '<div class="spin" id="sp">جارٍ التحميل...</div><div class="osd" id="osd"><div class="tt" id="tt"></div><div class="bar"><i id="bi"></i></div><div class="tm" id="tm"></div></div>';
  P.L = push('player', html);
  P.av = useAV;
  playCurrent();
}
function curUrl() {
  var o = P.o;
  if (o.kind === 'live') { var c = o.list[o.idx]; return urlLive(c.stream_id, P.ext || ls('livefmt') || 'm3u8'); }
  if (o.kind === 'ep') { var e = o.list[o.idx]; return urlEp(e.id, e.container_extension); }
  return o.url;
}
function curTitle() { var o = P.o; return o.kind === 'live' ? o.list[o.idx].name : o.title; }
function playCurrent() {
  var url = curUrl(), o = P.o;
  $('#tt', P.L.el).textContent = curTitle(); spin(true); showOsd();
  P.t = 0; P.dur = 0; P.paused = false;
  var start = o.start || 0; o.start = 0;
  if (P.av) {
    try {
      var av = window.webapis.avplay;
      try { av.close(); } catch (e0) {}
      av.open(url); av.setDisplayRect(0, 0, 1920, 1080);
      av.setListener({
        onbufferingstart: function () { spin(true); }, onbufferingcomplete: function () { spin(false); },
        oncurrentplaytime: function (ms) { P.t = ms / 1000; tick(); }, onstreamcompleted: function () { onEnd(); },
        onerror: function () { onFail(); }, onevent: function () {}, onsubtitlechange: function () {}, ondrmevent: function () {}
      });
      av.prepareAsync(function () {
        try { P.dur = o.kind === 'live' ? 0 : av.getDuration() / 1000; if (start > 5) av.seekTo(start * 1000); av.play(); } catch (e1) { onFail(); }
        spin(false);
      }, function () { onFail(); });
    } catch (e) { onFail(); }
  } else {
    var v = $('#vd', P.L.el); P.v = v;
    v.onplaying = function () { if (P && P.v === v) spin(false); };
    v.onwaiting = function () { if (P && P.v === v) spin(true); };
    v.onerror = function () { if (P && P.v === v) onFail(); };
    v.onended = function () { if (P && P.v === v) onEnd(); };
    v.ontimeupdate = function () { if (!P || P.v !== v) return; P.t = v.currentTime; P.dur = o.kind === 'live' ? 0 : (v.duration || 0); tick(); };
    v.onloadedmetadata = function () { if (start > 5) { try { v.currentTime = start; } catch (e2) {} } };
    v.src = url; try { v.load(); var pr = v.play(); if (pr && pr.catch) pr.catch(function () {}); } catch (e3) {}
  }
}
function spin(on) { if (P) { var s = $('#sp', P.L.el); if (s) s.style.display = on ? 'block' : 'none'; } }
function showOsd() {
  if (!P) return; var d = $('#osd', P.L.el); d.style.display = 'block'; clearTimeout(P.osdT);
  P.osdT = setTimeout(function () { if (P && !P.paused) d.style.display = 'none'; }, 4500);
}
function tick() {
  if (!P) return;
  var o = P.o;
  if (o.kind === 'live') { $('#tm', P.L.el).textContent = 'مباشر'; $('#bi', P.L.el).style.width = '100%'; return; }
  $('#tm', P.L.el).textContent = fmt(P.t) + ' / ' + fmt(P.dur);
  $('#bi', P.L.el).style.width = (P.dur ? Math.min(100, P.t / P.dur * 100) : 0) + '%';
  var n = Date.now(); if (o.key && n - P.lastSave > 10000) { P.lastSave = n; savePos(o.key, P.t, P.dur); }
}
function onFail() {
  if (!P) return; var o = P.o;
  if (o.kind === 'live') {
    var cur = P.ext || ls('livefmt') || 'm3u8';
    if (!P.tried[cur]) {
      P.tried[cur] = 1; var nx = cur === 'm3u8' ? 'ts' : 'm3u8';
      if (!P.tried[nx]) { P.ext = nx; playCurrent(); return; }
    }
  }
  spin(false); toast('تعذّر تشغيل هذا المحتوى. جرّب غيره أو تأكد من الاشتراك', 4500);
}
function onEnd() {
  if (!P) return; var o = P.o;
  if (o.key) savePos(o.key, P.dur, P.dur);
  if (o.kind === 'ep' && o.idx + 1 < o.list.length) { o.idx++; var e = o.list[o.idx]; o.key = 'e' + e.id; o.start = 0; P.tried = {}; toast('الحلقة التالية...'); return playCurrent(); }
  closePlayer();
}
function stopBackend() {
  if (!P) return;
  try { if (P.av) { var av = window.webapis.avplay; try { av.stop(); } catch (e) {} av.close(); } else if (P.v) { P.v.onerror = null; P.v.pause(); P.v.removeAttribute('src'); P.v.load(); } } catch (e2) {}
}
function closePlayer(silent) {
  if (!P) return;
  var o = P.o; if (o.key) savePos(o.key, P.t, P.dur);
  if (o.kind === 'live' && P.ext) ls('livefmt', P.ext);
  stopBackend(); clearTimeout(P.osdT);
  var wasTop = top() && top() === P.L; P = null;
  if (wasTop) pop();
  if (!silent) syncBody();
}
function seekBy(sec) {
  if (!P || P.o.kind === 'live') return; showOsd();
  try {
    if (P.av) { var av = window.webapis.avplay; if (sec > 0) av.jumpForward(sec * 1000); else av.jumpBackward(-sec * 1000); }
    else if (P.v) { var t = Math.max(0, Math.min((P.v.duration || 1e9) - 1, P.v.currentTime + sec)); P.v.currentTime = t; }
  } catch (e) {}
}
function togglePause() {
  if (!P) return; P.paused = !P.paused; showOsd();
  try {
    if (P.av) { if (P.paused) window.webapis.avplay.pause(); else window.webapis.avplay.play(); }
    else if (P.v) { if (P.paused) P.v.pause(); else P.v.play(); }
  } catch (e) {}
}
function zap(d) {
  if (!P) return; var o = P.o, n = o.idx + d;
  if (o.kind === 'live' || o.kind === 'ep') {
    if (n < 0 || n >= o.list.length) return;
    o.idx = n; P.tried = {}; if (o.kind === 'ep') { o.key = 'e' + o.list[n].id; o.start = 0; }
    playCurrent();
  } else showOsd();
}
function playerKey(k) {
  if (k === 37) return seekBy(-10); if (k === 39) return seekBy(10);
  if (k === 38) return zap(P && P.o.kind === 'live' ? -1 : 0); if (k === 40) return zap(P && P.o.kind === 'live' ? 1 : 0);
  if (k === 13 || k === 415 || k === 19 || k === 10252) return (k === 415 && P && !P.paused) ? showOsd() : togglePause();
  if (k === 417) return seekBy(30); if (k === 412) return seekBy(-30);
  if (k === 427 || k === 428) return zap(k === 427 ? 1 : -1);
  showOsd();
}

/* ---------- keys ---------- */
function exitApp() {
  try { if (IS_TIZEN && window.tizen) { window.tizen.application.getCurrentApplication().exit(); return; } } catch (e) {}
  try { window.close(); } catch (e2) {}
}
function goBack() {
  if (P) return closePlayer();
  if (layers.length > 1) return pop();
  var t = top();
  if (t && t.cls === 'home' && S.tab !== 'live') { return showTab(t, 'live'); }
  exitApp();
}
document.addEventListener('keydown', function (e) {
  var k = e.keyCode, ae = document.activeElement;
  if (ae && ae.tagName === 'INPUT') {
    if (k === 13 || k === 65376 || k === 65385 || k === 461 || k === 10009 || k === 27) { ae.blur(); e.preventDefault(); }
    return;
  }
  if (k === 461 || k === 10009 || k === 27 || k === 8) { e.preventDefault(); return goBack(); }
  if (P) { e.preventDefault(); return playerKey(k); }
  var d = k === 37 ? 'L' : k === 39 ? 'R' : k === 38 ? 'U' : k === 40 ? 'D' : null;
  if (d) { e.preventDefault(); return move(d); }
  if (k === 13) { e.preventDefault(); var L = top(); if (L && L.last && L.act) L.act(L.last); }
});
document.addEventListener('click', function (e) {
  var el = e.target; while (el && el !== document.body && !(el.className && /\bf\b/.test(el.className))) el = el.parentNode;
  var L = top(); if (el && el !== document.body && L && L.act && L.el.contains(el)) { setFocus(el); L.act(el); }
});
if (IS_TIZEN) {
  try {
    var s = document.createElement('script'); s.src = '$WEBAPIS/webapis/webapis.js'; document.head.appendChild(s);
    if (window.tizen && window.tizen.tvinputdevice) ['MediaPlay', 'MediaPause', 'MediaPlayPause', 'MediaStop', 'MediaFastForward', 'MediaRewind', 'ChannelUp', 'ChannelDown'].forEach(function (n) { try { window.tizen.tvinputdevice.registerKey(n); } catch (e) {} });
  } catch (e) {}
}

/* ---------- boot ---------- */
function afterLicense() {
  var a = ls('acct');
  if (!a) return screenLogin();
  A = a;
  push('', '<div class="center"><img class="logo" src="logo.png"><div class="sub">جارٍ الدخول...</div></div>');
  api('').then(function (j) {
    if (j && j.user_info && +j.user_info.auth === 1) { INFO = j; screenHome(); } else screenLogin('انتهت صلاحية بيانات الدخول، سجّل من جديد');
  }, function (e) {
    if (e === 'net') { INFO = null; screenHome(); toast('تعذّر الوصول للسيرفر. تأكد من الإنترنت'); } else screenLogin('تعذّر الدخول، تأكد من البيانات');
  });
}
function boot() {
  var code = ls('code');
  if (!code) return screenLicense();
  push('', '<div class="center"><img class="logo" src="logo.png"><h1>Ayham Player</h1></div>');
  licCheck(code).then(function (r) {
    if (r.ok) { ls('lastok', Date.now()); afterLicense(); }
    else { if (r.err === 'invalid' || r.err === 'revoked') ls('code', null); screenLicense(LMSG[r.err] || 'تعذّر التحقق'); }
  }, function () {
    if (Date.now() - (ls('lastok') || 0) < GRACE) afterLicense(); else screenLicense('يلزم اتصال بالإنترنت للتحقق من الكود');
  });
}
setInterval(function () {
  var c = ls('code'); if (!c) return;
  licCheck(c).then(function (r) { if (r.ok) ls('lastok', Date.now()); else { if (r.err === 'invalid' || r.err === 'revoked') ls('code', null); screenLicense(LMSG[r.err] || 'تعذّر التحقق'); } }, function () {});
}, 4 * 60 * 1000);
window.addEventListener('pagehide', function () {
  var c = ls('code'); if (!c || !navigator.sendBeacon) return;
  try { navigator.sendBeacon(LIC + '/v1/release', new Blob([JSON.stringify({ code: c, device: dev })], { type: 'text/plain' })); } catch (e) {}
});
boot();
})();
