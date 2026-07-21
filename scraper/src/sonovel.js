/**
 * so-novel 规则引擎 — 多书源搜索 + 下载
 *
 * 核心机制：
 * 1. 加载 JSON 规则文件（CSS 选择器 + DSL 语法）
 * 2. 并发搜索多个书源，聚合结果
 * 3. 按规则提取章节列表和正文内容
 * 4. 支持 filterTxt（正则过滤广告）和 filterTag（移除元素）
 * 5. 支持 @js: DSL 语法（如 base64 解码、URL 拼接）
 *
 * 规则文件格式（参考 so-novel 项目的 bundle/rules/main.json）：
 * {
 *   "url": "http://...",          // 书源根 URL
 *   "name": "书源名称",
 *   "search": {
 *     "url": "搜索接口 URL",
 *     "method": "post|get",
 *     "data": "{searchkey: %s}",  // POST body 模板，%s 替换为关键词
 *     "result": "CSS选择器",       // 搜索结果行
 *     "bookName": "CSS选择器",
 *     "author": "CSS选择器",
 *     ...
 *   },
 *   "toc": { "item": "CSS选择器" },  // 章节链接
 *   "chapter": {
 *     "title": "CSS选择器",
 *     "content": "CSS选择器",
 *     "filterTxt": "正则",          // 广告过滤
 *     "filterTag": "CSS选择器",      // 要移除的元素
 *     "paragraphTag": "<br>+",      // 段落分隔
 *     "paragraphTagClosed": false    // 段落标签是否闭合
 *   }
 * }
 */

const fs = require('fs');
const path = require('path');
const cheerio = require('cheerio');
const { fetchText, postText, PC_HEADERS, batchRun } = require('./utils');

// ===========================
// 规则加载
// ===========================

let RULES = [];

function loadRules() {
  const rulePath = path.join(__dirname, 'sonovel-rules', 'main.json');
  try {
    const raw = fs.readFileSync(rulePath, 'utf8');
    RULES = JSON.parse(raw);
    console.log(`[sonovel] 加载 ${RULES.length} 个书源规则`);
    RULES.forEach(r => console.log(`[sonovel]   - ${r.name} (${r.url})`));
  } catch (e) {
    console.error('[sonovel] 规则加载失败:', e.message);
    RULES = [];
  }
}

loadRules();

// ===========================
// DSL 解析 — 处理 @js: 和 @java: 语法
// ===========================

/**
 * 执行 DSL 后处理
 * 规则中 CSS 选择器后面可能跟 @js: 或 @java: 表示提取后的变换
 * 例如: "meta[property=\"og:image\"]@js:r='http://www.mcxs.la'+r"
 *       "content@js:var qsbs=...;r=r.replace(...)"
 *       "content@java:base64.decode()"
 *
 * @param {string} selector 原始规则字符串
 * @returns {{css: string, dsl: string|null}} 拆分后的 CSS 选择器和 DSL 代码
 */
function parseDsl(selector) {
  if (!selector) return { css: '', dsl: null };
  const jsIdx = selector.indexOf('@js:');
  const javaIdx = selector.indexOf('@java:');
  const dslIdx = jsIdx >= 0 ? jsIdx : javaIdx;
  if (dslIdx < 0) return { css: selector, dsl: null };
  return {
    css: selector.substring(0, dslIdx).trim(),
    dsl: selector.substring(dslIdx).trim(),
  };
}

/**
 * 执行 DSL 变换
 * @param {string} html 提取到的 HTML/文本
 * @param {string} dsl DSL 指令（如 "@js:r='http://...'+r"）
 * @returns {string} 变换后的结果
 */
