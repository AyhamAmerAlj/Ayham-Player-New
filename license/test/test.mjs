import w, { Lic } from '../worker.js';
const mem = new Map();
const storage = { get: async (k) => (mem.has(k) ? structuredClone(mem.get(k)) : undefined), put: async (k, v) => { mem.set(k, structuredClone(v)); }, delete: async (k) => { mem.delete(k); }, list: async ({ prefix }) => new Map([...mem].filter(([k]) => k.startsWith(prefix)).map(([k, v]) => [k, structuredClone(v)])) };
const lic = new Lic({ storage });
const env = { ADMIN_KEY: 'k', LINK_KEY: 'lk', LIC: { idFromName: () => 'x', get: () => ({ fetch: (r) => lic.fetch(r) }) } };
const call = async (path, body, hdr = {}) => (await w.fetch(new Request('http://x' + path, { method: 'POST', headers: hdr, body: JSON.stringify(body) }), env)).json();
const eq = (a, b, m) => { if (JSON.stringify(a) !== JSON.stringify(b)) { console.log('FAIL', m, a, b); process.exit(1); } console.log('ok', m); };
const noAuth = await w.fetch(new Request('http://x/admin/api', { method: 'POST', body: '{}' }), env); eq(noAuth.status, 401, 'admin needs key');
const wrong = await w.fetch(new Request('http://x/admin/api', { method: 'POST', headers: { 'x-admin-key': 'bad' }, body: '{}' }), env); eq(wrong.status, 401, 'wrong key');
const { code } = await call('/admin/api', { action: 'create', note: 'ali' }, { 'x-admin-key': 'k' });
eq(code.length, 14, 'code format');
eq((await call('/v1/check', { code: 'AAAA-BBBB-CCCC', device: 'd1' })).err, 'invalid', 'unknown code');
eq((await call('/v1/check', { code: code.toLowerCase(), device: 'd1' })).ok, true, 'device1 ok');
eq((await call('/v1/check', { code, device: 'd1' })).ok, true, 'device1 heartbeat');
eq((await call('/v1/check', { code, device: 'd2' })).err, 'busy', 'device2 blocked while d1 active');
await call('/v1/release', { code, device: 'd1' });
eq((await call('/v1/check', { code, device: 'd2' })).ok, true, 'device2 ok after release');
await call('/v1/release', { code, device: 'd2' });
eq((await call('/v1/check', { code, device: 'd3' })).err, 'devices_full', 'third device refused');
await call('/admin/api', { action: 'revoke', code }, { 'x-admin-key': 'k' });
eq((await call('/v1/check', { code, device: 'd1' })).err, 'revoked', 'revoked');
await call('/admin/api', { action: 'unrevoke', code }, { 'x-admin-key': 'k' });
await call('/admin/api', { action: 'reset', code }, { 'x-admin-key': 'k' });
eq((await call('/v1/check', { code, device: 'd3' })).ok, true, 'after reset new device ok');
const l = await call('/admin/api', { action: 'list' }, { 'x-admin-key': 'k' }); eq(l.items.length, 1, 'list');
eq((await w.fetch(new Request('http://x/other', { method: 'POST', body: '{}' }), env)).status, 404, 'unknown path');
const big = await w.fetch(new Request('http://x/v1/check', { method: 'POST', body: 'x'.repeat(5000) }), env); eq(big.status, 413, 'oversize body');

// ---- المزامنة ----
const code2 = (await call('/admin/api', { action: 'create', note: 'sync', max: 2 }, { 'x-admin-key': 'k' })).code;
await call('/v1/check', { code: code2, device: 's1' }); await call('/v1/release', { code: code2, device: 's1' });
eq((await call('/v1/sync/get', { code: code2, device: 'nope' })).err, 'invalid', 'sync needs registered device');
eq((await call('/v1/sync/get', { code: code2, device: 's1', profile: 'Ali' })).data, null, 'sync empty');
eq((await call('/v1/sync/put', { code: code2, device: 's1', profile: 'Ali', base: 0, data: { a: 1 } })).ver, 1, 'sync put v1');
eq((await call('/v1/sync/put', { code: code2, device: 's1', profile: 'Ali', base: 0, data: { a: 2 } })).err, 'conflict', 'sync conflict');
eq((await call('/v1/sync/get', { code: code2, device: 's1', profile: 'Ali' })).data, { a: 1 }, 'sync get');
eq((await call('/v1/sync/get', { code: code2, device: 's1', profile: 'Sara' })).data, null, 'profiles separate');
eq((await call('/v1/sync/put', { code: code2, device: 's1', profile: 'Ali', base: 1, data: { x: 'y'.repeat(130000) } })).err, 'too_big', 'sync too big');
await call('/admin/api', { action: 'revoke', code: code2 }, { 'x-admin-key': 'k' });
eq((await call('/v1/sync/get', { code: code2, device: 's1', profile: 'Ali' })).err, 'revoked', 'sync revoked');

