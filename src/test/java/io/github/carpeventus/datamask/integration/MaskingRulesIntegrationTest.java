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
 * Seam 1（HTTP 集成）：EMAIL 与 PHONE（手机/座机）的脱敏规则端到端行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MaskingRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void masksEmailUsernameKeepingFirstCharactersAndDomain() throws Exception {
        mockMvc.perform(get("/sensitive/rules/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("d**@qq.com"))
                .andExpect(jsonPath("$.shortEmail").value("a*@qq.com"))
                .andExpect(jsonPath("$.longEmail").value("he***@qq.com"));
    }

    @Test
    void masksMobileKeepingFirstThreeAndLastFourDigits() throws Exception {
        mockMvc.perform(get("/sensitive/rules/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobile").value("138****5678"));
    }

    @Test
    void masksLandlineKeepingAreaCodeAndLastFourDigits() throws Exception {
        mockMvc.perform(get("/sensitive/rules/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.landline").value("0571-****9999"))
                .andExpect(jsonPath("$.landlineShortArea").value("010-****5678"));
    }

    @Test
    void masksUnseparatedLandlineKeepingOnlyLastFourDigits() throws Exception {
        mockMvc.perform(get("/sensitive/rules/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.landlineWithoutSeparator").value("****9999"));
    }
}
