// BARÇA ZONE: وسيط آمن لـ API-Football (API-Sports). المفتاح سرّ على الخادم فقط، والتطبيق لا يراه أبدًا.
// كل الطلبات من كل المستخدمين تمرّ على كاش مشترك + سقف يومي، فيبقى استهلاك الطلبات ثابتًا مهما كثر المستخدمون.
export const TEAM = 529;   // FC Barcelona
export const LEAGUE = 140; // La Liga
const BASE = 'https://v3.football.api-sports.io/';
const H = 3600e3;
const TTL = { fixtures: 6 * H, standings: 3 * H, squad: 24 * H, players: 24 * H, coach: 7 * 24 * H, liveMatch: 120e3, soonMatch: 10 * 60e3, doneMatch: 7 * 24 * H };
const DONE = new Set(['FT', 'AET', 'PEN', 'CANC', 'PST', 'ABD', 'AWD', 'WO', 'SUSP']);
const PRE = new Set(['NS', 'TBD']);
const DAILY_LIMIT_DEFAULT = 90; // الخطة المجانية 100 باليوم، نترك هامشًا

const season = (env, t) => { const s = parseInt(env.BARCA_SEASON, 10); if (s) return s; const d = new Date(t); return d.getUTCMonth() >= 6 ? d.getUTCFullYear() : d.getUTCFullYear() - 1; };
const dayKey = (t) => 'bz:q:' + new Date(t).toISOString().slice(0, 10);
const pending = new Map();

// ---------- تقليص الردود إلى شكل صغير (حد التخزين 128KB للمفتاح) ----------
const mapFx = (r) => ({
  id: r.fixture.id, ts: r.fixture.timestamp, st: r.fixture.status.short, el: r.fixture.status.elapsed, venue: (r.fixture.venue && r.fixture.venue.name) || '',
  lg: { id: r.league.id, name: r.league.name, logo: r.league.logo, round: r.league.round },
  h: { id: r.teams.home.id, name: r.teams.home.name, logo: r.teams.home.logo }, a: { id: r.teams.away.id, name: r.teams.away.name, logo: r.teams.away.logo },
  gh: r.goals.home, ga: r.goals.away, ht: r.score && r.score.halftime ? [r.score.halftime.home, r.score.halftime.away] : null,
  pen: r.score && r.score.penalty && r.score.penalty.home != null ? [r.score.penalty.home, r.score.penalty.away] : null,
});
const mapMatch = (r) => ({
  ...mapFx(r),
  ev: (r.events || []).map((e) => ({ m: e.time && e.time.elapsed, x: e.time && e.time.extra, tid: e.team && e.team.id, p: e.player && e.player.name, a: e.assist && e.assist.name, ty: e.type, d: e.detail })),
  stt: (r.statistics || []).map((s) => ({ tid: s.team && s.team.id, s: (s.statistics || []).map((x) => ({ t: x.type, v: x.value })) })),
  lu: (r.lineups || []).map((l) => ({
    tid: l.team && l.team.id, form: l.formation, coach: l.coach && l.coach.name,
    xi: (l.startXI || []).map((p) => ({ n: p.player.number, name: p.player.name, pos: p.player.pos, grid: p.player.grid })),
    sub: (l.substitutes || []).map((p) => ({ n: p.player.number, name: p.player.name, pos: p.player.pos })),
  })),
});
const mapStand = (resp) => {
  const rows = (resp[0] && resp[0].league && resp[0].league.standings && resp[0].league.standings[0]) || [];
  return rows.map((x) => ({ rank: x.rank, id: x.team.id, name: x.team.name, logo: x.team.logo, pts: x.points, gd: x.goalsDiff, form: x.form, p: x.all.played, w: x.all.win, d: x.all.draw, l: x.all.lose, gf: x.all.goals.for, ga: x.all.goals.against, desc: x.description }));
};
const mapSquad = (resp) => ((resp[0] && resp[0].players) || []).map((p) => ({ id: p.id, name: p.name, age: p.age, n: p.number, pos: p.position, photo: p.photo }));
const mapPlayers = (resp) => resp.map((x) => {
  const st = (x.statistics || []).filter((s) => s.team && s.team.id === TEAM);
  const sum = (f) => st.reduce((a, s) => a + (f(s) || 0), 0);
  const rs = st.map((s) => parseFloat(s.games && s.games.rating)).filter((v) => !isNaN(v));
  return { id: x.player.id, name: x.player.name, age: x.player.age, nat: x.player.nationality, photo: x.player.photo, pos: st[0] && st[0].games && st[0].games.position,
    apps: sum((s) => s.games && s.games.appearences), min: sum((s) => s.games && s.games.minutes), goals: sum((s) => s.goals && s.goals.total), ast: sum((s) => s.goals && s.goals.assists),
    yc: sum((s) => s.cards && s.cards.yellow), rc: sum((s) => s.cards && s.cards.red), rating: rs.length ? Math.round((rs.reduce((a, b) => a + b, 0) / rs.length) * 100) / 100 : null };
});
const mapCoach = (resp) => {
  const c = resp.find((x) => (x.career || []).some((k) => k.team && k.team.id === TEAM && !k.end)) || resp[0];
  return c ? { id: c.id, name: c.name, age: c.age, nat: c.nationality, photo: c.photo } : null;
};

