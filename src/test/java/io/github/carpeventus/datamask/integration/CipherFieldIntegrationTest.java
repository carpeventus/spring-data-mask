package io.github.carpeventus.datamask.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seam 1（HTTP 集成）：{@code addCipherField = true} 时同层级生成 {@code <字段名>Cipher} 密文字段，
 * 原字段仍输出脱敏值；未标注接口与 null/空值不产生密文字段。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CipherFieldIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addsCipherSiblingFieldWhileMaskingOriginal() throws Exception {
        mockMvc.perform(get("/sensitive/cipher/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("138****5678"))
                .andExpect(jsonPath("$.phoneCipher").isNotEmpty())
                .andExpect(jsonPath("$.email").value("d**@qq.com"))
                .andExpect(jsonPath("$.emailCipher").isNotEmpty())
                .andExpect(jsonPath("$.nickname").value("********"))
                .andExpect(jsonPath("$.nicknameCipher").doesNotExist());
    }

    @Test
    void leavesUnmarkedEndpointsWithoutCipherFields() throws Exception {
        mockMvc.perform(get("/plain/cipher/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13812345678"))
                .andExpect(jsonPath("$.phoneCipher").doesNotExist())
                .andExpect(jsonPath("$.email").value("dgs@qq.com"))
                .andExpect(jsonPath("$.emailCipher").doesNotExist());
    }

    @Test
    void skipsCipherFieldsForNullAndEmptyValues() throws Exception {
        mockMvc.perform(get("/sensitive/cipher/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nullablePhone").doesNotExist())
                .andExpect(jsonPath("$.nullablePhoneCipher").doesNotExist())
                .andExpect(jsonPath("$.emptyPhone").value(""))
                .andExpect(jsonPath("$.emptyPhoneCipher").doesNotExist());
    }
}
