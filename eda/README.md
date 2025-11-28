EDA quick spec (concise)

- Topic命名: `<boundedContext>.<eventName>.v<major>`，示例 `orders.created.v1`
- 事件规范: JSON Schema 存放于 `eda/schemas`；仅向后兼容演进（可选字段）
- 版本策略: 大版本并行（vN）；小改动不破坏旧消费者
- 可靠性: 每个主题配套 `<topic>.retry` 与 `<topic>.dlq`；消费者实现退避重试与 DLQ 转储/重放
- 追踪: 头部携带 `traceId` `spanId` `eventId` `producer` `timestamp`

