# NexaFlow 系统使用与技术手册

## 技术架构

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus、Axios、Nginx |
| 后端 | Java 21、Spring Boot 3.5、Spring Cloud Gateway、Spring Cloud Alibaba、Spring AI、JDBC/MyBatis-Plus |
| 数据与中间件 | MySQL 8.4、Redis 7.4、RabbitMQ 3.13、MinIO、Nacos 2.4.3 |
| 安全 | BCrypt、JWT 访问令牌、HttpOnly 刷新 Cookie、RBAC、数据范围、HMAC Webhook、限流、审计 |
| 交付 | Maven Monorepo、Flyway、Docker Compose、Nginx TLS、Actuator、Prometheus 指标 |

## 业务操作

1. 企业注册后创建独立租户与管理员。
2. 管理员导入产品和客户，邀请成员并配置部门、角色和数据范围。
3. 通过企业邮箱、签名 Webhook 或手工录入汇集询盘。
4. AI 根据企业知识库提炼需求并附来源；人员确认回复和报价草稿。
5. 报价命中折扣、利润或重要客户规则时进入审批。
6. 报价通过后创建订单，系统跟踪交期、风险、任务和通知。
7. 管理者查看转化、响应效率、AI 使用和订阅用量。

## 数据关系

- 所有核心业务表包含 `tenant_id`，使用逻辑外键和组合索引保持隔离与性能。
- 客户关联联系人、跟进和询盘；询盘关联 AI 分析、报价和任务；报价关联审批记录与订单。
- 部门、成员、角色权限和数据范围决定查询边界；审计表独立记录关键操作。
- 导入任务、集成调用、订阅订单、支持工单和演示数据追踪均独立建表，避免污染核心实体。

## 设计模式与解耦

- Strategy：客户数据范围、敏感字段脱敏、短信和支付渠道策略。
- Adapter：Spring AI 模型、IMAP 邮箱、MinIO、支付和渠道 Webhook。
- Repository/Store：业务服务依赖存储接口，JDBC 与内存测试实现可替换。
- Outbox：本地事务提交业务和事件，调度发布到 RabbitMQ。
- Template/Policy：报价审批规则、签名校验和统一权限策略。
- Facade：Controller 只负责协议适配，业务编排位于 Service。

## 安全原则

- 浏览器不持久化刷新令牌，生产 Cookie 使用 `HttpOnly + Secure + SameSite`。
- 网关清理外部伪造身份头，业务服务校验内部身份和权限；生产只公开 Nginx/网关入口。
- SQL 使用参数绑定，排序和动态表名使用白名单；文件限制类型、大小和租户对象路径。
- 验证码、登录、短信和集成接口同时做服务端分布式限流；前端限制重复点击只改善体验，不作为安全边界。

## 管理入口

- 工作台首页：开通进度、经营指标和待处理事项。
- 企业治理：成员、部门、权限、知识库、渠道、集成调用、订阅和审计。
- 帮助与支持：操作指南、服务承诺、支持工单及处理记录。
- 公开 `/openapi.yaml`：入站询盘签名接口契约。
