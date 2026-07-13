/**
 * MoChao Scraper Service — 网文榜单抓取服务
 *
 * 采集策略（全平台无浏览器，纯 HTTPS 请求）：
 *   起点中文网 → 移动端 m.qidian.com Vite SSR pageContext JSON 直读
 *   番茄小说   → 两步：榜单页取 bookId/热度，详情页取明文书名/作者/简介
 *                （番茄列表页有字体反爬，详情页 __INITIAL_STATE__ 是明文）
 *   晋江文学城 → topten.php 频道分组 + onebook.php detail 微数据
 *   知乎盐言   → 无公开排行榜，返回提示
 *
 * Express 独立进程，监听 localhost:3001
 */

const express = require('express');
const https = require('https');
const zlib = require('zlib');

const app = express();
app.use(express.json());

const PORT = process.env.SCRAPER_PORT || 3001;

// ===========================
// 通用工具
// ===========================

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

// ===========================
// 起点中文网 — 移动端 SSR
// ===========================

const QIDIAN_RANK_URLS = {
  month_ticket: 'https://m.qidian.com/rank/yuepiao/',
  recommend:    'https://m.qidian.com/rank/rec/',
  collect:      'https://m.qidian.com/rank/newfans/',
  hotsales:     'https://m.qidian.com/rank/hotsales/',
  readindex:    'https://m.qidian.com/rank/readindex/',
  newsign:      'https://m.qidian.com/rank/newsign/',
  signnewbook:  'https://m.qidian.com/rank/signnewbook/',
  newauthor:    'https://m.qidian.com/rank/newauthor/',
  pubnewbook:   'https://m.qidian.com/rank/newbook/',
  sanjiang:     'https://m.qidian.com/sanjiang/',
};

const QIDIAN_RANK_LABELS = {
  month_ticket: '月票榜',
  recommend:    '推荐票榜',
  collect:      '收藏榜',
  hotsales:     '畅销榜',
  readindex:    '阅读指数榜',
  newsign:      '新人签约新书榜',
  signnewbook:  '签约作者新书榜',
  newauthor:    '新人作者新书榜',
  pubnewbook:   '公众作者新书榜',
  sanjiang:     '三江推荐',
};

