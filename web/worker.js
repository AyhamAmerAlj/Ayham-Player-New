// Ayham Web: وسيط خفيف لتشغيل IPTV على المتصفح (خاصة آيفون).
// - يمنع فتحه لغير حاملي كود ترخيص صالح (عبر خادم التراخيص).
// - يمرّر طلبات Xtream والبث (HLS يُعاد كتابة روابطه ليمر عبر الوسيط).
// - لا يخزّن أي بيانات: بيانات السيرفر تبقى بمتصفح المستخدم فقط.
const json = (o, s = 200) => new Response(JSON.stringify(o), { status: s, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' } });
const okCache = new Map();

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

async function licCheck(env, code, dev) {
  const body = JSON.stringify({ code, device: dev, name: 'Web' });
  const req = new Request('https://license/v1/check', { method: 'POST', body });
  const r = await (env.LICENSE ? env.LICENSE.fetch(req) : fetch(req));
  return r.json();
}

export async function licensed(env, code, dev) {
  if (!code || !dev) return false;
  const k = code + '|' + dev;
  const e = okCache.get(k);
  if (e && e > Date.now()) return true;
  let r = null;
  try { r = await licCheck(env, code, dev); } catch (x) { r = null; }
  if (r && r.ok) {
    if (okCache.size > 500) okCache.clear();
    okCache.set(k, Date.now() + 4 * 60 * 1000);
    return true;
  }
  return false;
}

export function rewrite(text, base, c, d) {
  const px = (abs) => '/p?u=' + encodeURIComponent(abs) + '&c=' + encodeURIComponent(c) + '&d=' + encodeURIComponent(d);
  return text.split(/\r?\n/).map((line) => {
    if (!line.trim()) return line;
    if (line.startsWith('#')) {
      return line.replace(/URI="([^"]+)"/g, (m, v) => { try { return 'URI="' + px(new URL(v, base).toString()) + '"'; } catch (e) { return m; } });
    }
    try { return px(new URL(line.trim(), base).toString()); } catch (e) { return line; }
  }).join('\n');
}

export async function proxy(req, u, env, fetcher = fetch) {
  if (req.method !== 'GET' && req.method !== 'HEAD') return json({ ok: false, err: 'method' }, 405);
  const c = u.searchParams.get('c') || '', d = u.searchParams.get('d') || '';
  let cur;
  try { cur = new URL(u.searchParams.get('u') || ''); } catch (e) { return json({ ok: false, err: 'bad_url' }, 400); }
  if (!/^https?:$/.test(cur.protocol) || badHost(cur.hostname)) return json({ ok: false, err: 'bad_url' }, 400);
  if (!(await licensed(env, c, d))) return json({ ok: false, err: 'license' }, 403);
  const headers = new Headers({ 'user-agent': 'Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36', accept: '*/*' });
  const range = req.headers.get('range');
  if (range) headers.set('range', range);
  let res = null;
  for (let i = 0; i < 6; i++) {
    try { res = await fetcher(cur.toString(), { method: req.method, headers, redirect: 'manual' }); } catch (e) { return json({ ok: false, err: 'upstream' }, 502); }
    if (res.status >= 300 && res.status < 400 && res.headers.get('location')) {
      try { cur = new URL(res.headers.get('location'), cur); } catch (e) { break; }
      if (!/^https?:$/.test(cur.protocol) || badHost(cur.hostname)) return json({ ok: false, err: 'bad_url' }, 400);
      continue;
    }
    break;
  }
  if (!res) return json({ ok: false, err: 'upstream' }, 502);
  const ct = res.headers.get('content-type') || '';
  if (res.ok && (/mpegurl/i.test(ct) || /\.m3u8?$/i.test(cur.pathname))) {
    const text = await res.text();
    return new Response(req.method === 'HEAD' ? null : rewrite(text, cur, c, d), { status: 200, headers: { 'content-type': 'application/vnd.apple.mpegurl', 'cache-control': 'no-store' } });
  }
  const h = new Headers();
  for (const k of ['content-type', 'content-range', 'accept-ranges', 'last-modified']) { const v = res.headers.get(k); if (v) h.set(k, v); }
  if (!res.headers.get('content-encoding')) { const l = res.headers.get('content-length'); if (l) h.set('content-length', l); }
  h.set('cache-control', /^image\//i.test(ct) ? 'public, max-age=86400' : 'no-store');
  return new Response(req.method === 'HEAD' ? null : res.body, { status: res.status, headers: h });
}

export async function lic(req, env) {
  if (req.method !== 'POST') return json({ ok: false }, 405);
  const t = await req.text();
  if (t.length > 1024) return json({ ok: false }, 413);
  let b = {};
  try { b = JSON.parse(t); } catch (e) { return json({ ok: false, err: 'bad' }, 400); }
  try { return json(await licCheck(env, String(b.code || ''), String(b.device || '').slice(0, 80))); } catch (e) { return json({ ok: false, err: 'net' }, 502); }
}

export default {
  async fetch(req, env) {
    const u = new URL(req.url);
    if (u.pathname === '/p') return proxy(req, u, env);
    if (u.pathname === '/lic') return lic(req, env);
    return env.ASSETS.fetch(req);
  },
};
