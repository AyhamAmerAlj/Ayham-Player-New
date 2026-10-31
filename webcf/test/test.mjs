import assert from 'node:assert/strict';
import { badHost, rewrite, proxy, lic } from '../worker.js';
let pass = 0; const ok = (n, f) => Promise.resolve().then(f).then(() => { pass++; console.log('ok', n); });

const env = { LICENSE: { fetch: async (req) => { const b = JSON.parse(await req.text()); return new Response(JSON.stringify(b.code === 'GOOD' ? { ok: true } : { ok: false, err: 'invalid' })); } } };
const m3u = '#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI="key.bin"\n#EXTINF:4,\nseg1.ts\n#EXTINF:4,\nhttp://cdn.example:81/seg2.ts\n';
const fake = async (url, init) => {
  if (url === 'http://iptv.example:8080/live/u/p/1.m3u8') return new Response(null, { status: 302, headers: { location: 'http://edge.example/tok/1.m3u8' } });
  if (url === 'http://edge.example/tok/1.m3u8') return new Response(m3u, { headers: { 'content-type': 'application/vnd.apple.mpegurl' } });
  if (url === 'http://iptv.example:8080/player_api.php?username=u') return new Response('{"user_info":{"auth":1}}', { headers: { 'content-type': 'application/json' } });
  if (url === 'http://iptv.example/p.jpg') return new Response('IMG', { headers: { 'content-type': 'image/jpeg', 'content-length': '3' } });
  return new Response('nf', { status: 404 });
};
const P = (target, extra = '') => ['https://w/p?u=' + encodeURIComponent(target) + '&c=GOOD&d=dev1' + extra];
const call = (target, init) => { const [u] = P(target); return proxy(new Request(u, init), new URL(u), env, fake); };

await ok('badHost', () => { for (const h of ['localhost', '127.0.0.1', '10.1.2.3', '192.168.1.1', '172.20.0.1', '169.254.1.1', '[::1]', 'a.local']) assert.ok(badHost(h), h); assert.ok(!badHost('iptv.example')); assert.ok(!badHost('8.8.8.8')); });
await ok('rewrite', () => { const r = rewrite(m3u, new URL('http://edge.example/tok/1.m3u8'), 'C', 'D'); assert.ok(r.includes('/p?u=' + encodeURIComponent('http://edge.example/tok/seg1.ts') + '&c=C&d=D')); assert.ok(r.includes(encodeURIComponent('http://cdn.example:81/seg2.ts'))); assert.ok(r.includes('URI="/p?u=' + encodeURIComponent('http://edge.example/tok/key.bin'))); assert.ok(r.includes('#EXTINF:4,')); });
await ok('redirect + m3u8 rewrite', async () => { const r = await call('http://iptv.example:8080/live/u/p/1.m3u8'); assert.equal(r.status, 200); assert.match(r.headers.get('content-type'), /mpegurl/); const t = await r.text(); assert.ok(t.includes(encodeURIComponent('http://edge.example/tok/seg1.ts'))); });
await ok('json passthrough', async () => { const r = await call('http://iptv.example:8080/player_api.php?username=u'); assert.equal((await r.json()).user_info.auth, 1); assert.equal(r.headers.get('cache-control'), 'no-store'); });
await ok('image cached', async () => { const r = await call('http://iptv.example/p.jpg'); assert.match(r.headers.get('cache-control'), /max-age/); assert.equal(await r.text(), 'IMG'); });
await ok('upstream 404 passes', async () => { const r = await call('http://iptv.example/none'); assert.equal(r.status, 404); });
await ok('license denied', async () => { const u = 'https://w/p?u=' + encodeURIComponent('http://iptv.example/p.jpg') + '&c=BAD&d=x'; const r = await proxy(new Request(u), new URL(u), env, fake); assert.equal(r.status, 403); });
await ok('private host blocked', async () => { const r = await call('http://127.0.0.1/x'); assert.equal(r.status, 400); const r2 = await call('file:///etc/passwd'); assert.equal(r2.status, 400); });
await ok('post blocked on proxy', async () => { const r = await call('http://iptv.example/p.jpg', { method: 'POST', body: 'x' }); assert.equal(r.status, 405); });
await ok('lic route', async () => { const r = await lic(new Request('https://w/lic', { method: 'POST', body: JSON.stringify({ code: 'GOOD', device: 'd' }) }), env); assert.equal((await r.json()).ok, true); const r2 = await lic(new Request('https://w/lic', { method: 'POST', body: 'x'.repeat(2000) }), env); assert.equal(r2.status, 413); });
console.log(pass + ' tests passed');
