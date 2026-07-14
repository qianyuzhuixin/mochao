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

module.exports = {
  QIDIAN_RANK_URLS,
  QIDIAN_RANK_LABELS,
  scrapeQidian,
};