function extractQidianPageContext(html) {
  const m = html.match(
    /<script[^>]+id=["']vite-plugin-ssr_pageContext["'][^>]*>([\s\S]*?)<\/script>/i
  );
  if (!m) return null;
  try { return JSON.parse(m[1]); } catch (e) { return null; }
}

function normalizeQidianBook(record, idx) {
  const title = record.bName || record.bookName || '';
  const bid = record.bid || record.bookId || '';
  return {
    bookName: title,
    author: record.bAuth || record.author || '',
    category: [record.cat, record.subCat].filter(Boolean).join('·'),
    wordCount: parseInt(record.wordCnt) || 0,
    hotValue: parseInt(record.cnt) || parseInt(record.rankCnt) || 0,
    intro: (record.desc || '').slice(0, 200),
    coverUrl: '',
    bookUrl: bid ? `https://www.qidian.com/book/${bid}/` : '',
    rankNo: record.rankNum || idx + 1,
  };
}

async function scrapeQidian(rankType) {
  const url = QIDIAN_RANK_URLS[rankType];
  if (!url) throw new Error(`未知榜单: ${rankType}`);

  const label = QIDIAN_RANK_LABELS[rankType] || rankType;
  console.log(`[qidian] → 采集${label}（移动端 SSR）...`);

  const html = await fetchText(url, MOBILE_HEADERS);
  const pageContext = extractQidianPageContext(html);
  if (!pageContext) throw new Error('未找到 vite-plugin-ssr_pageContext');

  const pageData = pageContext?.pageContext?.pageProps?.pageData;
  const records = pageData?.records || [];
  if (!records.length) return [];

  const items = records
    .map((r, i) => normalizeQidianBook(r, i))
    .filter((b) => b.bookName);

  console.log(`[qidian]   ✓ ${items.length} 本`);
  logTop3(items, 'qidian');
  return items;
}

// ===========================
// 番茄小说 — 两步策略：榜单取 bookId + 详情取明文
// ===========================

// 分类 id 列表（来自 __INITIAL_STATE__.rank.rankCategoryTypeList）
const FANQIE_CATEGORIES = {
  male: {
    1141: '西方奇幻',  1140: '东方仙侠',    8: '科幻末世',
    261:  '都市日常',  124:  '都市修真',   1014: '都市高武',
    273:  '历史古代',   27:  '战神赘婿',    263: '都市种田',
    258:  '传统玄幻',  272:  '历史脑洞',    539: '悬疑脑洞',
    262:  '都市脑洞',  257:  '玄幻脑洞',    751: '悬疑灵异',
    504:  '抗战谍战',  746:  '游戏体育',    718: '动漫衍生',
    1016: '男频衍生',
  },
  female: {
    1139: '古风世情',    8: '科幻末世',    746: '游戏体育',
    1015: '女频衍生',  248: '玄幻言情',     23: '种田',
      79: '年代',      267: '现言脑洞',    246: '宫斗宅斗',
     539: '悬疑脑洞',  253: '古言脑洞',     24: '快穿',
     749: '青春甜宠',  745: '星光璀璨',    747: '女频悬疑',
     750: '职场婚恋',  748: '豪门总裁',   1017: '民国言情',
  },
};

/** 综合榜单（全站不分品类） */
const FANQIE_OVERALL_RANK_URLS = {
  hot_search: 'https://fanqienovel.com/rank/hot_search',
  read_rank:  'https://fanqienovel.com/rank/read_rank',
  new_book:   'https://fanqienovel.com/rank/new_book',
};

const FANQIE_OVERALL_LABELS = {
  hot_search: '热搜榜',
  read_rank:  '阅读榜',
  new_book:   '新书榜',
};

/**
 * 从番茄榜单页提取书籍列表（仅 bookId + 数字指标）
 * 文本字段（书名/作者/简介）因字体混淆不可靠，需从详情页补取
 */
function extractFanqieBookIdsFromRank(html) {
  const state = extractInitialState(html);
  if (!state) return [];
  const bookList = state?.rank?.book_list;
  if (!Array.isArray(bookList)) return [];
  return bookList
    .filter((b) => b.bookId)
    .map((b) => ({
      bookId: String(b.bookId),
      read_count: parseInt(b.read_count || b.readCount) || 0,
      wordNumber: parseInt(b.wordNumber) || 0,
      creationStatus: b.creationStatus,
      lastChapterTitle: b.lastChapterTitle || '',
      currentPos: parseInt(b.currentPos) || 0,
      category: b.category || '',
      // 列表页的文本字段可能有乱码，仅做初步记录
      bookNameRaw: b.bookName || '',
      authorRaw: b.author || '',
    }));
}

/**
 * 从番茄详情页 __INITIAL_STATE__.page 取明文字段
 * 详情页无字体反爬，bookName/author/abstract/categoryV2 均为正常中文
 */
async function fetchFanqieDetail(bookId) {
  try {
    const html = await fetchText(
      `https://fanqienovel.com/page/${bookId}`,
      PC_HEADERS, 3, 'utf8'
    );
    const state = extractInitialState(html);
    const page = state?.page;
    if (!page) return null;

    // categoryV2 是转义 JSON 数组，格式如 [{"Id":1141,"Name":"西方奇幻"}]
    let categoryV2 = '';
    try {
      const catArr = typeof page.categoryV2 === 'string'
        ? JSON.parse(page.categoryV2)
        : page.categoryV2;
      if (Array.isArray(catArr) && catArr.length) {
        categoryV2 = catArr.map((c) => c.Name).join('·');
      }
    } catch {}

    return {
      bookId,
      bookName: page.bookName || '',
      author: page.author || '',
      abstract: page.abstract || page.description || '',
      categoryV2,
      wordNumber: parseInt(page.wordNumber) || 0,
      readCount: parseInt(page.readCount) || 0,
      creationStatus: page.creationStatus,
      thumbUri: page.thumbUri || '',
      lastChapterTitle: page.lastChapterTitle || '',
      status: page.status || '',
    };
  } catch (e) {
    return null;
  }
}

async function scrapeFanqie(rankType) {
  const url = FANQIE_OVERALL_RANK_URLS[rankType];
  if (!url) throw new Error(`未知榜单: ${rankType}`);

  const label = FANQIE_OVERALL_LABELS[rankType] || rankType;
  console.log(`[fanqie] → 采集${label}（两步：榜单 → 详情页解码）...`);

  // Step 1: 从榜单页取 bookId + 数字指标
  const html = await fetchText(url, PC_HEADERS, 3, 'utf8');
  const bookIdList = extractFanqieBookIdsFromRank(html);
  if (!bookIdList.length) {
    console.log('[fanqie]   榜单页未提取到书籍');
    return [];
  }
  console.log(`[fanqie]   榜单提取 ${bookIdList.length} 个 bookId`);

  // Step 2: 并发请求详情页取明文字段（每次 5 个并发）
  const details = await batchRun(
    bookIdList.map((b) => b.bookId),
    5,
    fetchFanqieDetail
  );

  // 构建详情映射
  const detailMap = {};
  for (const d of details) {
    if (d && d.bookId) detailMap[d.bookId] = d;
  }

  // Step 3: 合并结果，优先使用详情页的明文字段
  const items = bookIdList
    .map((rank) => {
      const detail = detailMap[rank.bookId] || {};
      return {
        bookName: detail.bookName || rank.bookNameRaw || '',
        author: detail.author || rank.authorRaw || '',
        category: detail.categoryV2 || rank.category || '',
        wordCount: detail.wordNumber || rank.wordNumber,
        hotValue: detail.readCount || rank.read_count,
        intro: (detail.abstract || '').slice(0, 200),
        coverUrl: detail.thumbUri || '',
        bookUrl: `https://fanqienovel.com/page/${rank.bookId}`,
        rankNo: rank.currentPos,
        status: detail.creationStatus === 1 ? '连载中' : detail.creationStatus === 2 ? '已完结' : '',
        lastChapter: detail.lastChapterTitle || rank.lastChapterTitle,
      };
    })
    .filter((b) => b.bookName && b.rankNo > 0);

  const resolved = items.filter((b) => b.author && !b.author.includes('\uE000')).length;
  console.log(`[fanqie]   ✓ ${items.length} 本（明文 ${resolved}/${items.length}）`);
  logTop3(items, 'fanqie');
  return items;
}

/**
 * 番茄分类榜抓取
 * 按指定的频道、品类和榜单类型分别抓取
 * rankType 参数格式: "male-258-hot_search" 或 "female-248-new_book"
 */
async function scrapeFanqieCategory(rankType) {
  // 解析: {channel}-{catId}-{rankSubType}
  const parts = rankType.split('-');
  if (parts.length < 3) throw new Error(`分类榜格式: {channel}-{catId}-{rankSubType}，收到: ${rankType}`);

  const channel = parts[0] === 'female' ? '0' : '1';
  const catId = parts[1];
  const rankSubType = parts[2]; // hot_search / read_rank / new_book

  // 品类映射: hot_search -> 2(阅读榜), new_book -> 1(新书榜), read_rank -> 2
  const typeMap = { hot_search: '2', read_rank: '2', new_book: '1' };
  const type = typeMap[rankSubType] || '2';
  const typeLabel = rankSubType === 'new_book' ? '新书榜' : '阅读榜';

  const catName = (FANQIE_CATEGORIES[parts[0]] || {})[catId] || `品类${catId}`;
  const chLabel = channel === '1' ? '男频' : '女频';

  const url = `https://fanqienovel.com/rank/${channel}_${type}_${catId}`;
  console.log(`[fanqie] → 采集${chLabel}·${catName}${typeLabel}（${url}）...`);

  const html = await fetchText(url, PC_HEADERS, 3, 'utf8');
  const bookIdList = extractFanqieBookIdsFromRank(html);
  if (!bookIdList.length) {
    console.log('[fanqie]   未提取到书籍');
    return [];
  }
  console.log(`[fanqie]   榜单提取 ${bookIdList.length} 个 bookId`);

  // 并发请求详情页
  const details = await batchRun(
    bookIdList.map((b) => b.bookId),
    5,
    fetchFanqieDetail
  );

  const detailMap = {};
  for (const d of details) {
    if (d && d.bookId) detailMap[d.bookId] = d;
  }

  const items = bookIdList
    .map((rank) => {
      const detail = detailMap[rank.bookId] || {};
      return {
        bookName: detail.bookName || rank.bookNameRaw || '',
        author: detail.author || rank.authorRaw || '',
        category: detail.categoryV2 || catName,
        wordCount: detail.wordNumber || rank.wordNumber,
        hotValue: detail.readCount || rank.read_count,
        intro: (detail.abstract || '').slice(0, 200),
        coverUrl: detail.thumbUri || '',
        bookUrl: `https://fanqienovel.com/page/${rank.bookId}`,
        rankNo: rank.currentPos,
        status: detail.creationStatus === 1 ? '连载中' : detail.creationStatus === 2 ? '已完结' : '',
        lastChapter: detail.lastChapterTitle || rank.lastChapterTitle,
      };
    })
    .filter((b) => b.bookName && b.rankNo > 0);

  console.log(`[fanqie]   ✓ ${items.length} 本`);
  return items;
}

// ===========================
// 晋江文学城 — topten.php + onebook.php
// ===========================

const JINJIANG_RANK_CONFIG = {
  // 收入金榜 (orderstr=12)
  income12:  { url: 'https://www.jjwxc.net/topten.php?orderstr=12&t=0', label: '收入金榜', orderstr: '12' },
  // 月榜 (orderstr=7)
  month7:    { url: 'https://www.jjwxc.net/topten.php?orderstr=7&t=0',  label: '月榜',     orderstr: '7'  },
  // 季度榜 (orderstr=8)
  season8:   { url: 'https://www.jjwxc.net/topten.php?orderstr=8&t=0',  label: '季度榜',   orderstr: '8'  },
  // 完结金榜 (orderstr=14)
  finish14:  { url: 'https://www.jjwxc.net/topten.php?orderstr=14&t=0', label: '完结金榜', orderstr: '14' },
  // 新手金榜 (orderstr=15)
  new15:     { url: 'https://www.jjwxc.net/topten.php?orderstr=15&t=0', label: '新手金榜', orderstr: '15' },
  // 千字金榜 (orderstr=17)
  kzi17:     { url: 'https://www.jjwxc.net/topten.php?orderstr=17&t=0', label: '千字金榜', orderstr: '17' },
  // 收藏榜 (保留 bookbase.php 方案)
  collect:   { url: 'https://www.jjwxc.net/bookbase.php?fw0=0&fbsj=0&ycx=0&xx3=3&sortType=4', label: '收藏榜' },
};

/**
 * 从 topten.php 解析频道分组 + 书籍列表
 *
 * topten 页面结构：
 *   每个频道的书籍在独立 div 中，<b>频道名</b> 标记频道标题
 *   书籍通过 <a href="onebook.php?novelid=XXX">书名</a> 呈现
 *   直接基于 <a> 标签解析，更稳健、不依赖文本行结构
 */
function parseJinjiangToptens(html) {
  // 去掉 select/option/head/script/style 等干扰元素
  html = html.replace(/<select[\s\S]*?<\/select>/gi, '');
  html = html.replace(/<head>[\s\S]*?<\/head>/gi, '');
  html = html.replace(/<(script|style)[\s\S]*?<\/\1>/gi, '');

  // 晋江频道名
  const jjChannels = [
    '古代言情', '现代言情', '古代穿越', '现代都市纯爱', '现代幻想纯爱',
    '古代纯爱', '衍生纯爱', '幻想现言', '奇幻言情', '未来游戏悬疑',
    '百合', '无CP', '二次元言情', '衍生言情', '衍生无cp',
    '未来幻想纯爱', '原创轻小说', '多元',
  ];
  const channelSet = new Set(jjChannels);

  // 按频道分组的书籍
  // 先找出所有频道名的位置
  const channelPositions = [];
  for (const ch of jjChannels) {
    let pos = html.indexOf(ch);
    while (pos !== -1) {
      // 确保前后是标签边界（在 <b> 或 > 之后）
      const before = html.substring(Math.max(0, pos - 3), pos);
      if (/>/.test(before) || before === '') {
        channelPositions.push({ name: ch, pos });
      }
      pos = html.indexOf(ch, pos + 1);
    }
  }
  channelPositions.sort((a, b) => a.pos - b.pos);

  // 提取所有小说链接 (novelid)
  const allLinks = [];
  const linkRegex = /<a[^>]*href="[^"]*novelid=(\d+)"[^>]*alt="([^"]*)"[^>]*title="([^"]*)"[^>]*>/gi;
  let lm;
  while ((lm = linkRegex.exec(html)) !== null) {
    const novelId = lm[1];
    const alt = lm[2].trim();
    const titleAttr = lm[3].trim();
    // 跳过霸王票等非书籍链接
    if (alt === '花中娇客，现代言情。') continue; // 实际书名
    if (!alt || alt.indexOf('向《') > -1 || alt.indexOf('投') > -1) continue;
    allLinks.push({ novelId, title: alt, pos: lm.index });
  }

  // 按频道分组
  const channelBooks = {};
  for (const link of allLinks) {
    // 找到 link 所属的频道（pos 之前的最后一个频道名）
    let channel = '';
    for (let i = channelPositions.length - 1; i >= 0; i--) {
      if (channelPositions[i].pos < link.pos) {
        channel = channelPositions[i].name;
        break;
      }
    }
    if (!channel) channel = '未分类';
    if (!channelBooks[channel]) channelBooks[channel] = [];
    channelBooks[channel].push({ title: link.title, novelId: link.novelId });
  }

  // 转换为统一格式
  const items = [];
  for (const ch of jjChannels) {
    const books = channelBooks[ch];
    if (!books || !books.length) continue;
    books.forEach((b, i) => {
      items.push({
        bookName: b.title,
        author: '',  // topten 页面无作者，需从详情页补
        novelId: b.novelId,
        channel: ch,
        rankNo: i + 1,
      });
    });
  }

  return items;
}

