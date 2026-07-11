# 墨抄 (MoChao) — 网文创作练笔平台

> 面向网文作者的一站式创作平台 — 抄书练笔 + 好词好句收藏 + AI辅助小说创作

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 前端 | Vue 2 + Element UI + ECharts + Sass | 2.7 + 2.15 + 5.x |
| 后端 | Spring Boot + MyBatis-Plus + Spring Security + JWT | 2.7.18 + 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 6.x |
| AI | 大模型API (OpenAI兼容接口) | - |

## 项目结构

```
mochao/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/mochao/
│   │   ├── MochaoApplication.java
│   │   ├── config/             # 配置类 (Security, CORS, Redis, MyBatis-Plus)
│   │   ├── common/             # 公共模块 (Result, Exception, Utils)
│   │   ├── security/           # JWT认证 (TokenProvider, Filter)
│   │   └── module/             # 业务模块
│   │       ├── auth/           # 用户认证
│   │       ├── book/           # 书库管理
│   │       ├── practice/       # 抄书练习
│   │       ├── collection/     # 好词好句
│   │       ├── novel/          # 小说创作
│   │       ├── ai/             # AI写作
│   │       ├── statistics/     # 数据统计
│   │       └── admin/          # 管理后台
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── db/init.sql         # 数据库初始化脚本
├── frontend/                   # Vue 2 前端
│   ├── src/
│   │   ├── api/                # API请求封装 (8个模块)
│   │   ├── components/         # 组件 (练习/收藏/小说/公共)
│   │   ├── layouts/            # 布局 (前台/后台)
│   │   ├── views/              # 页面 (28个视图)
│   │   ├── router/             # 路由配置
│   │   ├── store/              # Vuex状态管理
│   │   ├── styles/             # 样式系统 (3主题)
│   │   └── utils/              # 工具函数
│   └── package.json
└── README.md
```

## 快速开始

### 1. 数据库初始化

```bash
# 登录MySQL，执行初始化脚本
mysql -u root -p < backend/src/main/resources/db/init.sql
```

### 2. 后端启动

```bash
cd backend

# 修改数据库配置
# 编辑 src/main/resources/application-dev.yml
# 设置你的 MySQL 和 Redis 连接信息

# 编译启动
mvn spring-boot:run
```

后端运行在 `http://localhost:8080/api`

### 3. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run serve
```

前端运行在 `http://localhost:8081`，自动代理API请求到后端

### 4. 默认管理员账号

- 用户名: `admin`
- 密码: `admin123`

## 核心功能

### 抄书练笔
- 并排对照抄写（原文左 + 输入右）
- 逐字高亮：正确绿色、错误红色、当前位置高亮
- 实时数据：计时器、正确率、打字速度
- 断点续练 + 完成评分

### 好词好句收藏
- 练习中/浏览时均可收藏
- 类型分类（好词/好句）+ 自定义标签 + 来源归类
- 关键词搜索 + 导出（TXT/Markdown）+ 每日回顾

### 小说创作
- 多部小说管理
- 大纲 / 世界观 / 人物设定 / 物品设定 / 章纲 / 章节
- 纯文本编辑器 + 自动保存
- 写作进度追踪（字数趋势/章节状态/热力图）

### AI写作辅助
- 选中文本操作：优化 / 扩写 / 缩写 / 续写 / 润色对话
- 预判后续内容
- AI生成设定：大纲 / 人物 / 世界观 / 章纲
- 自动携带小说上下文（大纲+世界观+前文+人物）
- AI结果可采纳/放弃/再试

### 多主题系统
- 亮色 / 暗色 / 护眼绿 三种主题
- CSS变量驱动，即时切换
- 主题偏好持久化

### 管理后台
- 素材管理（CRUD + 批量导入）
- 用户管理
- 平台数据概览

## API文档

启动后端后访问 Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

## 开发计划

- [x] 第一阶段：项目搭建 + 基础架构
- [x] 第二阶段：用户认证
- [x] 第三阶段：核心抄书功能
- [x] 第四阶段：好词好句收藏
- [x] 第五阶段：数据统计
- [x] 第六阶段：小说创作基础框架
- [x] 第七阶段：AI写作功能
- [x] 第八阶段：管理后台
- [ ] 第九阶段：内置书库内容 + 优化
