# 05: 解密校验加固（上限 / 归属 / 超期 / 损坏）

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 补全解密契约的全部校验与失败语义：单批最多 50 条；密文归属的当前用户必须与请求方一致；超过有效期（`data-mask.cipher-expire-time`，基于可注入 `Clock`）判定密文失效；密文损坏或非法编码报解密错误；任一条失败整批失败、不返回部分结果；错误统一为 `DataMaskException` 并携带可编程处理的错误码。

**Blocked by:** 04（密文字段与解密闭环）

**Status:** ready-for-agent

- [x] 51 条及以上被拒，错误码指明超出条数上限
- [x] 密文 `u` 与当前用户不一致 → 用户不一致错误码
- [x] 固定时钟下：超过有效期 → 密文失效错误码；未超期（含边界）→ 解密成功
- [x] 损坏密文 / 非法 Base64 → 解密错误码
- [x] 一批中第 N 条失败 → 整批失败，不返回部分结果
- [x] 每个失败形态都有独立测试（Seam 2），不断言实现内部结构

## Comments

- 2026-10-11 · root：实现完成，全部验收项通过（ticket 05 新增 10 个测试；全仓 41/41 绿）。
  - 新增公开异常 `DataMaskException`（错误码枚举：`CIPHER_EXPIRED`/`USER_MISMATCH`/`MALFORMED_CIPHER`/`TOO_MANY_CIPHERS`）；`DataMaskCipherService#decrypt` 的失败统一抛它。
  - 校验顺序：条数上限（恰好 50 通过、51 拒绝）→ 逐条解码（非法 Base64/长度不足/GCM 认证失败/载荷缺字段 → `MALFORMED_CIPHER`）→ 归属用户一致（不一致 → `USER_MISMATCH`）→ `now - t > cipher-expire-time` 判失效（相等边界仍有效；`Clock` 可注入，测试用可变时钟推进）。
  - 有效期来自 `DataMaskProperties#cipherExpireTime`（自动装配注入服务构造器），测试以 `PT12h` 属性 + 可变时钟锁定配置生效与边界行为。
  - 整批失败以异常中断实现，不返回部分结果；测试含"有效+无效+有效"与跨用户两种混批场景。
  - 测试对实现做了变异校验（放宽条数上限、把边界判为超期），对应测试均能失败，随后已还原。