function execDsl(html, dsl) {
  if (!dsl) return html;
  let r = html;

  if (dsl.startsWith('@js:')) {
    const code = dsl.substring(4);
    try {
      // 在沙箱中执行，r 是输入输出变量
      // 支持 @java:base64.decode() 语法
      const fn = new Function('r', 'base64', code + '\nreturn r;');
      r = fn(r, {
        decode: (s) => Buffer.from(s, 'base64').toString('utf8'),
        encode: (s) => Buffer.from(s).toString('base64'),
      });
    } catch (e) {
      console.warn(`[sonovel] DSL执行失败: ${e.message}`);
    }
  } else if (dsl.startsWith('@java:base64.decode()')) {
    try {
      r = Buffer.from(r, 'base64').toString('utf8');
    } catch (e) {
      console.warn(`[sonovel] base64解码失败: ${e.message}`);
    }
  }

  return r;
}

/**
 * 提取属性后缀 — 处理 @href, @src, @content 语法
 * 例如: "#info > a@href" → 提取 href 属性
 */
function extractAttr(selector) {
  const attrMatch = selector.match(/@(href|src|content)$/);
  if (attrMatch) {
    return {
      css: selector.substring(0, selector.lastIndexOf('@')),
      attr: attrMatch[1],
    };
  }
  return { css: selector, attr: null };
}

// ===========================
// 搜索
// ===========================

/**
 * 搜索单个书源
 * @param {object} rule 书源规则
 * @param {string} keyword 搜索关键词
 * @returns {Promise<Array>} 搜索结果数组
 */
async function searchOneSource(rule, keyword) {
  const s = rule.search;
  if (!s || !s.url) return [];

  try {
    let html;
    const headers = { ...PC_HEADERS, Referer: rule.url };

    if (s.method === 'post') {
      // 规则中 data 格式如 "{searchkey: %s}" 或 "{searchkey: %s, searchtype: all}"
      // 需要解析为 URL-encoded 表单: searchkey=<encoded>&searchtype=all
      const dataStr = s.data.replace('%s', encodeURIComponent(keyword));
      // 去掉花括号，按逗号分割键值对
      const inner = dataStr.replace(/^\{|\}$/g, '');
      const pairs = inner.split(',').map(p => p.trim());
      const formParts = pairs.map(p => {
        const colonIdx = p.indexOf(':');
        if (colonIdx < 0) return p;
        const key = p.substring(0, colonIdx).trim();
        const val = p.substring(colonIdx + 1).trim();
        // 值可能是已编码的关键词，或固定字符串如 "all", "搜索"
        return key + '=' + val;
      });
      const body = formParts.join('&');
      html = await postText(s.url, body, headers, 12000);
    } else {
      // GET 请求，替换 URL 中的 %s
      const url = s.url.replace('%s', encodeURIComponent(keyword));
      html = await fetchText(url, headers, 3, 'utf8');
    }

    const $ = cheerio.load(html);
    const results = [];

    $(s.result).each((_, el) => {
      const $row = $(el);
      const bookNameEl = $row.find(s.bookName).first();
      const bookUrl = bookNameEl.attr('href') || '';

      // 如果没有书名或链接，跳过（可能是表头）
      if (!bookNameEl.text().trim() || !bookUrl) return;

      const book = {
        sourceName: rule.name,
        sourceUrl: rule.url,
        bookName: bookNameEl.text().trim(),
        bookUrl: bookUrl.startsWith('http') ? bookUrl : new URL(bookUrl, rule.url).toString(),
        author: s.author ? $row.find(s.author).text().trim() : '',
        category: s.category ? $row.find(s.category).text().trim() : '',
        latestChapter: s.latestChapter ? $row.find(s.latestChapter).text().trim() : '',
        lastUpdateTime: s.lastUpdateTime ? $row.find(s.lastUpdateTime).text().trim() : '',
        status: s.status ? $row.find(s.status).text().trim() : '',
      };

      results.push(book);
    });

    console.log(`[sonovel] ${rule.name}: 搜索到 ${results.length} 条结果`);
    return results;
  } catch (e) {
    console.warn(`[sonovel] ${rule.name} 搜索失败: ${e.message}`);
    return [];
  }
}

