// سيرفر Xtream تجريبي للاختبار المحلي فقط (لا يُنشر)
const http = require('http'), fs = require('fs'), path = require('path');
const M = path.join(__dirname, 'media');
const PORT = 9100;
const live = [{ num: 1, name: 'قناة تجريبية 1', stream_id: 1, stream_icon: '', category_id: '1' }, { num: 2, name: 'Test Channel 2 (redirect)', stream_id: 2, stream_icon: '', category_id: '1' }];
const vod = [{ num: 1, name: 'فيلم تجريبي', stream_id: 5, stream_icon: `http://127.0.0.1:${PORT}/poster.png`, rating: '7.1', added: '1700000000', category_id: '2', container_extension: 'mp4' }];
const series = [{ num: 1, name: 'مسلسل تجريبي', series_id: 7, cover: '', rating: '8', last_modified: '1700000100', category_id: '3' }];
const srv = http.createServer((req, res) => {
  const u = new URL(req.url, 'http://x');
  const p = u.pathname;
  const J = (o) => { res.writeHead(200, { 'Content-Type': 'application/json' }); res.end(JSON.stringify(o)); };
  if (p === '/player_api.php') {
    if (u.searchParams.get('username') !== 'user1' || u.searchParams.get('password') !== 'pass1') { res.writeHead(401); return res.end('{}'); }
    switch (u.searchParams.get('action')) {
      case null: return J({ user_info: { username: 'user1', password: 'pass1', status: 'Active', exp_date: '1900000000', max_connections: '2', active_cons: '0' }, server_info: { url: 'x' } });
      case 'get_live_categories': return J([{ category_id: '1', category_name: 'عام' }]);
      case 'get_live_streams': return J(live);
      case 'get_vod_categories': return J([{ category_id: '2', category_name: 'أكشن' }]);
      case 'get_vod_streams': return J(vod);
      case 'get_series_categories': return J([{ category_id: '3', category_name: 'دراما' }]);
      case 'get_series': return J(series);
      case 'get_vod_info': return J({ info: { plot: 'وصف تجريبي', genre: 'Action', releasedate: '2020-01-01', duration: '00:00:10', rating: '7.1' }, movie_data: { stream_id: 5, container_extension: 'mp4' } });
      case 'get_series_info': return J({ info: { plot: 'مسلسل', genre: 'Drama' }, episodes: { '1': [{ id: '11', episode_num: 1, title: 'الحلقة 1', container_extension: 'mp4', info: { duration: '00:00:10' } }] } });
      case 'get_short_epg': return J({ epg_listings: [] });
    }
    return J([]);
  }
  if (p === '/poster.png') { res.writeHead(200, { 'Content-Type': 'image/png' }); return fs.createReadStream(path.join(__dirname, '..', 'public', 'icon.png')).pipe(res); }
  let m;
  if ((m = p.match(/^\/live\/user1\/pass1\/(\d+)\.m3u8$/))) {
    if (m[1] === '2') { res.writeHead(302, { Location: `http://127.0.0.1:${PORT}/alt/live.m3u8` }); return res.end(); }
    res.writeHead(200, { 'Content-Type': 'application/vnd.apple.mpegurl' });
    return res.end(fs.readFileSync(path.join(M, 'live.m3u8')));
  }
  if (p === '/alt/live.m3u8') {
    const t = fs.readFileSync(path.join(M, 'live.m3u8'), 'utf8').replace(/^seg/gm, `http://127.0.0.1:${PORT}/seg/seg`);
    res.writeHead(200, { 'Content-Type': 'application/x-mpegURL' }); return res.end(t);
  }
  if ((m = p.match(/^\/seg\/(seg\d+\.ts)$/)) || (m = p.match(/^\/live\/user1\/pass1\/(seg\d+\.ts)$/))) {
    const f = path.join(M, m[1]); if (!fs.existsSync(f)) { res.writeHead(404); return res.end(); }
    res.writeHead(200, { 'Content-Type': 'video/mp2t' }); return fs.createReadStream(f).pipe(res);
  }
  if ((m = p.match(/^\/(movie|series)\/user1\/pass1\/\d+\.mp4$/))) {
    const f = path.join(M, 'movie.mp4'); const size = fs.statSync(f).size; const r = req.headers.range;
    if (r) { const [s, e] = r.replace('bytes=', '').split('-'); const a = +s, b = e ? +e : size - 1; res.writeHead(206, { 'Content-Type': 'video/mp4', 'Content-Range': `bytes ${a}-${b}/${size}`, 'Content-Length': b - a + 1, 'Accept-Ranges': 'bytes' }); return fs.createReadStream(f, { start: a, end: b }).pipe(res); }
    res.writeHead(200, { 'Content-Type': 'video/mp4', 'Content-Length': size, 'Accept-Ranges': 'bytes' }); return fs.createReadStream(f).pipe(res);
  }
  res.writeHead(404); res.end();
});
srv.listen(PORT, () => console.log('mock xtream on ' + PORT));
