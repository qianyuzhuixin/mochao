/**
 * 起点中文网 — 移动端 SSR 模块
 */

const { fetchText, MOBILE_HEADERS, logTop3 } = require('./utils');

const QIDIAN_RANK_URLS = {
  month_ticket: 'https://m.qidian.com/rank/yuepiao/',
  recommend:    'https://m.qidian.com/rank/rec/',
  collect:      'https://m.qidian.com/rank/newfans/',
  hotsales:     'https://m.qidian.com/rank/hotsales/',
  readindex:    'https://m.qidian.com/rank/readindex/',
  // newsign 已被起点移除（2026-07），不再有独立页面
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
  newsign:      '新人签约新书榜（已下架）',
  signnewbook:  '签约作者新书榜',
  newauthor:    '新人作者新书榜',
  pubnewbook:   '公众作者新书榜',
  sanjiang:     '三江推荐',
};

/** Hub 聚合页 — 部分榜单独立页 404 时的回退数据源 */
const HUB_PAGE_URL = 'https://m.qidian.com/rank/';
const HUB_RANK_KEY_MAP = {
  signnewbook: 'signRank',   // 签约作者新书榜
  newauthor:   'newbRank',   // 新人作者新书榜
  pubnewbook:  'newpRank',   // 公众作者新书榜
};

/** 已被起点下架的榜单类型 */
const DEPRECATED_RANKS = new Set(['newsign']);

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
  // Hub 页 cnt="9.39万字"（字数），独立页 cnt=603（热度值）
  const rawCnt = parseInt(record.cnt);
  const cntIsNumeric = typeof record.cnt === 'number' || /^\d+$/.test(String(record.cnt || ''));
  const hotVal = cntIsNumeric && !isNaN(rawCnt) ? rawCnt : 0;
  return {
    bookName: title,
    author: record.bAuth || record.author || '',
    category: [record.cat, record.subCat].filter(Boolean).join('·'),
    wordCount: parseInt(record.wordCnt) || 0,
    hotValue: hotVal,
    intro: (record.desc || ''),
    coverUrl: '',
    bookUrl: bid ? `https://www.qidian.com/book/${bid}/` : '',
    rankNo: record.rankNum || idx + 1,
  };
}

/**
 * 从 Hub 聚合页提取指定榜单（回退方案，仅 top 5）
 */
async function scrapeQidianHub(rankType) {
  const hubKey = HUB_RANK_KEY_MAP[rankType];
  const label = QIDIAN_RANK_LABELS[rankType] || rankType;

  console.log(`[qidian] → 采集${label}（Hub 聚合页回退）...`);
  const html = await fetchText(HUB_PAGE_URL, MOBILE_HEADERS);
  const pageContext = extractQidianPageContext(html);
  if (!pageContext) throw new Error('Hub 页未找到 vite-plugin-ssr_pageContext');

  const pageData = pageContext?.pageContext?.pageProps?.pageData;
  const records = pageData?.[hubKey] || [];
  if (!records.length) return [];

  const items = records
    .map((r, i) => normalizeQidianBook(r, i))
    .filter((b) => b.bookName);

  console.log(`[qidian]   ✓ ${items.length} 本（Hub 聚合，仅 top 5）`);
  logTop3(items, 'qidian');
  return items;
}

async function scrapeQidian(rankType) {
  // 已下架榜单
  if (DEPRECATED_RANKS.has(rankType)) {
    const label = QIDIAN_RANK_LABELS[rankType] || rankType;
    console.log(`[qidian] → ${label}：起点已下架该榜单，返回空`);
    return [];
  }

  const url = QIDIAN_RANK_URLS[rankType];
  if (!url) throw new Error(`未知榜单: ${rankType}`);

  const label = QIDIAN_RANK_LABELS[rankType] || rankType;
  console.log(`[qidian] → 采集${label}（移动端 SSR）...`);

  // 1. 尝试独立页
  try {
    const html = await fetchText(url, MOBILE_HEADERS);
    const pageContext = extractQidianPageContext(html);
    if (pageContext) {
      const pageData = pageContext?.pageContext?.pageProps?.pageData;
      const records = pageData?.records || [];
      if (records.length) {
        const items = records
          .map((r, i) => normalizeQidianBook(r, i))
          .filter((b) => b.bookName);
        console.log(`[qidian]   ✓ ${items.length} 本`);
        logTop3(items, 'qidian');
        return items;
      }
    }
    // 独立页无数据，尝试 hub 回退
    console.log(`[qidian]   独立页无有效数据，尝试 Hub 回退...`);
  } catch (e) {
    // 独立页请求失败（如 404），尝试 hub 回退
    console.log(`[qidian]   独立页不可用（${e.message}），尝试 Hub 回退...`);
  }

  // 2. Hub 页回退
  if (HUB_RANK_KEY_MAP[rankType]) {
    return scrapeQidianHub(rankType);
  }

  throw new Error(`榜单 ${label} 独立页不可用且无 Hub 回退`);
}

module.exports = {
  QIDIAN_RANK_URLS,
  QIDIAN_RANK_LABELS,
  scrapeQidian,
};
