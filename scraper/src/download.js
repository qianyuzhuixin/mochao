/**
 * 整本下载模块 — 仿造 fanqienovel-downloader 核心机制重构
 * 
 * 核心改进（对标 fanqienovel-downloader 项目）：
 * 1. Cookie 预验证 — 生成 cookie → 测试下载验证章节 → 有效才存入缓存复用
 * 2. Cookie 连续失败自动刷新 — 7 次失败后重新生成并验证 cookie
 * 3. cheerio DOM 提取 — Reader 页面用 cheerio 提取渲染后可见文本，绕过 charset 解密
 * 4. 下载后二次校验修复 — 逐章检查坏章节，重新下载3次
 * 5. 第三方代理 API 最高优先级 — 绕过所有解密问题
 */

const fs = require('fs');
const path = require('path');
const cheerio = require('cheerio');
const { fetchText, httpFetchText, PC_HEADERS, MOBILE_HEADERS, extractInitialState } = require('./utils');
const { fetchFanqieDetail } = require('./fanqie');

/** 加载新版字体解密映射表（PUA Unicode -> 真实字符） */
let FANQIE_FONT_MAP = null;
try {
  const fontMapPath = path.join(__dirname, '..', 'font-map.json');
  FANQIE_FONT_MAP = JSON.parse(fs.readFileSync(fontMapPath, 'utf8'));
  console.log('[scraper] font-map.json 加载成功');
} catch (e) {
  console.warn('[scraper] font-map.json 加载失败，番茄章节内容将无法解密:', e.message);
}

/** 旧版 charset.json 作为兜底（兼容老数据/备用映射） */
let FANQIE_CHARSET = null;
try {
  const charsetPath = path.join(__dirname, '..', 'charset.json');
  FANQIE_CHARSET = JSON.parse(fs.readFileSync(charsetPath, 'utf8'));
} catch (e) {
  // 静默失败，新版映射表已足够
}

/** 番茄字体 PUA 编码范围 */
const FANQIE_CODE_START = 58344;
const FANQIE_CODE_END = 58715;

/** User-Agent 轮换池（PC / 移动 / 微信内嵌浏览器），防风控 */
const UA_POOL = [
  PC_HEADERS,
  MOBILE_HEADERS,
  {
    ...PC_HEADERS,
    'User-Agent':
      'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 ' +
      '(KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36 ' +
      'MicroMessenger/8.0.33.2400(0x28003358) WeChat/arm64',
  },
];

// ===========================
// Cookie 预验证 + 缓存管理
// 仿 fanqienovel-downloader 的 _get_new_cookie 机制
// ===========================

/** Cookie 缓存路径 */
const COOKIE_CACHE_PATH = path.join(__dirname, '..', 'cookie_cache.json');

/** 当前验证过的有效 cookie */
let _validatedCookie = null;

/** 连续失败计数（仿 fanqienovel-downloader 的 tcs） */
let _consecutiveFailures = 0;

/** 连续失败阈值，超过后自动刷新 cookie（仿原项目的 7 次） */
const COOKIE_REFRESH_THRESHOLD = 7;

/**
 * 生成随机 novel_web_id
 * 仿 fanqienovel-downloader 的 bas * 6 ~ bas * 9 范围
 */
function generateNovelWebId() {
  const bas = 1000000000000000000;
  const min = bas * 6;
  const max = bas * 9;
  return String(Math.floor(Math.random() * (max - min + 1)) + min);
}

/**
 * 从缓存加载已验证的 cookie
 * 仿 fanqienovel-downloader 从 cookie.json 读取
 */
function loadCachedCookie() {
  try {
    if (fs.existsSync(COOKIE_CACHE_PATH)) {
      const data = JSON.parse(fs.readFileSync(COOKIE_CACHE_PATH, 'utf8'));
      if (data && data.cookie && data.cookie.includes('novel_web_id')) {
        console.log('[cookie] 从缓存加载已验证 cookie');
        return data.cookie;
      }
    }
  } catch (e) {
    console.warn('[cookie] 缓存读取失败:', e.message);
  }
  return null;
}

/**
 * 保存已验证的 cookie 到缓存
 * 仿 fanqienovel-downloader 写入 cookie.json
 */
function saveCachedCookie(cookie) {
  try {
    fs.writeFileSync(COOKIE_CACHE_PATH, JSON.stringify({ cookie, validatedAt: new Date().toISOString() }), 'utf8');
    console.log('[cookie] 验证 cookie 已缓存');
  } catch (e) {
    console.warn('[cookie] 缓存写入失败:', e.message);
  }
}

/**
 * 获取当前有效 cookie（缓存优先）
 */
function getValidCookie() {
  if (_validatedCookie) return _validatedCookie;
  const cached = loadCachedCookie();
  if (cached) {
    _validatedCookie = cached;
    return cached;
  }
  return null;
}

/**
 * Cookie 预验证 — 生成 cookie → 测试下载验证章节 → 有效才保存
 * 仿 fanqienovel-downloader 的 _get_new_cookie 方法
 * @param {string} testChapterId 测试章节 ID（用于验证 cookie 是否有效）
 * @returns {Promise<string>} 验证成功的 cookie
 */
