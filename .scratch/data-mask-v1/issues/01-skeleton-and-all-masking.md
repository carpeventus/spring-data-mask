# 01: 骨架贯通：自动装配 + 注解 + ALL 脱敏端到端

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 接入方第一次"能用"的最短完整路径：引入依赖后，标注 `@SensitiveApi` 的接口，其 `@SensitiveData(type = ALL)` 的 String 字段输出固定 8 个星号；未标注的接口完全不受影响。同时立起工程骨架：Maven 坐标 `io.github.carpeventus:data-mask-spring-boot-starter`、包 `io.github.carpeventus.datamask`、`--release 8`、依赖最小化（仅 Jackson 与 Spring 集成骨架）、Boot 2.7 测试栈、双自动装配注册文件、`data-mask.enabled` 开关（默认 true），以及测试作用域内最小 Boot 应用 + MockMvc 的验证骨架。

**Blocked by:** None（可立即开始）

**Status:** ready-for-agent

- [x] 标注 `@SensitiveApi` 的接口，其 `type = ALL` 字段返回 `********`（固定 8 星）
- [x] 同一应用中未标注 `@SensitiveApi` 的接口输出与引入前完全一致
- [x] 标注字段位于嵌套对象与 List 内时同样被脱敏
- [x] POST 请求体原样到达控制器（脱敏只作用于响应）
- [x] `data-mask.enabled=false` 时所有响应原样输出
- [x] 上述验证全部走 MockMvc（完整 MVC 栈）；`mvn install` 可产出构件

## Comments

- 2026-10-11 · ticket_01：实现完成，全部验收项通过。
  - 机制：`@SensitiveApi` 命中检测放在与 servlet 命名空间无关的 `@ControllerAdvice`（`ResponseBodyAdvice`）里，读取处理器方法注解并在 Jackson 写出响应体前置位请求作用域标记；Jackson `BeanSerializerModifier` 把标注字段换成脱敏序列化器，天然覆盖任意深度嵌套与 List 元素。刻意不用 `HandlerInterceptor`（会把构件绑死在 javax/jakarta servlet 类型上，见 ADR-0004）。
  - 验证：4 个 MockMvc 集成测试全绿（ALL→8 星含嵌套与 List、未标注接口原样、POST 请求体不受影响、`enabled=false` 原样）；`mvn install` 产出 `data-mask-spring-boot-starter-0.1.0-SNAPSHOT.jar`（class 文件 major 52，双自动装配注册文件在包内）。
  - 过渡说明：非 ALL 类型的脱敏规则、非 String 标注的报错、密文字段与解密留待 ticket 02/03/04。
