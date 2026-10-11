package io.github.carpeventus.datamask.autoconfigure;

import com.fasterxml.jackson.databind.Module;
import io.github.carpeventus.datamask.CipherKeyProvider;
import io.github.carpeventus.datamask.DataMaskCipherService;
import io.github.carpeventus.datamask.CurrentUserProvider;
import io.github.carpeventus.datamask.internal.SensitiveApiResponseAdvice;
import io.github.carpeventus.datamask.internal.SensitiveDataJacksonModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * SDK 自动装配：注册 Jackson 脱敏模块与 {@code @SensitiveApi} 命中检测。
 *
 * <p>{@code data-mask.enabled=false} 时整个装配跳过，响应输出与引入 SDK 前完全一致。
 *
 * <p>启用状态下缺少任一扩展点实现（{@link CurrentUserProvider} / {@link CipherKeyProvider}）
 * 都会在应用启动时失败，并指明缺少的是哪个扩展点。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "data-mask", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataMaskProperties.class)
public class DataMaskAutoConfiguration {

    public DataMaskAutoConfiguration(ObjectProvider<CurrentUserProvider> currentUserProvider,
            ObjectProvider<CipherKeyProvider> cipherKeyProvider) {
        requireExtensionPoint(currentUserProvider, CurrentUserProvider.class);
        requireExtensionPoint(cipherKeyProvider, CipherKeyProvider.class);
    }

    @Bean
    public Module dataMaskJacksonModule(DataMaskCipherService dataMaskCipherService) {
        return new SensitiveDataJacksonModule(dataMaskCipherService);
    }

    @Bean
    public SensitiveApiResponseAdvice sensitiveApiResponseAdvice() {
        return new SensitiveApiResponseAdvice();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock dataMaskClock() {
        return Clock.systemUTC();
    }

    @Bean
    public DataMaskCipherService dataMaskCipherService(CipherKeyProvider cipherKeyProvider,
            CurrentUserProvider currentUserProvider, Clock dataMaskClock, DataMaskProperties dataMaskProperties) {
        return new DataMaskCipherService(cipherKeyProvider, currentUserProvider, dataMaskClock,
                dataMaskProperties.getCipherExpireTime());
    }

    private static void requireExtensionPoint(ObjectProvider<?> provider, Class<?> extensionPoint) {
        if (provider.getIfAvailable() == null) {
            throw new IllegalStateException("data-mask 已启用但缺少扩展点实现：" + extensionPoint.getName()
                    + "。请实现该接口并注册为 Spring Bean，或通过 data-mask.enabled=false 关闭 SDK。");
        }
    }
}
