package io.github.carpeventus.datamask.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code data-mask.*} 配置项（Spring 宽松绑定，{@code cipher_expire_time} 等写法同样生效）。
 */
@ConfigurationProperties(prefix = "data-mask")
public class DataMaskProperties {

    /** 是否启用 SDK，默认开启。 */
    private boolean enabled = true;

    /** 密文有效期，默认 24 小时。 */
    private Duration cipherExpireTime = Duration.ofHours(24);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getCipherExpireTime() {
        return cipherExpireTime;
    }

    public void setCipherExpireTime(Duration cipherExpireTime) {
        this.cipherExpireTime = cipherExpireTime;
    }
}
