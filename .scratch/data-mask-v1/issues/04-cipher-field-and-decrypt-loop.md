# 04: 密文字段与解密闭环（主干路径）

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 端到端可逆加密：`addCipherField = true` 的字段在响应中除脱敏值外，同层级多出 `<字段名>Cipher` 密文字段；密文串为 AES-256-GCM（每次随机 12 字节 IV）的 URL-safe Base64(`IV‖密文体‖tag`)；明文载荷为 `{p,u,t}`（`u` 取当前用户、`t` 为 epoch 秒）；`DataMaskCipherService#decrypt(List<String>)` 主干路径在同用户、未超期时按序还原为等长明文列表；SDK 不新增任何 HTTP 入口。

**Blocked by:** 01（骨架贯通）、03（扩展点与启动校验）

**Status:** ready-for-agent

- [x] `addCipherField = true` 时响应同层出现 `<字段名>Cipher`，原字段仍输出脱敏值
- [x] 同一明文两次加密产生不同的密文串
- [x] 闭环成立：HTTP 响应中取出的密文可被 `decrypt` 还原为原值
- [x] 批量解密保持输入顺序、返回等长列表
- [x] 密文串格式符合约定：URL-safe Base64（无 padding），含 12B IV 与 16B 认证标签
- [x] 确认 SDK 未注册任何 HTTP 接口/Controller

## Comments

- 2026-10-11 · root：实现完成，全部验收项通过（ticket 04 新增 9 个测试；全仓 31/31 绿）。
  - 密文串：AES-256-GCM，随机 12 字节 IV，URL-safe Base64（无 padding）编码 `IV‖密文体‖tag(16B)`；载荷 JSON `{"p","u","t"}`，`u` 取 `CurrentUserProvider#currentUserId()`，`t` 为 `Instant.now(clock)` 的 epoch 秒。格式由独立手工解密测试锁定（含 p/u/t 断言）。
  - 密文字段：`CipherFieldWriter`（`BeanPropertyWriter` 子类）在命中 `@SensitiveApi` 标记且值非空时写出 `<字段名>Cipher`；未标注接口、null/空值不产生密文字段（均有测试锁定）。
  - 服务：`DataMaskCipherService#encrypt/decrypt`（公开 Bean，构造注入两个扩展点与 `Clock`；密钥非 32 字节在使用时报错）；自动装配新增 `Clock`（`@ConditionalOnMissingBean`，便于测试固定时钟）。
  - SDK 未注册任何 HTTP 接口：以 `@Controller` Bean 扫描断言（SDK 包名下无控制器）。
