'use strict';
/**
 * Ayham Player Web: خادم واحد بلا مكتبات: يقدّم الواجهة + وسيط (proxy) لسيرفر Xtream.
 * بيانات الاشتراك تبقى على الخادم فقط (متغيرات بيئة)، والمتصفح ما يشوفها أبدًا.
 * كل شي محمي بكلمة سر (كوكي موقّع). الـ proxy ما يفتح إلا روابط ظهرت داخل قوائم السيرفر نفسه (موقّعة).
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const zlib = require('zlib');
const dns = require('dns').promises;
const net = require('net');

const PORT = parseInt(process.env.PORT || '3000', 10);
const PASSWORD = process.env.ACCESS_PASSWORD || '';
const XT = (process.env.XTREAM_URL || '').replace(/\/+$/, '');
const XU = process.env.XTREAM_USER || '';
const XP = process.env.XTREAM_PASS || '';
if (!PASSWORD || !XT || !XU || !XP) {
  console.error('Missing env: ACCESS_PASSWORD, XTREAM_URL, XTREAM_USER, XTREAM_PASS');
  process.exit(1);
}
if (PASSWORD.length < 8) console.warn('WARNING: ACCESS_PASSWORD is short; use at least 12 characters.');
const SECRET = process.env.SESSION_SECRET || crypto.createHash('sha256').update('ayham|' + PASSWORD + '|' + XU + '|' + XP).digest('hex');
const PUB = path.join(__dirname, 'public');
const SESSION_DAYS = 30;

// ------------------------------------------------------------------ أدوات
const b64u = (s) => Buffer.from(s).toString('base64url');
const unb64u = (s) => Buffer.from(s, 'base64url').toString();
const hmac = (s) => crypto.createHmac('sha256', SECRET).update(s).digest('base64url').slice(0, 32);
const safeEq = (a, b) => {
  const x = crypto.createHash('sha256').update(String(a)).digest();
  const y = crypto.createHash('sha256').update(String(b)).digest();
  return crypto.timingSafeEqual(x, y);
};
const agents = { 'http:': new http.Agent({ keepAlive: true, maxSockets: 64 }), 'https:': new https.Agent({ keepAlive: true, maxSockets: 64 }) };
const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.json': 'application/json', '.png': 'image/png', '.ttf': 'font/ttf', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8', '.webmanifest': 'application/manifest+json',
};

function send(res, code, body, headers = {}) {
  res.writeHead(code, Object.assign({ 'Cache-Control': 'no-store' }, headers));
  res.end(body);
}
function json(res, code, obj) { send(res, code, JSON.stringify(obj), { 'Content-Type': 'application/json; charset=utf-8' }); }
function clientIp(req) { return ((req.headers['x-forwarded-for'] || '').split(',')[0] || req.socket.remoteAddress || '').trim(); }
const isHttps = (req) => (req.headers['x-forwarded-proto'] || '').split(',')[0] === 'https';

// ------------------------------------------------------------------ الجلسة
function makeSession() {
  const exp = Date.now() + SESSION_DAYS * 864e5;
  return exp + '.' + hmac('s' + exp);
}
function getCookie(req, name) {
  const c = req.headers.cookie || '';
  for (const part of c.split(';')) {
    const i = part.indexOf('=');
    if (i > 0 && part.slice(0, i).trim() === name) return part.slice(i + 1).trim();
  }
  return '';
}
function authed(req) {
  const v = getCookie(req, 'ap_session');
  const i = v.indexOf('.');
  if (i < 1) return false;
  const exp = v.slice(0, i);
  return Number(exp) > Date.now() && safeEq(v.slice(i + 1), hmac('s' + exp));
}
const attempts = new Map();
function tooMany(ip) {
  const now = Date.now();
  const a = attempts.get(ip);
  if (!a || a.reset < now) return false;
  return a.n >= 6;
}
function failed(ip) {
  const now = Date.now();
  const a = attempts.get(ip);
  if (!a || a.reset < now) attempts.set(ip, { n: 1, reset: now + 10 * 60 * 1000 });
  else a.n++;
}
setInterval(() => { const n = Date.now(); for (const [k, v] of attempts) if (v.reset < n) attempts.delete(k); }, 60000).unref();

function readBody(req, limit = 4096) {
  return new Promise((resolve, reject) => {
    let n = 0; const chunks = [];
    req.on('data', (c) => { n += c.length; if (n > limit) { reject(new Error('too large')); req.destroy(); } else chunks.push(c); });
    req.on('end', () => resolve(Buffer.concat(chunks).toString()));
    req.on('error', reject);
  });
}

// ------------------------------------------------------------------ الاتصال بالسيرفر الأصلي
function fetchUp(urlStr, headers = {}, redirects = 5) {
  return new Promise((resolve, reject) => {
    let u;
    try { u = new URL(urlStr); } catch (e) { return reject(new Error('bad url')); }
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return reject(new Error('bad protocol'));
    const lib = u.protocol === 'https:' ? https : http;
    const req = lib.request(u, {
      method: 'GET', agent: agents[u.protocol], timeout: 20000,
      headers: Object.assign({ 'User-Agent': 'AyhamPlayer/1.0', Accept: '*/*' }, headers),
    }, (r) => {
      if (r.statusCode >= 300 && r.statusCode < 400 && r.headers.location && redirects > 0) {
        r.resume();
        let next;
        try { next = new URL(r.headers.location, u).toString(); } catch (e) { return reject(new Error('bad redirect')); }
        return resolve(fetchUp(next, headers, redirects - 1));
      }
      resolve({ res: r, url: u.toString(), req });
    });
    req.on('timeout', () => req.destroy(new Error('timeout')));
    req.on('error', reject);
    req.end();
  });
}
function collect(stream, limit) {
  return new Promise((resolve, reject) => {
    let n = 0; const chunks = [];
    stream.on('data', (c) => { n += c.length; if (n > limit) { stream.destroy(); reject(new Error('too large')); } else chunks.push(c); });
    stream.on('end', () => resolve(Buffer.concat(chunks)));
    stream.on('error', reject);
  });
}

