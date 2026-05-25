# 第一阶段
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
# 将本地的多模块源码复制到容器中
COPY . .
# 执行Maven打包，跳过测试
RUN mvn clean package -DskipTests

# 第二阶段：最小化运行环境
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 从第一阶段中，把打包好的可独立运行的 Jar 包复制过来
COPY --from=builder /app/shortlink-core/target/*.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]