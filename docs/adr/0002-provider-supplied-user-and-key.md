# 密钥与当前用户由接入方提供

SDK 不持有、也不缓存任何秘密：当前用户由 `CurrentUserProvider#currentUserId()` 提供，AES-256 密钥由 `CipherKeyProvider#cipherKey()` 提供，两者都是接入方实现的 Spring Bean。密钥从配置、KMS 还是远端获取，如何轮换、是否缓存，全部由接入方决定；SDK 每次使用即调用，密钥不是 32 字节则直接报错。

## Consequences

- 两个 Bean 缺失（且 SDK 启用）时启动即失败，配置错误在最早期暴露。
- 接入方若想减少每次调用的开销，可在自己的实现里加缓存，SDK 不做二次缓存。