/**
 * 从 flat table 格式的 topten 页面解析（月榜/季榜/完结金榜/新手金榜）
 * 表格列：序号 | 作者 | 作品 | 类型 | 进度 | 字数 | 作品积分 | 发表时间
 */
function parseJinjiangFlatTable(html) {
  // 去掉 head/script/style
  html = html.replace(/<head>[\s\S]*?<\/head>/gi, '');
  html = html.replace(/<(script|style)[\s\S]*?<\/\1>/gi, '');

  // 找包含 novelid 链接的表格行
  const items = [];
  const rowRegex = /<tr[^>]*>([\s\S]*?)<\/tr>/gi;
  let rm;
  while ((rm = rowRegex.exec(html)) !== null) {
    const row = rm[1];
    if (!row.includes('novelid=')) continue;

    // 提取 novelId 和书名（只从含 novelid 的 <a> 标签提取 title）
    const idMatch = row.match(/novelid=(\d+)/i);
    if (!idMatch) continue;
    const novelId = idMatch[1];
    
    // 只匹配含 novelid 的 <a> 标签内的 title 属性
    const novelLink = row.match(/<a[^>]*novelid=\d+[\s\S]*?>/i);
    let title = '';
    if (novelLink) {
      const tMatch = novelLink[0].match(/title="([^"]+)"/i);
      if (tMatch) title = tMatch[1].trim();
    }
    if (!title) {
      // fallback: inner text
      const inner = row.match(/<a[^>]*novelid=\d+[\s\S]*?>([^<]*)</i);
      if (inner) title = inner[1].trim();
    }

    // 提取各列纯文本
    const cells = [];
    const tdRegex = /<td[^>]*>([\s\S]*?)<\/td>/gi;
    let tm;
    while ((tm = tdRegex.exec(row)) !== null) {
      cells.push(tm[1].replace(/<[^>]+>/g, '').replace(/&nbsp;/g, '').trim());
    }

    // 列结构: [序号, 作者, 作品, 类型, 进度, 字数, 积分, 发表时间]
    const author = cells[1] || '';
    const category = cells[3] || '';
    const status = cells[4] || '';
    const wordCount = parseInt((cells[5] || '').replace(/[^0-9]/g, '')) || 0;
    const hotValue = parseInt((cells[6] || '').replace(/[^0-9]/g, '')) || 0;

    items.push({
      bookName: title,
      author: author,
      novelId: novelId,
      channel: category,
      category: category,
      status: status,
      wordCount: wordCount,
      hotValue: hotValue,
      rankNo: items.length + 1,
    });
  }

  return items;
}

