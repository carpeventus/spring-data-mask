# 脱敏后端SDK

## 产出物和接入方

jar，最终会同步到maven中央仓库，供SpringBoot或SpringCloud引入使用。

## 整体要求

- 支持JDK8+
- 打包方式为Maven
- SDK自身尽可能少引入依赖
- 涉及Json序列化/反序列化使用Jackson
- 仅支持HTTP接口，不考虑RPC接口

## 接入方需要做什么 

### 配置文件

`application.yml`或者`application.properties`中配置密文过期时间等

示例yml配置

```yaml
data-mask:
  cipher_expire_time:PT12h
```

### 代码实现

1.sdk提供了一个interface，用户需要实现它。

- 如何获取当前登录用户
- 加解密的密钥获取（如果这不是一个好方式，请你提出疑问我们商量）

2.使用`SensitiveApi`和`SensitiveData`进行单接口与字段处理

## 脱敏要求

注解1：`SensitiveApi`，放在单个HTTP接口上，如和`@GetMapping`放一起

注解2：`SensitiveData`，放在字段上。

注解2支持的字段：
- type=SensitiveDataType.EMAIL\PHONE\ADDRESS\BANK_CARD_NO\ID_CARD_NO\ALL
- addCipherField=true/false，默认false

### 脱敏规则

- ALL: 全打 `*`号，固定8个
- PHONE：中间四位打 `*`号，如13812345678 → 138****5678，座机号也同等处理。
- BANK_CARD_NO/ID_CARD_NO: 保留前6位和后4位，中间打 `*`号
- EMAIL: 保留用户名首字符或前1-2位，保留 @ 后域名，其余掩码(如用户名很短只有3位及以下，就严格一些只保留首字符)
- ADDRESS：考虑国内常见的地址关键字词，使用正则进行匹配，将关键信息使用`*`号隐藏，比如**栋**室，每部分敏感数据固定两个*号

流程：先看是否有`SensitiveApi`注解，无则跳过，有则继续寻找`SensitiveData`。

被加`SensitiveData`的字段，只能是String类型。在返回结构中可能位于较深的层级，上层也可能是List。如下，`data.list.mobile`是需要脱敏的，且`list`下每个对象的`mobile`都需要脱敏
；`data.user.email`也需要脱敏。如果你觉得在很深的对象结构中不好寻找字段，或性能很低，你可以考虑新增第三个注解（与我商量，如非必要不要新增实体）

```json
{
    "data": {
        "list": [
            {"mobile": "13345678909"}
        ],
        "user": {
            "email": "dgs@qq.com"
        }
    }
}
```

## 加密要求

算法：AES-256-GCM
随机加密：对同一明文加密产生不同的密文
密钥：sdk不存储，由接入方提供；sdk提供扩展点由接入方维护

待加密的明文格式

```json
{"p": "13340567898","u": "4554", "t": 1791602452}
```

- `p`: plainText，明文字段
- `u`：user，用户信息，4554是用户id
- `t`: time，当前时间

新增的加密字段放在明文字段同层级，默认名称为xxCipher

 ## 解密要求

 输入：密文列表
 参数校验：长度最多50

 单个密文经解密后为 

 ```json
{"p": "13340567898","u": "4554", "t": 1791602452}
 ```

字段说明不再赘述。

需校验：

- 用户id是否一致
- 当前时间和t进行比较，超过配置的密文有效期，则报错密文失效

直接返回明文列表，需保证返回值列表大小与输入参数大小一致，部分解密失败认为整体失败，因为是按list顺序解密的，不做密文和明文的映射。