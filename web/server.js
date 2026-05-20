const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const DIST_DIR = '/home/app/dist';
const API_BASE_URL = process.env.API_BASE_URL || '';

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject'
};

function proxyRequest(req, res) {
  const startTime = Date.now();
  const { method, headers } = req;

  let targetUrl;
  try {
    targetUrl = new URL(API_BASE_URL + req.url.replace(/^\/api/, ''));
  } catch (e) {
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ code: 500, message: 'API_BASE_URL 配置无效' }));
    return;
  }

  const proxyHeaders = { ...headers };
  delete proxyHeaders.host;
  proxyHeaders['X-Real-IP'] = headers['x-forwarded-for'] || headers['x-real-ip'] || req.socket.remoteAddress;
  proxyHeaders['X-Forwarded-For'] = proxyHeaders['X-Real-IP'];
  proxyHeaders['X-Forwarded-Proto'] = 'http';

  const proxyOptions = {
    hostname: targetUrl.hostname,
    port: targetUrl.port || 80,
    path: targetUrl.pathname + targetUrl.search,
    method: method,
    headers: proxyHeaders,
  };

  const proxyReq = http.request(proxyOptions, (proxyRes) => {
    const duration = Date.now() - startTime;
    console.log(`${new Date().toISOString()} [PROXY] ${method} ${req.url} -> ${targetUrl.href} ${proxyRes.statusCode} - ${duration}ms`);

    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (err) => {
    const duration = Date.now() - startTime;
    console.error(`${new Date().toISOString()} [PROXY ERROR] ${method} ${req.url} -> ${targetUrl.href} - ${duration}ms - ${err.message}`);
    res.writeHead(502, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ code: 502, message: '后端服务不可用: ' + err.message }));
  });

  req.pipe(proxyReq);
}

function serveStatic(req, res) {
  const startTime = Date.now();
  const { method, url, headers } = req;

  console.log(`${new Date().toISOString()} [REQUEST] ${method} ${url} - ${headers['user-agent']}`);

  let filePath = path.join(DIST_DIR, req.url === '/' ? 'index.html' : req.url);
  const ext = path.extname(filePath);
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, data) => {
    const duration = Date.now() - startTime;

    if (err) {
      if (err.code === 'ENOENT') {
        fs.readFile(path.join(DIST_DIR, 'index.html'), (err2, data2) => {
          if (err2) {
            res.writeHead(500);
            res.end('Internal Server Error');
            console.log(`${new Date().toISOString()} [RESPONSE] 500 ${method} ${url} - ${duration}ms`);
          } else {
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end(data2);
            console.log(`${new Date().toISOString()} [RESPONSE] 200 ${method} ${url} - ${duration}ms - Fallback to index.html`);
          }
        });
      } else {
        res.writeHead(500);
        res.end('Internal Server Error');
        console.log(`${new Date().toISOString()} [RESPONSE] 500 ${method} ${url} - ${duration}ms - ${err.message}`);
      }
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(data);
      console.log(`${new Date().toISOString()} [RESPONSE] 200 ${method} ${url} - ${duration}ms - ${contentType}`);
    }
  });
}

const server = http.createServer((req, res) => {
  if (API_BASE_URL && req.url.startsWith('/api/')) {
    proxyRequest(req, res);
  } else {
    serveStatic(req, res);
  }
});

server.listen(PORT, () => {
  console.log(`Server running at http://0.0.0.0:${PORT}/`);
  if (API_BASE_URL) {
    console.log(`API proxy enabled: /api/* -> ${API_BASE_URL}/*`);
  } else {
    console.log('API_BASE_URL not set, API proxy disabled');
  }
});
