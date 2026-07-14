/**
 * 刺猬猫 — 传统 SSR，HTML 直解析
 */

const { fetchText, PC_HEADERS, logTop3 } = require('./utils');

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

module.exports = {
  CIWEIMAO_RANK_TYPES,
  scrapeCiweimao,
};
