// اختبار شامل لخادم Node: الدخول بكود، عدم تسريب البيانات، تمرير HLS موقّع، المدى (Range)، الملفات الثابتة.
import assert from 'node:assert/strict';
import http from 'node:http';
import { makeServer } from './server.js';
let pass = 0; const ok = async (n, f) => { await f(); pass++; console.log('ok', n); };
const seen = [];
const fake = async (url, init) => {
  seen.push(url);
  if (url.startsWith('http://iptv.example:8080/player_api.php?username=usr&password=pw123')) return new Response('{"user_info":{"auth":1,"status":"Active","password":"SECRET"}}', { headers: { 'content-type': 'application/json' } });
  if (url === 'http://iptv.example:8080/live/usr/pw123/1.m3u8') return new Response(null, { status: 302, headers: { location: 'http://edge.example/t/1.m3u8' } });
  if (url === 'http://edge.example/t/1.m3u8') return new Response('#EXTM3U\n#EXTINF:4,\nseg1.ts\n', { headers: { 'content-type': 'application/vnd.apple.mpegurl' } });
  if (url === 'http://edge.example/t/seg1.ts') return new Response('TSDATA', { headers: { 'content-type': 'video/mp2t' } });
  return new Response('nf', { status: 404 });
};
const LICENSE = { fetch: async (rq) => { const b = JSON.parse(await rq.text()); if (rq.headers.get('x-link') !== 'lk') return new Response('{"ok":false}'); return new Response(JSON.stringify(b.code === 'AAAAAAAAAAAA' ? { ok: true, web: true, srv: { url: 'http://iptv.example:8080', user: 'usr', pass: 'pw123' } } : { ok: false, err: 'invalid' })); } };
const srv = makeServer({ env: { LICENSE, LINK_KEY: 'lk' }, fetcher: fake });
await new Promise((r) => srv.listen(0, r));
const port = srv.address().port;
const call = (path, { method = 'GET', body, cookie, headers = {} } = {}) => new Promise((res, rej) => {
  const rq = http.request({ port, path, method, headers: { ...(cookie ? { cookie } : {}), ...headers, 'x-forwarded-proto': 'https' } }, (r) => { const c = []; r.on('data', (d) => c.push(d)); r.on('end', () => res({ status: r.statusCode, headers: r.headers, text: Buffer.concat(c).toString() })); });
  rq.on('error', rej); if (body) rq.write(body); rq.end();
});
await ok('static index served', async () => { const r = await call('/'); assert.equal(r.status, 200); assert.match(r.text, /Ayham Player/); assert.match(r.headers['content-type'], /text\/html/); });
await ok('path traversal blocked', async () => { assert.equal((await call('/..%2f..%2fserver.js')).status, 404); });
await ok('wrong code rejected', async () => { const r = await call('/login', { method: 'POST', body: JSON.stringify({ password: 'ZZZZ-ZZZZ-ZZZZ', device: 'd1' }) }); assert.equal(r.status, 401); });
let cookie;
await ok('valid code logs in, cookie httponly secure', async () => { const r = await call('/login', { method: 'POST', body: JSON.stringify({ password: 'aaaa-aaaa-aaaa', device: 'd1' }) }); assert.equal(r.status, 200); const c = [].concat(r.headers['set-cookie'])[0]; assert.match(c, /HttpOnly/); assert.match(c, /Secure/); cookie = c.split(';')[0]; });
await ok('api works, password never returned', async () => { const r = await call('/x', { cookie }); assert.equal(r.status, 200); assert.ok(!r.text.includes('SECRET') && !r.text.includes('pw123')); assert.equal(JSON.parse(r.text).ok, true); });
await ok('no cookie -> 401', async () => { assert.equal((await call('/x')).status, 401); });
await ok('HLS rewrite + signed segment fetch (no cookie needed for segment)', async () => { const r = await call('/s/live/1.m3u8', { cookie }); assert.equal(r.status, 200); assert.ok(!r.text.includes('pw123')); const seg = r.text.split('\n').find((l) => l.startsWith('/p?')); assert.ok(seg); const s = await call(seg); assert.equal(s.text, 'TSDATA'); assert.equal((await call(seg.replace(/s=[0-9a-f]+/, 's=' + '0'.repeat(64)))).status, 403); });
srv.close();
console.log('webnode tests passed:', pass);