// ------------------------------------------------------------------ API (Xtream)
const ACTIONS = new Set([
  'get_live_categories', 'get_live_streams', 'get_vod_categories', 'get_vod_streams',
  'get_series_categories', 'get_series', 'get_vod_info', 'get_series_info', 'get_short_epg',
]);
const cache = new Map(); // key -> {ts, ttl, gz}
function ttlFor(action) {
  if (action === 'get_short_epg') return 2 * 60e3;
  if (action.endsWith('_info')) return 30 * 60e3;
  return 10 * 60e3;
}
function xtUrl(params) {
  const q = new URLSearchParams({ username: XU, password: XP });
  for (const [k, v] of Object.entries(params)) q.set(k, v);
  return XT + '/player_api.php?' + q.toString();
}
async function apiXt(req, res, url) {
  const action = url.searchParams.get('action') || '';
  const params = {};
  if (action === 'account') {
    // معلومات الحساب بدون اسم المستخدم/كلمة المرور/الروابط
    try {
      const { res: r } = await fetchUp(xtUrl({}));
      const data = JSON.parse((await collect(r, 2e6)).toString());
      const ui = data.user_info || {};
      return json(res, 200, { status: ui.status, exp_date: ui.exp_date, max_connections: ui.max_connections, active_cons: ui.active_cons });
    } catch (e) { return json(res, 502, { error: 'upstream' }); }
  }
  if (!ACTIONS.has(action)) return json(res, 400, { error: 'bad action' });
  params.action = action;
  for (const k of ['category_id', 'vod_id', 'series_id', 'stream_id', 'limit']) {
    const v = url.searchParams.get(k);
    if (v !== null) { if (!/^[\w-]{1,20}$/.test(v)) return json(res, 400, { error: 'bad param' }); params[k] = v; }
  }
  const key = JSON.stringify(params);
  const hit = cache.get(key);
  let gz;
  if (hit && Date.now() - hit.ts < hit.ttl) gz = hit.gz;
  else {
    try {
      const { res: r } = await fetchUp(xtUrl(params), { 'Accept-Encoding': 'identity' });
      if (r.statusCode !== 200) { r.resume(); return json(res, 502, { error: 'upstream ' + r.statusCode }); }
      const buf = await collect(r, 200e6);
      JSON.parse(buf.toString()); // تحقق إنه JSON سليم قبل التخزين
      gz = zlib.gzipSync(buf, { level: 6 });
      cache.set(key, { ts: Date.now(), ttl: ttlFor(action), gz });
      if (cache.size > 60) cache.delete(cache.keys().next().value);
    } catch (e) { return json(res, 502, { error: 'upstream failed' }); }
  }
  const accepts = (req.headers['accept-encoding'] || '').includes('gzip');
  const h = { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'private, max-age=60', Vary: 'Accept-Encoding' };
  if (accepts) { h['Content-Encoding'] = 'gzip'; return send(res, 200, gz, h); }
  return send(res, 200, zlib.gunzipSync(gz), h);
}

