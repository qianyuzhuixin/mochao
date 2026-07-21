/**
 * 番茄小说模块 — 榜单抓取 + 详情页解码 + 搜索
 */

const { fetchText, PC_HEADERS, extractInitialState, batchRun, logTop3 } = require('./utils');

// 分类 id 列表（来自 __INITIAL_STATE__.rank.rankCategoryTypeList）
const FANQIE_CATEGORIES = {
  male: {
    1141: '西方奇幻', 1140: '东方仙侠', 8: '科幻末世',
    261: '都市日常', 124: '都市修真', 1014: '都市高武',
    273: '历史古代', 27: '战神赘婿', 263: '都市种田',
    258: '传统玄幻', 272: '历史脑洞', 539: '悬疑脑洞',
    262: '都市脑洞', 257: '玄幻脑洞', 751: '悬疑灵异',
    504: '抗战谍战', 746: '游戏体育', 718: '动漫衍生',
    1016: '男频衍生',
  },
  female: {
    1139: '古风世情', 8: '科幻末世', 746: '游戏体育',
    1015: '女频衍生', 248: '玄幻言情', 23: '种田',
    79: '年代', 267: '现言脑洞', 246: '宫斗宅斗',
    539: '悬疑脑洞', 253: '古言脑洞', 24: '快穿',
    749: '青春甜宠', 745: '星光璀璨', 747: '女频悬疑',
    750: '职场婚恋', 748: '豪门总裁', 1017: '民国言情',
  },
};

/** 综合榜单（全站不分品类） */
const FANQIE_OVERALL_RANK_URLS = {
  hot_search: 'https://fanqienovel.com/rank/hot_search',
  read_rank: 'https://fanqienovel.com/rank/read_rank',
  new_book: 'https://fanqienovel.com/rank/new_book',
};

const FANQIE_OVERALL_LABELS = {
  hot_search: '热搜榜',
  read_rank: '阅读榜',
  new_book: '新书榜',
};

/**
 * 检测字符串是否包含 PUA 区字符（字体反爬乱码）
 * PUA: U+E000 ~ U+F8FF (Private Use Area)
 */
function hasPuaChars(str) {
  if (!str) return false;
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i);
    if (code >= 0xE000 && code <= 0xF8FF) return true;
  }
  return false;
}

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
      // 列表页的文本字段可能有乱码，仅做初步记录（仅在不含PUA字符时才保留）
      bookNameRaw: hasPuaChars(b.bookName) ? '' : (b.bookName || ''),
      authorRaw: hasPuaChars(b.author) ? '' : (b.author || ''),
    }));
}

/**
 * 通过搜索 API 获取指定书籍的评分
 * 尝试用书名全名搜索，未命中则取书名后 5 个字符作为关键词再试
 * @param {string} bookName
 * @param {string} bookId
 * @returns {Promise<string>} 评分或空字符串
 */
async function fetchFanqieScoreFromSearch(bookName, bookId) {
  try {
    const keywords = [bookName];
    if (bookName.length > 5) {
      keywords.push(bookName.slice(-5));
    }

    for (const q of keywords) {
      const result = await searchFanqieBooks(q, 0, 20);
      const matched = result.books.find((b) => b.bookId === bookId);
      if (matched && matched.score) {
        return matched.score;
      }
    }
  } catch (e) {
    // 搜索失败不影响主流程
  }
  return '';
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
    // 第一个作为主分类，其余作为标签
    let category = '';
    let tags = '';
    try {
      const catArr = typeof page.categoryV2 === 'string'
        ? JSON.parse(page.categoryV2)
        : page.categoryV2;
      if (Array.isArray(catArr) && catArr.length) {
        const names = catArr.map((c) => c.Name).filter(Boolean);
        category = names[0] || '';
        tags = names.slice(1).join('·');
      }
    } catch { }

    const bookName = page.bookName || '';

    // 评分不在详情页，需通过搜索 API 补全
    const score = await fetchFanqieScoreFromSearch(bookName, bookId);

    return {
      bookId,
      bookName,
      author: page.author || '',
      abstract: page.abstract || page.description || '',
      categoryV2: category,
      tags,
      score,
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
  // 关键：详情页失败时不 fallback 到乱码 raw 数据，而是丢弃该条目
  const items = bookIdList
    .map((rank) => {
      const detail = detailMap[rank.bookId] || {};
      const bookName = detail.bookName || rank.bookNameRaw || '';
      const author = detail.author || rank.authorRaw || '';
      return {
        bookName,
        author,
        category: detail.categoryV2 || rank.category || '',
        wordCount: detail.wordNumber || rank.wordNumber,
        hotValue: detail.readCount || rank.read_count,
        intro: (detail.abstract || ''),
        coverUrl: detail.thumbUri || '',
        bookUrl: `https://fanqienovel.com/page/${rank.bookId}`,
        rankNo: rank.currentPos,
        status: detail.creationStatus === 1 ? '连载中' : detail.creationStatus === 2 ? '已完结' : '',
        lastChapter: detail.lastChapterTitle || rank.lastChapterTitle,
      };
    })
    // 过滤掉含 PUA 乱码字符的结果（绝不让乱码数据入库）
    .filter((b) => b.bookName && b.rankNo > 0 && !hasPuaChars(b.bookName) && !hasPuaChars(b.author));

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
      const bookName = detail.bookName || rank.bookNameRaw || '';
      const author = detail.author || rank.authorRaw || '';
      return {
        bookName,
        author,
        category: detail.categoryV2 || catName,
        wordCount: detail.wordNumber || rank.wordNumber,
        hotValue: detail.readCount || rank.read_count,
        intro: (detail.abstract || ''),
        coverUrl: detail.thumbUri || '',
        bookUrl: `https://fanqienovel.com/page/${rank.bookId}`,
        rankNo: rank.currentPos,
        status: detail.creationStatus === 1 ? '连载中' : detail.creationStatus === 2 ? '已完结' : '',
        lastChapter: detail.lastChapterTitle || rank.lastChapterTitle,
      };
    })
    // 过滤掉含 PUA 乱码字符的结果（绝不让乱码数据入库）
    .filter((b) => b.bookName && b.rankNo > 0 && !hasPuaChars(b.bookName) && !hasPuaChars(b.author));

  console.log(`[fanqie]   ✓ ${items.length} 本`);
  return items;
}

