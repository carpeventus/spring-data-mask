# 数据脱敏 SDK（data-mask-spring-boot-starter）

面向 Spring Boot / Spring Cloud 应用的后端 SDK：对 HTTP 响应做字段级脱敏，并提供可逆的字段级加密与批量解密。接入方实现扩展点即可启用。

## 角色与上下文

**接入方（Consumer）**:
引入本 SDK 的 Spring Boot / Spring Cloud 应用；由它实现扩展点、提供密钥，并决定对外如何暴露解密能力。
_Avoid_: 客户端、用户

**当前用户（Current user）**:
发起本次 HTTP 请求的已登录用户；由接入方通过 `CurrentUserProvider` 提供，是密文中 `u` 的取值。
_Avoid_: 账号、登录人

**扩展点（Provider）**:
SDK 暴露给接入方实现的接口：`CurrentUserProvider`（当前用户）与 `CipherKeyProvider`（AES-256 密钥）；启用状态下缺失则 SDK 启动失败。
_Avoid_: SPI、插件

## 脱敏

**脱敏（Masking）**:
对响应中 `@SensitiveData` 字段的值做不可逆的部分遮蔽，敏感片段替换为 `*`；只作用于响应，不改请求。
_Avoid_: 加密、混淆

**注解**:
`@SensitiveApi` 标在 HTTP 接口方法上，开启该响应的脱敏处理；`@SensitiveData` 标在 String 字段上，声明脱敏类型与是否附加密字段。
_Avoid_: 标签

**脱敏类型（SensitiveDataType）**:
决定遮蔽规则的枚举：`EMAIL`、`PHONE`、`ADDRESS`、`BANK_CARD_NO`、`ID_CARD_NO`、`ALL`。

## 加密与解密

**密文字段（Cipher field）**:
`addCipherField = true` 时与明文字段同层级、名为 `<字段名>Cipher` 的兄弟字段，承载可逆加密的结果。
_Avoid_: token

**密文串（Ciphertext）**:
字段加密后的文本形式，供持有密钥的服务端解密还原；随机 IV 使同一明文的每次加密产生不同的串。

**明文载荷（Plaintext payload）**:
密文串解出的 JSON：`p` 原始字段值、`u` 当前用户、`t` 加密时刻（epoch 秒）。

**密文失效（Cipher expiry）**:
解密校验不通过的情形：超过 `data-mask.cipher-expire-time` 有效期、用户不一致或密文损坏——整批解密失败。
_Avoid_: 过期（单独使用时含义模糊）

**解密（Decrypt）**:
把密文列表按顺序还原为等长明文列表，并校验归属与有效期；不做密文与明文的映射。
