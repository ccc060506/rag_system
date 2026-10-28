# RAG System

前后端分离的企业知识库与 RAG 问答系统。

- `backend/`：Spring Boot 3 + Java 21 + MyBatis-Plus + MySQL 8 + Redis Stack
- `frontend/`：Vue 3 + Vite + Element Plus

## 功能

- 文档上传、解析、切分与向量化（PDF / Word / Excel / TXT）
- 基于知识库的流式问答，回答附带来源文件
- 历史对话：点击记录可回到对应问答并继续提问
- 敏感词过滤、分级拦截与分类管理
- 用户配额、API Key 配置、Token 消耗统计
- 深色 Midnight 视觉系统与官网首页

## 环境要求

- JDK 21
- Maven 3.8+
- Node.js 18+
- MySQL 8
- Redis Stack（需要 RediSearch，普通 Redis 不支持）
- DeepSeek API Key（对话）
- 阿里云 DashScope API Key（向量化）
- 阿里云 OSS（文档上传）

## 本地运行

### 1. 启动 MySQL 和 Redis

```bash
cd backend
docker compose up -d
```

首次启动会自动执行 `src/main/resources/db/schema.sql` 建表。

### 2. 配置后端

复制环境变量，或创建 `backend/src/main/resources/application-local.yaml`（该文件已被 `.gitignore` 忽略）：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` | 默认 `127.0.0.1:3306/RAG_system` |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 数据库账号密码 |
| `REDIS_HOST` / `REDIS_PORT` | 默认 `127.0.0.1:6379` |
| `DEEPSEEK_API_KEY` | DeepSeek 对话 Key |
| `DASHSCOPE_API_KEY` | DashScope 向量化 Key |
| `ALIYUN_OSS_ENDPOINT` / `ALIYUN_OSS_ACCESS_KEY_ID` / `ALIYUN_OSS_ACCESS_KEY_SECRET` / `ALIYUN_OSS_BUCKET_NAME` | OSS 配置 |
| `JWT_SECRET` | 至少 32 位的登录签名密钥 |
| `DATA_ENCRYPTION_KEY` | 用户 API Key 加密密钥 |

启动：

```bash
mvn spring-boot:run
```

默认地址 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址 `http://localhost:3000`，Vite 已代理接口到 8080。

新注册用户默认是管理员，可以直接维护敏感词和查看日志。

## 说明

仓库不包含任何真实密钥、`application-local.yaml`、IDE 配置、构建产物或依赖目录。运行前请自行填写环境变量。
