# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# 构建（在项目根目录）
./mvnw clean package

# 跳过测试构建
./mvnw clean package -DskipTests

# 运行测试
./mvnw test

# 启动应用（在 shortlink-core 子模块）
cd shortlink-core && ../mvnw spring-boot:run
```

项目需要本地 MySQL 运行在 `localhost:3306`（数据库名 `shortlink_db`，用户名 `root`，密码见 `application.properties`）和 Redis 运行在 `localhost:6379`（密码见 `application.properties`）。表结构由 Hibernate `ddl-auto=update` 自动管理。

## 架构

多模块 Maven 项目，分层如下：

```
shortlink-platform/                    ← Root (parent POM)
├── shortlink-common/                  ← 公共模块（无 Spring 依赖）
│   └── Result, BusinessException, RateLimit 注解, Base62Encoder
└── shortlink-core/                    ← 核心业务模块
    ├── controller/ShortLinkController
    ├── service/{ShortLink,ClickLog}Service
    ├── common/{BloomFilterService,GlobalExceptionHandler,RateLimitInterceptor,RedisConfig}
    ├── repository/{ShortLink,ClickLog}Repository
    ├── entity/{ShortLink,ClickLog}
    └── util/SnowflakeIdGenerator

Controller (REST) → Service (业务逻辑) → Repository (Spring Data JPA) → Entity (JPA 映射 MySQL)
     ↑                    ↓                           ↓
 @Validated       SnowflakeIdGenerator           Redis Cache
 @RateLimit        (雪花算法生成唯一ID)            (@Cacheable)
     │                    ↓                           │
     │           Base62Encoder.encode(id)             │
     │           (62进制编码为短码)                   │
     │                    │                           │
     │            BloomFilterService                  │
     │          (缓存穿透防御 + 懒加载缓存)            │
     │                    │                           │
     │            ClickLogService                     │
     │          (@Async 异步点击日志)                  │
     └──────────────────┬─────────────────────────────┘
                        │
                 GlobalExceptionHandler
              (@RestControllerAdvice 统一异常处理 → Result<T> 统一响应)
```

- **入口**：`ShortlinkCoreApplication`（`@SpringBootApplication` + `@EnableCaching` + `@EnableAsync`）
- **三个 API 端点**：
  - `POST /shorten?url=<longUrl>` → 雪花 ID → Base62 短码 → 存库 + 布隆过滤器同步 → 返回 `Result<String>`（限流 2 QPS）
  - `GET /{shortCode}` → Redis 缓存 → 布隆过滤器 → MySQL → 异步记录点击日志 → 302 重定向
  - `GET /stats/{shortCode}` → 查询点击次数
- **数据库**：`short_link`（id, short_code, long_url, create_time）+ `click_log`（id, short_code, visitor_ip, user_agent, referer, click_time）

## 技术栈与注意点

| 项 | 版本/值 |
|----|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| 数据库 | MySQL + JPA/Hibernate |
| 缓存 | Redis + Spring Cache |
| 布隆过滤器 | Guava BloomFilter |
| ID 生成 | 雪花算法（Snowflake）+ Base62 |
| 构建 | Maven 多模块（包含 Wrapper） |
| 限流 | Guava RateLimiter + 自定义 @RateLimit 注解 |
| 异步 | Spring @Async |

- **Jakarta EE 命名空间**：Spring Boot 4.x 使用 `jakarta.*` 包名（如 `jakarta.persistence.*`、`jakarta.servlet.http.*`），不要使用旧的 `javax.*`
- **ddl-auto=update**：开发环境用 update 自动建表，生产环境需改为 none/validate 并配合 Flyway
- **Lombok**：已通过 `@Data`、`@NoArgsConstructor` 规范化实体类
- **短码生成**：雪花算法生成唯一 Long → Base62 编码（0-9 a-z A-Z），无需碰撞重试
- **Redis 序列化**：手动配置 `StringRedisSerializer`（非默认 JDK 序列化），缓存 TTL 1 小时
- **RateLimitInterceptor**：非 `@Component`，由 `WebMvcConfig.addInterceptors()` 手动 new 并注册

## 当前局限（已知待改进）

- 无 API 文档（Swagger/OpenAPI）
- 无 Docker 容器化部署

完整升级计划见 `TODO.md`，按四个阶段逐步推进。