// ---- ما الجديد؟ ----
const A = { 'x-admin-key': 'k' };
eq((await call('/v1/news', {})).items, [], 'news empty');
const ns = await call('/admin/api', { action: 'news_save', version: '1.0.130', build: 130, date: '2026-10-31', title: 'T', desc: 'D', items: [{ icon: '🎃', title: 'a', desc: 'b', image: 'http://bad' }, { title: '' }], published: false }, A);
eq(ns.ok, true, 'news saved');
eq((await call('/v1/news', {})).items.length, 0, 'unpublished hidden');
await call('/admin/api', { action: 'news_publish', id: ns.id, published: true }, A);
const pub = (await call('/v1/news', {})).items;
eq([pub.length, pub[0].items.length, pub[0].items[0].image], [1, 1, ''], 'published visible, bad image dropped');
await call('/admin/api', { action: 'news_save', id: ns.id, version: '1.0.130', title: 'T2', published: true }, A);
eq((await call('/v1/news', {})).items[0].title, 'T2', 'news edit');
eq((await call('/admin/api', { action: 'news_save', title: 'x' }, A)).err, 'missing', 'needs version');
await call('/admin/api', { action: 'news_delete', id: ns.id }, A);
eq((await call('/admin/api', { action: 'news_list' }, A)).items.length, 0, 'news deleted');

{
  const wc = (await call('/admin/api', { action: 'create', note: 'web ahmad', max: 3 }, { 'x-admin-key': 'k' })).code;
  eq((await call('/v1/check', { code: wc, device: 'tv' })).web, true, 'web flag returned');
  eq((await call('/v1/check', { code: wc, device: 'wsite' })).ok, true, 'web code: 2nd device at the same time ok');
  eq((await call('/v1/check', { code: wc, device: 'phone' })).ok, true, '3rd device at the same time ok');
  eq((await call('/v1/check', { code: wc, device: 'fourth' })).err, 'devices_full', '4th device blocked');
}

{
  const adm = { 'x-admin-key': 'k' };
  const sc = (await call('/admin/api', { action: 'create', note: 'web zbon', max: 3, srv_url: 'http://s1.example:8080/', srv_user: 'u1', srv_pass: 'p1' }, adm)).code;
  const rawCall = async (path, body, hdr = {}) => (await w.fetch(new Request('http://x' + path, { method: 'POST', headers: hdr, body: JSON.stringify(body) }), env));
  eq((await rawCall('/v1/creds', { code: sc, device: 'wa' })).status, 404, 'creds needs link key');
  eq((await rawCall('/v1/creds', { code: sc, device: 'wa' }, { 'x-link': 'bad' })).status, 404, 'creds wrong link key');
  const cr = await (await rawCall('/v1/creds', { code: sc, device: 'wa' }, { 'x-link': 'lk' })).json();
  eq([cr.ok, cr.web, cr.srv.url, cr.srv.user, cr.srv.pass], [true, true, 'http://s1.example:8080', 'u1', 'p1'], 'creds returns server');
  eq((await call('/v1/check', { code: sc, device: 'wb' })).srv, undefined, 'public check never leaks server');
  eq((await call('/admin/api', { action: 'list' }, adm)).items.find((i) => i.note === 'web zbon').server, 's1.example:8080 / u1', 'list shows host/user only');
  eq(JSON.stringify(await call('/admin/api', { action: 'list' }, adm)).includes('p1'), false, 'list hides password');
  eq((await call('/admin/api', { action: 'create', note: 'x', srv_url: 'ftp://bad' }, adm)).err, 'bad_server', 'bad url rejected');
  eq((await call('/admin/api', { action: 'setserver', code: sc, srv_url: 'http://s2.example:80', srv_user: 'u2', srv_pass: 'p2' }, adm)).ok, true, 'setserver');
  eq((await (await rawCall('/v1/creds', { code: sc, device: 'wa' }, { 'x-link': 'lk' })).json()).srv.user, 'u2', 'server updated');
}
