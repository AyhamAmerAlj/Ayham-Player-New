import assert from 'node:assert/strict';
import w, { Lic } from '../worker.js';
import { barca } from '../barca.js';
let pass = 0; const ok = async (n, f) => { await f(); pass++; console.log('ok', n); };
const mkStore = () => { const m = new Map(); return { get: async (k) => (m.has(k) ? structuredClone(m.get(k)) : undefined), put: async (k, v) => { m.set(k, structuredClone(v)); }, delete: async (k) => { m.delete(k); }, list: async ({ prefix }) => new Map([...m].filter(([k]) => k.startsWith(prefix))), _m: m }; };
const KEY = 'SECRETKEY123';
const H = 3600e3;
const fxRow = (id, ts, st, gh, ga) => ({ fixture: { id, timestamp: ts, status: { short: st, elapsed: st === 'FT' ? 90 : null }, venue: { name: 'Camp Nou' } }, league: { id: 140, name: 'La Liga', logo: 'l.png', round: 'Round 1' }, teams: { home: { id: 529, name: 'Barcelona', logo: 'b.png' }, away: { id: 541, name: 'Real Madrid', logo: 'r.png' } }, goals: { home: gh, away: ga }, score: { halftime: { home: 1, away: 0 }, penalty: { home: null, away: null } } });
const NOW = Date.UTC(2026, 10, 1, 12, 0, 0);
let calls = []; let live = false;
const fetcher = async (url, init) => {
  calls.push({ url, key: init.headers['x-apisports-key'] });
  const R = (response, errors = []) => new Response(JSON.stringify({ errors, response }), { headers: { 'content-type': 'application/json' } });
  if (url.includes('fixtures?team=529&season=2026')) return R([fxRow(2, NOW / 1000 + 86400 * 5, 'NS', null, null), fxRow(1, NOW / 1000 - 86400 * 7, 'FT', 2, 1)]);
  if (url.includes('fixtures?id=2')) return R([{ ...fxRow(2, NOW / 1000, live ? '2H' : 'FT', 2, 1), events: [{ time: { elapsed: 10, extra: null }, team: { id: 529 }, player: { name: 'Lamine Yamal' }, assist: { name: 'Pedri' }, type: 'Goal', detail: 'Normal Goal' }, { time: { elapsed: 33, extra: 2 }, team: { id: 541 }, player: { name: 'X' }, assist: { name: null }, type: 'Card', detail: 'Yellow Card' }], statistics: [{ team: { id: 529 }, statistics: [{ type: 'Ball Possession', value: '61%' }] }], lineups: [{ team: { id: 529 }, formation: '4-3-3', coach: { name: 'Flick' }, startXI: [{ player: { number: 1, name: 'Ter Stegen', pos: 'G', grid: '1:1' } }], substitutes: [{ player: { number: 13, name: 'Peña', pos: 'G' } }] }] }]);
  if (url.includes('standings?league=140&season=2026')) return R([{ league: { standings: [[{ rank: 1, team: { id: 529, name: 'Barcelona', logo: 'b.png' }, points: 30, goalsDiff: 20, form: 'WWWDW', all: { played: 11, win: 9, draw: 3, lose: 0, goals: { for: 30, against: 10 } }, description: 'Champions League' }]] } }]);
  if (url.includes('players/squads?team=529')) return R([{ players: [{ id: 1, name: 'Ter Stegen', age: 33, number: 1, position: 'Goalkeeper', photo: 'p.png' }] }]);
  if (url.includes('coachs?team=529')) return R([{ id: 9, name: 'H. Flick', age: 61, nationality: 'Germany', photo: 'c.png', career: [{ team: { id: 529 }, end: null }] }]);
  if (url.includes('players?team=529&season=2026&page=1')) return R([{ player: { id: 5, name: 'Pedri', age: 23, nationality: 'Spain', photo: 'pe.png' }, statistics: [{ team: { id: 529 }, games: { appearences: 10, minutes: 800, position: 'Midfielder', rating: '7.5' }, goals: { total: 2, assists: 3 }, cards: { yellow: 1, red: 0 } }, { team: { id: 529 }, games: { appearences: 3, minutes: 200, rating: '7.0' }, goals: { total: 0, assists: 1 }, cards: { yellow: 0, red: 0 } }] }]);
  if (url.includes('season=2030')) return R([], { plan: 'Free plans do not have access to this season, try from 2022 to 2024.' });
  return new Response('nf', { status: 404 });
};
const env = { APIFOOTBALL_KEY: KEY };
const B = (b, e = env, st) => barca(st, e, b, { fetcher, now: () => NOW });
let st = mkStore();
await ok('not configured without key', async () => { assert.equal((await B({ ep: 'fixtures' }, {}, st)).err, 'not_configured'); });
await ok('fixtures compact + sorted, key sent only upstream', async () => {
  const r = await B({ ep: 'fixtures' }, env, st); assert.equal(r.ok, true); assert.equal(r.season, 2026);
  assert.deepEqual(r.data.map((f) => f.id), [1, 2]); assert.equal(r.data[0].h.name, 'Barcelona'); assert.equal(calls[0].key, KEY);
  assert.ok(!JSON.stringify(r).includes(KEY));
});
await ok('second call served from cache (no upstream)', async () => { const n = calls.length; const r = await B({ ep: 'fixtures' }, env, st); assert.equal(r.cached, true); assert.equal(calls.length, n); });
await ok('standings / squad / coach / players mapped', async () => {
  const s = await B({ ep: 'standings' }, env, st); assert.equal(s.data[0].pts, 30); assert.equal(s.data[0].gf, 30);
  assert.equal((await B({ ep: 'squad' }, env, st)).data[0].name, 'Ter Stegen');
  assert.equal((await B({ ep: 'coach' }, env, st)).data.name, 'H. Flick');
  const p = (await B({ ep: 'players', page: 1 }, env, st)).data[0]; assert.equal(p.apps, 13); assert.equal(p.goals, 2); assert.equal(p.ast, 4); assert.equal(p.rating, 7.25);
});
await ok('match detail mapped (events, stats, lineups)', async () => {
  const r = await B({ ep: 'match', id: 2 }, env, st); assert.equal(r.ok, true);
  assert.equal(r.data.ev[0].p, 'Lamine Yamal'); assert.equal(r.data.ev[0].a, 'Pedri'); assert.equal(r.data.stt[0].s[0].v, '61%'); assert.equal(r.data.lu[0].form, '4-3-3'); assert.equal(r.data.lu[0].xi[0].name, 'Ter Stegen');
  assert.equal((await st.get('bz:fixtures:2026')).t, 0, 'finished match invalidates the fixtures list');
});
await ok('only Barcelona fixtures ids allowed (quota protection)', async () => { const r = await B({ ep: 'match', id: 999 }, env, st); assert.equal(r.err, 'bad_id'); assert.ok(!calls.some((c) => c.url.includes('id=999'))); });
await ok('live match TTL 120s, finished 7d', async () => {
  const st2 = mkStore(); live = true; calls = [];
  await B({ ep: 'match', id: 2 }, env, st2); const n = calls.length;
  await barca(st2, env, { ep: 'match', id: 2 }, { fetcher, now: () => NOW + 60e3 }); assert.equal(calls.length, n, 'within 120s cached');
  await barca(st2, env, { ep: 'match', id: 2 }, { fetcher, now: () => NOW + 130e3 }); assert.equal(calls.length, n + 1, 'after 120s refetched');
  live = false;
});
await ok('daily limit enforced, stale served when available', async () => {
  const st3 = mkStore(); const e3 = { ...env, BARCA_DAILY_LIMIT: '2' }; calls = [];
  await barca(st3, e3, { ep: 'fixtures' }, { fetcher, now: () => NOW }); await barca(st3, e3, { ep: 'standings' }, { fetcher, now: () => NOW });
  assert.equal(calls.length, 2);
  const r = await barca(st3, e3, { ep: 'squad' }, { fetcher, now: () => NOW }); assert.equal(r.err, 'quota'); assert.equal(calls.length, 2);
  const stale = await barca(st3, e3, { ep: 'fixtures' }, { fetcher, now: () => NOW + 7 * H }); assert.equal(stale.stale, true); assert.equal(stale.ok, true);
});
await ok('plan error surfaced (no fake data)', async () => { const st4 = mkStore(); const r = await barca(st4, { ...env, BARCA_SEASON: '2030' }, { ep: 'fixtures' }, { fetcher, now: () => NOW }); assert.equal(r.ok, false); assert.equal(r.err, 'plan'); assert.match(r.msg, /Free plans/); });
await ok('upstream failure without cache -> error, not fake data', async () => { const st5 = mkStore(); const bad = async () => new Response('x', { status: 500 }); const r = await barca(st5, env, { ep: 'fixtures' }, { fetcher: bad, now: () => NOW }); assert.equal(r.ok, false); assert.equal(r.err, 'upstream'); });
await ok('bad endpoint', async () => { assert.equal((await B({ ep: 'x' }, env, mkStore())).err, 'bad_ep'); });
await ok('requires valid license code through the worker', async () => {
  const mem = new Map(); const storage = { get: async (k) => (mem.has(k) ? structuredClone(mem.get(k)) : undefined), put: async (k, v) => { mem.set(k, structuredClone(v)); }, delete: async (k) => { mem.delete(k); }, list: async ({ prefix }) => new Map([...mem].filter(([k]) => k.startsWith(prefix))) };
  const lic = new Lic({ storage }, { APIFOOTBALL_KEY: KEY });
  const e = { ADMIN_KEY: 'k', LIC: { idFromName: () => 'x', get: () => ({ fetch: (r) => lic.fetch(r) }) } };
  const call = async (p, b, h = {}) => (await w.fetch(new Request('http://x' + p, { method: 'POST', headers: h, body: JSON.stringify(b) }), e)).json();
  assert.equal((await call('/v1/barca', { ep: 'fixtures', code: 'AAAA-BBBB-CCCC', device: 'd' })).ok, false);
  const { code } = await call('/admin/api', { action: 'create', note: 't' }, { 'x-admin-key': 'k' });
  assert.equal((await call('/v1/barca', { ep: 'fixtures', code, device: 'd' })).ok, false, 'unregistered device refused');
  await call('/v1/check', { code, device: 'd' });
  const r = await call('/v1/barca', { ep: 'fixtures', code, device: 'd' }); assert.ok(r.ok === true || r.err); // الشبكة الحقيقية غير متاحة هنا: المهم أن الدخول مسموح
  assert.notEqual(r.err, 'invalid');
  assert.ok(!JSON.stringify(r).includes(KEY));
});
console.log('barca tests passed:', pass);
