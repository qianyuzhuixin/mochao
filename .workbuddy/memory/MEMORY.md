# 墨抄项目记忆

## 项目概况
- **名称**: 墨抄 (MoChao) — 网文创作练笔平台
- **技术栈**: Vue 2.7 + Element UI / Spring Boot 2.7 + MyBatis-Plus / Node.js Scraper / MySQL 8 + Redis + MinIO
- **结构**: backend (Java) + frontend (Vue) + scraper (Node.js) 三模块

## 架构亮点
- 后端模块化分层清晰 (auth/book/novel/ai/collection/practice/statistics/admin/ranking/music)
- 构造器注入、全局异常处理、统一 Result 响应格式
- NovelServiceImpl 原子 SQL 更新防并发竞态
- 前端 axios 拦截器完善、三主题系统

## 主要技术债 (2026-07-15 二次评审更新)
- P0 已修: 零测试→20个测试、JWT硬编码→环境变量+强度校验、无CI/CD→GitHub Actions
- P1 已修: AI重试+枚举+Token统计、限流、Scraper拆分、JWT一次解析
- P0(V2新增已修): AiRuntimeConfig Bug(this.model=apiKey→model)、dev.yml明文凭据→环境变量
- P0(V2新增已修): package-lock.json恢复追踪、cookie_cache.json加入gitignore
- P2 未修: 前端分包优化、Redis缓存策略、Vue 2→3迁移、监控告警

## 已交付
- 2026-07-14: 全量代码评审 + P0+P1 代码修改 + 《团队技术提升方案.md》
- 2026-07-15: 二次深度扫描 + P0紧急修复(3项) + 《团队技术提升方案V2.md》

## 已完成代码修改汇总
- JWT: 弱密钥移除 + @PostConstruct强度校验 + parseClaims单次解析优化
- CI/CD: GitHub Actions 4-Job流水线
- 测试: 后端 JwtTokenProviderTest(11个) + 前端 auth.spec.js(9个)
- AI: callAiWithRetry(3次重试) + AiFeatureType枚举 + extractTokenUsage真实统计 + 构造函数Bug修复
- 限流: Bucket4j RateLimitFilter(AI 10/认证 20/通用 60次/分)
- Scraper: 2149行 → 25行入口 + 9模块
- 安全: dev.yml明文凭据清除 + package-lock.json恢复 + gitignore补全
- 排行榜乱码: fanqie.js PUA检测+过滤 + 后端PUA检测+清洗API + 三层自动自愈机制

## 关键技术知识
- 番茄小说字体反爬: 榜单页bookName/author使用PUA区(U+E000~U+F8FF)映射，需从详情页获取明文
- 七猫/刺猬猫: SSR HTML直解析，无字体反爬
- 排行榜数据流: Scraper→ScraperClient→RankingServiceImpl→MySQL+Redis→前端Ranking.vue
- 乱码自愈三层机制: 查询层(实时过滤+异步重抓) / 抓取层(乱码强制覆盖) / 定时巡检(4:00)
- Spring @Async 同类内部调用不生效 → 用 CompletableFuture.runAsync 替代
