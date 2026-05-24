# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# 构建（在 shortlink-core 目录下）
./mvnw clean package

# 跳过测试构建
./mvnw clean package -DskipTests

# 运行测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ShortlinkCoreApplicationTests

# 启动应用
./mvnw spring-boot:run
```

项目需要本地 MySQL 运行在 `localhost:3306`（数据库名 `shortlink_db`，用户名 `root`，密码见 `application.properties`）和 Redis 运行在 `localhost:6379`（密码见 `application.properties`）。表结构由 Hibernate `ddl-auto=update` 自动管理。

## 架构

单模块 Maven 项目（`shortlink-core`），分层如下：

```
Controller (REST) → Service (业务逻辑) → Repository (Spring Data JPA) → Entity (JPA 映射 MySQL)
     ↑                    ↓                           ↓
 @Validated       SnowflakeIdGenerator           Redis Cache
 校验参数         (雪花算法生成唯一ID)            (@Cacheable)
     │                    ↓                           │
     │           Base62Encoder.encode(id)             │
     │           (62进制编码为短码)                   │
     │                    │                           │
     │            BloomFilterService                  │
     │          (缓存穿透防御 + 懒加载缓存)            │
     └──────────────────┬─────────────────────────────┘
                        │
                 GlobalExceptionHandler
              (@RestControllerAdvice 统一异常处理 → Result<T> 统一响应)
```

- **入口**：`ShortlinkCoreApplication`（`@SpringBootApplication` + `@EnableCaching`）
- **两个 API 端点**：
  - `POST /shorten?url=<longUrl>` → 雪花 ID → Base62 短码 → 存库 + 布隆过滤器同步 → 返回 `Result<String>`
  - `GET /{shortCode}` → Redis 缓存 → 布隆过滤器 → MySQL → 302 重定向
- **数据库**：单表 `short_link`（id, short_code, long_url, create_time），`short_code` 有 unique 约束

## 技术栈与注意点

| 项 | 版本/值 |
|----|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| 数据库 | MySQL + JPA/Hibernate |
| 缓存 | Redis + Spring Cache |
| 布隆过滤器 | Guava BloomFilter |
| ID 生成 | 雪花算法（Snowflake）+ Base62 |
| 构建 | Maven（包含 Wrapper） |

- **Jakarta EE 命名空间**：Spring Boot 4.x 使用 `jakarta.*` 包名（如 `jakarta.persistence.*`、`jakarta.servlet.http.*`），不要使用旧的 `javax.*`
- **ddl-auto=update**：开发环境用 update 自动建表，生产环境需改为 none/validate 并配合 Flyway
- **Lombok**：已通过 `@Data`、`@NoArgsConstructor` 规范化实体类
- **短码生成**：雪花算法生成唯一 Long → Base62 编码（0-9 a-z A-Z），无需碰撞重试
- **Redis 序列化**：手动配置 `StringRedisSerializer`（非默认 JDK 序列化），缓存 TTL 1 小时
- **Alibaba Maven 镜像**：IDE 配置了 `maven.aliyun.com` 加速依赖下载

## 当前局限（已知待改进）

- 无短链访问统计（点击日志、UV/PV）
- 无限流保护（API 防刷）
- 无 API 文档（Swagger/OpenAPI）
- 无 Docker 容器化部署
- 单模块结构（后续可拆分为 common/core/cache/admin）

完整升级计划见 `TODO.md`，按四个阶段逐步推进。
