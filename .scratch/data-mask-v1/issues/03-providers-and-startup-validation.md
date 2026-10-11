# 03: 扩展点与启动校验（fail-fast）

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 加密能力的地基与接入方配置面：两个扩展点 `CurrentUserProvider#currentUserId()` 与 `CipherKeyProvider#cipherKey()`（32 字节）由接入方实现为 Spring Bean；启用状态下缺任一 Bean 应用启动失败并给出清晰报错；`enabled=false` 时跳过校验；`data-mask.cipher-expire-time` 可配置（显式 `PT12h` 生效），未配置时默认 `PT24h`。

**Blocked by:** 01（骨架贯通）

**Status:** ready-for-agent

- [ ] 缺 `CurrentUserProvider` 且启用 → 启动失败，报错信息指明缺少哪个扩展点
- [ ] 缺 `CipherKeyProvider` 且启用 → 启动失败，报错信息指明缺少哪个扩展点
- [ ] `data-mask.enabled=false` 时缺扩展点也能正常启动
- [ ] `data-mask.cipher-expire-time=PT12h` 正确绑定；未配置时默认 `PT24h`
- [ ] 全部通过 `ApplicationContextRunner` 验证（自动装配 seam）