async function validateNewCookie(testChapterId) {
  console.log('[cookie] 开始生成并验证新 cookie...');

  const bas = 1000000000000000000;
  const maxAttempts = 30; // 最多尝试 30 个随机 cookie

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    // 仿原项目的随机范围 bas*6 ~ bas*9
    const min = bas * 6;
    const max = bas * 9;
    const webId = String(Math.floor(Math.random() * (max - min + 1)) + min);
    const cookie = `novel_web_id=${webId}`;

    // 随机间隔 50-150ms（仿原项目的 time.sleep(random.randint(50, 150) / 1000)）
    await new Promise(r => setTimeout(r, 50 + Math.random() * 100));

    try {
      // 用测试章节验证 cookie
      const headers = { ...PC_HEADERS, cookie, referer: 'https://fanqienovel.com/' };
      const testContent = await _downloadViaReaderPage(testChapterId, headers);

      // 仿原项目：内容 > 200 字才算有效 cookie
      if (testContent && testContent.length > 200) {
        _validatedCookie = cookie;
        _consecutiveFailures = 0;
        saveCachedCookie(cookie);
        console.log(`[cookie] ✓ 验证成功 (测试内容 ${testContent.length}字, attempt=${attempt + 1})`);
        return cookie;
      }
    } catch (e) {
      // 验证失败，继续尝试下一个 cookie
    }
  }

  console.error('[cookie] ✗ 无法找到有效 cookie (尝试了 ' + maxAttempts + ' 个)');
  // 最后兜底：返回一个随机 cookie，虽然没验证过
  const fallbackId = generateNovelWebId();
  _validatedCookie = `novel_web_id=${fallbackId}`;
  return _validatedCookie;
}

/**
 * 记录一次下载失败 — 超过阈值后自动刷新 cookie
 * 仿 fanqienovel-downloader 的 tcs > 7 自动 _get_new_cookie
 * @param {string} testChapterId 用于重新验证的测试章节 ID
 */
async function recordFailureAndMaybeRefreshCookie(testChapterId) {
  _consecutiveFailures++;
  console.warn(`[cookie] 连续失败计数: ${_consecutiveFailures}/${COOKIE_REFRESH_THRESHOLD}`);

  if (_consecutiveFailures >= COOKIE_REFRESH_THRESHOLD) {
    console.log('[cookie] 连续失败超过阈值，刷新 cookie');
    await validateNewCookie(testChapterId);
  }
}

/**
 * 记录一次下载成功 — 重置失败计数
 */
function recordSuccess() {
  _consecutiveFailures = 0;
}

// ===========================
// 番茄字体解密 + HTML清洗
// ===========================

/**
 * 番茄字体解密 — 将 PUA 字符映射回真实字符
 * 优先使用新版 font-map.json（PUA Unicode 十进制 -> 真实字符）
 * 新版映射表覆盖番茄当前字体反爬的 58344~58715 区间
 * @param {string} content 加密内容
 * @returns {string} 解密后内容
 */
function decodeFanqieText(content) {
  if (!FANQIE_FONT_MAP && !FANQIE_CHARSET) return content;
  let result = '';
  for (const char of content) {
    const uni = char.charCodeAt(0);
    if (uni >= FANQIE_CODE_START && uni <= FANQIE_CODE_END) {
      const key = String(uni);
      if (FANQIE_FONT_MAP && FANQIE_FONT_MAP[key]) {
        result += FANQIE_FONT_MAP[key];
        continue;
      }
      // 兜底：旧版 mode0 偏移映射
      if (FANQIE_CHARSET && FANQIE_CHARSET[0]) {
        const bias = uni - FANQIE_CODE_START;
        if (bias < FANQIE_CHARSET[0].length && FANQIE_CHARSET[0][bias] !== '?') {
          result += FANQIE_CHARSET[0][bias];
          continue;
        }
      }
      result += char;
    } else {
      result += char;
    }
  }
  return result;
}

/**
 * 清洗 HTML 内容为纯文本
 */
