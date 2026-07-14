/**
 * MoChao Scraper Service — 网文榜单抓取服务
 *
 * 采集策略（全平台无浏览器，纯 HTTPS 请求）：
 *   起点中文网 → 移动端 m.qidian.com Vite SSR pageContext JSON 直读
 *   番茄小说   → 两步：榜单页取 bookId/热度，详情页取明文书名/作者/简介
 *   晋江文学城 → topten.php 频道分组 + onebook.php detail 微数据
 *   知乎盐言   → 无公开排行榜，返回提示
 *
 * Express 独立进程，监听 localhost:3001
 */

const { app, PLATFORM_SCRAPERS, FANQIE_CAT_SCRAPERS } = require('./src/routes');

const PORT = process.env.SCRAPER_PORT || 3001;

app.listen(PORT, '127.0.0.1', () => {
  console.log(`[scraper] 抓取服务已启动: http://127.0.0.1:${PORT}`);
  console.log(`[scraper] 模式: 纯 HTTP（SSR HTML / JSON 提取，无浏览器）`);
  console.log(`[scraper] 平台: ${Object.keys(PLATFORM_SCRAPERS).join(', ')}`);
  console.log(`[scraper] 番茄分类榜: ${Object.keys(FANQIE_CAT_SCRAPERS).length} 个`);
  console.log(`[scraper] 整本下载: fanqie`);
  console.log(`[scraper] 搜索: fanqie`);
  console.log(`[scraper] 健康检查: http://127.0.0.1:${PORT}/health`);
});
