import assert from 'node:assert/strict';
import { badHost, rewrite, handle } from '../worker.js';
let pass = 0; const ok = (n, f) => Promise.resolve().then(f).then(() => { pass++; console.log('ok', n); });
const env = { XTREAM_URL: 'http://iptv.example:8080', XTREAM_USER: 'usr', XTREAM_PASS: 'pw123', ACCESS_PASSWORD: 'correct-horse-battery' };
const seen = [];
const fake = async (url, init) => {
  seen.push(url);
  if (url === 'http://iptv.example:8080/live/usr/pw123/1.m3u8') return new Response(null, { status: 302, headers: { location: 'http://edge.example/tok/1.m3u8' } });
  if (url === 'http://edge.example/tok/1.m3u8') return new Response('#EXTM3U\n#EXTINF:4,\nseg1.ts\n', { headers: { 'content-type': 'application/vnd.apple.mpegurl' } });
  if (url === 'http://edge.example/tok/seg1.ts') return new Response('TS', { headers: { 'content-type': 'video/mp2t' } });
  if (url.startsWith('http://iptv.example:8080/player_api.php?username=usr&password=pw123')) {
    if (url.includes('action=get_vod_streams')) return new Response('[{"name":"A"}]', { headers: { 'content-type': 'application/json' } });
    return new Response('{"user_info":{"auth":1,"status":"Active","password":"SECRET","username":"usr","exp_date":"1","max_connections":"1"}}', { headers: { 'content-type': 'application/json' } });
  }
  if (url === 'http://img.example/p.jpg') return new Response('IMG', { headers: { 'content-type': 'image/jpeg' } });
  if (url === 'http://img.example/x.html') return new Response('<html>', { headers: { 'content-type': 'text/html' } });
  return new Response('nf', { status: 404 });
};
const R = (path, init) => handle(new Request('https://w' + path, init), env, fake);
async function login() { const r = await R('/login', { method: 'POST', body: JSON.stringify({ password: env.ACCESS_PASSWORD }) }); assert.equal(r.status, 200); return decodeURIComponent(r.headers.get('set-cookie').split(';')[0]).replace(/^ap_s=/, ''); }

await ok('static passthrough', async () => { assert.equal(await R('/index.html'), null); });
await ok('not configured', async () => { const r = await handle(new Request('https://w/x'), {}, fake); assert.equal(r.status, 503); });
await ok('wrong password', async () => { const r = await R('/login', { method: 'POST', body: JSON.stringify({ password: 'nope' }) }); assert.equal(r.status, 401); });
await ok('auth required', async () => { for (const p of ['/x?action=get_vod_streams', '/s/live/1.m3u8', '/i?u=http://img.example/p.jpg', '/me']) assert.equal((await R(p)).status, 401, p); });
await ok('login sets httponly cookie', async () => { const r = await R('/login', { method: 'POST', body: JSON.stringify({ password: env.ACCESS_PASSWORD }) }); const c = r.headers.get('set-cookie'); assert.match(c, /HttpOnly/); assert.match(c, /Secure/); });
await ok('x adds creds server-side', async () => { const t = await login(); const r = await R('/x?action=get_vod_streams&category_id=7', { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }); assert.equal(r.status, 200); assert.ok(seen.some((u) => u.includes('password=pw123') && u.includes('category_id=7'))); });
await ok('x login test hides password', async () => { const t = await login(); const r = await R('/x', { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }); const txt = await r.text(); assert.ok(!txt.includes('SECRET')); assert.ok(!txt.includes('pw123')); assert.equal(JSON.parse(txt).ok, true); });
await ok('live m3u8 redirect + signed rewrite', async () => { const t = await login(); const r = await R('/s/live/1.m3u8', { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }); const txt = await r.text(); assert.match(txt, /\/p\?u=http%3A%2F%2Fedge\.example%2Ftok%2Fseg1\.ts&s=[0-9a-f]{64}/); assert.ok(!txt.includes('pw123')); const seg = txt.split('\n').find((l) => l.startsWith('/p?')); const r2 = await R(seg); assert.equal(await r2.text(), 'TS'); });
await ok('forged signature rejected', async () => { const r = await R('/p?u=' + encodeURIComponent('http://edge.example/tok/seg1.ts') + '&s=' + '0'.repeat(64)); assert.equal(r.status, 403); const r2 = await R('/p?u=' + encodeURIComponent('http://edge.example/x')); assert.equal(r2.status, 403); });
await ok('image proxy only images', async () => { const t = await login(); const h = { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }; assert.equal((await R('/i?u=' + encodeURIComponent('http://img.example/p.jpg'), h)).status, 200); assert.equal((await R('/i?u=' + encodeURIComponent('http://img.example/x.html'), h)).status, 403); assert.equal((await R('/i?u=' + encodeURIComponent('http://127.0.0.1/x.png'), h)).status, 400); });
await ok('bad action / stream path', async () => { const t = await login(); const h = { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }; assert.equal((await R('/x?action=../etc', h)).status, 400); assert.equal((await R('/s/live/a..b.m3u8', h)).status, 400); });
await ok('expired/forged cookie rejected', async () => { assert.equal((await R('/me', { headers: { cookie: 'ap_s=' + (Date.now() + 1e9) + '.abc' } })).status, 401); assert.equal((await R('/me', { headers: { cookie: 'ap_s=1.abc' } })).status, 401); });
await ok('code-style secret ignores dashes/case', async () => { const e2 = { ...env, ACCESS_PASSWORD: 'ABCD-EFGH-JKLM' }; const r = await handle(new Request('https://w/login', { method: 'POST', body: JSON.stringify({ password: 'abcdefghjklm' }) }), e2, fake); assert.equal(r.status, 200); });
await ok('badHost', () => { for (const h of ['localhost', '127.0.0.1', '10.0.0.1', '192.168.0.1', '[::1]']) assert.ok(badHost(h)); });
console.log(pass + ' tests passed');

