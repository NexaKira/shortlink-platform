# 短链平台升级计划

## 第一阶段：工程化基础（必做）

### 1. 统一响应格式 + 全局异常处理
- 封装泛型 `Result<T>` 响应类，包含 code、message、data 三个字段
- 使用 `@RestControllerAdvice` + `@ExceptionHandler` 统一拦截异常
- 消除 Tomcat 默认错误页 JSON，所有返回统一格式
- **涉及知识点**：`@RestControllerAdvice`、`@ExceptionHandler`、泛型封装

### 2. 参数校验
- Controller 层加入 `@Validated` / `@Valid`
- `@RequestParam` 加 `@NotBlank`、`@URL` 校验
- 处理 `MethodArgumentNotValidException` 全局异常
- **涉及知识点**：JSR-380 Bean Validation、Spring 校验框架

### 3. 配置外部化
- 把硬编码的 `http://localhost:8080/` 提取到 `application.properties` / `application.yml`
- 使用 `@Value` 或 `@ConfigurationProperties` 注入
- **涉及知识点**：Spring 配置管理、`@Value`、`@ConfigurationProperties`

### 4. Lombok 规范化
- `ShortLink` 实体改用 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`
- 删除手写 getter/setter/无参构造，保持代码简洁
- **涉及知识点**：Lombok 常用注解、编译期注解处理

---

## 第二阶段：性能与可靠性（加分项）

### 5. Redis 缓存热点短链
- 引入 `spring-boot-starter-data-redis`
- `getLongUrl` 加 `@Cacheable`，`createShortLink` 加 `@CachePut`
- 配置 Redis 序列化（Jackson2JsonRedisSerializer）
- **涉及知识点**：Spring Cache 抽象、`@Cacheable`、`@CacheEvict`、缓存穿透/击穿/雪崩概念

### 6. 缓存穿透防御 —— 布隆过滤器
- 使用 Guava `BloomFilter` 或 Redisson `RBloomFilter`
- 在查 Redis/MySQL 之前做第一道存在性过滤
- 初始化时从数据库加载已有短码到布隆过滤器
- **涉及知识点**：布隆过滤器原理、缓存穿透的三种防御手段对比

### 7. 分布式 ID 生成 + Base62 编码
- 替代纯随机短码，改用雪花算法（Snowflake）生成唯一 ID
- 将 Long 类型 ID 通过 Base62 编码转为 6-8 位短码
- 比较雪花算法 vs 号段模式（美团 Leaf）的优劣
- **涉及知识点**：雪花算法原理、Base62 编码、分布式 ID 方案对比

---

## 第三阶段：深度扩展（亮点项）

### 8. 短链访问统计
- 新建 `click_log` 表（short_code、visitor_ip、user_agent、referer、click_time）
- 每次重定向通过 `@Async` 异步写入日志，不阻塞主流程
- 提供 `/stats/{shortCode}` 接口查询访问次数
- **涉及知识点**：`@Async`、`ThreadPoolTaskExecutor`、AOP 切面

### 9. 限流保护
- 使用 Guava `RateLimiter` 对 `/shorten` 接口限流
- 通过 `HandlerInterceptor` 拦截器 + 注解实现声明式限流
- **涉及知识点**：令牌桶/漏桶算法、`HandlerInterceptor`、自定义注解

### 10. 多模块拆分
```
shortlink-platform/
├── shortlink-common/      # 公共工具类、统一响应、异常定义
├── shortlink-core/        # 核心业务逻辑
├── shortlink-cache/       # Redis/缓存相关
└── shortlink-admin/       # 后台统计管理（新增）
```

---

## 第四阶段：部署与文档

### 11. Docker 容器化部署
- 编写 `Dockerfile`（多阶段构建）
- 编写 `docker-compose.yml`（MySQL + Redis + 应用）
- **涉及知识点**：Docker 镜像构建、Docker Compose 编排

### 12. API 文档
- 集成 SpringDoc OpenAPI（Swagger UI）
- 自动生成在线接口文档和调试页面
- **涉及知识点**：OpenAPI 3.0 规范、Swagger 注解

---

## 进度记录

| 日期 | 完成项 | 备注 |
|------|--------|------|
| - | - | - |

---

## 技术栈总览

| 层级 | 当前 | 目标 |
|------|------|------|
| 框架 | Spring Boot 4.0.6 | Spring Boot 4.0.6 |
| 语言 | Java 21 | Java 21 |
| 数据库 | MySQL | MySQL |
| 缓存 | 无 | Redis |
| ORM | JPA / Hibernate | JPA / Hibernate |
| 校验 | 无 | Bean Validation |
| 限流 | 无 | Guava RateLimiter / Redisson |
| 文档 | 无 | SpringDoc OpenAPI |
| 部署 | 本地 IDE | Docker Compose |
| 构建 | Maven | Maven |
