/**
 * 七猫小说 — Nuxt SSR，HTML 直解析
 */

const { fetchText, PC_HEADERS, logTop3 } = require('./utils');

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
    const intro = introM ? introM[1].trim() : '';

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

module.exports = {
  QIMAO_RANK_TYPES,
  QIMAO_CHANNELS,
  scrapeQimao,
};
