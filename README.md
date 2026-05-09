# MyShortLink 短链接系统

## 项目简介
MyShortLink 是一个基于 **Spring Boot 3**、**Spring Cloud** 的分布式短链接平台，提供短链生成、跳转、统计、分组管理以及回收站等功能。项目采用微服务架构，使用 **Nacos** 进行服务发现与配置中心，**Sentinel** 进行流量防护，**RocketMQ** 负责异步统计，**Redis** 实现缓存与分布式锁，**ShardingSphere** 完成数据库分片，**MyBatis‑Plus**、**Lombok**、**Knife4j** 等提升开发效率。

## 项目结构
- **admin** 模块 `[\`admin\`]`([`admin/pom.xml`](admin/pom.xml:1))
  - 入口类：[`ShortLinkAdminApplication`](admin/src/main/java/org/yilena/myShortLink/admin/ShortLinkAdminApplication.java:1)
  - 主要职责：后台管理接口、用户、分组、短链管理等
  - 配置文件：[`admin/src/main/resources/application.yaml`](admin/src/main/resources/application.yaml:1)

- **gateway** 模块 `[\`gateway\`]`([`gateway/pom.xml`](gateway/pom.xml:1))
  - 入口类：[`GatewayServiceApplication`](gateway/src/main/java/org/yilena/myShortLink/gateway/GatewayServiceApplication.java:1)
  - 主要职责：统一入口、路由转发、限流防护

- **project** 模块 `[\`project\`]`([`project/pom.xml`](project/pom.xml:1))
  - 入口类：[`ShortLinkApplication`](project/src/main/java/org/yilena/myShortLink/project/ShortLinkApplication.java:1)
  - 业务实现：[`ShortLinkServiceImpl`](project/src/main/java/org/yilena/myShortLink/project/service/impl/ShortLinkServiceImpl.java:1)（实现 `ShortLinkService` 接口）
  - 配置文件：[`project/src/main/resources/application.yaml`](project/src/main/resources/application.yaml:1)
  - 关键控制层：[`ShortLinkController`](project/src/main/java/org/yilena/myShortLink/project/controller/ShortLinkController.java:1)

- **aggregation** 模块 `[\`aggregation\`]`([`aggregation/pom.xml`](aggregation/pom.xml:1))
  - 负责聚合各子模块的公共依赖与统一构建配置（目前为空实现，仅保留占位）

## 技术栈
- **语言/平台**：Java 21, Spring Boot 3.0.7
- **微服务**：Spring Cloud Alibaba (Nacos, Sentinel, OpenFeign)
- **数据库**：MySQL + ShardingSphere-JDBC（分库分表）
- **缓存/锁**：Redis + Redisson
- **消息队列**：RocketMQ
- **ORM**：MyBatis‑Plus
- **工具库**：Lombok, Hutool, Knife4j (Swagger), FastJSON, EasyExcel
- **构建**：Maven 多模块 (`pom.xml` 位于根目录，声明四个子模块)

## 快速开始
### 1. 环境准备
| 组件 | 版本 | 备注 |
|------|------|------|
| JDK | 21 | 必须 | 
| Maven | 3.9+ | 必须 |
| MySQL | 8.x | 用于持久化短链数据 |
| Redis | 6.x+ | 用于缓存、分布式锁 |
| Nacos | 2.x | 注册中心 & 配置中心 |
| RocketMQ | 5.x | 异步统计 |

确保以上服务已启动并在 `application.yaml` 中配置相应地址（例如 `spring.datasource.url`、`spring.redis.host`、`spring.cloud.nacos.discovery.server-addr` 等）。

### 2. 构建项目
```bash
# 克隆仓库（已在本地）
cd F:/YilenaCode/MyShortLink
# 编译并打包所有模块
mvn clean install -DskipTests
```
编译完成后，各模块的可执行 jar 位于 `target` 目录，例如 `admin/target/admin-1.0-SNAPSHOT.jar`、`gateway/target/gateway-1.0-SNAPSHOT.jar`、`project/target/project-1.0-SNAPSHOT.jar`。

### 3. 运行服务
```bash
# 运行网关（默认端口 80）
java -jar gateway/target/gateway-*.jar

# 运行短链接核心服务（默认端口 8002）
java -jar project/target/project-*.jar

# 运行后台管理服务（默认端口 8000）
java -jar admin/target/admin-*.jar
```
> 若使用 IDE（IntelliJ IDEA/Eclipse），直接运行相应的 `main` 方法亦可。

### 4. 接口文档
服务启动后，可通过 Swagger UI（Knife4j）访问接口文档：
- **网关**：`http://localhost:80/doc.html`
- **短链接服务**：`http://localhost:8002/doc.html`
- **后台管理**：`http://localhost:8000/doc.html`

## 主要功能概览
- **短链生成**（普通、分布式锁、批量）
- **短链跳转**（防缓存穿透、统计 PV/UV、访问设备/网络信息）
- **短链分组管理**（查询分组内短链数量）
- **回收站**（软删、恢复）
- **限流防护**（Sentinel + Redis）
- **分布式锁**（Redisson）
- **异步统计**（RocketMQ）

## 单元测试
项目已集成 Spring Boot Test 与 MyBatis‑Plus Test，运行方式：
```bash
mvn test -Dtest=*
```
可根据需要编写或补充测试用例。
