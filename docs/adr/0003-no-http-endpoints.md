# SDK 不注册任何 HTTP 接口

解密只以服务 Bean（`DataMaskCipherService#decrypt(List<String>)`）的形式提供，接入方在自己的 Controller 中暴露，URL、参数格式与鉴权风格由接入方决定。SDK 自动注册 HTTP 接口的方案被否决：那会替接入方决定 URL 约定与鉴权方式，且扩大 SDK 的暴露面。

## Consequences

- 错误以单一异常类 `DataMaskException`（携带错误码）抛出，由接入方转成自己的 HTTP 响应。