/**
 * 从晋江详情页获取微数据（收藏/营养液/积分/字数/状态）
 * 使用 fetch + arrayBuffer + TextDecoder('gb18030') 解码
 * 指标来自 itemprop 微数据
 */
async function fetchJinjiangDetail(novelId) {
  try {
    const html = await fetchText(
      `https://www.jjwxc.net/onebook.php?novelid=${novelId}`,
      GBK_HEADERS, 3, 'gbk'
    );

    const extract = (prop) => {
      const m = html.match(new RegExp(`itemprop="${prop}"[^>]*>([^<]*)<`));
      return m ? m[1].trim() : '';
    };

    const statusMatch = html.match(/itemprop="updataStatus"[^>]*>\s*([^<\s]{1,6})/);
    const status = statusMatch ? statusMatch[1]
      : (html.match(/(连载中|已完结|完结)/) || [])[1] || '';

    return {
      novelId,
      collect: extract('collectedCount'),
      nutrition: extract('nutritionCount'),
      score: extract('scoreCount'),
      wordCount: extract('wordCount'),
      status,
    };
  } catch (e) {
    return null;
  }
}

async function scrapeJinjiang(rankType) {
  const cfg = JINJIANG_RANK_CONFIG[rankType];
  if (!cfg) throw new Error(`未知榜单: ${rankType}`);

  const label = cfg.label || rankType;
  console.log(`[jinjiang] → 采集${label}（${cfg.url}）...`);

  const html = await fetchText(cfg.url, GBK_HEADERS, 3, 'gbk');

  // 先尝试 h5 频道分组格式（收入金榜/千字金榜）
  let items = parseJinjiangToptens(html);

  if (!items.length) {
    // 尝试 flat table 格式（月榜/季榜/完结金榜/新手金榜）
    console.log('[jinjiang]   topten 频道分组为空，尝试 flat table 格式...');
    items = parseJinjiangFlatTable(html);
  }

  if (!items.length) {
    console.log('[jinjiang]   两种格式均解析失败');
    return [];
  }

  const isFlatTable = items[0] && items[0].author; // flat table 自带作者等字段
  console.log(`[jinjiang]   列表提取 ${items.length} 本${!isFlatTable ? '，' + new Set(items.map((i) => i.channel)).size + ' 个频道' : ''}`);

  // 选每频道前 10 本补采详情页指标（有 novelId 的才补）
  const toDetail = [];
  const channelCounts = {};
  for (const item of items) {
    if (!item.novelId) continue;
    const cnt = channelCounts[item.channel] || 0;
    if (cnt >= 10) continue;
    toDetail.push(item.novelId);
    channelCounts[item.channel] = cnt + 1;
    if (toDetail.length >= 100) break; // 总量上限
  }

  let detailMap = {};
  if (toDetail.length) {
    console.log(`[jinjiang]   → 补采详情 ${toDetail.length} 本...`);
    const details = await batchRun(toDetail, 6, fetchJinjiangDetail);
    for (const d of details) {
      if (d && d.novelId) detailMap[d.novelId] = d;
    }
    const ok = Object.values(detailMap).filter((d) => d.collect).length;
    console.log(`[jinjiang]   详情命中 ${ok}/${toDetail.length}`);
  }

  // 合并结果
  const result = items.map((item) => {
    const detail = item.novelId ? (detailMap[item.novelId] || {}) : {};
    return {
      bookName: item.bookName,
      author: item.author || '',
      category: item.channel || item.category || '',
      wordCount: item.wordCount || parseInt(detail.wordCount) || 0,
      hotValue: item.hotValue || parseInt(detail.collect) || parseInt(detail.score) || 0,
      intro: '',
      coverUrl: '',
      bookUrl: item.novelId ? `https://www.jjwxc.net/onebook.php?novelid=${item.novelId}` : '',
      rankNo: item.rankNo,
      collectCount: detail.collect ? parseInt(detail.collect.replace(/[^0-9]/g, '')) || 0 : 0,
      nutrition: parseInt(detail.nutrition) || 0,
      status: item.status || detail.status || '',
    };
  });

  console.log(`[jinjiang]   ✓ ${result.length} 本`);
  logTop3(result, 'jinjiang');
  return result;
}

// ===========================
// 知乎盐言 — 无公开排行榜
// ===========================

async function scrapeZhihu(rankType) {
  console.log(`[zhihu] → 知乎盐言无公开排行榜`);
  throw new Error(
    '知乎盐言（付费故事）无公开排行榜页面。' +
    '知乎内容分发依赖个性化推荐算法，不提供传统意义上的榜单。'
  );
}

// ===========================
// 七猫小说 — Nuxt SSR，HTML 直解析
// ===========================

const QIMAO_RANK_TYPES = {
  hot:    { label: '大热榜', path: 'hot' },
  new:    { label: '新书榜', path: 'new' },
  over:   { label: '完结榜', path: 'over' },
  collect:{ label: '收藏榜', path: 'collect' },
  update: { label: '更新榜', path: 'update' },
};

const QIMAO_CHANNELS = {
  boy:   { label: '男频', path: 'boy' },
  girl:  { label: '女频', path: 'girl' },
};

/**
 * 从七猫榜单页解析书籍列表
 * rankType 格式: "boy-hot", "girl-new" 等
 */
