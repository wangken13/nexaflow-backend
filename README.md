# NexaFlow Backend

Java 微服务后端，支撑企业客户协同平台的认证、租户、客户、需求、AI 分析、报价、订单、任务、通知和文件能力。

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Cloud Alibaba
- Spring Cloud Gateway
- Nacos
- Sentinel
- Spring AI
- MyBatis-Plus
- MySQL 8
- Redis
- RabbitMQ
- MinIO

## Services

- `gateway-service`: API 网关、JWT 校验、路由
- `auth-service`: 登录注册、JWT
- `tenant-service`: 租户与套餐
- `customer-service`: 客户管理
- `inquiry-service`: 询盘录入与 AI 分析事件
- `ai-service`: Spring AI 模型封装与降级分析
- `quotation-service`: 报价草稿
- `order-service`: 订单与风险识别
- `task-service`: 跟进任务与日报
- `notification-service`: 站内通知
- `file-service`: 文件上传与 MinIO 接入点
- `product-service`: 产品目录、价格与批量导入
- `aigc-service`: 独立 AIGC 能力接入

## Production Delivery

- 采购验收矩阵：[`docs/CUSTOMER_ACCEPTANCE.md`](docs/CUSTOMER_ACCEPTANCE.md)
- 系统使用手册：[`docs/SYSTEM_MANUAL.md`](docs/SYSTEM_MANUAL.md)
- 部署运维手册：[`docs/OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md)

## Local Infrastructure

```bash
cd infra
docker compose up -d
```

## Build

需要 JDK 21 和 Maven。

```bash
mvn clean test
```

生产编排校验、部署、冒烟与备份：

```bash
docker compose --env-file infra/.env.prod -f infra/docker-compose.prod.yml config --quiet
sh infra/scripts/deploy.sh
sh infra/scripts/smoke-test.sh https://www.example.com
sh infra/scripts/backup.sh
```

## Run

```bash
mvn -pl auth-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

其他服务按需启动。
