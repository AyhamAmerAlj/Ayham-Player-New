// ينسخ hls.js من node_modules إلى public/vendor (يعمل وقت التثبيت على الخادم)
const fs = require('fs'), path = require('path');
const src = path.join(__dirname, '..', 'node_modules', 'hls.js', 'dist', 'hls.min.js');
const dstDir = path.join(__dirname, '..', 'public', 'vendor');
if (!fs.existsSync(src)) { console.warn('hls.js not found, skipping vendor copy'); process.exit(0); }
fs.mkdirSync(dstDir, { recursive: true });
fs.copyFileSync(src, path.join(dstDir, 'hls.min.js'));
console.log('hls.js copied to public/vendor');
