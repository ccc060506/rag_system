# RAG System

基于 Spring Boot 3 + LangChain4j 的检索增强生成（RAG）知识库系统，支持文档上传解析、向量检索、流式对话与敏感词过滤。

## 技术栈

- Java 21 / Spring Boot 3.2.5
- LangChain4j 1.12.2
- MyBatis-Plus 3.5.10
- MySQL 8 + Redis（向量存储）
- DeepSeek（对话）+ 阿里云 DashScope（向量化）
- 阿里云 OSS（文件存储）

## 环境要求

- JDK 21
- Maven 3.8+
- MySQL 8
- Redis Stack（需支持 RediSearch；普通 Redis 不行，可用 Docker 启动）

> Redis 不要求虚拟机：本机安装、Docker、云 Redis（需支持 RediSearch）都可以，应用只认 `REDIS_HOST` / `REDIS_PORT`。

## 快速启动依赖（Docker）

本机没有 MySQL / Redis 时，直接使用项目自带的 compose：

```bash
docker compose up -d
```

- MySQL：`127.0.0.1:3306`，root 密码默认 `root`（可用 `MYSQL_ROOT_PASSWORD` 覆盖），首次启动自动执行 `db/schema.sql`
- Redis Stack：`127.0.0.1:6379`，自带 RediSearch

然后在 `application-local.yaml` 里把 `MYSQL_PASSWORD` 改成对应密码即可。重置数据：`docker compose down -v`。

## 配置

项目中的敏感配置均通过环境变量注入，仓库内不包含任何密钥。

| 环境变量 | 说明 |
| --- | --- |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API Key |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 阿里云 OSS AccessKeyId |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 阿里云 OSS AccessKeySecret |
| `ALIYUN_OSS_BUCKET_NAME` | OSS Bucket 名称 |
| `ALIYUN_OSS_ENDPOINT` | OSS Endpoint，默认 `https://oss-cn-beijing.aliyuncs.com` |
| `ALIYUN_STS_ROLE_ARN` | 获取 OSS 临时凭证所用的 RAM Role ARN |
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` | 数据库连接，默认 `127.0.0.1:3306/RAG_system` |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 数据库账号，默认 `root` / 空 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接，默认 `127.0.0.1:6379` |
| `JWT_SECRET` | JWT 签名密钥（至少 32 字符） |
| `DATA_ENCRYPTION_KEY` | 用户 API Key 的 AES 加密密钥（任意长度字符串，内部取 SHA-256） |
| `AI_PROXY_HOST` / `AI_PROXY_PORT` | 请求 AI 服务时使用的 HTTP 代理，留空则不启用 |
| `RAG_EMBEDDING_MODEL` | 向量化模型，默认 `text-embedding-v1` |
| `RAG_REDIS_DIMENSION` | 向量维度兜底值，默认 `0` 表示启动时自动探测 |
| `SERVER_PORT` | 服务端口，默认 `8080` |

> JWT 有效期由 `jwt.expiration` 控制（毫秒，默认 2 小时），Redis 中的登录会话 TTL 与之一致，登出后 Token 立即失效。

### 本地开发

创建 `src/main/resources/application-local.yaml`（已被 `.gitignore` 忽略）填写真实值，然后以 `local` profile 启动：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

或在 IDE 中设置环境变量 `SPRING_PROFILES_ACTIVE=local`。

> 注意：直接运行主类而不启用 `local` profile 时，`application-local.yaml` 不会加载，启动会因缺少 DashScope API Key 失败。
> IDEA：Run/Debug Configurations → Spring Boot → Active profiles 填 `local`。

## 数据库初始化

**全新环境**：执行完整建表脚本（已包含 `role` / `owner_id` / `is_public` 字段）：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

> 如果使用上面的 `docker compose`，建表脚本会在首次启动时自动执行，可跳过这一步。

启动后先注册账号，再把它设为管理员：

```sql
UPDATE `user` SET `role` = 'ADMIN' WHERE `username` = '你的账号';
```

**从旧版本升级**：不要执行 `schema.sql`，改执行迁移脚本（先把脚本里的 `@admin_username` 改成你的账号名）：

```bash
mysql -u root -p RAG_system < src/main/resources/db/migration/V1__p0_p1_hardening.sql
```

管理员可以查看全部知识库和日志，并能维护敏感词。

## 启动

```bash
mvn clean package
java -jar target/RAG_system-0.0.1-SNAPSHOT.jar
```

服务默认运行在 `http://localhost:8080`，接口文档见 `http://localhost:8080/swagger-ui/index.html`。

### Redis 向量索引与维度

启动时会调用一次 embedding 接口自动探测当前模型的向量维度（`text-embedding-v1` 为 1536 维），并用它创建或校验索引，不需要手写 `dimension`。

已有索引会自动校验两件事：

- 索引维度是否与当前模型一致；
- 索引是否由同一个模型生成（记录在 Redis 的 `rag-meta:embedding-model` 中）。

任一不匹配都会直接启动失败，避免不同维度/不同模型的向量混用。此时需要删除索引并重新上传文档：

```bash
redis-cli FT.DROPINDEX rag-embeddings DD
```

如果启动环境访问不到 embedding 接口，可以显式设置 `RAG_REDIS_DIMENSION` 作为兜底。Redis Stack 使用 JSON 索引；从旧版本升级时，启动会用 `FT.ALTER` 自动补齐缺失的元数据字段。

## 目录结构

```
src/main/java/com/ragsystem/
├── common/              # 通用配置、工具、返回结构
├── interceptor/         # 登录拦截器
└── module/
    ├── AIChatService/   # 流式对话
    ├── KnowledgeService/# 知识库与文档处理
    ├── manager/         # 日志、敏感词管理
    └── userCenter/      # 用户与配置
```