// ---- دخول بكود الأدمن (LICENSE) ----
{
  const store = { 'AAAAAAAAAAAA': { web: true }, 'BBBBBBBBBBBB': { web: false }, 'CCCCCCCCCCCC': { err: 'revoked' }, 'DDDDDDDDDDDD': { err: 'busy' } };
  let calls = 0;
  const LICENSE = { fetch: async (rq) => { calls++; const b = JSON.parse(await rq.text()); const r = store[b.code]; return new Response(JSON.stringify(r ? (r.err ? { ok: false, err: r.err } : { ok: true, web: r.web }) : { ok: false, err: 'invalid' })); } };
  const e2 = { XTREAM_URL: env.XTREAM_URL, XTREAM_USER: env.XTREAM_USER, XTREAM_PASS: env.XTREAM_PASS, LICENSE }; // بلا كلمة مالك
  const L = (code, dev = 'dev1') => handle(new Request('https://w/login', { method: 'POST', body: JSON.stringify({ password: code, device: dev }) }), e2, fake);
  const tokOf = (r) => decodeURIComponent(r.headers.get('set-cookie').split(';')[0]).replace(/^ap_s=/, '');
  await ok('license code with web flag logs in', async () => { const r = await L('aaaa-aaaa-aaaa'); assert.equal(r.status, 200); assert.match(r.headers.get('set-cookie'), /HttpOnly/); });
  await ok('valid code without web flag is rejected', async () => { const r = await L('BBBB-BBBB-BBBB'); assert.equal(r.status, 403); assert.equal((await r.json()).err, 'not_web'); assert.equal(r.headers.get('set-cookie'), null); });
  await ok('revoked / busy / unknown map to errors', async () => { assert.equal((await (await L('CCCC-CCCC-CCCC')).json()).err, 'revoked'); assert.equal((await (await L('DDDD-DDDD-DDDD')).json()).err, 'busy'); assert.equal((await L('ZZZZ-ZZZZ-ZZZZ')).status, 401); assert.equal((await L('short')).status, 401); });
  await ok('session works for /x and license check is cached', async () => {
    const t = tokOf(await L('AAAA-AAAA-AAAA', 'devZ'));
    const Rx = () => handle(new Request('https://w/x', { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }), e2, fake);
    assert.equal((await Rx()).status, 200);
    const before = calls; await Rx(); assert.equal(calls, before, 'cached within 4 min');
  });
  await ok('forged / tampered token rejected', async () => {
    const t = tokOf(await L('AAAA-AAAA-AAAA', 'devY')); const bad = t.replace(/\.[0-9a-f]{64}$/, '.' + '0'.repeat(64));
    assert.equal((await handle(new Request('https://w/x', { headers: { cookie: 'ap_s=' + encodeURIComponent(bad) } }), e2, fake)).status, 401);
    assert.equal((await handle(new Request('https://w/x', { headers: { cookie: 'ap_s=' + encodeURIComponent('9999999999999.O.-.' + '0'.repeat(64)) } }), e2, fake)).status, 401);
  });
  await ok('owner password still works when set', async () => { assert.equal((await R('/login', { method: 'POST', body: JSON.stringify({ password: env.ACCESS_PASSWORD }) })).status, 200); });
}
console.log('tests passed (with license):', pass);
