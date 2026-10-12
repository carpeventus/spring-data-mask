# 03: 扩展点与启动校验（fail-fast）

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 加密能力的地基与接入方配置面：两个扩展点 `CurrentUserProvider#currentUserId()` 与 `CipherKeyProvider#cipherKey()`（32 字节）由接入方实现为 Spring Bean；启用状态下缺任一 Bean 应用启动失败并给出清晰报错；`enabled=false` 时跳过校验；`data-mask.cipher-expire-time` 可配置（显式 `PT12h` 生效），未配置时默认 `PT24h`。

**Blocked by:** 01（骨架贯通）

**Status:** ready-for-agent

- [x] 缺 `CurrentUserProvider` 且启用 → 启动失败，报错信息指明缺少哪个扩展点
- [x] 缺 `CipherKeyProvider` 且启用 → 启动失败，报错信息指明缺少哪个扩展点
- [x] `data-mask.enabled=false` 时缺扩展点也能正常启动
- [x] `data-mask.cipher-expire-time=PT12h` 正确绑定；未配置时默认 `PT24h`
- [x] 全部通过 `ApplicationContextRunner` 验证（自动装配 seam）

## Comments

- 2026-10-11 · root：实现完成，全部验收项通过（7/7 Seam 3 测试绿，另回归集成测试 4 个）。
  - 扩展点：`io.github.carpeventus.datamask.CurrentUserProvider#currentUserId()` 与 `CipherKeyProvider#cipherKey()`（javadoc 写明 32 字节契约与"每次调用、不缓存"语义）；实现类是接入方的 Spring Bean。
  - fail-fast：`DataMaskAutoConfiguration` 构造器经 `ObjectProvider` 校验两个扩展点，缺失时抛 `IllegalStateException`，信息含缺失扩展点全限定名与解决办法；`data-mask.enabled=false` 时整个自动装配跳过、无需扩展点。
  - 配置：新增 `DataMaskProperties`（`@ConfigurationProperties("data-mask")`），`cipher-expire-time` 默认 `PT24h`，宽松绑定（`cipher_expire_time` 亦可）由 Seam 3 测试锁定。
  - 顺带修正：集成测试应用 `DataMaskTestApplication` 注册测试用当前用户与 32 字节密钥（fail-fast 生效后旧的集成测试上下文需要它们）。
