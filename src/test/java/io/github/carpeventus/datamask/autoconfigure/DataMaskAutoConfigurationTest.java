package io.github.carpeventus.datamask.autoconfigure;

import io.github.carpeventus.datamask.CipherKeyProvider;
import io.github.carpeventus.datamask.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seam 3（自动装配/启动）：扩展点缺失的 fail-fast、开关跳过校验与配置绑定。
 */
class DataMaskAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataMaskAutoConfiguration.class));

    @Test
    void failsFastWhenCurrentUserProviderIsMissing() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(allMessagesOf(context.getStartupFailure())).contains("CurrentUserProvider");
        });
    }

    @Test
    void failsFastWhenCipherKeyProviderIsMissing() {
        runner.withUserConfiguration(CurrentUserOnlyConfiguration.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(allMessagesOf(context.getStartupFailure())).contains("CipherKeyProvider");
        });
    }

    @Test
    void startsWhenBothExtensionPointsAreProvided() {
        runner.withUserConfiguration(BothProvidersConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void skipsValidationWhenDisabled() {
        runner.withPropertyValues("data-mask.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void cipherExpireTimeDefaultsToTwentyFourHours() {
        runner.withUserConfiguration(BothProvidersConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(DataMaskProperties.class).getCipherExpireTime())
                    .isEqualTo(Duration.ofHours(24));
        });
    }

    @Test
    void bindsExplicitCipherExpireTime() {
        runner.withUserConfiguration(BothProvidersConfiguration.class)
                .withPropertyValues("data-mask.cipher-expire-time=PT12h")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DataMaskProperties.class).getCipherExpireTime())
                            .isEqualTo(Duration.ofHours(12));
                });
    }

    @Test
    void bindsRelaxedCipherExpireTimeSpelling() {
        runner.withUserConfiguration(BothProvidersConfiguration.class)
                .withPropertyValues("data-mask.cipher_expire_time=PT6h")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DataMaskProperties.class).getCipherExpireTime())
                            .isEqualTo(Duration.ofHours(6));
                });
    }

    private static String allMessagesOf(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }

    @Configuration(proxyBeanMethods = false)
    static class CurrentUserOnlyConfiguration {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return () -> "4554";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BothProvidersConfiguration {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return () -> "4554";
        }

        @Bean
        CipherKeyProvider cipherKeyProvider() {
            return () -> new byte[32];
        }
    }
}