// ===========================
// 番茄小说搜索
// ===========================
// 番茄搜索 API（来自 TomatoNovelDownloader Official-API）
// 返回: { code: 0, data: { ret_data: [{ book_id, title, author, abstract, category, creation_status, thumb_url, score }], has_more, offset } }
const SEARCH_API_URL = 'https://novel.snssdk.com/api/novel/channel/homepage/search/search/v1/';

/**
 * 搜索番茄小说（by 书名/作者关键词）
 * 使用 novel.snssdk.com 的新搜索接口，只需 aid + 关键词即可
 * @param {string} keyword
 * @param {number} page  页码（0 起始）
 * @param {number} pageSize  每页条数
 * @returns {Promise<{books: [], total: number, hasMore: boolean}>}
 */
async function searchFanqieBooks(keyword, page = 0, pageSize = 10) {
  const offset = page * pageSize;
  const params = new URLSearchParams({
    device_platform: 'android',
    parent_enterfrom: 'novel_channel_search.tab.',
    offset: String(offset),
    aid: '1967',
    q: keyword,
  });
  const url = SEARCH_API_URL + '?' + params.toString();

  console.log(`[search] 番茄搜索: "${keyword}" page=${page} size=${pageSize}`);

  const searchHeaders = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    'Accept': 'application/json, text/plain, */*',
    'Referer': 'https://fanqienovel.com/',
  };

  const html = await fetchText(url, searchHeaders, 3, 'utf8');

  // 调试日志
  console.log('[search] 响应前200字:', html.substring(0, 200));

  let data;
  try {
    data = JSON.parse(html);
  } catch (e) {
    throw new Error('搜索响应解析失败: ' + html.substring(0, 200));
  }

  console.log('[search] API code:', data.code, 'msg:', data.message || '');

  if (data.code !== 0) {
    throw new Error(`搜索失败: code=${data.code} msg=${data.message || ''}`);
  }

  const retData = data.data?.ret_data || [];
  const hasMore = data.data?.has_more || false;

  console.log(`[search] 结果: ${retData.length}条, hasMore=${hasMore}`);

  const books = retData.map(item => ({
    bookId: String(item.book_id || ''),
    bookName: item.title || '',
    author: item.author || '',
    category: item.category || '',
    wordCount: 0,                               // 此接口不返回字数
    readCount: 0,                               // 此接口不返回阅读量
    coverUrl: item.thumb_url || item.audio_thumb_uri || '',
    status: item.creation_status === '1' ? '连载中' : '',
    abstract: item.abstract || '',
    bookUrl: item.book_id ? `https://fanqienovel.com/page/${item.book_id}` : '',
    score: item.score || '',
  }));

  return { books, total: retData.length, hasMore };
}

module.exports = {
  FANQIE_CATEGORIES,
  FANQIE_OVERALL_RANK_URLS,
  FANQIE_OVERALL_LABELS,
  hasPuaChars,
  scrapeFanqie,
  scrapeFanqieCategory,
  fetchFanqieDetail,
  searchFanqieBooks,
};
