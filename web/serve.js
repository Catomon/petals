const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.resolve(process.argv[2] || 'build/dist/webapp');
const port = Number(process.argv[3] || 8080);

const types = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.mp3': 'audio/mpeg',
  '.ogg': 'audio/ogg',
  '.wav': 'audio/wav',
  '.atlas': 'text/plain; charset=utf-8',
  '.fnt': 'text/plain; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.ttf': 'font/ttf',
};

http.createServer((req, res) => {
  let urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
  if (urlPath === '/') urlPath = '/index.html';

  const file = path.join(root, urlPath);
  if (!file.startsWith(root)) {
    res.writeHead(403).end();
    return;
  }

  fs.readFile(file, (error, data) => {
    if (error) {
      res.writeHead(404, { 'Content-Type': 'text/plain' }).end('not found: ' + urlPath);
      return;
    }
    res.writeHead(200, {
      'Content-Type': types[path.extname(file).toLowerCase()] || 'application/octet-stream',
      'Cache-Control': 'no-store',
    }).end(data);
  });
}).listen(port, () => console.log('Serving ' + root + ' on http://localhost:' + port));