async function scrapeQimao(rankType) {
  const [chKey, typeKey] = rankType.split('-');
  const channel = QIMAO_CHANNELS[chKey];
  const rankCfg = QIMAO_RANK_TYPES[typeKey];
  if (!channel || !rankCfg) throw new Error(`未知榜单: ${rankType}，格式: boy-hot / girl-new 等`);

  // 默认都取日榜
  const url = `https://www.qimao.com/paihang/${channel.path}/${rankCfg.path}/date/`;
  console.log(`[qimao] → 采集${channel.label}·${rankCfg.label}（SSR）...`);

  const html = await fetchText(url, PC_HEADERS);

  // 解析每本书的 li.rank-list-item
  const items = [];
  const itemBlocks = html.match(/<li[^>]*class="[^"]*rank-list-item[^"]*"[^>]*>[\s\S]*?<\/li>/gi) || [];

  for (let i = 0; i < itemBlocks.length; i++) {
    const blk = itemBlocks[i];

    // 提取 book URL 和 bookId
    const urlM = blk.match(/href="https?:\/\/www\.qimao\.com\/shuku\/(\d+)\//);
    const bookId = urlM ? urlM[1] : '';

    // 书名
    const titleM = blk.match(/class="s-book-title"[^>]*>([^<]+)</);
    const title = titleM ? titleM[1].trim() : '';

    // s-book-info 内容: 作者 · 分类 · 子分类 · 状态 · 字数
    const infoBlock = blk.match(/class="s-book-info clearfix"[^>]*>([\s\S]*?)<\/span>/i);
    let author = '', category = '', subCategory = '', status = '', wordCount = '';
    if (infoBlock) {
      const infoHtml = infoBlock[1];
      // 提取所有 <a> 和 <em>
      const links = [...infoHtml.matchAll(/<a[^>]*>([^<]*)<\/a>/g)];
      const ems = [...infoHtml.matchAll(/<em[^>]*>([^<]*)<\/em>/g)];

      if (links.length >= 1) author = links[0][1].trim();
      if (links.length >= 2) category = links[1][1].trim();
      if (links.length >= 3) subCategory = links[2][1].trim();
      if (ems.length >= 1) status = ems[0][1].trim();
      if (ems.length >= 2) {
        const wcRaw = ems[1][1].trim();
        wordCount = wcRaw.replace('万字', '').replace(/[^0-9.]/g, '');
      }
    }

    // 简介
    const introM = blk.match(/class="s-book-intro"[^>]*>([^<]*)</);
    const intro = introM ? introM[1].trim().slice(0, 200) : '';

    // 热度值
    const heatM = blk.match(/class="rank-num"[^>]*>([^<]*)</);
    let hotValue = 0;
    if (heatM) {
      const hRaw = heatM[1].replace(/[万,]/g, '');
      hotValue = parseFloat(hRaw) || 0;
    }

    // 排名
    let rankNo = i + 1;
    const rankM = blk.match(/class="rank-number[^"]*"[^>]*>(\d+)</);
    if (rankM) rankNo = parseInt(rankM[1]);

    // 封面
    const coverM = blk.match(/src="(https?:[^"]+\.(?:jpg|png|jpeg|webp)[^"]*)"/i);
    const coverUrl = coverM ? coverM[1] : '';

    if (title) {
      items.push({
        bookName: title,
        author,
        category: [category, subCategory].filter(Boolean).join('·'),
        wordCount: Math.round(parseFloat(wordCount) * 10000) || 0,
        hotValue,
        intro,
        coverUrl,
        bookUrl: bookId ? `https://www.qimao.com/shuku/${bookId}/` : '',
        rankNo,
        status,
      });
    }
  }

  console.log(`[qimao]   ✓ ${items.length} 本`);
  logTop3(items, 'qimao');
  return items;
}

// ===========================
// 刺猬猫 — 传统 SSR，HTML 直解析
// ===========================

const CIWEIMAO_RANK_TYPES = {
  click:      { label: '点击榜', h3: '点击榜' },
  favor:      { label: '收藏榜', h3: '收藏榜' },
  recommend:  { label: '推荐榜', h3: '推荐榜' },
  subscribe:  { label: '订阅榜', h3: '订阅榜' },
  monthly:    { label: '月票榜', h3: '月票榜' },
  tsukkomi:   { label: '吐槽榜', h3: '吐槽榜' },
  newbook:    { label: '新书榜', h3: '新书榜' },
  blade:      { label: '刀片榜', h3: '刀片榜' },
  update:     { label: '更新榜', h3: '更新榜' },
};

/**
 * 从刺猬猫榜单页解析指定榜单
 * rankType: click / favor / recommend / subscribe / monthly / tsukkomi / newbook / blade / update
 */
async function scrapeCiweimao(rankType) {
  const rankCfg = CIWEIMAO_RANK_TYPES[rankType];
  if (!rankCfg) throw new Error(`未知榜单: ${rankType}`);

  const url = 'https://www.ciweimao.com/rank-index';
  console.log(`[ciweimao] → 采集${rankCfg.label}（SSR）...`);

  const html = await fetchText(url, PC_HEADERS);

  // 定位对应榜单的 J_RecommendBox
  const h3Escaped = rankCfg.h3.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const sectionRe = new RegExp(
    `<div class="J_RecommendBox recommend-box">[\\s\\S]*?<h3[^>]*>${h3Escaped}</h3>[\\s\\S]*?<ul class="tab">([\\s\\S]*?)</ul>`,
    'i'
  );
  const sectionM = html.match(sectionRe);
  if (!sectionM) throw new Error(`未找到榜单: ${rankCfg.h3}`);

  const sectionHtml = sectionM[1];
  const items = [];

  // 提取所有 <li> 标签
  const liBlocks = [...sectionHtml.matchAll(/<li[^>]*>([\s\S]*?)<\/li>/gi)];
  for (let i = 0; i < liBlocks.length; i++) {
    const liHtml = liBlocks[i][1];

    // 是否 NO.1（有 info div 和 img）
    const isNo1 = liHtml.includes('class="info"');

    let title = '', author = '', genre = '', metric = '', bookId = '';

    // 提取 book URL
    const urlM = liHtml.match(/href="https?:\/\/www\.ciweimao\.com\/book\/(\d+)"/i);
    if (urlM) bookId = urlM[1];

    // NO.1 特殊格式
    if (isNo1) {
      const tM = liHtml.match(/<h3><a[^>]*>([^<]+)<\/a><\/h3>/i);
      const aM = liHtml.match(/<p class="author"><a[^>]*>([^<]+)<\/a><\/p>/i);
      const mM = liHtml.match(/class="num"[^>]*><span>([^<]*)<\/span>/i);

      if (tM) title = tM[1].trim();
      if (aM) author = aM[1].trim();
      if (mM) metric = mM[1].trim();
    } else {
      // #2+ 格式: <a href="..."><i>N</i><b>[genre]</b>title <span>metric</span></a>
      const aM = liHtml.match(/<a[^>]*href="\/\/www\.ciweimao\.com\/book\/\d+"[^>]*>([\s\S]*?)<\/a>/i) ||
                   liHtml.match(/<a[^>]*href="https?:\/\/www\.ciweimao\.com\/book\/\d+"[^>]*>([\s\S]*?)<\/a>/i);
      if (aM) {
        const inner = aM[1];
        const genreM = inner.match(/<b>\[([^\]]*)\]<\/b>/);
        const titleM = inner.match(/<\/b>([^<]+)</);
        const metricM = inner.match(/<span[^>]*>([^<]*)<\/span>/);

        if (genreM) genre = genreM[1].trim();
        if (titleM) title = titleM[1].trim();
        if (metricM) metric = metricM[1].trim();
      }
    }

    if (title) {
      // 解析指标值为数字
      let hotValue = 0;
      if (metric) {
        const numStr = metric.replace(/[万,]/g, '');
        hotValue = parseFloat(numStr) || 0;
      }

      items.push({
        bookName: title,
        author,
        category: genre,
        wordCount: 0,
        hotValue,
        intro: '',
        coverUrl: '',
        bookUrl: bookId ? `https://www.ciweimao.com/book/${bookId}/` : '',
        rankNo: i + 1,
        status: '',
      });
    }
  }

  console.log(`[ciweimao]   ✓ ${items.length} 本`);
  logTop3(items, 'ciweimao');
  return items;
}

