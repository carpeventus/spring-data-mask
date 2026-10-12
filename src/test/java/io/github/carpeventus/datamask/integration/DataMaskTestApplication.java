package io.github.carpeventus.datamask.integration;

import io.github.carpeventus.datamask.CipherKeyProvider;
import io.github.carpeventus.datamask.CurrentUserProvider;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

/**
 * 测试作用域内的最小 Boot 应用：承载集成测试的完整 MVC 栈。
 *
 * <p>同时提供 SDK 所需的两个扩展点（当前用户与 32 字节密钥），供加密/解密相关测试复用。
 */
@SpringBootApplication
public class DataMaskTestApplication {

    /** 测试用固定 32 字节 AES-256 密钥。 */
    public static final byte[] TEST_CIPHER_KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    @Bean
    CurrentUserProvider currentUserProvider() {
        return () -> "4554";
    }

    @Bean
    CipherKeyProvider cipherKeyProvider() {
        return () -> TEST_CIPHER_KEY;
    }
}
