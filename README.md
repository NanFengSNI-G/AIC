<div align="center">

<img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs" alt="Vue">
<img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?logo=springboot" alt="Spring Boot">
<img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk" alt="Java">
<img src="https://img.shields.io/badge/Docker-🐳-2496ED?logo=docker" alt="Docker">
<img src="https://img.shields.io/badge/license-MIT-green" alt="License">

</div>

# AIC — AI 驱动的开发者社区

一个集**社区论坛**、**AI 模拟面试**、**AI 博客助手**于一体的全栈 Web 应用。前端 Vue 3 + Tailwind CSS，后端 Spring Boot 3 + LangGraph4j 多 Agent 协作，Docker 一键部署。

---

## 功能概览

### 社区论坛

| 功能 | 说明 |
|------|------|
| 多板块发帖 | 支持 Markdown 正文、本地上传图片、板块分类 |
| 评论 & 回复 | 二级评论嵌套，点赞互动 |
| 全文搜索 | Elasticsearch 驱动，支持按板块/时间/相关性排序 |
| 私信聊天 | WebSocket 实时通讯，好友管理，离线消息缓存 |
| 收藏 & 点赞 | Redis 计数 + 持久化同步 |

### AI 模拟面试

| 功能 | 说明 |
|------|------|
| 简历解析 | 上传 PDF/DOCX，AI 提取关键信息 |
| 自定义 JD | 输入目标岗位描述，精准匹配面试方向 |
| 实时语音 | WebSocket + 阿里云 ASR/TTS，全程语音交互 |
| 智能追问 | 基于回答深度动态生成追问，模拟真实面试 |
| 评估报告 | 面试结束后 AI 综合分析并输出评分 & 改进建议 |

**面试流程** — 每一轮对话都经历完整的 ASR → Decision → Agent → TTS 循环：

```
用户语音输入 (WebSocket PCM)
    │
    ▼
┌──────────┐
│   ASR    │ ← 阿里云 Qwen-ASR 实时语音转文字
└────┬─────┘
     │
     ▼
┌──────────┐
│ Decision │ ← 决策 Agent 判断面试阶段 & 路由
└────┬─────┘
     │
     ├──→ opening    开场：自我介绍 & 背景了解
     ├──→ tech       技术：根据简历/JD 深度提问
     ├──→ project    项目：深挖项目经验 & 难点
     ├──→ followup   追问：基于回答动态深入
     ├──→ algorithm  算法：编程题 & 复杂度分析
     └──→ ending     结束：总结评价 & 改进建议
     │
     ▼
┌──────────┐
│   TTS    │ ← 阿里云 Qwen-TTS 流式合成语音
└────┬─────┘
     │
     ▼
用户听到语音回复 → 下一轮回答 → 循环
     │
     ▼
 ending → 生成评估报告 → 关闭会话
```

### AI 博客助手

| 功能 | 说明 |
|------|------|
| 意图分析 | 自动识别写博客意图，提取主题和附加要求（字数、风格等） |
| 大纲设计 | 根据主题规划章节结构 & 关键要点 |
| 正文撰写 | ChatModel + WebSearch 工具，撰写完整 Markdown 正文 |
| 代码生成 | 分析正文自动在合适位置插入代码示例，支持多语言 |
| 配图生成 | 分析正文生成文生图提示词 → 调用 Qwen-Image-2.0 → 本地存储 |
| 审核发布 | 预览完整博客 → 选择板块 → 一键发布到论坛或存草稿 |
| SSE 进度 | 全流程实时推送进度，前端可视化展示每个 Agent 状态 |

基于 **LangGraph 多 Agent 工作流**，全自动创作：

```
用户输入主题
    │
    ▼
┌──────────────┐
│  意图分析     │ → 判断意图 & 提取主题 & 附加要求
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  大纲设计     │ → 规划章节结构 & 关键要点
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  正文撰写     │ → 完整正文（ChatModel + 网页搜索工具）
└──────┬───────┘
       │
       ▼  (并行)
┌──────────┐  ┌──────────┐
│ 代码生成  │  │ 配图生成  │
│ 分析正文  │  │ 分析正文  │
│ 自动插入  │  │ 文生图    │
│ 代码块    │  │ 本地存储  │
└────┬─────┘  └────┬─────┘
     │             │
     ▼             ▼
┌──────────────┐
│  拼装审核     │ → 智能合并正文/代码/配图 → 生成预览
└──────┬───────┘
       │
       ▼
  ┌─────────┐
  │ 发布 /   │ ← 用户确认后发布到论坛或保存草稿
  │ 存草稿   │
  └─────────┘
```

