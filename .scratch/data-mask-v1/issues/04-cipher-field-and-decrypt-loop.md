# 04: 密文字段与解密闭环（主干路径）

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 端到端可逆加密：`addCipherField = true` 的字段在响应中除脱敏值外，同层级多出 `<字段名>Cipher` 密文字段；密文串为 AES-256-GCM（每次随机 12 字节 IV）的 URL-safe Base64(`IV‖密文体‖tag`)；明文载荷为 `{p,u,t}`（`u` 取当前用户、`t` 为 epoch 秒）；`DataMaskCipherService#decrypt(List<String>)` 主干路径在同用户、未超期时按序还原为等长明文列表；SDK 不新增任何 HTTP 入口。

**Blocked by:** 01（骨架贯通）、03（扩展点与启动校验）

**Status:** ready-for-agent

- [ ] `addCipherField = true` 时响应同层出现 `<字段名>Cipher`，原字段仍输出脱敏值
- [ ] 同一明文两次加密产生不同的密文串
- [ ] 闭环成立：HTTP 响应中取出的密文可被 `decrypt` 还原为原值
- [ ] 批量解密保持输入顺序、返回等长列表
- [ ] 密文串格式符合约定：URL-safe Base64（无 padding），含 12B IV 与 16B 认证标签
- [ ] 确认 SDK 未注册任何 HTTP 接口/Controller
