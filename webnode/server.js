// Ayham Web على Node (للاستضافة خارج Cloudflare، مثل Render): نفس منطق webcf/worker.js بالضبط.
// المتغيرات: LICENSE_URL (رابط سيرفر الترخيص)، LINK_KEY (مفتاح الربط من لوحة الأدمن)، PORT (تلقائي).
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { Readable } from 'node:stream';
import { fileURLToPath } from 'node:url';
import { handle } from '../webcf/worker.js';

const PUB = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../webcf/public');
const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.webmanifest': 'application/manifest+json', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.ttf': 'font/ttf', '.woff2': 'font/woff2', '.txt': 'text/plain; charset=utf-8' };

// يحوّل نداء LICENSE الداخلي في الكود إلى طلب HTTPS لسيرفر الترخيص.
const licenseShim = (url) => ({ fetch: async (rq) => fetch(String(url).replace(/\/+$/, '') + new URL(rq.url).pathname, { method: 'POST', headers: rq.headers, body: await rq.text() }) });

export function makeServer({ env = process.env, fetcher = fetch } = {}) {
  const E = { ...env, LICENSE: env.LICENSE || (env.LICENSE_URL ? licenseShim(env.LICENSE_URL) : undefined) };
  return http.createServer(async (nreq, nres) => {
    try {
      const url = new URL(nreq.url, 'http://' + (nreq.headers.host || 'localhost'));
      const hdrs = new Headers();
      for (const [k, v] of Object.entries(nreq.headers)) if (v != null && !['host', 'connection', 'content-length', 'transfer-encoding'].includes(k)) hdrs.set(k, Array.isArray(v) ? v.join(', ') : v);
      const body = ['GET', 'HEAD'].includes(nreq.method) ? undefined : await new Promise((ok) => { const b = []; nreq.on('data', (c) => b.push(c)); nreq.on('end', () => ok(Buffer.concat(b).slice(0, 8192))); });
      const proto = (nreq.headers['x-forwarded-proto'] || 'https').split(',')[0];
      const wreq = new Request(proto + '://' + (nreq.headers.host || 'localhost') + url.pathname + url.search, { method: nreq.method, headers: hdrs, body });
      let resp = await handle(wreq, E, fetcher);
      if (!resp) { // ملف ثابت
        let p = decodeURIComponent(url.pathname); if (p.endsWith('/')) p += 'index.html';
        const f = path.resolve(PUB, '.' + p);
        if (!f.startsWith(PUB) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) { nres.writeHead(404, { 'content-type': 'text/plain' }); return nres.end('not found'); }
        const ext = path.extname(f).toLowerCase();
        nres.writeHead(200, { 'content-type': MIME[ext] || 'application/octet-stream', 'cache-control': ext === '.html' || p.endsWith('sw.js') ? 'no-cache' : 'public, max-age=86400' });
        return nreq.method === 'HEAD' ? nres.end() : fs.createReadStream(f).pipe(nres);
      }
      const h = {}; resp.headers.forEach((v, k) => { h[k] = v; });
      const sc = resp.headers.getSetCookie ? resp.headers.getSetCookie() : null; if (sc && sc.length) h['set-cookie'] = sc;
      nres.writeHead(resp.status, h);
      if (!resp.body || nreq.method === 'HEAD') return nres.end();
      const rs = Readable.fromWeb(resp.body);
      nres.on('close', () => rs.destroy());
      rs.on('error', () => nres.destroy());
      rs.pipe(nres);
    } catch (e) { if (!nres.headersSent) nres.writeHead(500, { 'content-type': 'application/json' }); nres.end('{"ok":false,"err":"server"}'); }
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const port = +process.env.PORT || 8080;
  makeServer().listen(port, () => console.log('Ayham Web listening on', port));
}
