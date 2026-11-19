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

console.log('tests passed:', pass);

await ok('403 retries with other user-agents; persistent 403 -> 502 with status', async () => {
  const uas = []; const f = async (u, init) => { uas.push(init.headers.get('user-agent')); return uas.length < 3 ? new Response('no', { status: 403 }) : new Response('{"user_info":{"auth":1}}', { headers: { 'content-type': 'application/json' } }); };
  const t = await login(); const r = await handle(new Request('https://w/x', { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }), env, f);
  assert.equal(r.status, 200); assert.equal(uas[0], 'AyhamPlayer/1.0'); assert.match(uas[2], /okhttp/);
  const f2 = async () => new Response('no', { status: 403 });
  const r2 = await handle(new Request('https://w/x', { headers: { cookie: 'ap_s=' + encodeURIComponent(t) } }), env, f2);
  assert.equal(r2.status, 502); assert.equal((await r2.json()).status, 403);
});

// ---- بيانات الاشتراك تُدخل من الموقع وتُحفظ مشفّرة ----
{
  const e3 = { ACCESS_PASSWORD: 'correct-horse-battery' }; // بلا أسرار سيرفر
  const R3 = (path, init) => handle(new Request('https://w' + path, init), e3, fake);
  const sealedCookie = (r) => (r.headers.getSetCookie ? r.headers.getSetCookie() : [r.headers.get('set-cookie')]).find((c) => c && c.startsWith('ap_x='));
  let sess = '';
  await ok('no creds yet -> /me says setup, /x says no_server', async () => {
    const lr = await R3('/login', { method: 'POST', body: JSON.stringify({ password: e3.ACCESS_PASSWORD }) });
    sess = decodeURIComponent(lr.headers.get('set-cookie').split(';')[0]).replace(/^ap_s=/, '');
    const me = await (await R3('/me', { headers: { cookie: 'ap_s=' + encodeURIComponent(sess) } })).json();
    assert.equal(me.setup, true);
    assert.equal((await R3('/x?action=get_vod_streams', { headers: { cookie: 'ap_s=' + encodeURIComponent(sess) } })).status, 409);
  });
  await ok('setup requires a valid session', async () => {
    const r = await R3('/setup', { method: 'POST', body: JSON.stringify({ url: 'http://iptv.example:8080', user: 'usr', pass: 'pw123' }) });
    assert.equal(r.status, 401);
  });
  await ok('setup rejects private hosts and wrong credentials', async () => {
    const h = { cookie: 'ap_s=' + encodeURIComponent(sess), 'content-type': 'application/json' };
    assert.equal((await R3('/setup', { method: 'POST', headers: h, body: JSON.stringify({ url: 'http://127.0.0.1:8080', user: 'usr', pass: 'pw123' }) })).status, 400);
    assert.equal((await R3('/setup', { method: 'POST', headers: h, body: JSON.stringify({ url: 'ftp://iptv.example', user: 'usr', pass: 'pw123' }) })).status, 400);
    assert.ok((await R3('/setup', { method: 'POST', headers: h, body: JSON.stringify({ url: 'http://iptv.example:8080', user: 'bad', pass: 'nope' }) })).status >= 400);
  });
  await ok('good setup stores encrypted creds; /x then uses them; creds not in body', async () => {
    const h = { cookie: 'ap_s=' + encodeURIComponent(sess), 'content-type': 'application/json' };
    const r = await R3('/setup', { method: 'POST', headers: h, body: JSON.stringify({ url: 'http://iptv.example:8080', user: 'usr', pass: 'pw123' }) });
    assert.equal(r.status, 200);
    const sc = sealedCookie(r); assert.ok(sc && /HttpOnly/.test(sc) && /Secure/.test(sc));
    const body = await r.text(); assert.ok(!body.includes('pw123'));
    assert.ok(!sc.includes('pw123') && !sc.includes('usr'));
    const xc = sc.split(';')[0];
    const x = await R3('/x?action=get_vod_streams', { headers: { cookie: 'ap_s=' + encodeURIComponent(sess) + '; ' + xc } });
    assert.equal(x.status, 200);
    const me = await (await R3('/me', { headers: { cookie: 'ap_s=' + encodeURIComponent(sess) + '; ' + xc } })).json();
    assert.equal(me.setup, false);
  });
  await ok('link returns direct url only for owner and valid input', async () => {
    const h = { cookie: 'ap_s=' + encodeURIComponent(sess) };
    const lr = await R3('/setup', { method: 'POST', headers: { ...h, 'content-type': 'application/json' }, body: JSON.stringify({ url: 'http://iptv.example:8080', user: 'usr', pass: 'pw123' }) });
    const xc = sealedCookie(lr).split(';')[0];
    const ok1 = await (await R3('/link?kind=movie&id=55&ext=mkv', { headers: { cookie: h.cookie + '; ' + xc } })).json();
    assert.equal(ok1.url, 'http://iptv.example:8080/movie/usr/pw123/55.mkv');
    assert.equal((await R3('/link?kind=live&id=55&ext=mkv', { headers: { cookie: h.cookie + '; ' + xc } })).status, 400);
    assert.equal((await R3('/link?kind=movie&id=55&ext=mkv')).status, 401);
  });
  await ok('tampered creds cookie is rejected', async () => {
    const r = await R3('/x?action=get_vod_streams', { headers: { cookie: 'ap_s=' + encodeURIComponent(sess) + '; ap_x=' + '00'.repeat(12) + '.' + 'ab'.repeat(20) } });
    assert.equal(r.status, 409);
  });
}
console.log('setup tests done');