function stripFanqieHtml(html) {
  if (!html) return '';
  let text = html;
  text = text.replace(/<img[^>]*>.*?<\/img>/gi, '');
  text = text.replace(/<img[^>]*\/?>/gi, '');
  text = text.replace(/<\/p>\s*<p[^>]*>/gi, '\n');
  text = text.replace(/<br\s*\/?>/gi, '\n');
  text = text.replace(/<[^>]+>/g, '');
  text = text.replace(/&nbsp;/g, ' ');
  text = text.replace(/&lt;/g, '<');
  text = text.replace(/&gt;/g, '>');
  text = text.replace(/&amp;/g, '&');
  text = text.replace(/&quot;/g, '"');
  text = text.replace(/&#39;/g, "'");
  text = text.replace(/\n{3,}/g, '\n\n');
  return text.trim();
}

/** 解密并返回清洗后的文本 */
function decodeBest(content) {
  return stripFanqieHtml(decodeFanqieText(content));
}

/** 截断检测：侧重结构性截断指标 */
function isTruncated(content) {
  if (!content) return true;
  if (content.length < 50) return true;
  const tail = content.slice(-30);
  if (!/[。！？…」』"]/.test(tail)) return true;
  const openSingle = (content.match(/「/g) || []).length;
  const closeSingle = (content.match(/」/g) || []).length;
  if (openSingle !== closeSingle) return true;
  const openDouble = (content.match(/『/g) || []).length;
  const closeDouble = (content.match(/』/g) || []).length;
  if (openDouble !== closeDouble) return true;
  return false;
}

/**
 * 章节内容有效性检查（仿 fanqienovel-downloader 的 check_chapter_content）
 * 内容 < 100字 → 无效；包含错误标记 → 无效
 */
function isChapterContentValid(content) {
  if (!content) return false;
  if (content.length < 100) return false;
  const errorMarkers = ['下载失败', '获取失败', '请求失败', '访问太频繁', '本章内容获取失败'];
  return !errorMarkers.some(marker => content.includes(marker));
}

// ===========================
// 核心下载策略（仿 fanqienovel-downloader 重构）
// ===========================

/**
 * 策略A: cheerio DOM 提取 Reader 页面渲染文本
 * 仿 fanqienovel-downloader 的 XPath 策略
 * 用 cheerio 替代 Python lxml 的 XPath，提取浏览器渲染后的可见文本
 * 这是最稳定的策略——即使 charset 解密有问题，只要页面能渲染就能拿到文本
 * 
 * @param {string} chapterId
 * @param {object} headers 包含验证过的 cookie
 * @returns {Promise<string|null>}
 */
async function _downloadViaReaderPage(chapterId, headers) {
  try {
    const readerUrl = `https://fanqienovel.com/reader/${chapterId}`;
    const readerHtml = await fetchText(readerUrl, headers, 3, 'utf8');

    // 仿 fanqienovel-downloader 的 XPath:
    // '//div[@class="muye-reader-content noselect"]//p/text()'
    // 用 cheerio 等价实现
    const $ = cheerio.load(readerHtml);
    const paragraphs = [];

    // 选择器: muye-reader-content 区域内的 <p> 标签文本
    // 番茄 Reader 页面的正文 DOM 结构
    $('div.muye-reader-content p').each(function () {
      const text = $(this).text().trim();
      if (text.length > 0) paragraphs.push(text);
    });

    // 兜底选择器：如果上面的选择器没命中，尝试更宽泛的选择器
    if (paragraphs.length === 0) {
      $('div.reader-content p, div.chapter-content p, div.content p').each(function () {
        const text = $(this).text().trim();
        if (text.length > 0) paragraphs.push(text);
      });
    }

    if (paragraphs.length > 3) { // 至少有几个段落才认为是有效内容
      const content = paragraphs.join('\n');
      // Reader 页面直接返回的文本仍可能是 PUA 编码，需要解密
      return decodeFanqieText(content);
    }
  } catch (e) {
    // 静默失败
  }
  return null;
}

/**
 * 策略B: __INITIAL_STATE__ + font-map 解密
 * 从 Reader 页面的 __INITIAL_STATE__ 提取加密内容，用 charset.json 解密
 * 
 * @param {string} chapterId
 * @param {object} headers
 * @returns {Promise<string|null>}
 */
async function _downloadViaInitialState(chapterId, headers) {
  try {
    const readerUrl = `https://fanqienovel.com/reader/${chapterId}`;
    const readerHtml = await fetchText(readerUrl, headers, 3, 'utf8');
    const state = extractInitialState(readerHtml);
    const content =
      state?.reader?.chapterData?.content ||
      state?.page?.chapterData?.content ||
      null;

    if (content && content.length > 50) {
      const text = decodeBest(content);
      if (text.length > 50) return text;
    }
  } catch (e) {
    // 静默失败
  }
  return null;
}

/**
 * 策略C: 官方 API endpoint + font-map 解密
 *
 * @param {string} chapterId
 * @param {object} headers
 * @returns {Promise<string|null>}
 */
async function _downloadViaApi(chapterId, headers) {
  try {
    const apiUrl = `https://fanqienovel.com/api/reader/full?itemId=${chapterId}`;
    const apiHtml = await fetchText(apiUrl, headers, 3, 'utf8');

    // JSON 截断容错
    if (!apiHtml || !apiHtml.trim().startsWith('{') || !apiHtml.trim().endsWith('}')) {
      return null;
    }

    const apiData = JSON.parse(apiHtml);
    const content = apiData?.data?.chapterData?.content;
    if (content && content.length > 50) {
      const text = decodeBest(content);
      if (text.length > 50) return text;
    }
  } catch (e) {
    // 静默失败
  }
  return null;
}

/**
 * 策略D: 第三方代理 API（仿 fanqienovel-downloader 的 ref_main.py down_text）
 * 
 * Python 项目的核心策略 — 代理直接返回已解码内容，无需 charset 解密：
 *   api_url = f"http://yuefanqie.jingluo.love/content?item_id={it}"
 *   response = network_manager.make_request(api_url)
 *   data = response.json()
 *   if data.get("code") == 0:
 *     content = data.get("data", {}).get("content", "")
 *     content = re.sub(r'<header>.*?</header>', '', content)
 *     content = re.sub(r'<footer>.*?</footer>', '', content)
 *     content = re.sub(r'</?article>', '', content)
 *     content = re.sub(r'<p idx="\d+">', '\n', content)
 *     content = re.sub(r'</p>', '\n', content)
 *     content = re.sub(r'<[^>]+>', '', content)
 *     content = re.sub(r'\n{3,}', '\n\n', content).strip()
 * 
 * 之前的 bug：fetchText 只支持 HTTPS → 代理 URL 是 HTTP → 请求直接失败
 *              代理返回 JSON 格式 → 我们把 JSON 当 HTML 用 cheerio 解析 → 内容提取失败
 * 
 * @param {string} chapterId
 * @param {number} maxRetries 最大重试次数
 * @returns {Promise<string|null>} 已解码的纯文本内容
 */
async function _downloadViaProxy(chapterId, maxRetries = 3) {
  for (let retry = 0; retry < maxRetries; retry++) {
    try {
      const proxyUrl = `http://yuefanqie.jingluo.love/content?item_id=${chapterId}`;
      // 使用 httpFetchText 支持 HTTP 协议（之前用 fetchText/https 导致直接失败）
      const rawResponse = await httpFetchText(proxyUrl, { 'User-Agent': 'Mozilla/5.0' }, 10000);

      // 代理返回 JSON 格式: {"code": 0, "data": {"content": "<p>...</p>"}}
      // 之前的 bug：把 JSON 字符串当 HTML 用 cheerio 解析 → 完全无效
      const data = JSON.parse(rawResponse);

      if (data.code === 0 && data.data && data.data.content) {
        let content = data.data.content;

        // 清洗 HTML → 完全仿照 Python 项目的正则清洗逻辑
        content = content.replace(/<header>[\s\S]*?<\/header>/gi, '');
        content = content.replace(/<footer>[\s\S]*?<\/footer>/gi, '');
        content = content.replace(/<\/?article>/gi, '');
        content = content.replace(/<p\s+idx="\d+">/gi, '\n');
        content = content.replace(/<\/p>/gi, '\n');
        content = content.replace(/<[^>]+>/g, '');
        content = content.replace(/\n{3,}/g, '\n\n');
        content = content.trim();

        if (content.length > 50) {
          return content;
        }
      }
    } catch (e) {
      console.warn(`[proxy] 章节 ${chapterId} 代理API第${retry + 1}次失败: ${e.message}`);
      if (retry < maxRetries - 1) {
        // 仿 Python 项目: time.sleep(1 * retry_count)
        await new Promise(r => setTimeout(r, 1000 * (retry + 1)));
      }
    }
  }
  return null;
}

/**
 * 下载番茄单章内容（仿 fanqienovel-downloader 核心架构）
 * 
 * 核心策略优先级（仿 Python 项目 down_text）：
 * - 代理API（最高优先级） → 直接返回已解码内容，无需 charset 解密
 * - 只有代理完全失败才回退到 charset 策略
 * 
 * 之前的致命 bug：
 * 1. fetchText 只支持 HTTPS → 代理 URL 是 HTTP → 请求直接失败
 * 2. 代理返回 JSON 格式 → 我们把 JSON 当 HTML 用 cheerio 解析 → 内容提取失败
 * 
 * 结果：唯一能绕过 charset 解密的策略一直没生效，所有内容都走的 charset 解密
 *       → charset 解密不可靠 → 章节不全 + 获取失败
 * 
 * @param {string} chapterId
 * @param {string} testChapterId 用于 cookie 刷新验证的测试章节 ID
 * @returns {Promise<string|null>}
 */
async function fetchFanqieChapterContent(chapterId, testChapterId) {
  // --- 跳过代理API：直走 charset 策略 ---
  // 代理API (yuefanqie.jingluo.love) 不稳定频繁超时，charset cheerio DOM 提取已足够可靠
  // 如需重新启用代理API，取消下面注释即可：
  // console.log(`[download] 章节 ${chapterId} → 尝试代理API`);
  // const proxyContent = await _downloadViaProxy(chapterId, 3);
  // if (proxyContent) {
  //   console.log(`[download] 章节 ${chapterId} ✓ 代理API成功 (${proxyContent.length}字)`);
  //   recordSuccess();
  //   return proxyContent;
  // }
  // console.warn(`[download] 章节 ${chapterId} 代理API失败，回退到 charset 策略`);
  let bestContent = null;

  // 获取验证过的 cookie
  let cookie = getValidCookie();
  if (!cookie) {
    if (!testChapterId) {
      testChapterId = '7143038691944959011';
    }
    cookie = await validateNewCookie(testChapterId);
  }

  for (let retry = 0; retry < 3; retry++) {
    const uaIndex = retry % UA_POOL.length;
    const baseHeaders = UA_POOL[uaIndex];
    const uaLabel = ['PC', 'Mobile', 'WeChat'][uaIndex];
    const headers = {
      ...baseHeaders,
      cookie,
      referer: 'https://fanqienovel.com/',
    };

    console.log(`[download] 章节 ${chapterId} charset回退 (UA=${uaLabel}, attempt=${retry + 1})`);

    // --- 全部策略并行收集，取最长内容 ---
    // 之前的 bug：cheerio 拿到几百字就 return，charset 解码根本没机会跑
    // 新策略：所有策略结果都收集，最后取最优（最长且通过校验的）

    // 策略1: cheerio DOM 提取 Reader 页面
    const domContent = await _downloadViaReaderPage(chapterId, headers);
    if (domContent && domContent.length > 50) {
      const tag = domContent.length < 500 ? '⚠ 偏短' : '✓';
      console.log(`[download] 章节 ${chapterId} cheerio ${tag} (${domContent.length}字)`);
      if (!bestContent || domContent.length > bestContent.length) {
        bestContent = domContent;
      }
      // 内容足够长且通过校验 → 直接返回（不必等 charset）
      if (domContent.length >= 500 && !isTruncated(domContent) && isChapterContentValid(domContent)) {
        recordSuccess();
        return domContent;
      }
    }

    // 策略2: API endpoint + charset 解密
    const apiContent = await _downloadViaApi(chapterId, headers);
    if (apiContent && apiContent.length > 50) {
      console.log(`[download] 章节 ${chapterId} API+charset (${apiContent.length}字)`);
      if (!bestContent || apiContent.length > bestContent.length) {
        bestContent = apiContent;
      }
      if (apiContent.length >= 500 && !isTruncated(apiContent) && isChapterContentValid(apiContent)) {
        recordSuccess();
        return apiContent;
      }
    }

    // 策略3: __INITIAL_STATE__ + charset 解密
    const stateContent = await _downloadViaInitialState(chapterId, headers);
    if (stateContent && stateContent.length > 50) {
      console.log(`[download] 章节 ${chapterId} INITIAL_STATE+charset (${stateContent.length}字)`);
      if (!bestContent || stateContent.length > bestContent.length) {
        bestContent = stateContent;
      }
      if (stateContent.length >= 500 && !isTruncated(stateContent) && isChapterContentValid(stateContent)) {
        recordSuccess();
        return stateContent;
      }
    }

    // 三个策略都跑完了 → 检查 bestContent 是否足够好
    if (bestContent && bestContent.length >= 500 && !isTruncated(bestContent) && isChapterContentValid(bestContent)) {
      console.log(`[download] 章节 ${chapterId} ✓ 取最优策略结果 (${bestContent.length}字)`);
      recordSuccess();
      return bestContent;
    }

    // 首轮所有策略都试过，偏短则直接放弃重试（不同 UA 拿到的内容几乎一样，重试无意义）
    if (retry === 0 && bestContent && bestContent.length < 500) {
      console.warn(`[download] 章节 ${chapterId} ⚠ 内容偏短 (${bestContent.length}字)，跳过剩余重试直接返回`);
      break;
    }

    // 本轮没拿到满意结果 → 记录失败，可能刷新 cookie 后重试
    await recordFailureAndMaybeRefreshCookie(testChapterId);
    cookie = getValidCookie();

    if (retry < 2) {
      const delay = 1000 * Math.pow(2, retry);
      console.warn(`[download] 章节 ${chapterId} 本轮不满意 (best=${(bestContent || '').length}字)，${delay}ms后重试`);
      await new Promise(r => setTimeout(r, delay));
    }
  }

  // 所有重试结束：返回最佳已有内容（即使不完美）
  if (bestContent) {
    console.warn(`[download] 章节 ${chapterId} ⚠ 返回已有最佳内容 (${bestContent.length}字，可能不完整)`);
    return bestContent;
  }
  console.error(`[download] 章节 ${chapterId} ✗ 完全失败，无可用内容`);
  return null;
}

// ===========================
// 章节列表提取
// ===========================

/**
 * 从番茄书籍页面提取章节列表
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

  // 策略2: 正则提取
  const regex = /href="\/reader\/([^"]+)"[^>]*>(?:<[^>]*>)*([^<]+)/gi;
  const chapters = [];
  let match;
  while ((match = regex.exec(html)) !== null) {
    const title = match[2].trim();
    if (title && !title.startsWith('最近更新')) {
      chapters.push({ chapterId: match[1], title });
    }
  }
  return chapters.length ? chapters : null;
}

// ===========================
// 下载后二次校验修复
// 仿 fanqienovel-downloader 的 verify_and_fix_chapters
// ===========================

/**
 * 下载后二次校验：逐章检查坏章节，重新下载 3 次
 * 仿 fanqienovel-downloader 的 verify_and_fix_chapters 函数
 * 
 * @param {Array} chapters 已下载的章节列表 [{chapterId, title, content, wordCount}]
 * @param {string} testChapterId 用于 cookie 验证的测试章节 ID
 * @returns {Promise<{chapters: Array, failedCount: number}>}
 */
async function verifyAndFixChapters(chapters, testChapterId) {
  console.log('[verify] 开始校验章节完整性...');
  const failedChapters = [];

  // 检查每个章节（仿 check_chapter_content）
  for (let i = 0; i < chapters.length; i++) {
    const ch = chapters[i];
    // 第0章（书籍信息）不需要校验
    if (ch.title === '书籍信息') continue;

    if (!isChapterContentValid(ch.content)) {
      console.warn(`[verify] 发现问题章节: 第${i}章「${ch.title}」(${(ch.content || '').length}字)`);
      failedChapters.push(i);
    }
  }

  if (failedChapters.length === 0) {
    console.log('[verify] ✓ 所有章节内容完整');
    return { chapters, failedCount: 0 };
  }

  console.log(`[verify] 发现 ${failedChapters.length} 个问题章节，开始修复`);

  // 重新下载坏章节（仿原项目的 max_retries = 3）
  for (let retry = 1; retry <= 3; retry++) {
    console.log(`[verify] 第 ${retry} 次修复尝试`);
    const stillFailed = [];

    for (const idx of failedChapters) {
      const ch = chapters[idx];
      console.log(`[verify] 重新下载: 「${ch.title}」(${ch.chapterId})`);

      try {
        // 修复下载时加更长间隔（1-3秒），仿原项目
        await new Promise(r => setTimeout(r, 1000 + Math.random() * 2000));
        const newContent = await fetchFanqieChapterContent(ch.chapterId, testChapterId);

        if (newContent && isChapterContentValid(newContent)) {
          chapters[idx] = {
            ...ch,
            content: newContent,
            wordCount: newContent.length,
          };
          console.log(`[verify] ✓ 修复成功: 「${ch.title}」(${newContent.length}字)`);
        } else {
          stillFailed.push(idx);
          console.warn(`[verify] ✗ 修复失败: 「${ch.title}」`);
        }
      } catch (e) {
        stillFailed.push(idx);
        console.warn(`[verify] ✗ 修复异常: 「${ch.title}」 - ${e.message}`);
      }
    }

    failedChapters = stillFailed;
    if (failedChapters.length === 0) break;
  }

  // 最终仍未修复的章节保留为 bestContent 或标记失败
  for (const idx of failedChapters) {
    const ch = chapters[idx];
    console.error(`[verify] ✗ 最终未能修复: 「${ch.title}」(${ch.chapterId})`);
    // 保留原内容（哪怕是截断的），比完全丢失好
    if (!ch.content || ch.content.length < 10) {
      chapters[idx] = {
        ...ch,
        content: `[本章内容获取失败，请稍后重试]`,
        wordCount: 0,
      };
    }
  }

  console.log(`[verify] 校验完成: ${failedChapters.length} 章仍有问题`);
  return { chapters, failedCount: failedChapters.length };
}

// ===========================
// 书籍信息第0章 + 整本下载
// ===========================

/**
 * 构建书籍信息第0章
 */
function buildBookInfoChapter(detail, totalChapters) {
  const lines = [];
  lines.push(`书名：${detail.bookName || '未知'}`);
  lines.push(`作者：${detail.author || '未知'}`);
  lines.push(`book_id=${detail.bookId || ''}`);
  const statusLabel = detail.creationStatus === 1 ? '已完结' : '连载中';
  lines.push(`状态：${statusLabel}`);
  lines.push(`评分：${detail.score || ''}`);
  lines.push(`字数：${detail.wordNumber || 0}`);
  lines.push(`章节：${totalChapters}`);
  lines.push(`分类：${detail.categoryV2 || ''}`);
  lines.push(`标签：${detail.tags || ''}`);
  lines.push(`在读：${detail.readCount || 0}`);
  lines.push(`简介：${detail.abstract || ''}`);

  const content = lines.join('\n');
  return { title: '书籍信息', content, wordCount: content.length };
}

/**
 * 下载番茄整本小说（仿 fanqienovel-downloader 核心架构）
 * 
 * 改进点：
 * - Cookie 预验证（复用缓存或动态验证）
 * - 策略优先级：代理API → cheerio DOM → API → __INITIAL_STATE__
 * - 连续失败自动刷新 cookie
 * - 下载后二次校验修复
 * - 单章彻底失败即中止
 * - 第0章为书籍元数据
 * 
 * @param {string} bookId
 * @param {number} maxChapters 最多下载章节数 (0 = 全部)
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

  // 构建第0章：书籍元数据
  const bookInfoChapter = buildBookInfoChapter(detail, chapterList.length);

  // 选择测试章节 ID（用于 cookie 验证）
  // 仿原项目：从章节列表中取一个章节 ID 作为测试
  const testChapterId = chapterList.length > 0 ? chapterList[0].chapterId : '7143038691944959011';

  // 限制章节数
  const chaptersToDownload = maxChapters > 0
    ? chapterList.slice(0, maxChapters)
    : chapterList;

  console.log(`[download] 将下载 ${chaptersToDownload.length} 章（+ 第0章书籍信息）`);

  // Step 2: 多线程并发下载（可配置并发数）
  const CONCURRENCY = 3; // 同时下载章节数
  const succeededChapters = [];
  let abortReason = null;
  let globalAbort = false; // 全局中止标志，任意线程检测到彻底失败后通知所有线程停止

  // 将章节分批并发下载
  for (let batchStart = 0; batchStart < chaptersToDownload.length; batchStart += CONCURRENCY) {
    const batch = chaptersToDownload.slice(batchStart, batchStart + CONCURRENCY);
    const batchPromises = batch.map((ch) => {
      const idx = chaptersToDownload.indexOf(ch);
      // 如果已全局中止，直接跳过
      if (globalAbort) return Promise.resolve(null);

      console.log(`[download] 下载第 ${idx + 1}/${chaptersToDownload.length} 章: ${ch.title} (${ch.chapterId})`);

      return fetchFanqieChapterContent(ch.chapterId, testChapterId)
        .then((content) => {
          if (globalAbort) return null;
          if (content === null) {
            console.error(`[download] 章节 ${ch.chapterId} (${ch.title}) 完全失败，触发全局中止`);
            globalAbort = true;
            abortReason = `第${idx + 1}章「${ch.title}」获取失败，已中止下载`;
            return null;
          }
          // 内容偏短（<500字）视为实质失败，触发全局中止
          if (content.length < 500) {
            console.error(`[download] 章节 ${ch.chapterId} (${ch.title}) 内容偏短 (${content.length}字)，触发全局中止`);
            globalAbort = true;
            abortReason = `第${idx + 1}章「${ch.title}」内容不完整 (${content.length}字)，已中止下载`;
            return null;
          }
          return { chapterId: ch.chapterId, title: ch.title, content, wordCount: content.length, idx };
        })
        .catch((e) => {
          console.warn(`[download] 章节 ${ch.chapterId} (${ch.title}) 下载异常: ${e.message}`);
          return null;
        });
    });

    // 等待当前批次全部完成
    const batchResults = await Promise.all(batchPromises);

    // 检查是否有触发全局中止的
    if (globalAbort) {
      // 收集本批次已完成的结果（abort 之前的）
      for (const r of batchResults) {
        if (r && r.content) {
          succeededChapters.push({
            chapterId: r.chapterId,
            title: r.title,
            content: r.content,
            wordCount: r.wordCount,
          });
        }
      }
      break;
    }

    // 按下载顺序整理结果
    const sorted = batchResults.filter(Boolean).sort((a, b) => a.idx - b.idx);
    for (const r of sorted) {
      succeededChapters.push({
        chapterId: r.chapterId,
        title: r.title,
        content: r.content,
        wordCount: r.wordCount,
      });
    }

    // 批次间加间隔（防风控）
    if (batchStart + CONCURRENCY < chaptersToDownload.length) {
      await new Promise(r => setTimeout(r, 300 + Math.random() * 400));
    }
  }

  // 第0章（书籍信息） + 成功下载的正文章节
  const orderedChapters = [bookInfoChapter, ...succeededChapters];

  // Step 3: 下载后二次校验修复（仿 fanqienovel-downloader 的 verify_and_fix_chapters）
  console.log('[download] 开始二次校验修复...');
  const verifyResult = await verifyAndFixChapters(orderedChapters, testChapterId);
  const finalChapters = verifyResult.chapters;

  // Step 4: 拼接全文 (用 === 章节标题 === 分隔)
  const fullText = finalChapters
    .map((ch) => `=== ${ch.title} ===\n\n${ch.content}`)
    .join('\n\n');

  const totalWords = finalChapters.reduce((sum, ch) => sum + ch.wordCount, 0);
  const successCount = finalChapters.filter(ch => ch.title !== '书籍信息' && isChapterContentValid(ch.content)).length;

  if (abortReason) {
    console.warn(`[download] ⚠ 《${detail.bookName}》下载中止: ${successCount}/${chaptersToDownload.length} 章, ${totalWords} 字 — ${abortReason}`);
  } else {
    console.log(`[download] ✓ 《${detail.bookName}》下载完成: ${successCount}/${chaptersToDownload.length} 章, ${totalWords} 字 (校验修复 ${verifyResult.failedCount} 章仍有问题)`);
  }

  return {
    bookName: detail.bookName,
    author: detail.author,
    abstract: detail.abstract || '',
    category: detail.categoryV2 || '',
    chapters: finalChapters,
    fullText,
    totalChapters: chapterList.length,
    downloadedChapters: finalChapters.length,
    successCount,
    totalWords,
    aborted: !!abortReason,
    abortReason: abortReason || '',
    failedChapters: verifyResult.failedCount,
  };
}

// ===========================
// 格式转换 — 生成 TXT / HTML / PDF 文件
// ===========================

/** HTML 转义 */
function escHtml(s) {
  if (!s) return '';
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/**
 * 生成 TXT 格式全文
 */
function generateTxtContent(bookData) {
  const lines = [];
  lines.push(`《${bookData.bookName}》`);
  lines.push(`作者：${bookData.author}`);
  if (bookData.category) lines.push(`分类：${bookData.category}`);
  if (bookData.abstract) lines.push(`简介：${bookData.abstract}`);
  lines.push(`总章节：${bookData.downloadedChapters || bookData.chapters.length} 章`);
  lines.push(`总字数：${(bookData.totalWords || 0).toLocaleString()} 字`);
  lines.push('');
  lines.push('='.repeat(60));
  lines.push('');

  const indent = '　　';
  for (const ch of bookData.chapters) {
    lines.push(`=== ${ch.title} ===`);
    lines.push('');
    const paragraphs = (ch.content || '').split('\n').filter(p => p.trim());
    for (const p of paragraphs) {
      lines.push(indent + p.trim());
    }
    lines.push('');
    lines.push('-'.repeat(40));
    lines.push('');
  }

  lines.push('');
  lines.push('='.repeat(60));
  lines.push(`本书由 MoChao 素材助手下载`);
  lines.push(`来源：番茄小说 fanqienovel.com`);
  lines.push(`下载时间：${new Date().toLocaleString('zh-CN')}`);
  lines.push('仅供个人学习参考，请于下载后24小时内删除');

  return lines.join('\n');
}

/**
 * 生成 HTML 格式全文
 */
function generateHtmlContent(bookData) {
  const now = new Date().toLocaleString('zh-CN');
  const tocItems = bookData.chapters.map((ch, i) =>
    `<li><a href="#ch${i}">${escHtml(ch.title)}</a></li>`
  ).join('\n');

  const chapterHtml = bookData.chapters.map((ch, i) => {
    const paragraphs = (ch.content || '').split('\n').filter(p => p.trim());
    const body = paragraphs.map(p => `<p>${escHtml(p.trim())}</p>`).join('\n');
    return `
    <section class="chapter" id="ch${i}">
      <h2>${escHtml(ch.title)}</h2>
      <div class="chapter-body">${body}</div>
      <div class="chapter-nav">
        ${i > 0 ? `<a href="#ch${i - 1}">← 上一章</a>` : '<span></span>'}
        <a href="#toc">回到目录</a>
        ${i < bookData.chapters.length - 1 ? `<a href="#ch${i + 1}">下一章 →</a>` : '<span></span>'}
      </div>
    </section>`;
  }).join('\n');

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>《${escHtml(bookData.bookName)}》- ${escHtml(bookData.author)}</title>
<style>
  :root {
    --bg: #fdf6ec;
    --card-bg: #fffef9;
    --text: #2c2416;
    --text-secondary: #6b5e4a;
    --border: #e0d5c1;
    --accent: #8b6914;
    --link: #5a3e0b;
    --heading: #3d2911;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #1a1814;
      --card-bg: #24211c;
      --text: #e8ddcb;
      --text-secondary: #a89880;
      --border: #3d3629;
      --accent: #c9a74b;
      --link: #d4b76a;
      --heading: #e8d5a8;
    }
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: "PingFang SC", "Noto Serif CJK SC", "Source Han Serif SC", "SimSun", "STSong", serif;
    background: var(--bg);
    color: var(--text);
    line-height: 1.8;
    max-width: 800px;
    margin: 0 auto;
    padding: 2rem 1.5rem 4rem;
  }
  .book-header { text-align: center; padding: 2rem 0 1.5rem; border-bottom: 2px solid var(--accent); margin-bottom: 1.5rem; }
  .book-header h1 { font-size: 1.8rem; color: var(--heading); margin-bottom: 0.4rem; }
  .book-header .author { font-size: 1rem; color: var(--text-secondary); }
  .book-meta { display: flex; justify-content: center; gap: 1.5rem; flex-wrap: wrap; font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.6rem; }
  .book-abstract { background: var(--card-bg); border-left: 3px solid var(--accent); padding: 0.8rem 1rem; margin: 1rem 0; font-size: 0.9rem; border-radius: 0 6px 6px 0; }
  #toc { background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 1.2rem 1.5rem; margin-bottom: 2rem; }
  #toc h2 { font-size: 1.2rem; color: var(--accent); margin-bottom: 0.8rem; text-align: center; }
  #toc ol { columns: 2; column-gap: 2rem; padding-left: 1.5rem; font-size: 0.9rem; }
  #toc li { margin-bottom: 0.2rem; break-inside: avoid; }
  #toc a { color: var(--link); text-decoration: none; }
  #toc a:hover { text-decoration: underline; color: var(--accent); }
  .chapter { background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 1.5rem; margin-bottom: 1.5rem; }
  .chapter h2 { font-size: 1.2rem; color: var(--heading); border-bottom: 1px solid var(--border); padding-bottom: 0.5rem; margin-bottom: 1rem; }
  .chapter-body p { text-indent: 2em; margin-bottom: 0.6rem; text-align: justify; }
  .chapter-nav { display: flex; justify-content: space-between; margin-top: 1.2rem; padding-top: 0.8rem; border-top: 1px dashed var(--border); font-size: 0.85rem; }
  .chapter-nav a { color: var(--link); text-decoration: none; }
  .chapter-nav a:hover { color: var(--accent); }
  .book-footer { text-align: center; padding: 1.5rem 0; border-top: 1px solid var(--border); margin-top: 2rem; font-size: 0.8rem; color: var(--text-secondary); line-height: 1.8; }
  @media (max-width: 600px) { body { padding: 1rem 0.8rem 3rem; } #toc ol { columns: 1; } }
  @media print {
    :root { --bg: #fff; --card-bg: #fff; --text: #000; --text-secondary: #444; --border: #ccc; --accent: #333; --link: #000; --heading: #000; }
    body { max-width: none; padding: 1cm; }
    .chapter-nav, #toc { break-inside: avoid; }
    .chapter { break-inside: avoid; border: none; box-shadow: none; }
  }
</style>
</head>
<body>
<header class="book-header">
  <h1>《${escHtml(bookData.bookName)}》</h1>
  <div class="author">作者：${escHtml(bookData.author)}</div>
  <div class="book-meta">
    ${bookData.category ? `<span>分类：${escHtml(bookData.category)}</span>` : ''}
    <span>共 ${bookData.downloadedChapters || bookData.chapters.length} 章</span>
    <span>${(bookData.totalWords || 0).toLocaleString()} 字</span>
  </div>
  ${bookData.abstract ? `<div class="book-abstract">${escHtml(bookData.abstract)}</div>` : ''}
</header>
<nav id="toc"><h2>📑 目录</h2><ol>${tocItems}</ol></nav>
<main>${chapterHtml}</main>
<footer class="book-footer">
  <p>本书由 <strong>MoChao 素材助手</strong> 生成</p>
  <p>来源：番茄小说 · fanqienovel.com</p>
  <p>下载时间：${now}</p>
  <p style="font-size:0.75rem;color:#999;">仅供个人学习参考，请于下载后24小时内删除</p>
</footer>
</body>
</html>`;
}

/**
 * 生成 PDF（使用 pdfkit）
 */
async function generatePdfContent(bookData) {
  const PDFDocument = require('pdfkit');

  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({
      size: 'A4',
      margins: { top: 60, bottom: 60, left: 72, right: 72 },
      bufferPages: true,
      info: {
        Title: bookData.bookName,
        Author: bookData.author,
        Subject: bookData.category || '',
        Creator: 'MoChao Scraper',
      },
    });

    const chunks = [];
    doc.on('data', (chunk) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);

    let chineseFont = null;
    const fontPaths = [
      'C:/Windows/Fonts/simsun.ttc',
      'C:/Windows/Fonts/msyh.ttc',
      'C:/Windows/Fonts/simhei.ttf',
      'C:/Windows/Fonts/STSONG.TTF',
      'C:/Windows/Fonts/STKAITI.TTF',
    ];

    for (const fp of fontPaths) {
      try {
        if (fs.existsSync(fp)) {
          chineseFont = fp;
          break;
        }
      } catch {}
    }

    if (chineseFont) doc.registerFont('CJK', chineseFont);

    const useFont = (doc, size) => {
      if (chineseFont) doc.font('CJK').fontSize(size);
      else doc.font('Helvetica').fontSize(size);
    };

    useFont(doc, 22);
    doc.text(`《${bookData.bookName}》`, { align: 'center' });
    doc.moveDown(1);
    useFont(doc, 14);
    doc.text(`作者：${bookData.author}`, { align: 'center' });
    doc.moveDown(0.5);
    if (bookData.category) { useFont(doc, 11); doc.text(`分类：${bookData.category}`, { align: 'center' }); }
    doc.moveDown(0.5);
    useFont(doc, 10);
    doc.text(`共 ${bookData.downloadedChapters || bookData.chapters.length} 章  ·  ${(bookData.totalWords || 0).toLocaleString()} 字`, { align: 'center' });
    if (bookData.abstract) { doc.moveDown(1.5); useFont(doc, 10); doc.text(bookData.abstract, { align: 'justify', width: 400 }); }
    doc.moveDown(1);
    useFont(doc, 8);
    doc.text(`生成时间：${new Date().toLocaleString('zh-CN')}`, { align: 'center' });
    doc.text('来源：番茄小说 fanqienovel.com', { align: 'center' });

    for (const ch of bookData.chapters) {
      doc.addPage();
      useFont(doc, 16);
      doc.text(ch.title, { align: 'center' });
      doc.moveDown(1);
      useFont(doc, 11);
      const paragraphs = (ch.content || '').split('\n').filter(p => p.trim());
      for (const p of paragraphs) {
        doc.text('　　' + p.trim(), { align: 'justify', lineGap: 4 });
        doc.moveDown(0.3);
      }
      const bottom = doc.page.height - 50;
      useFont(doc, 7);
      doc.text(`${bookData.bookName}  ·  ${ch.title}`, 72, bottom, { width: doc.page.width - 144, align: 'center' });
    }

    doc.addPage();
    useFont(doc, 10);
    doc.text('— 全书完 —', { align: 'center' });
    doc.moveDown(1);
    useFont(doc, 8);
    doc.text('本书由 MoChao 素材助手下载', { align: 'center' });
    doc.text('仅供个人学习参考，请于下载后24小时内删除', { align: 'center' });
    doc.end();
  });
}

module.exports = {
  downloadFanqieBook,
  generateTxtContent,
  generateHtmlContent,
  generatePdfContent,
  // 以下为内部函数，导出用于测试
  decodeFanqieText,
  decodeBest,
  isTruncated,
  isChapterContentValid,
  stripFanqieHtml,
};