/**
 * 并发搜索所有书源
 * @param {string} keyword 搜索关键词
 * @param {number} timeout 总超时（ms）
 * @returns {Promise<Array>} 聚合后的搜索结果
 */
async function searchAllSources(keyword, timeout = 30000) {
  console.log(`[sonovel] 开始全平台搜索: "${keyword}"`);

  // 并发搜索所有书源，每个最多 12s
  const searchPromises = RULES.map(rule =>
    searchOneSource(rule, keyword).catch(() => [])
  );

  // 总超时保护
  const timeoutPromise = new Promise(resolve =>
    setTimeout(() => resolve(null), timeout)
  );

  const settled = await Promise.race([
    Promise.allSettled(searchPromises),
    timeoutPromise,
  ]);

  let allResults = [];
  if (Array.isArray(settled)) {
    for (const r of settled) {
      if (r.status === 'fulfilled' && Array.isArray(r.value)) {
        allResults = allResults.concat(r.value);
      }
    }
  }

  // 去重：相同书名+作者只保留第一条
  const seen = new Set();
  const deduped = [];
  for (const book of allResults) {
    const key = `${book.bookName}|${book.author}`;
    if (!seen.has(key)) {
      seen.add(key);
      deduped.push(book);
    }
  }

  console.log(`[sonovel] 全平台搜索完成: ${allResults.length} 条 → 去重后 ${deduped.length} 条`);
  return deduped;
}

// ===========================
// 章节列表
// ===========================

/**
 * 获取书籍的章节列表
 * @param {string} bookUrl 书籍详情页 URL
 * @param {string} sourceName 书源名称
 * @returns {Promise<Array<{title, url}>>} 章节列表
 */
