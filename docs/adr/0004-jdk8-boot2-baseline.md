# 兼容基线：JDK 8 + Spring Boot 2.7，为 Boot 3 留门

验收与测试只覆盖 JDK 8 + Spring Boot 2.7.x。实现时刻意不碰 servlet 的 `javax`/`jakarta` 类型，并同时携带 `spring.factories` 与 `AutoConfiguration.imports` 两种自动装配注册文件，让 Boot 3 成为"尽力兼容"（不写进验收）。否则将来支持 Boot 3 需要重写而不是顺延。

## Considered Options

- 只面向 Boot 3（JDK 17）：被否决——需求要求 JDK 8+，而 Boot 3 需要 JDK 17。
