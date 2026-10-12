# 06: README 完善（终态收尾）

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 等全部功能到达终态后，为仓库补上可用的 README：五分钟接入指引（依赖坐标、两个扩展点的实现示例、注解用法、配置项说明、解密调用方式）与最小示例，让使用者不读源码即可接入。此时规格中的接口与配置已冻结，文档不会再随实现变动。

**Blocked by:** 01、02、03、04、05（全部到达终态后开始）

**Status:** ready-for-agent

- [x] README 覆盖：坐标引入、两个扩展点示例、`@SensitiveApi`/`@SensitiveData` 用法、密文字段与解密调用、全部配置项与默认值、JDK/Boot 兼容说明
- [x] 示例与当前实现一致，可照抄运行
- [x] 不含过时说法或失效路径

## Comments

- 2026-10-11 · root：README 终态完成，全部验收项通过。
  - 覆盖：本地安装 + Maven 坐标（注明尚未发布中央仓库）、两个扩展点实现示例（含"缺失即启动失败"说明）、注解与响应示例（脱敏值 + `<字段名>Cipher`）、配置表（`data-mask.enabled`=true、`cipher-expire-time`=PT24h，固定常量说明）、解密调用与 `DataMaskException` 四个错误码、密文串格式、六类脱敏规则表与边界行为、JDK 8+/Boot 2.7 验收与 Boot 3 尽力兼容说明、MIT。
  - 每处 DSL/类名/默认值/示例值均与当前实现及测试字面量核对一致（如 `138****5678`、`d**@qq.com`、`622202******2233`、地址示例、50 条上限、`Cipher` 后缀）。
