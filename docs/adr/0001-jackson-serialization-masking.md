# 脱敏在 Jackson 序列化层实现

`@SensitiveApi` 命中时给请求打上标记，脱敏值与密文字段的生成都发生在 Jackson 序列化 `@SensitiveData` String 字段时，而不是反射遍历返回对象。序列化层天然覆盖 List、Map、任意深度的嵌套，且不需要改动业务对象、不需要第三个注解；反射遍历则需要 SDK 自己重建对象树，深结构下复杂且容易漏。

## Considered Options

- 反射遍历返回对象（在 `ResponseBodyAdvice` 里改写对象）：浅结构可行，深结构风险与成本都高。
- 新增第三个注解用于标注深层字段：被否决——序列化层方案不需要它。
