# 短链平台升级计划

## 第一阶段：工程化基础（必做）

### 1. 统一响应格式 + 全局异常处理 ✅
- [x] 封装泛型 `Result<T>` 响应类，包含 code、message、data 三个字段
- [x] 使用 `@RestControllerAdvice` + `@ExceptionHandler` 统一拦截异常
- [x] 消除 Tomcat 默认错误页 JSON，所有返回统一格式
- **涉及知识点**：`@RestControllerAdvice`、`@ExceptionHandler`、泛型封装

### 2. 参数校验 ✅
- [x] Controller 层加入 `@Validated`
- [x] `@RequestParam` 加 `@NotBlank`、`@URL` 校验
- [x] 添加 `spring-boot-starter-validation` 依赖
- **涉及知识点**：JSR-380 Bean Validation、Spring 校验框架

### 3. 配置外部化 ✅
- [x] 把硬编码的 `http://localhost:8080/` 提取到 `application.properties`
- [x] 使用 `@Value` 注入
- [x] 同步更新 `application-template.properties`
- **涉及知识点**：Spring 配置管理、`@Value`、`@ConfigurationProperties`

### 4. Lombok 规范化 ✅
- [x] `ShortLink` 实体改用 `@Data`、`@NoArgsConstructor`
- [x] 删除手写 getter/setter/无参构造
- **涉及知识点**：Lombok 常用注解、编译期注解处理

### 附：自定义业务异常 ✅
- [x] 创建 `BusinessException(int code, String message)` 区分 4xx / 5xx 错误
- [x] 404 → 短链不存在，500 → 短码生成失败（服务端问题）
- **涉及知识点**：HTTP 状态码语义、异常继承体系

---

## 第二阶段：性能与可靠性（加分项）

### 5. Redis 缓存热点短链 ✅
- [x] 引入 `spring-boot-starter-data-redis`
- [x] `getLongUrl` 加 `@Cacheable(value="shortlink", key="#shortCode")`，懒加载缓存策略
- [x] `RedisConfig` 手动配置 `RedisCacheManager`（StringRedisSerializer + TTL）
- [x] 新增 Redis 连接配置（`application-template.properties` 同步更新）
- **涉及知识点**：Spring Cache 抽象、`@Cacheable`、SpEL 表达式、`@Configuration` + `@Bean`、序列化策略

### 6. 缓存穿透防御 —— 布隆过滤器 ✅
- [x] 使用 Guava `BloomFilter<String>`
- [x] `@PostConstruct` 启动时从数据库加载已有短码初始化
- [x] `getLongUrl` 方法体中，在查 MySQL 之前做布隆过滤器拦截
- [x] `createShortLink` 创建后同步 `put` 新短码到布隆过滤器
- **涉及知识点**：布隆过滤器原理（位数组 + 多哈希）、缓存穿透 vs 击穿 vs 雪崩、`@PostConstruct` Bean 生命周期

### 7. 分布式 ID 生成 + Base62 编码 ✅
- [x] `SnowflakeIdGenerator`：41bit 时间戳 + 10bit 机器ID + 12bit 序列号，位运算拼接
- [x] 处理时钟回拨异常、同一毫秒序列号溢出自旋
- [x] `Base62Encoder`：0-9 a-z A-Z 六十二进制编码解码
- [x] `createShortLink` 去掉碰撞重试逻辑——雪花 ID 保证唯一
- **涉及知识点**：雪花算法原理、位运算（移位/掩码/OR拼接）、进制转换算法、发号器 vs 随机串优劣

---

## 第三阶段：深度扩展（亮点项）

### 8. 短链访问统计 ✅
- [x] 新建 `ClickLog` 实体（click_log 表，含 short_code、visitor_ip、user_agent、referer、click_time）
- [x] `ClickLogService.logClick()` 用 `@Async` 异步写入，不阻塞 302 跳转
- [x] `@EnableAsync` 开启异步支持
- [x] Controller 从 `HttpServletRequest` 提取 IP/UA/Referer，经 Service 层委托写入
- [x] 提供 `GET /stats/{shortCode}` 接口查询点击次数
- **涉及知识点**：`@Async`、`@EnableAsync`、分层架构（Controller 不碰 Repository）、`HttpServletRequest`

### 9. 限流保护 ✅
- [x] 使用 Guava `RateLimiter` 对 `/shorten` 接口限流
- [x] 通过 `HandlerInterceptor` 拦截器 + 自定义 `@RateLimit` 注解实现声明式限流
- [x] `ConcurrentHashMap<String, RateLimiter>` 按方法名隔离限流器
- **涉及知识点**：令牌桶/漏桶算法、`HandlerInterceptor`、自定义注解、`computeIfAbsent`

### 10. 多模块拆分 ✅
- [x] Root POM（packaging=pom）统一管理子模块
- [x] `shortlink-common`：公共工具类（Result、BusinessException、RateLimit 注解、Base62Encoder）
- [x] `shortlink-core`：核心业务逻辑（依赖 common）
- [x] package 命名规范（com.sc.shortlinkcommon / com.sc.shortlinkcore）
- **涉及知识点**：Maven 多模块、父子 POM 继承、依赖传递、provided scope 不传递

---

## 第四阶段：部署与文档

