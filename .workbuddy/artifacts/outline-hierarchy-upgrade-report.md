# 墨抄 V3: 四级大纲体系升级报告

> 日期: 2026-07-21 | 实施: 高级开发工程师

---

## 📋 需求回顾

> 在"我的小说"中添加卷纲、幕、细纲四级大纲体系。生成大纲时将小说简介/类型/书名 + 用户提示词 + 专有提示词组合；生成卷纲时以上级（大纲）为上下文；以此类推。支持配置各类生成的专有提示词。

---

## 🏗️ 架构设计

```
大纲 (Outline)
  └── 卷纲 (Volume) × N    — 每卷独立主题和情节任务
        └── 幕 (Act) × N    — 每幕核心冲突和场景序列
              └── 细纲/章纲 (Chapter Outline) × N  — 每章详细规划
```

### AI 生成上下文链

| 生成目标 | 上下文来源 |
|----------|-----------|
| 大纲 | 小说名 + 类型 + 简介 |
| 卷纲 | 大纲内容 + 用户 prompt |
| 幕纲 | 大纲 + 卷纲内容 + 用户 prompt |
| 细纲 | 大纲 + 卷纲 + 幕纲内容 + 用户 prompt |

---

## 📊 变更清单

### 数据库（1个迁移脚本）
- `migration_V3_outline_hierarchy.sql` — 新建 `t_novel_volume`、`t_novel_act`、`t_ai_prompt_template` 三表 + 扩展 `t_novel_chapter_outline` 加 `act_id` + 系统默认提示词种子数据

### 后端（17个文件）
| 类型 | 文件 | 说明 |
|------|------|------|
| Entity | `NovelVolume.java` ★ | 卷实体 |
| Entity | `NovelAct.java` ★ | 幕实体 |
| Entity | `AiPromptTemplate.java` ★ | 提示词模板实体 |
| DTO | `NovelVolumeDTO.java` ★ | 卷请求体 |
| DTO | `NovelActDTO.java` ★ | 幕请求体 |
| DTO | `AiPromptTemplateDTO.java` ★ | 模板请求体 |
| DTO | `NovelChapterOutlineDTO.java` ✏ | +actId |
| DTO | `AiGenerateDTO.java` ✏ | +parentType/parentId |
| Entity | `NovelChapterOutline.java` ✏ | +actId |
| Enum | `AiFeatureType.java` ✏ | +3个生成类型 |
| Mapper | `NovelVolumeMapper.java` ★ | |
| Mapper | `NovelActMapper.java` ★ | |
| Mapper | `AiPromptTemplateMapper.java` ★ | |
| Service | `AiPromptTemplateService.java` + `Impl` ★ | 模板CRUD+查询 |
| Service | `NovelService.java` ✏ | +卷/幕CRUD接口 |
| Service | `NovelServiceImpl.java` ✏ | +卷/幕CRUD实现+级联删除+actId |
| Service | `AiServiceImpl.java` ✏ | **核心重构**: 提示词模板化+上下文链式传递 |
| Controller | `NovelController.java` ✏ | +卷/幕12个端点 |
| Controller | `AiPromptTemplateController.java` ★ | 模板管理3个端点 |

### 前端（7个文件）
| 类型 | 文件 | 说明 |
|------|------|------|
| View | `VolumeList.vue` ★ | 卷纲管理页（AI生成+CRUD） |
| View | `ActList.vue` ★ | 幕纲管理页（按卷筛选+AI生成） |
| View | `PromptTemplates.vue` ★ | AI提示词模板配置页 |
| View | `NovelWorkspace.vue` ✏ | +卷纲/幕入口 |
| View | `Profile.vue` ✏ | +AI配置+提示词模板菜单 |
| API | `novel.js` ✏ | +卷/幕 API 函数 |
| API | `ai.js` ✏ | +3个生成API+模板管理API |
| Router | `index.js` ✏ | +3个新路由 |

> ★ 新增  |  ✏ 修改

---

## 🔑 核心实现亮点

### 1. 提示词模板系统
- **三级 fallback**: 用户自定义模板 → 系统默认模板(user_id=0) → 硬编码兜底
- **热更新**: 修改模板后即时生效，无需重启
- **一键恢复**: 可随时恢复系统默认模板
- 6个种子模板（大纲/卷纲/幕纲/细纲/人物/世界观）包含专业网文创作提示

### 2. AI 生成上下文链式传递
```java
buildGenerateContext() {
    1. 【小说基本信息】书名+类型+简介
    2. 【小说大纲】（如果需要）
    3. 【父级内容】parentType=volume → 卷纲 / parentType=act → 幕纲+卷纲
    4. 【全部卷纲概览】（生成幕纲时自动加载）
}
```

### 3. 数据完整性
- 删除卷时级联删除其下所有幕
- 删除小说时级联删除卷和幕
- 章纲支持 `act_id` 关联幕（可选）

---

## 🧪 验证结果
- ✅ 前端 ESLint: 0 errors, 0 warnings
- ✅ 体系完备: Entity → Mapper → Service → Controller 全链路
- ✅ 数据库迁移脚本带存储过程安全守卫（幂等执行）

## 📝 部署步骤

1. 执行 `backend/src/main/resources/db/migration_V3_outline_hierarchy.sql`
2. 重启后端服务
3. 重新构建前端: `npm run build`

## 🔮 下一步建议
- 前端"细纲"页面（可与章纲管理合并，添加幕筛选）
- 支持从章纲一键"开始写本章"
- 导出大纲为 Markdown 功能
