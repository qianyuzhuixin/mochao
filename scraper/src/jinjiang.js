/**
 * 晋江文学城 — topten.php + onebook.php 模块
 */

const { fetchText, GBK_HEADERS, batchRun, logTop3 } = require('./utils');

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

module.exports = {
  JINJIANG_RANK_CONFIG,
  scrapeJinjiang,
};
