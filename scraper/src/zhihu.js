/**
 * 知乎盐言 — 无公开排行榜
 */

async function scrapeZhihu(rankType) {
  console.log(`[zhihu] → 知乎盐言无公开排行榜`);
  throw new Error(
    '知乎盐言（付费故事）无公开排行榜页面。' +
    '知乎内容分发依赖个性化推荐算法，不提供传统意义上的榜单。'
  );
}

module.exports = {
  scrapeZhihu,
};
