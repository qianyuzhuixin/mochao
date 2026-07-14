/**
 * Express 路由定义 + 平台注册表
 */

const express = require('express');

const { QIDIAN_RANK_URLS, scrapeQidian } = require('./qidian');
const {
  FANQIE_CATEGORIES,
  FANQIE_OVERALL_RANK_URLS,
  scrapeFanqie,
  scrapeFanqieCategory,
  searchFanqieBooks,
} = require('./fanqie');
const { JINJIANG_RANK_CONFIG, scrapeJinjiang } = require('./jinjiang');
const { scrapeZhihu } = require('./zhihu');
const { QIMAO_RANK_TYPES, QIMAO_CHANNELS, scrapeQimao } = require('./qimao');
const { CIWEIMAO_RANK_TYPES, scrapeCiweimao } = require('./ciweimao');
const {
  downloadFanqieBook,
  generateTxtContent,
  generateHtmlContent,
  generatePdfContent,
} = require('./download');

const app = express();
app.use(express.json());

// ===========================
// 平台注册表
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
// 路由定义
// ===========================

/** 小说搜索 */
app.post('/search', async (req, res) => {
  const { platform, keyword, page, pageSize } = req.body;

  if (!platform || !keyword) {
    return res.status(400).json({ error: '缺少参数: platform, keyword' });
  }

  const pg = Math.max(0, parseInt(page) || 0);
  const ps = Math.min(50, Math.max(1, parseInt(pageSize) || 20));

  console.log(`[search] 收到搜索请求: ${platform} "${keyword}" page=${pg}`);

  try {
    if (platform === 'fanqie') {
      const result = await searchFanqieBooks(keyword, pg, ps);
      return res.json({
        success: true,
        platform,
        keyword,
        page: pg,
        pageSize: ps,
        books: result.books,
        total: result.total,
        hasMore: result.hasMore,
      });
    }

    return res.status(400).json({
      error: `暂不支持 ${platform} 平台的搜索`,
      supported: ['fanqie'],
    });
  } catch (err) {
    console.error(`[search] API 调用失败（可能被滑块验证码拦截）:`, err.message);
    res.json({
      success: false,
      error: '番茄搜索API需要验证码，请用后端本地搜索',
      detail: err.message,
      platform,
      keyword,
      books: [],
      total: 0,
      hasMore: false,
    });
  }
});

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
      aborted: result.aborted,
      abortReason: result.abortReason,
      failedChapters: result.failedChapters,
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

/**
 * POST /download-file
 * 下载整本小说并返回指定格式的文件
 * Body: { platform, bookId, format: "txt"|"html"|"pdf", maxChapters? }
 */
app.post('/download-file', async (req, res) => {
  const { platform, bookId, format, maxChapters } = req.body;

  if (!platform || !bookId || !format) {
    return res.status(400).json({ error: '缺少参数: platform, bookId, format' });
  }

  if (platform !== 'fanqie') {
    return res.status(400).json({
      error: `暂不支持 ${platform} 平台的整本下载`,
      supported: ['fanqie'],
    });
  }

  if (!['txt', 'html', 'pdf'].includes(format)) {
    return res.status(400).json({
      error: `不支持的格式: ${format}`,
      supported: ['txt', 'html', 'pdf'],
    });
  }

  console.log(`[download-file] ${platform}/${bookId} → ${format} (maxChapters=${maxChapters || 0})`);

  try {
    // 1. 下载小说内容
    const bookData = await downloadFanqieBook(bookId, maxChapters || 0);

    // 2. 按格式生成文件
    let content, contentType, ext;
    switch (format) {
      case 'txt':
        content = generateTxtContent(bookData);
        contentType = 'text/plain; charset=utf-8';
        ext = 'txt';
        break;
      case 'html':
        content = generateHtmlContent(bookData);
        contentType = 'text/html; charset=utf-8';
        ext = 'html';
        break;
      case 'pdf':
        content = await generatePdfContent(bookData);
        contentType = 'application/pdf';
        ext = 'pdf';
        break;
    }

    // 3. 构建文件名
    const rawName = bookData.bookName.replace(/[\\/:*?"<>|]/g, '_');
    const filename = encodeURIComponent(`${rawName} - ${bookData.author}`) + '.' + ext;

    // 4. 返回文件
    res.setHeader('Content-Type', contentType);
    res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${filename}`);
    res.setHeader('Content-Length', Buffer.isBuffer(content) ? content.length : Buffer.byteLength(content, 'utf8'));
    res.send(content);

    console.log(`[download-file] ✓ ${bookData.bookName}.${ext} (${format === 'pdf' ? content.length + ' bytes' : content.length + ' chars'})`);
  } catch (err) {
    console.error(`[download-file] 失败:`, err.message);
    res.status(500).json({
      error: '下载失败',
      detail: err.message,
    });
  }
});

module.exports = {
  app,
  PLATFORM_SCRAPERS,
  FANQIE_CAT_SCRAPERS,
};
