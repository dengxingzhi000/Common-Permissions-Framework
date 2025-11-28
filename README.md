组织介绍
💪CommonPermissionsFramework是基于SpringCloud2025的微服务开发平台，整合了Spring Security + Jwt、Springcloud Alibaba等组件。
包含了基础的RBAC权限管理、授权认证、网关管理、服务治理、审计日志、熔断限流等系统管理基础应用。
定义了相关开发规范、风格并落地在框架层，开箱即用，支持Docker、Kubenetes的部署。
让项目开发快速进入业务开发，而不需过多时间花费在架构搭建和编码风格规范上。
如何加入
请发送申请邮件至 dengxingzhi2015@gmail.com

运行指引（Nacos / Feign / Dubbo）
- 环境变量（可选，未设置则使用默认值）
  - `NACOS_SERVER`：Nacos 地址，默认 `127.0.0.1:8848`
  - `NACOS_NAMESPACE`：默认 `public`
  - `NACOS_GROUP`：默认 `DEFAULT_GROUP`
- 配置中心（Nacos Config）
  - 在 Nacos 中创建以下 Data ID（YAML 格式）：
    - `gateway-service.yaml`
    - `auth-service.yaml`
    - `system-service.yaml`
    - 可选公用：`common.yaml`
  - 各服务本地 `application.yaml` 已通过 `spring.config.import` 引入对应 Data ID 和 `common.yaml`
- 服务发现（Nacos Discovery）
  - 网关：`gateway-service`
  - 认证：`auth-service`
  - 系统：`system-service`
- 服务间通信
  - 核心高频调用：使用 Dubbo（如用户认证、权限检查、订单处理等）
    - 提供方：`system-service` 暴露 `com.frog.system.api.UserDubboService`、`com.frog.system.api.PermissionDubboService`
    - 消费方：`gateway`、`auth-service` 优先使用 Dubbo，失败回退到 Feign
  - 非实时异步操作：使用 Feign（如通知、日志记录等）
  - 跨团队服务集成：使用 Feign，便于理解和集成
  - 大数据传输：使用 Dubbo，性能更优
  - 外部系统对接：使用 Feign，协议兼容性好

启动顺序建议
1) 启动 Nacos（并创建上述 Data ID 配置）
2) 启动 `system-service`
3) 启动 `auth-service`
4) 启动 `gateway-service`