### 11. Docker 容器化部署 ✅
- [x] 编写 `Dockerfile`（多阶段构建：Maven 编译 → JRE 运行）
- [x] 编写 `docker-compose.yml`（MySQL 8.0 + Redis 7 + 应用）
- [x] `application-docker.properties` Docker 环境配置
- [x] Spring profiles 切换（docker profile）
- **涉及知识点**：Docker 镜像构建、多阶段构建、Docker Compose 编排、容器网络、数据卷

### 12. API 文档 ✅
- [x] 集成 SpringDoc OpenAPI（Swagger UI）
- [x] 访问 `http://localhost:8080/swagger-ui.html` 在线调试
- **涉及知识点**：OpenAPI 3.0 规范、SpringDoc 自动配置

---

## 第五阶段：高并发 & 分布式（含金量核心）

### 13. RocketMQ 异步削峰 —— 替换 @Async

- [ ] 引入 `rocketmq-spring-boot-starter` 依赖
- [ ] `ClickLogService` 拆分为 Producer（Controller → MQ）和 Consumer（MQ → MySQL）
- [ ] Consumer 批量写入优化（攒批 + 定时 flush），替代逐条 INSERT
- [ ] 消息幂等：短码 + 时间戳做唯一键，`INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE`
- [ ] 消费失败重试 + 死信队列（DLQ），兜底不丢数据
- [ ] `docker-compose.yml` 新增 RocketMQ 容器（NameServer + Broker）
- **涉及知识点**：异步消息削峰填谷、消息幂等、消费重试与死信队列、Producer/Consumer 解耦

### 14. Redis 分布式限流 —— 替换 Guava RateLimiter

- [ ] 重写 `@RateLimit` 注解，新增 `type` 属性（IP / 全局限流）
- [ ] `RateLimitInterceptor` 改用 Redis Lua 脚本实现滑动窗口算法
- [ ] Lua 脚本原子性保证：key = `rate_limit:{method}:{ip/window}`，INCR + EXPIRE 一条命令
- [ ] `ConcurrentHashMap` 降级为本地兜底（Redis 不可用时回退到 Guava）
- [ ] 支持按 IP 限流（防单用户刷接口）和全局限流（防总量打爆）
- **涉及知识点**：滑动窗口算法、Redis Lua 原子脚本、分布式限流 vs 单机限流、降级策略

### 15. 雪花算法 WorkerId 动态分配

- [ ] 启动时通过 Redis `INCR` 原子获取 workerId（范围 0-1023）
- [ ] 定时心跳续约（每 5 秒 `SETEX worker:heartbeat:{id} 10 alive`）
- [ ] 时钟回拨优化：不再抛异常，改为短暂自旋等待 + WARN 日志告警
- [ ] 优雅关闭时释放 workerId（`DECR` 归还）
- **涉及知识点**：分布式 ID 生成、Redis 原子操作、时钟回拨处理策略、优雅关闭钩子

### 16. 缓存击穿保护 —— Redis 互斥锁

- [ ] `getLongUrl` 方法中，缓存未命中时先 `SETNX` 获取锁
- [ ] 获锁成功 → 查 MySQL → 回写 Redis → 释放锁
- [ ] 获锁失败 → 短暂自旋等待 → 再次尝试读缓存
- [ ] 锁超时自动释放（`SETEX` 防死锁），超时时间 5 秒
- **涉及知识点**：缓存击穿 vs 穿透 vs 雪崩、分布式锁、SETNX/SETEX、自旋等待

---

## 进度记录

| 日期 | 完成项 | 备注 |
|------|--------|------|
| 2026-05-21 | 第一阶段全部完成 | 统一响应格式、参数校验、配置外部化、Lombok、自定义业务异常 |
| 2026-05-22 | 第二阶段全部完成 | Redis缓存、布隆过滤器、雪花算法ID生成、Base62编码 |
| 2026-05-23 | 短链访问统计 | ClickLog异步记录、/stats接口、分层架构重构 |
| 2026-05-24 | 限流保护、多模块拆分 | RateLimiter声明式限流、common/core分离 |
| 2026-05-25 | Docker 容器化部署、API 文档 | 多阶段构建、docker-compose 编排、SpringDoc OpenAPI |

---

## 技术栈总览

| 层级 | 当前 | 目标 |
|------|------|------|
| 框架 | Spring Boot 4.0.6 | Spring Boot 4.0.6 |
| 语言 | Java 21 | Java 21 |
| 数据库 | MySQL | MySQL |
| 缓存 | Redis | Redis |
| ID 生成 | 随机 | 雪花算法 + Base62 |
| 布隆过滤器 | 无 | Guava BloomFilter |
| ORM | JPA / Hibernate | JPA / Hibernate |
| 校验 | Bean Validation | Bean Validation |
| 限流 | Guava RateLimiter | Redis Lua 滑动窗口 + @RateLimit |
| 消息队列 | 无 | RocketMQ（异步削峰 + 幂等消费） |
| WorkerId | 硬编码 1 | Redis 动态分配 + 心跳续约 |
| 缓存击穿 | 无 | Redis SETNX 互斥锁 |
| 文档 | SpringDoc OpenAPI | SpringDoc OpenAPI |
| 部署 | Docker Compose + RocketMQ | Docker Compose + RocketMQ |
| 构建 | Maven | Maven 多模块 |
