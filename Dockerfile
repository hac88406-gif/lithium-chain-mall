# syntax=docker/dockerfile:1.6
# ==========================================================================
# 后端 Spring Boot Dockerfile（多阶段 + BuildKit 依赖缓存）
# 第一次构建：下载 200+ 依赖 jar（慢，约 3-5 分钟）
# 之后构建：直接复用 BuildKit 缓存的 /root/.m2，秒下载 ✅
# ==========================================================================

# ---- Stage 1: Maven 编译 ----
# 注意：pom.xml 中 java.version=17，构建镜像必须用 JDK 17（原为 JDK 8，无法编译）
FROM maven:3.8-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom.xml 让 Docker 层缓存生效（pom 没变就不重新执行这一层）
COPY pom.xml .

# 预下载依赖（--mount=type=cache 让 /root/.m2 持久化到 Docker BuildKit 缓存）
# 效果等同于挂载本地 ~/.m2/repository，但跨容器更干净
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 再复制源码并打包
# 注意：application-local.yml 含真实密钥，已在 .dockerignore 中排除，不会被打进镜像
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# ---- Stage 2: 仅运行 jar（jre 比 jdk 小很多）----
# 必须与 java.version=17 一致，否则运行时报 UnsupportedClassVersionError
FROM eclipse-temurin:17-jre

WORKDIR /app

# 创建上传目录
RUN mkdir -p /app/uploads

# 从 builder 阶段复制打包好的 jar
COPY --from=builder /build/target/green-chain-procurement-1.0.0.jar app.jar

# 时区设为中国（默认 UTC，日志时间会差 8 小时）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Spring Boot 端口
EXPOSE 8080

# 启动
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/dump.hprof", \
  "-jar", "/app/app.jar"]
