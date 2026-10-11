package io.github.carpeventus.datamask.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seam 1（HTTP 集成）：{@code data-mask.enabled=false} 时，标注接口也原样输出。
 */
@SpringBootTest(properties = "data-mask.enabled=false")
@AutoConfigureMockMvc
class DataMaskDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void leavesResponsesUntouchedWhenDisabled() throws Exception {
        mockMvc.perform(get("/sensitive/order"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n"
                        + "  \"phone\": \"13812345678\",\n"
                        + "  \"nickname\": \"Sam\",\n"
                        + "  \"address\": {\"email\": \"dgs@qq.com\", \"city\": \"Hangzhou\"},\n"
                        + "  \"cards\": [{\"cardNo\": \"6222020200112233\", \"bank\": \"ICBC\"}]\n"
                        + "}", true));
    }
}