// ------------------------------------------------------------------ البث
// الرابط الأصلي يُشفَّر (AES-GCM): ما يظهر للمتصفح، ولا يمكن تزويره أو تغييره
const KEY = crypto.createHash('sha256').update('k|' + SECRET).digest();
function seal(text) {
  const iv = crypto.randomBytes(12);
  const c = crypto.createCipheriv('aes-256-gcm', KEY, iv);
  const enc = Buffer.concat([c.update(text, 'utf8'), c.final()]);
  return Buffer.concat([iv, c.getAuthTag(), enc]).toString('base64url');
}
function unseal(tok) {
  const b = Buffer.from(tok, 'base64url');
  if (b.length < 29) throw new Error('bad token');
  const d = crypto.createDecipheriv('aes-256-gcm', KEY, b.subarray(0, 12));
  d.setAuthTag(b.subarray(12, 28));
  return Buffer.concat([d.update(b.subarray(28)), d.final()]).toString('utf8');
}
function signedLink(abs) { return '/s/p?t=' + seal(abs); }
function rewritePlaylist(text, base) {
  const abs = (u) => { try { return new URL(u, base).toString(); } catch (e) { return null; } };
  return text.split(/\r?\n/).map((line) => {
    if (!line) return line;
    if (line.startsWith('#')) {
      return line.replace(/URI="([^"]+)"/g, (m, u) => { const a = abs(u); return a ? 'URI="' + signedLink(a) + '"' : m; });
    }
    const a = abs(line.trim());
    return a ? signedLink(a) : line;
  }).join('\n');
}
function isPlaylist(url, r) {
  const ct = (r.headers['content-type'] || '').toLowerCase();
  return ct.includes('mpegurl') || ct.includes('m3u8') || /\.m3u8(\?|$)/i.test(url);
}
const PASS_HEADERS = ['content-type', 'content-length', 'content-range', 'accept-ranges', 'last-modified', 'etag'];
async function proxyStream(req, res, upstreamUrl, forcePlaylist) {
  let up;
  try {
    const h = {};
    if (req.headers.range) h.Range = req.headers.range;
    up = await fetchUp(upstreamUrl, h);
  } catch (e) { return send(res, 502, 'upstream unreachable'); }
  const { res: r, url: finalUrl, req: upReq } = up;
  // نقطع الاتصال الأصلي فورًا لو المتصفح سكّر (مهم لحد اتصالات الاشتراك)
  const abort = () => { try { upReq.destroy(); r.destroy(); } catch (e) { /* ignore */ } };
  res.on('close', abort);
  if (r.statusCode >= 400) { r.resume(); return send(res, r.statusCode === 404 ? 404 : 502, 'upstream ' + r.statusCode); }
  if (forcePlaylist || isPlaylist(finalUrl, r)) {
    try {
      const body = (await collect(r, 5e6)).toString('utf8');
      if (!body.trimStart().startsWith('#EXTM3U') && forcePlaylist) return send(res, 502, 'not a playlist');
      return send(res, 200, rewritePlaylist(body, finalUrl), { 'Content-Type': 'application/vnd.apple.mpegurl', 'Cache-Control': 'no-store' });
    } catch (e) { return send(res, 502, 'playlist failed'); }
  }
  const h = { 'Cache-Control': 'private, max-age=300' };
  for (const k of PASS_HEADERS) if (r.headers[k]) h[k] = r.headers[k];
  res.writeHead(r.statusCode, h);
  r.pipe(res);
}

// ------------------------------------------------------------------ الصور (مع حماية SSRF)
function privateIp(ip) {
  if (net.isIPv4(ip)) {
    const [a, b] = ip.split('.').map(Number);
    return a === 10 || a === 127 || a === 0 || (a === 169 && b === 254) || (a === 172 && b >= 16 && b <= 31) || (a === 192 && b === 168) || (a === 100 && b >= 64 && b <= 127);
  }
  const l = ip.toLowerCase();
  return l === '::1' || l.startsWith('fc') || l.startsWith('fd') || l.startsWith('fe80') || l.startsWith('::ffff:');
}
async function proxyImage(req, res, url) {
  const u = url.searchParams.get('u') || '';
  let target;
  try { target = new URL(u); } catch (e) { return send(res, 400, 'bad url'); }
  if (!/^https?:$/.test(target.protocol)) return send(res, 400, 'bad url');
  try {
    // الصور من سيرفر الاشتراك نفسه مسموحة دائمًا، وغيرها فقط لو العنوان عام (مو داخلي)
    const sameHost = new URL(XT).host === target.host;
    if (!sameHost) {
      const addrs = await dns.lookup(target.hostname, { all: true });
      if (!addrs.length || addrs.some((a) => privateIp(a.address))) return send(res, 403, 'blocked');
    }
    const { res: r, req: upReq } = await fetchUp(target.toString());
    const abort = () => { try { upReq.destroy(); r.destroy(); } catch (e) { /* ignore */ } };
    res.on('close', abort);
    const ct = (r.headers['content-type'] || '').toLowerCase();
    if (r.statusCode !== 200 || !ct.startsWith('image/')) { r.resume(); return send(res, 404, 'not an image'); }
    const buf = await collect(r, 4e6);
    return send(res, 200, buf, { 'Content-Type': ct, 'Cache-Control': 'private, max-age=604800' });
  } catch (e) { return send(res, 502, 'image failed'); }
}

