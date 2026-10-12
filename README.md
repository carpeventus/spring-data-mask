# data-mask-spring-boot-starter

面向 Spring Boot / Spring Cloud 应用的**响应字段脱敏**与**可逆字段加密** SDK：接口方法标注 `@SensitiveApi`、字段标注 `@SensitiveData` 后，响应中的 String 字段按规则脱敏；`addCipherField = true` 的字段在同一层级额外输出可逆的密文字段，服务端可按批解密还原。

## 特性

- 声明式接入：一个依赖 + 两个扩展点实现，无需改动既有控制器逻辑
- 脱敏只作用于响应；未标注 `@SensitiveApi` 的接口输出与引入前完全一致
- 任意深度的嵌套对象、List / 分页包装结构中的标注字段都会处理
- 密文字段（`<字段名>Cipher`）：AES-256-GCM、每次加密随机 IV，同一明文每次密文不同
- 解密以服务 Bean 提供（`DataMaskCipherService#decrypt`），校验密文归属与有效期；单批最多 50 条
- 运行时仅 Jackson 为硬依赖；SDK 不注册任何 HTTP 接口

## 环境要求

- JDK 8 及以上（工件以 Java 8 字节码编译）
- Spring Boot 2.7.x 为验收环境；Boot 3 尽力兼容（同时携带 `spring.factories` 与 `AutoConfiguration.imports`，且不触碰 `javax`/`jakarta` servlet 类型）
- MIT 协议

## 五分钟接入

### 1. 引入依赖

当前版本尚未发布到 Maven 中央仓库，先本地安装：

```bash
mvn install
```

```xml
<dependency>
  <groupId>io.github.carpeventus</groupId>
  <artifactId>data-mask-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. 实现两个扩展点（Spring Bean）

```java
@Configuration
class DataMaskConfig {

    @Bean
    CurrentUserProvider currentUserProvider() {
        // 当前登录用户标识，例如用户 ID；会写入密文并用于解密时的归属校验
        return () -> UserContext.currentUserId();
    }

    @Bean
    CipherKeyProvider cipherKeyProvider() {
        // AES-256 密钥，必须 32 字节；来源（配置/KMS/远端）与缓存策略由接入方决定
        return () -> loadAes256KeyFromKms(); // 接入方自己的密钥装载逻辑
    }
}
```

SDK 启用时缺少任一 Bean，应用会在启动阶段直接失败并指明缺少的扩展点；`data-mask.enabled=false` 时无需提供。

### 3. 标注接口与字段

```java
@SensitiveApi
@GetMapping("/orders/{id}")
public OrderView order() {
    return new OrderView("13812345678", "dgs@qq.com");
}

public class OrderView {

    @SensitiveData(type = SensitiveDataType.PHONE)
    private final String mobile;   // 响应中输出 138****5678

    @SensitiveData(type = SensitiveDataType.EMAIL, addCipherField = true)
    private final String email;    // 输出 d**@qq.com，并同层额外输出 emailCipher
    // 构造器与 getter 略
}
```

响应示例：

```json
{
  "mobile": "138****5678",
  "email": "d**@qq.com",
  "emailCipher": "R2lAd...（URL-safe Base64，无 padding）"
}
```

### 4. 配置

```yaml
data-mask:
  enabled: true             # 默认 true；一键关闭 SDK，响应与引入前一致
  cipher-expire-time: PT24h # 密文有效期，默认 24 小时；也兼容 cipher_expire_time 写法
```

| 配置项 | 默认值 | 说明 |
| ------ | ------ | ---- |
| `data-mask.enabled` | `true` | 是否启用 SDK；`false` 时跳过校验、全部原样输出 |
| `data-mask.cipher-expire-time` | `PT24h` | 密文有效期（`Duration`）；解密时超过即判“密文失效” |

密文字段后缀固定为 `Cipher`、单批解密上限固定为 50 条，均为常量、不提供配置。

### 5. 解密（接入方自行暴露 HTTP 接口）

```java
@RestController
class DecryptController {

    private final DataMaskCipherService cipherService; // SDK 提供的 Bean，直接注入

    DecryptController(DataMaskCipherService cipherService) {
        this.cipherService = cipherService;
    }

    @PostMapping("/decrypt")
    public List<String> decrypt(@RequestBody List<String> ciphertexts) {
        // 返回与输入顺序一致、等长的明文列表；不做密文与明文的映射
        return cipherService.decrypt(ciphertexts);
    }
}
```

失败统一抛 `DataMaskException`，错误码可编程处理：

| 错误码 | 含义 |
| ------ | ---- |
| `TOO_MANY_CIPHERS` | 单批超过 50 条 |
| `USER_MISMATCH` | 密文归属用户与当前用户不一致 |
| `CIPHER_EXPIRED` | 密文超过有效期 |
| `MALFORMED_CIPHER` | 密文损坏或非法编码 |

任一条失败整批失败（不返回部分结果）；状态码与文案由接入方自行决定。

密文串为 URL-safe Base64（无 padding）编码的 `IV(12B) ‖ 密文体 ‖ 认证标签(16B)`，明文载荷为 JSON `{"p": 原始值, "u": 当前用户, "t": epoch 秒}`。

## 脱敏规则

| 类型 | 规则 | 示例 |
| ---- | ---- | ---- |
| `EMAIL` | 用户名 ≤3 位只保留首字符、≥4 位保留前 2 位，其余 1:1 换 `*`；`@` 后域名原样保留 | `dgs@qq.com` → `d**@qq.com`；`hello@qq.com` → `he***@qq.com`；`ab@qq.com` → `a*@qq.com` |
| `PHONE`（手机） | 以 `1` 开头的 11 位纯数字：保留前 3 位与后 4 位 | `13812345678` → `138****5678` |
| `PHONE`（座机） | 含分隔符时区号原样保留、号码部分只保留后 4 位；无分隔纯数字只保留后 4 位 | `0571-88889999` → `0571-****9999`；`88889999` → `****9999` |
| `BANK_CARD_NO` / `ID_CARD_NO` | 保留前 6 位与后 4 位，中间 1:1 换 `*` | `6222020200112233` → `622202******2233` |
| `ADDRESS` | 仅掩门牌级数字（`号楼`/`栋`/`幢`/`单元`/`室`/`号` 前），各固定 `**`；省市区、道路名、小区名不掩 | `浙江省杭州市西湖区文一西路969号3栋2单元502室` → `浙江省杭州市西湖区文一西路**号**栋**单元**室` |
| `ALL` | 固定 8 个 `*` | 任意非空值 → `********` |

边界行为：

- 值为 `null` 或空串时原样输出，且不生成密文字段
- 非空且不符合对应模式的值整体输出 `********`（宁可多掩也不泄漏），例如不含 `@` 的 `EMAIL` 值、不含门牌级数字的 `ADDRESS` 值
- `@SensitiveData` 仅支持 String 字段；误标在非 String 字段上会以清晰异常暴露，而不是静默不脱敏
- 脱敏与密文字段只作用于标注了 `@SensitiveApi` 的接口响应；请求参数与日志不受影响

## 说明

- SDK 不缓存密钥与当前用户：每次加密/解密都会调用扩展点；如需缓存请在实现内自行处理
- 密文只用于服务端按用户、限时还原业务原文，不替代传输安全（HTTPS 请另行保证）
- 密钥轮换与密文格式版本不在 v1 范围内
