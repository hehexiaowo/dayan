# 大雁养老后端服务统一 Dockerfile 模板
# 用法：docker build -f docker/Dockerfile -t dayan-<svc> --build-arg JAR_FILE=dayan-<svc>.jar .
# 各服务 jar 由 spring-boot-maven-plugin 生成于各自 target/ 目录

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="dayan"

ARG JAR_FILE=dayan-admin.jar
WORKDIR /app

# 时区
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制 jar（构建上下文为仓库根，jar 路径需含模块相对路径）
COPY ${JAR_FILE} app.jar

# JVM 参数：容器感知，限制堆内存
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
