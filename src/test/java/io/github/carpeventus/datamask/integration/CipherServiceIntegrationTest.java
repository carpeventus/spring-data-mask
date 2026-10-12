package io.github.carpeventus.datamask.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.carpeventus.datamask.DataMaskCipherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Seam 2（服务 Bean）：加密随机性、批量解密顺序与等长、密文串格式，以及与 Seam 1 的加密→解密闭环。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CipherServiceIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private DataMaskCipherService cipherService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void samePlaintextEncryptsToDifferentCiphertexts() {
        String first = cipherService.encrypt("13812345678");
        String second = cipherService.encrypt("13812345678");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipherService.decrypt(Arrays.asList(first, second)))
                .containsExactly("13812345678", "13812345678");
    }

    @Test
    void decryptsCipherFromHttpResponseBackToOriginalValue() throws Exception {
        String body = mockMvc.perform(get("/sensitive/cipher/contact"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String phoneCipher = JsonPath.read(body, "$.phoneCipher");
        String emailCipher = JsonPath.read(body, "$.emailCipher");

        List<String> plaintexts = cipherService.decrypt(Arrays.asList(phoneCipher, emailCipher));

        assertThat(plaintexts).containsExactly("13812345678", "dgs@qq.com");
    }

    @Test
    void batchDecryptPreservesInputOrderAndLength() {
        List<String> originals = Arrays.asList("13812345678", "dgs@qq.com", "0571-88889999");
        List<String> ciphertexts = originals.stream()
                .map(cipherService::encrypt)
                .collect(Collectors.toList());

        List<String> plaintexts = cipherService.decrypt(ciphertexts);

        assertThat(plaintexts).hasSize(originals.size()).containsExactlyElementsOf(originals);
    }

    @Test
    void cipherStringIsUrlSafeBase64WithoutPaddingContainingIvCiphertextAndTag() throws Exception {
        String ciphertext = cipherService.encrypt("13812345678");

        assertThat(ciphertext).matches("[A-Za-z0-9_-]+");
        byte[] combined = Base64.getUrlDecoder().decode(ciphertext);
        assertThat(combined.length).isGreaterThanOrEqualTo(12 + 16 + 1);

        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] sealed = Arrays.copyOfRange(combined, 12, combined.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DataMaskTestApplication.TEST_CIPHER_KEY, "AES"),
                new GCMParameterSpec(128, iv));
        JsonNode payload = JSON.readTree(cipher.doFinal(sealed));

        assertThat(payload.get("p").asText()).isEqualTo("13812345678");
        assertThat(payload.get("u").asText()).isEqualTo("4554");
        assertThat(payload.get("t").asLong())
                .isBetween(Instant.now().getEpochSecond() - 60, Instant.now().getEpochSecond() + 60);
    }

    @Test
    void sdkRegistersNoHttpEndpoints() {
        Map<String, Object> controllers = context.getBeansWithAnnotation(Controller.class);

        List<String> sdkControllers = controllers.values().stream()
                .map(bean -> bean.getClass().getName())
                .filter(name -> name.startsWith("io.github.carpeventus.datamask")
                        && !name.startsWith("io.github.carpeventus.datamask.integration"))
                .collect(Collectors.toList());

        assertThat(sdkControllers).as("SDK 自身不应注册任何 HTTP 接口/Controller").isEmpty();
    }

    @Test
    void rejectsKeysThatAreNot32BytesAtUseTime() {
        DataMaskCipherService shortKeyService = new DataMaskCipherService(
                () -> new byte[16], () -> "4554", java.time.Clock.systemUTC(), Duration.ofHours(24));

        assertThatThrownBy(() -> shortKeyService.encrypt("13812345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 字节");
    }
}
