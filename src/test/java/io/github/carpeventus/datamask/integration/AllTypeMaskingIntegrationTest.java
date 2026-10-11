package io.github.carpeventus.datamask.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seam 1（HTTP 集成）：{@code @SensitiveApi} + {@code type = ALL} 的端到端行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AllTypeMaskingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void masksAllAnnotatedStringFieldsAcrossNestedObjectsAndListElements() throws Exception {
        mockMvc.perform(get("/sensitive/order"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n"
                        + "  \"phone\": \"********\",\n"
                        + "  \"nickname\": \"Sam\",\n"
                        + "  \"address\": {\"email\": \"********\", \"city\": \"Hangzhou\"},\n"
                        + "  \"cards\": [{\"cardNo\": \"********\", \"bank\": \"ICBC\"}]\n"
                        + "}", true));
    }

    @Test
    void leavesUnannotatedEndpointOutputUntouched() throws Exception {
        mockMvc.perform(get("/plain/order"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n"
                        + "  \"phone\": \"13812345678\",\n"
                        + "  \"nickname\": \"Sam\",\n"
                        + "  \"address\": {\"email\": \"dgs@qq.com\", \"city\": \"Hangzhou\"},\n"
                        + "  \"cards\": [{\"cardNo\": \"6222020200112233\", \"bank\": \"ICBC\"}]\n"
                        + "}", true));
    }

    @Test
    void doesNotMaskRequestBody() throws Exception {
        mockMvc.perform(post("/sensitive/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"13812345678\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n"
                        + "  \"echo\": \"13812345678\",\n"
                        + "  \"masked\": \"********\"\n"
                        + "}", true));
    }
}