// ---------- النداء مع الكاش والسقف اليومي ----------
async function upstream(env, path, fetcher, tnow, st) {
  const lim = parseInt(env.BARCA_DAILY_LIMIT, 10) || DAILY_LIMIT_DEFAULT;
  const used = (await st.get(dayKey(tnow))) || 0;
  if (used >= lim) return { err: 'quota' };
  await st.put(dayKey(tnow), used + 1);
  let r;
  try { r = await fetcher(BASE + path, { headers: { 'x-apisports-key': env.APIFOOTBALL_KEY, accept: 'application/json' } }); } catch (e) { return { err: 'upstream' }; }
  if (r.status === 429) return { err: 'quota' };
  if (!r.ok) return { err: 'upstream' };
  let j; try { j = await r.json(); } catch (e) { return { err: 'upstream' }; }
  const errs = j.errors; const has = Array.isArray(errs) ? errs.length : errs && Object.keys(errs).length;
  if (has) {
    const m = JSON.stringify(errs);
    if (/plan|season/i.test(m)) return { err: 'plan', msg: String(Object.values(errs)[0] || '').slice(0, 160) };
    if (/rate|limit|request/i.test(m)) return { err: 'quota' };
    return { err: 'upstream' };
  }
  return { resp: j.response || [] };
}

async function cached(st, env, key, ttlFn, path, map, fetcher, tnow) {
  const rec = await st.get('bz:' + key);
  const ttl = typeof ttlFn === 'function' ? ttlFn(rec) : ttlFn;
  if (rec && tnow - rec.t < ttl) return { ok: true, data: rec.data, t: rec.t, cached: true };
  const k = 'bz:' + key;
  if (pending.has(k)) return pending.get(k);
  const p = (async () => {
    const u = await upstream(env, path, fetcher, tnow, st);
    if (u.err) return rec ? { ok: true, data: rec.data, t: rec.t, stale: true } : { ok: false, err: u.err, msg: u.msg };
    let data; try { data = map(u.resp); } catch (e) { return rec ? { ok: true, data: rec.data, t: rec.t, stale: true } : { ok: false, err: 'upstream' }; }
    if (data === undefined || data === null || (Array.isArray(data) && !data.length && key.startsWith('fixtures'))) return rec ? { ok: true, data: rec.data, t: rec.t, stale: true } : { ok: false, err: 'empty' };
    await st.put(k, { t: tnow, data });
    return { ok: true, data, t: tnow };
  })().finally(() => pending.delete(k));
  pending.set(k, p);
  return p;
}

export async function barca(st, env, body, { fetcher = fetch, now = Date.now } = {}) {
  if (!env.APIFOOTBALL_KEY) return { ok: false, err: 'not_configured' };
  const tnow = now();
  const S = season(env, tnow);
  const ep = String(body.ep || '');
  const fx = () => cached(st, env, 'fixtures:' + S, TTL.fixtures, `fixtures?team=${TEAM}&season=${S}`, (r) => r.map(mapFx).sort((a, b) => a.ts - b.ts), fetcher, tnow);
  if (ep === 'fixtures') { const r = await fx(); return r.ok ? { ...r, season: S } : r; }
  if (ep === 'standings') { const r = await cached(st, env, 'standings:' + S, TTL.standings, `standings?league=${LEAGUE}&season=${S}`, mapStand, fetcher, tnow); return r.ok ? { ...r, season: S } : r; }
  if (ep === 'squad') return cached(st, env, 'squad', TTL.squad, `players/squads?team=${TEAM}`, mapSquad, fetcher, tnow);
  if (ep === 'coach') return cached(st, env, 'coach', TTL.coach, `coachs?team=${TEAM}`, mapCoach, fetcher, tnow);
  if (ep === 'players') {
    const page = Math.min(3, Math.max(1, parseInt(body.page, 10) || 1));
    return cached(st, env, `players:${S}:${page}`, TTL.players, `players?team=${TEAM}&season=${S}&page=${page}`, mapPlayers, fetcher, tnow);
  }
  if (ep === 'match') {
    const id = parseInt(body.id, 10);
    if (!id || id < 1) return { ok: false, err: 'bad_id' };
    const l = await fx(); // نسمح فقط بمباريات برشلونة الموجودة في قائمة الموسم (حماية من استهلاك الطلبات)
    if (!l.ok) return l;
    if (!l.data.some((f) => f.id === id)) return { ok: false, err: 'bad_id' };
    const ttlFn = (rec) => (!rec ? 0 : DONE.has(rec.data.st) ? TTL.doneMatch : PRE.has(rec.data.st) ? TTL.soonMatch : TTL.liveMatch);
    const before = await st.get('bz:match:' + id);
    const r = await cached(st, env, 'match:' + id, ttlFn, `fixtures?id=${id}`, (resp) => (resp[0] ? mapMatch(resp[0]) : null), fetcher, tnow);
    if (r.ok && !r.cached && !r.stale && DONE.has(r.data.st) && (!before || !DONE.has(before.data.st))) {
      const k = 'bz:fixtures:' + S; const rec = await st.get(k); if (rec) await st.put(k, { ...rec, t: 0 }); // انتهت المباراة: حدّث القائمة
    }
    return r;
  }
  return { ok: false, err: 'bad_ep' };
}
