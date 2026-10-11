# 02: 脱敏规则全集与边界行为

> 规格：`.scratch/data-mask-v1/spec.md`

**What to build:** 在骨架之上补齐全部脱敏类型与边界行为，使规格中每条脱敏规则都可用、可验证：EMAIL、PHONE（含手机/座机判定）、BANK_CARD_NO、ID_CARD_NO、ADDRESS（仅门牌级、固定两个星号），以及 null/空原样、不符合模式全掩、非 String 标注报错、非 JSON 响应不动。

**Blocked by:** 01（骨架贯通）

**Status:** ready-for-agent

- [x] EMAIL：`dgs@qq.com → d**@qq.com`、`hello@qq.com → he***@qq.com`、`ab@qq.com → a*@qq.com`，`@` 后域名保留
- [x] PHONE 手机：`13812345678 → 138****5678`；座机：`0571-88889999 → 0571-****9999`、`010-12345678 → 010-****5678`；无分隔纯数字座机只保留后 4 位
- [x] BANK_CARD_NO / ID_CARD_NO：保留前 6 位与后 4 位，中间按 1:1 换星
- [x] ADDRESS：`浙江省杭州市西湖区文一西路969号3栋2单元502室 → 浙江省杭州市西湖区文一西路**号**栋**单元**室`；省市区、道路名、小区名不掩
- [x] 各类型位于嵌套对象与 List 中时同样生效
- [x] null / 空串原样输出；非空且不符合模式（如"未填写"、位数不足）输出 `********`
- [x] `@SensitiveData` 标注在非 String 字段上时抛出清晰异常，而不是静默不脱敏
- [x] 非 JSON（不经 Jackson 序列化）的响应不受影响

## Comments

- 2026-10-11 · root：实现完成，全部验收项通过（15/15 测试绿，含 ticket 01 的 4 个回归测试）。
  - 规则集中在 `internal/MaskingRules`（EMAIL/PHONE 手机与座机/卡证/地址/ALL），位置与判据：数值型（座机、卡证）均要求掩码后仍有字符被遮蔽，否则整体 `********`；ADDRESS 仅掩 号楼/栋/幢/单元/室/号 前的数字（固定 `**`），不含门牌级数字时整体全掩。
  - 误标报错刻意放在**写出字段时**（`MisconfiguredFieldSerializer`）而不是序列化器构建期：Spring 的 `MappingJackson2HttpMessageConverter.canWrite` 会吞掉构建期异常并回退到未注册本模块的转换器，字段会被静默原样输出；写出期抛错可稳定得到 500 与清晰信息（测试已锁定该行为）。
  - 非 JSON 响应（`text/plain`）已验证不受影响；null/空串原样。
