// خدمة صغيرة داخل شاشة LG: تجلب روابط API (بدون قيود CORS) عندما يفشل الطلب المباشر
var Service = require('webos-service');
var http = require('http');
var https = require('https');
var url = require('url');
var svc = new Service('com.ayham.player.tv.service');

function get(u, n, cb) {
  var p = url.parse(u);
  if (p.protocol !== 'http:' && p.protocol !== 'https:') return cb(new Error('bad url'));
  var m = p.protocol === 'https:' ? https : http;
  var req = m.get({
    hostname: p.hostname, port: p.port, path: p.path,
    headers: { 'user-agent': 'Mozilla/5.0 (Linux; Android 11) AyhamPlayer/1.0', 'accept-encoding': 'identity' }
  }, function (res) {
    if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location && n < 4) {
      res.resume();
      return get(url.resolve(u, res.headers.location), n + 1, cb);
    }
    var b = [];
    res.on('data', function (d) { b.push(d); });
    res.on('end', function () { cb(null, res.statusCode, Buffer.concat(b).toString('utf8')); });
  });
  req.setTimeout(25000, function () { req.abort(); });
  req.on('error', function (e) { cb(e); });
}

svc.register('fetch', function (msg) {
  var u = String((msg.payload && msg.payload.url) || '');
  get(u, 0, function (e, st, body) {
    if (e) msg.respond({ returnValue: false, errorText: String(e) });
    else msg.respond({ returnValue: true, status: st, body: body });
  });
});
