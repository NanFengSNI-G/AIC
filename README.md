# AIC — AI 驱动的开发者社区论坛

一个集社区论坛、AI 模拟面试、AI 博客助手于一体的全栈 Web 应用。

## 功能特性

### 社区论坛
- 多板块帖子发布/编辑/删除，Markdown 正文 + 图片上传
- 评论与回复，点赞与收藏
- 全文搜索（Elasticsearch）
- 实时私信聊天（WebSocket）

### AI 模拟面试
- 上传简历 + 输入 JD，AI 面试官实时语音提问
- 30 分钟模拟面试，结束后生成评估报告
- 面试记录回顾，支持文字/语音交互

### AI 博客助手
- 输入主题，AI 自动设计大纲 → 撰写正文 → 生成代码示例 → 配图
- 正文、代码、配图三 Agent 协作（LangGraph 多 Agent 工作流）
- 一键发布到论坛或保存草稿
- SSE 实时流式展示生成进度

## 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | Vue 3 + TypeScript + Vite + Pinia + Tailwind CSS |
| **后端** | Spring Boot 3 + MyBatis-Plus + LangChain4j + LangGraph4j |
| **数据库** | MySQL 8.0 |
| **缓存** | Redis 7 |
| **消息队列** | RabbitMQ 3 |
| **搜索引擎** | Elasticsearch 8 |
| **AI 模型** | 阿里云 DashScope（Qwen3.6-Flash / Qwen-Image-2.0-Pro） |
| **容器化** | Docker + Docker Compose |

## 项目结构

```
AIC/
├── backend/                     # Spring Boot 后端
│   ├── src/main/java/           # Java 源码
│   │   └── com/project/demo/
│   │       ├── agent/           # AI Agent（LangGraph）
│   │       │   ├── blog/        #   博客助手（多 Agent 协同）
│   │       │   └── interview/   #   模拟面试
│   │       ├── config/          # Spring 配置
│   │       ├── controller/      # REST API
│   │       ├── service/         # 业务逻辑
│   │       └── entity/          # 数据实体
│   ├── src/main/resources/      # 配置文件
│   ├── docs/                    # SQL 初始化脚本
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                    # Vue 3 前端
│   ├── src/
│   │   ├── components/          # 组件（forum/chat/blog/common）
│   │   ├── pages/               # 页面
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── services/            # API 请求
│   │   └── styles/              # 全局样式
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml           # 容器编排
├── .env.example                 # 环境变量模板
└── README.md
```

## 快速开始（Docker 部署）

### 前置要求

- [Docker](https://docs.docker.com/get-docker/) & Docker Compose v2
- 阿里云 DashScope [API Key](https://dashscope.console.aliyun.com/)

### 1. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，填入必要信息：

| 变量 | 说明 | 必填 |
|------|------|------|
| `API_KEY` | DashScope API Key | ✅ |
| `JWT_SECRET` | JWT 签名密钥 | ✅ |
| `SMTP_USERNAME` | 发件邮箱 | 可选 |
| `SMTP_PASSWORD` | 邮箱授权码 | 可选 |
| `TAVILY_API_KEY` | 网页搜索 API Key | 可选 |
| `MYSQL_ROOT_PASSWORD` | MySQL 密码 | 默认值可用 |
| `REDIS_PASSWORD` | Redis 密码 | 默认值可用 |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | 默认值可用 |
| `ES_PASSWORD` | Elasticsearch 密码 | 默认值可用 |

### 2. 启动所有服务

```bash
docker compose up -d
```

首次启动会自动构建前后端镜像并拉取依赖服务镜像，预计耗时 3-5 分钟。

### 3. 访问应用

| 服务 | 地址 |
|------|------|
| 论坛前端 | http://localhost |
| RabbitMQ 管理面板 | http://localhost:15672 |
| Elasticsearch | http://localhost:9200 |

### 4. 停止服务

```bash
docker compose down
```

## 本地开发

### 后端

```bash
cd backend
# 确保 MySQL / Redis / RabbitMQ / ES 已启动
# 设置环境变量后运行
./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev          # 访问 http://localhost:3000
```

## License

MIT