// ===========================
// 路由分发
// ===========================

const PLATFORM_SCRAPERS = {
  qidian:   scrapeQidian,
  fanqie:   scrapeFanqie,
  jinjiang: scrapeJinjiang,
  zhihu:    scrapeZhihu,
  qimao:    scrapeQimao,
  ciweimao: scrapeCiweimao,
};

const PLATFORM_LABELS = {
  qidian:   '起点',
  fanqie:   '番茄',
  jinjiang: '晋江',
  zhihu:    '知乎',
  qimao:    '七猫',
  ciweimao: '刺猬猫',
};

/** 番茄分类榜路由映射 */
const FANQIE_CAT_SCRAPERS = {};
for (const ch of ['male', 'female']) {
  const cats = ch === 'male' ? Object.keys(FANQIE_CATEGORIES.male) : Object.keys(FANQIE_CATEGORIES.female);
  for (const catId of cats) {
    for (const sub of ['hot_search', 'read_rank', 'new_book']) {
      const key = `${ch}-${catId}-${sub}`;
      FANQIE_CAT_SCRAPERS[key] = () => scrapeFanqieCategory(key);
    }
  }
}

// ===========================
// 番茄小说整本下载
// ===========================

/** 加载字体解密映射表 */
const fs = require('fs');
const path = require('path');
let FANQIE_CHARSET = null;
try {
  const charsetPath = path.join(__dirname, 'charset.json');
  FANQIE_CHARSET = JSON.parse(fs.readFileSync(charsetPath, 'utf8'));
  console.log('[scraper] charset.json 加载成功');
} catch (e) {
  console.warn('[scraper] charset.json 加载失败，番茄章节内容将无法解密:', e.message);
}

/** 番茄字体 PUA 编码范围: mode 0 和 mode 1 */
const FANQIE_CODE = [[58344, 58715], [58345, 58716]];

/**
 * 番茄字体解密 — 将 PUA 字符映射回真实汉字
 * @param {string} content 加密内容
 * @param {number} mode 0 或 1
 * @returns {string} 解密后内容
 */
function decodeFanqieText(content, mode = 0) {
  if (!FANQIE_CHARSET) return content;
  const charset = FANQIE_CHARSET[mode];
  if (!charset) return content;
  const [codeStart, codeEnd] = FANQIE_CODE[mode];
  let result = '';
  for (const char of content) {
    const uni = char.charCodeAt(0);
    if (uni >= codeStart && uni <= codeEnd) {
      const bias = uni - codeStart;
      if (bias < charset.length && charset[bias] !== '?') {
        result += charset[bias];
      } else {
        result += char;
      }
    } else {
      result += char;
    }
  }
  return result;
}

/**
 * 生成随机 novel_web_id cookie
 */
function generateNovelWebId() {
  const bas = 1000000000000000000;
  const min = bas * 6;
  const max = bas * 9;
  return String(Math.floor(Math.random() * (max - min + 1)) + min);
}

/**
 * 从番茄书籍页面提取章节列表
 * 页面 URL: https://fanqienovel.com/page/{bookId}
 * @returns {Promise<{title: string, chapterId: string}[]|null>}
 */
async function fetchFanqieChapterList(bookId) {
  const url = `https://fanqienovel.com/page/${bookId}`;
  const html = await fetchText(url, PC_HEADERS, 3, 'utf8');

  // 策略1: 从 __INITIAL_STATE__ 提取
  const state = extractInitialState(html);
  if (state?.page?.chapterListWithVolume) {
    const chapters = [];
    for (const vol of state.page.chapterListWithVolume) {
      // chapterListWithVolume 是分卷数组，每个元素本身是章节对象数组
      if (Array.isArray(vol)) {
        for (const ch of vol) {
          if (ch.itemId && ch.title) {
            chapters.push({
              title: ch.title,
              chapterId: String(ch.itemId),
            });
          }
        }
      } else if (vol && Array.isArray(vol.chapterList)) {
        // 兼容可能的 {chapterList: [...]} 格式
        for (const ch of vol.chapterList) {
          if (ch.itemId && ch.title) {
            chapters.push({
              title: ch.title,
              chapterId: String(ch.itemId),
            });
          }
        }
      }
    }
    if (chapters.length) {
      console.log(`[download] __INITIAL_STATE__ 提取到 ${chapters.length} 章`);
      return chapters;
    }
  }
  if (Array.isArray(state?.page?.chapterList) && state.page.chapterList.length > 0) {
    const chapters = state.page.chapterList.map((ch) => ({
      title: ch.title || '',
      chapterId: String(ch.itemId || ch.chapterId || ''),
    })).filter((ch) => ch.chapterId);
    if (chapters.length) return chapters;
  }

  // 策略2: 正则提取 HTML 中的章节链接
  // 匹配 href="/reader/{id}" 后面的标题文本（可能嵌套在 span 标签中）
  const regex = /href="\/reader\/([^"]+)"[^>]*>(?:<[^>]*>)*([^<]+)/gi;
  const chapters = [];
  let match;
  while ((match = regex.exec(html)) !== null) {
    const title = match[2].trim();
    // 跳过 "最近更新" 等非章节标题
    if (title && !title.startsWith('最近更新')) {
      chapters.push({
        chapterId: match[1],
        title,
      });
    }
  }
  return chapters.length ? chapters : null;
}

