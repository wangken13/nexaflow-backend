# NexaFlow 部署与运维手册

## 1. 上线前

1. 从 `.env.prod.example` 创建仅服务器可读的 `.env.prod`，权限设为 `600`。
2. JWT、集成主密钥、Nacos、MySQL、Redis、RabbitMQ、MinIO 和支付回调分别使用独立随机密钥。
3. 只开放 `80/443`，数据库、中间件和业务服务仅在 Compose 网络内通信。
4. 配置有效域名证书，并确认 `AUTH_COOKIE_SECURE=true`。

## 2. 一键部署

```bash
cd /opt/nexaflow/tradeflow-ai-backend
chmod +x infra/scripts/*.sh
sh infra/scripts/deploy.sh
```

脚本依次等待 MySQL、Nacos、前端容器健康，并通过 `/readyz` 验证 Nginx 到网关的真实链路。

## 3. 上线验收

```bash
sh infra/scripts/smoke-test.sh https://www.example.com
node infra/scripts/load-test.mjs https://www.example.com/healthz 500 20 500
docker compose --env-file infra/.env.prod -f infra/docker-compose.prod.yml ps
```

性能脚本参数依次是 URL、请求数、并发数和允许的 P95 毫秒数。业务接口性能应使用脱敏测试租户单独压测。

## 4. 监控和告警

- `/actuator/health/liveness`：进程存活。
- `/actuator/health/readiness`：服务就绪。
- `/actuator/prometheus`：JVM、HTTP、线程、连接池和进程指标。
- 重点告警：5xx 比例、P95 延迟、JVM 堆、数据库连接池、RabbitMQ 堆积/死信、磁盘、备份失败和证书到期。
- Actuator 不通过公网直接暴露；外部只暴露返回最少信息的 `/readyz`。

## 5. 备份与恢复

```bash
sh infra/scripts/backup.sh
ENV_FILE=infra/.env.prod sh infra/scripts/restore.sh /opt/nexaflow/backups/<时间戳> --confirm
```

备份包含 `trade_ai`、`nacos_config` 和 MinIO 业务桶，并生成 SHA-256 校验。建议每天备份、保留 14 天，并复制到独立存储。每月至少在隔离环境恢复一次。

## 6. 发布与回滚

1. 每次发布设置不可变 `IMAGE_TAG`，保留上一版本镜像。
2. 发布前执行 Flyway 备份；数据库迁移只前进，不修改已发布脚本。
3. 应用异常时将 `IMAGE_TAG` 改回上一版本并重新 `docker compose up -d`。
4. 只有确认迁移不向后兼容时才执行数据恢复，并先关闭业务写流量。

## 7. 故障降级

- AI 不可用：保留人工录入、询盘、报价和订单流程，返回明确的稍后重试提示。
- 短信不可用：保留账号密码登录，不返回虚假“已发送”。
- RabbitMQ 短时不可用：业务事件进入 Outbox，恢复后重试；消费者幂等。
- 邮箱不可用：记录同步错误，不影响手工询盘录入。

## 8. 首期服务目标

- 月可用性目标：99.9%（正式承诺以合同为准）。
- 严重故障响应目标：30 分钟内响应。
- 数据恢复目标：RPO 24 小时以内，RTO 4 小时以内；启用更高频备份后可收紧。