async function fetchChapterList(bookUrl, sourceName) {
  const rule = RULES.find(r => r.name === sourceName || r.url === sourceName);
  if (!rule) throw new Error(`找不到书源: ${sourceName}`);

  console.log(`[sonovel] 获取章节列表: ${bookUrl} (来源: ${rule.name})`);

  const html = await fetchText(bookUrl, { ...PC_HEADERS, Referer: rule.url }, 3, 'utf8');
  const $ = cheerio.load(html);

  // 如果 toc 有自定义 URL，先跳转
  let tocUrl = bookUrl;
  if (rule.toc && rule.toc.url) {
    // toc.url 格式如 "http://www.ujxsw.org/read/%s/" 其中 %s 是书籍 ID
    // 从 bookUrl 中提取 ID
    const bookIdMatch = bookUrl.match(/\/(\d+)\//);
    if (bookIdMatch) {
      tocUrl = rule.toc.url.replace('%s', bookIdMatch[1]);
      const tocHtml = await fetchText(tocUrl, { ...PC_HEADERS, Referer: rule.url }, 3, 'utf8');
      $('html').replaceWith(tocHtml);
    }
  }

  const chapters = [];
  const itemSelector = rule.toc?.item;
  if (!itemSelector) throw new Error(`书源 ${rule.name} 缺少 toc.item 规则`);

  $(itemSelector).each((_, el) => {
    const $a = $(el);
    const title = $a.text().trim();
    const href = $a.attr('href');
    if (title && href) {
      const fullUrl = href.startsWith('http') ? href : new URL(href, tocUrl).toString();
      chapters.push({ title, url: fullUrl });
    }
  });

  // 处理分页（如果有 nextPage 规则，如 select > option）
  if (rule.toc?.nextPage) {
    const nextPages = [];
    $(rule.toc.nextPage).each((_, el) => {
      const val = $(el).attr('value') || $(el).attr('href');
      if (val) {
        const fullUrl = val.startsWith('http') ? val : new URL(val, tocUrl).toString();
        if (fullUrl !== tocUrl) nextPages.push(fullUrl);
      }
    });

    // 并发获取分页章节
    const extraChapters = await batchRun(nextPages, 3, async (pageUrl) => {
      try {
        const pageHtml = await fetchText(pageUrl, { ...PC_HEADERS, Referer: rule.url }, 3, 'utf8');
        const $page = cheerio.load(pageHtml);
        const pageChapters = [];
        $page(itemSelector).each((_, el) => {
          const $a = $page(el);
          const title = $a.text().trim();
          const href = $a.attr('href');
          if (title && href) {
            const fullUrl = href.startsWith('http') ? href : new URL(href, pageUrl).toString();
            pageChapters.push({ title, url: fullUrl });
          }
        });
        return pageChapters;
      } catch (e) {
        console.warn(`[sonovel] 分页获取失败 ${pageUrl}: ${e.message}`);
        return null;
      }
    });

    // 合并分页章节
    for (const batch of extraChapters) {
      if (Array.isArray(batch)) chapters.push(...batch);
    }
  }

  console.log(`[sonovel] 获取到 ${chapters.length} 章`);
  return chapters;
}

// ===========================
// 章节内容
// ===========================

/**
 * 下载单个章节内容
 * @param {string} chapterUrl 章节 URL
 * @param {string} sourceName 书源名称
 * @returns {Promise<{title, content}|null>}
 */
async function fetchChapterContent(chapterUrl, sourceName) {
  const rule = RULES.find(r => r.name === sourceName || r.url === sourceName);
  if (!rule) throw new Error(`找不到书源: ${sourceName}`);

  const ch = rule.chapter;
  if (!ch) throw new Error(`书源 ${rule.name} 缺少 chapter 规则`);

  try {
    const html = await fetchText(chapterUrl, { ...PC_HEADERS, Referer: rule.url }, 3, 'utf8');
    const $ = cheerio.load(html);

    // 提取标题
    const title = $(ch.title).first().text().trim();

    // 提取内容（可能带 DSL 变换）
    const { css: contentSelector, dsl } = parseDsl(ch.content);
    let $content = $(contentSelector).first();

    if ($content.length === 0) {
      console.warn(`[sonovel] 章节内容选择器无匹配: ${contentSelector}`);
      return null;
    }

    // 移除广告标签
    if (ch.filterTag) {
      const tags = ch.filterTag.split(',').map(t => t.trim()).filter(Boolean);
      for (const tag of tags) {
        $content.find(tag).remove();
      }
    }

    // 获取 HTML
    let contentHtml = $content.html() || '';

    // 执行 DSL 变换（如 base64 解码）
    if (dsl) {
      contentHtml = execDsl(contentHtml, dsl);
    }

    // 转换为纯文本
    let text;
    if (ch.paragraphTagClosed) {
      // 段落标签闭合：<p>...</p> 格式，每段换行
      text = cheerio.load(contentHtml)('body').first().html() || contentHtml;
      text = text.replace(/<\/p>\s*<p[^>]*>/gi, '\n');
      text = text.replace(/<[^>]+>/g, '');
    } else {
      // 段落用 <br> 分隔
      const brPattern = ch.paragraphTag || '<br>+';
      const brRegex = new RegExp(brPattern, 'gi');
      text = contentHtml.replace(brRegex, '\n');
      text = text.replace(/<[^>]+>/g, '');
    }

    // HTML 实体解码
    text = text.replace(/&nbsp;/g, ' ')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&amp;/g, '&')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'");

    // 广告过滤
    if (ch.filterTxt) {
      const filterRegex = new RegExp(ch.filterTxt, 'g');
      text = text.replace(filterRegex, '');
    }

    // 清理多余空行
    text = text.replace(/\n{3,}/g, '\n\n').trim();

    if (text.length < 50) {
      console.warn(`[sonovel] 章节内容过短 (${text.length}字): ${chapterUrl}`);
      return null;
    }

    return { title: title || '未知章节', content: text };
  } catch (e) {
    console.warn(`[sonovel] 章节下载失败 ${chapterUrl}: ${e.message}`);
    return null;
  }
}

