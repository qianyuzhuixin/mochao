/**
 * 通用工具模块 — HTTP 请求、状态提取、并发控制
 */

const http = require('http');
const https = require('https');
const zlib = require('zlib');

/** PC 端请求头 */
const PC_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) ' +
    'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
  'Accept-Encoding': 'identity',
};

/** GBK 请求头 — 用于晋江（需 gzip 支持） */
const GBK_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) ' +
    'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
  'Accept-Encoding': 'gzip, deflate',
};

/** 移动端请求头 */
const MOBILE_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) ' +
    'AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
  'Accept-Encoding': 'identity',
};

/**
 * 纯 HTTPS GET 请求 — 返回解码后的文本
 * @param {string} url
 * @param {object} headers
 * @param {number} redirects
 * @param {'utf8'|'gbk'} encoding
 * @returns {Promise<string>}
 */
function fetchText(url, headers = PC_HEADERS, redirects = 3, encoding = 'utf8') {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const opts = {
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      headers,
      timeout: 15000,
      insecureHTTPParser: true,  // 七猫等站点 CDN 可能返回非标准 HTTP 头
    };

    const req = https.get(opts, (res) => {
      if (
        redirects > 0 &&
        res.statusCode >= 300 &&
        res.statusCode < 400 &&
        res.headers.location
      ) {
        res.resume();
        const nextUrl = new URL(res.headers.location, url).toString();
        fetchText(nextUrl, headers, redirects - 1, encoding).then(resolve, reject);
        return;
      }

      const chunks = [];
      res.on('data', (chunk) => { chunks.push(chunk); });
      res.on('end', () => {
        let body = Buffer.concat(chunks);
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(`HTTP ${res.statusCode}`));
          return;
        }

        // 处理 gzip 压缩
        const ce = res.headers['content-encoding'] || '';
        if (ce.includes('gzip')) {
          try { body = zlib.gunzipSync(body); } catch (e) {
            reject(new Error('gzip decompress failed: ' + e.message));
            return;
          }
        } else if (ce.includes('deflate')) {
          try { body = zlib.inflateSync(body); } catch (e) {
            reject(new Error('deflate decompress failed: ' + e.message));
            return;
          }
        }

        // 解码
        if (encoding === 'gbk') {
          try {
            const iconv = require('iconv-lite');
            resolve(iconv.decode(body, 'gbk'));
          } catch {
            resolve(body.toString('utf8'));
          }
        } else {
          resolve(body.toString('utf8'));
        }
      });
    });

    req.on('timeout', () => {
      req.destroy(new Error('request timeout'));
    });
    req.on('error', reject);
  });
}

/**
 * HTTP GET 请求 — 用于访问 HTTP 协议的 URL（如第三方代理 API）
 * 与 fetchText 不同，此函数使用 http 模块而非 https 模块
 * @param {string} url HTTP 协议的 URL
 * @param {object} headers 请求头
 * @param {number} timeout 超时时间 (ms)
 * @returns {Promise<string>} 响应文本
 */
function httpFetchText(url, headers = {}, timeout = 15000) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    if (parsed.protocol !== 'http:') {
      // 非 HTTP 协议，交给 fetchText 处理
      return fetchText(url, headers, 3, 'utf8').then(resolve, reject);
    }
    const opts = {
      hostname: parsed.hostname,
      port: parsed.port || 80,
      path: parsed.pathname + parsed.search,
      headers: { ...headers, 'Accept-Encoding': 'identity' },
      timeout,
    };

    const req = http.get(opts, (res) => {
      const chunks = [];
      res.on('data', (chunk) => { chunks.push(chunk); });
      res.on('end', () => {
        const body = Buffer.concat(chunks);
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(`HTTP ${res.statusCode}`));
          return;
        }
        resolve(body.toString('utf8'));
      });
    });

    req.on('timeout', () => { req.destroy(new Error('request timeout')); });
    req.on('error', reject);
  });
}

/** 从 HTML 中提取 window.__INITIAL_STATE__ JSON 对象 */
function extractInitialState(html) {
  const markerIdx = html.indexOf('__INITIAL_STATE__');
  if (markerIdx === -1) return null;

  const eqIdx = html.indexOf('=', markerIdx);
  if (eqIdx === -1) return null;
  const braceStart = html.indexOf('{', eqIdx);
  if (braceStart === -1) return null;

  let depth = 0, end = braceStart;
  for (let i = braceStart; i < html.length; i++) {
    if (html[i] === '{') depth++;
    else if (html[i] === '}') {
      depth--;
      if (depth === 0) { end = i + 1; break; }
    }
  }

  let raw = html.substring(braceStart, end);
  raw = raw.replace(/\bundefined\b/g, 'null');

  try {
    return JSON.parse(raw);
  } catch (e) {
    console.log(`  ⚠ __INITIAL_STATE__ JSON 解析失败: ${e.message}`);
    return null;
  }
}

/** 并发控制：分批执行异步任务 */
async function batchRun(items, batchSize, fn) {
  const results = [];
  for (let i = 0; i < items.length; i += batchSize) {
    const batch = items.slice(i, i + batchSize);
    const batchResults = await Promise.allSettled(batch.map(fn));
    for (const r of batchResults) {
      if (r.status === 'fulfilled' && r.value) results.push(r.value);
    }
    // 批次间稍作停顿，避免请求过于密集
    if (i + batchSize < items.length) {
      await new Promise((r) => setTimeout(r, 200));
    }
  }
  return results;
}

/** 打印前 3 条调试 */
function logTop3(items, platform) {
  items.slice(0, 3).forEach((b) => {
    console.log(`[${platform}]   #${b.rankNo} ${b.bookName} - ${b.author} | ${b.category} | hot=${b.hotValue}`);
  });
}

module.exports = {
  PC_HEADERS,
  GBK_HEADERS,
  MOBILE_HEADERS,
  fetchText,
  httpFetchText,
  extractInitialState,
  batchRun,
  logTop3,
};
