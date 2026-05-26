# 短链平台 (Shortlink Platform)

高性能短链接生成与跳转平台，基于 Spring Boot 4.0 + Java 21。

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 4.0.6 |
| 语言 | Java 21 |
| 数据库 | MySQL 8.0 + JPA / Hibernate |
| 缓存 | Redis 7 + Spring Cache |
| 布隆过滤器 | Guava BloomFilter（缓存穿透防御） |
| ID 生成 | 雪花算法（Snowflake）+ Base62 编码 |
| 限流 | Guava RateLimiter + 自定义 `@RateLimit` 注解 |
| 异步 | Spring `@Async`（点击日志异步写入） |
| API 文档 | SpringDoc OpenAPI（Swagger UI） |
| 部署 | Docker Compose（多阶段构建） |
| 构建 | Maven 多模块 |

## 快速启动

### 前置依赖

- JDK 21+
- Maven 3.9+（或使用 `./mvnw`）
- MySQL 8.0（本地需创建数据库 `shortlink_db`）
- Redis 7

### 本地开发

```bash
# 1. 初始化数据库（MySQL 需已运行）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS shortlink_db"

# 2. 复制并修改配置
cp shortlink-core/src/main/resources/application-template.properties shortlink-core/src/main/resources/application.properties
# 编辑 application.properties，填入你的 MySQL / Redis 密码

# 3. 构建
./mvnw clean package -DskipTests

# 4. 启动
cd shortlink-core && ../mvnw spring-boot:run
```

表结构由 Hibernate `ddl-auto=update` 自动管理，无需手动建表。

### Docker 部署（推荐）

```bash
# 一键启动（MySQL + Redis + 应用）
docker compose up -d

# 查看日志
docker compose logs -f shortlink-app

# 停止
docker compose down
```

服务编排自动处理网络和依赖顺序，开箱即用。

## API 接口

启动后访问 `http://localhost:8080/swagger-ui.html` 在线调试所有接口。

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/shorten?url=<longUrl>` | 生成短链接（限流 2 QPS） |
| `GET` | `/{shortCode}` | 访问短链接，302 重定向到原始 URL |
| `GET` | `/stats/{shortCode}` | 查询短链接点击次数 |

## 核心亮点

**雪花算法 + Base62 唯一短码**
41 位时间戳 + 10 位机器 ID + 12 位序列号生成全局唯一 ID，经 Base62（0-9 a-z A-Z）编码为短字符串。一次生成，无需碰撞重试。

**多层缓存穿透防御**
Redis 缓存（`@Cacheable`，TTL 1 小时）作一级缓存，布隆过滤器作二级拦截——请求先过布隆过滤器，不存在的短码直接返回 404，避免无效请求击穿到数据库。

**异步点击日志**
短链访问时通过 `@Async` 异步写入 ClickLog（IP、UA、Referer），302 跳转零阻塞，保证重定向响应速度。

**声明式限流**
基于 Guava RateLimiter 的自定义 `@RateLimit` 注解 + 拦截器，通过 `ConcurrentHashMap` 按方法隔离限流器，声明即用。

**Docker 多阶段构建**
Maven 编译和 JRE 运行分离，最终镜像仅包含 JRE 和可执行 Jar，体积轻量。

## 项目结构

```
shortlink-platform/
├── shortlink-common/        # 公共模块（工具类、注解）
│   └── util/Base62Encoder, Result, BusinessException, @RateLimit
├── shortlink-core/          # 核心业务模块
│   ├── controller/          # REST 控制器
│   ├── service/             # 业务逻辑
│   ├── repository/          # Spring Data JPA
│   ├── entity/              # JPA 实体
│   ├── common/              # 布隆过滤器、异常处理、限流拦截器、Redis 配置
│   └── util/                # 雪花算法 ID 生成器
├── docker-compose.yml       # Docker Compose 编排
├── Dockerfile               # 多阶段构建
└── pom.xml                  # Maven 父 POM
```