- **流式展示**：SSE 实时推送每个步骤的进度
- **用户决策点**：审核阶段可预览全文，选择发布或存草稿,选择发布板块

---

## 技术架构

```
┌─────────────────────────────────────────────────┐
│                    Nginx (Vue SPA)               │
│              :80 → /api → backend:8080           │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│              Spring Boot 3.4                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ REST API │ │WebSocket │ │ LangGraph4j Agent│ │
│  │ (论坛/用户│ │ (聊天/面试│ │ (博客助手/面试官) │ │
│  │  认证)   │ │  语音)    │ │                  │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
└──┬─────────┬──────────┬──────────┬──────────────┘
   │         │          │          │
   ▼         ▼          ▼          ▼
┌──────┐ ┌──────┐ ┌────────┐ ┌─────────────┐
│MySQL │ │Redis │ │RabbitMQ│ │Elasticsearch │
│ 8.0  │ │  7   │ │   3    │ │     8        │
└──────┘ └──────┘ └────────┘ └─────────────┘
```

| 组件 | 端口 | 用途 |
|------|------|------|
| **frontend** (Nginx) | 80 | Vue SPA + API 反向代理 + WebSocket 代理 |
| **backend** (Spring Boot) | 8080 | REST API、WebSocket、AI Agent |
| **mysql** | 3306 | 用户、帖子、评论、面试记录等持久化数据 |
| **redis** | 6379 | 会话缓存、点赞计数、聊天状态、Agent 记忆 |
| **rabbitmq** | 5672/15672 | 论坛帖子 ES 同步、离线消息队列 |
| **elasticsearch** | 9200 | 帖子全文搜索 |

---

## 快速部署（Docker）

### 前置条件

- [Docker](https://docs.docker.com/get-docker/) ≥ 24 + Docker Compose v2
- 阿里云 [DashScope API Key](https://dashscope.console.aliyun.com/)（用于 AI 功能）
- 2 GB 以上可用内存

### 1. 克隆项目

```bash
git clone https://github.com/NanFengSNI-G/AIC.git
cd AIC
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，必填项：

| 变量 | 说明 |
|------|------|
| `API_KEY` | 阿里云 DashScope API Key（[获取地址](https://dashscope.console.aliyun.com/apiKey)） |
| `JWT_SECRET` | JWT 签名密钥，建议用 `openssl rand -hex 32` 生成 |

以下可选（不填则使用默认值，功能不受影响）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SMTP_USERNAME` / `SMTP_PASSWORD` | - | 邮箱验证码服务（注册/找回密码） |
| `TAVILY_API_KEY` | - | 博客助手的网页搜索能力 |
| `MYSQL_ROOT_PASSWORD` | `root123` | MySQL root 密码 |
| `REDIS_PASSWORD` | `redis123` | Redis 密码 |
| `RABBITMQ_PASSWORD` | `rabbit123` | RabbitMQ 密码 |
| `ES_PASSWORD` | `elastic123` | Elasticsearch 密码 |

### 3. 一键启动

```bash
docker compose up -d
```

首次启动会拉取镜像并构建前后端，约 3-8 分钟（取决于网络）。

### 4. 检查状态

```bash
docker compose ps
```

所有服务状态均为 `healthy` 或 `Up` 即表示启动成功。

### 5. 访问

打开浏览器访问 **http://localhost**

> RabbitMQ 管理面板：http://localhost:15672 (admin/你设的密码)

### 停止 & 清理

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（重置所有数据）
docker compose down -v
```

---

## 本地开发

### 后端

```bash
# 1. 启动依赖服务
docker compose up -d mysql redis rabbitmq elasticsearch

# 2. 设置环境变量
export API_KEY=sk-your-key
export MySQL_PASSWORD=root123
export REDIS_PASSWORD=redis123
# ... 其余变量见 .env.example

# 3. 启动后端
cd backend
./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000
# Vite 自动代理 /api 到 localhost:8080
```

---

## 常见问题

**Q: 启动后访问页面空白？**
A: 等待所有容器健康检查通过（`docker compose ps` 确认），首次构建需要几分钟。

**Q: 博客助手报错 "连接失败"？**
A: 检查 `API_KEY` 是否正确设置，DashScope 账户是否有余额。

**Q: 图片上传失败？**
A: 检查 `uploads/` 目录是否有写入权限，Docker 部署时该目录由 volume 管理。

**Q: Elasticsearch 启动失败？**
A: Linux 主机可能需要调高 `vm.max_map_count`：
```bash
sudo sysctl -w vm.max_map_count=262144
```

---

## License

MIT