/**
 * 清洗 HTML 内容为纯文本
 * 1. 移除 <img> 标签
 * 2. 将 </p><p> 转换为换行
 * 3. 移除所有 HTML 标签
 * 4. 解码 HTML 实体
 */
function stripFanqieHtml(html) {
  if (!html) return '';
  let text = html;
  // 移除图片标签
  text = text.replace(/<img[^>]*>.*?<\/img>/gi, '');
  text = text.replace(/<img[^>]*\/?>/gi, '');
  // </p><p> 或 </p>\n<p> 转换为换行
  text = text.replace(/<\/p>\s*<p[^>]*>/gi, '\n');
  // <br> 转换为换行
  text = text.replace(/<br\s*\/?>/gi, '\n');
  // 移除所有剩余 HTML 标签
  text = text.replace(/<[^>]+>/g, '');
  // 解码常见 HTML 实体
  text = text.replace(/&nbsp;/g, ' ');
  text = text.replace(/&lt;/g, '<');
  text = text.replace(/&gt;/g, '>');
  text = text.replace(/&amp;/g, '&');
  text = text.replace(/&quot;/g, '"');
  text = text.replace(/&#39;/g, "'");
  // 清理多余空行
  text = text.replace(/\n{3,}/g, '\n\n');
  return text.trim();
}

/**
 * 下载番茄单章内容
 * 策略1: API endpoint https://fanqienovel.com/api/reader/full?itemId={chapterId}
 * 策略2: Reader page https://fanqienovel.com/reader/{chapterId} + __INITIAL_STATE__
 * 策略3: Reader page regex extraction
 * @returns {Promise<string|null>} 解密后的章节正文
 */
async function fetchFanqieChapterContent(chapterId) {
  const cookie = `novel_web_id=${generateNovelWebId()}`;
  const headers = {
    ...PC_HEADERS,
    cookie,
    referer: 'https://fanqienovel.com/',
  };

  // 策略1: API endpoint
  try {
    const apiUrl = `https://fanqienovel.com/api/reader/full?itemId=${chapterId}`;
    const apiHtml = await fetchText(apiUrl, headers, 3, 'utf8');
    const apiData = JSON.parse(apiHtml);
    const content = apiData?.data?.chapterData?.content;
    if (content && content.length > 50) {
      // 尝试两种解密模式
      const decoded0 = decodeFanqieText(content, 0);
      const decoded1 = decodeFanqieText(content, 1);
      // 选择解密效果更好的（含更多中文字符）
      const cnCount0 = (decoded0.match(/[\u4e00-\u9fff]/g) || []).length;
      const cnCount1 = (decoded1.match(/[\u4e00-\u9fff]/g) || []).length;
      const decoded = cnCount0 >= cnCount1 ? decoded0 : decoded1;
      return stripFanqieHtml(decoded);
    }
  } catch (e) {
    // 静默失败，继续尝试下一种策略
  }

  // 策略2: Reader page __INITIAL_STATE__
  try {
    const readerUrl = `https://fanqienovel.com/reader/${chapterId}`;
    const readerHtml = await fetchText(readerUrl, headers, 3, 'utf8');
    const state = extractInitialState(readerHtml);
    const content =
      state?.reader?.chapterData?.content ||
      state?.page?.chapterData?.content ||
      null;
    if (content && content.length > 50) {
      const decoded0 = decodeFanqieText(content, 0);
      const decoded1 = decodeFanqieText(content, 1);
      const cnCount0 = (decoded0.match(/[\u4e00-\u9fff]/g) || []).length;
      const cnCount1 = (decoded1.match(/[\u4e00-\u9fff]/g) || []).length;
      const decoded = cnCount0 >= cnCount1 ? decoded0 : decoded1;
      return stripFanqieHtml(decoded);
    }

    // 策略3: Regex extraction from HTML
    const pTagRegex = /<p[^>]*>([^<]+)<\/p>/gi;
    let match;
    const paragraphs = [];
    while ((match = pTagRegex.exec(readerHtml)) !== null) {
      const text = match[1].trim();
      if (text.length > 5) paragraphs.push(text);
    }
    if (paragraphs.length > 0) {
      const rawContent = paragraphs.join('\n');
      const decoded0 = decodeFanqieText(rawContent, 0);
      const decoded1 = decodeFanqieText(rawContent, 1);
      const cnCount0 = (decoded0.match(/[\u4e00-\u9fff]/g) || []).length;
      const cnCount1 = (decoded1.match(/[\u4e00-\u9fff]/g) || []).length;
      const decoded = cnCount0 >= cnCount1 ? decoded0 : decoded1;
      return stripFanqieHtml(decoded);
    }
  } catch (e) {
    // 静默失败
  }

  return null;
}

/**
 * 下载番茄整本小说
 * @param {string} bookId
 * @param {number} maxChapters 最多下载章节数 (0 = 全部)
 * @returns {Promise<{bookName, author, abstract, category, chapters: [{title, content, wordCount}], fullText, totalChapters}>}
 */
async function downloadFanqieBook(bookId, maxChapters = 0) {
  console.log(`[download] 开始下载番茄小说 bookId=${bookId}`);

  // Step 1: 获取书籍详情 + 章节列表
  const detail = await fetchFanqieDetail(bookId);
  if (!detail) throw new Error('无法获取书籍详情');

  const chapterList = await fetchFanqieChapterList(bookId);
  if (!chapterList || chapterList.length === 0) {
    throw new Error('无法获取章节列表');
  }

  console.log(`[download] 《${detail.bookName}》共 ${chapterList.length} 章`);

  // 限制章节数
  const chaptersToDownload = maxChapters > 0
    ? chapterList.slice(0, maxChapters)
    : chapterList;

  console.log(`[download] 将下载 ${chaptersToDownload.length} 章`);

  // Step 2: 并发下载章节内容 (每批 3 个，避免触发风控)
  const chapterContents = await batchRun(
    chaptersToDownload,
    3,
    async (ch) => {
      const content = await fetchFanqieChapterContent(ch.chapterId);
      return {
        title: ch.title,
        content: content || '[本章内容获取失败]',
        wordCount: content ? content.length : 0,
      };
    }
  );

  // 按原始顺序排列 (batchRun 可能打乱顺序)
  const contentMap = {};
  for (const cc of chapterContents) {
    contentMap[cc.title] = cc;
  }
  const orderedChapters = chaptersToDownload
    .map((ch) => contentMap[ch.title] || { title: ch.title, content: '[本章内容获取失败]', wordCount: 0 });

  // Step 3: 拼接全文 (用 === 章节标题 === 分隔)
  const fullText = orderedChapters
    .map((ch) => `=== ${ch.title} ===\n\n${ch.content}`)
    .join('\n\n');

  const totalWords = orderedChapters.reduce((sum, ch) => sum + ch.wordCount, 0);
  const successCount = orderedChapters.filter((ch) => ch.content !== '[本章内容获取失败]').length;

  console.log(`[download] ✓ 《${detail.bookName}》下载完成: ${successCount}/${orderedChapters.length} 章, ${totalWords} 字`);

  return {
    bookName: detail.bookName,
    author: detail.author,
    abstract: detail.abstract || '',
    category: detail.categoryV2 || '',
    chapters: orderedChapters,
    fullText,
    totalChapters: chapterList.length,
    downloadedChapters: orderedChapters.length,
    successCount,
    totalWords,
  };
}

/** 健康检查 */
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'mochao-scraper',
    mode: 'pure-http',
    platforms: Object.keys(PLATFORM_SCRAPERS),
    timestamp: Date.now(),
  });
});