// ------------------------------------------------------------------ الملفات الثابتة
function serveStatic(req, res, pathname) {
  let p = pathname === '/' ? '/index.html' : pathname;
  let file;
  try { file = path.normalize(path.join(PUB, decodeURIComponent(p))); } catch (e) { return send(res, 400, 'bad'); }
  if (!file.startsWith(PUB + path.sep) && file !== PUB) return send(res, 403, 'forbidden');
  fs.stat(file, (err, st) => {
    if (err || !st.isFile()) return send(res, 404, 'not found');
    const ext = path.extname(file).toLowerCase();
    const long = ext === '.ttf' || ext === '.png';
    res.writeHead(200, {
      'Content-Type': MIME[ext] || 'application/octet-stream',
      'Content-Length': st.size,
      'Cache-Control': long ? 'public, max-age=604800' : 'no-cache',
    });
    if (req.method === 'HEAD') return res.end();
    fs.createReadStream(file).pipe(res);
  });
}

// ------------------------------------------------------------------ الموجّه
const SEC = {
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
  'X-Robots-Tag': 'noindex, nofollow',
  'X-Frame-Options': 'DENY',
  'Content-Security-Policy': "default-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; font-src 'self'; worker-src 'self' blob:; frame-ancestors 'none'",
};

const server = http.createServer(async (req, res) => {
  for (const [k, v] of Object.entries(SEC)) res.setHeader(k, v);
  let url;
  try { url = new URL(req.url, 'http://x'); } catch (e) { return send(res, 400, 'bad'); }
  const p = url.pathname;
  try {
    if (p === '/healthz') return send(res, 200, 'ok');
    if (p === '/robots.txt') return send(res, 200, 'User-agent: *\nDisallow: /\n', { 'Content-Type': 'text/plain' });

    if (p === '/api/login' && req.method === 'POST') {
      const ip = clientIp(req);
      if (tooMany(ip)) return json(res, 429, { error: 'too many attempts, try later' });
      let pw = '';
      try { pw = JSON.parse(await readBody(req)).password || ''; } catch (e) { return json(res, 400, { error: 'bad request' }); }
      if (!safeEq(pw, PASSWORD)) { failed(ip); return json(res, 401, { error: 'wrong password' }); }
      const cookie = 'ap_session=' + makeSession() + '; HttpOnly; SameSite=Strict; Path=/; Max-Age=' + SESSION_DAYS * 86400 + (isHttps(req) ? '; Secure' : '');
      return send(res, 200, '{"ok":true}', { 'Content-Type': 'application/json', 'Set-Cookie': cookie });
    }
    if (p === '/api/logout' && req.method === 'POST') {
      return send(res, 200, '{"ok":true}', { 'Content-Type': 'application/json', 'Set-Cookie': 'ap_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0' });
    }
    if (p === '/api/me') return authed(req) ? json(res, 200, { ok: true }) : json(res, 401, { ok: false });

    if (p.startsWith('/api/') || p.startsWith('/s/') || p === '/i') {
      if (!authed(req)) return json(res, 401, { error: 'login required' });
      if (req.method !== 'GET' && req.method !== 'HEAD') return json(res, 405, { error: 'method' });
      if (p === '/api/xt') return apiXt(req, res, url);
      if (p === '/i') return proxyImage(req, res, url);
      let m;
      if ((m = p.match(/^\/s\/live\/(\d{1,12})(?:\.m3u8)?$/))) return proxyStream(req, res, `${XT}/live/${encodeURIComponent(XU)}/${encodeURIComponent(XP)}/${m[1]}.m3u8`, true);
      if ((m = p.match(/^\/s\/(movie|series)\/(\d{1,12})\.([a-z0-9]{2,5})$/))) {
        return proxyStream(req, res, `${XT}/${m[1]}/${encodeURIComponent(XU)}/${encodeURIComponent(XP)}/${m[2]}.${m[3]}`, m[3] === 'm3u8');
      }
      if (p === '/s/p') {
        let abs;
        try { abs = unseal(url.searchParams.get('t') || ''); } catch (e) { return send(res, 403, 'bad token'); }
        return proxyStream(req, res, abs, false);
      }
      return json(res, 404, { error: 'not found' });
    }
    if (req.method !== 'GET' && req.method !== 'HEAD') return send(res, 405, 'method');
    return serveStatic(req, res, p);
  } catch (e) {
    console.error('request error:', e && e.message);
    if (!res.headersSent) send(res, 500, 'server error'); else res.destroy();
  }
});
server.keepAliveTimeout = 65000;
server.listen(PORT, () => console.log('Ayham Player Web listening on :' + PORT));
