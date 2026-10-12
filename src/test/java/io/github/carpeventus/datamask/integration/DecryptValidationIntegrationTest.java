package io.github.carpeventus.datamask.integration;

import io.github.carpeventus.datamask.DataMaskCipherService;
import io.github.carpeventus.datamask.DataMaskException;
import io.github.carpeventus.datamask.DataMaskException.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Seam 2（服务 Bean）：解密契约的全部校验与失败语义——条数上限、归属用户、有效期（可注入 Clock）、
 * 密文损坏，以及任一条失败整批失败。
 */
@SpringBootTest(properties = "data-mask.cipher-expire-time=PT12h")
class DecryptValidationIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-10-11T00:00:00Z");

    @Autowired
    private DataMaskCipherService cipherService;

    @Autowired
    private MutableClock mutableClock;

    @Test
    void acceptsExactlyFiftyCiphers() {
        List<String> ciphertexts = encryptEach("13812345678", 50);

        assertThat(cipherService.decrypt(ciphertexts)).hasSize(50);
    }

    @Test
    void rejectsMoreThanFiftyCiphers() {
        List<String> ciphertexts = encryptEach("13812345678", 51);

        assertThatThrownBy(() -> cipherService.decrypt(ciphertexts))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_CIPHERS);
    }

    @Test
    void rejectsCipherBelongingToAnotherUser() {
        DataMaskCipherService otherUserService = new DataMaskCipherService(
                () -> DataMaskTestApplication.TEST_CIPHER_KEY, () -> "9999", mutableClock, Duration.ofHours(12));
        String foreignCipher = otherUserService.encrypt("13812345678");

        assertThatThrownBy(() -> cipherService.decrypt(Collections.singletonList(foreignCipher)))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_MISMATCH);
    }

    @Test
    void acceptsCipherAtExactExpiryBoundary() {
        mutableClock.setInstant(BASE_TIME);
        String ciphertext = cipherService.encrypt("13812345678");
        mutableClock.setInstant(BASE_TIME.plus(Duration.ofHours(12)));

        assertThat(cipherService.decrypt(Collections.singletonList(ciphertext)))
                .containsExactly("13812345678");
    }

    @Test
    void rejectsExpiredCipher() {
        mutableClock.setInstant(BASE_TIME);
        String ciphertext = cipherService.encrypt("13812345678");
        mutableClock.setInstant(BASE_TIME.plus(Duration.ofHours(12)).plusSeconds(1));

        assertThatThrownBy(() -> cipherService.decrypt(Collections.singletonList(ciphertext)))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CIPHER_EXPIRED);
    }

    @Test
    void rejectsIllegalBase64Cipher() {
        assertThatThrownBy(() -> cipherService.decrypt(Collections.singletonList("not-base64!!")))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MALFORMED_CIPHER);
    }

    @Test
    void rejectsTruncatedCipher() {
        String tooShort = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[] {1, 2, 3});

        assertThatThrownBy(() -> cipherService.decrypt(Collections.singletonList(tooShort)))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MALFORMED_CIPHER);
    }

    @Test
    void rejectsTamperedCipher() {
        String ciphertext = cipherService.encrypt("13812345678");
        char replacement = ciphertext.charAt(20) == 'A' ? 'B' : 'A';
        String tampered = ciphertext.substring(0, 20) + replacement + ciphertext.substring(21);

        assertThatThrownBy(() -> cipherService.decrypt(Collections.singletonList(tampered)))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MALFORMED_CIPHER);
    }

    @Test
    void failsWholeBatchWhenAnyItemFails() {
        List<String> batch = Arrays.asList(
                cipherService.encrypt("13812345678"),
                "not-base64!!",
                cipherService.encrypt("dgs@qq.com"));

        assertThatThrownBy(() -> cipherService.decrypt(batch))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MALFORMED_CIPHER);
    }

    @Test
    void failsWholeBatchWhenOneItemBelongsToAnotherUser() {
        DataMaskCipherService otherUserService = new DataMaskCipherService(
                () -> DataMaskTestApplication.TEST_CIPHER_KEY, () -> "9999", mutableClock, Duration.ofHours(12));
        List<String> batch = Arrays.asList(
                cipherService.encrypt("13812345678"),
                otherUserService.encrypt("dgs@qq.com"));

        assertThatThrownBy(() -> cipherService.decrypt(batch))
                .isInstanceOf(DataMaskException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_MISMATCH);
    }

    private List<String> encryptEach(String plaintext, int count) {
        List<String> ciphertexts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ciphertexts.add(cipherService.encrypt(plaintext));
        }
        return ciphertexts;
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        MutableClock mutableClock() {
            return new MutableClock(BASE_TIME);
        }
    }

    /**
     * 测试用可变时钟：让"加密时刻 → 解密时刻"的推进在测试内可控。
     */
    static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
