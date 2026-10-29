(() => {
'use strict';
/* Ayham Player Web: واجهة المتصفح. كل الطلبات تمرّ على الخادم الوسيط (لا بيانات اشتراك هنا). */

// ------------------------------------------------------------------ أدوات
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const LS = {
  get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch (e) { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) { /* ممتلئ أو ممنوع */ } },
  del(k) { try { localStorage.removeItem(k); } catch (e) { /* ignore */ } },
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const debounce = (fn, ms) => { let h; return (...a) => { clearTimeout(h); h = setTimeout(() => fn(...a), ms); }; };
const icon = (n) => `<svg class="i"><use href="#i-${n}"/></svg>`;
const normText = (s) => String(s || '').toLowerCase()
  .replace(/[ً-ٰٟـ]/g, '').replace(/[أإآٱ]/g, 'ا').replace(/ى/g, 'ي').replace(/ة/g, 'ه').replace(/ؤ/g, 'و').replace(/ئ/g, 'ي')
  .replace(/\s+/g, ' ').trim();
const fmtTime = (s) => {
  s = Math.max(0, Math.floor(s || 0));
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), x = s % 60;
  return (h ? h + ':' + String(m).padStart(2, '0') : m) + ':' + String(x).padStart(2, '0');
};
function b64utf8(b) { try { return decodeURIComponent(escape(atob(b))); } catch (e) { return b || ''; } }

// ------------------------------------------------------------------ اللغة
const T = {
  ar: {
    home: 'الرئيسية', movies: 'الأفلام', series: 'المسلسلات', live: 'البث المباشر', library: 'مكتبتي', settings: 'الإعدادات', search: 'بحث',
    search_ph: 'ابحث عن فيلم، مسلسل أو قناة...', login_title: 'Ayham Player', login_sub: 'أدخل كلمة السر للمتابعة', password: 'كلمة السر', enter: 'دخول',
    wrong_pw: 'كلمة السر غير صحيحة', too_many: 'محاولات كثيرة، حاول لاحقًا', conn_fail: 'تعذّر الاتصال بالخادم',
    continue_w: 'تابع المشاهدة', fav_channels: 'قنواتك المفضلة', fav_vod: 'المفضلة', recent_movies: 'أُضيف حديثًا: أفلام', recent_series: 'أُضيف حديثًا: مسلسلات',
    history: 'سجل المشاهدة', favorites: 'المفضلة', all: 'الكل', view_all: 'عرض الكل', loading: 'جارٍ التحميل…', first_load: 'التحميل الأول قد يأخذ وقتًا حسب حجم القائمة',
    no_results: 'لا توجد نتائج', empty: 'لا يوجد شيء هنا بعد', retry: 'إعادة المحاولة', failed: 'فشل التحميل', newest: 'الأحدث', by_name: 'الاسم', sort: 'الترتيب',
    watch: 'مشاهدة', resume: 'متابعة', add_fav: 'إضافة للمفضلة', rem_fav: 'إزالة من المفضلة', no_plot: 'لا يتوفر وصف حاليًا', season: 'الموسم', episode: 'الحلقة',
    back_live: 'العودة للبث المباشر', subs: 'ترجمة', sub_file: 'إضافة ملف ترجمة (SRT / VTT)', sub_off: 'إيقاف الترجمة', audio: 'مسار الصوت', quality: 'الجودة', speed: 'السرعة', auto: 'تلقائي',
    close: 'إغلاق', next: 'التالي', prev: 'السابق', play_fail: 'تعذّر تشغيل هذا المحتوى', play_fail_hint: 'جرّب مرة ثانية أو اختر عنصرًا آخر', unsupported: 'المتصفح لا يدعم هذه الصيغة',
    lang: 'اللغة', account: 'الحساب', status: 'الحالة', expires: 'ينتهي', connections: 'الاتصالات المسموحة', active: 'المستخدمة الآن', logout: 'تسجيل الخروج',
    clear_hist: 'مسح السجل', clear_prog: 'مسح التقدم', clear_fav: 'مسح المفضلة', done: 'تم', confirm: 'متأكد؟',
    now: 'الآن', local_note: 'المفضلة والسجل محفوظة على هذا المتصفح فقط.', on_air: 'مباشر', no_epg: '',
  },
  en: {
    home: 'Home', movies: 'Movies', series: 'Series', live: 'Live TV', library: 'My Library', settings: 'Settings', search: 'Search',
    search_ph: 'Search movies, series or channels...', login_title: 'Ayham Player', login_sub: 'Enter your password to continue', password: 'Password', enter: 'Sign in',
    wrong_pw: 'Wrong password', too_many: 'Too many attempts, try later', conn_fail: 'Could not reach the server',
    continue_w: 'Continue watching', fav_channels: 'Favorite channels', fav_vod: 'Favorites', recent_movies: 'Recently added: movies', recent_series: 'Recently added: series',
    history: 'Watch history', favorites: 'Favorites', all: 'All', view_all: 'View all', loading: 'Loading…', first_load: 'The first load may take a while for large lists',
    no_results: 'No results', empty: 'Nothing here yet', retry: 'Retry', failed: 'Load failed', newest: 'Newest', by_name: 'Name', sort: 'Sort',
    watch: 'Watch', resume: 'Resume', add_fav: 'Add to favorites', rem_fav: 'Remove from favorites', no_plot: 'No description available', season: 'Season', episode: 'Episode',
    back_live: 'Back to live', subs: 'Subtitles', sub_file: 'Add subtitle file (SRT / VTT)', sub_off: 'Subtitles off', audio: 'Audio track', quality: 'Quality', speed: 'Speed', auto: 'Auto',
    close: 'Close', next: 'Next', prev: 'Previous', play_fail: 'Could not play this content', play_fail_hint: 'Try again or pick something else', unsupported: 'This browser cannot play this format',
    lang: 'Language', account: 'Account', status: 'Status', expires: 'Expires', connections: 'Allowed connections', active: 'In use now', logout: 'Sign out',
    clear_hist: 'Clear history', clear_prog: 'Clear progress', clear_fav: 'Clear favorites', done: 'Done', confirm: 'Sure?',
    now: 'Now', local_note: 'Favorites and history are stored in this browser only.', on_air: 'LIVE', no_epg: '',
  },
};
let lang = LS.get('lang', 'ar');
if (!T[lang]) lang = 'ar';
const t = (k) => T[lang][k] ?? T.en[k] ?? k;
function applyLang() { document.documentElement.lang = lang; document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr'; }

// ------------------------------------------------------------------ الشبكة والبيانات
async function http(url, opt) {
  const r = await fetch(url, opt);
  if (r.status === 401 && !url.startsWith('/api/login') && !url.startsWith('/api/me')) { showLogin(); throw new Error('auth'); }
  return r;
}
async function api(action, params = {}) {
  const q = new URLSearchParams(Object.assign({ action }, params));
  const r = await http('/api/xt?' + q.toString());
  if (!r.ok) throw new Error('http ' + r.status);
  return r.json();
}
const KINDS = {
  live: { cats: 'get_live_categories', list: 'get_live_streams', p: 'l' },
  vod: { cats: 'get_vod_categories', list: 'get_vod_streams', p: 'v' },
  series: { cats: 'get_series_categories', list: 'get_series', p: 's' },
};
const store = { live: null, vod: null, series: null };
const loadingP = {};
function yearOf(x) {
  const y = x.year || x.releaseDate || x.releasedate || '';
  const m = String(y).match(/(19|20)\d{2}/);
  return m ? m[0] : '';
}
function normItem(kind, x) {
  if (kind === 'live') return { key: 'l' + x.stream_id, kind, id: String(x.stream_id), name: String(x.name || ''), img: x.stream_icon || '', cat: String(x.category_id ?? ''), added: +x.added || 0 };
  if (kind === 'vod') return { key: 'v' + x.stream_id, kind, id: String(x.stream_id), name: String(x.name || ''), img: x.stream_icon || '', cat: String(x.category_id ?? ''), rating: x.rating && x.rating !== '0' ? String(x.rating) : '', added: +x.added || 0, ext: x.container_extension || 'mp4', year: yearOf(x) };
  return { key: 's' + x.series_id, kind, id: String(x.series_id), name: String(x.name || ''), img: x.cover || '', cat: String(x.category_id ?? ''), rating: x.rating && x.rating !== '0' ? String(x.rating) : '', added: +x.last_modified || 0, year: yearOf(x) };
}
function loadKind(kind) {
  if (store[kind]) return Promise.resolve(store[kind]);
  if (loadingP[kind]) return loadingP[kind];
  loadingP[kind] = (async () => {
    const [cats, list] = await Promise.all([api(KINDS[kind].cats), api(KINDS[kind].list)]);
    const items = (Array.isArray(list) ? list : []).map((x) => normItem(kind, x));
    for (const i of items) i.n = normText(i.name);
    const cs = (Array.isArray(cats) ? cats : []).map((c) => ({ id: String(c.category_id), name: c.category_name || '' }));
    const byKey = new Map(items.map((i) => [i.key, i]));
    store[kind] = { cats: cs, items, byKey };
    return store[kind];
  })();
  loadingP[kind].catch(() => {}).finally(() => { loadingP[kind] = null; });
  return loadingP[kind];
}

// ------------------------------------------------------------------ بيانات محلية (المفضلة / السجل / التقدم)
const meta = (i) => ({ key: i.key, kind: i.kind, id: i.id, name: i.name, img: i.img || '', ext: i.ext || '', cat: i.cat || '', rating: i.rating || '', year: i.year || '' });
let fav = LS.get('fav', {});
let hist = LS.get('hist', []);
let prog = LS.get('prog', {});
const isFav = (k) => !!fav[k];
function toggleFav(i) { if (fav[i.key]) delete fav[i.key]; else fav[i.key] = meta(i); LS.set('fav', fav); }
function addHist(i) {
  hist = [meta(i)].concat(hist.filter((h) => h.key !== i.key)).slice(0, 120);
  LS.set('hist', hist);
}
function saveProg(key, m, pos, dur) {
  if (!dur || !isFinite(dur) || dur < 30) return;
  if (pos / dur > 0.93) { delete prog[key]; } else if (pos > 10) { prog[key] = { m, pos, dur, t: Date.now() }; }
  LS.set('prog', prog);
}

// ------------------------------------------------------------------ مكوّنات العرض
function imgTag(url) {
  if (!url) return '';
  return `<img loading="lazy" decoding="async" src="/i?u=${encodeURIComponent(url)}" alt="">`;
}
function card(i, o = {}) {
  const sub = (i.kind === 'live') ? '' : [i.year, i.rating ? '★ ' + i.rating : ''].filter(Boolean).join(' • ');
  const p = o.prog && prog[i.key] ? `<div class="bar"><i style="width:${Math.min(100, Math.round(prog[i.key].pos / prog[i.key].dur * 100))}%"></i></div>` : '';
  return `<a class="card ${i.kind === 'live' ? 'live' : ''}" tabindex="0" role="button" data-open="${esc(i.key)}"><div class="poster"><span class="ph">${esc((i.name || '?').trim().charAt(0))}</span>${imgTag(i.img)}${p}</div><div class="t">${esc(i.name)}</div>${sub ? `<div class="s">${esc(sub)}</div>` : ''}</a>`;
}
const row = (title, items, link, o) => items.length ? `<h2 class="h"><span>${esc(title)}</span>${link ? `<a href="${link}">${esc(t('view_all'))}</a>` : ''}</h2><div class="row">${items.map((i) => card(i, o)).join('')}</div>` : '';
const spinner = (msg) => `<div class="loading"><div class="spin"></div><div>${esc(msg || t('loading'))}</div></div>`;
function toast(msg) {
  const el = $('#toast');
  el.textContent = msg; el.hidden = false;
  clearTimeout(toast.h); toast.h = setTimeout(() => { el.hidden = true; }, 2600);
}
// صور مكسورة: نخفيها ويبقى الحرف الأول
document.addEventListener('error', (e) => { if (e.target && e.target.tagName === 'IMG') e.target.remove(); }, true);

// ------------------------------------------------------------------ الهيكل
let rid = 0; // رقم العرض الحالي (لإلغاء النتائج المتأخرة)
let lastList = []; // قائمة العناصر المعروضة حاليًا (للتنقل بين القنوات)
const NAV = [['home', 'home', '#/home'], ['movies', 'movie', '#/movies'], ['series', 'tv', '#/series'], ['live', 'live', '#/live'], ['library', 'heart', '#/library'], ['settings', 'gear', '#/settings']];

function showLogin(msg) {
  closePlayer(true);
  $('#root').innerHTML = `<div class="login"><form id="lf" autocomplete="on"><img src="/icon.png" alt=""><h1>${esc(t('login_title'))}</h1><p>${esc(t('login_sub'))}</p><input type="password" id="pw" placeholder="${esc(t('password'))}" autocomplete="current-password" required autofocus><div class="err" id="le">${esc(msg || '')}</div><button class="btn" type="submit">${esc(t('enter'))}</button></form></div>`;
  $('#lf').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = $('#lf button'); btn.disabled = true;
    try {
      const r = await fetch('/api/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password: $('#pw').value }) });
      if (r.ok) { showApp(); return; }
      $('#le').textContent = r.status === 429 ? t('too_many') : t('wrong_pw');
    } catch (err) { $('#le').textContent = t('conn_fail'); }
    btn.disabled = false;
  });
}

function showApp() {
  applyLang();
  const navHtml = NAV.map(([k, ic, h]) => `<a class="nav" tabindex="0" href="${h}" data-nav="${k}">${icon(ic)}<span>${esc(t(k))}</span></a>`).join('');
  const bnav = NAV.filter(([k]) => k !== 'settings').map(([k, ic, h]) => `<a href="${h}" data-nav="${k}">${icon(ic)}<span>${esc(t(k))}</span></a>`).join('');
  $('#root').innerHTML = `<div class="app"><aside class="side"><div class="brand"><img src="/icon.png" alt=""><span>Ayham <b>Player</b></span></div>${navHtml}<div class="grow"></div><small>${esc(t('local_note'))}</small></aside><div class="main"><div class="top"><div class="search">${icon('search')}<input type="search" id="gs" placeholder="${esc(t('search_ph'))}" autocomplete="off"></div><div class="sp"></div><a class="btn ghost sm" href="#/settings" aria-label="${esc(t('settings'))}">${icon('gear')}</a></div><div class="page" id="view"></div></div><nav class="bnav">${bnav}</nav></div>`;
  const gs = $('#gs');
  const go = debounce(() => { const q = gs.value.trim(); if (q.length >= 2) location.hash = '#/search?q=' + encodeURIComponent(q); }, 350);
  gs.addEventListener('input', go);
  gs.addEventListener('keydown', (e) => { if (e.key === 'Enter') { const q = gs.value.trim(); if (q) location.hash = '#/search?q=' + encodeURIComponent(q); } });
  route();
}

// ------------------------------------------------------------------ الموجّه
function parseHash() {
  const h = location.hash.replace(/^#\/?/, '');
  const [pathPart, qs] = h.split('?');
  const parts = pathPart.split('/').filter(Boolean);
  return { parts, q: new URLSearchParams(qs || '') };
}
function setNav(k) { $$('[data-nav]').forEach((a) => a.classList.toggle('on', a.dataset.nav === k)); }
function route() {
  if (!$('#view')) return;
  const { parts, q } = parseHash();
  const my = ++rid;
  window.scrollTo(0, 0);
  const p0 = parts[0] || 'home';
  if (p0 === 'movies') { setNav('movies'); return browseView('vod', my); }
  if (p0 === 'series') { setNav('series'); return browseView('series', my); }
  if (p0 === 'live') { setNav('live'); return browseView('live', my); }
  if (p0 === 'library') { setNav('library'); return libraryView(my, q.get('tab') || 'fav'); }
  if (p0 === 'settings') { setNav('settings'); return settingsView(my); }
  if (p0 === 'search') { setNav(''); return searchView(my, q.get('q') || ''); }
  if (p0 === 'm' && parts[1]) { setNav('movies'); return movieDetail(my, parts[1]); }
  if (p0 === 's' && parts[1]) { setNav('series'); return seriesDetail(my, parts[1]); }
  setNav('home');
  return homeView(my);
}
window.addEventListener('hashchange', () => { if (!playerOpen) route(); });

// ------------------------------------------------------------------ عرض: الرئيسية
async function homeView(my) {
  const v = $('#view');
  v.innerHTML = '<div id="hrows"></div>' + spinner(t('first_load'));
  const draw = (movies, series) => {
    if (my !== rid) return;
    const cont = Object.values(prog).sort((a, b) => b.t - a.t).slice(0, 20).map((x) => x.m);
    const favCh = Object.values(fav).filter((x) => x.kind === 'live');
    const favVod = Object.values(fav).filter((x) => x.kind !== 'live');
    const rm = movies ? movies.items.filter((i) => i.added).sort((a, b) => b.added - a.added).slice(0, 24) : [];
    const rs = series ? series.items.filter((i) => i.added).sort((a, b) => b.added - a.added).slice(0, 24) : [];
    lastList = [];
    const html = row(t('continue_w'), cont, '#/library?tab=prog', { prog: true }) + row(t('fav_channels'), favCh, '#/library') + row(t('fav_vod'), favVod, '#/library') +
      row(t('recent_movies'), rm, '#/movies') + row(t('recent_series'), rs, '#/series');
    $('#hrows').innerHTML = html || '';
    const sp = $('.loading', v); if (sp && (movies || series)) sp.remove();
    if (!html && movies && series) $('#hrows').innerHTML = `<div class="empty">${esc(t('empty'))}</div>`;
  };
  draw(null, null);
  try {
    const [m, s] = await Promise.all([loadKind('vod').catch(() => null), loadKind('series').catch(() => null)]);
    draw(m, s);
    if (!m && !s && my === rid) v.innerHTML = `<div class="empty">${esc(t('failed'))}<br><button class="btn" id="rt">${esc(t('retry'))}</button></div>`, $('#rt').onclick = route;
  } catch (e) { /* مصادقة */ }
}

// ------------------------------------------------------------------ عرض: الأقسام (أفلام / مسلسلات / قنوات)
const uiState = { vod: { cat: 'all', q: '', sort: 'new' }, series: { cat: 'all', q: '', sort: 'new' }, live: { cat: 'all', q: '', sort: 'name' } };
async function browseView(kind, my) {
  const v = $('#view');
  const st = uiState[kind];
  v.innerHTML = spinner(t('first_load'));
  let data;
  try { data = await loadKind(kind); } catch (e) {
    if (my !== rid) return;
    v.innerHTML = `<div class="empty">${esc(t('failed'))}<br><br><button class="btn" id="rt">${esc(t('retry'))}</button></div>`;
    $('#rt').onclick = route; return;
  }
  if (my !== rid) return;
  v.innerHTML = `<div class="tools"><input type="search" id="bq" placeholder="${esc(t('search_ph'))}" value="${esc(st.q)}" style="max-width:340px"><select id="bs" aria-label="${esc(t('sort'))}"><option value="new">${esc(t('newest'))}</option><option value="name">${esc(t('by_name'))}</option></select></div><div class="chips" id="bc"></div><div id="bg"></div>`;
  $('#bs').value = st.sort;
  const chips = [{ id: 'all', name: t('all') }].concat(data.cats);
  $('#bc').innerHTML = chips.map((c) => `<span class="chip ${st.cat === c.id ? 'on' : ''}" tabindex="0" role="button" data-cat="${esc(c.id)}">${esc(c.name)}</span>`).join('');
  let io = null;
  const render = () => {
    if (my !== rid) return;
    if (io) io.disconnect();
    const q = normText(st.q);
    const words = q.split(' ').filter(Boolean);
    let list = data.items;
    if (st.cat !== 'all') list = list.filter((i) => i.cat === st.cat);
    if (words.length) list = list.filter((i) => words.every((w) => i.n.includes(w)));
    list = list.slice();
    if (st.sort === 'name') list.sort((a, b) => a.n.localeCompare(b.n, lang)); else list.sort((a, b) => b.added - a.added);
    lastList = list;
    const g = $('#bg');
    if (!list.length) { g.innerHTML = `<div class="empty">${esc(t('no_results'))}</div>`; return; }
    g.innerHTML = '<div class="grid"></div><div id="sent" style="height:1px"></div>';
    const grid = $('.grid', g);
    let n = 0;
    const more = () => {
      if (n >= list.length) return;
      grid.insertAdjacentHTML('beforeend', list.slice(n, n + 60).map((i) => card(i)).join(''));
      n += 60;
    };
    more();
    io = new IntersectionObserver((es) => { if (es.some((e) => e.isIntersecting)) more(); }, { rootMargin: '900px' });
    io.observe($('#sent', g));
  };
  const rd = debounce(render, 250);
  $('#bq').addEventListener('input', (e) => { st.q = e.target.value; rd(); });
  $('#bs').addEventListener('change', (e) => { st.sort = e.target.value; render(); });
  $('#bc').addEventListener('click', (e) => {
    const c = e.target.closest('[data-cat]'); if (!c) return;
    st.cat = c.dataset.cat;
    $$('#bc .chip').forEach((x) => x.classList.toggle('on', x === c));
    render();
  });
  render();
}

// ------------------------------------------------------------------ عرض: البحث
async function searchView(my, q) {
  const v = $('#view');
  const gs = $('#gs'); if (gs && gs.value !== q) gs.value = q;
  const words = normText(q).split(' ').filter(Boolean);
  if (!words.length) { v.innerHTML = `<div class="empty">${esc(t('search_ph'))}</div>`; return; }
  v.innerHTML = spinner(t('first_load'));
  const res = await Promise.all(['live', 'vod', 'series'].map((k) => loadKind(k).catch(() => null)));
  if (my !== rid) return;
  const out = [['live', t('live'), res[0]], ['vod', t('movies'), res[1]], ['series', t('series'), res[2]]].map(([k, title, d]) => {
    if (!d) return '';
    const list = d.items.filter((i) => words.every((w) => i.n.includes(w))).slice(0, 120);
    return list.length ? `<h2 class="h"><span>${esc(title)} (${list.length}${list.length === 120 ? '+' : ''})</span></h2><div class="grid">${list.map((i) => card(i)).join('')}</div>` : '';
  }).join('');
  v.innerHTML = out || `<div class="empty">${esc(t('no_results'))}</div>`;
  lastList = [];
}

// ------------------------------------------------------------------ عرض: المكتبة
function libraryView(my, tab) {
  const v = $('#view');
  const tabs = [['fav', t('favorites')], ['hist', t('history')], ['prog', t('continue_w')]];
  let items;
  if (tab === 'hist') items = hist;
  else if (tab === 'prog') items = Object.values(prog).sort((a, b) => b.t - a.t).map((x) => x.m);
  else items = Object.values(fav);
  lastList = items.filter((i) => i.kind === 'live');
  v.innerHTML = `<div class="chips">${tabs.map(([k, n]) => `<a class="chip ${tab === k ? 'on' : ''}" href="#/library?tab=${k}">${esc(n)}</a>`).join('')}</div>` +
    (items.length ? `<div class="grid">${items.map((i) => card(i, { prog: tab === 'prog' })).join('')}</div>` : `<div class="empty">${esc(t('empty'))}</div>`);
}

// ------------------------------------------------------------------ عرض: الإعدادات
async function settingsView(my) {
  const v = $('#view');
  v.innerHTML = `<h2 class="h"><span>${esc(t('settings'))}</span></h2>
  <div class="tools" style="margin:10px 0"><span>${esc(t('lang'))}:</span><button class="btn ghost sm" data-lang="ar">العربية</button><button class="btn ghost sm" data-lang="en">English</button></div>
  <h2 class="h"><span>${esc(t('account'))}</span></h2><div id="acc" class="plot">${esc(t('loading'))}</div>
  <div class="acts" style="margin-top:22px"><button class="btn ghost sm" data-clear="hist">${esc(t('clear_hist'))}</button><button class="btn ghost sm" data-clear="prog">${esc(t('clear_prog'))}</button><button class="btn ghost sm" data-clear="fav">${esc(t('clear_fav'))}</button></div>
  <div class="acts"><button class="btn" id="lo">${esc(t('logout'))}</button></div><p class="plot">${esc(t('local_note'))}</p>`;
  $('#lo').onclick = async () => { await fetch('/api/logout', { method: 'POST' }); showLogin(); };
  try {
    const a = await api('account');
    if (my !== rid) return;
    const exp = a.exp_date && +a.exp_date ? new Date(+a.exp_date * 1000).toLocaleDateString(lang === 'ar' ? 'ar-IQ' : 'en-GB') : '—';
    $('#acc').innerHTML = `${esc(t('status'))}: <b>${esc(a.status || '—')}</b><br>${esc(t('expires'))}: <b>${esc(exp)}</b><br>${esc(t('connections'))}: <b>${esc(a.max_connections || '—')}</b><br>${esc(t('active'))}: <b>${esc(a.active_cons ?? '—')}</b>`;
  } catch (e) { if ($('#acc')) $('#acc').textContent = t('failed'); }
}

// ------------------------------------------------------------------ عرض: التفاصيل
const favBtn = (i) => `<button class="btn ghost" id="fv">${icon('heart')}<span>${esc(t(isFav(i.key) ? 'rem_fav' : 'add_fav'))}</span></button>`;
function bindFav(i) { const b = $('#fv'); if (b) b.onclick = () => { toggleFav(i); b.querySelector('span').textContent = t(isFav(i.key) ? 'rem_fav' : 'add_fav'); }; }
async function movieDetail(my, id) {
  const v = $('#view');
  v.innerHTML = spinner();
  let info;
  try { info = await api('get_vod_info', { vod_id: id }); } catch (e) { if (my === rid) v.innerHTML = `<div class="empty">${esc(t('failed'))}</div>`; return; }
  if (my !== rid) return;
  const inf = info.info || {}, md = info.movie_data || {};
  const base = store.vod && store.vod.byKey.get('v' + id);
  const item = base || { key: 'v' + id, kind: 'vod', id: String(id), name: md.name || inf.name || '', img: inf.movie_image || '', ext: md.container_extension || 'mp4', year: yearOf(inf), rating: inf.rating && inf.rating !== '0' ? String(inf.rating) : '' };
  item.ext = item.ext || md.container_extension || 'mp4';
  const bd = Array.isArray(inf.backdrop_path) ? inf.backdrop_path[0] : '';
  const pr = prog[item.key];
  const metaLine = [yearOf(inf) || item.year, inf.genre, inf.duration, inf.rating && inf.rating !== '0' ? '★ ' + inf.rating : ''].filter(Boolean);
  v.innerHTML = `<div class="detail"><div class="poster">${imgTag(item.img || inf.movie_image)}</div><div><h1>${esc(item.name)}</h1><div class="meta">${metaLine.map((x) => `<span>${esc(x)}</span>`).join('')}</div><p class="plot">${esc(inf.plot || inf.description || t('no_plot'))}</p><div class="acts"><button class="btn" id="pl">${icon('play')}<span>${esc(pr ? t('resume') + ' ' + fmtTime(pr.pos) : t('watch'))}</span></button>${favBtn(item)}</div></div></div>`;
  void bd;
  $('#pl').onclick = () => playItem(item, { startAt: pr ? pr.pos : 0 });
  bindFav(item);
}
async function seriesDetail(my, id) {
  const v = $('#view');
  v.innerHTML = spinner();
  let info;
  try { info = await api('get_series_info', { series_id: id }); } catch (e) { if (my === rid) v.innerHTML = `<div class="empty">${esc(t('failed'))}</div>`; return; }
  if (my !== rid) return;
  const inf = info.info || {};
  const base = store.series && store.series.byKey.get('s' + id);
  const item = base || { key: 's' + id, kind: 'series', id: String(id), name: inf.name || '', img: inf.cover || '', year: yearOf(inf), rating: inf.rating && inf.rating !== '0' ? String(inf.rating) : '' };
  const eps = info.episodes && !Array.isArray(info.episodes) ? info.episodes : {};
  const seasons = Object.keys(eps).sort((a, b) => +a - +b);
  const flat = [];
  for (const s of seasons) for (const e of eps[s]) flat.push({ key: 'e' + e.id, kind: 'ep', id: String(e.id), ext: e.container_extension || 'mp4', name: `${item.name} • S${s}E${e.episode_num}`, title: e.title || (t('episode') + ' ' + e.episode_num), season: s, num: e.episode_num, dur: e.info && e.info.duration, img: (e.info && e.info.movie_image) || item.img, series: meta(item) });
  const metaLine = [yearOf(inf) || item.year, inf.genre, inf.rating && inf.rating !== '0' ? '★ ' + inf.rating : ''].filter(Boolean);
  v.innerHTML = `<div class="detail"><div class="poster">${imgTag(item.img || inf.cover)}</div><div><h1>${esc(item.name)}</h1><div class="meta">${metaLine.map((x) => `<span>${esc(x)}</span>`).join('')}</div><p class="plot">${esc(inf.plot || t('no_plot'))}</p><div class="acts">${favBtn(item)}</div></div></div><div class="seasons" id="ss">${seasons.map((s, n) => `<span class="chip ${n === 0 ? 'on' : ''}" tabindex="0" role="button" data-season="${esc(s)}">${esc(t('season'))} ${esc(s)}</span>`).join('')}</div><div class="eps" id="eps"></div>`;
  bindFav(item);
  const showSeason = (s) => {
    const list = flat.filter((e) => e.season === s);
    $('#eps').innerHTML = list.map((e) => {
      const pr = prog[e.key];
      return `<div class="ep" tabindex="0" role="button" data-ep="${esc(e.key)}"><div class="n">${esc(e.num)}</div><div style="flex:1;min-width:0"><div style="font-weight:700">${esc(e.title)}</div><div class="s" style="color:var(--dim);font-size:12px">${esc(e.dur || '')}</div></div>${icon('play')}${pr ? `<div class="bar"><i style="width:${Math.min(100, Math.round(pr.pos / pr.dur * 100))}%"></i></div>` : ''}</div>`;
    }).join('');
    $$('#ss .chip').forEach((c) => c.classList.toggle('on', c.dataset.season === s));
  };
  if (seasons.length) showSeason(seasons[0]); else $('#eps').innerHTML = `<div class="empty">${esc(t('empty'))}</div>`;
  $('#ss').addEventListener('click', (e) => { const c = e.target.closest('[data-season]'); if (c) showSeason(c.dataset.season); });
  $('#eps').addEventListener('click', (e) => {
    const c = e.target.closest('[data-ep]'); if (!c) return;
    const ep = flat.find((x) => x.key === c.dataset.ep);
    playItem(ep, { list: flat, startAt: prog[ep.key] ? prog[ep.key].pos : 0 });
  });
}

// ------------------------------------------------------------------ فتح العناصر
function findItem(key) {
  const k = { l: 'live', v: 'vod', s: 'series' }[key[0]];
  const d = k && store[k];
  if (d && d.byKey.has(key)) return d.byKey.get(key);
  return fav[key] || hist.find((h) => h.key === key) || (prog[key] && prog[key].m) || null;
}
document.addEventListener('click', (e) => {
  const c = e.target.closest('[data-open]');
  if (c) { e.preventDefault(); openItem(c.dataset.open); return; }
  const lg = e.target.closest('[data-lang]');
  if (lg) { lang = lg.dataset.lang; LS.set('lang', lang); showApp(); return; }
  const cl = e.target.closest('[data-clear]');
  if (cl) {
    if (cl.dataset.armed) {
      const w = cl.dataset.clear;
      if (w === 'hist') { hist = []; LS.set('hist', hist); } else if (w === 'prog') { prog = {}; LS.set('prog', prog); } else { fav = {}; LS.set('fav', fav); }
      toast(t('done')); delete cl.dataset.armed;
    } else { cl.dataset.armed = '1'; toast(t('confirm')); setTimeout(() => delete cl.dataset.armed, 4000); }
  }
});
document.addEventListener('keydown', (e) => {
  if (playerOpen) return;
  if ((e.key === 'Enter' || e.key === ' ') && e.target && e.target.matches && e.target.matches('[data-open],[data-cat],[data-season],[data-ep],.chip,.nav')) { e.preventDefault(); e.target.click(); }
});
function openItem(key) {
  const i = findItem(key);
  if (!i) return;
  if (i.kind === 'live') { playItem(i, { list: lastList.filter((x) => x.kind === 'live') }); return; }
  location.hash = (i.kind === 'vod' ? '#/m/' : '#/s/') + i.id;
}

// ------------------------------------------------------------------ المشغّل
let playerOpen = false, hls = null, cur = null, ctx = null, idleT = 0, saveT = 0, netRetry = 0, triedAlt = false;
function urlFor(i) {
  if (i.kind === 'live') return `/s/live/${i.id}`;
  if (i.kind === 'vod') return `/s/movie/${i.id}.${i.ext || 'mp4'}`;
  return `/s/series/${i.id}.${i.ext || 'mp4'}`;
}
function playItem(item, o = {}) {
  cur = item;
  ctx = { list: o.list && o.list.length ? o.list : null, startAt: o.startAt || 0 };
  if (item.kind === 'vod' || item.kind === 'live') addHist(item); else if (item.series) addHist(Object.assign({}, item.series));
  openPlayer();
}
function openPlayer() {
  const P = $('#player');
  const first = !playerOpen;
  playerOpen = true;
  if (first) history.pushState({ p: 1 }, '');
  P.hidden = false; P.classList.remove('idle');
  const i = cur;
  const isLive = i.kind === 'live';
  const hasList = ctx.list && ctx.list.length > 1;
  const title = i.kind === 'ep' ? i.series.name : i.name;
  const subt = i.kind === 'ep' ? `S${i.season}E${i.num} • ${i.title}` : '';
  P.innerHTML = `<video id="vd" playsinline autoplay ${isLive ? '' : 'controls'} ${isLive ? 'controls' : ''}></video>
  <div class="pbar" id="pb"><button class="ibtn" id="pc" aria-label="${esc(t('close'))}">${icon('close')}</button><div class="title">${esc(title)} <span class="sub" id="psub">${esc(subt)}</span></div>
  ${hasList ? `<button class="ibtn" id="pp" aria-label="${esc(t('prev'))}">${icon('prev')}</button><button class="ibtn" id="pn" aria-label="${esc(t('next'))}">${icon('next')}</button>` : ''}
  ${isLive ? `<button class="btn sm" id="pl2" hidden>${esc(t('back_live'))}</button>` : ''}
  <button class="ibtn" id="ps" aria-label="${esc(t('subs'))}">${icon('sub')}</button><button class="ibtn" id="pm" aria-label="${esc(t('settings'))}">${icon('gear')}</button></div><div class="pmsg" id="pmsg"></div><input type="file" id="sf" accept=".srt,.vtt,text/vtt" hidden>`;
  const vd = $('#vd');
  $('#pc').onclick = () => history.back();
  if (hasList) { $('#pp').onclick = () => step(-1); $('#pn').onclick = () => step(1); }
  $('#ps').onclick = () => toggleMenu('subs');
  $('#pm').onclick = () => toggleMenu('main');
  $('#sf').onchange = onSubFile;
  if (isLive && $('#pl2')) $('#pl2').onclick = () => { if (hls && hls.liveSyncPosition) vd.currentTime = hls.liveSyncPosition; else if (vd.seekable.length) vd.currentTime = vd.seekable.end(vd.seekable.length - 1); vd.play().catch(() => {}); };
  netRetry = 0; triedAlt = false;
  attach(vd, urlFor(i), isLive || /\.m3u8$/.test(urlFor(i)), ctx.startAt);
  vd.addEventListener('error', () => onVideoError(vd));
  vd.addEventListener('timeupdate', onTime);
  vd.addEventListener('ended', () => { if (!isLive && ctx.list) step(1, true); });
  vd.addEventListener('playing', () => { $('#pmsg').textContent = ''; $('#pmsg').className = 'pmsg'; });
  vd.addEventListener('waiting', () => { $('#pmsg').textContent = t('loading'); });
  loadSavedSub(vd);
  if (isLive) loadEpg(i);
  kick();
}
function attach(vd, url, m3u8, startAt) {
  destroyHls();
  if (m3u8) {
    if (window.Hls && window.Hls.isSupported()) {
      hls = new window.Hls({ maxBufferLength: 30, backBufferLength: 120, liveSyncDurationCount: 3, enableWorker: true, startPosition: startAt > 0 ? startAt : -1 });
      hls.loadSource(url); hls.attachMedia(vd);
      hls.on(window.Hls.Events.MANIFEST_PARSED, () => { vd.play().catch(() => {}); });
      hls.on(window.Hls.Events.LEVEL_LOADED, (e, d) => { if (cur && cur.kind === 'live') updLive(d); });
      hls.on(window.Hls.Events.ERROR, (e, d) => {
        if (!d.fatal) return;
        if (d.type === window.Hls.ErrorTypes.NETWORK_ERROR && netRetry < 5) { netRetry++; setTimeout(() => hls && hls.startLoad(), 1200 * netRetry); return; }
        if (d.type === window.Hls.ErrorTypes.MEDIA_ERROR && netRetry < 5) { netRetry++; hls.recoverMediaError(); return; }
        fail();
      });
    } else if (vd.canPlayType('application/vnd.apple.mpegurl')) {
      vd.src = url;
      if (startAt > 0) vd.addEventListener('loadedmetadata', () => { vd.currentTime = startAt; }, { once: true });
    } else { fail(t('unsupported')); }
  } else {
    vd.src = url;
    if (startAt > 0) vd.addEventListener('loadedmetadata', () => { vd.currentTime = startAt; }, { once: true });
  }
}
function updLive() {
  const b = $('#pl2'), vd = $('#vd');
  if (!b || !vd || !hls) return;
  const behind = (hls.liveSyncPosition || 0) - vd.currentTime;
  b.hidden = !(behind > 8);
}
function onVideoError(vd) {
  if (hls) return; // hls.js يتولى أخطاءه
  if (cur && cur.kind !== 'live' && !triedAlt) {
    // بعض الصيغ (mkv مثلًا) ما يقبلها المتصفح: نجرب m3u8 للمحتوى نفسه
    triedAlt = true;
    const alt = (cur.kind === 'vod' ? '/s/movie/' : '/s/series/') + cur.id + '.m3u8';
    attach(vd, alt, true, ctx.startAt);
    return;
  }
  fail();
}
function fail(msg) {
  const m = $('#pmsg'); if (!m) return;
  m.className = 'pmsg err';
  m.innerHTML = `${esc(msg || t('play_fail'))}<br><small>${esc(t('play_fail_hint'))}</small>`;
}
function destroyHls() { if (hls) { try { hls.destroy(); } catch (e) { /* ignore */ } hls = null; } }
function onTime() {
  const vd = $('#vd'); if (!vd || !cur) return;
  if (cur.kind === 'live') { updLive(); return; }
  const n = Date.now();
  if (n - saveT > 5000) {
    saveT = n;
    const m = cur.kind === 'ep' ? Object.assign({}, cur) : meta(cur);
    saveProg(cur.key, m, vd.currentTime, vd.duration);
  }
}
async function loadEpg(i) {
  try {
    const r = await api('get_short_epg', { stream_id: i.id, limit: 2 });
    const l = (r.epg_listings || [])[0];
    const el = $('#psub');
    if (l && el && cur === i) el.textContent = t('now') + ': ' + b64utf8(l.title);
  } catch (e) { /* بدون EPG */ }
}
function step(d, auto) {
  if (!ctx.list) return;
  const idx = ctx.list.findIndex((x) => x.key === cur.key);
  const n = idx + d;
  if (n < 0 || n >= ctx.list.length) { if (auto) history.back(); else if (cur.kind === 'live') { cur = ctx.list[(n + ctx.list.length) % ctx.list.length]; ctx.startAt = 0; openPlayer(); } return; }
  cur = ctx.list[n]; ctx.startAt = (prog[cur.key] && prog[cur.key].pos) || 0;
  if (cur.kind === 'live') addHist(cur);
  openPlayer();
}
function closePlayer(silent) {
  const P = $('#player');
  if (!playerOpen && !silent) return;
  const vd = $('#vd');
  if (vd && cur && cur.kind !== 'live') saveProg(cur.key, cur.kind === 'ep' ? Object.assign({}, cur) : meta(cur), vd.currentTime, vd.duration);
  destroyHls();
  if (vd) { try { vd.pause(); vd.removeAttribute('src'); vd.load(); } catch (e) { /* ignore */ } }
  P.hidden = true; P.innerHTML = '';
  playerOpen = false; cur = null; ctx = null;
  clearTimeout(idleT);
  if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
}
window.addEventListener('popstate', () => { if (playerOpen) { closePlayer(); route(); } });

// إخفاء الأزرار عند عدم الحركة
function kick() {
  const P = $('#player'); if (!P) return;
  P.classList.remove('idle');
  clearTimeout(idleT);
  idleT = setTimeout(() => { if (playerOpen && !$('.menu')) P.classList.add('idle'); }, 3500);
}
['mousemove', 'touchstart', 'keydown', 'click'].forEach((ev) => document.addEventListener(ev, () => { if (playerOpen) kick(); }, { passive: true }));
document.addEventListener('keydown', (e) => {
  if (!playerOpen) return;
  const vd = $('#vd'); if (!vd) return;
  if (e.target && /INPUT|SELECT|TEXTAREA/.test(e.target.tagName)) return;
  const seekable = vd.seekable && vd.seekable.length;
  if (e.key === 'Escape') { if ($('.menu')) closeMenu(); else history.back(); }
  else if (e.key === ' ' || e.key === 'k') { e.preventDefault(); vd.paused ? vd.play() : vd.pause(); }
  else if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
    if (!seekable) return;
    e.preventDefault();
    const d = (e.key === 'ArrowRight' ? 1 : -1) * 10;
    const lo = vd.seekable.start(0), hi = vd.seekable.end(vd.seekable.length - 1);
    vd.currentTime = Math.min(hi - 1, Math.max(lo, vd.currentTime + d));
  } else if (e.key === 'ArrowUp') { e.preventDefault(); vd.volume = Math.min(1, vd.volume + 0.1); }
  else if (e.key === 'ArrowDown') { e.preventDefault(); vd.volume = Math.max(0, vd.volume - 0.1); }
  else if (e.key === 'f') { document.fullscreenElement ? document.exitFullscreen() : $('#player').requestFullscreen && $('#player').requestFullscreen(); }
  else if (e.key === 'm') vd.muted = !vd.muted;
  else if (e.key === 'n' && ctx && ctx.list) step(1);
  else if (e.key === 'p' && ctx && ctx.list) step(-1);
}, true);

// القوائم: صوت / جودة / سرعة / ترجمة
function closeMenu() { const m = $('.menu'); if (m) m.remove(); }
function toggleMenu(kind) {
  const open = $('.menu');
  if (open && open.dataset.kind === kind) { closeMenu(); return; }
  closeMenu();
  const vd = $('#vd'); if (!vd) return;
  const m = document.createElement('div');
  m.className = 'menu'; m.dataset.kind = kind;
  const btn = (label, on, fn) => { const b = document.createElement('button'); b.textContent = label; if (on) b.classList.add('on'); b.onclick = () => { fn(); closeMenu(); }; return b; };
  const head = (x) => { const h = document.createElement('h4'); h.textContent = x; m.appendChild(h); };
  if (kind === 'subs') {
    head(t('subs'));
    m.appendChild(btn(t('sub_file'), false, () => $('#sf').click()));
    const tracks = Array.from(vd.textTracks);
    m.appendChild(btn(t('sub_off'), !tracks.some((x) => x.mode === 'showing'), () => tracks.forEach((x) => { x.mode = 'disabled'; })));
    tracks.forEach((tr, n) => m.appendChild(btn(tr.label || ('#' + (n + 1)), tr.mode === 'showing', () => tracks.forEach((x, k) => { x.mode = k === n ? 'showing' : 'disabled'; }))));
  } else {
    if (hls && hls.audioTracks && hls.audioTracks.length > 1) {
      head(t('audio'));
      hls.audioTracks.forEach((a, n) => m.appendChild(btn(a.name || a.lang || ('#' + (n + 1)), hls.audioTrack === n, () => { hls.audioTrack = n; })));
    }
    if (hls && hls.levels && hls.levels.length > 1) {
      head(t('quality'));
      m.appendChild(btn(t('auto'), hls.currentLevel === -1, () => { hls.currentLevel = -1; }));
      hls.levels.forEach((l, n) => m.appendChild(btn((l.height ? l.height + 'p' : Math.round(l.bitrate / 1000) + 'k'), hls.currentLevel === n, () => { hls.currentLevel = n; })));
    }
    if (cur && cur.kind !== 'live') {
      head(t('speed'));
      [0.75, 1, 1.25, 1.5, 2].forEach((s) => m.appendChild(btn(s + 'x', vd.playbackRate === s, () => { vd.playbackRate = s; })));
    }
    head(t('subs'));
    m.appendChild(btn(t('sub_file'), false, () => $('#sf').click()));
  }
  $('#player').appendChild(m);
}

// ------------------------------------------------------------------ الترجمة (ملف محلي)
function srtToVtt(s) {
  return 'WEBVTT\n\n' + s.replace(/\r+/g, '').replace(/^﻿/, '').replace(/(\d{2}:\d{2}:\d{2}),(\d{3})/g, '$1.$2');
}
async function decodeSub(buf) {
  let txt = new TextDecoder('utf-8').decode(buf);
  if (txt.includes('�')) { try { txt = new TextDecoder('windows-1256').decode(buf); } catch (e) { /* keep */ } }
  return txt;
}
function addTrack(vd, vtt, label) {
  Array.from(vd.querySelectorAll('track')).forEach((x) => x.remove());
  const url = URL.createObjectURL(new Blob([vtt], { type: 'text/vtt' }));
  const tr = document.createElement('track');
  tr.kind = 'subtitles'; tr.label = label || 'Subtitle'; tr.srclang = lang; tr.src = url; tr.default = true;
  vd.appendChild(tr);
  setTimeout(() => { if (vd.textTracks[0]) vd.textTracks[0].mode = 'showing'; }, 100);
}
async function onSubFile(e) {
  const f = e.target.files && e.target.files[0]; if (!f || !cur) return;
  try {
    let txt = await decodeSub(await f.arrayBuffer());
    const vtt = /\.vtt$/i.test(f.name) || txt.trimStart().startsWith('WEBVTT') ? txt : srtToVtt(txt);
    addTrack($('#vd'), vtt, f.name.replace(/\.[^.]+$/, '').slice(0, 30));
    if (vtt.length < 600000) LS.set('sub:' + cur.key, { l: f.name.slice(0, 30), v: vtt });
    toast(t('done'));
  } catch (err) { toast(t('failed')); }
  e.target.value = '';
}
function loadSavedSub(vd) {
  if (!cur || cur.kind === 'live') return;
  const s = LS.get('sub:' + cur.key, null);
  if (s && s.v) addTrack(vd, s.v, s.l);
}

// ------------------------------------------------------------------ الإقلاع
async function boot() {
  applyLang();
  try {
    const r = await fetch('/api/me');
    if (r.ok) showApp(); else showLogin();
  } catch (e) { showLogin(t('conn_fail')); }
}
boot();
})();
