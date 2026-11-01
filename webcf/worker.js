// Ayham Web (خاص): موقع لك وحدك. بيانات اشتراكك (السيرفر/المستخدم/كلمة المرور) تبقى كأسرار على الخادم
// ولا تصل للمتصفح أبدًا. الدخول بكلمة سر واحدة تُحفظ بكوكي موقّع على جهازك.
// الأسرار: XTREAM_URL, XTREAM_USER, XTREAM_PASS, ACCESS_PASSWORD (+ SESSION_SECRET اختياري).
const enc = new TextEncoder();
const json = (o, s = 200, h = {}) => new Response(JSON.stringify(o), { status: s, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store', ...h } });
const COOKIE = 'ap_s';
const DAYS = 90;

export function badHost(h) {
  h = String(h || '').toLowerCase();
  if (!h || h === 'localhost' || h.endsWith('.local') || h.endsWith('.internal') || h.startsWith('[')) return true;
  const m = h.match(/^(\d+)\.(\d+)\.(\d+)\.(\d+)$/);
  if (m) {
    const a = +m[1], b = +m[2];
    if (a === 0 || a === 10 || a === 127 || a >= 224 || (a === 169 && b === 254) || (a === 172 && b >= 16 && b <= 31) || (a === 192 && b === 168) || (a === 100 && b >= 64 && b <= 127)) return true;
  }
  return false;
}

const ownerPw = (env) => (env.ACCESS_PASSWORD && String(env.ACCESS_PASSWORD).length >= 8 ? String(env.ACCESS_PASSWORD) : '');
// الدخول: كلمة المالك (اختيارية) أو كود من لوحة الأدمن ملاحظته فيها web (عبر الربط LICENSE).
// سيرفر الاشتراك: يأتي مع كل كود من لوحة الأدمن (لكل زبون سيرفره)، أو من أسرار الموقع كاحتياط.
const envAcct = (env) => (env.XTREAM_URL && env.XTREAM_USER && env.XTREAM_PASS ? { url: String(env.XTREAM_URL).replace(/\/+$/, ''), user: String(env.XTREAM_USER), pass: String(env.XTREAM_PASS) } : null);
const hasSecret = (env) => !!(env.SESSION_SECRET || env.LINK_KEY || ownerPw(env));
function configured(env) { return hasSecret(env) && !!(env.LICENSE || (ownerPw(env) && envAcct(env))); }
const secret = (env) => env.SESSION_SECRET || ('k|' + (env.LINK_KEY || '') + '|' + (env.ACCESS_PASSWORD || ''));

async function hmac(env, msg) {
  const key = await crypto.subtle.importKey('raw', enc.encode(secret(env)), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const sig = new Uint8Array(await crypto.subtle.sign('HMAC', key, enc.encode(msg)));
  let s = ''; for (const b of sig) s += b.toString(16).padStart(2, '0');
  return s;
}
function safeEq(a, b) { a = String(a || ''); b = String(b || ''); let d = a.length ^ b.length; for (let i = 0; i < Math.max(a.length, b.length); i++) d |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0); return d === 0; }
// لو السر على شكل كود (حروف وأرقام وشرطات) نتجاهل الشرطات وحالة الأحرف.
const normSecret = (x) => { x = String(x || ''); return /^[A-Za-z0-9-]+$/.test(x) ? x.toUpperCase().replace(/-/g, '') : x; };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const normCode = (x) => String(x || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
const normDev = (x) => String(x || '').replace(/[^A-Za-z0-9]/g, '').slice(0, 24);
// التوكن: انتهاء.جهة.جهاز.توقيع  (جهة = O للمالك أو كود الترخيص)
async function makeSession(env, sub = 'O', dev = '-') { const exp = Date.now() + DAYS * 864e5; return exp + '.' + sub + '.' + dev + '.' + (await hmac(env, 'sess|' + exp + '|' + sub + '|' + dev)); }
// نداء داخلي لسيرفر الترخيص (عبر الربط + مفتاح مشترك): يتحقق من الكود ويرجع سيرفره.
async function licCheck(env, code, dev) {
  try {
    const r = await env.LICENSE.fetch(new Request('https://license/v1/creds', { method: 'POST', headers: { 'x-link': env.LINK_KEY || '' }, body: JSON.stringify({ code, device: 'w' + dev, name: 'Web' }) }));
    return await r.json();
  } catch (e) { return { ok: false, err: 'server' }; }
}
const acctFrom = (r) => (r.srv && r.srv.url ? { url: String(r.srv.url).replace(/\/+$/, ''), user: String(r.srv.user || ''), pass: String(r.srv.pass || '') } : null);
const licCache = new Map(); // كود+جهاز -> {t, acct} لتخفيف الاتصال بالترخيص (4 دقائق)
// يرجع بيانات الحساب إن كانت الجلسة صالحة، وإلا null. acct.url فارغ = لا يوجد سيرفر.
async function validSession(env, req) {
  const m = (req.headers.get('cookie') || '').match(new RegExp('(?:^|;\\s*)' + COOKIE + '=([^;]+)'));
  if (!m) return null;
  const [exp, sub, dev, sig] = decodeURIComponent(m[1]).split('.');
  if (!exp || !sub || !dev || !sig || +exp < Date.now()) return null;
  if (!safeEq(sig, await hmac(env, 'sess|' + exp + '|' + sub + '|' + dev))) return null;
  if (sub === 'O') return ownerPw(env) ? (envAcct(env) || { url: '' }) : null;
  if (!env.LICENSE) return null;
  const k = sub + '|' + dev, c = licCache.get(k);
  if (c && Date.now() - c.t < 240000) return c.acct;
  const r = await licCheck(env, sub, dev);
  const acct = r.ok && r.web ? (acctFrom(r) || envAcct(env) || { url: '' }) : null;
  licCache.set(k, { t: Date.now(), acct });
  if (licCache.size > 500) licCache.clear();
  return acct;
}

const px = async (env, abs) => '/p?u=' + encodeURIComponent(abs) + '&s=' + (await hmac(env, 'u|' + abs));

export async function rewrite(env, text, base) {
  const out = [];
  for (const line of text.split(/\r?\n/)) {
    if (!line.trim()) { out.push(line); continue; }
    if (line.startsWith('#')) {
      let l = line; const re = /URI="([^"]+)"/g; let m; const reps = [];
      while ((m = re.exec(line))) { try { reps.push([m[0], 'URI="' + (await px(env, new URL(m[1], base).toString())) + '"']); } catch (e) { /* keep */ } }
      for (const [a, b] of reps) l = l.replace(a, b);
      out.push(l);
    } else {
      try { out.push(await px(env, new URL(line.trim(), base).toString())); } catch (e) { out.push(line); }
    }
  }
  return out.join('\n');
}

// نجرّب أولًا نفس هوية التطبيق (تعمل مع سيرفرك)، وإن رُفضنا (403/401/406) نجرّب هويات أخرى مألوفة للسيرفرات.
const UAS = ['AyhamPlayer/1.0', 'VLC/3.0.20 LibVLC/3.0.20', 'okhttp/4.12.0', 'Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'];

// يجلب الرابط مع تتبّع التحويلات يدويًا، ويعيد كتابة قوائم m3u8 ليمر كل شيء عبر الوسيط.
export async function fetchUpstream(env, req, target, fetcher = fetch, opts = {}) {
  let cur;
  try { cur = new URL(target); } catch (e) { return json({ ok: false, err: 'bad_url' }, 400); }
  if (!/^https?:$/.test(cur.protocol) || badHost(cur.hostname)) return json({ ok: false, err: 'bad_url' }, 400);
  const headers = new Headers({ 'user-agent': UAS[0], accept: '*/*' });
  const range = req.headers.get('range');
  if (range) headers.set('range', range);
  let res = null;
  for (let i = 0; i < 6; i++) {
    try {
      res = await fetcher(cur.toString(), { method: req.method, headers, redirect: 'manual' });
      for (let u = 1; i === 0 && u < UAS.length && [401, 403, 406].includes(res.status); u++) { // رفض بسبب الهوية؟ جرّب هوية أخرى
        headers.set('user-agent', UAS[u]);
        res = await fetcher(cur.toString(), { method: req.method, headers, redirect: 'manual' });
      }
    } catch (e) { return json({ ok: false, err: 'upstream' }, 502); }
    if (res.status >= 300 && res.status < 400 && res.headers.get('location')) {
      try { cur = new URL(res.headers.get('location'), cur); } catch (e) { break; }
      if (!/^https?:$/.test(cur.protocol) || badHost(cur.hostname)) return json({ ok: false, err: 'bad_url' }, 400);
      continue;
    }
    break;
  }
  if (!res) return json({ ok: false, err: 'upstream' }, 502);
  const ct = res.headers.get('content-type') || '';
  if (opts.imageOnly && !/^image\//i.test(ct)) return json({ ok: false, err: 'not_image' }, 403);
  if (res.ok && (/mpegurl/i.test(ct) || /\.m3u8?$/i.test(cur.pathname))) {
    const text = await res.text();
    return new Response(req.method === 'HEAD' ? null : await rewrite(env, text, cur), { status: 200, headers: { 'content-type': 'application/vnd.apple.mpegurl', 'cache-control': 'no-store' } });
  }
  const h = new Headers();
  for (const k of ['content-type', 'content-range', 'accept-ranges', 'last-modified']) { const v = res.headers.get(k); if (v) h.set(k, v); }
  if (!res.headers.get('content-encoding')) { const l = res.headers.get('content-length'); if (l) h.set('content-length', l); }
  h.set('cache-control', /^image\//i.test(ct) ? 'private, max-age=86400' : 'no-store');
  return new Response(req.method === 'HEAD' ? null : res.body, { status: res.status, headers: h });
}

const cred = (a) => encodeURIComponent(a.user) + '/' + encodeURIComponent(a.pass);

export async function handle(req, env, fetcher = fetch) {
  const u = new URL(req.url);
  const p = u.pathname;
  const ro = req.method === 'GET' || req.method === 'HEAD';
  if (!['/login', '/logout', '/me', '/x', '/s', '/i', '/p'].some((x) => p === x || p.startsWith(x + '/'))) return null; // ملفات ثابتة
  if (!configured(env)) return json({ ok: false, err: 'not_configured' }, 503);

  if (p === '/login') {
    if (req.method !== 'POST') return json({ ok: false }, 405);
    let b = {}; try { b = JSON.parse((await req.text()).slice(0, 512)); } catch (e) { /* */ }
    const cookie = (tok) => ({ 'set-cookie': COOKIE + '=' + encodeURIComponent(tok) + '; Path=/; Max-Age=' + DAYS * 86400 + '; HttpOnly; Secure; SameSite=Lax' });
    if (ownerPw(env) && safeEq(normSecret(b.password), normSecret(ownerPw(env)))) return json({ ok: true }, 200, cookie(await makeSession(env)));
    const code = normCode(b.password), dev = normDev(b.device) || 'x';
    if (env.LICENSE && code.length === 12) {
      const r = await licCheck(env, code, dev);
      if (r.ok && r.web) {
        const acct = acctFrom(r) || envAcct(env);
        if (!acct) return json({ ok: false, err: 'no_server' }, 403);
        licCache.set(code + '|' + dev, { t: Date.now(), acct });
        return json({ ok: true }, 200, cookie(await makeSession(env, code, dev)));
      }
      if (r.ok) { await sleep(800); return json({ ok: false, err: 'not_web' }, 403); }
      if (r.err === 'revoked' || r.err === 'devices_full' || r.err === 'busy') return json({ ok: false, err: r.err }, 403);
    }
    await sleep(1200); return json({ ok: false, err: 'wrong' }, 401);
  }
  if (p === '/logout') return json({ ok: true }, 200, { 'set-cookie': COOKIE + '=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Lax' });

  // مسار موقّع من الوسيط نفسه (لقطع البث): لا يحتاج كوكي لكنه لا يُزوَّر.
  if (p === '/p') {
    if (!ro) return json({ ok: false }, 405);
    const t = u.searchParams.get('u') || '';
    if (!safeEq(u.searchParams.get('s'), await hmac(env, 'u|' + t))) return json({ ok: false, err: 'sig' }, 403);
    return fetchUpstream(env, req, t, fetcher);
  }

  const acct = await validSession(env, req);
  if (!acct) return json({ ok: false, err: 'auth' }, 401);
  if (p === '/me') return json({ ok: true });
  if (!ro) return json({ ok: false }, 405);
  if (!acct.url && p !== '/i') return json({ ok: false, err: 'no_server' }, 409);

  if (p === '/x') {
    // API Xtream: نضيف بيانات الدخول من الخادم، ونحجب أي حقول حساسة من الردّ.
    const q = new URLSearchParams();
    const action = u.searchParams.get('action') || '';
    if (action && !/^[a-z_]+$/.test(action)) return json({ ok: false, err: 'bad' }, 400);
    for (const k of ['category_id', 'vod_id', 'series_id']) { const v = u.searchParams.get(k); if (v && /^[0-9A-Za-z_-]{1,20}$/.test(v)) q.set(k, v); }
    const target = acct.url + '/player_api.php?username=' + encodeURIComponent(acct.user) + '&password=' + encodeURIComponent(acct.pass) + (action ? '&action=' + action : '') + (q.toString() ? '&' + q.toString() : '');
    const r = await fetchUpstream(env, req, target, fetcher);
    if (r.status >= 400 && r.status !== 401) return json({ ok: false, err: 'upstream', status: r.status }, 502);
    if (!action) {
      // اختبار الدخول: نرجع فقط حالة الاشتراك بدون كلمة المرور
      if (r.status !== 200) return r;
      try { const j = await r.json(); const ui = j.user_info || {}; return json({ ok: String(ui.auth) === '1', status: ui.status || '', exp: ui.exp_date || '', max: ui.max_connections || '' }); } catch (e) { return json({ ok: false, err: 'json' }, 502); }
    }
    return r;
  }
  if (p === '/i') {
    const t = u.searchParams.get('u') || '';
    return fetchUpstream(env, req, t, fetcher, { imageOnly: true });
  }
  if (p.startsWith('/s/')) {
    // /s/live/ID.m3u8  /s/movie/ID.ext  /s/series/ID.ext
    const m = p.match(/^\/s\/(live|movie|series)\/([0-9A-Za-z_-]{1,20})\.([a-z0-9]{2,5})$/);
    if (!m) return json({ ok: false, err: 'bad' }, 400);
    return fetchUpstream(env, req, acct.url + '/' + m[1] + '/' + cred(acct) + '/' + m[2] + '.' + m[3], fetcher);
  }
  return json({ ok: false }, 404);
}

export default {
  async fetch(req, env) {
    const r = await handle(req, env);
    return r || env.ASSETS.fetch(req);
  },
};
