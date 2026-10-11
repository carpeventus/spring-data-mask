package io.github.carpeventus.datamask.autoconfigure;

import com.fasterxml.jackson.databind.Module;
import io.github.carpeventus.datamask.internal.SensitiveApiResponseAdvice;
import io.github.carpeventus.datamask.internal.SensitiveDataJacksonModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SDK 自动装配：注册 Jackson 脱敏模块与 {@code @SensitiveApi} 命中检测。
 *
 * <p>{@code data-mask.enabled=false} 时整个装配跳过，响应输出与引入 SDK 前完全一致。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "data-mask", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataMaskAutoConfiguration {

    @Bean
    public Module dataMaskJacksonModule() {
        return new SensitiveDataJacksonModule();
    }

    @Bean
    public SensitiveApiResponseAdvice sensitiveApiResponseAdvice() {
        return new SensitiveApiResponseAdvice();
    }
}
