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

## 主要技术债 (2026-07-14 评审)
- P0: 零测试覆盖（后端+前端均无测试）、JWT密钥硬编码在 .env.template、无 CI/CD
- P1: AI服务无重试/流式、Scraper单文件2149行、JWT重复解析3次、无API限流、Token用量统计用字符长度代替
- P2: 前端无懒加载/分包、无Redis缓存策略、Vue 2已EOL、无监控告警

## 已交付
- 2026-07-14: 完成全量代码评审，产出《团队技术提升方案.md》
- 2026-07-14: 执行全部 P0+P1 代码修改（JWT安全/CI/CD/测试框架/AI重试+枚举+Token统计/API限流/Scraper拆分），产出《代码修改执行清单.md》

## 已完成代码修改 (2026-07-14)
- JWT: 弱密钥移除 + @PostConstruct强度校验 + parseClaims单次解析优化
- CI/CD: GitHub Actions 4-Job流水线
- 测试: 后端 JwtTokenProviderTest(12个) + 前端 auth.spec.js(10个)
- AI: callAiWithRetry(3次重试) + AiFeatureType枚举 + extractTokenUsage真实统计
- 限流: Bucket4j RateLimitFilter(AI 10/认证 20/通用 60次/分)
- Scraper: 2149行 → 25行入口 + 9模块
