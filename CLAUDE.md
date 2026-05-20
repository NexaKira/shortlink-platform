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

项目需要本地 MySQL 运行在 `localhost:3306`，数据库名 `shortlink_db`，用户名 `root`，密码 `123456`。表结构由 Hibernate `ddl-auto=update` 自动管理。

## 架构

单模块 Maven 项目（`shortlink-core`），分层如下：

```
Controller (REST) → Service (业务逻辑) → Repository (Spring Data JPA) → Entity (JPA 映射 MySQL)
                     ↓
              ShortCodeGenerator (6 位随机短码工具)
```

- **入口**：`ShortlinkCoreApplication`（标准 `@SpringBootApplication`）
- **两个 API 端点**：
  - `POST /shorten?url=<longUrl>` → 生成长度为 6 的短码，返回完整短链
  - `GET /{shortCode}` → 查库后 302 重定向到原始长链接
- **数据库**：单表 `short_link`（id, short_code, long_url, create_time），`short_code` 有 unique 约束

## 技术栈与注意点

| 项 | 版本/值 |
|----|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| 数据库 | MySQL + JPA/Hibernate |
| 构建 | Maven（包含 Wrapper） |

- **Jakarta EE 命名空间**：Spring Boot 4.x 使用 `jakarta.*` 包名（如 `jakarta.persistence.*`、`jakarta.servlet.http.*`），不要使用旧的 `javax.*`
- **ddl-auto=update**：开发环境用 update 自动建表，生产环境需改为 none/validate 并配合 Flyway
- **Lombok 已配置但未使用**：`ShortLink` 仍手写 getter/setter，后续可改用 `@Data` 简化
- **Alibaba Maven 镜像**：IDE 配置了 `maven.aliyun.com` 加速依赖下载
- **短码生成**：`SecureRandom` 从 36 个字符（a-z + 0-9）中随机取 6 位，碰撞时最多重试 3 次

## 当前局限（已知待改进）

- `baseUrl` 硬编码为 `http://localhost:8080/`
- 无输入校验、无统一异常处理、无统一响应格式
- 无缓存、无限流、无访问统计

完整升级计划见 `TODO.md`，按四个阶段逐步推进。