// ===========================
// 整本下载
// ===========================

/**
 * 下载整本书
 * @param {string} bookUrl 书籍详情页 URL
 * @param {string} sourceName 书源名称
 * @param {number} maxChapters 最大章节数（0=全部）
 * @returns {Promise<{bookName, author, chapters: Array}>}
 */
async function downloadBook(bookUrl, sourceName, maxChapters = 0) {
  const rule = RULES.find(r => r.name === sourceName || r.url === sourceName);
  if (!rule) throw new Error(`找不到书源: ${sourceName}`);

  console.log(`[sonovel] 开始下载: ${bookUrl} (来源: ${rule.name})`);

  // 1. 获取书籍详情
  const html = await fetchText(bookUrl, { ...PC_HEADERS, Referer: rule.url }, 3, 'utf8');
  const $ = cheerio.load(html);

  let bookName = '';
  let author = '';
  let intro = '';

  if (rule.book) {
    if (rule.book.bookName) {
      const { css, dsl } = parseDsl(rule.book.bookName);
      bookName = dsl ? execDsl($(css).first().text().trim(), dsl) : $(css).first().text().trim();
    }
    if (rule.book.author) {
      author = $(rule.book.author).first().text().trim().replace('作者：', '');
    }
    if (rule.book.intro) {
      intro = $(rule.book.intro).first().text().trim();
    }
  }

  // 如果详情页没有书名，从搜索结果 URL 推断
  if (!bookName) {
    const titleEl = $('title').text().split(/[-_－]/);
    bookName = titleEl[0]?.trim() || '未知书名';
  }
  if (!author) {
    author = '';
  }

  console.log(`[sonovel] 书名: ${bookName}, 作者: ${author}`);

  // 2. 获取章节列表
  const chapterList = await fetchChapterList(bookUrl, sourceName);
  const chaptersToDownload = maxChapters > 0 ? chapterList.slice(0, maxChapters) : chapterList;

  console.log(`[sonovel] 共 ${chapterList.length} 章，下载 ${chaptersToDownload.length} 章`);

  // 3. 并发下载章节（3并发，避免被限流）
  const chapters = [];
  let successCount = 0;
  let failCount = 0;

  for (let i = 0; i < chaptersToDownload.length; i += 3) {
    const batch = chaptersToDownload.slice(i, i + 3);
    const results = await Promise.allSettled(
      batch.map(ch => fetchChapterContent(ch.url, sourceName))
    );

    for (let j = 0; j < results.length; j++) {
      const idx = i + j + 1;
      if (results[j].status === 'fulfilled' && results[j].value) {
        chapters.push({
          chapterNo: idx,
          title: results[j].value.title,
          content: results[j].value.content,
        });
        successCount++;
      } else {
        chapters.push({
          chapterNo: idx,
          title: batch[j].title,
          content: '[下载失败]',
        });
        failCount++;
      }
    }

    // 进度日志
    if ((i + 3) % 30 === 0 || i + 3 >= chaptersToDownload.length) {
      console.log(`[sonovel] 下载进度: ${Math.min(i + 3, chaptersToDownload.length)}/${chaptersToDownload.length} (成功${successCount} 失败${failCount})`);
    }

    // 批次间停顿
    if (i + 3 < chaptersToDownload.length) {
      await new Promise(r => setTimeout(r, 300));
    }
  }

  console.log(`[sonovel] 下载完成: ${bookName} - 成功${successCount}章 失败${failCount}章`);

  return {
    bookName,
    author,
    intro,
    sourceName: rule.name,
    totalChapters: chapterList.length,
    downloadedChapters: successCount,
    chapters,
  };
}

// ===========================
// 导出
// ===========================

module.exports = {
  RULES,
  searchAllSources,
  fetchChapterList,
  fetchChapterContent,
  downloadBook,
};
