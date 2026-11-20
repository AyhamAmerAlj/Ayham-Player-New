// Ayham Web على Deno Deploy (مجاني بدون بطاقة): نفس منطق webcf/worker.js.
// المتغيرات المطلوبة: ACCESS_PASSWORD (كلمة مرورك) و SESSION_SECRET (أي نص عشوائي طويل).
import { handle } from '../webcf/worker.js';

const MIME = { html: 'text/html; charset=utf-8', js: 'text/javascript; charset=utf-8', css: 'text/css; charset=utf-8', json: 'application/json', webmanifest: 'application/manifest+json', png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', svg: 'image/svg+xml', ico: 'image/x-icon', ttf: 'font/ttf', woff2: 'font/woff2', txt: 'text/plain; charset=utf-8' };
const PUB = new URL('../webcf/public/', import.meta.url);

Deno.serve(async (req) => {
  try {
    const env = Deno.env.toObject();
    const r = await handle(req, env, fetch);
    if (r) return r;
    let p = decodeURIComponent(new URL(req.url).pathname);
    if (p.endsWith('/')) p += 'index.html';
    if (p.includes('..')) return new Response('not found', { status: 404 });
    const ext = p.split('.').pop().toLowerCase();
    const data = await Deno.readFile(new URL('.' + p, PUB));
    return new Response(data, { headers: { 'content-type': MIME[ext] || 'application/octet-stream', 'cache-control': ext === 'html' || p.endsWith('sw.js') ? 'no-cache' : 'public, max-age=86400' } });
  } catch (e) {
    return new Response(e && e.name === 'NotFound' ? 'not found' : '{"ok":false,"err":"server"}', { status: e && e.name === 'NotFound' ? 404 : 500 });
  }
});
