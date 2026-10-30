// خادم التراخيص لـ Ayham Player (Cloudflare Worker + Durable Object بتخزين SQLite).
// كل كود: يسمح بجهازين كحد أقصى، وجهاز واحد فقط يعمل بنفس الوقت.
// نستخدم Durable Object واحدًا لأن عملياته متسلسلة (لا تتضارب) وحدّه المجاني للكتابة كافٍ.
const ACTIVE_MS = 10 * 60 * 1000; // الجلسة تُعتبر حيّة 10 دقائق بعد آخر نبضة
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

const json = (o, s = 200) => new Response(JSON.stringify(o), { status: s, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' } });
const now = () => Date.now();
const normCode = (c) => String(c || '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 12);
const fmtCode = (c) => c.replace(/(.{4})(?=.)/g, '$1-');
const getRec = async (st, c) => (await st.get('c:' + c)) || null;
const putRec = (st, c, r) => st.put('c:' + c, r);

function genCode() {
  const b = new Uint8Array(12); crypto.getRandomValues(b);
  return Array.from(b, (x) => ALPHABET[x % ALPHABET.length]).join('');
}
function safeEq(a, b) {
  a = String(a || ''); b = String(b || '');
  let d = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) d |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return d === 0;
}

export async function check(st, body) {
  const code = normCode(body.code);
  const dev = String(body.device || '').slice(0, 80);
  if (code.length !== 12 || !dev) return { ok: false, err: 'invalid' };
  const r = await getRec(st, code);
  if (!r) return { ok: false, err: 'invalid' };
  if (r.revoked) return { ok: false, err: 'revoked' };
  const t = now();
  r.devices = r.devices || {};
  if (!r.devices[dev]) {
    if (Object.keys(r.devices).length >= (r.max || 2)) return { ok: false, err: 'devices_full' };
    r.devices[dev] = { name: String(body.name || '').slice(0, 40), first: t, last: t };
  }
  if (r.active && r.active.d !== dev && t - r.active.t < ACTIVE_MS) return { ok: false, err: 'busy' };
  r.active = { d: dev, t };
  r.devices[dev].last = t;
  if (body.name) r.devices[dev].name = String(body.name).slice(0, 40);
  await putRec(st, code, r);
  return { ok: true };
}

export async function release(st, body) {
  const code = normCode(body.code), dev = String(body.device || '');
  const r = await getRec(st, code);
  if (r && r.active && r.active.d === dev) { r.active = null; await putRec(st, code, r); }
  return { ok: true };
}

// ---- مزامنة بيانات المشاهدة بين أجهزة نفس الكود (لكل ملف شخصي مساحة مستقلة) ----
const SYNC_MAX = 120 * 1024; // حد حجم البيانات لكل ملف شخصي
const SYNC_PROFILES = 5;
async function syncAuth(st, body) {
  const code = normCode(body.code);
  const dev = String(body.device || '').slice(0, 80);
  if (code.length !== 12 || !dev) return { err: 'invalid' };
  const r = await getRec(st, code);
  if (!r) return { err: 'invalid' };
  if (r.revoked) return { err: 'revoked' };
  if (!r.devices || !r.devices[dev]) return { err: 'invalid' };
  const prof = String(body.profile || 'main').toLowerCase().replace(/[^a-z0-9\u0600-\u06ff_-]/g, '').slice(0, 32) || 'main';
  return { code, key: 'd:' + code + ':' + prof };
}
export async function syncGet(st, body) {
  const a = await syncAuth(st, body);
  if (a.err) return { ok: false, err: a.err };
  const rec = (await st.get(a.key)) || null;
  return { ok: true, ver: rec ? rec.ver : 0, data: rec ? rec.data : null };
}
export async function syncPut(st, body) {
  const a = await syncAuth(st, body);
  if (a.err) return { ok: false, err: a.err };
  if (!body.data || typeof body.data !== 'object') return { ok: false, err: 'bad' };
  const raw = JSON.stringify(body.data);
  if (raw.length > SYNC_MAX) return { ok: false, err: 'too_big' };
  const cur = (await st.get(a.key)) || null;
  if (!cur) {
    const l = await st.list({ prefix: 'd:' + a.code + ':' });
    if (l.size >= SYNC_PROFILES) return { ok: false, err: 'too_many_profiles' };
  }
  if ((cur ? cur.ver : 0) !== (+body.base || 0)) return { ok: false, err: 'conflict' };
  const ver = (cur ? cur.ver : 0) + 1;
  await st.put(a.key, { ver, data: body.data, t: now() });
  return { ok: true, ver };
}

async function admin(st, body) {
  if (body.action === 'create') {
    const code = genCode();
    await putRec(st, code, { note: String(body.note || '').slice(0, 80), max: Math.min(5, Math.max(1, +body.max || 2)), created: now(), revoked: false, devices: {}, active: null });
    return { ok: true, code: fmtCode(code) };
  }
  if (body.action === 'list') {
    const out = [];
    const l = await st.list({ prefix: 'c:' });
    for (const [k, r] of l) out.push({ code: fmtCode(k.slice(2)), note: r.note, revoked: r.revoked, max: r.max, created: r.created, devices: Object.values(r.devices || {}).map((d) => ({ name: d.name, last: d.last })), active: !!(r.active && now() - r.active.t < ACTIVE_MS) });
    out.sort((a, b) => b.created - a.created);
    return { ok: true, items: out };
  }
  const code = normCode(body.code);
  const r = await getRec(st, code);
  if (!r) return { ok: false, err: 'not_found' };
  if (body.action === 'revoke') r.revoked = true;
  else if (body.action === 'unrevoke') r.revoked = false;
  else if (body.action === 'reset') { r.devices = {}; r.active = null; }
  else if (body.action === 'delete') {
    await st.delete('c:' + code);
    const l = await st.list({ prefix: 'd:' + code + ':' });
    for (const k of l.keys()) await st.delete(k);
    return { ok: true };
  }
  else return { ok: false, err: 'bad_action' };
  await putRec(st, code, r);
  return { ok: true };
}

// الكائن الدائم: كل العمليات تمر عليه بالتسلسل.
export class Lic {
  constructor(state) { this.st = state.storage; }
  async fetch(req) {
    const u = new URL(req.url);
    const body = await req.json();
    if (u.pathname === '/admin/api') return json(await admin(this.st, body));
    if (u.pathname === '/v1/check') return json(await check(this.st, body));
    if (u.pathname === '/v1/release') return json(await release(this.st, body));
    if (u.pathname === '/v1/sync/get') return json(await syncGet(this.st, body));
    if (u.pathname === '/v1/sync/put') return json(await syncPut(this.st, body));
    return json({ ok: false }, 404);
  }
}

const PAGE = `<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>إدارة الأكواد</title>
<style>body{font-family:system-ui,sans-serif;background:#07040f;color:#f1ecff;margin:0;padding:16px;max-width:760px;margin-inline:auto}input,button{font:inherit;padding:10px 12px;border-radius:10px;border:1px solid #3b2a6b;background:#140b26;color:#fff}button{background:#7c3aed;cursor:pointer}button.s{background:#2a1a4d;padding:6px 10px;font-size:13px}.r{display:flex;gap:8px;flex-wrap:wrap;margin:10px 0}.c{background:#140b26;border:1px solid #2a1a4d;border-radius:14px;padding:12px;margin:10px 0}code{direction:ltr;display:inline-block;font-size:18px;letter-spacing:1px}.x{color:#f87171}.g{color:#4ade80}</style>
<h2>إدارة أكواد Ayham Player</h2><div class="r"><input id="k" type="password" placeholder="مفتاح الإدارة"><button onclick="L()">دخول</button></div>
<div class="r"><input id="n" placeholder="ملاحظة (اسم الزبون)"><select id="m" style="padding:10px;border-radius:10px;border:1px solid #3b2a6b;background:#140b26;color:#fff"><option value="1">Basic (جهاز)</option><option value="2" selected>Pro (جهازان)</option><option value="3">Elite (3 أجهزة)</option></select><button onclick="C()">إنشاء كود جديد</button></div><div id="o"></div>
<script>
const $=s=>document.querySelector(s);const E=s=>String(s==null?'':s).replace(/[&<>"']/g,c=>'&#'+c.charCodeAt(0)+';');let K=sessionStorage.k||'';if(K)$('#k').value=K;
async function A(b){const r=await fetch('/admin/api',{method:'POST',headers:{'content-type':'application/json','x-admin-key':$('#k').value},body:JSON.stringify(b)});if(r.status==401){$('#o').textContent='مفتاح خاطئ';throw 0}return r.json()}
async function L(){sessionStorage.k=$('#k').value;const d=await A({action:'list'});$('#o').innerHTML=d.items.map(i=>'<div class="c"><code>'+i.code+'</code> '+(i.revoked?'<b class="x">ملغي</b>':i.active?'<b class="g">يعمل الآن</b>':'')+'<div>'+E(i.note)+' · الأجهزة: '+i.devices.length+'/'+i.max+'</div><div style="font-size:13px;color:#9a8fb8">'+i.devices.map(d=>E(d.name)||'جهاز').join(' ، ')+'</div><div class="r"><button class="s" onclick="X(\\'revoke\\',\\''+i.code+'\\')">إلغاء</button><button class="s" onclick="X(\\'unrevoke\\',\\''+i.code+'\\')">تفعيل</button><button class="s" onclick="X(\\'reset\\',\\''+i.code+'\\')">تصفير الأجهزة</button><button class="s" onclick="X(\\'delete\\',\\''+i.code+'\\')">حذف</button></div></div>').join('')||'لا توجد أكواد'}
async function C(){const d=await A({action:'create',note:$('#n').value,max:+$('#m').value});alert('الكود: '+d.code);$('#n').value='';L()}
async function X(a,c){if(a=='delete'&&!confirm('حذف نهائي؟'))return;await A({action:a,code:c});L()}
if(K)L();
</script></html>`;

export default {
  async fetch(req, env) {
    const u = new URL(req.url);
    try {
      if (u.pathname === '/admin') return new Response(PAGE, { headers: { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store' } });
      if (req.method !== 'POST') return json({ ok: true });
      if (u.pathname === '/admin/api') {
        if (!env.ADMIN_KEY || !safeEq(req.headers.get('x-admin-key'), env.ADMIN_KEY)) return json({ ok: false }, 401);
      } else if (u.pathname !== '/v1/check' && u.pathname !== '/v1/release' && u.pathname !== '/v1/sync/get' && u.pathname !== '/v1/sync/put') return json({ ok: false }, 404);
      const text = await req.text();
      if (text.length > (u.pathname === '/v1/sync/put' ? 160 * 1024 : 4096)) return json({ ok: false }, 413);
      const stub = env.LIC.get(env.LIC.idFromName('main'));
      return await stub.fetch(new Request(req.url, { method: 'POST', body: text }));
    } catch (e) { return json({ ok: false, err: 'server' }, 500); }
  },
};