/** 支持的平台列表 */
app.get('/platforms', (_req, res) => {
  // 构建番茄分类榜列表
  const fanqieCats = {};
  for (const ch of ['male', 'female']) {
    const cats = FANQIE_CATEGORIES[ch];
    for (const [catId, catName] of Object.entries(cats)) {
      fanqieCats[`${ch}-${catId}`] = {
        name: catName,
        hot_search: `fanqie/${ch}-${catId}-hot_search`,
        read_rank: `fanqie/${ch}-${catId}-read_rank`,
        new_book: `fanqie/${ch}-${catId}-new_book`,
      };
    }
  }

  res.json({
    platforms: {
      qidian: Object.fromEntries(
        Object.entries(QIDIAN_RANK_URLS).map(([k, v]) => [k, v])
      ),
      fanqie: {
        overall: Object.fromEntries(
          Object.entries(FANQIE_OVERALL_RANK_URLS).map(([k, v]) => [k, v])
        ),
        categories: fanqieCats,
        categoryCount: Object.keys(fanqieCats).length,
      },
      jinjiang: Object.fromEntries(
        Object.entries(JINJIANG_RANK_CONFIG).map(([k, c]) => [k, c.url])
      ),
      zhihu: {
        hot: 'zhihu.com — 知乎盐言无公开排行榜',
      },
      qimao: {
        channels: { boy: '男频', girl: '女频' },
        types: Object.fromEntries(
          Object.entries(QIMAO_RANK_TYPES).map(([k, v]) => [k, v.label])
        ),
        rankTypes: Object.keys(QIMAO_CHANNELS).flatMap(ch =>
          Object.keys(QIMAO_RANK_TYPES).map(t => `${ch}-${t}`)
        ),
      },
      ciweimao: Object.fromEntries(
        Object.entries(CIWEIMAO_RANK_TYPES).map(([k, v]) => [k, v.label])
      ),
    },
  });
});

/** 执行抓取 */
app.post('/scrape', async (req, res) => {
  const { platform, rankType } = req.body;

  if (!platform || !rankType) {
    return res.status(400).json({ error: '缺少参数: platform, rankType' });
  }

  // 番茄分类榜
  if (platform === 'fanqie' && FANQIE_CAT_SCRAPERS[rankType]) {
    try {
      const items = await FANQIE_CAT_SCRAPERS[rankType]();
      return res.json({
        success: true,
        platform,
        rankType,
        count: items.length,
        items,
        scrapedAt: new Date().toISOString(),
      });
    } catch (err) {
      return res.json({
        success: false,
        error: '番茄分类榜抓取失败',
        detail: err.message,
        platform,
        rankType,
        count: 0,
        items: [],
        scrapedAt: new Date().toISOString(),
      });
    }
  }

  const scraper = PLATFORM_SCRAPERS[platform];
  if (!scraper) {
    return res.status(400).json({
      error: `不支持的平台: ${platform}`,
      supported: Object.keys(PLATFORM_SCRAPERS),
    });
  }

  const pLabel = PLATFORM_LABELS[platform] || platform;
  console.log(`[scraper] 收到抓取请求: ${platform}/${rankType}`);

  try {
    const items = await scraper(rankType);

    res.json({
      success: true,
      platform,
      rankType,
      count: items.length,
      items,
      scrapedAt: new Date().toISOString(),
    });
  } catch (err) {
    console.error(`[scraper] ${pLabel} 抓取失败:`, err.message);

    res.json({
      success: false,
      error: `${pLabel}抓取失败，请稍后重试`,
      detail: err.message,
      platform,
      rankType,
      count: 0,
      items: [],
      scrapedAt: new Date().toISOString(),
    });
  }
});

/** 下载整本小说 */
app.post('/download', async (req, res) => {
  const { platform, bookId, maxChapters } = req.body;

  if (!platform || !bookId) {
    return res.status(400).json({ error: '缺少参数: platform, bookId' });
  }

  if (platform !== 'fanqie') {
    return res.status(400).json({
      error: `暂不支持 ${platform} 平台的整本下载`,
      supported: ['fanqie'],
    });
  }

  console.log(`[download] 收到下载请求: ${platform}/${bookId}`);

  try {
    const result = await downloadFanqieBook(bookId, maxChapters || 0);

    res.json({
      success: true,
      platform,
      bookId,
      bookName: result.bookName,
      author: result.author,
      abstract: result.abstract,
      category: result.category,
      chapters: result.chapters,
      fullText: result.fullText,
      totalChapters: result.totalChapters,
      downloadedChapters: result.downloadedChapters,
      successCount: result.successCount,
      totalWords: result.totalWords,
      downloadedAt: new Date().toISOString(),
    });
  } catch (err) {
    console.error(`[download] 下载失败:`, err.message);
    res.json({
      success: false,
      error: '下载失败',
      detail: err.message,
      platform,
      bookId,
      downloadedAt: new Date().toISOString(),
    });
  }
});

// ===========================
// 启动
// ===========================

app.listen(PORT, '127.0.0.1', () => {
  console.log(`[scraper] 抓取服务已启动: http://127.0.0.1:${PORT}`);
  console.log(`[scraper] 模式: 纯 HTTP（SSR HTML / JSON 提取，无浏览器）`);
  console.log(`[scraper] 平台: ${Object.keys(PLATFORM_SCRAPERS).join(', ')}`);
  console.log(`[scraper] 番茄分类榜: ${Object.keys(FANQIE_CAT_SCRAPERS).length} 个`);
  console.log(`[scraper] 整本下载: fanqie`);
  console.log(`[scraper] 健康检查: http://127.0.0.1:${PORT}/health`);
});
