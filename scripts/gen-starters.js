// 生成 6 个启动模块（admin/channel/agent/client/supplier/distributor）
// 运行：node scripts/gen-starters.js
const fs = require('fs');
const path = require('path');

const SERVER_ROOT = path.join(__dirname, '..', 'dayan-server');
const STARTERS_ROOT = path.join(SERVER_ROOT, 'dayan-starters');

// 6 端配置：端口 / context-path / 业务模块依赖（按规格 §4.10）
const STARTERS = [
  {
    dir: 'dayan-admin', pkg: 'admin', cn: 'Admin 运营端', port: 8080, ctx: '/admin-api',
    modules: ['system', 'organ', 'channel', 'agent', 'client', 'equity', 'service', 'order',
              'finance', 'park', 'supplier', 'butler', 'goods', 'scene', 'content', 'course', 'distributor'],
  },
  {
    dir: 'dayan-channel', pkg: 'channel', cn: 'Channel 渠道端', port: 8081, ctx: '/channel-api',
    modules: ['system', 'organ', 'channel', 'agent', 'client', 'park', 'goods', 'scene', 'content'],
  },
  {
    dir: 'dayan-agent', pkg: 'agent', cn: 'Agent 代理人端', port: 8082, ctx: '/agent-api',
    modules: ['system', 'organ', 'agent', 'client', 'equity', 'service', 'goods', 'scene', 'content', 'course'],
  },
  {
    dir: 'dayan-client', pkg: 'client', cn: 'Client 客户端', port: 8083, ctx: '/client-api',
    modules: ['system', 'organ', 'agent', 'client', 'equity', 'service', 'goods', 'scene', 'content', 'course'],
  },
  {
    dir: 'dayan-supplier', pkg: 'supplier', cn: 'Supplier 供应商端（预留）', port: 8084, ctx: '/supplier-api',
    modules: ['system', 'organ', 'supplier', 'park', 'goods', 'scene'],
  },
  {
    dir: 'dayan-distributor', pkg: 'distributor', cn: 'Distributor 分销商端（预留）', port: 8085, ctx: '/distributor-api',
    modules: ['system', 'organ', 'distributor', 'channel', 'park', 'goods'],
  },
];

function deps(modules) {
  return modules.map(m =>
    `        <dependency>\n            <groupId>com.dayan</groupId>\n            <artifactId>dayan-module-${m}</artifactId>\n        </dependency>`
  ).join('\n');
}

function pomXml(s) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.dayan</groupId>
        <artifactId>dayan-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>${s.dir}</artifactId>
    <name>${s.dir}</name>
    <description>${s.cn}启动模块</description>

    <dependencies>
${deps(s.modules)}
        <!-- 公共基础设施 -->
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-core</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-redis</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-mybatis</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-security</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-log</artifactId></dependency>
        <dependency><groupId>com.dayan</groupId><artifactId>dayan-common-swagger</artifactId></dependency>

        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    </dependencies>

    <build>
        <finalName>${s.dir}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.dayan.${s.pkg}.Dayan${titleCase(s.pkg)}Application</mainClass>
                </configuration>
                <executions>
                    <execution><goals><goal>repackage</goal></goals></execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
`;
}

function titleCase(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

function appJava(s) {
  const cls = `Dayan${titleCase(s.pkg)}Application`;
  return `package com.dayan.${s.pkg};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ${s.cn}启动类（端口 ${s.port}，context-path ${s.ctx}）。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ${cls} {

    public static void main(String[] args) {
        SpringApplication.run(${cls}.class, args);
    }
}
`;
}

function appYml(s) {
  return `server:
  port: ${s.port}
  servlet:
    context-path: ${s.ctx}

spring:
  application:
    name: ${s.dir}
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://\${MYSQL_HOST:127.0.0.1}:\${MYSQL_PORT:3306}/dayan?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: \${MYSQL_USER:root}
    password: \${MYSQL_PASSWORD:root123}
    type: com.alibaba.druid.pool.DruidDataSource
  data:
    redis:
      host: \${REDIS_HOST:127.0.0.1}
      port: \${REDIS_PORT:6379}
      database: 0
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
  cloud:
    nacos:
      discovery:
        server-addr: \${NACOS_ADDR:127.0.0.1:8848}

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

sa-token:
  token-name: Authorization
  timeout: 7200
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false

springdoc:
  swagger-ui:
    path: /swagger-ui.html
knife4j:
  enable: true
  setting:
    language: zh_cn

management:
  endpoints:
    web:
      exposure:
        include: health,info

logging:
  level:
    com.dayan: DEBUG
    org.springframework: INFO
`;
}

let count = 0;
for (const s of STARTERS) {
  const dir = path.join(STARTERS_ROOT, s.dir);
  const javaDir = path.join(dir, 'src/main/java', 'com/dayan', s.pkg);
  const resDir = path.join(dir, 'src/main/resources');
  fs.mkdirSync(javaDir, { recursive: true });
  fs.mkdirSync(resDir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'pom.xml'), pomXml(s));
  fs.writeFileSync(path.join(javaDir, `Dayan${titleCase(s.pkg)}Application.java`), appJava(s));
  fs.writeFileSync(path.join(resDir, 'application.yml'), appYml(s));
  count++;
  console.log(`generated ${s.dir} (port ${s.port}, ${s.modules.length} modules)`);
}
console.log(`Done: ${count} starters generated.`);
